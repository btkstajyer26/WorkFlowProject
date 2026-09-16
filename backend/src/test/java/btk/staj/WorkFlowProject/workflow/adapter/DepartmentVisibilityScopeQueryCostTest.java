package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.port.DepartmentVisibilityPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R07 - "Departman eligibility sorgu sayisi olceklenebilir mi?" sorusunun
 * OLCULMUS cevabi. Tahmin degil: Hibernate Statistics.getPrepareStatementCount()
 * ile DepartmentVisibilityAdapter.scopesFor(...) tek cagrisinin urettigi
 * GERCEK SQL sorgu sayisi sayiliyor.
 *
 * <p>Kok neden (DepartmentRoutingAdapter.resolve): tek bir resolve() cagrisi
 * en fazla DORT ayri sorgu uretir - isActiveDepartment, routing kurali
 * arama, hedef rol dogrulama, uygun uye listesi. DepartmentVisibilityAdapter
 * bu metodu her (aktif departman x gecis kurali) cifti icin tekrar cagirir;
 * hicbir toplu (batch) sorgu veya departman-basi memoization yoktur. Sonuc:
 * sorgu sayisi departman uyelik sayisiyla DOGRUSAL (O(D)) artar, sabit
 * degildir.
 *
 * <p>Gercek PostgreSQL ister.
 */
@SpringBootTest
@DisplayName("R07 - departman eligibility sorgu maliyeti olcumu")
class DepartmentVisibilityScopeQueryCostTest {

    @Autowired
    private DepartmentVisibilityPort departmentVisibility;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private UUID actorId;
    private final List<Integer> createdDepartmentIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Integer departmentId : createdDepartmentIds) {
            jdbc.update("DELETE FROM department_routing_rules WHERE department_id = ?", departmentId);
            jdbc.update("DELETE FROM department_members WHERE department_id = ?", departmentId);
            jdbc.update("DELETE FROM departments WHERE id = ?", departmentId);
        }
        if (actorId != null) jdbc.update("DELETE FROM users WHERE id = ?", actorId);
    }

    /**
     * Iki farkli uyelik sayisiyla olcup orantiyi gosterir - tek bir sayi
     * "yavas/hizli" demez, BUYUME EGRISINI kanitlar. Kucuk (2) ve orta
     * buyuklukte (20 - "epey departmana uye, ama olagan disi olmayan bir
     * kullanici") uyelik sayisiyla karsilastirilir.
     */
    @Test
    @DisplayName("SQL sorgu sayisi departman uyelik sayisiyla dogrusal artiyor")
    void queryCountGrowsLinearlyWithDepartmentMembership() {
        actorId = seedActor();

        long statementsFor2 = measureStatementsFor(2);
        long statementsFor20 = measureStatementsFor(20);

        System.out.printf(
                "%nR07 olcum sonucu - DepartmentVisibilityAdapter.scopesFor tek cagrisi:%n"
                        + "  2 departman uyeligi  -> %d SQL statement%n"
                        + "  20 departman uyeligi -> %d SQL statement%n"
                        + "  10x uyelikte sorgu sayisi %.1fx artti (dogrusal buyume beklenir: ~10x)%n%n",
                statementsFor2, statementsFor20, statementsFor20 / (double) statementsFor2);

        // Sabit maliyet olsaydi (orn. tek bir toplu sorgu) 20 departmanlik
        // olcum 2 departmanlikle ayni civarda kalirdi. Burada BUYUMEYI
        // kanitliyoruz - department sayisi arttikca sorgu sayisi da artiyor,
        // bu da O(D) davranisinin gercekten var oldugunun kaniti.
        assertThat(statementsFor20)
                .as("20 departman uyeligi, 2 departmanlikten belirgin sekilde daha fazla sorgu uretmeli "
                        + "(sabit maliyetli bir tasarimda ikisi yakin cikardi)")
                .isGreaterThan(statementsFor2 * 5);
    }

    private long measureStatementsFor(int departmentCount) {
        List<Integer> departments = createDepartmentsWithMembershipAndRouting(departmentCount);

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        VisibilityActor actor = new VisibilityActor(
                actorId,
                actorRoleId(),
                Optional.empty(),
                Set.of("RECORD_VIEW", "RECORD_FORWARD"));

        departmentVisibility.scopesFor(actor);

        long statementCount = statistics.getPrepareStatementCount();

        for (Integer departmentId : departments) {
            jdbc.update("DELETE FROM department_routing_rules WHERE department_id = ?", departmentId);
            jdbc.update("DELETE FROM department_members WHERE department_id = ?", departmentId);
            jdbc.update("DELETE FROM departments WHERE id = ?", departmentId);
        }
        createdDepartmentIds.removeAll(departments);

        return statementCount;
    }

    private UUID seedActor() {
        Integer roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'BASKAN_YARDIMCISI'", Integer.class);
        return jdbc.queryForObject("""
                        INSERT INTO users(first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('R07', 'Yukleme', ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class, UUID.randomUUID() + "@r07.invalid", roleId);
    }

    private RoleId actorRoleId() {
        Integer roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'BASKAN_YARDIMCISI'", Integer.class);
        return new RoleId(roleId);
    }

    /**
     * Her departmanda: aktor uye, ve BSK_YRD_INCELEMESINDE + BASKANA_ILET icin
     * hedefi aktorun kendi rolune (BASKAN_YARDIMCISI) cozen bir routing kurali
     * var - yani resolve() her departmanda TAM yola girip dort sorgunun hepsini
     * calistiriyor, en kotu senaryo degil, GERCEKCI "her departman calisir
     * durumda" senaryosu.
     */
    private List<Integer> createDepartmentsWithMembershipAndRouting(int count) {
        Integer roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'BASKAN_YARDIMCISI'", Integer.class);
        List<Integer> departments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Integer departmentId = jdbc.queryForObject(
                    "INSERT INTO departments(name, is_active) VALUES (?, true) RETURNING id",
                    Integer.class, "R07-Dept-" + UUID.randomUUID());
            jdbc.update("INSERT INTO department_members(department_id, user_id) VALUES (?, ?)",
                    departmentId, actorId);
            jdbc.update("""
                            INSERT INTO department_routing_rules(department_id, from_status_id, action_id, target_role_id)
                            VALUES (?, (SELECT id FROM workflow_statuses WHERE name = 'BSK_YRD_INCELEMESINDE'),
                                       (SELECT id FROM workflow_actions WHERE name = 'BASKANA_ILET'), ?)
                            """,
                    departmentId, roleId);
            departments.add(departmentId);
        }
        createdDepartmentIds.addAll(departments);
        return departments;
    }
}