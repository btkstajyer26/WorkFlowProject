package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final FileContentValidator fileContentValidator;
    private final RecordLockValidator recordLockValidator; // yeni eklendi

    public FileEntity uploadFile(MultipartFile file, UUID recordId, UUID uploadedBy) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }

        recordLockValidator.assertUploadAllowed(recordId); // TODO yerine geldi

        String detectedType = fileContentValidator.detectAndValidate(file);

        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;

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

    public void deleteFile(UUID id, UUID deletedBy) {
        FileEntity fileEntity = fileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı: " + id));

        fileEntity.setDeletedAt(LocalDateTime.now());
        fileEntity.setDeletedBy(deletedBy);

        fileRepository.save(fileEntity);
        // Fiziksel dosya diskten silinmiyor - soft delete'in amacı geri dönüşü mümkün kılmak.
    }

    public ResponseEntity<Resource> downloadFile(UUID id) {
        return buildFileResponse(id, "attachment");
    }

    public ResponseEntity<Resource> previewFile(UUID id) {
        return buildFileResponse(id, "inline");
    }

    private ResponseEntity<Resource> buildFileResponse(UUID id, String dispositionType) {

        FileEntity fileEntity = fileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı: " + id));

        Resource resource = fileStorageService.loadAsResource(fileEntity.getStoredName());

        String contentDisposition = dispositionType + "; filename=\"" + fileEntity.getOriginalName() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }
}