package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B05 — refresh token tek seferde tuketilir.
 *
 * <p>Gercek PostgreSQL ister. Onceki tasarim token'i okuyup bellekte kontrol
 * edip geri yaziyordu; ayni token ile gelen iki eszamanli istek ikisi de
 * "revoked = false" gorup rotasyonu tamamlayabiliyordu. Inceleme probu bunu
 * uretmisti: beklenen 1 basarili yenileme, gerceklesen 2.
 *
 * <p>Iki katman ayri ayri sinanir: once kosullu UPDATE'in kendisi (mekanizma),
 * sonra servis ucundan eszamanli iki yenileme (davranis).
 */
@SpringBootTest
@DisplayName("B05 - refresh token tek tuketim")
class RefreshTokenConsumptionIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private AuthService authService;

    private UUID userId;
    private String tokenValue;

    @BeforeEach
    void fixture() {
        Integer employeeRole = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'CALISAN'", Integer.class);

        userId = jdbc.queryForObject("""
                        INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('B05', 'Regresyon', ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class, UUID.randomUUID() + "@b05.invalid", employeeRole);

        tokenValue = "b05-refresh-" + UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO tokens (user_id, token, token_type, expires_at)
                        VALUES (?, ?, 'REFRESH', current_timestamp + interval '1 day')
                        """,
                userId, tokenValue);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tokens WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    /**
     * Mekanizma: kosullu UPDATE'i iki transaction ayni anda calistirdiginda
     * satiri yalnizca biri gunceller. Ikincisi satir kilidini bekler, kilit
     * birakildiginda "revoked = false" sartini yeniden degerlendirir ve 0 doner.
     */
    @Test
    @DisplayName("eszamanli iki kosullu tuketimden yalnizca biri satiri gunceller")
    void kosulluTuketimTamOlarakBirKezUygulanir() throws Exception {
        CyclicBarrier bothInTransaction = new CyclicBarrier(2);
        TransactionTemplate tx = new TransactionTemplate(txManager);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> consume(tx, bothInTransaction));
            Future<Integer> second = executor.submit(() -> consume(tx, bothInTransaction));

            int updated = first.get(20, TimeUnit.SECONDS) + second.get(20, TimeUnit.SECONDS);

            assertThat(updated)
                    .as("Bir refresh token tam olarak bir kez tuketilebilmeli")
                    .isEqualTo(1);
        }

        assertThat(jdbc.queryForObject(
                "SELECT revoked FROM tokens WHERE token = ?", Boolean.class, tokenValue))
                .isTrue();
    }

    private int consume(TransactionTemplate tx, CyclicBarrier barrier) {
        return tx.execute(status -> {
            try {
                barrier.await(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("bariyer beklenirken hata", e);
            }
            return tokenRepository.revokeIfActive(tokenValue);
        });
    }

    /**
     * Davranis: servis ucundan ayni token ile gelen iki eszamanli yenilemeden
     * yalnizca biri yeni token cifti uretir. Kaybeden istek tuketimden 0
     * aldigi icin hicbir sey yazmaz.
     */
    @Test
    @DisplayName("eszamanli iki refresh cagrisindan yalnizca biri yeni token uretir")
    void eszamanliRefreshtenTamOlarakBiriKabulEdilir() throws Exception {
        CyclicBarrier bothReady = new CyclicBarrier(2);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> attemptRefresh(bothReady));
            Future<Boolean> second = executor.submit(() -> attemptRefresh(bothReady));

            List<Boolean> outcomes = List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS));

            assertThat(outcomes)
                    .as("Ayni refresh token ile iki istekten yalnizca biri kabul edilmeli")
                    .containsExactlyInAnyOrder(true, false);
        }

        // Eski token tuketildi, yerine tam olarak bir yeni token yazildi.
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM tokens WHERE user_id = ? AND revoked = false", Integer.class, userId))
                .isEqualTo(1);
    }

    private boolean attemptRefresh(CyclicBarrier barrier) {
        try {
            barrier.await(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("bariyer beklenirken hata", e);
        }

        try {
            authService.refresh(tokenValue);
            return true;
        } catch (RuntimeException expectedForTheLoser) {
            return false;
        }
    }
}
