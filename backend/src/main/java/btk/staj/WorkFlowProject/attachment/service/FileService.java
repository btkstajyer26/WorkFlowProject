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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    /**
     * Sartnamedeki desteklenen formatlar. Ayni tablo hem izin listesi olarak hem de
     * diskteki uzantinin kaynagi olarak kullanilir; uzanti kullanicinin gonderdigi
     * dosya adindan turetilmez.
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

    @Transactional
    public FileEntity uploadFile(MultipartFile file, UUID recordId, UUID uploadedBy) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Desteklenmeyen dosya formatı: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();

        // Diskteki ad yalnizca GUID'den uretilir. Kullanicinin gonderdigi dosya adi
        // yola hic karismaz; yalnizca veritabaninda saklanip indirmede geri verilir.
        String storedFilename = UUID.randomUUID() + extension;

        fileStorageService.store(file, storedFilename);

        FileEntity entity = new FileEntity();
        entity.setRecordId(recordId);
        entity.setOriginalName(originalFilename);
        entity.setStoredName(storedFilename);
        entity.setMimeType(contentType);
        entity.setFileSize((int) file.getSize());
        entity.setUploadedBy(uploadedBy);
        entity.setUploadedAt(LocalDateTime.now());

        return fileRepository.save(entity);
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

        FileEntity fileEntity = fileRepository.findById(id)
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