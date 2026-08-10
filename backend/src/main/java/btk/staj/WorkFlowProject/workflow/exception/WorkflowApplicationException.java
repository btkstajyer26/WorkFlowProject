package btk.staj.WorkFlowProject.workflow.exception;

import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;

import java.util.Objects;

/** Application exception that preserves a state-machine workflow error code. */
public class WorkflowApplicationException extends RuntimeException {

    private final WorkflowErrorCode errorCode;

    public WorkflowApplicationException(WorkflowErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode").name());
        this.errorCode = errorCode;
    }

    public WorkflowErrorCode errorCode() {
        return errorCode;
    }
}
