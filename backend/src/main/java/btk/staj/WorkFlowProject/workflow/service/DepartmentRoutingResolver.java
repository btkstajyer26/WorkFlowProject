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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    /**
     * Kaydin {@code landingStatus} durumuna gonderilebilecegi aktif departmanlar (APP-9 SS2).
     *
     * <p>Kume {@link #hasUsableRoutingInto} ile suzulur: donen her departman icin gonderim
     * {@code WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED} almayacagi <em>onceden</em>
     * dogrulanmistir. Hem hedef departman kesfi ucu hem {@code DEPARTMANA_GONDER}
     * aksiyonunun kullanilabilirligi ayni hesabi tuketir; iki yerde ayri kural kurulmaz.
     */
    public Set<Integer> usableTargetDepartments(RecordStatus landingStatus, UUID creatorId,
            TransitionRuleSource snapshot) {
        if (landingStatus.isTerminal()) return Set.of();
        Set<Integer> usable = new LinkedHashSet<>();
        for (int departmentId : routing.activeDepartmentIds()) {
            if (hasUsableRoutingInto(departmentId, landingStatus, creatorId, snapshot)) {
                usable.add(departmentId);
            }
        }
        return Set.copyOf(usable);
    }

    public boolean hasUsableRoutingInto(int departmentId, RecordStatus status, UUID creatorId,
            TransitionRuleSource snapshot) {
        if (status.isTerminal()) return false;
        // Ayni (departman, durum, aksiyon) uclusu birden fazla aktor rolunun kuralinda
        // tekrar edebilir. Cozum bu cagri boyunca deterministiktir, bir kez hesaplanir.
        Map<WorkflowAction, DepartmentRoutingResolution> resolutions = new HashMap<>();
        return snapshot.all().stream().filter(rule -> rule.from() == status).anyMatch(rule ->
                resolutions.computeIfAbsent(rule.action(), action -> routing.resolve(departmentId, status, action))
                        instanceof DepartmentRoutingResolution.Resolved resolved
                        && resolved.targetRoleId().equals(rule.actorRoleId())
                        && routing.roleHasPermission(resolved.targetRoleId(), "RECORD_VIEW")
                        && routing.roleHasPermission(resolved.targetRoleId(), rule.requiredPermissionCode())
                        && resolved.eligibleUserIds().stream().anyMatch(id ->
                                rule.actorRequirement().isSatisfiedBy(id.equals(creatorId), true)));
    }

    public Set<UUID> eligibleAssignees(int departmentId, RecordStatus status,
            TransitionRuleSource snapshot) {
        if (status.isTerminal()) return Set.of();

        Map<WorkflowAction, DepartmentRoutingResolution> resolutions = new HashMap<>();
        Set<UUID> eligible = new LinkedHashSet<>();

        snapshot.all().stream()
                .filter(rule -> rule.from() == status)
                .filter(rule -> rule.actorRequirement()
                        == btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement.ASSIGNEE)
                .forEach(rule -> {
                    DepartmentRoutingResolution resolution = resolutions.computeIfAbsent(
                            rule.action(),
                            action -> routing.resolve(departmentId, status, action));

                    if (resolution instanceof DepartmentRoutingResolution.Resolved resolved
                            && resolved.targetRoleId().equals(rule.actorRoleId())
                            && routing.roleHasPermission(resolved.targetRoleId(), "RECORD_VIEW")
                            && routing.roleHasPermission(
                                    resolved.targetRoleId(), rule.requiredPermissionCode())) {
                        eligible.addAll(resolved.eligibleUserIds());
                    }
                });

        return Set.copyOf(eligible);
    }
}
