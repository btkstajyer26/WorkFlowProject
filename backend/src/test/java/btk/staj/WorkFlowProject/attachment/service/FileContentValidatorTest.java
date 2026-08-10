package btk.staj.WorkFlowProject.attachment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dosya icerik dogrulama")
class FileContentValidatorTest {

    private final FileContentValidator validator = new FileContentValidator();

    /** PDF magic byte'lari ile baslayan gecerli bir govde. */
    private static byte[] pdfBytes() {
        return "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes();
    }

    private static byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    }

    /** OOXML dosyalari birer ZIP'tir; icerik olarak gecerli bir zip uretilir. */
    private static byte[] ooxmlBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes());
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private MockMultipartFile dosya(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    @Test
    @DisplayName("PDF icerigi taninir")
    void pdfTanininir() {
        String type = validator.detectAndValidate(dosya("rapor.pdf", pdfBytes()));

        assertThat(type).isEqualTo("application/pdf");
        assertThat(validator.extensionFor(type)).isEqualTo(".pdf");
    }

    @Test
    @DisplayName("PNG icerigi taninir")
    void pngTanininir() {
        String type = validator.detectAndValidate(dosya("resim.png", pngBytes()));

        assertThat(type).isEqualTo("image/png");
        assertThat(validator.extensionFor(type)).isEqualTo(".png");
    }

    @Test
    @DisplayName("docx kabul edilir (tika-core container tespiti yapamadigi icin ad ipucu gerekir)")
    void docxKabulEdilir() throws IOException {
        String type = validator.detectAndValidate(dosya("yazi.docx", ooxmlBytes()));

        assertThat(type)
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(validator.extensionFor(type)).isEqualTo(".docx");
    }

    @Test
    @DisplayName("xlsx kabul edilir")
    void xlsxKabulEdilir() throws IOException {
        String type = validator.detectAndValidate(dosya("tablo.xlsx", ooxmlBytes()));

        assertThat(type)
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(validator.extensionFor(type)).isEqualTo(".xlsx");
    }

    @Test
    @DisplayName("uzantisi degistirilmis calistirilabilir dosya reddedilir")
    void uzantisiDegistirilmisDosyaReddedilir() {
        // Windows PE basligi; adi .pdf yapilsa bile icerikten yakalanmali.
        byte[] exeBytes = new byte[]{'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};

        assertThatThrownBy(() -> validator.detectAndValidate(dosya("rapor.pdf", exeBytes)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Desteklenmeyen dosya formatı");
    }

    @Test
    @DisplayName("duz metin reddedilir")
    void duzMetinReddedilir() {
        assertThatThrownBy(() -> validator.detectAndValidate(dosya("not.txt", "merhaba".getBytes())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Desteklenmeyen dosya formatı");
    }
}
