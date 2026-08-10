package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID UPLOADER_ID = UUID.randomUUID();

    private FileService fileService() {
        return new FileService(fileRepository, fileStorageService);
    }

    private String storedNameFor(String originalName, String contentType) {
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(call -> call.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", originalName, contentType, "icerik".getBytes());

        fileService().uploadFile(file, RECORD_ID, UPLOADER_ID);

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).store(any(), storedName.capture());
        return storedName.getValue();
    }

    @Test
    @DisplayName("diskteki ad yalnizca GUID'den uretilir, orijinal ad karismaz")
    void diskAdiSadeceGuidOlur() {
        String storedName = storedNameFor("Butce Raporu 2026.pdf", "application/pdf");

        assertThat(storedName).doesNotContain("Butce", "Raporu", " ");
        assertThat(storedName).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf");
    }

    @Test
    @DisplayName("dosya adindaki dizin asimi denemesi diskteki ada tasinmaz")
    void dizinAsimiDenemesiAdaTasinmaz() {
        String storedName = storedNameFor("../../../etc/passwd.pdf", "application/pdf");

        assertThat(storedName).doesNotContain("..", "/", "\\");
    }

    @ParameterizedTest(name = "{1} -> {2}")
    @DisplayName("sartnamedeki formatlar kabul edilir ve uzanti MIME'dan turetilir")
    @CsvSource({
            "rapor.pdf,  application/pdf,                                                            .pdf",
            "yazi.doc,   application/msword,                                                         .doc",
            "yazi.docx,  application/vnd.openxmlformats-officedocument.wordprocessingml.document,    .docx",
            "tablo.xls,  application/vnd.ms-excel,                                                   .xls",
            "tablo.xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,          .xlsx",
            "resim.png,  image/png,                                                                  .png",
            "resim.jpg,  image/jpeg,                                                                 .jpg"
    })
    void desteklenenFormatlarKabulEdilir(String originalName, String contentType, String expectedExtension) {
        assertThat(storedNameFor(originalName, contentType)).endsWith(expectedExtension);
    }

    @Test
    @DisplayName("desteklenmeyen format reddedilir ve diske hicbir sey yazilmaz")
    void desteklenmeyenFormatReddedilir() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "zararli.exe", "application/x-msdownload", "icerik".getBytes());

        assertThatThrownBy(() -> fileService().uploadFile(file, RECORD_ID, UPLOADER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Desteklenmeyen dosya formatı");

        verify(fileStorageService, never()).store(any(), anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("orijinal ad veritabaninda korunur")
    void orijinalAdVeritabaninaYazilir() {
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(call -> call.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "Butce Raporu 2026.pdf", "application/pdf", "icerik".getBytes());

        FileEntity saved = fileService().uploadFile(file, RECORD_ID, UPLOADER_ID);

        assertThat(saved.getOriginalName()).isEqualTo("Butce Raporu 2026.pdf");
        assertThat(saved.getMimeType()).isEqualTo("application/pdf");
        assertThat(saved.getStoredName()).isNotEqualTo(saved.getOriginalName());
    }
}
