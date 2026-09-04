package btk.staj.WorkFlowProject.workflow.exception;

/** Stable business reasons for the WF-8 service and the future AP-8 HTTP adapter. */
public class WorkflowBindingException extends RuntimeException {
    public enum Reason {
        INVALID_ID, TEMPLATE_NOT_FOUND, BINDING_NOT_FOUND, ROLE_NOT_FOUND,
        INVALID_TEMPLATE, INVALID_ROLE, MISSING_ROLE_PERMISSION,
        DUPLICATE_BINDING, METADATA_MISMATCH, PROTECTED_BINDING, BINDING_IN_USE
    }

    private final Reason reason;

    public WorkflowBindingException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() { return reason; }
    public String code() { return "WORKFLOW_BINDING_" + reason.name(); }
}
