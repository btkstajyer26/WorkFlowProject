package btk.staj.WorkFlowProject.subtask;

import btk.staj.WorkFlowProject.record.entity.Category;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.rbac.Permission;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.RolePermission;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.entity.User;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** V25'ten V26'ya gercek PostgreSQL/Flyway zincirini izole bir semada dogrular. */
class SubtaskSchemaMigrationIntegrationTest {

    private String schema;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private LocalContainerEntityManagerFactoryBean jpaFactory;

    @BeforeEach
    void isolateSchema() {
        schema = "subtask_schema_test_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:postgresql://" + env("DB_HOST", "localhost") + ":" + env("DB_PORT", "5432")
                + "/" + env("DB_NAME", "workflowdb") + "?currentSchema=" + schema;
        dataSource = new DriverManagerDataSource(
                url,
                env("DB_USER", "postgres"),
                env("DB_PASSWORD", "postgres"));
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void removeOnlyTheTestSchema() {
        if (jpaFactory != null) {
            jpaFactory.destroy();
        }
        if (schema != null && schema.matches("subtask_schema_test_[a-f0-9]{32}")) {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    @DisplayName("V26 Parent/Subtask semasini, kataloglarini ve JPA mappinglerini kurar")
    void upgradesPopulatedV25ToV26() {
        migrate("25");
        Fixture fixture = seedV25Record();

        Flyway flyway = migrate("26");
        flyway.validate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("26");
        assertThat(jdbc.queryForObject(
                "SELECT title FROM records WHERE id = ?", String.class, fixture.recordId()))
                .isEqualTo("V25 Parent");
        assertRecordApprovalColumnsAndConstraints(fixture.recordId());
        assertSubtaskTable(fixture);
        assertCatalogAndTransitionSeeds();
        assertSystemIdentitySeed();

        EntityManagerFactory entityManagerFactory = entityManagerFactory();
        assertThat(entityManagerFactory.getMetamodel().entity(Record.class)).isNotNull();
        assertThat(entityManagerFactory.getMetamodel().entity(Subtask.class)).isNotNull();
    }

    private void assertRecordApprovalColumnsAndConstraints(UUID recordId) {
        assertThat(columnType("records", "subtask_approval_policy")).isEqualTo("character varying");
        assertThat(columnType("records", "subtask_required_approvals")).isEqualTo("integer");
        assertThat(jdbc.queryForMap("SELECT subtask_approval_policy, subtask_required_approvals "
                + "FROM records WHERE id = ?", recordId))
                .containsEntry("subtask_approval_policy", null)
                .containsEntry("subtask_required_approvals", null);

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE records SET subtask_approval_policy = 'UNANIMOUS' WHERE id = ?", recordId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_records_subtask_approval_fields");
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE records SET subtask_required_approvals = 1 WHERE id = ?", recordId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_records_subtask_approval_fields");
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE records SET subtask_approval_policy = 'UNKNOWN', "
                        + "subtask_required_approvals = 1 WHERE id = ?", recordId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_records_subtask_approval_policy");
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE records SET subtask_approval_policy = 'MAJORITY', "
                        + "subtask_required_approvals = 0 WHERE id = ?", recordId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_records_subtask_required_approvals");

        assertThat(jdbc.update("UPDATE records SET subtask_approval_policy = 'MAJORITY', "
                + "subtask_required_approvals = 2 WHERE id = ?", recordId)).isOne();
    }

    private void assertSubtaskTable(Fixture fixture) {
        assertThat(columnType("subtasks", "id")).isEqualTo("uuid");
        assertThat(columnType("subtasks", "parent_record_id")).isEqualTo("uuid");
        assertThat(columnType("subtasks", "assigned_to")).isEqualTo("uuid");
        assertThat(columnType("subtasks", "created_by")).isEqualTo("uuid");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE connamespace = "
                + "(SELECT oid FROM pg_namespace WHERE nspname = ?) AND conname IN "
                + "('fk_subtasks_parent_record', 'fk_subtasks_assigned_to', 'fk_subtasks_created_by') "
                + "AND confdeltype = 'a'", Integer.class, schema)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = ? "
                + "AND indexname IN ('idx_subtasks_parent_record_id', 'idx_subtasks_assigned_to')",
                Integer.class, schema)).isEqualTo(2);

        UUID subtaskId = jdbc.queryForObject("INSERT INTO subtasks "
                + "(parent_record_id, title, assigned_to, created_by) VALUES (?, 'Schema Subtask', ?, ?) "
                + "RETURNING id", UUID.class, fixture.recordId(), fixture.userId(), fixture.userId());
        assertThat(jdbc.queryForMap("SELECT status, version FROM subtasks WHERE id = ?", subtaskId))
                .containsEntry("status", "DEGERLENDIRME")
                .containsEntry("version", 0);
        for (SubtaskStatus status : SubtaskStatus.values()) {
            assertThat(jdbc.update(
                    "UPDATE subtasks SET status = ? WHERE id = ?", status.name(), subtaskId))
                    .isOne();
        }

        assertThatThrownBy(() -> jdbc.update("INSERT INTO subtasks "
                + "(parent_record_id, title, assigned_to, status, created_by) "
                + "VALUES (?, 'Invalid status', ?, 'BEKLEMEDE', ?)",
                fixture.recordId(), fixture.userId(), fixture.userId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_subtasks_status");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO subtasks "
                + "(parent_record_id, title, assigned_to, created_by) "
                + "VALUES (?, 'Invalid parent', ?, ?)",
                UUID.randomUUID(), fixture.userId(), fixture.userId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_subtasks_parent_record");
    }

    private void assertCatalogAndTransitionSeeds() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_statuses WHERE "
                + "(name = 'ALT_GOREV_BEKLIYOR' AND display_name = 'Alt Görevler Bekleniyor' "
                + "AND is_terminal = false AND is_editable_by_creator = false AND is_active = true) OR "
                + "(name = 'KONTROL' AND display_name = 'Kontrol' "
                + "AND is_terminal = false AND is_editable_by_creator = false AND is_active = true)",
                Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_actions WHERE "
                + "name IN ('ALT_GOREVLERE_AYIR', 'ALT_GOREVLER_SONUCLANDI') "
                + "AND comment_required = false AND is_active = true", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_transitions", Integer.class))
                .isEqualTo(12);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_transitions t "
                + "JOIN workflow_statuses fs ON fs.id = t.from_status_id "
                + "JOIN workflow_actions a ON a.id = t.action_id "
                + "JOIN roles ar ON ar.id = t.actor_role_id "
                + "JOIN workflow_statuses ts ON ts.id = t.to_status_id "
                + "JOIN permissions p ON p.id = t.required_permission_id "
                + "WHERE fs.name = 'BSK_YRD_INCELEMESINDE' AND a.name = 'ALT_GOREVLERE_AYIR' "
                + "AND ar.system_key = 'BASKAN_YARDIMCISI' AND t.actor_requirement = 'ASSIGNEE' "
                + "AND ts.name = 'ALT_GOREV_BEKLIYOR' AND t.target_strategy = 'NONE' "
                + "AND t.expected_target_role_id IS NULL AND p.code = 'RECORD_FORWARD' AND t.is_active = true",
                Integer.class)).isOne();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM workflow_transitions t "
                + "JOIN workflow_statuses fs ON fs.id = t.from_status_id "
                + "JOIN workflow_actions a ON a.id = t.action_id "
                + "JOIN roles ar ON ar.id = t.actor_role_id "
                + "JOIN workflow_statuses ts ON ts.id = t.to_status_id "
                + "WHERE fs.name = 'ALT_GOREV_BEKLIYOR' AND a.name = 'ALT_GOREVLER_SONUCLANDI' "
                + "AND ar.system_key = 'SISTEM' AND t.actor_requirement = 'SYSTEM' "
                + "AND ts.name = 'KONTROL' AND t.target_strategy = 'NONE' "
                + "AND t.expected_target_role_id IS NULL AND t.required_permission_id IS NULL "
                + "AND t.is_active = true", Integer.class)).isOne();

        assertThatThrownBy(() -> jdbc.update("UPDATE workflow_transitions SET actor_requirement = 'ROBOT' "
                + "WHERE actor_requirement = 'SYSTEM'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_transition_actor_requirement");
    }

    private void assertSystemIdentitySeed() {
        Map<String, Object> role = jdbc.queryForMap("SELECT id, name, description, system_key, is_system, "
                + "is_workflow_actor, max_users, is_active FROM roles WHERE system_key = 'SISTEM'");
        assertThat(role)
                .containsEntry("name", "SISTEM")
                .containsEntry("description", "Uygulama içi otomatik workflow geçişlerini çalıştıran servis rolü")
                .containsEntry("system_key", "SISTEM")
                .containsEntry("is_system", true)
                .containsEntry("is_workflow_actor", true)
                .containsEntry("max_users", 1)
                .containsEntry("is_active", true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM roles WHERE system_key = 'SISTEM'", Integer.class))
                .isOne();

        Map<String, Object> user = jdbc.queryForMap("SELECT u.id, u.first_name, u.last_name, u.email, "
                + "u.password_hash, u.role_id, u.is_active, u.must_change_password, u.created_at "
                + "FROM users u JOIN roles r ON r.id = u.role_id "
                + "WHERE r.system_key = 'SISTEM'");
        assertThat(user.get("id")).isNotNull();
        assertThat(user).containsEntry("first_name", "Workflow").containsEntry("last_name", "Sistem");
        assertThat(user.get("email").toString()).endsWith(".invalid");
        assertThat(user.get("password_hash").toString())
                .matches("^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$");
        assertThat(user.get("role_id")).isEqualTo(role.get("id"));
        assertThat(user.get("created_at")).isNotNull();
        assertThat(user).containsEntry("is_active", true).containsEntry("must_change_password", false);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users u JOIN roles r ON r.id = u.role_id "
                + "WHERE r.system_key = 'SISTEM'", Integer.class)).isOne();
    }

    private Fixture seedV25Record() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users "
                + "(id, first_name, last_name, email, password_hash, role_id, is_active) "
                + "VALUES (?, 'Schema', 'User', ?, 'unused', "
                + "(SELECT id FROM roles WHERE system_key = 'CALISAN'), true)",
                userId, userId + "@subtask-schema.test");
        UUID recordId = UUID.randomUUID();
        jdbc.update("INSERT INTO records "
                + "(id, title, description, category_id, status, created_by) "
                + "VALUES (?, 'V25 Parent', 'Preserved during upgrade', "
                + "(SELECT min(id) FROM categories), 'BSK_YRD_INCELEMESINDE', ?)",
                recordId, userId);
        return new Fixture(userId, recordId);
    }

    private String columnType(String table, String column) {
        return jdbc.queryForObject("SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name = ?",
                String.class, schema, table, column);
    }

    private Flyway migrate(String target) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target(target)
                .load();
        flyway.migrate();
        return flyway;
    }

    private EntityManagerFactory entityManagerFactory() {
        if (jpaFactory == null) {
            jpaFactory = new LocalContainerEntityManagerFactoryBean();
            jpaFactory.setDataSource(dataSource);
            jpaFactory.setManagedTypes(PersistenceManagedTypes.of(
                    Category.class.getName(),
                    Record.class.getName(),
                    Subtask.class.getName(),
                    Token.class.getName(),
                    User.class.getName(),
                    Permission.class.getName(),
                    Role.class.getName(),
                    RolePermission.class.getName()));
            jpaFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            jpaFactory.setJpaPropertyMap(Map.of(
                    "hibernate.hbm2ddl.auto", "validate",
                    "hibernate.default_schema", schema));
            jpaFactory.afterPropertiesSet();
        }
        return jpaFactory.getObject();
    }

    private static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }

    private record Fixture(UUID userId, UUID recordId) {
    }
}
