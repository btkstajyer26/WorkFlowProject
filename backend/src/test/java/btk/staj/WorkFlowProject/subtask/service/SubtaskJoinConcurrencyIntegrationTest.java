package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gercek PostgreSQL satir kilidiyle iki join denemesinin tek Parent gecisine
 * seri hale geldigini dogrular.
 *
 * <p>Test gelistirme semasini kullanmaz: her test sinifi icin rastgele bir
 * schema olusturulur, Flyway zinciri oraya uygulanir ve sinif sonunda yalniz o
 * schema silinir. Bilerek DB'siz unit test grubuna dahil degildir.</p>
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Subtask join PostgreSQL concurrency")
class SubtaskJoinConcurrencyIntegrationTest {

    private static final String SCHEMA =
            "subtask_join_test_" + UUID.randomUUID().toString().replace("-", "");
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
    private PlatformTransactionManager txManager;
    @Autowired
    private RecordRepository records;
    @Autowired
    private SubtaskJoinService joins;

    private UUID firstAssigneeId;
    private UUID secondAssigneeId;
    private UUID parentId;

    @BeforeEach
    void fixture() {
        firstAssigneeId = user("Birinci");
        secondAssigneeId = user("İkinci");
        Integer categoryId = jdbc.queryForObject("SELECT min(id) FROM categories", Integer.class);
        parentId = jdbc.queryForObject("""
                        INSERT INTO records (
                            title, description, category_id, status, created_by,
                            subtask_approval_policy, subtask_required_approvals)
                        VALUES ('Join race', 'test-only', ?, 'ALT_GOREV_BEKLIYOR', ?, 'UNANIMOUS', 2)
                        RETURNING id
                        """,
                UUID.class, categoryId, firstAssigneeId);

        insertTerminalSubtask("Birinci alt görev", firstAssigneeId);
        insertTerminalSubtask("İkinci alt görev", secondAssigneeId);
    }

    @AfterAll
    void removeOnlyTheIsolatedSchema() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(BASE_URL, USERNAME, PASSWORD);
        new JdbcTemplate(dataSource).execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    @DisplayName("eş zamanlı iki son join Parent'ı yalnız bir kez ilerletir")
    void concurrentJoinAttemptsTransitionTheParentAtMostOnce() throws Exception {
        CountDownLatch firstHoldsParentLock = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(txManager);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<SubtaskJoinService.JoinOutcome> first = executor.submit(() ->
                    transaction.execute(status -> {
                        records.findByIdForUpdate(parentId).orElseThrow();
                        firstHoldsParentLock.countDown();
                        await(releaseFirst);
                        return joins.joinIfReady(parentId);
                    }));

            assertThat(firstHoldsParentLock.await(15, TimeUnit.SECONDS)).isTrue();

            Future<SubtaskJoinService.JoinOutcome> second = executor.submit(() -> {
                secondStarted.countDown();
                return joins.joinIfReady(parentId);
            });
            assertThat(secondStarted.await(15, TimeUnit.SECONDS)).isTrue();

            try {
                assertThatThrownBy(() -> second.get(2, TimeUnit.SECONDS))
                        .as("ikinci join Parent satır kilidinde beklemeli")
                        .isInstanceOf(TimeoutException.class);
            } finally {
                releaseFirst.countDown();
            }

            List<SubtaskJoinService.JoinOutcome> outcomes = List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS));
            assertThat(outcomes).containsExactlyInAnyOrder(
                    SubtaskJoinService.JoinOutcome.TRANSITIONED_APPROVED,
                    SubtaskJoinService.JoinOutcome.ALREADY_JOINED);
        }

        assertThat(jdbc.queryForObject(
                "SELECT status FROM records WHERE id = ?", String.class, parentId))
                .isEqualTo("KONTROL");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE record_id = ? "
                        + "AND action = 'ALT_GOREVLER_SONUCLANDI'",
                Integer.class,
                parentId))
                .isOne();
    }

    private UUID user(String suffix) {
        Integer roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'CALISAN'", Integer.class);
        return jdbc.queryForObject("""
                        INSERT INTO users (
                            first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('Join', ?, ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class,
                suffix,
                UUID.randomUUID() + "@subtask-join.invalid",
                roleId);
    }

    private void insertTerminalSubtask(String title, UUID assignedTo) {
        jdbc.update("""
                        INSERT INTO subtasks (
                            parent_record_id, title, assigned_to, status, created_by, completed_at)
                        VALUES (?, ?, ?, 'TAMAMLANDI', ?, current_timestamp)
                        """,
                parentId,
                title,
                assignedTo,
                firstAssigneeId);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("join bariyeri zaman aşımına uğradı");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("join bariyeri beklenirken kesildi", exception);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
