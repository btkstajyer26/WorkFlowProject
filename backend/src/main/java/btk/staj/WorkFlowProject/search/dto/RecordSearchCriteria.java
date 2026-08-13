package btk.staj.WorkFlowProject.search.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import java.time.LocalDateTime;

public class RecordSearchCriteria {

    private String q;
    private RecordStatus status;
    private Integer categoryId;
    private LocalDateTime from;
    private LocalDateTime to;

    public RecordSearchCriteria() {
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
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

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }
}