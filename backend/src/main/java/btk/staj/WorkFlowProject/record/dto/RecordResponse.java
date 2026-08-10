package btk.staj.WorkFlowProject.record.dto;


import java.time.LocalDateTime;
import java.util.UUID;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

public record RecordResponse(
    UUID id,
    String title,
    String description,
    Integer categoryId,
    RecordStatus status,
    LocalDateTime createdAt
) {}