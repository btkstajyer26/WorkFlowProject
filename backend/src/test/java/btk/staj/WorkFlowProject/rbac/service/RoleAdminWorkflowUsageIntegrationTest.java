package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.common.exception.RoleInUseException;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRoleRequest;
import btk.staj.WorkFlowProject.rbac.port.WorkflowRoleUsagePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AP-2'nin rol yazicilarinin WF-8 ile ayni etki analizini yaptigini gercek veritabanina
 * karsi dogrular. Birim testleri portu mock'ladigi icin turetilmis sorgunun
 * ({@code findAllByActorRoleIdAndActiveTrue}) gercekten cozuldugunu yalniz burasi kanitlar.
 *
 * <p>Senaryolar bilerek WF-8'in {@code WorkflowActorBindingIntegrationTest} senaryolariyla
 * ayni sekle sahiptir: ayni durumda iki yol da reddetmelidir.
 */
@SpringBootTest
@Transactional
@DisplayName("Rol yonetiminin akis kullanim korumasi")
class RoleAdminWorkflowUsageIntegrationTest {

    @Autowired private RoleAdminService roleAdminService;
    @Autowired private WorkflowRoleUsagePort workflowRoleUsage;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("bagli olmayan rol icin kullanim yoktur")
    void baglantisizRolKullanimdaDegildir() {
        int role = dynamicRole();

        assertThat(workflowRoleUsage.hasOpenWorkflowUsage(role)).isFalse();
    }

    @Test
    @DisplayName("acik kaydi olan bagli rolun aktorlugu kapatilamaz")
    void acikKaydiOlanBagliRolunAktorluguKapatilamaz() {
        int role = dynamicRole();
        bindToTaslakGonder(role);
        record("TASLAK", user(role));

        assertThat(workflowRoleUsage.hasOpenWorkflowUsage(role)).isTrue();

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setWorkflowActor(false);
        assertThatThrownBy(() -> roleAdminService.update(role, request))
                .isInstanceOf(RoleInUseException.class);

        assertThat(jdbc.queryForObject(
                "SELECT is_workflow_actor FROM roles WHERE id = ?", Boolean.class, role)).isTrue();
    }

    /**
     * WF-8 ile ayni muhafazakarlik: gecici olarak pasiflestirilen kullanici/rol koruma
     * sorgusunu daraltmaz, boylece koruma yetki kaldirarak asilamaz.
     */
    @Test
    @DisplayName("kullanici pasiflestirilse de koruma surer")
    void pasifKullaniciKorumayiKaldirmaz() {
        int role = dynamicRole();
        bindToTaslakGonder(role);
        UUID creator = user(role);
        record("TASLAK", creator);
        jdbc.update("UPDATE users SET is_active = false WHERE id = ?", creator);

        assertThat(workflowRoleUsage.hasOpenWorkflowUsage(role)).isTrue();
    }

    @Test
    @DisplayName("terminal ve silinmis kayitlar kullanim sayilmaz")
    void terminalVeSilinmisKayitlarKullanimSayilmaz() {
        int role = dynamicRole();
        bindToTaslakGonder(role);
        UUID creator = user(role);
        UUID deleted = record("TASLAK", creator);
        jdbc.update("UPDATE records SET deleted_at = now() WHERE id = ?", deleted);
        record("ONAYLANDI", creator);

        // Izin verilen yolun servis davranisi birim testlerinde; burada dogrulanan
        // sorgunun kendisi. update(...) cagrisi audit yazimi icin kimlik isterdi.
        assertThat(workflowRoleUsage.hasOpenWorkflowUsage(role)).isFalse();
    }

    @Test
    @DisplayName("pasif bag kullanim uretmez")
    void pasifBagKullanimUretmez() {
        int role = dynamicRole();
        int binding = bindToTaslakGonder(role);
        record("TASLAK", user(role));
        jdbc.update("UPDATE workflow_transitions SET is_active = false WHERE id = ?", binding);

        assertThat(workflowRoleUsage.hasOpenWorkflowUsage(role)).isFalse();
    }

    // --- fixture ---

    private int dynamicRole() {
        return jdbc.queryForObject("INSERT INTO roles(name, is_active, is_workflow_actor) "
                + "VALUES (?, true, true) RETURNING id", Integer.class, "ap2-" + UUID.randomUUID());
    }

    /** Rolu mevcut TASLAK+GONDER adimina CREATOR iliskisiyle aktor olarak baglar. */
    private int bindToTaslakGonder(int roleId) {
        return jdbc.queryForObject("""
                INSERT INTO workflow_transitions(from_status_id, action_id, actor_role_id,
                        actor_requirement, to_status_id, expected_target_role_id,
                        target_strategy, required_permission_id, is_active)
                SELECT t.from_status_id, t.action_id, ?, 'CREATOR', t.to_status_id,
                       t.expected_target_role_id, t.target_strategy, t.required_permission_id, true
                FROM workflow_transitions t
                JOIN workflow_statuses s ON s.id = t.from_status_id
                JOIN workflow_actions a ON a.id = t.action_id
                JOIN roles r ON r.id = t.actor_role_id AND r.is_system = true
                WHERE s.name = 'TASLAK' AND a.name = 'GONDER'
                RETURNING id
                """, Integer.class, roleId);
    }

    private UUID user(int roleId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, 'AP2', 'Fixture', ?, 'unused', ?, true)", id, id + "@ap2.test", roleId);
        return id;
    }

    private UUID record(String status, UUID creator) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO records(id, title, description, category_id, status, created_by, version) "
                + "VALUES (?, 'AP2 fixture', 'AP2 guard', (SELECT min(id) FROM categories), ?, ?, 0)",
                id, status, creator);
        return id;
    }
}
