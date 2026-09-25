package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * `ADR-0008` kabul senaryolari: dinamik rolun Baskana iletip geri alabilmesi (`B02`) ve
 * geri donen kaydi gormeye devam etmesi (`B13`).
 *
 * <p>Bu iki bulgu ayni kok nedenin iki yuzudur. `B02` gecisi acar, `B13` kaydi gorunur
 * kilar; biri olmadan digeri demoyu tamamlamaz. Bu yuzden ayni sinifta olculurler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Onceki aktore donus")
class PreviousActorReturnIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RolePermissionRepository permissions;
    @Autowired ReloadableTransitionRuleSource rules;
    @MockitoBean MailService mail;

    private String prefix;
    private int specialistRole;
    private UUID employee;
    private UUID specialist;
    private UUID chair;
    private UUID recordId;

    @BeforeEach
    void fixture() {
        prefix = "b02-" + UUID.randomUUID();

        // Dinamik rol: system_key NULL. Mevcut iki gecise aktor olarak baglanir -
        // BASKANA_ILET (Baskana iletebilsin) ve CALISANA_GERI_GONDER (kayit ona
        // dondugunde inis durumunda islem yapabilsin, yani ADR-0008 K4.3 saglansin).
        specialistRole = insertRole();
        bindExistingTransition("BSK_YRD_INCELEMESINDE", "BASKANA_ILET");
        bindExistingTransition("BSK_YRD_INCELEMESINDE", "CALISANA_GERI_GONDER");

        employee = insertUser(systemRole("CALISAN"));
        specialist = insertUser(specialistRole);
        chair = insertUser(systemRole("BASKAN"));

        recordId = insertRecord("BSK_YRD_INCELEMESINDE", specialist);
        rules.reload();
    }

    @AfterTransaction
    void restoreSnapshot() {
        rules.reload();
    }

    @Test
    @DisplayName("dinamik aktore donus basarili olur ve kayit ona atanir")
    void theChairCanReturnTheRecordToADynamicPreviousActor() throws Exception {
        forwardToChair();

        returnToPreviousActor()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("BSK_YRD_INCELEMESINDE"))
                .andExpect(jsonPath("$.assignment.kind").value("USER"))
                .andExpect(jsonPath("$.assignment.userId").value(specialist.toString()));

        assertThat(jdbc.queryForObject("SELECT assigned_to FROM records WHERE id = ?", UUID.class, recordId))
                .isEqualTo(specialist);
    }

    @Test
    @DisplayName("onceki aktor pasifse gecis reddedilir ve kayit yerinde kalir")
    void rejectsWhenThePreviousActorIsInactive() throws Exception {
        forwardToChair();
        jdbc.update("UPDATE users SET is_active = false WHERE id = ?", specialist);
        // jdbc persistence context'i atlar; adapter bayat entity okumasin.
        em.clear();

        // WORKFLOW_TARGET_INACTIVE mevcut eslemede 400; bu teslimde degistirilmedi.
        returnToPreviousActor()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_INACTIVE"));

        assertThat(statusOf(recordId)).isEqualTo("BASKAN_INCELEMESINDE");
    }

    @Test
    @DisplayName("onceki aktorun aktor rol bagi kalkmissa gecis reddedilir")
    void rejectsWhenThePreviousActorCanNoLongerActInTheLandingStatus() throws Exception {
        forwardToChair();
        // K4.3 duser: inis durumunda bu role tanimli hicbir gecis kalmaz.
        jdbc.update("UPDATE workflow_transitions SET is_active = false WHERE actor_role_id = ?", specialistRole);
        em.clear();
        rules.reload();

        returnToPreviousActor()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_CANNOT_ACT"));

        assertThat(statusOf(recordId)).isEqualTo("BASKAN_INCELEMESINDE");
    }

    @Test
    @DisplayName("rolu workflow aktoru olmaktan cikarilan onceki aktor de reddedilir")
    void rejectsWhenThePreviousActorRoleIsNoLongerAWorkflowActor() throws Exception {
        forwardToChair();
        jdbc.update("UPDATE roles SET is_workflow_actor = false WHERE id = ?", specialistRole);
        em.clear();

        returnToPreviousActor()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_CANNOT_ACT"));
    }

    @Test
    @DisplayName("B13: geri donen kaydi ileten aktor gorur ve icerigi dondurulmus gelir")
    void theDynamicActorKeepsSeeingTheRecordAfterItGoesBackForCorrection() throws Exception {
        forwardToChair();
        // Baskan Calisana geri gonderir: kayit artik uzmanda degil, olusturanda.
        act(chair, "{\"action\":\"CALISANA_GERI_GONDER\",\"comment\":\"Düzeltiniz\"}")
                .andExpect(status().isOk());
        jdbc.update("UPDATE records SET title = ?, description = ? WHERE id = ?",
                prefix + "-guncel", "calisan duzeltti", recordId);
        em.clear();

        // Uzman kaydi hala gorur (B13) ve devir anindaki icerigi gorur.
        read(specialist, "/api/records/" + recordId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(prefix))
                .andExpect(jsonPath("$.description").value("ilk hali"));

        read(specialist, "/api/records?q=" + prefix)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("kaydi iletmemis dinamik rol kaydi goremez")
    void anUnrelatedDynamicActorStillCannotSeeTheRecord() throws Exception {
        UUID stranger = insertUser(specialistRole);
        forwardToChair();

        read(stranger, "/api/records/" + recordId).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("yerlesik akis regresyonsuz calisir")
    void theBuiltInDeputyFlowIsUnchanged() throws Exception {
        UUID deputy = insertUser(systemRole("BASKAN_YARDIMCISI"));
        UUID builtIn = insertRecord("BSK_YRD_INCELEMESINDE", deputy);

        act(deputy, "{\"action\":\"BASKANA_ILET\"}", builtIn).andExpect(status().isOk());
        act(chair, "{\"action\":\"BASKAN_YARDIMCISINA_GERI_GONDER\",\"comment\":\"Tekrar bakiniz\"}", builtIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment.userId").value(deputy.toString()));
    }

    // ---------------------------------------------------------------

    private void forwardToChair() throws Exception {
        act(specialist, "{\"action\":\"BASKANA_ILET\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT last_deputy_id FROM records WHERE id = ?", UUID.class, recordId))
                .as("ileten aktor kim olursa olsun last_deputy_id'ye yazilir (ADR-0008 K1)")
                .isEqualTo(specialist);
    }

    private ResultActions returnToPreviousActor() throws Exception {
        return act(chair, "{\"action\":\"BASKAN_YARDIMCISINA_GERI_GONDER\",\"comment\":\"Tekrar bakiniz\"}");
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM records WHERE id = ?", String.class, id);
    }

    private int systemRole(String key) {
        return jdbc.queryForObject("SELECT id FROM roles WHERE system_key = ?", Integer.class, key);
    }

    private int insertRole() {
        int id = jdbc.queryForObject(
                "INSERT INTO roles(name, is_active, is_workflow_actor) VALUES (?, true, true) RETURNING id",
                Integer.class, prefix + "-uzman");
        jdbc.update("INSERT INTO role_permissions(role_id, permission_id) "
                + "SELECT ?, id FROM permissions WHERE is_active = true", id);
        return id;
    }

    /** Mevcut bir yerlesik gecisi dinamik role aktor olarak baglar (WF-8 sablonu). */
    private void bindExistingTransition(String fromStatus, String action) {
        jdbc.update("""
                INSERT INTO workflow_transitions(from_status_id, action_id, actor_role_id, actor_requirement,
                    to_status_id, expected_target_role_id, target_strategy, required_permission_id, is_active)
                SELECT t.from_status_id, t.action_id, ?, t.actor_requirement, t.to_status_id,
                    t.expected_target_role_id, t.target_strategy, t.required_permission_id, true
                FROM workflow_transitions t
                JOIN workflow_statuses s ON s.id = t.from_status_id
                JOIN workflow_actions a ON a.id = t.action_id
                JOIN roles r ON r.id = t.actor_role_id
                WHERE s.name = ? AND a.name = ? AND r.system_key = 'BASKAN_YARDIMCISI'
                """, specialistRole, fromStatus, action);
    }

    private UUID insertUser(int roleId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, 'Previous', 'Actor', ?, 'x', ?, true)", id, prefix + id + "@test.local", roleId);
        return id;
    }

    private UUID insertRecord(String statusName, UUID assignee) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO records(id, title, description, category_id, status, created_by, "
                + "assigned_to, version) "
                + "VALUES (?, ?, 'ilk hali', (SELECT min(id) FROM categories), ?, ?, ?, 0)",
                id, prefix, statusName, employee, assignee);
        return id;
    }

    private AuthenticatedUser principal(UUID id) {
        var user = users.findById(id).orElseThrow();
        return new AuthenticatedUser(user, permissions.findActiveCodesByRoleId(user.getRole().getId()));
    }

    private ResultActions act(UUID actor, String body) throws Exception {
        return act(actor, body, recordId);
    }

    private ResultActions act(UUID actor, String body, UUID target) throws Exception {
        return mvc.perform(post("/api/records/{id}/workflow/actions", target)
                .with(user(principal(actor)))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions read(UUID actor, String path) throws Exception {
        return mvc.perform(get(path).with(user(principal(actor))));
    }
}
