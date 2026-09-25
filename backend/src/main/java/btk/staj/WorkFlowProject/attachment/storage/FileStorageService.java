package btk.staj.WorkFlowProject.attachment.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public void store(MultipartFile file, String storedFilename) {
        try {
            Path uploadPath = uploadRoot();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path targetPath = resolveWithinUploadDir(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Dosya kaydedilirken hata oluştu: " + e.getMessage());
        }
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path filePath = resolveWithinUploadDir(storedFilename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new IllegalArgumentException("Dosya diskte bulunamadı: " + storedFilename);
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Dosya yolu hatalı: " + e.getMessage());
        }
    }

    /**
     * Diske yazilmis dosyayi kaliciyi olarak siler (R06).
     *
     * <p>Yalnizca transaction geri alindiginda, ayni transaction icinde yazilmis
     * dosyalari temizlemek icin kullanilir. Kullanicinin sildigi ekler soft-delete
     * ile yonetilir ve diskte kalir: dondurulmus gorunum onlari hala acabilmelidir.
     *
     * <p>Silme basarisiz olursa istisna firlatilmaz; geri alma yolunda ikinci bir
     * hata uretmek, asil hatanin ustunu ortmekten baska ise yaramaz.
     */
    public void delete(String storedFilename) {
        try {
            Files.deleteIfExists(resolveWithinUploadDir(storedFilename));
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Geri alma sonrasi dosya silinemedi: {} ({})", storedFilename, e.getMessage());
        }
    }

    private Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Cozulen yolun yukleme dizininin disina cikmadigini dogrular. Cagiran taraf
     * zaten GUID uretiyor; bu kontrol, ileride ada kullanici girdisi karisirsa
     * dizin asimini engelleyen ikinci katmandir.
     */
    private Path resolveWithinUploadDir(String storedFilename) {
        Path root = uploadRoot();
        Path target = root.resolve(storedFilename).normalize();

        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Geçersiz dosya adı: " + storedFilename);
        }

        return target;
    }
}