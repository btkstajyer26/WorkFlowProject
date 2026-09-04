package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.Permission;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.repository.PermissionRepository;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActorBindingView;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowActionEntity;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowStatusEntity;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowTransitionEntity;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowBindingException;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowActionRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowStatusRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static btk.staj.WorkFlowProject.workflow.exception.WorkflowBindingException.Reason.*;

/**
 * WF-8: actor bindings on fixed edges, never graph editing. Call mutations through
 * this Spring bean without an ambient transaction. The source coordinates these
 * transactions with manual reload and publishes only their committed snapshots.
 */
@Service
public class WorkflowActorBindingService {
    private final WorkflowTransitionRepository transitions;
    private final WorkflowStatusRepository statuses;
    private final WorkflowActionRepository actions;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final CurrentVisibilityActorProvider actors;
    private final ReloadableTransitionRuleSource rules;
    private final AuditLogService auditLogs;
    private final UserAuditLogService userAuditLogs;
    private final TransactionTemplate transaction;

    public WorkflowActorBindingService(WorkflowTransitionRepository transitions,
            WorkflowStatusRepository statuses, WorkflowActionRepository actions,
            RoleRepository roles, PermissionRepository permissions, RolePermissionRepository rolePermissions,
            CurrentVisibilityActorProvider actors, ReloadableTransitionRuleSource rules,
            AuditLogService auditLogs, UserAuditLogService userAuditLogs,
            PlatformTransactionManager transactionManager) {
        this.transitions = transitions;
        this.statuses = statuses;
        this.actions = actions;
        this.roles = roles;
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
        this.actors = actors;
        this.rules = rules;
        this.auditLogs = auditLogs;
        this.userAuditLogs = userAuditLogs;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<WorkflowActorBindingView> listTransitions() {
        requireAuthority("WORKFLOW_VIEW");
        return transitions.findAllByOrderByIdAsc().stream().map(this::view).toList();
    }

    @Transactional(propagation = Propagation.NEVER)
    public WorkflowActorBindingView bind(Integer templateTransitionId, Integer actorRoleId) {
        VisibilityActor actor = requireAuthority("WORKFLOW_MANAGE");
        requireId(templateTransitionId);
        requireId(actorRoleId);
        return rules.updateAndReload(transaction, () -> {
            Role role = roles.findByIdForUpdate(actorRoleId)
                    .orElseThrow(() -> failure(ROLE_NOT_FOUND, "Aktör rolü bulunamadı"));
            if (protectedRole(role) || !role.isActive() || !role.isWorkflowActor()) {
                throw failure(INVALID_ROLE, "Aktif, dinamik bir workflow aktör rolü gerekli");
            }
            WorkflowTransitionEntity template = transitions.findById(templateTransitionId)
                    .orElseThrow(() -> failure(TEMPLATE_NOT_FOUND, "Kaynak geçiş bulunamadı"));
            WorkflowTransitionEntity existing = transitions.findByFromStatusIdAndActionIdAndActorRoleId(
                    template.getFromStatusId(), template.getActionId(), actorRoleId).orElse(null);
            // All WF-8 writers lock the role first, then transition IDs in ascending order.
            List<Integer> ids = existing == null ? List.of(templateTransitionId)
                    : List.of(templateTransitionId, existing.getId()).stream().distinct().sorted().toList();
            transitions.findAllForUpdate(ids);
            TransitionRule rule = validateTemplate(template);
            Set<String> codes = Set.copyOf(rolePermissions.findActiveCodesByRoleId(actorRoleId));
            if (!codes.contains("RECORD_VIEW") || !codes.contains(rule.requiredPermissionCode())) {
                throw failure(MISSING_ROLE_PERMISSION, "Rol RECORD_VIEW ve geçiş permission'ına sahip olmalı");
            }
            if (existing != null && existing.isActive()) {
                throw failure(DUPLICATE_BINDING, "Bu geçiş için rol bağı zaten aktif");
            }
            if (existing != null && !sameMetadata(template, existing)) {
                throw failure(METADATA_MISMATCH, "Pasif bağın sabit geçiş alanları kaynakla uyuşmuyor");
            }
            WorkflowTransitionEntity binding = existing == null ? copy(template, actorRoleId) : existing;
            binding.setActive(true);
            transitions.saveAndFlush(binding);
            audit(actor, "WORKFLOW_BINDING_ENABLED", templateTransitionId, binding, false, true);
            transitions.flush();
            return view(binding);
        });
    }

    @Transactional(propagation = Propagation.NEVER)
    public WorkflowActorBindingView unbind(Integer bindingId) {
        VisibilityActor actor = requireAuthority("WORKFLOW_MANAGE");
        requireId(bindingId);
        return rules.updateAndReload(transaction, () -> {
            WorkflowTransitionEntity binding = transitions.findById(bindingId)
                    .orElseThrow(() -> failure(BINDING_NOT_FOUND, "Rol bağı bulunamadı"));
            Role role = roles.findByIdForUpdate(binding.getActorRoleId())
                    .orElseThrow(() -> failure(ROLE_NOT_FOUND, "Aktör rolü bulunamadı"));
            transitions.findAllForUpdate(List.of(bindingId));
            if (protectedRole(role)) {
                throw failure(PROTECTED_BINDING, "Sistem rolüne ait geçiş bağı kaldırılamaz");
            }
            if (!binding.isActive()) return view(binding);
            if (transitions.hasOpenRecords(binding.getFromStatusId(), binding.getActorRoleId(),
                    binding.getActorRequirement().name())) {
                throw failure(BINDING_IN_USE, "Bu bağı kullanabilecek açık kayıtlar var");
            }
            binding.setActive(false);
            transitions.saveAndFlush(binding);
            audit(actor, "WORKFLOW_BINDING_DISABLED", null, binding, true, false);
            transitions.flush();
            return view(binding);
        });
    }

    private VisibilityActor requireAuthority(String code) {
        // This provider validates the trusted principal and active user/role. ADMIN
        // is allowed to manage bindings; record-visibility scope is not evaluated.
        VisibilityActor actor = actors.currentVisibilityActor();
        if (!actor.permissionCodes().contains(code)) throw new AccessDeniedException("Gerekli yetki: " + code);
        return actor;
    }

    private TransitionRule validateTemplate(WorkflowTransitionEntity template) {
        try {
            WorkflowStatusEntity from = statuses.findById(template.getFromStatusId()).orElseThrow();
            WorkflowStatusEntity to = statuses.findById(template.getToStatusId()).orElseThrow();
            WorkflowActionEntity action = actions.findById(template.getActionId()).orElseThrow();
            Permission permission = template.getRequiredPermissionId() == null ? null
                    : permissions.findById(template.getRequiredPermissionId()).orElseThrow();
            RecordStatus fromStatus = RecordStatus.valueOf(from.getName());
            RecordStatus toStatus = RecordStatus.valueOf(to.getName());
            WorkflowAction workflowAction = WorkflowAction.valueOf(action.getName());
            if (!template.isActive() || !from.isActive() || !to.isActive() || !action.isActive()
                    || from.isTerminal() || fromStatus.isTerminal()
                    || to.isTerminal() != toStatus.isTerminal()
                    || action.isCommentRequired() != workflowAction.isCommentRequired()
                    || permission == null || !permission.isActive()) {
                throw failure(INVALID_TEMPLATE, "Kaynak geçiş aktif ve desteklenen katalog değerleri kullanmalı");
            }
            return new TransitionRule(fromStatus, workflowAction, new RoleId(template.getActorRoleId()),
                    template.getActorRequirement(), toStatus, TargetStrategy.valueOf(template.getTargetStrategy()),
                    template.getExpectedTargetRoleId() == null ? null : new RoleId(template.getExpectedTargetRoleId()),
                    permission.getCode());
        } catch (IllegalArgumentException | java.util.NoSuchElementException | NullPointerException ex) {
            throw failure(INVALID_TEMPLATE, "Kaynak geçişin yapısal alanları geçersiz");
        }
    }

    private static boolean sameMetadata(WorkflowTransitionEntity a, WorkflowTransitionEntity b) {
        return Objects.equals(a.getFromStatusId(), b.getFromStatusId())
                && Objects.equals(a.getActionId(), b.getActionId())
                && a.getActorRequirement() == b.getActorRequirement()
                && Objects.equals(a.getToStatusId(), b.getToStatusId())
                && Objects.equals(a.getExpectedTargetRoleId(), b.getExpectedTargetRoleId())
                && Objects.equals(a.getTargetStrategy(), b.getTargetStrategy())
                && Objects.equals(a.getRequiredPermissionId(), b.getRequiredPermissionId());
    }

    private static WorkflowTransitionEntity copy(WorkflowTransitionEntity template, Integer roleId) {
        WorkflowTransitionEntity result = new WorkflowTransitionEntity();
        result.setFromStatusId(template.getFromStatusId());
        result.setActionId(template.getActionId());
        result.setActorRoleId(roleId);
        result.setActorRequirement(template.getActorRequirement());
        result.setToStatusId(template.getToStatusId());
        result.setExpectedTargetRoleId(template.getExpectedTargetRoleId());
        result.setTargetStrategy(template.getTargetStrategy());
        result.setRequiredPermissionId(template.getRequiredPermissionId());
        return result;
    }

    private WorkflowActorBindingView view(WorkflowTransitionEntity binding) {
        WorkflowStatusEntity from = statuses.findById(binding.getFromStatusId()).orElseThrow();
        WorkflowStatusEntity to = statuses.findById(binding.getToStatusId()).orElseThrow();
        WorkflowActionEntity action = actions.findById(binding.getActionId()).orElseThrow();
        Role role = roles.findById(binding.getActorRoleId()).orElseThrow();
        String permission = binding.getRequiredPermissionId() == null ? null
                : permissions.findById(binding.getRequiredPermissionId()).orElseThrow().getCode();
        return new WorkflowActorBindingView(binding.getId(), from.getId(), from.getName(), from.getDisplayName(),
                action.getId(), action.getName(), action.getDisplayName(), to.getId(), to.getName(), to.getDisplayName(),
                role.getId(), role.getName(), binding.getActorRequirement(), binding.getTargetStrategy(),
                binding.getExpectedTargetRoleId(), binding.getRequiredPermissionId(), permission,
                binding.isActive(), protectedRole(role));
    }

    private void audit(VisibilityActor actor, String action, Integer templateId,
            WorkflowTransitionEntity binding, boolean previousActive, boolean active) {
        String comment = "templateTransitionId=" + templateId + ";bindingId=" + binding.getId()
                + ";actorRoleId=" + actor.roleId().value() + ";boundRoleId=" + binding.getActorRoleId()
                + ";fromStatusId=" + binding.getFromStatusId() + ";actionId=" + binding.getActionId()
                + ";previousActive=" + previousActive + ";active=" + active;
        RequestAccessEvent event = new RequestAccessEvent(action, actor.id(), actor.roleId().value(),
                actor.systemRole().map(Enum::name).orElse(null), null, null, null, null, comment);
        if (event.adminActor()) auditLogs.recordAccess(event);
        else userAuditLogs.recordAccess(event);
    }

    private static boolean protectedRole(Role role) { return role.isSystem() || role.getSystemKey() != null; }

    private static void requireId(Integer id) {
        if (id == null || id <= 0) throw failure(INVALID_ID, "Kimlik pozitif bir sayı olmalı");
    }

    private static WorkflowBindingException failure(WorkflowBindingException.Reason reason, String message) {
        return new WorkflowBindingException(reason, message);
    }
}
