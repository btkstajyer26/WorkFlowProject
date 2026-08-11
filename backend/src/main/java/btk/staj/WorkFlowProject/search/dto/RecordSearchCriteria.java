package btk.staj.WorkFlowProject.search.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class RecordSearchCriteria {

    private String text;

    private RecordStatus status;

    private Integer categoryId;

    private UUID userId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    public RecordSearchCriteria() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}