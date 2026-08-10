package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution.DataIntegrityReason;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the workflow target dictated by an action without applying transition
 * validation. Role and active-state checks deliberately remain the state
 * machine's responsibility.
 */
public final class TargetUserResolver {

    private final WorkflowUserPort userPort;

    public TargetUserResolver(WorkflowUserPort userPort) {
        this.userPort = Objects.requireNonNull(userPort, "userPort");
    }

    public TargetResolution resolve(
            WorkflowAction action,
            UUID requestedTargetUserId,
            WorkflowRecordSnapshot record) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(record, "record");

        return switch (action) {
            case GONDER, TEKRAR_GONDER -> resolveRequestedTarget(requestedTargetUserId);
            case BASKANA_ILET -> resolveSingleActiveRole(RoleName.BASKAN);
            case CALISANA_GERI_GONDER -> resolveCreatedBy(record.createdBy());
            case BASKAN_YARDIMCISINA_GERI_GONDER -> resolveLastDeputy(record.lastDeputyId());
            case ONAYLA, REDDET -> new TargetResolution.NotProvided();
        };
    }

    private TargetResolution resolveRequestedTarget(UUID requestedTargetUserId) {
        if (requestedTargetUserId == null) {
            return new TargetResolution.NotProvided();
        }

        Optional<WorkflowUserSnapshot> user = findById(requestedTargetUserId);
        if (user.isEmpty()) {
            return new TargetResolution.RequestTargetNotFound(requestedTargetUserId);
        }
        return new TargetResolution.Resolved(user.get());
    }

    private TargetResolution resolveSingleActiveRole(RoleName role) {
        List<WorkflowUserSnapshot> activeUsers = Objects.requireNonNull(
                userPort.findActiveByRole(role),
                "userPort.findActiveByRole(role)");

        if (activeUsers.size() != 1) {
            return new TargetResolution.RoleNotConfigured(role, activeUsers.size());
        }
        return new TargetResolution.Resolved(activeUsers.getFirst());
    }

    private TargetResolution resolveCreatedBy(UUID createdBy) {
        Optional<WorkflowUserSnapshot> user = findById(createdBy);
        if (user.isEmpty()) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.CREATED_BY_USER_NOT_FOUND,
                    createdBy);
        }
        return new TargetResolution.Resolved(user.get());
    }

    private TargetResolution resolveLastDeputy(UUID lastDeputyId) {
        if (lastDeputyId == null) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.LAST_DEPUTY_ID_MISSING,
                    null);
        }

        Optional<WorkflowUserSnapshot> user = findById(lastDeputyId);
        if (user.isEmpty()) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.LAST_DEPUTY_USER_NOT_FOUND,
                    lastDeputyId);
        }
        return new TargetResolution.Resolved(user.get());
    }

    private Optional<WorkflowUserSnapshot> findById(UUID userId) {
        return Objects.requireNonNull(userPort.findById(userId), "userPort.findById(userId)");
    }
}
