package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.adapter.RecordPortAdapter;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL routing, HTTP contract, visibility parity and competing transactions. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepartmentWorkflowIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired RecordRepository records;
    @Autowired UserRepository users;
    @Autowired RolePermissionRepository permissions;
    @Autowired RecordAccessPolicy policy;
    @Autowired ReloadableTransitionRuleSource rules;
    @Autowired WorkflowTransitionRepository transitions;
    @Autowired JwtUtil jwt;
    @MockitoBean MailService mail;
    @MockitoSpyBean RecordPortAdapter recordPort;

    private String prefix;
    private int role;
    private int wrongRole;
    private int department;
    private UUID creator;
    private UUID first;
    private UUID second;
    private UUID outsider;
    private UUID wrongMember;

    @BeforeEach
    void fixture() {
        prefix = "wf-dept-" + UUID.randomUUID();
        role = insertRole("eligible");
        wrongRole = insertRole("wrong");
        creator = insertUser(jdbc.queryForObject("SELECT id FROM roles WHERE system_key = 'CALISAN'", Integer.class));
        first = insertUser(role);
        second = insertUser(role);
        outsider = insertUser(role);
        wrongMember = insertUser(wrongRole);
        department = jdbc.queryForObject("INSERT INTO departments(name, is_active) VALUES (?, true) RETURNING id", Integer.class, prefix);
        for (var id : List.of(first, second, wrongMember)) {
            jdbc.update("INSERT INTO department_members(department_id, user_id) VALUES (?, ?)", department, id);
        }
        jdbc.update("""
                INSERT INTO department_routing_rules(department_id, from_status_id, action_id, target_role_id, is_active)
                SELECT ?, s.id, a.id, ?, true FROM workflow_statuses s, workflow_actions a
                WHERE s.name = 'BSK_YRD_INCELEMESINDE' AND a.name = 'CALISANA_GERI_GONDER'
                """, department, role);
        rules.reload();
    }

    private int insertRole(String suffix) {
        int id = jdbc.queryForObject("INSERT INTO roles(name, is_active, is_workflow_actor) VALUES (?, true, true) RETURNING id",
                Integer.class, prefix + suffix);
        jdbc.update("INSERT INTO role_permissions(role_id, permission_id) SELECT ?, id FROM permissions WHERE is_active = true", id);
        jdbc.update("""
                INSERT INTO workflow_transitions(from_status_id, action_id, actor_role_id, actor_requirement,
                    to_status_id, expected_target_role_id, target_strategy, required_permission_id, is_active)
                SELECT t.from_status_id, t.action_id, ?, t.actor_requirement, t.to_status_id,
                    t.expected_target_role_id, t.target_strategy, t.required_permission_id, true
                FROM workflow_transitions t JOIN roles r ON r.id = t.actor_role_id
                JOIN workflow_actions a ON a.id = t.action_id
                WHERE r.system_key = 'BASKAN_YARDIMCISI' AND a.name = 'CALISANA_GERI_GONDER'
                """, id);
        return id;
    }

    @AfterTransaction
    void restoreSnapshot() { rules.reload(); }

    @AfterEach
    void cleanCommittedConcurrencyFixture() {
        if (TestTransaction.isActive()) return;
        // Only this test's UUID-prefixed fixtures; ordinary tests roll back automatically.
        var ids = jdbc.queryForList("SELECT id FROM users WHERE email LIKE ?", UUID.class, prefix + "%");
        var recordIds = jdbc.queryForList("SELECT id FROM records WHERE title = ?", UUID.class, prefix);
        for (UUID id : recordIds) {
            jdbc.update("DELETE FROM notifications WHERE record_id = ?", id);
            jdbc.update("DELETE FROM audit_logs WHERE record_id = ?", id);
            jdbc.update("DELETE FROM records WHERE id = ?", id);
        }
        for (UUID id : ids) {
            jdbc.update("DELETE FROM user_audit_logs WHERE target_user_id = ? OR performed_by = ?", id, id);
            jdbc.update("DELETE FROM audit_logs WHERE user_id = ?", id);
            jdbc.update("DELETE FROM notifications WHERE user_id = ?", id);
            jdbc.update("DELETE FROM tokens WHERE user_id = ?", id);
            jdbc.update("DELETE FROM department_members WHERE user_id = ?", id);
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM department_routing_rules WHERE department_id = ?", department);
        jdbc.update("DELETE FROM departments WHERE id = ?", department);
        for (int id : List.of(role, wrongRole)) {
            jdbc.update("DELETE FROM workflow_transitions WHERE actor_role_id = ?", id);
            jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", id);
            jdbc.update("DELETE FROM roles WHERE id = ?", id);
        }
        rules.reload();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TASLAK", "DUZENLEME_BEKLIYOR"})
    void sendAssignsOnlyDepartmentAndEligibleMemberCanReturnToCreator(String statusName) throws Exception {
        UUID id = record(statusName, null, statusName.equals("TASLAK") ? null : creator);
        send(id, department).andExpect(status().isOk());
        em.flush();
        em.clear();
        var sent = records.findById(id).orElseThrow();
        assertThat(sent.getAssignedDepartmentId()).isEqualTo(department);
        assertThat(sent.getAssignedTo()).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE record_id = ? AND action = 'DEPARTMANA_GONDER'",
                Integer.class, id)).isOne();
        act(id, first).andExpect(status().isOk()).andExpect(jsonPath("$.assignedTo").value(creator.toString()));
        em.flush();
        em.clear();
        assertThat(records.findById(id).orElseThrow().getAssignedDepartmentId()).isNull();
        assertThat(records.findById(id).orElseThrow().getAssignedTo()).isEqualTo(creator);
    }

    @Test
    void requestTargetsAreExclusiveAndWrongOrMissingFieldsHaveStableCodes() throws Exception {
        UUID id = record("TASLAK", null, null);
        request(id, creator, "{\"action\":\"DEPARTMANA_GONDER\",\"targetUserId\":\"" + first
                + "\",\"targetDepartmentId\":" + department + "}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        request(id, creator, "{\"action\":\"DEPARTMANA_GONDER\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_REQUIRED"));
        request(id, creator, "{\"action\":\"DEPARTMANA_GONDER\",\"targetUserId\":\"" + first + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_NOT_ALLOWED"));
        request(id, creator, "{\"action\":\"GONDER\",\"targetDepartmentId\":" + department + "}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_NOT_ALLOWED"));
        assertThat(jdbc.queryForObject("SELECT version FROM records WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void absentAndInactiveDepartmentsAreInvalid() throws Exception {
        UUID id = record("TASLAK", null, null);
        send(id, Integer.MAX_VALUE).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKFLOW_DEPARTMENT_INVALID"));
        jdbc.update("UPDATE departments SET is_active = false WHERE id = ?", department);
        send(id, department).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKFLOW_DEPARTMENT_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"routing", "wrong_status", "members", "role", "workflow_actor", "permission", "view", "transition"})
    void sendRejectsUnusableLandingRouting(String missing) throws Exception {
        disable(missing);
        send(record("TASLAK", null, null), department).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED"));
    }

    @Test
    void membershipRoleAndRoutingAreRequiredBeforeTargetResolution() throws Exception {
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);
        act(id, outsider).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WORKFLOW_FORBIDDEN"));
        act(id, wrongMember).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WORKFLOW_FORBIDDEN"));
        disable("routing");
        act(id, first).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WORKFLOW_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT version FROM records WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void departmentAndStatusPairsMatchPolicyAndSqlWithoutDuplicates() throws Exception {
        UUID visible = record("BSK_YRD_INCELEMESINDE", department, null);
        UUID wrongStatus = record("BASKAN_INCELEMESINDE", department, null);
        UUID deleted = record("BSK_YRD_INCELEMESINDE", department, null);
        jdbc.update("UPDATE records SET deleted_at = now() WHERE id = ?", deleted);
        int otherDepartment = jdbc.queryForObject("INSERT INTO departments(name, is_active) VALUES (?, true) RETURNING id", Integer.class, prefix + "other");
        UUID other = record("BSK_YRD_INCELEMESINDE", otherDepartment, null);
        for (UUID viewer : List.of(first, second, outsider, wrongMember)) {
            var actor = VisibilityActor.from(principal(viewer));
            var criteria = new RecordSearchCriteria(); criteria.setQ(prefix);
            var all = records.findAll().stream().filter(r -> r.getTitle().equals(prefix)).toList();
            var expected = all.stream().filter(r -> policy.canView(actor, r)).map(Record::getId).toList();
            var page = records.findAll(RecordSpecifications.withFilters(criteria, policy.scopeFor(actor)), PageRequest.of(0, 1));
            assertThat(page.getTotalElements()).isEqualTo(expected.size());
            assertThat(page.getContent()).extracting(Record::getId).containsExactlyInAnyOrderElementsOf(expected);
        }
        for (UUID viewer : List.of(first, second)) {
            read(viewer, "/api/records?q=" + prefix + "&size=1").andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(visible.toString()));
            read(viewer, "/api/records/" + visible).andExpect(status().isOk());
            read(viewer, "/api/audit-logs/record/" + visible).andExpect(status().isOk());
            read(viewer, "/api/records/" + visible + "/files").andExpect(status().isOk());
            for (UUID hidden : List.of(wrongStatus, other)) read(viewer, "/api/records/" + hidden).andExpect(status().isForbidden());
        }
        read(wrongMember, "/api/records/" + visible).andExpect(status().isForbidden());
        read(wrongMember, "/api/records?q=" + prefix).andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"routing", "members", "role", "workflow_actor", "permission", "view", "transition"})
    void revokedEligibilityImmediatelyRemovesDepartmentVisibility(String missing) throws Exception {
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);
        var actor = VisibilityActor.from(principal(first));
        assertThat(policy.canView(actor, records.findById(id).orElseThrow())).isTrue();
        disable(missing);
        em.clear();
        // Authentication refreshes permissions on each real JWT request.
        if (missing.equals("members") || missing.equals("role")) {
            read(first, "/api/records?q=" + prefix).andExpect(status().isUnauthorized());
        } else {
            read(first, "/api/records?q=" + prefix).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    void openDepartmentUsageCannotBeBypassedByRevokingEligibility() {
        record("BSK_YRD_INCELEMESINDE", department, null);
        int statusId = jdbc.queryForObject("SELECT id FROM workflow_statuses WHERE name = 'BSK_YRD_INCELEMESINDE'", Integer.class);
        assertThat(transitions.hasOpenRecords(statusId, role, "ASSIGNEE")).isTrue();
        disable("members");
        disable("routing");
        assertThat(transitions.hasOpenRecords(statusId, role, "ASSIGNEE")).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoEligibleMembersCompeteForOneVersionOnlyOneTransitionCommits() throws Exception {
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);
        var barrier = new CyclicBarrier(2);
        doAnswer(call -> {
            WorkflowRecordUpdate update = call.getArgument(0);
            if (update.recordId().equals(id)) barrier.await(15, TimeUnit.SECONDS);
            return call.callRealMethod();
        }).when(recordPort).update(any());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> act(id, first).andReturn());
            var b = executor.submit(() -> act(id, second).andReturn());
            var results = List.of(a.get(25, TimeUnit.SECONDS), b.get(25, TimeUnit.SECONDS));
            assertThat(results).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(200, 409);
            assertThat(results.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow()
                    .getResponse().getContentAsString()).contains("WORKFLOW_VERSION_CONFLICT");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE record_id = ?", Integer.class, id)).isOne();
        assertThat(jdbc.queryForObject("SELECT version FROM records WHERE id = ?", Integer.class, id)).isOne();
    }

    private void disable(String component) {
        switch (component) {
            case "routing" -> jdbc.update("UPDATE department_routing_rules SET is_active = false WHERE department_id = ?", department);
            case "wrong_status" -> jdbc.update("UPDATE department_routing_rules SET from_status_id = (SELECT id FROM workflow_statuses WHERE name = 'TASLAK') WHERE department_id = ?", department);
            case "members" -> jdbc.update("UPDATE users SET is_active = false WHERE role_id = ?", role);
            case "role" -> jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", role);
            case "workflow_actor" -> jdbc.update("UPDATE roles SET is_workflow_actor = false WHERE id = ?", role);
            case "view" -> jdbc.update("DELETE FROM role_permissions WHERE role_id = ? AND permission_id = (SELECT id FROM permissions WHERE code = 'RECORD_VIEW')", role);
            case "permission" -> jdbc.update("DELETE FROM role_permissions WHERE role_id = ? AND permission_id IN (SELECT required_permission_id FROM workflow_transitions WHERE actor_role_id = ?)", role, role);
            case "transition" -> { jdbc.update("UPDATE workflow_transitions SET is_active = false WHERE actor_role_id = ?", role); rules.reload(); }
            default -> throw new IllegalArgumentException(component);
        }
        em.clear();
    }

    private UUID insertUser(int roleId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) VALUES (?, 'Department', 'Test', ?, 'x', ?, true)", id, prefix + id + "@test.local", roleId);
        return id;
    }

    private UUID record(String statusName, Integer departmentId, UUID assignee) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO records(id, title, description, category_id, status, created_by, assigned_to, assigned_department_id, version) VALUES (?, ?, 'test', (SELECT id FROM categories ORDER BY id LIMIT 1), ?, ?, ?, ?, 0)",
                id, prefix, statusName, creator, assignee, departmentId);
        return id;
    }

    private AuthenticatedUser principal(UUID id) {
        var u = users.findById(id).orElseThrow();
        return new AuthenticatedUser(u, permissions.findActiveCodesByRoleId(u.getRole().getId()));
    }

    private ResultActions request(UUID id, UUID actor, String body) throws Exception {
        return mvc.perform(post("/api/records/{id}/workflow/actions", id).with(user(principal(actor)))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions send(UUID id, int target) throws Exception {
        return request(id, creator, "{\"action\":\"DEPARTMANA_GONDER\",\"targetDepartmentId\":" + target + "}");
    }

    private ResultActions act(UUID id, UUID actor) throws Exception {
        return request(id, actor, "{\"action\":\"CALISANA_GERI_GONDER\",\"comment\":\"Düzeltiniz\"}");
    }

    private ResultActions read(UUID viewer, String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + jwt.generateAccessToken(viewer, prefix + viewer + "@test.local", "display")));
    }
}
