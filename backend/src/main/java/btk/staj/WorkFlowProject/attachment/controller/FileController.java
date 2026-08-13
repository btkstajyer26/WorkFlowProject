package btk.staj.WorkFlowProject.attachment.controller;

import btk.staj.WorkFlowProject.attachment.dto.FileResponseDto;
import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.service.FileService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // YENİ UÇ ADRESİ: POST /api/records/{id}/files
    @PreAuthorize("hasRole('CALISAN')")
    @PostMapping(value = "/api/records/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @PathVariable("id") UUID recordId, // @RequestParam yerine @PathVariable yapıldı
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        try {
            FileEntity saved = fileService.uploadFile(file, recordId, currentUser.getId());
            FileResponseDto response = toDto(saved);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // İndirme adresi aynen korundu: GET /api/files/{id}/download
    @GetMapping("/api/files/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        return fileService.downloadFile(id);
    }

    // Önizleme adresi aynen korundu: GET /api/files/{id}/preview
    @GetMapping("/api/files/{id}/preview")
    public ResponseEntity<Resource> previewFile(@PathVariable UUID id) {
        return fileService.previewFile(id);
    }

    // Silme adresi aynen korundu: DELETE /api/files/{id}
    @PreAuthorize("hasRole('CALISAN')")
    @DeleteMapping("/api/files/{id}")
    public ResponseEntity<?> deleteFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        try {
            fileService.deleteFile(id, currentUser.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    private FileResponseDto toDto(FileEntity entity) {
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