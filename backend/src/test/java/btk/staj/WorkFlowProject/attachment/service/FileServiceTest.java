package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dosya yukleme")
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileContentValidator fileContentValidator;

    @Mock
    private RecordLockValidator recordLockValidator;

    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID UPLOADER_ID = UUID.randomUUID();

    private FileService fileService() {
        return new FileService(fileRepository, fileStorageService, fileContentValidator, recordLockValidator);
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
                .thenThrow(new IllegalArgumentException("Desteklenmeyen dosya formatı: application/x-msdownload"));

        assertThatThrownBy(() -> fileService().uploadFile(dosya("zararli.exe"), RECORD_ID, UPLOADER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Desteklenmeyen dosya formatı");

        verify(fileStorageService, never()).store(any(), anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("kilitli kayda dosya eklenemez")
    void kilitliKaydaEklenemez() {
        doThrow(new IllegalArgumentException("Bu kayıt 'ONAYLANDI' durumunda, dosya eklenemez"))
                .when(recordLockValidator).assertUploadAllowed(RECORD_ID);

        assertThatThrownBy(() -> fileService().uploadFile(dosya("rapor.pdf"), RECORD_ID, UPLOADER_ID))
                .isInstanceOf(IllegalArgumentException.class)
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

        FileEntity saved = fileService().uploadFile(dosya("Butce Raporu 2026.pdf"), RECORD_ID, UPLOADER_ID);

        assertThat(saved.getOriginalName()).isEqualTo("Butce Raporu 2026.pdf");
        assertThat(saved.getMimeType()).isEqualTo("application/pdf");
        assertThat(saved.getStoredName()).isNotEqualTo(saved.getOriginalName());
    }
}
