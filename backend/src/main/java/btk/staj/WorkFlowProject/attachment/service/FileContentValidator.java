package btk.staj.WorkFlowProject.attachment.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class FileContentValidator {

    private static final Tika TIKA = new Tika();

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    public String detectAndValidate(MultipartFile file) {
        String detectedType;
        try {
            detectedType = TIKA.detect(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Dosya içeriği okunamadı");
        }

        if (!ALLOWED_TYPES.contains(detectedType)) {
            throw new IllegalArgumentException("Desteklenmeyen dosya formatı: " + detectedType);
        }

        return detectedType;
    }
}