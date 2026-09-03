package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Committed fixtures and separate real PostgreSQL transactions exercise the row locks. */
@SpringBootTest
class RoleCapacityIntegrationTest {
    @Autowired UserService service;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired RecordRepository records;
    @MockitoSpyBean UserAuditLogService audit;
    private final List<Integer> roleIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> recordIds = new ArrayList<>();
    private TransactionTemplate tx;
    private User manager;

    @BeforeEach void prepare() {
        tx = new TransactionTemplate(transactions);
        manager = user(role(null), true);
        authenticate();
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        tx.executeWithoutResult(status -> {
            for (UUID id : recordIds) jdbc.update("DELETE FROM records WHERE id = ?", id);
            for (UUID id : userIds) {
                jdbc.update("DELETE FROM user_audit_logs WHERE target_user_id = ? OR performed_by = ?", id, id);
                jdbc.update("DELETE FROM audit_logs WHERE user_id = ?", id);
            }
            for (UUID id : userIds) jdbc.update("DELETE FROM users WHERE id = ?", id);
            for (Integer id : roleIds) jdbc.update("DELETE FROM roles WHERE id = ?", id);
        });
    }

    private Role role(Integer limit) {
        Role role = new Role(); role.setName("capacity-test-" + UUID.randomUUID());
        role.setActive(true); role.setMaxUsers(limit);
        Role saved = tx.execute(status -> roles.saveAndFlush(role)); roleIds.add(saved.getId()); return saved;
    }

    private User user(Role role, boolean active) {
        User user = new User(); user.setRole(role); user.setActive(active);
        user.setFirstName("Capacity"); user.setLastName("Test"); user.setCreatedAt(LocalDateTime.now());
        user.setEmail(UUID.randomUUID()+"@capacity.test"); user.setPasswordHash("unused");
        User saved = tx.execute(status -> users.saveAndFlush(user)); userIds.add(saved.getId()); return saved;
    }

    private void authenticate() {
        AuthenticatedUser principal = new AuthenticatedUser(manager, Set.of("USER_MANAGE"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test void arbitraryLimitsCountOnlyActiveUsersAndAllowSameRole() {
        Role limited = role(2), unlimited = role(null);
        User first = user(unlimited, true), second = user(unlimited, true), inactive = user(unlimited, false);
        service.changeRole(first.getId(), limited.getId(), null);
        service.changeRole(second.getId(), limited.getId(), null);
        service.changeRole(first.getId(), limited.getId(), null);
        service.changeRole(inactive.getId(), limited.getId(), null);
        assertThat(users.countByRole_IdAndActiveTrue(limited.getId())).isEqualTo(2);
        assertThatThrownBy(() -> service.setActive(inactive.getId(), true)).isInstanceOf(AdminLimitExceededException.class);
        service.changeRole(first.getId(), unlimited.getId(), null);
        service.setActive(inactive.getId(), true);
        assertThat(users.countByRole_IdAndActiveTrue(limited.getId())).isEqualTo(2);
    }

    @Test void inactiveRoleRejectsBothAssignmentAndReactivation() {
        Role inactiveRole = role(1);
        User inactive = user(inactiveRole, false), candidate = user(role(null), true);
        tx.executeWithoutResult(s -> jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", inactiveRole.getId()));
        assertThatThrownBy(() -> service.changeRole(candidate.getId(), inactiveRole.getId(), null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.setActive(inactive.getId(), true)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void simultaneousAssignmentAndActivationCannotOverbookLastSeat() throws Exception {
        Role limited = role(1);
        User candidate = user(role(null), true), inactive = user(limited, false);
        CyclicBarrier start = new CyclicBarrier(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> assign = executor.submit(() -> attempt(start, () -> service.changeRole(candidate.getId(), limited.getId(), null)));
            Future<Boolean> activate = executor.submit(() -> attempt(start, () -> service.setActive(inactive.getId(), true)));
            assertThat(List.of(assign.get(15, TimeUnit.SECONDS), activate.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(users.countByRole_IdAndActiveTrue(limited.getId())).isEqualTo(1);
    }

    private boolean attempt(CyclicBarrier start, Runnable operation) throws Exception {
        authenticate();
        try {
            start.await(5, TimeUnit.SECONDS);
            operation.run();
            return true;
        } catch (AdminLimitExceededException expected) {
            return false;
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test void auditFailureRollsBackAssignmentAndReleasesCapacity() {
        Role limited = role(1), original = role(null);
        User candidate = user(original, true), next = user(original, true);
        doThrow(new IllegalStateException("audit failure")).when(audit).logIslem(eq(candidate.getId()), any(),
                eq("ROLE_CHANGED"), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> service.changeRole(candidate.getId(), limited.getId(), null))
                .isInstanceOf(IllegalStateException.class).hasMessage("audit failure");
        assertThat(users.findById(candidate.getId()).orElseThrow().getRole().getId()).isEqualTo(original.getId());
        assertThat(users.countByRole_IdAndActiveTrue(limited.getId())).isZero();
        service.changeRole(next.getId(), limited.getId(), null);
        assertThat(users.countByRole_IdAndActiveTrue(limited.getId())).isEqualTo(1);
    }

    @Test void failureAfterTaskTransferRollsBackBothUsersRecordsAndAudit() {
        Role deputyRole = roles.findBySystemKey("BASKAN_YARDIMCISI").orElseThrow();
        Role employeeRole = roles.findBySystemKey("CALISAN").orElseThrow();
        User deputy = user(deputyRole, true), replacement = user(employeeRole, true);
        Role destination = role(null);
        Record record = tx.execute(status -> records.saveAndFlush(Record.builder()
                .title("Atomic handoff").description("Rollback verification")
                .categoryId(jdbc.queryForObject("SELECT min(id) FROM categories", Integer.class))
                .createdBy(replacement.getId()).assignedTo(deputy.getId()).lastDeputyId(deputy.getId())
                .status(RecordStatus.BSK_YRD_INCELEMESINDE).build()));
        recordIds.add(record.getId());
        doAnswer(invocation -> {
            // This audit call happens after both role writes and both record transfer statements.
            assertThat(jdbc.queryForObject("SELECT assigned_to FROM records WHERE id = ?", UUID.class, record.getId()))
                    .isEqualTo(replacement.getId());
            throw new IllegalStateException("handoff audit failure");
        }).when(audit).logIslem(eq(replacement.getId()), any(), eq("TASKS_REASSIGNED"), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> service.changeRole(deputy.getId(), destination.getId(), replacement.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessage("handoff audit failure");
        assertThat(users.findById(deputy.getId()).orElseThrow().getRole().getId()).isEqualTo(deputyRole.getId());
        assertThat(users.findById(replacement.getId()).orElseThrow().getRole().getId()).isEqualTo(employeeRole.getId());
        Record unchanged = records.findById(record.getId()).orElseThrow();
        assertThat(unchanged.getAssignedTo()).isEqualTo(deputy.getId());
        assertThat(unchanged.getLastDeputyId()).isEqualTo(deputy.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_audit_logs WHERE performed_by = ?", Integer.class, manager.getId())).isZero();
    }
}
