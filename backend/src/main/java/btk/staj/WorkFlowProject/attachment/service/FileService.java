package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    public FileEntity uploadFile(MultipartFile file, UUID recordId, UUID uploadedBy) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Desteklenmeyen dosya formatı: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;

        fileStorageService.store(file, storedFilename);

        FileEntity entity = new FileEntity();
        entity.setRecordId(recordId);
        entity.setOriginalName(originalFilename);
        entity.setStoredName(storedFilename);
        entity.setMimeType(contentType);
        entity.setFileSize((int) file.getSize());
        entity.setUploadedBy(uploadedBy);
        entity.setUploadedAt(java.time.LocalDateTime.now());

        return fileRepository.save(entity);
    }

    public ResponseEntity<Resource> downloadFile(UUID id) {
        return buildFileResponse(id, "attachment");
    }

    public ResponseEntity<Resource> previewFile(UUID id) {
        return buildFileResponse(id, "inline");
    }

    private ResponseEntity<Resource> buildFileResponse(UUID id, String dispositionType) {

        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı: " + id));

        Resource resource = fileStorageService.loadAsResource(fileEntity.getStoredName());

        String contentDisposition = dispositionType + "; filename=\"" + fileEntity.getOriginalName() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }
}