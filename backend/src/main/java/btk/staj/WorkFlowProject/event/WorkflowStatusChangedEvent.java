package btk.staj.WorkFlowProject.event;

import java.util.UUID;

public class WorkflowStatusChangedEvent {
    private final UUID recordId;
    private final String userEmail;
    private final String recipientName;
    private final String title;
    private final String status;
    private final String reason;

    public WorkflowStatusChangedEvent(UUID recordId, String userEmail, String recipientName, String title, String status, String reason) {
        this.recordId = recordId;
        this.userEmail = userEmail;
        this.recipientName = recipientName;
        this.title = title;
        this.status = status;
        this.reason = reason;
    }

    // Getters
    public UUID getRecordId() { return recordId; }
    public String getUserEmail() { return userEmail; }
    public String getRecipientName() { return recipientName; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
}