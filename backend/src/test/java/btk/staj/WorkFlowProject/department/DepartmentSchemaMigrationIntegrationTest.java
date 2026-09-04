package btk.staj.WorkFlowProject.department;

import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.*;

/** Runs the real migration chain in a unique PostgreSQL schema for every test. */
class DepartmentSchemaMigrationIntegrationTest {
    private String schema;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private LocalContainerEntityManagerFactoryBean jpaFactory;

    @BeforeEach
    void isolateSchema() {
        schema = "dept_schema_test_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:postgresql://" + env("DB_HOST", "localhost") + ":" + env("DB_PORT", "5432")
                + "/" + env("DB_NAME", "workflowdb") + "?currentSchema=" + schema;
        dataSource = new DriverManagerDataSource(url, env("DB_USER", "postgres"), env("DB_PASSWORD", "postgres"));
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void removeOnlyTheTestSchema() {
        if (jpaFactory != null) jpaFactory.destroy();
        // This identifier is generated here, never read from environment/user input.
        // Do not use Flyway.clean(), which could also target configured application schemas.
        if (schema != null && schema.matches("dept_schema_test_[a-f0-9]{32}")) {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void installsCompleteChainIntoEmptySchemaAndValidatesJpaMappings() {
        Flyway flyway = migrate("22");
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("22");
        assertFinalSchema();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_transitions", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_actions WHERE name = 'DEPARTMANA_GONDER'",
                Integer.class)).isZero();
        entityManagerFactory(); // Hibernate validates the real department entity mappings.
    }

    @Test
    void upgradesPopulatedV21WithoutChangingRowsOrPriorChecksums() {
        migrate("21");
        seedDepartmentData();
        Map<String, List<Map<String, Object>>> before = rows();
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, checksum FROM flyway_schema_history ORDER BY installed_rank");

        Flyway upgraded = migrate("22");

        upgraded.validate();
        assertThat(rows()).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT version, checksum FROM flyway_schema_history "
                + "WHERE version <> '22' OR version IS NULL ORDER BY installed_rank")).isEqualTo(history);
        assertFinalSchema();
    }

    @Test
    void preexistingSelfParentAbortsUpgradeWithoutPartialSchemaOrDataChanges() {
        migrate("21");
        Fixture fixture = seedDepartmentData();
        jdbc.update("UPDATE departments SET parent_department_id = id WHERE id = ?", fixture.department());
        Map<String, List<Map<String, Object>>> before = rows();

        assertThatThrownBy(() -> migrate("22")).isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("chk_department_parent_not_self");

        assertThat(rows()).isEqualTo(before);
        assertThat(nameLength()).isEqualTo(100); // ALTER TYPE precedes the failing CHECK and must roll back.
        assertThat(deleteRules()).containsEntry("fk_department_member_department", "c")
                .containsEntry("fk_department_member_user", "c").containsEntry("fk_routing_department", "c");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE connamespace = "
                + "(SELECT oid FROM pg_namespace WHERE nspname = ?) AND conname = 'chk_department_parent_not_self'",
                Integer.class, schema)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '22'",
                Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"memberDepartment", "memberUser", "routingDepartment"})
    void restrictsParentDeletionAndPreservesReferencingRows(String reference) {
        migrate("22");
        Fixture fixture = seedDepartmentData();
        // Separate the FK protections: no record assignment or hierarchy may mask the tested FK.
        jdbc.update("DELETE FROM records");
        jdbc.update("UPDATE departments SET parent_department_id = NULL");
        if (reference.equals("routingDepartment")) jdbc.update("DELETE FROM department_members");
        else jdbc.update("DELETE FROM department_routing_rules");
        Map<String, List<Map<String, Object>>> before = rows();
        String expectedConstraint = switch (reference) {
            case "memberDepartment" -> "fk_department_member_department";
            case "memberUser" -> "fk_department_member_user";
            default -> "fk_routing_department";
        };

        assertThatThrownBy(() -> {
            if (reference.equals("memberUser")) jdbc.update("DELETE FROM users WHERE id = ?", fixture.user());
            else jdbc.update("DELETE FROM departments WHERE id = ?", fixture.department());
        }).isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining(expectedConstraint);
        assertThat(rows()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Integer.class, fixture.user())).isOne();
    }

    @Test
    void rejectsSelfParentOnInsertAndUpdateButAcceptsNormalHierarchy() {
        migrate("22");
        int root = department("Root", null);
        int child = department("Child", root);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO departments(id, name, parent_department_id) "
                + "VALUES (10000, 'Self', 10000)"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_department_parent_not_self");
        assertThatThrownBy(() -> jdbc.update("UPDATE departments SET parent_department_id = id WHERE id = ?", child))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_department_parent_not_self");
        assertThat(jdbc.queryForObject("SELECT parent_department_id FROM departments WHERE id = ?", Integer.class, child))
                .isEqualTo(root);
    }

    @Test
    void jpaStores101And150CharactersButRejects151AndDuplicateNames() {
        migrate("22");
        entityManagerFactory();
        int shortId = persistDepartment("a".repeat(101));
        int maxId = persistDepartment("b".repeat(150));

        String shortName = inJpa(em -> em.find(DepartmentEntity.class, shortId).getName());
        String maxName = inJpa(em -> em.find(DepartmentEntity.class, maxId).getName());
        assertThat(shortName).hasSize(101);
        assertThat(maxName).hasSize(150);
        assertThatThrownBy(() -> persistDepartment("c".repeat(151)))
                .hasStackTraceContaining("value too long for type character varying(150)");
        assertThatThrownBy(() -> persistDepartment("a".repeat(101)))
                .hasStackTraceContaining("departments_name_key");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM departments", Integer.class)).isEqualTo(2);
    }

    @Test
    void preservesMultipleMembershipRoutingUniquenessAndAssignmentExclusion() {
        migrate("22");
        Fixture fixture = seedDepartmentData();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM department_members WHERE user_id = ?",
                Integer.class, fixture.user())).isEqualTo(2);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO department_members(department_id, user_id) VALUES (?, ?)",
                fixture.department(), fixture.user())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO department_routing_rules "
                + "(department_id, from_status_id, action_id, target_role_id) SELECT department_id, from_status_id, "
                + "action_id, target_role_id FROM department_routing_rules LIMIT 1"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_routing_dept_status_action");
        assertThatThrownBy(() -> jdbc.update("UPDATE records SET assigned_to = ? WHERE id = ?",
                fixture.user(), fixture.record())).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_records_assignment_exclusive");
        // Both NULL and each single assignment are allowed; this is not a global XOR.
        jdbc.update("UPDATE records SET assigned_department_id = NULL, assigned_to = NULL WHERE id = ?", fixture.record());
        jdbc.update("UPDATE records SET assigned_to = ? WHERE id = ?", fixture.user(), fixture.record());
        assertThatThrownBy(() -> jdbc.update("UPDATE records SET assigned_department_id = ? WHERE id = ?",
                fixture.department(), fixture.record())).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_records_assignment_exclusive");
    }

    private Flyway migrate(String target) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target(target).load();
        flyway.migrate();
        return flyway;
    }

    private void assertFinalSchema() {
        assertThat(nameLength()).isEqualTo(150);
        assertThat(deleteRules()).containsEntry("fk_department_member_department", "r")
                .containsEntry("fk_department_member_user", "r").containsEntry("fk_routing_department", "r");
        assertThat(jdbc.queryForObject("SELECT convalidated FROM pg_constraint WHERE connamespace = "
                + "(SELECT oid FROM pg_namespace WHERE nspname = ?) AND conname = 'chk_department_parent_not_self'",
                Boolean.class, schema)).isTrue();
    }

    private int nameLength() {
        return jdbc.queryForObject("SELECT character_maximum_length FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = 'departments' AND column_name = 'name'", Integer.class, schema);
    }

    private Map<String, String> deleteRules() {
        Map<String, String> result = new LinkedHashMap<>();
        jdbc.query("SELECT conname, confdeltype::text FROM pg_constraint WHERE connamespace = "
                + "(SELECT oid FROM pg_namespace WHERE nspname = ?) AND contype = 'f'",
                rs -> { result.put(rs.getString(1), rs.getString(2)); }, schema);
        return result;
    }

    private Fixture seedDepartmentData() {
        int parent = department("Parent", null);
        int child = department("Child", parent);
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, 'Schema', 'Test', ?, 'unused', (SELECT id FROM roles WHERE system_key = 'CALISAN'), true)",
                user, user + "@department-schema.test");
        jdbc.update("INSERT INTO department_members(department_id, user_id) VALUES (?, ?), (?, ?)",
                parent, user, child, user);
        jdbc.update("INSERT INTO department_routing_rules(department_id, from_status_id, action_id, target_role_id) "
                + "VALUES (?, (SELECT id FROM workflow_statuses WHERE name = 'BSK_YRD_INCELEMESINDE'), "
                + "(SELECT id FROM workflow_actions WHERE name = 'BASKANA_ILET'), "
                + "(SELECT id FROM roles WHERE system_key = 'CALISAN'))", parent);
        UUID record = UUID.randomUUID();
        jdbc.update("INSERT INTO records(id, title, description, category_id, status, created_by, assigned_department_id) "
                + "VALUES (?, 'Department fixture', 'Preserved during upgrade', (SELECT min(id) FROM categories), "
                + "'BSK_YRD_INCELEMESINDE', ?, ?)", record, user, parent);
        return new Fixture(parent, user, record);
    }

    private int department(String name, Integer parent) {
        return jdbc.queryForObject("INSERT INTO departments(name, parent_department_id) VALUES (?, ?) RETURNING id",
                Integer.class, name, parent);
    }

    private Map<String, List<Map<String, Object>>> rows() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : List.of("departments", "department_routing_rules", "records", "workflow_transitions")) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        result.put("department_members", jdbc.queryForList("SELECT * FROM department_members ORDER BY department_id, user_id"));
        return result;
    }

    private EntityManagerFactory entityManagerFactory() {
        if (jpaFactory == null) {
            jpaFactory = new LocalContainerEntityManagerFactoryBean();
            jpaFactory.setDataSource(dataSource);
            jpaFactory.setPackagesToScan("btk.staj.WorkFlowProject.department.entity");
            jpaFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            jpaFactory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate", "hibernate.default_schema", schema));
            jpaFactory.afterPropertiesSet();
        }
        return jpaFactory.getObject();
    }

    private int persistDepartment(String name) {
        return inJpa(em -> {
            DepartmentEntity department = new DepartmentEntity();
            department.setName(name);
            department.setActive(true);
            em.persist(department);
            em.flush();
            return department.getId();
        });
    }

    private <T> T inJpa(Function<EntityManager, T> action) {
        try (EntityManager em = entityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            try {
                T result = action.apply(em);
                em.getTransaction().commit();
                return result;
            } catch (RuntimeException ex) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw ex;
            }
        }
    }

    private static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }

    private record Fixture(int department, UUID user, UUID record) {}
}
