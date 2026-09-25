package btk.staj.WorkFlowProject.department.service;

import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.department.dto.CreateDepartmentRoutingRuleRequest;
import btk.staj.WorkFlowProject.department.dto.DepartmentRoutingRuleResponse;
import btk.staj.WorkFlowProject.department.dto.UpdateDepartmentRoutingRuleRequest;
import btk.staj.WorkFlowProject.department.entity.DepartmentRoutingRuleEntity;
import btk.staj.WorkFlowProject.department.exception.DepartmentNotFoundException;
import btk.staj.WorkFlowProject.department.exception.DepartmentRoutingRuleNotFoundException;
import btk.staj.WorkFlowProject.department.repository.DepartmentRepository;
import btk.staj.WorkFlowProject.department.repository.DepartmentRoutingRuleRepository;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowActionEntity;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowStatusEntity;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowActionRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowStatusRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * AP-5 departman routing kurallari. Bir kural, bir kaydin belirli bir
 * departmana atandiktan sonra belirli bir (durum, aksiyon) icin hangi roldeki
 * uyenin islem yapabilecegini tanimlar (WORKFLOW_V1_V2_PLANI.md SS11).
 *
 * <p>(fromStatusId, actionId) rastgele bir kombinasyon olamaz: departman
 * kuyruguna dusen bir kaydin gercekten alabilecegi bir aksiyona karsilik
 * gelmelidir - {@link WorkflowTransitionRepository#existsByFromStatusIdAndActionIdAndActiveTrue}
 * ile dogrulanir.
 *
 * <p>Hedef rol ADR-0007'nin baglayici sonucuna tabidir: {@code max_users}
 * sinirli bir yerlesik rol (ADMIN/BASKAN/BASKAN_YARDIMCISI) hedef gosterilemez
 * - "departman tek kisiye cozulur ve model anlamini yitirir" riskini
 * dogrudan engeller (ADR-0007 "Maliyet ve riskler").
 */
@Service
public class DepartmentRoutingRuleAdminService {

    private final DepartmentRepository departments;
    private final DepartmentRoutingRuleRepository routingRules;
    private final WorkflowTransitionRepository transitions;
    private final WorkflowStatusRepository statuses;
    private final WorkflowActionRepository actions;
    private final RoleRepository roles;
    private final CurrentVisibilityActorProvider actors;
    private final AuditLogService auditLogs;
    private final UserAuditLogService userAuditLogs;

    public DepartmentRoutingRuleAdminService(DepartmentRepository departments,
                                             DepartmentRoutingRuleRepository routingRules,
                                             WorkflowTransitionRepository transitions,
                                             WorkflowStatusRepository statuses,
                                             WorkflowActionRepository actions,
                                             RoleRepository roles,
                                             CurrentVisibilityActorProvider actors,
                                             AuditLogService auditLogs,
                                             UserAuditLogService userAuditLogs) {
        this.departments = departments;
        this.routingRules = routingRules;
        this.transitions = transitions;
        this.statuses = statuses;
        this.actions = actions;
        this.roles = roles;
        this.actors = actors;
        this.auditLogs = auditLogs;
        this.userAuditLogs = userAuditLogs;
    }

    @Transactional(readOnly = true)
    public List<DepartmentRoutingRuleResponse> listRules(Integer departmentId) {
        requireDepartment(departmentId);
        return routingRules.findAllByDepartmentIdOrderByIdAsc(departmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentRoutingRuleResponse create(Integer departmentId, CreateDepartmentRoutingRuleRequest request) {
        requireDepartment(departmentId);
        requireValidTransitionPair(request.getFromStatusId(), request.getActionId());
        Role targetRole = requireValidTargetRole(request.getTargetRoleId());

        if (routingRules.existsByDepartmentIdAndFromStatusIdAndActionId(
                departmentId, request.getFromStatusId(), request.getActionId())) {
            throw new BusinessRuleException(
                    "Bu departman, durum ve aksiyon için zaten bir routing kuralı var");
        }

        DepartmentRoutingRuleEntity rule = new DepartmentRoutingRuleEntity();
        rule.setDepartmentId(departmentId);
        rule.setFromStatusId(request.getFromStatusId());
        rule.setActionId(request.getActionId());
        rule.setTargetRoleId(targetRole.getId());
        rule.setActive(true);

        DepartmentRoutingRuleEntity saved = routingRules.save(rule);
        audit("DEPARTMENT_ROUTING_RULE_CREATED", "ruleId=" + saved.getId() + ";departmentId=" + departmentId
                + ";fromStatusId=" + saved.getFromStatusId() + ";actionId=" + saved.getActionId()
                + ";targetRoleId=" + saved.getTargetRoleId());

        return toResponse(saved);
    }

    @Transactional
    public DepartmentRoutingRuleResponse update(Integer departmentId, Integer ruleId,
                                                UpdateDepartmentRoutingRuleRequest request) {
        DepartmentRoutingRuleEntity rule = routingRules.findById(ruleId)
                .filter(r -> r.getDepartmentId().equals(departmentId))
                .orElseThrow(() -> new DepartmentRoutingRuleNotFoundException("Routing kuralı bulunamadı: " + ruleId));

        List<String> changes = new ArrayList<>();

        if (request.getTargetRoleId() != null && !request.getTargetRoleId().equals(rule.getTargetRoleId())) {
            Role newRole = requireValidTargetRole(request.getTargetRoleId());
            changes.add("hedef rol: " + rule.getTargetRoleId() + " → " + newRole.getId());
            rule.setTargetRoleId(newRole.getId());
        }

        if (request.getActive() != null && request.getActive() != rule.isActive()) {
            rule.setActive(request.getActive());
            changes.add(request.getActive() ? "etkinleştirildi" : "pasifleştirildi");
        }

        if (changes.isEmpty()) return toResponse(rule);

        DepartmentRoutingRuleEntity saved = routingRules.save(rule);
        audit("DEPARTMENT_ROUTING_RULE_UPDATED", "ruleId=" + saved.getId() + ";departmentId=" + departmentId
                + ";değişiklikler=" + String.join(", ", changes));

        return toResponse(saved);
    }

    private void requireDepartment(Integer departmentId) {
        if (!departments.existsById(departmentId)) {
            throw new DepartmentNotFoundException("Departman bulunamadı: " + departmentId);
        }
    }

    private void requireValidTransitionPair(Integer fromStatusId, Integer actionId) {
        if (!statuses.existsById(fromStatusId)) {
            throw new BusinessRuleException("Durum bulunamadı: " + fromStatusId);
        }
        if (!actions.existsById(actionId)) {
            throw new BusinessRuleException("Aksiyon bulunamadı: " + actionId);
        }
        if (!transitions.existsByFromStatusIdAndActionIdAndActiveTrue(fromStatusId, actionId)) {
            throw new BusinessRuleException(
                    "Bu durum/aksiyon birleşimi için aktif bir geçiş yok; routing kuralı gerçek bir geçişi hedeflemeli");
        }
    }

    /** ADR-0007: hedef rol aktif, workflow aktoru ve sinirsiz kapasiteli (max_users = NULL) olmali. */
    private Role requireValidTargetRole(Integer targetRoleId) {
        Role role = roles.findById(targetRoleId)
                .orElseThrow(() -> new BusinessRuleException("Hedef rol bulunamadı: " + targetRoleId));
        if (!role.isActive() || !role.isWorkflowActor()) {
            throw new BusinessRuleException(
                    "Hedef rol aktif ve workflow aktörü olmalı: " + role.getName());
        }
        if ("ADMIN".equals(role.getSystemKey())) {
            throw new BusinessRuleException("ADMIN rolü departman routing hedefi olamaz");
        }
        if (role.getMaxUsers() != null) {
            throw new BusinessRuleException("Hedef rol kapasite sınırlı (max " + role.getMaxUsers()
                    + " kullanıcı) olamaz; departman routing hedefleri sınırsız kapasiteli olmalı: " + role.getName());
        }
        return role;
    }

    private DepartmentRoutingRuleResponse toResponse(DepartmentRoutingRuleEntity rule) {
        WorkflowStatusEntity status = statuses.findById(rule.getFromStatusId()).orElseThrow();
        WorkflowActionEntity action = actions.findById(rule.getActionId()).orElseThrow();
        Role role = roles.findById(rule.getTargetRoleId()).orElseThrow();
        return new DepartmentRoutingRuleResponse(
                rule.getId(), rule.getDepartmentId(),
                status.getId(), status.getName(), status.getDisplayName(),
                action.getId(), action.getName(), action.getDisplayName(),
                role.getId(), role.getName(),
                rule.isActive());
    }

    private void audit(String action, String comment) {
        VisibilityActor actor = actors.currentVisibilityActor();
        RequestAccessEvent event = new RequestAccessEvent(action, actor.id(), actor.roleId().value(),
                actor.systemRole().map(Enum::name).orElse(null), null, null, null, null, comment);
        if (event.adminActor()) auditLogs.recordAccess(event);
        else userAuditLogs.recordAccess(event);
    }
}
