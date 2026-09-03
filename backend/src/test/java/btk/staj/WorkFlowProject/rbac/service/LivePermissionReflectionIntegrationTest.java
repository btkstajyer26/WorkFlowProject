package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WF-4 &mdash; rol ve yetki degisikliklerinin <strong>yeniden baslatma olmadan</strong>
 * yansidigini sabitler.
 *
 * <p>Bu davranis bugun dogru ama hicbir test onu tutmuyordu: {@code JwtAuthenticationFilter}
 * token'dan yalnizca e-postayi aliyor, rol ve permission'lari her istekte
 * {@code CustomUserDetailsService} &rarr; {@code AuthenticatedUserFactory} &rarr;
 * {@code RolePermissionRepository} zinciri veritabanindan okuyor. Yarin biri araya bir
 * cache koyarsa panelden yapilan degisiklik sessizce etkisiz kalirdi; bu test o gun
 * kirmizi yanar.
 *
 * <p>Testler <strong>ayni JWT'yi</strong> kullanmaya devam eder; degisen tek sey
 * veritabanidir. Kanit degeri buradan gelir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Rol ve yetki degisiminin canli yansimasi")
class LivePermissionReflectionIntegrationTest {

    private static final String ROLES_URL = "/api/admin/roles";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("rolden yetki alinirsa ayni token bir sonraki istekte 403 alir")
    void revokingAPermissionTakesEffectOnTheNextRequest() throws Exception {
        Admin admin = insertAdmin();

        mockMvc.perform(get(ROLES_URL).header("Authorization", "Bearer " + admin.token))
                .andExpect(status().isOk());

        revokePermission(admin.roleId, "ROLE_VIEW");

        mockMvc.perform(get(ROLES_URL).header("Authorization", "Bearer " + admin.token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("role yetki eklenirse ayni token bir sonraki istekte gorur")
    void grantingAPermissionTakesEffectOnTheNextRequest() throws Exception {
        Admin admin = insertAdmin();
        revokePermission(admin.roleId, "ROLE_VIEW");

        mockMvc.perform(get(ROLES_URL).header("Authorization", "Bearer " + admin.token))
                .andExpect(status().isForbidden());

        grantPermission(admin.roleId, "ROLE_VIEW");

        mockMvc.perform(get(ROLES_URL).header("Authorization", "Bearer " + admin.token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("rol pasiflestirilirse gecerli token bile kabul edilmez")
    void deactivatingTheRoleInvalidatesAStillValidToken() throws Exception {
        Admin admin = insertAdmin();

        mockMvc.perform(get(ROLES_URL).header("Authorization", "Bearer " + admin.token))
                .andExpect(status().isOk());

        jdbc.update("UPDATE roles SET is_active = FALSE WHERE id = ?", admin.roleId);

        mockMvc.perform(get(ROLES_URL).header("Authorization", "Bearer " + admin.token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("permission pasiflestirilirse authority listesinden duser")
    void deactivatingAPermissionDropsItFromTheAuthorityList() {
        Admin admin = insertAdmin();

        assertThat(authoritiesOf(admin.email)).contains("ROLE_VIEW");

        jdbc.update("UPDATE permissions SET is_active = FALSE WHERE code = 'ROLE_VIEW'");

        assertThat(authoritiesOf(admin.email))
                .as("pasif permission authority olarak yayinlanmamali")
                .doesNotContain("ROLE_VIEW");
    }

    /**
     * Authority listesi her cagride yeniden uretiliyor mu &mdash; yani bir yerde
     * onbelleklenmiyor mu?
     */
    @Test
    @DisplayName("authority listesi her cagride veritabanindan yeniden uretilir")
    void authoritiesAreRebuiltOnEveryLookup() {
        Admin admin = insertAdmin();
        assertThat(authoritiesOf(admin.email)).contains("USER_MANAGE");

        revokePermission(admin.roleId, "USER_MANAGE");

        assertThat(authoritiesOf(admin.email)).doesNotContain("USER_MANAGE");
    }

    private java.util.Set<String> authoritiesOf(String email) {
        UserDetails details = userDetailsService.loadUserByUsername(email);
        return details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void revokePermission(int roleId, String code) {
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?"
                + " AND permission_id = (SELECT id FROM permissions WHERE code = ?)", roleId, code);
    }

    private void grantPermission(int roleId, String code) {
        jdbc.update("INSERT INTO role_permissions (role_id, permission_id)"
                + " SELECT ?, id FROM permissions WHERE code = ?"
                + " ON CONFLICT DO NOTHING", roleId, code);
    }

    /** Testin kendi ADMIN kullanicisi; paylasilan seed kullanicilarina dokunulmaz. */
    private Admin insertAdmin() {
        int roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'ADMIN'", Integer.class);
        UUID id = UUID.randomUUID();
        String email = "wf4-" + id + "@ornek.test";
        jdbc.update("INSERT INTO users (id, first_name, last_name, email, password_hash, role_id, is_active)"
                        + " VALUES (?, ?, ?, ?, ?, ?, true)",
                id, "WF4", "Admin", email, "x", roleId);
        return new Admin(id, email, roleId, jwtUtil.generateAccessToken(id, email, "ADMIN"));
    }

    private record Admin(UUID id, String email, int roleId, String token) {
    }
}
