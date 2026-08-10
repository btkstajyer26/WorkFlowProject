package btk.staj.WorkFlowProject.attachment.service;

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
     * Dosyanin gercek turunu icerigine bakarak belirler ve izin listesine gore dogrular.
     *
     * <p>Tespit dosya adi ipucuyla birlikte yapilir: tika-core tek basina container
     * tespiti yapamadigi icin .docx/.xlsx yalnizca magic byte'lara bakildiginda
     * {@code application/zip}, .doc/.xls ise {@code application/x-tika-msoffice} olarak
     * gorunur. Ad ipucu bu turleri kendi alt turlerine indirger. Ipucu guvenligi
     * zayiflatmaz: Tika, ad ipucunu yalnizca icerikten bulunan turun bir alt turuyse
     * dikkate alir; uzantisi .docx yapilmis bir .exe yine icerik turuyle reddedilir.
     */
    public String detectAndValidate(MultipartFile file) {
        String detectedType;
        try {
            detectedType = TIKA.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new IllegalArgumentException("Dosya içeriği okunamadı");
        }

        if (!ALLOWED_TYPES.containsKey(detectedType)) {
            throw new IllegalArgumentException("Desteklenmeyen dosya formatı: " + detectedType);
        }

        return detectedType;
    }

    /** Dogrulanmis MIME turune karsilik gelen dosya uzantisi. */
    public String extensionFor(String mimeType) {
        String extension = ALLOWED_TYPES.get(mimeType);
        if (extension == null) {
            throw new IllegalArgumentException("Desteklenmeyen dosya formatı: " + mimeType);
        }
        return extension;
    }
}
