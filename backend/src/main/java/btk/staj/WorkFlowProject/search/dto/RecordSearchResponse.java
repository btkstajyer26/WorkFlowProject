package btk.staj.WorkFlowProject.search.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class RecordSearchResponse {

    private UUID id;

    private String title;

    private String description;

    private Integer categoryId;

    private RecordStatus status;

    private UUID createdBy;

    /**
     * Olusturan kisinin gorunur adi. Normal kullanicilar baska kullanicilari
     * cozebilecekleri bir uca sahip olmadigi icin ad listeyle birlikte gelir
     * (sozlesme §5); istemci yalnizca UUID alsaydi adi gostermek icin denetim
     * izini tarardi ve gecmisi kirpilan roller yanlis ad gorurdu.
     */
    private String createdByFullName;

    private UUID assignedTo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public RecordSearchResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByFullName() {
        return createdByFullName;
    }

    public void setCreatedByFullName(String createdByFullName) {
        this.createdByFullName = createdByFullName;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}