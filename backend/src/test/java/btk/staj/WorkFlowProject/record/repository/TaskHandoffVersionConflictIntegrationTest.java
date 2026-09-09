package btk.staj.WorkFlowProject.record.repository;

import btk.staj.WorkFlowProject.record.entity.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * B03 — gorev devrinin toplu guncellemeleri version'i artirmiyordu.
 *
 * <p>Gercek PostgreSQL ister. Onceki tasarimda {@code devretBekleyenIsleri} ve
 * {@code updateLastDeputyId} duz JPQL UPDATE'lerdi ve {@code records.version}
 * kolonuna dokunmuyorlardi. Bir transaction kaydin eski snapshot'ini tuttugu
 * surece, devir commit olduktan sonra bile o snapshot uzerinden yapilan yazim
 * hicbir catisma almadan basariyla gidiyordu (inceleme probu: beklenen
 * catisma, gerceklesen sessiz basari).
 *
 * <p>RefreshTokenConsumptionIntegrationTest (B05) ile ayni desen: iki gercek
 * transaction, CountDownLatch ile senkronize, gercek PostgreSQL uzerinde.
 */
@SpringBootTest
@DisplayName("B03 - gorev devri version catismasi")
class TaskHandoffVersionConflictIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private RecordRepository recordRepository;

    private UUID oldUserId;
    private UUID newUserId;
    private UUID recordId;

    @BeforeEach
    void fixture() {
        Integer employeeRole = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'CALISAN'", Integer.class);

        oldUserId = jdbc.queryForObject("""
                        INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('B03', 'Eski', ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class, UUID.randomUUID() + "@b03.invalid", employeeRole);

        newUserId = jdbc.queryForObject("""
                        INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('B03', 'Yeni', ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class, UUID.randomUUID() + "@b03.invalid", employeeRole);

        Integer categoryId = jdbc.queryForObject("SELECT id FROM categories LIMIT 1", Integer.class);

        recordId = jdbc.queryForObject("""
                        INSERT INTO records (title, description, category_id, status, created_by, assigned_to, last_deputy_id)
                        VALUES ('B03 regresyon', 'test', ?, 'TASLAK', ?, ?, ?)
                        RETURNING id
                        """,
                UUID.class, categoryId, oldUserId, oldUserId, oldUserId);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM records WHERE id = ?", recordId);
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", oldUserId, newUserId);
    }

    /**
     * Sirali: A once kaydi okuyup version'i eline aliyor -> B devir'i tamamlayip
     * commit ediyor (version artiyor) -> A elindeki bayat snapshot'i degistirip
     * flush ediyor. A'nin flush'i optimistic lock exception ile reddedilmeli.
     */
    @Test
    @DisplayName("devir commit olduktan sonra bayat snapshot'a yazim reddedilir")
    void devirSirasindaBayatYazimReddedilir() throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch handoffCommitted = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(txManager);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> staleWriter = executor.submit(() -> tx.execute(status -> {
                Record record = recordRepository.findById(recordId).orElseThrow();
                loaded.countDown();
                await(handoffCommitted);
                record.setTitle("bayat yazim - gecmemeli");
                recordRepository.saveAndFlush(record);
                return null;
            }));

            Future<?> handoff = executor.submit(() -> {
                await(loaded);
                tx.execute(status -> {
                    recordRepository.devretBekleyenIsleri(oldUserId, newUserId);
                    return null;
                });
                handoffCommitted.countDown();
                return null;
            });

            handoff.get(20, TimeUnit.SECONDS);

            assertThatThrownBy(() -> staleWriter.get(20, TimeUnit.SECONDS))
                    .as("Devir commit olduktan sonra eski snapshot ile yazim catisma almali")
                    .hasCauseInstanceOf(ObjectOptimisticLockingFailureException.class);
        }

        assertThat(jdbc.queryForObject("SELECT assigned_to FROM records WHERE id = ?", UUID.class, recordId))
                .as("Devir basariyla uygulanmis olmali")
                .isEqualTo(newUserId);
        assertThat(jdbc.queryForObject("SELECT title FROM records WHERE id = ?", String.class, recordId))
                .as("Reddedilen bayat yazim kalici olmamali")
                .isEqualTo("B03 regresyon");
        assertThat(jdbc.queryForObject("SELECT version FROM records WHERE id = ?", Integer.class, recordId))
                .as("Version yalniz devir tarafindan bir kez artirilmali")
                .isEqualTo(1);
    }

    /**
     * Ayni yaris, updateLastDeputyId icin. Bşk. Yrd. koltugu el degistirdiginde
     * eski kullaniciyi "son Bşk. Yrd." olarak referanslayan kayitlar da ayni
     * version korumasina tabi olmali.
     */
    @Test
    @DisplayName("son Bşk. Yrd. devri commit olduktan sonra bayat snapshot'a yazim reddedilir")
    void lastDeputyDevriSirasindaBayatYazimReddedilir() throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch handoffCommitted = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(txManager);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> staleWriter = executor.submit(() -> tx.execute(status -> {
                Record record = recordRepository.findById(recordId).orElseThrow();
                loaded.countDown();
                await(handoffCommitted);
                record.setTitle("bayat yazim - gecmemeli");
                recordRepository.saveAndFlush(record);
                return null;
            }));

            Future<?> handoff = executor.submit(() -> {
                await(loaded);
                tx.execute(status -> {
                    recordRepository.updateLastDeputyId(oldUserId, newUserId);
                    return null;
                });
                handoffCommitted.countDown();
                return null;
            });

            handoff.get(20, TimeUnit.SECONDS);

            assertThatThrownBy(() -> staleWriter.get(20, TimeUnit.SECONDS))
                    .as("Son Bşk. Yrd. devri commit olduktan sonra eski snapshot ile yazim catisma almali")
                    .hasCauseInstanceOf(ObjectOptimisticLockingFailureException.class);
        }

        assertThat(jdbc.queryForObject("SELECT last_deputy_id FROM records WHERE id = ?", UUID.class, recordId))
                .as("Son Bşk. Yrd. devri basariyla uygulanmis olmali")
                .isEqualTo(newUserId);
        assertThat(jdbc.queryForObject("SELECT version FROM records WHERE id = ?", Integer.class, recordId))
                .as("Version yalniz devir tarafindan bir kez artirilmali")
                .isEqualTo(1);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("bariyer zaman asimina ugradi");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("bariyer beklenirken kesildi", e);
        }
    }
}