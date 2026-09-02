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
            // Referans kolonun kendisi bossa gosterilecek bir kullanici kimligi de yoktur;
            // diger butun sebeplerde kimlik zorunludur.
            if (!reason.referenceIsMissing()) {
                Objects.requireNonNull(referencedUserId, "referencedUserId");
            }
        }
    }

    enum DataIntegrityReason {

        /** {@code records.last_deputy_id} bos. */
        LAST_DEPUTY_ID_MISSING(true),

        /** {@code records.assigned_to} bos. */
        CURRENT_ASSIGNEE_MISSING(true),

        /** {@code records.created_by} dolu ama kullanici bulunamadi. */
        CREATED_BY_USER_NOT_FOUND(false),

        /** {@code records.last_deputy_id} dolu ama kullanici bulunamadi. */
        LAST_DEPUTY_USER_NOT_FOUND(false),

        /** {@code records.assigned_to} dolu ama kullanici bulunamadi. */
        CURRENT_ASSIGNEE_USER_NOT_FOUND(false);

        private final boolean referenceIsMissing;

        DataIntegrityReason(boolean referenceIsMissing) {
            this.referenceIsMissing = referenceIsMissing;
        }

        /** Referans kolonun kendisinin bos oldugu sebepleri isaretler. */
        public boolean referenceIsMissing() {
            return referenceIsMissing;
        }
    }
}
