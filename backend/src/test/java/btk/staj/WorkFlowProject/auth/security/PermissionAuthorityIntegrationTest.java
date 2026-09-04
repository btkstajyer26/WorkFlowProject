package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermissionAuthorityIntegrationTest {
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired CustomUserDetailsService details;
    @Autowired JwtUtil jwt;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private User actor(String... codes) {
        Role role = new Role(); role.setName("permission-test-" + UUID.randomUUID()); role.setActive(true);
        roles.saveAndFlush(role);
        for (String code : codes) jdbc.update("INSERT INTO role_permissions(role_id, permission_id) SELECT ?, id FROM permissions WHERE code = ?", role.getId(), code);
        User user = new User(); user.setEmail(UUID.randomUUID()+"@permission.test");
        user.setFirstName("Permission"); user.setLastName("Test");
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setPasswordHash("unused"); user.setRole(role);
        return users.saveAndFlush(user);
    }

    private String token(User user) { return jwt.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName()); }

    @Test void roleSelectorIsDescribedInOpenApi() throws Exception {
        String schema = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ChangeRoleRequest.properties.roleId.minimum").value(1))
                .andExpect(jsonPath("$.components.schemas.ChangeRoleRequest.properties.roleName.deprecated").value(true))
                .andExpect(jsonPath("$.components.schemas.ChangeRoleRequest.properties.roleSelectorValid").doesNotExist())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.writeString(java.nio.file.Path.of("target", "openapi-wf2.json"), schema);
    }

    @Test void dynamicRecordEditorUsesRoleIdForAllLifecycleAuditEvents() throws Exception {
        User editor = actor("RECORD_CREATE", "RECORD_EDIT", "RECORD_DELETE"); String token = token(editor);
        Integer category = jdbc.queryForObject("SELECT min(id) FROM categories", Integer.class);
        String body = "{\"title\":\"Dynamic editor\",\"description\":\"Test\",\"categoryId\":" + category + "}";
        mvc.perform(post("/api/records").header("Authorization", "Bearer " + token).contentType("application/json").content(body))
                .andExpect(status().isCreated());
        em.flush();
        UUID recordId = jdbc.queryForObject("SELECT id FROM records WHERE created_by = ?", UUID.class, editor.getId());
        mvc.perform(put("/api/records/" + recordId).header("Authorization", "Bearer " + token).contentType("application/json").content(body))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/records/" + recordId).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        em.flush();
        assertThat(jdbc.queryForList("SELECT action FROM audit_logs WHERE record_id = ? AND role_id = ?", String.class, recordId, editor.getRole().getId()))
                .containsExactlyInAnyOrder("RECORD_CREATED", "RECORD_UPDATED", "RECORD_DELETED");
    }

    @Test void oldJwtReloadsPermissionsAndNeverGetsLegacyAuthorities() throws Exception {
        User user = actor("ROLE_VIEW"); String token = token(user);
        Integer roleId = user.getRole().getId(); String email = user.getEmail();
        em.clear();
        AuthenticatedUser loaded = (AuthenticatedUser) details.loadUserByUsername(email);
        em.clear();
        assertThat(loaded.getAuthorities()).extracting("authority").containsExactly("ROLE_VIEW");
        mvc.perform(get("/api/admin/roles").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
        mvc.perform(get("/api/admin/roles").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test void inactivePermissionAndInactiveRoleAreNotGranted() {
        User user = actor("USER_VIEW"); String email = user.getEmail(); Integer roleId = user.getRole().getId();
        jdbc.update("UPDATE permissions SET is_active = false WHERE code = 'USER_VIEW'"); em.clear();
        assertThat(((AuthenticatedUser) details.loadUserByUsername(email)).getAuthorities()).isEmpty();
        jdbc.update("UPDATE roles SET is_active = false WHERE id = ?", roleId); em.clear();
        assertThat(details.loadUserByUsername(email).isEnabled()).isFalse();
    }

    @Test void dynamicUserManagerCreatesUserAndAuditWithoutWorkflowRole() throws Exception {
        User manager = actor("USER_MANAGE"); String email = UUID.randomUUID()+"@created.test";
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + token(manager))
                .contentType("application/json").content("{\"firstName\":\"Yetki\",\"lastName\":\"Test\",\"email\":\""+email+"\",\"password\":\"StrongTest123!\"}"))
                .andExpect(status().isOk());
        assertThat(users.findByEmail(email)).isPresent();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_audit_logs WHERE performed_by = ? AND action = 'USER_CREATED'", Integer.class, manager.getId()))
                .isEqualTo(1);
    }

    @Test void roleIdAndLegacyNameAreSupportedButAmbiguousSelectorsAreRejected() throws Exception {
        User manager = actor("USER_MANAGE"), target = actor();
        String url = "/api/admin/users/" + target.getId() + "/role";
        String token = token(manager);
        mvc.perform(patch(url).header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"roleId\":" + manager.getRole().getId() + "}" )).andExpect(status().isOk());
        mvc.perform(patch(url).header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"roleName\":\"" + manager.getRole().getName() + "\"}" )).andExpect(status().isOk());
        for (String body : java.util.List.of("{}", "{\"roleId\":0}", "{\"roleName\":\" \"}",
                "{\"roleId\":1,\"roleName\":\"CALISAN\"}")) {
            mvc.perform(patch(url).header("Authorization", "Bearer " + token).contentType("application/json")
                    .content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test void viewPermissionDoesNotGrantManagementOrPanelAccess() throws Exception {
        User reader = actor("USER_VIEW"); String token = token(reader);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/roles").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"denied@example.test\",\"password\":\"StrongTest123!\"}"))
                .andExpect(status().isForbidden());
    }
}
