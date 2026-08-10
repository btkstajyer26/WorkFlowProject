package btk.staj.WorkFlowProject.record.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RecordSummaryResponse {
    
    private UUID id;
    private String title;
    private Integer categoryId;
    private RecordStatus status;
    private LocalDateTime createdAt;
    
}