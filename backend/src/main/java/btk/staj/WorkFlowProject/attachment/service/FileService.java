package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.attachment.dto.FileResponseDto;
import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final FileContentValidator fileContentValidator;
    private final RecordLockValidator recordLockValidator;
    private final RecordRepository recordRepository;
    private final RecordAccessPolicy recordAccessPolicy;

    private void assertCanViewRecord(UUID recordId, RoleName role, UUID currentUserId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Kayıt bulunamadı: " + recordId));
        recordAccessPolicy.assertCanView(
                role, currentUserId, record.getCreatedBy(), record.getAssignedTo(), record.getStatus());
    }

    @Transactional
    public FileResponseDto uploadFile(MultipartFile file, UUID recordId, UUID uploadedBy) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("Dosya boş olamaz");
        }

        recordLockValidator.assertModifyAllowed(recordId, uploadedBy);

        String detectedType = fileContentValidator.detectAndValidate(file);
        String originalFilename = file.getOriginalFilename();
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

        FileEntity saved = fileRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void deleteFile(UUID id, UUID deletedBy) {
        FileEntity fileEntity = fileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dosya bulunamadı: " + id));

        recordLockValidator.assertModifyAllowed(fileEntity.getRecordId(), deletedBy);

        fileEntity.setDeletedAt(LocalDateTime.now());
        fileEntity.setDeletedBy(deletedBy);
        fileRepository.save(fileEntity);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadFile(UUID id, RoleName role, UUID currentUserId) {
        return buildFileResponse(id, "attachment", role, currentUserId);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> previewFile(UUID id, RoleName role, UUID currentUserId) {
        return buildFileResponse(id, "inline", role, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<FileResponseDto> listByRecord(UUID recordId, RoleName role, UUID currentUserId) {
        assertCanViewRecord(recordId, role, currentUserId);
        return fileRepository.findAllByRecordIdAndDeletedAtIsNull(recordId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ResponseEntity<Resource> buildFileResponse(UUID id, String dispositionType, RoleName role, UUID currentUserId) {
        FileEntity fileEntity = fileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dosya bulunamadı: " + id));

        assertCanViewRecord(fileEntity.getRecordId(), role, currentUserId);

        Resource resource = fileStorageService.loadAsResource(fileEntity.getStoredName());

        ContentDisposition contentDisposition = ContentDisposition
                .builder(dispositionType)
                .filename(fileEntity.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    public FileResponseDto toDto(FileEntity entity) {
        return new FileResponseDto(
                entity.getId(),
                entity.getRecordId(),
                entity.getOriginalName(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getUploadedBy(),
                entity.getUploadedAt()
        );
    }
}