package btk.staj.WorkFlowProject.attachment.controller;

import btk.staj.WorkFlowProject.attachment.dto.FileResponseDto;
import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.service.FileService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PreAuthorize("hasRole('CALISAN')")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("recordId") UUID recordId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        try {
            FileEntity saved = fileService.uploadFile(file, recordId, currentUser.getId());
            FileResponseDto response = toDto(saved);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        return fileService.downloadFile(id);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewFile(@PathVariable UUID id) {
        return fileService.previewFile(id);
    }

    @PreAuthorize("hasRole('CALISAN')")
    @DeleteMapping("/{id}")
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