package btk.staj.WorkFlowProject.subtask.exception;

import java.util.Objects;

/** Stable business errors raised while managing a Parent record's Subtasks. */
public final class SubtaskException extends RuntimeException {

    public enum Reason {
        PARENT_NOT_FOUND,
        PARENT_STATUS_INVALID,
        ALREADY_SPLIT,
        COUNT_TOO_LOW,
        INVALID_SUBTASK,
        DUPLICATE_ASSIGNEE,
        ASSIGNEE_NOT_FOUND,
        ASSIGNEE_INACTIVE,
        ASSIGNEE_SYSTEM,
        INVALID_APPROVAL_POLICY,
        INVALID_REQUIRED_APPROVALS,
        NOT_FOUND,
        ACTION_FORBIDDEN,
        INVALID_TRANSITION,
        REJECTION_COMMENT_REQUIRED,
        TERMINAL
    }

    private final Reason reason;

    public SubtaskException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }

    public String code() {
        return "SUBTASK_" + reason.name();
    }
}
