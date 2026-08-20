package btk.staj.WorkFlowProject.attachment.controller;

import btk.staj.WorkFlowProject.attachment.dto.FileResponseDto;
import btk.staj.WorkFlowProject.attachment.service.FileService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PreAuthorize("hasRole('CALISAN')")
    @PostMapping(value = "/api/records/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileResponseDto>> uploadFiles(
            @PathVariable("id") UUID recordId,
            @RequestPart("file") MultipartFile[] files,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        List<FileResponseDto> response = fileService.uploadFiles(files, recordId, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/records/{id}/files")
    public ResponseEntity<List<FileResponseDto>> listFiles(
            @PathVariable("id") UUID recordId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        RoleName role = RoleName.valueOf(currentUser.getRoleName());
        List<FileResponseDto> files = fileService.listByRecord(recordId, role, currentUser.getId());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/api/files/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        RoleName role = RoleName.valueOf(currentUser.getRoleName());
        return fileService.downloadFile(id, role, currentUser.getId());
    }

    @GetMapping("/api/files/{id}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        RoleName role = RoleName.valueOf(currentUser.getRoleName());
        return fileService.previewFile(id, role, currentUser.getId());
    }

    @PreAuthorize("hasRole('CALISAN')")
    @DeleteMapping("/api/files/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        fileService.deleteFile(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}