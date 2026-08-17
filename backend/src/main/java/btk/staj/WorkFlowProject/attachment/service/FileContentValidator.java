package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
public class FileContentValidator {

    private static final Tika TIKA = new Tika();

    /**
     * Sartnamedeki desteklenen formatlar. Ayni tablo hem izin listesi hem de diskteki
     * uzantinin kaynagidir; uzanti kullanicinin gonderdigi dosya adindan turetilmez.
     */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", ".pdf",
            "application/msword", ".doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            "application/vnd.ms-excel", ".xls",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx",
            "image/png", ".png",
            "image/jpeg", ".jpg"
    );

    /**
     * Dosyanin gercek turunu icerigine bakarak belirler, izin listesine gore dogrular
     * ve dosya uzantisi ile gercek MIME tipinin tutarliligini kontrol eder.
     */
    public String detectAndValidate(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessRuleException("Dosya adı boş olamaz.");
        }

        String detectedType;
        try {
            detectedType = TIKA.detect(file.getInputStream(), originalFilename);
        } catch (IOException e) {
            throw new BusinessRuleException("Dosya içeriği okunamadı.");
        }

        // 1. İçerik tipi izin verilen listede mi?
        if (!ALLOWED_TYPES.containsKey(detectedType)) {
            throw new BusinessRuleException("Desteklenmeyen dosya formatı: " + detectedType);
        }

        // 2. Dosya uzantısı ile tespit edilen içerik tipi birbiriyle uyuşuyor mu?
        String expectedExtension = ALLOWED_TYPES.get(detectedType);
        String lowerFilename = originalFilename.toLowerCase();

        // JPEG için hem .jpg hem .jpeg geçerli kabul edilir
        boolean matchesExtension = lowerFilename.endsWith(expectedExtension)
                || (detectedType.equals("image/jpeg") && lowerFilename.endsWith(".jpeg"));

        if (!matchesExtension) {
            throw new BusinessRuleException(
                    "Dosya uzantısı ile gerçek dosya türü uyuşmuyor. Beklenen uzantı: " + expectedExtension
            );
        }

        return detectedType;
    }

    /** Dogrulanmis MIME turune karsilik gelen dosya uzantisi. */
    public String extensionFor(String mimeType) {
        String extension = ALLOWED_TYPES.get(mimeType);
        if (extension == null) {
            throw new BusinessRuleException("Desteklenmeyen dosya formatı: " + mimeType);
        }
        return extension;
    }
}