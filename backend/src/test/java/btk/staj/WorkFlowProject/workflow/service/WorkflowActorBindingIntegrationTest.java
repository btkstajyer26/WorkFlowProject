package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.workflow.adapter.JpaTransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActorBindingView;
import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowBindingException;
import btk.staj.WorkFlowProject.workflow.port.WorkflowEventPublisher;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static btk.staj.WorkFlowProject.workflow.exception.WorkflowBindingException.Reason.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real commits are essential: an enclosing rollback-only test cannot verify publication. */
@SpringBootTest
class WorkflowActorBindingIntegrationTest {
    @Autowired WorkflowActorBindingService bindings;
    @Autowired WorkflowActionService workflow;
    @Autowired ReloadableTransitionRuleSource rules;
    @Autowired JdbcTemplate jdbc;
    @Autowired CustomUserDetailsService users;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean JpaTransitionRuleRecordReader reader;
    @MockitoBean WorkflowEventPublisher events;

    private final List<Integer> roleIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> recordIds = new ArrayList<>();
    private List<TransitionRule> originalRules;
    private List<Map<String, Object>> originalSystemBindings;
    private Integer roleId;
    private Integer managerRoleId;
    private UUID actorId;
    private UUID managerId;

    @BeforeEach
    void setup() {
        originalRules = rules.all();
        originalSystemBindings = jdbc.queryForList("SELECT t.* FROM workflow_transitions t JOIN roles r "
                + "ON r.id = t.actor_role_id WHERE r.is_system OR r.system_key IS NOT NULL ORDER BY t.id");
        roleId = role("RECORD_VIEW", "RECORD_FORWARD", "RECORD_APPROVE", "RECORD_RETURN");
        managerRoleId = role("WORKFLOW_VIEW", "WORKFLOW_MANAGE");
        actorId = user(roleId);
        managerId = user(managerRoleId);
        login(managerId);
    }

    @AfterEach
    void cleanup() {
        reset(reader);
        SecurityContextHolder.clearContext();
        for (UUID id : recordIds) {
            jdbc.update("DELETE FROM audit_logs WHERE record_id = ?", id);
            jdbc.update("DELETE FROM records WHERE id = ?", id);
        }
        for (UUID id : userIds) {
            jdbc.update("DELETE FROM audit_logs WHERE user_id = ?", id);
            jdbc.update("DELETE FROM user_audit_logs WHERE performed_by = ? OR target_user_id = ?", id, id);
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        for (Integer id : roleIds) {
            jdbc.update("DELETE FROM workflow_transitions WHERE actor_role_id = ?", id);
            jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", id);
            jdbc.update("DELETE FROM roles WHERE id = ?", id);
        }
        rules.reload();
        assertThat(rules.all()).containsExactlyElementsOf(originalRules);
        assertThat(jdbc.queryForList("SELECT t.* FROM workflow_transitions t JOIN roles r "
                + "ON r.id = t.actor_role_id WHERE r.is_system OR r.system_key IS NOT NULL ORDER BY t.id"))
                .containsExactlyElementsOf(originalSystemBindings);
    }

    @Test
    void copiesOnlyActorBindingAndReactivatesSameIdWithTransactionalAudit() {
        int template = template("TASLAK", "GONDER");
        WorkflowActorBindingView result = bindings.bind(template, roleId);
        Map<String, Object> expected = row(template);
        expected.put("id", result.bindingId());
        expected.put("actor_role_id", roleId);
        assertThat(row(result.bindingId())).isEqualTo(expected);
        assertThat(rules.find(RecordStatus.TASLAK, WorkflowAction.GONDER, new RoleId(roleId))).isPresent();
        assertReason(() -> bindings.bind(template, roleId), DUPLICATE_BINDING);
        assertThat(bindings.listTransitions()).contains(result);

        assertThat(bindings.unbind(result.bindingId()).active()).isFalse();
        long count = auditCount();
        assertThat(bindings.unbind(result.bindingId()).active()).isFalse();
        assertThat(auditCount()).isEqualTo(count);
        assertThat(bindings.bind(template, roleId).bindingId()).isEqualTo(result.bindingId());
        assertThat(auditCount()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT comment FROM user_audit_logs WHERE performed_by = ? "
                + "ORDER BY created_at DESC LIMIT 1", String.class, managerId))
                .contains("templateTransitionId=" + template, "bindingId=" + result.bindingId(),
                        "boundRoleId=" + roleId, "previousActive=false;active=true");
    }

    @Test
    void adminCanManageBindingsWithoutRecordVisibility() {
        int adminRole = jdbc.queryForObject("SELECT id FROM roles WHERE system_key = 'ADMIN'", Integer.class);
        jdbc.update("UPDATE users SET role_id = ? WHERE id = ?", adminRole, managerId);
        login(managerId);
        bindings.bind(template("TASLAK", "GONDER"), roleId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE user_id = ? "
                + "AND action = 'WORKFLOW_BINDING_ENABLED' AND record_id IS NULL", Integer.class, managerId)).isOne();
    }

    @Test
    void permissionsAreCheckedAtServiceBoundary() {
        revoke(managerRoleId, "WORKFLOW_MANAGE");
        login(managerId);
        assertThat(bindings.listTransitions()).isNotEmpty();
        assertThatThrownBy(() -> bindings.bind(template("TASLAK", "GONDER"), roleId))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> bindings.unbind(template("TASLAK", "GONDER")))
                .isInstanceOf(AccessDeniedException.class);
        revoke(managerRoleId, "WORKFLOW_VIEW");
        login(managerId);
        assertThatThrownBy(bindings::listTransitions).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "role"})
    void inactiveManagementIdentityCannotUseService(String inactive) {
        if (inactive.equals("user")) jdbc.update("UPDATE users SET is_active = false WHERE id = ?", managerId);
        else jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", managerRoleId);
        login(managerId);
        assertThatThrownBy(bindings::listTransitions).isInstanceOf(DisabledException.class);
        assertThatThrownBy(() -> bindings.bind(template("TASLAK", "GONDER"), roleId))
                .isInstanceOf(DisabledException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactive", "notActor", "system", "RECORD_VIEW", "RECORD_FORWARD"})
    void rejectsIneligibleBoundRoles(String invalid) {
        switch (invalid) {
            case "inactive" -> jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", roleId);
            case "notActor" -> jdbc.update("UPDATE roles SET is_workflow_actor = false WHERE id = ?", roleId);
            case "system" -> { /* Use a valid built-in role; DB forbids inconsistent system metadata. */ }
            default -> revoke(roleId, invalid);
        }
        int selectedRole = invalid.equals("system")
                ? jdbc.queryForObject("SELECT id FROM roles WHERE system_key = 'CALISAN'", Integer.class) : roleId;
        assertReason(() -> bindings.bind(template("TASLAK", "GONDER"), selectedRole),
                invalid.startsWith("RECORD_") ? MISSING_ROLE_PERMISSION : INVALID_ROLE);
    }

    @Test
    void protectsSystemBindingsAndReportsMissingResources() {
        int template = template("TASLAK", "GONDER");
        assertReason(() -> bindings.unbind(template), PROTECTED_BINDING);
        assertReason(() -> bindings.bind(Integer.MAX_VALUE, roleId), TEMPLATE_NOT_FOUND);
        assertReason(() -> bindings.bind(template, Integer.MAX_VALUE), ROLE_NOT_FOUND);
        assertReason(() -> bindings.unbind(Integer.MAX_VALUE), BINDING_NOT_FOUND);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rejectsMalformedIdentifiers(Integer id) {
        assertReason(() -> bindings.bind(id, roleId), INVALID_ID);
        assertReason(() -> bindings.bind(template("TASLAK", "GONDER"), id), INVALID_ID);
        assertReason(() -> bindings.unbind(id), INVALID_ID);
    }

    @Test
    void doesNotOverwriteDifferentInactiveMetadata() {
        int template = template("TASLAK", "GONDER");
        int id = bindings.bind(template, roleId).bindingId();
        bindings.unbind(id);
        jdbc.update("UPDATE workflow_transitions SET actor_requirement = 'ASSIGNEE' WHERE id = ?", id);
        assertReason(() -> bindings.bind(template, roleId), METADATA_MISMATCH);
        assertThat(row(id)).containsEntry("is_active", false).containsEntry("actor_requirement", "ASSIGNEE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactive", "permission", "strategy", "terminal"})
    void rejectsInvalidTemplateWithoutChangingTheOriginalGraph(String invalid) {
        int id = bindings.bind(template("TASLAK", "GONDER"), roleId).bindingId();
        switch (invalid) {
            case "inactive" -> jdbc.update("UPDATE workflow_transitions SET is_active = false WHERE id = ?", id);
            case "permission" -> jdbc.update("UPDATE workflow_transitions SET required_permission_id = NULL WHERE id = ?", id);
            // DB allows this combination; domain requires a target role for non-NONE strategies.
            case "strategy" -> jdbc.update("UPDATE workflow_transitions SET target_strategy = 'CREATOR', "
                    + "expected_target_role_id = NULL WHERE id = ?", id);
            case "terminal" -> jdbc.update("UPDATE workflow_transitions SET from_status_id = "
                    + "(SELECT id FROM workflow_statuses WHERE name = 'ONAYLANDI') WHERE id = ?", id);
        }
        int otherRole = role("RECORD_VIEW", "RECORD_FORWARD");
        assertReason(() -> bindings.bind(id, otherRole), INVALID_TEMPLATE);
    }

    @ParameterizedTest
    @CsvSource({"TASLAK,GONDER,CREATOR", "BSK_YRD_INCELEMESINDE,BASKANA_ILET,ASSIGNEE",
            "DUZENLEME_BEKLIYOR,TEKRAR_GONDER,CREATOR_AND_ASSIGNEE"})
    void blocksRemovalForEachRelationshipEvenIfActorIsTemporarilyDisabled(String status, String action, String relation) {
        int id = bindings.bind(template(status, action), roleId).bindingId();
        record(status, relation.equals("ASSIGNEE") ? managerId : actorId,
                relation.equals("CREATOR") ? managerId : actorId);
        jdbc.update("UPDATE users SET is_active = false WHERE id = ?", actorId);
        jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", roleId);
        revoke(roleId, "RECORD_FORWARD");
        assertReason(() -> bindings.unbind(id), BINDING_IN_USE);
        assertThat(row(id)).containsEntry("is_active", true);
    }

    @Test
    void anotherActionDoesNotPermitRemoval() {
        int id = bindings.bind(template("BSK_YRD_INCELEMESINDE", "BASKANA_ILET"), roleId).bindingId();
        bindings.bind(template("BSK_YRD_INCELEMESINDE", "CALISANA_GERI_GONDER"), roleId);
        record("BSK_YRD_INCELEMESINDE", managerId, actorId);
        assertReason(() -> bindings.unbind(id), BINDING_IN_USE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"deleted", "terminal", "creatorOnly", "assigneeOnly", "differentActorSameRole", "unrelated"})
    void removalExcludesRecordsThatCannotUseTheBinding(String kind) {
        int id = bindings.bind(template("DUZENLEME_BEKLIYOR", "TEKRAR_GONDER"), roleId).bindingId();
        UUID creator = kind.equals("assigneeOnly") || kind.equals("unrelated") ? managerId : actorId;
        UUID assignee = switch (kind) {
            case "creatorOnly", "unrelated" -> managerId;
            case "differentActorSameRole" -> user(roleId);
            default -> actorId;
        };
        UUID record = record(kind.equals("terminal") ? "ONAYLANDI" : "DUZENLEME_BEKLIYOR", creator, assignee);
        if (kind.equals("deleted")) jdbc.update("UPDATE records SET deleted_at = NOW() WHERE id = ?", record);
        assertThat(bindings.unbind(id).active()).isFalse();
    }

    @Test
    void dynamicActorUsesNewBindingAndLosesItAfterSafeRemoval() {
        UUID first = record("BASKAN_INCELEMESINDE", managerId, actorId);
        login(actorId);
        assertThatThrownBy(() -> workflow.performAction(first, new WorkflowActionRequest(WorkflowAction.ONAYLA, null, null)))
                .isInstanceOf(WorkflowApplicationException.class);
        login(managerId);
        int id = bindings.bind(template("BASKAN_INCELEMESINDE", "ONAYLA"), roleId).bindingId();
        assertReason(() -> bindings.unbind(id), BINDING_IN_USE);
        revoke(roleId, "RECORD_APPROVE");
        login(actorId);
        assertThatThrownBy(() -> workflow.performAction(first, new WorkflowActionRequest(WorkflowAction.ONAYLA, null, null)))
                .isInstanceOfSatisfying(WorkflowApplicationException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(
                                btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode.WORKFLOW_FORBIDDEN));
        jdbc.update("INSERT INTO role_permissions(role_id, permission_id) SELECT ?, id FROM permissions "
                + "WHERE code = 'RECORD_APPROVE'", roleId);
        login(actorId);
        workflow.performAction(first, new WorkflowActionRequest(WorkflowAction.ONAYLA, null, null));
        assertThat(jdbc.queryForObject("SELECT status FROM records WHERE id = ?", String.class, first)).isEqualTo("ONAYLANDI");
        login(managerId);
        bindings.unbind(id);
        UUID second = record("BASKAN_INCELEMESINDE", managerId, actorId);
        login(actorId);
        assertThatThrownBy(() -> workflow.performAction(second, new WorkflowActionRequest(WorkflowAction.ONAYLA, null, null)))
                .isInstanceOf(WorkflowApplicationException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM records WHERE id = ?", String.class, second))
                .isEqualTo("BASKAN_INCELEMESINDE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"snapshot", "commit"})
    void failedPreparationOrCommitRollsBackBindingAndAuditAndKeepsSnapshot(String failure) {
        var before = rules.snapshot();
        doAnswer(invocation -> {
            if (failure.equals("snapshot")) throw new TransitionRuleConfigurationException("forced invalid snapshot");
            Object result = invocation.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) { throw new IllegalStateException("forced commit failure"); }
            });
            return result;
        }).when(reader).findAllActive();
        assertThatThrownBy(() -> bindings.bind(template("TASLAK", "GONDER"), roleId)).isInstanceOf(RuntimeException.class);
        assertThat(rules.snapshot()).isSameAs(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_transitions WHERE actor_role_id = ?",
                Integer.class, roleId)).isZero();
        assertThat(auditCount()).isZero();
    }

    @Test
    void refusesAmbientTransactionInsteadOfPublishingUncommittedRules() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status ->
                bindings.bind(template("TASLAK", "GONDER"), roleId)))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(auditCount()).isZero();
    }

    @Test
    void localRollbackOnlyCannotPublishEvenWhenTransactionTemplateReturnsNormally() {
        var before = rules.snapshot();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager) {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> callback) {
                return super.execute(status -> {
                    T value = callback.doInTransaction(status);
                    status.setRollbackOnly();
                    return value;
                });
            }
        };
        // Exercise the coordinator's transaction contract with a real PostgreSQL rollback.
        assertThatThrownBy(() -> rules.updateAndReload(transaction, () -> {
            jdbc.update("INSERT INTO workflow_transitions(from_status_id, action_id, actor_role_id, "
                    + "actor_requirement, to_status_id, expected_target_role_id, target_strategy, required_permission_id) "
                    + "SELECT from_status_id, action_id, ?, actor_requirement, to_status_id, expected_target_role_id, "
                    + "target_strategy, required_permission_id FROM workflow_transitions WHERE id = ?",
                    roleId, template("TASLAK", "GONDER"));
            return "not committed";
        })).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(rules.snapshot()).isSameAs(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_transitions WHERE actor_role_id = ?",
                Integer.class, roleId)).isZero();
    }

    @Test
    void concurrentDuplicateIsRejectedAndManualReloadCannotOverwriteCommittedMutation() throws Exception {
        int template = template("TASLAK", "GONDER");
        int secondRole = role("RECORD_VIEW", "RECORD_FORWARD");
        var old = rules.snapshot();
        CountDownLatch prepared = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (first.getAndSet(false)) {
                prepared.countDown();
                if (!commit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("commit latch timed out");
            }
            return result;
        }).when(reader).findAllActive();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var create = executor.submit(() -> {
                login(managerId);
                try { return bindings.bind(template, roleId); }
                finally { SecurityContextHolder.clearContext(); }
            });
            try {
                assertThat(prepared.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(rules.snapshot()).isSameAs(old);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_transitions WHERE actor_role_id = ?",
                        Integer.class, roleId)).isZero();
                var reload = executor.submit(rules::reload);
                var duplicate = executor.submit(() -> {
                    login(managerId);
                    try { assertReason(() -> bindings.bind(template, roleId), DUPLICATE_BINDING); }
                    finally { SecurityContextHolder.clearContext(); }
                });
                var second = executor.submit(() -> {
                    login(managerId);
                    try { return bindings.bind(template, secondRole); }
                    finally { SecurityContextHolder.clearContext(); }
                });
                assertThatThrownBy(() -> reload.get(100, TimeUnit.MILLISECONDS))
                        .isInstanceOf(java.util.concurrent.TimeoutException.class);
                commit.countDown();
                assertThat(create.get(10, TimeUnit.SECONDS).active()).isTrue();
                reload.get(10, TimeUnit.SECONDS);
                duplicate.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
                assertThat(rules.all()).hasSize(originalRules.size() + 2);
                assertThat(auditCount()).isEqualTo(2);
            } finally { commit.countDown(); }
        }
    }

    private int template(String status, String action) {
        return jdbc.queryForObject("SELECT t.id FROM workflow_transitions t JOIN roles r ON r.id = t.actor_role_id "
                + "JOIN workflow_statuses s ON s.id = t.from_status_id JOIN workflow_actions a ON a.id = t.action_id "
                + "WHERE r.is_system = true AND s.name = ? AND a.name = ?", Integer.class, status, action);
    }

    private int role(String... permissions) {
        int id = jdbc.queryForObject("INSERT INTO roles(name, is_active, is_workflow_actor) "
                + "VALUES (?, true, true) RETURNING id", Integer.class, "wf8-" + UUID.randomUUID());
        roleIds.add(id);
        for (String permission : permissions) jdbc.update("INSERT INTO role_permissions(role_id, permission_id) "
                + "SELECT ?, id FROM permissions WHERE code = ?", id, permission);
        return id;
    }

    private UUID user(int role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, 'WF8', 'Fixture', ?, 'unused', ?, true)", id, id + "@wf8.test", role);
        userIds.add(id);
        return id;
    }

    private UUID record(String status, UUID creator, UUID assignee) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO records(id, title, description, category_id, status, created_by, assigned_to, version) "
                + "VALUES (?, 'WF8 fixture', 'WF8 acceptance', (SELECT min(id) FROM categories), ?, ?, ?, 0)",
                id, status, creator, assignee);
        recordIds.add(id);
        return id;
    }

    private void login(UUID id) {
        var principal = users.loadUserByUsername(id + "@wf8.test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private void revoke(int role, String permission) {
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ? AND permission_id = "
                + "(SELECT id FROM permissions WHERE code = ?)", role, permission);
    }

    private Map<String, Object> row(int id) {
        return jdbc.queryForMap("SELECT * FROM workflow_transitions WHERE id = ?", id);
    }

    private long auditCount() {
        return jdbc.queryForObject("SELECT count(*) FROM user_audit_logs WHERE performed_by = ? "
                + "AND action LIKE 'WORKFLOW_BINDING_%'", Long.class, managerId);
    }

    private static void assertReason(Runnable operation, WorkflowBindingException.Reason reason) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(WorkflowBindingException.class,
                ex -> assertThat(ex.reason()).isEqualTo(reason));
    }
}
