package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real PostgreSQL and JWT filter chain; no mocked principal, role or workflow port. */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Transactional
class DynamicWorkflowRoleIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtUtil jwt;
    @Autowired private EntityManager em;
    @Autowired private TransitionRuleSource rules;
    @Autowired private ApplicationEvents events;

    private List<TransitionRule> originalRules;
    private Integer actorRoleId;
    private Integer targetRoleId;
    private UUID actorId;
    private UUID targetId;
    private UUID recordId;
    private String token;

    @BeforeEach
    void configureDynamicRoles() {
        originalRules = rules.all();
        actorRoleId = insertRole(true);
        targetRoleId = insertRole(false);
        actorId = insertUser(actorRoleId);
        targetId = insertUser(targetRoleId);
        jdbc.update("INSERT INTO role_permissions(role_id, permission_id) "
                + "SELECT ?, id FROM permissions WHERE code = 'RECORD_FORWARD'", actorRoleId);
        jdbc.update("""
                INSERT INTO workflow_transitions
                    (from_status_id, action_id, actor_role_id, actor_requirement, to_status_id,
                     expected_target_role_id, target_strategy, required_permission_id)
                SELECT fs.id, a.id, ?, 'CREATOR', ts.id, ?, 'ROLE', p.id
                FROM workflow_statuses fs, workflow_actions a, workflow_statuses ts, permissions p
                WHERE fs.name = 'TASLAK' AND a.name = 'GONDER'
                  AND ts.name = 'BSK_YRD_INCELEMESINDE' AND p.code = 'RECORD_FORWARD'
                """, actorRoleId, targetRoleId);
        recordId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO records (id, title, description, category_id, status, created_by, version)
                VALUES (?, 'Dynamic workflow', 'Role ID acceptance',
                        (SELECT min(id) FROM categories), 'TASLAK', ?, 0)
                """, recordId, actorId);
        token = jwt.generateAccessToken(actorId, email(actorId), "display-only-role-claim");
        reload();
    }

    // Database rollback alone cannot undo the shared in-memory rule snapshot.
    @AfterTransaction
    void restoreSnapshotAfterRollback() {
        reload();
        assertThat(rules.all()).containsExactlyElementsOf(originalRules);
    }

    @Test
    void dynamicActorAndTargetCompleteTransitionThroughJwtAndPersistRoleId() throws Exception {
        assertThat(jdbc.queryForObject("SELECT system_key FROM roles WHERE id = ?",
                String.class, actorRoleId)).isNull();
        assertThat(jdbc.queryForObject("SELECT system_key FROM roles WHERE id = ?",
                String.class, targetRoleId)).isNull();

        perform().andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("BSK_YRD_INCELEMESINDE"));
        em.flush();
        em.clear();
        var record = jdbc.queryForMap("SELECT status, assigned_to, last_deputy_id, version "
                + "FROM records WHERE id = ?", recordId);
        assertThat(record.get("status")).isEqualTo("BSK_YRD_INCELEMESINDE");
        assertThat(record.get("assigned_to")).isEqualTo(targetId);
        assertThat(record.get("last_deputy_id")).isNull();
        assertThat(record.get("version")).isEqualTo(1);
        var audit = jdbc.queryForMap("SELECT user_id, role_id, action FROM audit_logs "
                + "WHERE record_id = ?", recordId);
        assertThat(audit.get("user_id")).isEqualTo(actorId);
        assertThat(audit.get("role_id")).isEqualTo(actorRoleId);
        assertThat(audit.get("action")).isEqualTo("GONDER");
        assertThat(events.stream(WorkflowStatusChangedEvent.class).toList())
                .singleElement().satisfies(event -> {
                    assertThat(event.actorRoleId()).isEqualTo(new RoleId(actorRoleId));
                    assertThat(event.assignedTo()).isEqualTo(targetId);
                });
    }

    @ParameterizedTest
    @CsvSource({
            "permission, 403, WORKFLOW_FORBIDDEN",
            "workflowActor, 403, WORKFLOW_ROLE_NOT_ALLOWED",
            "creator, 403, WORKFLOW_FORBIDDEN",
            "transition, 400, WORKFLOW_INVALID_TRANSITION"
    })
    void dynamicRoleStillRequiresAllWorkflowGates(String missing, int httpStatus, String code) throws Exception {
        switch (missing) {
            case "permission" -> jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", actorRoleId);
            case "workflowActor" -> jdbc.update("UPDATE roles SET is_workflow_actor = false WHERE id = ?", actorRoleId);
            case "creator" -> jdbc.update("UPDATE records SET created_by = ? WHERE id = ?", targetId, recordId);
            case "transition" -> {
                jdbc.update("UPDATE workflow_transitions SET is_active = false WHERE actor_role_id = ?", actorRoleId);
                reload();
            }
            default -> throw new IllegalArgumentException(missing);
        }
        perform().andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code));
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactiveUser", "inactiveRole", "multipleUsers"})
    void roleStrategyStillRequiresExactlyOneActiveUserWithAnActiveRole(String invalid) throws Exception {
        switch (invalid) {
            case "inactiveUser" -> jdbc.update("UPDATE users SET is_active = false WHERE id = ?", targetId);
            case "inactiveRole" -> jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", targetRoleId);
            case "multipleUsers" -> insertUser(targetRoleId);
            default -> throw new IllegalArgumentException(invalid);
        }
        perform().andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_ROLE_NOT_CONFIGURED"));
        assertUnchanged();
    }

    @Test
    void creatorStrategyRejectsDifferentRoleId() throws Exception {
        jdbc.update("UPDATE workflow_transitions SET target_strategy = 'CREATOR' WHERE actor_role_id = ?",
                actorRoleId);
        reload();
        perform().andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_ROLE_INVALID"));
        assertUnchanged();
    }

    @Test
    void workflowPermissionAloneDoesNotGrantRecordVisibility() throws Exception {
        mvc.perform(get("/api/audit-logs/record/{recordId}", recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void databaseRejectsUnknownActorRoleForeignKey() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM roles WHERE id = ?", Integer.class,
                Integer.MAX_VALUE)).isZero();
        assertThatThrownBy(() -> jdbc.update("UPDATE workflow_transitions SET actor_role_id = ? "
                        + "WHERE actor_role_id = ?", Integer.MAX_VALUE, actorRoleId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_transition_actor_role");
        // PostgreSQL has aborted this transaction; the test framework rolls it back next.
    }

    private Integer insertRole(boolean workflowActor) {
        return jdbc.queryForObject("INSERT INTO roles(name, is_active, is_workflow_actor) VALUES (?, true, ?) RETURNING id",
                Integer.class, "wf2d2-" + UUID.randomUUID(), workflowActor);
    }

    private UUID insertUser(Integer roleId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, 'Dynamic', 'Workflow', ?, 'unused', ?, true)", id, email(id), roleId);
        return id;
    }

    private static String email(UUID id) { return id + "@wf2d2.test"; }

    private ResultActions perform() throws Exception {
        em.clear(); // The JWT filter must read current database role state and permissions.
        return mvc.perform(post("/api/records/{recordId}/workflow/actions", recordId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"action\":\"GONDER\"}"));
    }

    private void assertUnchanged() {
        assertThat(jdbc.queryForMap("SELECT status, assigned_to, version FROM records WHERE id = ?", recordId))
                .containsEntry("status", "TASLAK").containsEntry("assigned_to", null).containsEntry("version", 0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE record_id = ?", Integer.class,
                recordId)).isZero();
        assertThat(events.stream(WorkflowStatusChangedEvent.class)).isEmpty();
    }

    private void reload() { ((ReloadableTransitionRuleSource) rules).reload(); }
}
