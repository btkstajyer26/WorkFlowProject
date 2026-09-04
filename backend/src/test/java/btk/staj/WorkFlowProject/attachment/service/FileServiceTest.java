package btk.staj.WorkFlowProject.attachment.service;

import static btk.staj.WorkFlowProject.support.AuthorizationFixtures.visibility;

import btk.staj.WorkFlowProject.attachment.dto.FileResponseDto;
import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dosya yukleme ve yonetimi (M7)")
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileContentValidator fileContentValidator;

    @Mock
    private RecordLockValidator recordLockValidator;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private RecordAccessPolicy recordAccessPolicy;

    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID UPLOADER_ID = UUID.randomUUID();

    private FileService fileService() {
        return new FileService(
                fileRepository,
                fileStorageService,
                fileContentValidator,
                recordLockValidator,
                recordRepository,
                recordAccessPolicy,
                new RecordContentView(new RecordAccessPolicy(actor -> java.util.Set.of()))
        );
    }

    private MockMultipartFile dosya(String originalName) {
        return new MockMultipartFile("file", originalName, "application/octet-stream", "icerik".getBytes());
    }

    private String storedNameFor(String originalName) {
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(fileContentValidator.detectAndValidate(any())).thenReturn("application/pdf");
        when(fileContentValidator.extensionFor("application/pdf")).thenReturn(".pdf");

        fileService().uploadFile(dosya(originalName), RECORD_ID, UPLOADER_ID);

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).store(any(), storedName.capture());
        return storedName.getValue();
    }

    @Test
    @DisplayName("diskteki ad yalnizca GUID'den uretilir, orijinal ad karismaz")
    void diskAdiSadeceGuidOlur() {
        String storedName = storedNameFor("Butce Raporu 2026.pdf");

        assertThat(storedName).doesNotContain("Butce", "Raporu", " ");
        assertThat(storedName).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf");
    }

    @Test
    @DisplayName("dosya adindaki dizin asimi denemesi diskteki ada tasinmaz")
    void dizinAsimiDenemesiAdaTasinmaz() {
        String storedName = storedNameFor("../../../etc/passwd.pdf");

        assertThat(storedName).doesNotContain("..", "/", "\\");
    }

    @Test
    @DisplayName("uzanti dosya adindan degil dogrulanmis turden gelir")
    void uzantiTurdenTuretilir() {
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(fileContentValidator.detectAndValidate(any())).thenReturn("image/png");
        when(fileContentValidator.extensionFor("image/png")).thenReturn(".png");

        fileService().uploadFile(dosya("aslinda-resim.pdf"), RECORD_ID, UPLOADER_ID);

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).store(any(), storedName.capture());
        assertThat(storedName.getValue()).endsWith(".png");
    }

    @Test
    @DisplayName("desteklenmeyen format reddedilir ve diske hicbir sey yazilmaz")
    void desteklenmeyenFormatReddedilir() {
        when(fileContentValidator.detectAndValidate(any()))
                .thenThrow(new BusinessRuleException("Desteklenmeyen dosya formatı: application/x-msdownload"));

        assertThatThrownBy(() -> fileService().uploadFile(dosya("zararli.exe"), RECORD_ID, UPLOADER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Desteklenmeyen dosya formatı");

        verify(fileStorageService, never()).store(any(), anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("kilitli kayda dosya eklenemez")
    void kilitliKaydaEklenemez() {
        doThrow(new BusinessRuleException("Bu kayıt 'ONAYLANDI' durumunda, dosya eklenemez"))
                .when(recordLockValidator).assertModifyAllowed(RECORD_ID, UPLOADER_ID);

        assertThatThrownBy(() -> fileService().uploadFile(dosya("rapor.pdf"), RECORD_ID, UPLOADER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dosya eklenemez");

        verify(fileStorageService, never()).store(any(), anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("orijinal ad veritabaninda korunur")
    void orijinalAdVeritabaninaYazilir() {
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(fileContentValidator.detectAndValidate(any())).thenReturn("application/pdf");
        when(fileContentValidator.extensionFor("application/pdf")).thenReturn(".pdf");

        FileResponseDto saved = fileService().uploadFile(dosya("Butce Raporu 2026.pdf"), RECORD_ID, UPLOADER_ID);

        assertThat(saved.getOriginalName()).isEqualTo("Butce Raporu 2026.pdf");
        assertThat(saved.getMimeType()).isEqualTo("application/pdf");
    }

    // --- M7 Mobil Senaryo Testleri ---

    @Test
    @DisplayName("M7: Coklu gecerli dosya yukleme basarili olur ve liste doner")
    void cokluGecerliDosyaYuklemeBasarili() {
        MultipartFile[] files = new MultipartFile[]{
                dosya("belge1.pdf"),
                dosya("belge2.pdf")
        };

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(fileContentValidator.detectAndValidate(any())).thenReturn("application/pdf");
        when(fileContentValidator.extensionFor("application/pdf")).thenReturn(".pdf");

        List<FileResponseDto> responses = fileService().uploadFiles(files, RECORD_ID, UPLOADER_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getOriginalName()).isEqualTo("belge1.pdf");
        assertThat(responses.get(1).getOriginalName()).isEqualTo("belge2.pdf");
        verify(fileStorageService, times(2)).store(any(), anyString());
        verify(fileRepository, times(2)).save(any(FileEntity.class));
    }

    @Test
    @DisplayName("M7: Bos dosya dizisi gonderildiginde BusinessRuleException firlatilir")
    void bosDosyaDizisiReddedilir() {
        assertThatThrownBy(() -> fileService().uploadFiles(new MultipartFile[]{}, RECORD_ID, UPLOADER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("En az bir dosya yüklenmelidir");

        verify(fileStorageService, never()).store(any(), anyString());
    }

    @Test
    @DisplayName("M7: Coklu yuklemede dosyalardan biri bos ise islem iptal edilir")
    void cokluYuklemedeBosDosyaVarsaReddedilir() {
        MultipartFile gecerli = dosya("gecerli.pdf");
        MultipartFile bos = new MockMultipartFile("file", "bos.pdf", "application/pdf", new byte[0]);

        MultipartFile[] files = new MultipartFile[]{gecerli, bos};

        when(fileContentValidator.detectAndValidate(gecerli)).thenReturn("application/pdf");

        assertThatThrownBy(() -> fileService().uploadFiles(files, RECORD_ID, UPLOADER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Boş dosya yüklenemez");

        verify(fileStorageService, never()).store(any(), anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("M7: Download ucunda dogru Content-Type ve attachment Content-Disposition doner")
    void downloadFileResponseHeadersKontrolu() {
        UUID fileId = UUID.randomUUID();
        FileEntity entity = new FileEntity();
        entity.setId(fileId);
        entity.setRecordId(RECORD_ID);
        entity.setOriginalName("ek.pdf");
        entity.setStoredName("stored-guid.pdf");
        entity.setMimeType("application/pdf");

        Record record = new Record();
        record.setId(RECORD_ID);
        record.setCreatedBy(UPLOADER_ID);
        record.setStatus(RecordStatus.TASLAK);

        when(fileRepository.findByIdAndDeletedAtIsNull(fileId)).thenReturn(Optional.of(entity));
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(fileStorageService.loadAsResource("stored-guid.pdf"))
                .thenReturn(new ByteArrayResource("pdf-content".getBytes()));

        ResponseEntity<Resource> response = fileService().downloadFile(fileId, visibility(RoleName.CALISAN, UPLOADER_ID));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("filename=\"ek.pdf\"");
    }

    @Test
    @DisplayName("M7: Preview ucunda dogru Content-Type ve inline Content-Disposition doner")
    void previewFileResponseHeadersKontrolu() {
        UUID fileId = UUID.randomUUID();
        FileEntity entity = new FileEntity();
        entity.setId(fileId);
        entity.setRecordId(RECORD_ID);
        entity.setOriginalName("onizleme.png");
        entity.setStoredName("stored-guid.png");
        entity.setMimeType("image/png");

        Record record = new Record();
        record.setId(RECORD_ID);
        record.setCreatedBy(UPLOADER_ID);
        record.setStatus(RecordStatus.TASLAK);

        when(fileRepository.findByIdAndDeletedAtIsNull(fileId)).thenReturn(Optional.of(entity));
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(fileStorageService.loadAsResource("stored-guid.png"))
                .thenReturn(new ByteArrayResource("image-content".getBytes()));

        ResponseEntity<Resource> response = fileService().previewFile(fileId, visibility(RoleName.CALISAN, UPLOADER_ID));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline")
                .contains("filename=\"onizleme.png\"");
    }
}