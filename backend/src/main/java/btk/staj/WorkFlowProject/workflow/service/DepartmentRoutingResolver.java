package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.model.DepartmentRoutingResolution;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.port.DepartmentRoutingPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import java.util.Objects;
import java.util.UUID;

/** Pure application service; all final actor checks remain in the validator. */
public final class DepartmentRoutingResolver {
    private final DepartmentRoutingPort routing;

    public DepartmentRoutingResolver(DepartmentRoutingPort routing) {
        this.routing = Objects.requireNonNull(routing, "routing");
    }

    public boolean actorHoldsAssignment(CurrentActor actor, WorkflowRecordSnapshot record, WorkflowAction action) {
        if (actor.id().equals(record.assignedTo())) return true;
        if (record.assignedDepartmentId() == null) return false;
        // Missing routing on the act path is a failed relationship, not an early 409.
        return routing.resolve(record.assignedDepartmentId(), record.status(), action)
                instanceof DepartmentRoutingResolution.Resolved resolved
                && resolved.targetRoleId().equals(actor.roleId())
                && resolved.eligibleUserIds().contains(actor.id());
    }

    public void validateTarget(int departmentId, RecordStatus landingStatus, UUID creatorId,
            TransitionRuleSource snapshot) {
        if (!routing.isActiveDepartment(departmentId)) {
            throw new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_DEPARTMENT_INVALID);
        }
        if (!hasUsableRoutingInto(departmentId, landingStatus, creatorId, snapshot)) {
            throw new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED);
        }
    }

    public boolean hasUsableRoutingInto(int departmentId, RecordStatus status, UUID creatorId,
            TransitionRuleSource snapshot) {
        if (status.isTerminal()) return false;
        return snapshot.all().stream().filter(rule -> rule.from() == status).anyMatch(rule ->
                routing.resolve(departmentId, status, rule.action()) instanceof DepartmentRoutingResolution.Resolved resolved
                        && resolved.targetRoleId().equals(rule.actorRoleId())
                        && routing.roleHasPermission(resolved.targetRoleId(), "RECORD_VIEW")
                        && routing.roleHasPermission(resolved.targetRoleId(), rule.requiredPermissionCode())
                        && resolved.eligibleUserIds().stream().anyMatch(id ->
                                rule.actorRequirement().isSatisfiedBy(id.equals(creatorId), true)));
    }
}
