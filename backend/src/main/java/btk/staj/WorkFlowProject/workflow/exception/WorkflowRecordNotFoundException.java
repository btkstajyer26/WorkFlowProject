package btk.staj.WorkFlowProject.workflow.exception;

import java.util.Objects;
import java.util.UUID;

/** Record absence exposed separately for future common HTTP error mapping. */
public class WorkflowRecordNotFoundException extends RuntimeException {

    private final UUID recordId;

    public WorkflowRecordNotFoundException(UUID recordId) {
        super("Workflow record not found: " + Objects.requireNonNull(recordId, "recordId"));
        this.recordId = recordId;
    }

    public UUID recordId() {
        return recordId;
    }
}
