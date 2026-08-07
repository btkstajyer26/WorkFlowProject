package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;

import java.util.Objects;
import java.util.UUID;

/** Complete, persistence-neutral outcome of resolving a workflow target. */
public sealed interface TargetResolution permits
        TargetResolution.Resolved,
        TargetResolution.NotProvided,
        TargetResolution.RequestTargetNotFound,
        TargetResolution.RoleNotConfigured,
        TargetResolution.DataIntegrityFailure {

    record Resolved(WorkflowUserSnapshot user) implements TargetResolution {

        public Resolved {
            Objects.requireNonNull(user, "user");
        }
    }

    record NotProvided() implements TargetResolution {
    }

    record RequestTargetNotFound(UUID requestedUserId) implements TargetResolution {

        public RequestTargetNotFound {
            Objects.requireNonNull(requestedUserId, "requestedUserId");
        }
    }

    record RoleNotConfigured(RoleName role, int activeUserCount) implements TargetResolution {

        public RoleNotConfigured {
            Objects.requireNonNull(role, "role");
        }
    }

    record DataIntegrityFailure(DataIntegrityReason reason, UUID referencedUserId) implements TargetResolution {

        public DataIntegrityFailure {
            Objects.requireNonNull(reason, "reason");
            if (reason != DataIntegrityReason.LAST_DEPUTY_ID_MISSING) {
                Objects.requireNonNull(referencedUserId, "referencedUserId");
            }
        }
    }

    enum DataIntegrityReason {
        LAST_DEPUTY_ID_MISSING,
        CREATED_BY_USER_NOT_FOUND,
        LAST_DEPUTY_USER_NOT_FOUND
    }
}
