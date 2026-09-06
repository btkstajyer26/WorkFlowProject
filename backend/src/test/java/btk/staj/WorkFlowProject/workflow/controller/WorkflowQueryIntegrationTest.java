package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * APP-9 uclarinin gercek PostgreSQL, gercek kural snapshot'i ve gercek gorunurluk
 * policy'si ile uctan uca davranisi.
 *
 * <p>Asil kabul: {@code system_key = NULL} olan <strong>dinamik</strong> bir rol, kendi
 * yetkili aksiyonunu listede gorur. Bugun web paneli ve mobil bu kullaniciya hicbir dugme
 * cizmiyor ({@code B10}/{@code B09}); bu ucun varlik sebebi odur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Workflow okuma uclari")
class WorkflowQueryIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RolePermissionRepository permissions;
    @Autowired ReloadableTransitionRuleSource rules;
    @MockitoBean MailService mail;

    private String prefix;
    private int dynamicRole;
    private UUID creator;
    private UUID specialist;
    private UUID outsider;
    private int department;

    @BeforeEach
    void fixture() {
        prefix = "wf-query-" + UUID.randomUUID();

        dynamicRole = insertDynamicRole();
        creator = insertUser(jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'CALISAN'", Integer.class));
        specialist = insertUser(dynamicRole);
        outsider = insertUser(dynamicRole);

        department = jdbc.queryForObject(
                "INSERT INTO departments(name, is_active) VALUES (?, true) RETURNING id", Integer.class, prefix);
        jdbc.update("INSERT INTO department_members(department_id, user_id) VALUES (?, ?)", department, specialist);
        jdbc.update("""
                INSERT INTO department_routing_rules(department_id, from_status_id, action_id, target_role_id, is_active)
                SELECT ?, s.id, a.id, ?, true FROM workflow_statuses s, workflow_actions a
                WHERE s.name = 'BSK_YRD_INCELEMESINDE' AND a.name = 'CALISANA_GERI_GONDER'
                """, department, dynamicRole);

        rules.reload();
    }

    /** Bellekteki kural snapshot'i DB rollback'inden etkilenmez; el ile geri alinir. */
    @AfterTransaction
    void restoreSnapshot() {
        rules.reload();
    }

    // ---------------------------------------------------------------
    // available-actions
    // ---------------------------------------------------------------

    @Test
    @DisplayName("dinamik rol kendi yetkili aksiyonunu gorur")
    void aDynamicRoleSeesItsAuthorisedAction() throws Exception {
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);

        availableActions(id, specialist)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("BSK_YRD_INCELEMESINDE"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.actions[?(@.action == 'CALISANA_GERI_GONDER')]").isNotEmpty());
    }

    @Test
    @DisplayName("aciklamasi zorunlu aksiyon listelenir ve bayragi acik gelir")
    void marksCommentRequiredActions() throws Exception {
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);

        // Bu aktor bu durumda tek aksiyon tasir; dogrudan indisle okumak filtreli
        // JSONPath projeksiyonundan daha az kirilgan.
        availableActions(id, specialist)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions.length()").value(1))
                .andExpect(jsonPath("$.actions[0].action").value("CALISANA_GERI_GONDER"))
                .andExpect(jsonPath("$.actions[0].commentRequired").value(true))
                .andExpect(jsonPath("$.actions[0].targetUserRequired").value(false))
                .andExpect(jsonPath("$.actions[0].targetDepartmentRequired").value(false))
                // display_name katalogdan gelir; istemci kendi sozlugunu tutmaz.
                .andExpect(jsonPath("$.actions[0].displayName").isNotEmpty());
    }

    @Test
    @DisplayName("routing kalkinca uzman kaydi hic goremez")
    void removesVisibilityWhenRoutingIsRemoved() throws Exception {
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);
        jdbc.update("UPDATE department_routing_rules SET is_active = false WHERE department_id = ?", department);

        // Departman gorunurlugu de routing uzerinden kuruluyor: kural kalkinca uzman
        // yalnizca aksiyonu degil kaydin kendisini kaybeder. Uc bunu kayit detay ucuyla
        // ayni sekilde 403 olarak bildirir.
        availableActions(id, specialist)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("kaydi gorebilen ama islem yapamayan kullanici bos liste alir, 403 degil")
    void returnsAnEmptyListRatherThanForbidden() throws Exception {
        // Olusturan kendi kaydini gorur; BSK_YRD_INCELEMESINDE'de yapabilecegi islem yok.
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);

        availableActions(id, creator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions").isEmpty());
    }

    @Test
    @DisplayName("terminal kayitta liste bostur")
    void returnsAnEmptyListForTerminalRecords() throws Exception {
        UUID id = record("ONAYLANDI", null, null);

        availableActions(id, creator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions").isEmpty());
    }

    // ---------------------------------------------------------------
    // Gorunurluk: kayit detay ucuyla birebir
    // ---------------------------------------------------------------

    @Test
    @DisplayName("kapsam disi kullanici 403 alir")
    void rejectsAnActorOutsideTheVisibilityScope() throws Exception {
        // outsider dinamik rolde ama departmanin uyesi degil: kaydi hic goremez.
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);

        availableActions(id, outsider)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        targetDepartments(id, outsider).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("silinmis ve olmayan kayit 404 doner")
    void reportsDeletedAndMissingRecordsAsNotFound() throws Exception {
        UUID deleted = record("TASLAK", null, null);
        jdbc.update("UPDATE records SET deleted_at = now() WHERE id = ?", deleted);

        availableActions(deleted, creator)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        availableActions(UUID.randomUUID(), creator).andExpect(status().isNotFound());
        targetDepartments(deleted, creator).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // target-departments
    // ---------------------------------------------------------------

    @Test
    @DisplayName("yalnizca gonderilebilir departmani, yalnizca kimlik ve adiyla doner")
    void listsOnlyRoutableDepartmentsWithIdAndNameOnly() throws Exception {
        // TASLAK'tan DEPARTMANA_GONDER, BSK_YRD_INCELEMESINDE'ye iner; routing oraya kurulu.
        UUID id = record("TASLAK", null, null);

        targetDepartments(id, creator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments[?(@.id == " + department + ")]").isNotEmpty())
                .andExpect(jsonPath("$.departments[0].name").exists())
                // Organizasyon dizini acilmaz (SS2.3).
                .andExpect(jsonPath("$.departments[0].members").doesNotExist())
                .andExpect(jsonPath("$.departments[0].targetRoleId").doesNotExist());
    }

    @Test
    @DisplayName("kullanilamayan departman listelenmez ve departmana gonderme aksiyonu da duser")
    void hidesUnroutableDepartmentsAndTheSendAction() throws Exception {
        UUID id = record("TASLAK", null, null);
        jdbc.update("UPDATE department_routing_rules SET is_active = false WHERE department_id = ?", department);

        targetDepartments(id, creator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments").isEmpty());

        // Ayni kosul iki yerde tek hesaptan geldigi icin aksiyon da listelenmez.
        availableActions(id, creator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions[?(@.action == 'DEPARTMANA_GONDER')]").isEmpty());
    }

    @Test
    @DisplayName("departmana gonderme kurali olmayan aktor icin liste bostur")
    void returnsAnEmptyListForAnActorWithoutTheDepartmentRule() throws Exception {
        UUID id = record("BSK_YRD_INCELEMESINDE", department, null);

        targetDepartments(id, specialist)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments").isEmpty());
    }

    // ---------------------------------------------------------------
    // Fixture yardimcilari
    // ---------------------------------------------------------------

    /** system_key = NULL olan, mevcut bir gecise aktor olarak baglanmis dinamik rol. */
    private int insertDynamicRole() {
        int id = jdbc.queryForObject(
                "INSERT INTO roles(name, is_active, is_workflow_actor) VALUES (?, true, true) RETURNING id",
                Integer.class, prefix + "-uzman");
        jdbc.update("INSERT INTO role_permissions(role_id, permission_id) "
                + "SELECT ?, id FROM permissions WHERE is_active = true", id);
        jdbc.update("""
                INSERT INTO workflow_transitions(from_status_id, action_id, actor_role_id, actor_requirement,
                    to_status_id, expected_target_role_id, target_strategy, required_permission_id, is_active)
                SELECT t.from_status_id, t.action_id, ?, t.actor_requirement, t.to_status_id,
                    t.expected_target_role_id, t.target_strategy, t.required_permission_id, true
                FROM workflow_transitions t
                JOIN roles r ON r.id = t.actor_role_id
                JOIN workflow_actions a ON a.id = t.action_id
                WHERE r.system_key = 'BASKAN_YARDIMCISI' AND a.name = 'CALISANA_GERI_GONDER'
                """, id);
        return id;
    }

    private UUID insertUser(int roleId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, 'Query', 'Test', ?, 'x', ?, true)", id, prefix + id + "@test.local", roleId);
        return id;
    }

    private UUID record(String statusName, Integer departmentId, UUID assignee) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO records(id, title, description, category_id, status, created_by, "
                + "assigned_to, assigned_department_id, version) "
                + "VALUES (?, ?, 'test', (SELECT id FROM categories ORDER BY id LIMIT 1), ?, ?, ?, ?, 0)",
                id, prefix, statusName, creator, assignee, departmentId);
        return id;
    }

    private AuthenticatedUser principal(UUID id) {
        var user = users.findById(id).orElseThrow();
        return new AuthenticatedUser(user, permissions.findActiveCodesByRoleId(user.getRole().getId()));
    }

    private ResultActions availableActions(UUID recordId, UUID actor) throws Exception {
        return mvc.perform(get("/api/records/{id}/workflow/available-actions", recordId)
                .with(user(principal(actor))));
    }

    private ResultActions targetDepartments(UUID recordId, UUID actor) throws Exception {
        return mvc.perform(get("/api/records/{id}/workflow/target-departments", recordId)
                .with(user(principal(actor))));
    }
}
