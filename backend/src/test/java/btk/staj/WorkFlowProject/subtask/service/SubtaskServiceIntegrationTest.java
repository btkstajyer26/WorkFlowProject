package btk.staj.WorkFlowProject.subtask.service;

import static org.assertj.core.api.Assertions.assertThat;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskCreateItem;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Gercek PostgreSQL flush zamanlamasiyla split() cevabinin tam
 * doldugunu dogrular - mock'lu {@code SubtaskServiceTest} bu regresyonu
 * yakalayamaz cunku Hibernate'in {@code @CreationTimestamp} doldurma
 * zamanlamasi yalniz gercek bir flush ile ortaya cikar.
 *
 * <p>{@link SubtaskJoinConcurrencyIntegrationTest} ile ayni izole-sema
 * deseni kullanilir: her test sinifi kendi rastgele semasinda calisir.</p>
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SubtaskService.split PostgreSQL")
class SubtaskServiceIntegrationTest {

    private static final String SCHEMA =
            "subtask_split_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String HOST = env("DB_HOST", "localhost");
    private static final String PORT = env("DB_PORT", "5432");
    private static final String DATABASE = env("DB_NAME", "workflowdb");
    private static final String USERNAME = env("DB_USER", "postgres");
    private static final String PASSWORD = env("DB_PASSWORD", "postgres");
    private static final String BASE_URL =
            "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;

    @DynamicPropertySource
    static void isolatedPostgresSchema(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> BASE_URL + "?currentSchema=" + SCHEMA);
        properties.add("spring.datasource.username", () -> USERNAME);
        properties.add("spring.datasource.password", () -> PASSWORD);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.flyway.create-schemas", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private SubtaskService service;
    @Autowired
    private CustomUserDetailsService users;

    private UUID deputyId;
    private UUID firstAssigneeId;
    private UUID secondAssigneeId;
    private UUID parentId;

    @BeforeEach
    void fixture() {
        deputyId = user("Yardimci", "BASKAN_YARDIMCISI");
        firstAssigneeId = user("Birinci", "CALISAN");
        secondAssigneeId = user("Ikinci", "CALISAN");
        Integer categoryId = jdbc.queryForObject("SELECT min(id) FROM categories", Integer.class);
        parentId = jdbc.queryForObject("""
                        INSERT INTO records (
                            title, description, category_id, status, created_by, assigned_to)
                        VALUES ('Split test', 'test-only', ?, 'BSK_YRD_INCELEMESINDE', ?, ?)
                        RETURNING id
                        """,
                UUID.class, categoryId, deputyId, deputyId);
        login(deputyId);
    }

    @AfterAll
    void removeOnlyTheIsolatedSchema() {
        SecurityContextHolder.clearContext();
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(BASE_URL, USERNAME, PASSWORD);
        new JdbcTemplate(dataSource).execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    @DisplayName("dönen her alt görevin createdAt alanı boş kalmaz")
    void splitResponseCarriesCreatedAtForEverySubtask() {
        SubtaskSplitResponse response = service.split(parentId, new SubtaskSplitRequest(
                SubtaskApprovalPolicy.UNANIMOUS,
                java.util.List.of(
                        new SubtaskCreateItem("Birinci alt görev", null, firstAssigneeId),
                        new SubtaskCreateItem("İkinci alt görev", null, secondAssigneeId))));

        assertThat(response.subtasks()).hasSize(2);
        for (SubtaskView view : response.subtasks()) {
            assertThat(view.createdAt())
                    .as("subtask %s createdAt", view.id())
                    .isNotNull();
            assertThat(view.id()).isNotNull();
            assertThat(view.assignedToName()).isNotBlank();
        }
    }

    private UUID user(String suffix, String roleSystemKey) {
        Integer roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = ?", Integer.class, roleSystemKey);
        return jdbc.queryForObject("""
                        INSERT INTO users (
                            first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('Split', ?, ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class,
                suffix,
                UUID.randomUUID() + "@subtask-split.invalid",
                roleId);
    }

    private void login(UUID id) {
        var principal = users.loadUserByUsername(
                jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, id));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
