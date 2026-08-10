package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final FileContentValidator fileContentValidator;
    private final RecordLockValidator recordLockValidator;

    @Transactional
    public FileEntity uploadFile(MultipartFile file, UUID recordId, UUID uploadedBy) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }

        recordLockValidator.assertUploadAllowed(recordId);

        // Tur, istemcinin gonderdigi Content-Type'a degil dosya icerigine bakilarak belirlenir.
        String detectedType = fileContentValidator.detectAndValidate(file);

        String originalFilename = file.getOriginalFilename();

        // Diskteki ad yalnizca GUID'den uretilir; uzanti da dogrulanmis turden gelir.
        // Kullanicinin gonderdigi dosya adi yola hic karismaz, yalnizca veritabaninda
        // saklanip indirmede geri verilir.
        String storedFilename = UUID.randomUUID() + fileContentValidator.extensionFor(detectedType);

        fileStorageService.store(file, storedFilename);

        FileEntity entity = new FileEntity();
        entity.setRecordId(recordId);
        entity.setOriginalName(originalFilename);
        entity.setStoredName(storedFilename);
        entity.setMimeType(detectedType);
        entity.setFileSize((int) file.getSize());
        entity.setUploadedBy(uploadedBy);
        entity.setUploadedAt(LocalDateTime.now());

        return fileRepository.save(entity);
    }

    @Transactional
    public void deleteFile(UUID id, UUID deletedBy) {
        FileEntity fileEntity = fileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı: " + id));

        fileEntity.setDeletedAt(LocalDateTime.now());
        fileEntity.setDeletedBy(deletedBy);

        fileRepository.save(fileEntity);
        // Fiziksel dosya diskten silinmiyor - soft delete'in amacı geri dönüşü mümkün kılmak.
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadFile(UUID id) {
        return buildFileResponse(id, "attachment");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> previewFile(UUID id) {
        return buildFileResponse(id, "inline");
    }

    private ResponseEntity<Resource> buildFileResponse(UUID id, String dispositionType) {

        FileEntity fileEntity = fileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı: " + id));

        Resource resource = fileStorageService.loadAsResource(fileEntity.getStoredName());

        // Orijinal ad kullanicidan geldigi icin header'a elle birlestirilmez;
        // Spring'in kodlayicisi tirnak ve satir sonu gibi karakterleri guvenli hale getirir.
        ContentDisposition contentDisposition = ContentDisposition
                .builder(dispositionType)
                .filename(fileEntity.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }
}
