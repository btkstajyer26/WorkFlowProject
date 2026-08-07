package btk.staj.WorkFlowProject.attachment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class FileResponseDto {

    private UUID id;
    private UUID recordId;
    private String originalName;
    private String mimeType;
    private int fileSize;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;

    public FileResponseDto() {
    }

    public FileResponseDto(UUID id, UUID recordId, String originalName, String mimeType,
                           int fileSize, UUID uploadedBy, LocalDateTime uploadedAt) {
        this.id = id;
        this.recordId = recordId;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}