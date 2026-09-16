package btk.staj.WorkFlowProject.department.service;

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
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowActionEntity;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowStatusEntity;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowActionRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowStatusRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentRoutingRuleAdminServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private DepartmentRepository departments;
    private DepartmentRoutingRuleRepository routingRules;
    private WorkflowTransitionRepository transitions;
    private WorkflowStatusRepository statuses;
    private WorkflowActionRepository actions;
    private RoleRepository roles;
    private AuditLogService auditLogs;
    private DepartmentRoutingRuleAdminService service;

    @BeforeEach
    void setUp() {
        departments = mock(DepartmentRepository.class);
        routingRules = mock(DepartmentRoutingRuleRepository.class);
        transitions = mock(WorkflowTransitionRepository.class);
        statuses = mock(WorkflowStatusRepository.class);
        actions = mock(WorkflowActionRepository.class);
        roles = mock(RoleRepository.class);
        auditLogs = mock(AuditLogService.class);
        UserAuditLogService userAuditLogs = mock(UserAuditLogService.class);
        CurrentVisibilityActorProvider actorsProvider = mock(CurrentVisibilityActorProvider.class);
        when(actorsProvider.currentVisibilityActor()).thenReturn(new VisibilityActor(
                ADMIN_ID, new RoleId(1), Optional.of(SystemRoleKey.ADMIN), Set.of("DEPARTMENT_MANAGE")));
        service = new DepartmentRoutingRuleAdminService(departments, routingRules, transitions, statuses, actions,
                roles, actorsProvider, auditLogs, userAuditLogs);

        when(departments.existsById(1)).thenReturn(true);
        when(routingRules.save(any(DepartmentRoutingRuleEntity.class)))
                .thenAnswer(invocation -> {
                    DepartmentRoutingRuleEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) entity.setId(10);
                    return entity;
                });
        when(statuses.existsById(5)).thenReturn(true);
        when(statuses.findById(5)).thenReturn(Optional.of(status(5, "BSK_YRD_INCELEMESINDE")));
        when(actions.existsById(7)).thenReturn(true);
        when(actions.findById(7)).thenReturn(Optional.of(action(7, "BASKANA_ILET")));
        when(transitions.existsByFromStatusIdAndActionIdAndActiveTrue(5, 7)).thenReturn(true);
        when(roles.findById(9)).thenReturn(Optional.of(dynamicRole(9, "HUKUK_UZMANI")));
    }

    private static WorkflowStatusEntity status(Integer id, String name) {
        WorkflowStatusEntity status = new WorkflowStatusEntity();
        status.setId(id);
        status.setName(name);
        status.setDisplayName(name);
        return status;
    }

    private static WorkflowActionEntity action(Integer id, String name) {
        WorkflowActionEntity action = new WorkflowActionEntity();
        action.setId(id);
        action.setName(name);
        action.setDisplayName(name);
        return action;
    }

    private static Role dynamicRole(Integer id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setActive(true);
        role.setWorkflowActor(true);
        role.setSystemKey(null);
        role.setMaxUsers(null);
        return role;
    }

    private static Role systemRole(Integer id, String name, String systemKey, Integer maxUsers) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setActive(true);
        role.setWorkflowActor(true);
        role.setSystem(true);
        role.setSystemKey(systemKey);
        role.setMaxUsers(maxUsers);
        return role;
    }

    private static CreateDepartmentRoutingRuleRequest request(Integer fromStatusId, Integer actionId, Integer targetRoleId) {
        CreateDepartmentRoutingRuleRequest request = new CreateDepartmentRoutingRuleRequest();
        request.setFromStatusId(fromStatusId);
        request.setActionId(actionId);
        request.setTargetRoleId(targetRoleId);
        return request;
    }

    @Nested
    class Olusturma {

        @Test
        void gecerli_kural_olusturulur() {
            DepartmentRoutingRuleResponse response = service.create(1, request(5, 7, 9));

            assertThat(response.departmentId()).isEqualTo(1);
            assertThat(response.fromStatus()).isEqualTo("BSK_YRD_INCELEMESINDE");
            assertThat(response.action()).isEqualTo("BASKANA_ILET");
            assertThat(response.targetRoleName()).isEqualTo("HUKUK_UZMANI");
            assertThat(response.active()).isTrue();
        }

        @Test
        void bilinmeyen_departman_reddedilir() {
            when(departments.existsById(404)).thenReturn(false);

            assertThatThrownBy(() -> service.create(404, request(5, 7, 9)))
                    .isInstanceOf(DepartmentNotFoundException.class);
            verify(routingRules, never()).save(any());
        }

        @Test
        void gercek_gecise_karsilik_gelmeyen_durum_aksiyon_reddedilir() {
            when(transitions.existsByFromStatusIdAndActionIdAndActiveTrue(5, 7)).thenReturn(false);

            assertThatThrownBy(() -> service.create(1, request(5, 7, 9)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("aktif bir geçiş yok");
            verify(routingRules, never()).save(any());
        }

        @Test
        void kapasite_sinirli_yerlesik_rol_hedef_gosterilemez() {
            when(roles.findById(2)).thenReturn(Optional.of(systemRole(2, "BASKAN_YARDIMCISI", "BASKAN_YARDIMCISI", 1)));

            assertThatThrownBy(() -> service.create(1, request(5, 7, 2)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("kapasite sınırlı");
            verify(routingRules, never()).save(any());
        }

        @Test
        void admin_rolu_hedef_gosterilemez() {
            when(roles.findById(3)).thenReturn(Optional.of(systemRole(3, "ADMIN", "ADMIN", 1)));

            assertThatThrownBy(() -> service.create(1, request(5, 7, 3)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ADMIN");
            verify(routingRules, never()).save(any());
        }

        @Test
        void pasif_rol_hedef_gosterilemez() {
            Role role = dynamicRole(9, "HUKUK_UZMANI");
            role.setActive(false);
            when(roles.findById(9)).thenReturn(Optional.of(role));

            assertThatThrownBy(() -> service.create(1, request(5, 7, 9)))
                    .isInstanceOf(BusinessRuleException.class);
            verify(routingRules, never()).save(any());
        }

        @Test
        void ayni_uclu_icin_ikinci_kural_reddedilir() {
            when(routingRules.existsByDepartmentIdAndFromStatusIdAndActionId(1, 5, 7)).thenReturn(true);

            assertThatThrownBy(() -> service.create(1, request(5, 7, 9)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("zaten bir routing kuralı var");
            verify(routingRules, never()).save(any());
        }

        @Test
        void olusturma_audit_kaydi_yazar() {
            service.create(1, request(5, 7, 9));

            verify(auditLogs).recordAccess(argThat(event -> "DEPARTMENT_ROUTING_RULE_CREATED".equals(event.action())));
        }
    }

    @Nested
    class Guncelleme {

        private DepartmentRoutingRuleEntity existingRule() {
            DepartmentRoutingRuleEntity rule = new DepartmentRoutingRuleEntity();
            rule.setId(10);
            rule.setDepartmentId(1);
            rule.setFromStatusId(5);
            rule.setActionId(7);
            rule.setTargetRoleId(9);
            rule.setActive(true);
            return rule;
        }

        @Test
        void hedef_rol_degistirilebilir() {
            when(routingRules.findById(10)).thenReturn(Optional.of(existingRule()));
            Role otherRole = dynamicRole(11, "SATIN_ALMA_UZMANI");
            when(roles.findById(11)).thenReturn(Optional.of(otherRole));
            UpdateDepartmentRoutingRuleRequest request = new UpdateDepartmentRoutingRuleRequest();
            request.setTargetRoleId(11);

            assertThat(service.update(1, 10, request).targetRoleName()).isEqualTo("SATIN_ALMA_UZMANI");
        }

        @Test
        void baska_departmanin_kurali_bulunamadi_sayilir() {
            DepartmentRoutingRuleEntity rule = existingRule();
            rule.setDepartmentId(2);
            when(routingRules.findById(10)).thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> service.update(1, 10, new UpdateDepartmentRoutingRuleRequest()))
                    .isInstanceOf(DepartmentRoutingRuleNotFoundException.class);
        }

        @Test
        void pasiflestirilir_ve_yeniden_etkinlestirilir() {
            when(routingRules.findById(10)).thenReturn(Optional.of(existingRule()));
            UpdateDepartmentRoutingRuleRequest deactivate = new UpdateDepartmentRoutingRuleRequest();
            deactivate.setActive(false);

            assertThat(service.update(1, 10, deactivate).active()).isFalse();
        }

        @Test
        void degisiklik_yoksa_kayit_yapilmaz() {
            when(routingRules.findById(10)).thenReturn(Optional.of(existingRule()));

            assertThatCode(() -> service.update(1, 10, new UpdateDepartmentRoutingRuleRequest()))
                    .doesNotThrowAnyException();
            verify(routingRules, never()).save(any());
        }
    }

    @Nested
    class Listeleme {

        @Test
        void bilinmeyen_departman_reddedilir() {
            when(departments.existsById(404)).thenReturn(false);

            assertThatThrownBy(() -> service.listRules(404)).isInstanceOf(DepartmentNotFoundException.class);
        }
    }
}
