package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionResponse;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowDataIntegrityException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowRecordNotFoundException;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;
import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.model.WorkflowTransitionAudit;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.port.WorkflowEventPublisher;
import btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionContext;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionDecision;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowTransitionValidator;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates a workflow action without depending on persistence, security, or
 * messaging implementations.
 *
 * <p>This class deliberately is not a Spring bean and does not open a
 * transaction. In production it may be invoked only from a Spring adapter that
 * supplies one transaction boundary for the record update and audit write and
 * coordinates event publication with that transaction. Calling it directly
 * from a controller would not provide the required atomicity.</p>
 *
 * <p>Bu servis üretimde yalnız transaction sağlayan bir Spring adaptörü içinden çağrılabilir.</p>
 */
public final class WorkflowApplicationService {

    private static final WorkflowErrorCode UNRESOLVED_TARGET_SENTINEL =
            WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID;

    private final WorkflowRecordPort recordPort;
    private final CurrentActorProvider currentActorProvider;
    private final TargetUserResolver targetUserResolver;
    private final WorkflowTransitionValidator validator;
    private final AuditService auditService;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public WorkflowApplicationService(
            WorkflowRecordPort recordPort,
            CurrentActorProvider currentActorProvider,
            TargetUserResolver targetUserResolver,
            WorkflowTransitionValidator validator,
            AuditService auditService,
            WorkflowEventPublisher eventPublisher,
            Clock clock) {
        this.recordPort = Objects.requireNonNull(recordPort, "recordPort");
        this.currentActorProvider = Objects.requireNonNull(currentActorProvider, "currentActorProvider");
        this.targetUserResolver = Objects.requireNonNull(targetUserResolver, "targetUserResolver");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public WorkflowActionResponse performAction(UUID recordId, WorkflowActionRequest request) {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(request, "request");
        WorkflowAction action = Objects.requireNonNull(request.action(), "request.action");

        CurrentActor actor = Objects.requireNonNull(
                currentActorProvider.currentActor(),
                "currentActorProvider.currentActor()");
        WorkflowRecordSnapshot record = findActiveRecord(recordId);

        boolean targetProvidedInRequest = request.targetUserId() != null;
        TransitionDecision preliminaryDecision = validator.validate(new TransitionContext(
                record.status(),
                action,
                actor.role(),
                actor.id().equals(record.createdBy()),
                actor.id().equals(record.assignedTo()),
                request.comment(),
                targetProvidedInRequest,
                null,
                false));

        validatePreliminaryDecision(action, preliminaryDecision);

        TargetResolution resolution = Objects.requireNonNull(
                targetUserResolver.resolve(action, request.targetUserId(), record),
                "targetUserResolver.resolve(...)");
        WorkflowUserSnapshot target = resolvedTarget(action, resolution);

        TransitionDecision finalDecision = target == null
                ? preliminaryDecision
                : validator.validate(new TransitionContext(
                        record.status(),
                        action,
                        actor.role(),
                        actor.id().equals(record.createdBy()),
                        actor.id().equals(record.assignedTo()),
                        request.comment(),
                        targetProvidedInRequest,
                        target.role(),
                        target.active()));
        TransitionDecision.Allowed allowed = requireAllowed(finalDecision);

        UUID assignedTo = target == null ? null : target.id();
        UUID lastDeputyId = action == WorkflowAction.BASKANA_ILET
                ? actor.id()
                : record.lastDeputyId();
        Instant performedAt = clock.instant();

        recordPort.update(new WorkflowRecordUpdate(
                record.id(),
                allowed.targetStatus(),
                assignedTo,
                lastDeputyId,
                record.version(),
                performedAt));

        auditService.record(new WorkflowTransitionAudit(
                record.id(),
                action,
                record.status(),
                allowed.targetStatus(),
                actor.id(),
                actor.role(),
                assignedTo,
                request.comment(),
                performedAt));

        eventPublisher.publish(new WorkflowStatusChangedEvent(
                record.id(),
                action,
                record.status(),
                allowed.targetStatus(),
                actor.id(),
                actor.role(),
                record.assignedTo(),
                assignedTo,
                request.comment(),
                performedAt));

        return new WorkflowActionResponse(
                record.id(),
                action,
                record.status(),
                allowed.targetStatus(),
                assignedTo,
                actor.id(),
                performedAt);
    }

    private WorkflowRecordSnapshot findActiveRecord(UUID recordId) {
        Optional<WorkflowRecordSnapshot> result = Objects.requireNonNull(
                recordPort.findById(recordId),
                "recordPort.findById(recordId)");
        WorkflowRecordSnapshot record = result
                .orElseThrow(() -> new WorkflowRecordNotFoundException(recordId));
        if (record.isDeleted()) {
            throw new WorkflowRecordNotFoundException(recordId);
        }
        return record;
    }

    private static void validatePreliminaryDecision(
            WorkflowAction action,
            TransitionDecision decision) {
        if (action.requiresTargetUser()) {
            if (decision instanceof TransitionDecision.Rejected rejected
                    && rejected.errorCode() == UNRESOLVED_TARGET_SENTINEL) {
                return;
            }
            if (decision instanceof TransitionDecision.Rejected rejected) {
                throw new WorkflowApplicationException(rejected.errorCode());
            }
            throw new IllegalStateException(
                    "Target-requiring action passed validation before its target was resolved: " + action);
        }

        if (decision instanceof TransitionDecision.Rejected rejected) {
            throw new WorkflowApplicationException(rejected.errorCode());
        }
    }

    private static WorkflowUserSnapshot resolvedTarget(
            WorkflowAction action,
            TargetResolution resolution) {
        if (resolution instanceof TargetResolution.Resolved resolved) {
            if (!action.requiresTargetUser()) {
                throw new IllegalStateException("Unexpected target resolved for action: " + action);
            }
            return resolved.user();
        }
        if (resolution instanceof TargetResolution.NotProvided) {
            if (action.requiresTargetUser()) {
                throw new IllegalStateException("Target resolver returned NotProvided for action: " + action);
            }
            return null;
        }
        if (resolution instanceof TargetResolution.RequestTargetNotFound) {
            throw new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID);
        }
        if (resolution instanceof TargetResolution.RoleNotConfigured) {
            throw new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_ROLE_NOT_CONFIGURED);
        }
        if (resolution instanceof TargetResolution.DataIntegrityFailure failure) {
            throw new WorkflowDataIntegrityException(failure.reason(), failure.referencedUserId());
        }
        throw new IllegalStateException("Unknown target resolution: " + resolution.getClass().getName());
    }

    private static TransitionDecision.Allowed requireAllowed(TransitionDecision decision) {
        if (decision instanceof TransitionDecision.Allowed allowed) {
            return allowed;
        }
        TransitionDecision.Rejected rejected = (TransitionDecision.Rejected) decision;
        throw new WorkflowApplicationException(rejected.errorCode());
    }
}
