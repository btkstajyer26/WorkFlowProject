package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WF-4 &mdash; gecis kurallarinin yeniden baslatmadan tazelenmesi.
 *
 * <p>Testler {@code @Transactional}: {@code workflow_transitions} uzerindeki degisiklikler
 * test sonunda geri alinir. Ancak <strong>bellekteki snapshot geri alinmaz</strong> &mdash;
 * reload islemi transaction'a bagli degildir. Bu yuzden her test, degistirdigi kural
 * kumesini kendisi eski haline getirip son bir reload ile paylasilan bean'i temiz birakir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Gecis kurallarinin yeniden yuklenmesi")
class WorkflowRuleReloadIntegrationTest {

    private static final String RELOAD_URL = "/api/workflow/rules/reload";
    private static final int SEEDED_RULE_COUNT = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TransitionRuleSource ruleSource;

    @Test
    @DisplayName("WORKFLOW_MANAGE yetkisi olan kullanici kurallari tazeleyebilir")
    void adminCanReloadRules() throws Exception {
        mockMvc.perform(post(RELOAD_URL).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleCount").value(SEEDED_RULE_COUNT));
    }

    @Test
    @DisplayName("yetkisiz kullanici tazeleyemez")
    void nonAdminCannotReloadRules() throws Exception {
        mockMvc.perform(post(RELOAD_URL).header("Authorization", "Bearer " + employeeToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("kimliksiz istek reddedilir")
    void anonymousCannotReloadRules() throws Exception {
        mockMvc.perform(post(RELOAD_URL)).andExpect(status().isUnauthorized());
    }

    /**
     * Asil kanit: uygulama <strong>yeniden baslatilmadan</strong> veritabanindaki
     * degisikligi goruyor.
     */
    @Test
    @DisplayName("veritabanindaki degisiklik reload sonrasi bellekte gorunur")
    void reloadPicksUpDatabaseChangeWithoutRestart() throws Exception {
        assertThat(ruleSource.all()).hasSize(SEEDED_RULE_COUNT);

        jdbc.update("UPDATE workflow_transitions SET is_active = FALSE"
                + " WHERE id = (SELECT MIN(id) FROM workflow_transitions WHERE is_active)");
        try {
            mockMvc.perform(post(RELOAD_URL).header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruleCount").value(SEEDED_RULE_COUNT - 1));

            assertThat(ruleSource.all())
                    .as("tazeleme sonrasi bellekteki kural kumesi")
                    .hasSize(SEEDED_RULE_COUNT - 1);
        } finally {
            jdbc.update("UPDATE workflow_transitions SET is_active = TRUE");
            reloadDirectly();
        }

        assertThat(ruleSource.all()).hasSize(SEEDED_RULE_COUNT);
    }

    /**
     * Bu sarmalayicinin varlik sebebi: bozuk bir yapilandirma calisan uygulamayi kural
     * kaynagi olmadan birakmamali. Asagidaki kombinasyon veritabani CHECK'ini gecer
     * ({@code chk_transition_target_strategy_role} yalniz {@code ROLE} icin rolu zorunlu
     * kilar) ama {@code TransitionRule} invariant'ini ihlal eder.
     */
    @Test
    @DisplayName("bozuk yapilandirma ile reload basarisiz olur ama eski kurallar korunur")
    void failedReloadKeepsTheRunningRuleSet() throws Exception {
        assertThat(ruleSource.all()).hasSize(SEEDED_RULE_COUNT);

        jdbc.update("UPDATE workflow_transitions SET expected_target_role_id = NULL"
                + " WHERE target_strategy = 'CREATOR'");
        try {
            mockMvc.perform(post(RELOAD_URL).header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().is5xxServerError());

            assertThat(ruleSource.all())
                    .as("basarisiz tazelemeden sonra kurallar korunmali")
                    .hasSize(SEEDED_RULE_COUNT);
        } finally {
            jdbc.update("UPDATE workflow_transitions t SET expected_target_role_id = r.id"
                    + " FROM roles r WHERE r.system_key = 'CALISAN'"
                    + " AND t.target_strategy = 'CREATOR' AND t.expected_target_role_id IS NULL");
            reloadDirectly();
        }
    }

    /** Paylasilan bean'i temiz birakmak icin; HTTP katmanindan gecmeye gerek yok. */
    private void reloadDirectly() {
        ((ReloadableTransitionRuleSource) ruleSource).reload();
    }

    private String adminToken() {
        return tokenFor("ADMIN");
    }

    private String employeeToken() {
        return tokenFor("CALISAN");
    }

    private String tokenFor(String systemKey) {
        int roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = ?", Integer.class, systemKey);
        UUID id = UUID.randomUUID();
        String email = "reload-" + id + "@ornek.test";
        jdbc.update("INSERT INTO users (id, first_name, last_name, email, password_hash, role_id, is_active)"
                        + " VALUES (?, ?, ?, ?, ?, ?, true)",
                id, "Reload", systemKey, email, "x", roleId);
        return jwtUtil.generateAccessToken(id, email, systemKey);
    }
}
