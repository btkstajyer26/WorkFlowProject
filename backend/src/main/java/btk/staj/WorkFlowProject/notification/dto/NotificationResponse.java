package btk.staj.WorkFlowProject.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationResponse {
    private UUID id;
    private UUID recordId;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;

    // Constructor, Getter ve Setter metotları...
}