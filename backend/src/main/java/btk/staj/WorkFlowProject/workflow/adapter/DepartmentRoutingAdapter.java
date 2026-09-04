package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;
import btk.staj.WorkFlowProject.department.repository.DepartmentMemberRepository;
import btk.staj.WorkFlowProject.department.repository.DepartmentRepository;
import btk.staj.WorkFlowProject.department.repository.DepartmentRoutingRuleRepository;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.model.DepartmentRoutingResolution;
import btk.staj.WorkFlowProject.workflow.port.DepartmentRoutingPort;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowActionRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowStatusRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DepartmentRoutingAdapter implements DepartmentRoutingPort {
    private final DepartmentRepository departments;
    private final DepartmentMemberRepository members;
    private final DepartmentRoutingRuleRepository routing;
    private final RoleRepository roles;
    private final RolePermissionRepository permissions;
    private final Map<String, Integer> statusIds;
    private final Map<String, Integer> actionIds;

    public DepartmentRoutingAdapter(DepartmentRepository departments, DepartmentMemberRepository members,
            DepartmentRoutingRuleRepository routing, RoleRepository roles, RolePermissionRepository permissions,
            WorkflowStatusRepository statuses, WorkflowActionRepository actions) {
        this.departments = departments;
        this.members = members;
        this.routing = routing;
        this.roles = roles;
        this.permissions = permissions;
        // Only immutable catalog identities are cached. Rules, users and permissions are live.
        statusIds = statuses.findAll().stream().collect(Collectors.toUnmodifiableMap(s -> s.getName(), s -> s.getId()));
        actionIds = actions.findAll().stream().collect(Collectors.toUnmodifiableMap(a -> a.getName(), a -> a.getId()));
    }

    @Override
    public DepartmentRoutingResolution resolve(int departmentId, RecordStatus from, WorkflowAction action) {
        var missing = new DepartmentRoutingResolution.RuleNotConfigured(departmentId);
        if (!isActiveDepartment(departmentId)) return missing;
        Integer statusId = statusIds.get(from.name());
        Integer actionId = actionIds.get(action.name());
        if (statusId == null || actionId == null) return missing;
        var route = routing.findByDepartmentIdAndFromStatusIdAndActionIdAndActiveTrue(departmentId, statusId, actionId);
        if (route.isEmpty()) return missing;
        var role = roles.findById(route.get().getTargetRoleId())
                .filter(r -> r.isActive() && r.isWorkflowActor() && !"ADMIN".equals(r.getSystemKey()));
        if (role.isEmpty()) return missing;
        RoleId roleId = new RoleId(role.get().getId());
        Set<UUID> eligible = members.findActiveUsersByDepartmentId(departmentId).stream()
                .filter(u -> u.getRole() != null && roleId.value().equals(u.getRole().getId()))
                .map(u -> u.getId()).collect(Collectors.toSet());
        if (eligible.isEmpty()) return new DepartmentRoutingResolution.NoEligibleMember(departmentId, roleId);
        return new DepartmentRoutingResolution.Resolved(roleId, eligible);
    }

    @Override
    public boolean isActiveDepartment(int departmentId) {
        return departments.findById(departmentId).filter(DepartmentEntity::isActive).isPresent();
    }

    @Override
    public Set<Integer> activeDepartmentIdsFor(UUID userId) {
        var ids = members.findAllByIdUserId(userId).stream().map(m -> m.getId().getDepartmentId()).toList();
        return departments.findAllById(ids).stream().filter(DepartmentEntity::isActive)
                .map(DepartmentEntity::getId).collect(Collectors.toSet());
    }

    @Override
    public boolean roleHasPermission(RoleId roleId, String permissionCode) {
        return permissions.findActiveCodesByRoleId(roleId.value()).contains(permissionCode);
    }
}
