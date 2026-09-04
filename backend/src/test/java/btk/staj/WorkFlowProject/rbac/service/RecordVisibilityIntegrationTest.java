package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL predicates, authentication and HTTP consumers; only disk storage is substituted. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecordVisibilityIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @Autowired RecordRepository records;
    @Autowired RecordAccessPolicy policy;
    @MockitoBean FileStorageService storage;

    private String prefix;
    private Integer roleId;
    private UUID viewer;
    private UUID other;
    private UUID owned;
    private UUID assigned;
    private UUID unrelated;
    private UUID deleted;
    private UUID fileId;
    private String token;

    @BeforeEach
    void fixtures() {
        prefix = "wf2c2-" + UUID.randomUUID();
        roleId = jdbc.queryForObject("INSERT INTO roles(name, is_active, is_workflow_actor) "
                + "VALUES (?, true, false) RETURNING id", Integer.class, prefix);
        jdbc.update("INSERT INTO role_permissions(role_id, permission_id) "
                + "SELECT ?, id FROM permissions WHERE code = 'RECORD_VIEW'", roleId);
        viewer = user("Viewer");
        other = user("Other");
        token = jwt.generateAccessToken(viewer, viewer + "@wf2c2.test", "untrusted-display-name");
        owned = record(viewer, null, null, RecordStatus.TASLAK);
        assigned = record(other, viewer, null, RecordStatus.BSK_YRD_INCELEMESINDE);
        unrelated = record(other, other, viewer, RecordStatus.BASKAN_INCELEMESINDE);
        deleted = record(viewer, viewer, viewer, RecordStatus.TASLAK);
        jdbc.update("UPDATE records SET deleted_at = now() WHERE id = ?", deleted);
        fileId = UUID.randomUUID();
        jdbc.update("INSERT INTO files(id, record_id, original_name, stored_name, mime_type, file_size, uploaded_by, uploaded_at) "
                + "VALUES (?, ?, 'visible.pdf', ?, 'application/pdf', 3, ?, now())", fileId, assigned, fileId.toString(), other);
        // Both entries precede/cover the assignment. Dynamic readers have full history, without AUDIT_VIEW.
        jdbc.update("INSERT INTO audit_logs(record_id, user_id, role_id, action, new_status, comment, created_at) "
                + "VALUES (?, ?, ?, 'RECORD_CREATED', 'TASLAK', 'before assignment', now() - interval '1 hour'), "
                + "(?, ?, ?, 'GONDER', 'BSK_YRD_INCELEMESINDE', 'assigned', now())",
                assigned, other, roleId, assigned, other, roleId);
    }

    @Test
    void sqlAndPolicyReturnTheSameRecordsAcrossRelationsStatusesRolesAndPermissions() {
        for (var status : RecordStatus.values()) {
            for (int mask = 0; mask < 8; mask++) {
                record((mask & 1) == 0 ? other : viewer, (mask & 2) == 0 ? null : viewer,
                        (mask & 4) == 0 ? null : viewer, status);
            }
        }
        var actors = new ArrayList<VisibilityActor>();
        for (boolean viewPermission : List.of(false, true)) {
            Set<String> permissions = viewPermission ? Set.of("RECORD_VIEW") : Set.of();
            actors.add(new VisibilityActor(viewer, new RoleId(roleId), Optional.empty(), permissions));
            for (var key : SystemRoleKey.values()) {
                actors.add(new VisibilityActor(viewer, new RoleId(roleId), Optional.of(key), permissions));
            }
        }
        var fixtureRecords = records.findAll().stream().filter(r -> r.getTitle().equals(prefix)).toList();
        for (var actor : actors) {
            var expected = fixtureRecords.stream().filter(r -> policy.canView(actor, r)).map(Record::getId).toList();
            var criteria = new RecordSearchCriteria();
            criteria.setQ(prefix);
            var specification = RecordSpecifications.withFilters(criteria, actor);
            assertThat(records.findAll(specification)).as("scope for %s", actor)
                    .extracting(Record::getId).containsExactlyInAnyOrderElementsOf(expected);
            var page = records.findAll(specification, PageRequest.of(0, 7));
            assertThat(page.getTotalElements()).isEqualTo(expected.size());
            assertThat(page.getTotalPages()).isEqualTo((expected.size() + 6) / 7);
            assertThat(page.getContent()).extracting(Record::getId).doesNotContain(deleted);
        }
    }

    @Test
    void dynamicReaderCanUseEveryReadEndpointWithoutWorkflowOrAuditCapabilities() throws Exception {
        request("/api/records/" + owned).andExpect(status().isOk());
        request("/api/records/" + assigned).andExpect(status().isOk());
        request("/api/records?q=" + prefix + "&size=1").andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2));
        request("/api/records?q=" + prefix + "&creator=Other").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(assigned.toString()));
        request("/api/audit-logs/record/" + assigned).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].comment").value("before assignment"));
        request("/api/records/" + assigned + "/files").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fileId.toString()));
        when(storage.loadAsResource(fileId.toString())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        request("/api/files/" + fileId + "/download").andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1, 2, 3}));
        request("/api/files/" + fileId + "/preview").andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        request("/api/records/" + unrelated).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void removingAssignmentClosesEveryReadPathButRetainsCreatorAccess() throws Exception {
        request("/api/records/" + assigned).andExpect(status().isOk());
        jdbc.update("UPDATE records SET assigned_to = ?, last_deputy_id = ? WHERE id = ?", other, viewer, assigned);
        assertAssignedReadPaths(403, "FORBIDDEN");
        request("/api/records?q=" + prefix).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        jdbc.update("UPDATE records SET assigned_to = ? WHERE id = ?", other, owned);
        request("/api/records/" + owned).andExpect(status().isOk());
        verifyNoInteractions(storage);
    }

    @Test
    void revokingViewPermissionTakesEffectWithTheSameToken() throws Exception {
        request("/api/records/" + assigned).andExpect(status().isOk());
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
        assertAssignedReadPaths(403, "FORBIDDEN");
        request("/api/records/" + owned).andExpect(status().isForbidden());
        request("/api/records?q=" + prefix).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        verifyNoInteractions(storage);
    }

    @Test
    void deletedAndMissingRecordsAreNotFoundThroughEveryReadPath() throws Exception {
        jdbc.update("UPDATE records SET deleted_at = now() WHERE id = ?", assigned);
        assertAssignedReadPaths(404, "RESOURCE_NOT_FOUND");
        request("/api/records/" + deleted).andExpect(status().isNotFound());
        request("/api/records/" + UUID.randomUUID()).andExpect(status().isNotFound());
        request("/api/records?q=" + prefix).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "role"})
    void inactiveAccountsAndRolesCannotReadWithAnExistingToken(String disabled) throws Exception {
        request("/api/records/" + owned).andExpect(status().isOk());
        if (disabled.equals("user")) jdbc.update("UPDATE users SET is_active = false WHERE id = ?", viewer);
        else jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", roleId);
        for (var url : readPaths()) request(url).andExpect(status().isUnauthorized());
        request("/api/records?q=" + prefix).andExpect(status().isUnauthorized());
        verifyNoInteractions(storage);
    }

    @Test
    void adminCannotReadEvenWhenAssignedAndGrantedViewPermission() throws Exception {
        Integer adminRole = jdbc.queryForObject("SELECT id FROM roles WHERE system_key = 'ADMIN'", Integer.class);
        jdbc.update("UPDATE users SET role_id = ? WHERE id = ?", adminRole, viewer);
        jdbc.update("INSERT INTO role_permissions(role_id, permission_id) SELECT ?, id FROM permissions "
                + "WHERE code = 'RECORD_VIEW' ON CONFLICT DO NOTHING", adminRole);
        assertAssignedReadPaths(403, "FORBIDDEN");
        request("/api/records/" + owned).andExpect(status().isForbidden());
        request("/api/records?q=" + prefix).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        verifyNoInteractions(storage);
    }

    @Test
    void renamingARoleDoesNotChangeVisibility() throws Exception {
        jdbc.update("UPDATE roles SET name = ? WHERE id = ?", "renamed-" + UUID.randomUUID(), roleId);
        request("/api/records/" + assigned).andExpect(status().isOk());
        request("/api/records/" + unrelated).andExpect(status().isForbidden());
    }

    private void assertAssignedReadPaths(int statusCode, String errorCode) throws Exception {
        for (var url : readPaths()) request(url).andExpect(status().is(statusCode)).andExpect(jsonPath("$.code").value(errorCode));
    }

    private List<String> readPaths() {
        return List.of("/api/records/" + assigned, "/api/audit-logs/record/" + assigned,
                "/api/records/" + assigned + "/files", "/api/files/" + fileId + "/download", "/api/files/" + fileId + "/preview");
    }

    private ResultActions request(String url) throws Exception {
        em.flush();
        em.clear(); // Simulate separate requests, not a stale transaction-scoped principal.
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    private UUID user(String firstName) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, ?, 'Visibility', ?, 'unused', ?, true)", id, firstName, id + "@wf2c2.test", roleId);
        return id;
    }

    private UUID record(UUID creator, UUID assignee, UUID deputy, RecordStatus status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO records(id, title, description, category_id, status, created_by, assigned_to, last_deputy_id, version) "
                + "VALUES (?, ?, 'visibility test', (SELECT min(id) FROM categories), ?, ?, ?, ?, 0)",
                id, prefix, status.name(), creator, assignee, deputy);
        return id;
    }
}
