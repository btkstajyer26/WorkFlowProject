package btk.staj.WorkFlowProject.workflow.exception;

import btk.staj.WorkFlowProject.workflow.model.TargetResolution.DataIntegrityReason;

import java.util.Objects;
import java.util.UUID;

/** Internal workflow inconsistency without a newly invented public API error code. */
public class WorkflowDataIntegrityException extends RuntimeException {

    private final DataIntegrityReason reason;
    private final UUID referencedUserId;

    public WorkflowDataIntegrityException(DataIntegrityReason reason, UUID referencedUserId) {
        super(message(reason, referencedUserId));
        this.reason = reason;
        this.referencedUserId = referencedUserId;
    }

    public DataIntegrityReason reason() {
        return reason;
    }

    public UUID referencedUserId() {
        return referencedUserId;
    }

    private static String message(DataIntegrityReason reason, UUID referencedUserId) {
        Objects.requireNonNull(reason, "reason");
        if (reason != DataIntegrityReason.LAST_DEPUTY_ID_MISSING) {
            Objects.requireNonNull(referencedUserId, "referencedUserId");
        }
        return "Workflow data integrity failure: " + reason
                + (referencedUserId == null ? "" : " (userId=" + referencedUserId + ")");
    }
}
