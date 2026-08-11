package btk.staj.WorkFlowProject.audit.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID recordId;
    private UUID userId;
    private Integer roleId;
    private String action;
    private String previousStatus;
    private String newStatus;
    private String comment;
    private LocalDateTime createdAt;
}