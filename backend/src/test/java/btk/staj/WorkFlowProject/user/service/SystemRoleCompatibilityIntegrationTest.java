package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUserFactory;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.notification.service.MailActionTokenService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.config.BootstrapAdminRunner;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.CategoryRepository;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class SystemRoleCompatibilityIntegrationTest {
    @Autowired UserService service;
    @Autowired RoleCapacityService capacity;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired RecordRepository records;
    @Autowired CategoryRepository categories;
    @Autowired AuthenticatedUserFactory principals;
    @Autowired WorkflowActionService workflow;
    @Autowired MailActionTokenService mail;
    @Autowired PasswordEncoder encoder;
    @Autowired UserAuditLogService audit;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    private Role renamed(String key) {
        Role role = roles.findBySystemKey(key).orElseThrow();
        role.setName("Görünen ad " + UUID.randomUUID());
        return roles.saveAndFlush(role);
    }

    private User user(Role role) {
        User user = new User(); user.setRole(role); user.setFirstName("System"); user.setLastName("Test");
        user.setEmail(UUID.randomUUID()+"@system-role.test"); user.setPasswordHash("unused");
        user.setCreatedAt(LocalDateTime.now()); return users.saveAndFlush(user);
    }

    private void authenticate(User user) {
        AuthenticatedUser principal = principals.create(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Record record(User creator, User assigned, RecordStatus status) {
        return records.saveAndFlush(Record.builder().title("Role compatibility").description("Integration test")
                .categoryId(categories.findAll().getFirst().getId()).createdBy(creator.getId())
                .assignedTo(assigned == null ? null : assigned.getId()).status(status).build());
    }

    @Test void renamedRolesStillResolveWorkflowTargetsAndAuditRoleIdentity() {
        Role employeeRole = renamed("CALISAN"), deputyRole = renamed("BASKAN_YARDIMCISI");
        jdbc.update("UPDATE users SET is_active = false WHERE role_id = ?", deputyRole.getId());
        User creator = user(employeeRole), deputy = user(deputyRole);
        authenticate(creator);
        assertThat(principals.create(creator).getLegacyRole()).isEqualTo(RoleName.CALISAN);
        Record record = record(creator, null, RecordStatus.TASLAK);
        workflow.performAction(record.getId(), new WorkflowActionRequest(WorkflowAction.GONDER, null, null));
        em.flush(); em.clear();
        Record changed = records.findById(record.getId()).orElseThrow();
        assertThat(changed.getAssignedTo()).isEqualTo(deputy.getId());
        assertThat(changed.getStatus()).isEqualTo(RecordStatus.BSK_YRD_INCELEMESINDE);
        assertThat(jdbc.queryForObject("SELECT role_id FROM audit_logs WHERE record_id = ? AND action = 'GONDER'", Integer.class, record.getId()))
                .isEqualTo(employeeRole.getId());
    }

    @Test void defaultCreationUsesSystemKeyAndEnforcesConfiguredLimit() {
        Role employee = renamed("CALISAN");
        employee.setMaxUsers(Math.toIntExact(users.countByRole_IdAndActiveTrue(employee.getId())) + 1);
        roles.saveAndFlush(employee);
        User manager = user(renamed("ADMIN")); authenticate(manager);
        assertThatThrownBy(() -> service.setActive(manager.getId(), false)).isInstanceOf(BusinessRuleException.class);
        User created = service.createUser("Default", "Role", UUID.randomUUID()+"@created.test", "StrongTest123!");
        assertThat(created.getRole().getId()).isEqualTo(employee.getId());
        assertThatThrownBy(() -> service.createUser("Over", "Limit", UUID.randomUUID()+"@created.test", "StrongTest123!"))
                .isInstanceOf(AdminLimitExceededException.class);
    }

    @Test void renamedDeputySeatAndPendingWorkMoveTogether() {
        Role deputyRole = renamed("BASKAN_YARDIMCISI"), presidentRole = renamed("BASKAN"), employee = renamed("CALISAN");
        jdbc.update("UPDATE users SET is_active = false WHERE role_id IN (?, ?)", deputyRole.getId(), presidentRole.getId());
        User deputy = user(deputyRole), replacement = user(employee), manager = user(renamed("ADMIN"));
        authenticate(manager);
        Record record = record(replacement, deputy, RecordStatus.BSK_YRD_INCELEMESINDE);
        record.setLastDeputyId(deputy.getId()); records.saveAndFlush(record);
        service.changeRole(deputy.getId(), presidentRole.getId(), replacement.getId());
        em.flush(); em.clear();
        assertThat(users.findById(deputy.getId()).orElseThrow().getRole().getId()).isEqualTo(presidentRole.getId());
        assertThat(users.findById(replacement.getId()).orElseThrow().getRole().getId()).isEqualTo(deputyRole.getId());
        Record changed = records.findById(record.getId()).orElseThrow();
        assertThat(changed.getAssignedTo()).isEqualTo(replacement.getId());
        assertThat(changed.getLastDeputyId()).isEqualTo(replacement.getId());
    }

    @Test void bootstrapRecognizesRenamedAdminAndDoesNotCreateASecondHolder() {
        Role adminRole = renamed("ADMIN");
        jdbc.update("UPDATE users SET is_active = false WHERE role_id = ?", adminRole.getId());
        BootstrapAdminRunner runner = new BootstrapAdminRunner(users, roles, encoder, audit, capacity);
        String email = UUID.randomUUID()+"@bootstrap.test";
        ReflectionTestUtils.setField(runner, "adminEmail", email);
        ReflectionTestUtils.setField(runner, "adminPassword", "BootstrapTest123!");
        runner.run(null); runner.run(null);
        assertThat(users.findByEmail(email).orElseThrow().getRole().getId()).isEqualTo(adminRole.getId());
        assertThat(users.countByRole_IdAndActiveTrue(adminRole.getId())).isEqualTo(1);
    }

    @Test void mailActionUsesCurrentPermissionDataAndRealValidator() {
        Role president = renamed("BASKAN"); User actor = user(president);
        Record record = record(actor, actor, RecordStatus.BASKAN_INCELEMESINDE);
        String token = mail.issue(record.getId(), actor, WorkflowAction.ONAYLA);
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ? AND permission_id = (SELECT id FROM permissions WHERE code = 'RECORD_APPROVE')", president.getId());
        WorkflowApplicationException error = catchThrowableOfType(WorkflowApplicationException.class, () -> mail.consume(token));
        assertThat(error.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        assertThat(records.findById(record.getId()).orElseThrow().getStatus()).isEqualTo(RecordStatus.BASKAN_INCELEMESINDE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE record_id = ?", Integer.class, record.getId())).isZero();
    }
}
