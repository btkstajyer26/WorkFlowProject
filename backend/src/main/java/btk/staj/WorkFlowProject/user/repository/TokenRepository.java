package btk.staj.WorkFlowProject.user.repository;

import btk.staj.WorkFlowProject.user.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {
    Optional<Token> findByToken(String token);
    List<Token> findAllByUser_IdAndRevokedFalse(UUID userId);
    List<Token> findAllByExpiredFalseAndExpiresAtBefore(LocalDateTime now);

    /**
     * Refresh token'i tek seferde tuketir (B05).
     *
     * <p>Oku-kontrol-yaz yerine kosullu UPDATE kullanilir: {@code revoked = false}
     * sarti WHERE icinde oldugu icin ayni token ile gelen iki eszamanli istekten
     * yalnizca biri 1 satir gunceller. Ikincisi satir kilidini bekler, kilit
     * birakildiginda sarti yeniden degerlendirir ve 0 doner.
     *
     * <p>{@code clearAutomatically} bilerek acilmadi: cagiran, ayni transaction
     * icinde yuklenmis {@link Token} ve {@code User} orneklerini kullanmaya devam
     * ediyor. Bellekteki bayat {@code revoked} degeri zararsizdir, cunku entity
     * uzerinde degisiklik yapilmadigindan dirty-check bir UPDATE uretmez.
     */
    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Token t SET t.revoked = true WHERE t.token = :token AND t.revoked = false")
    int revokeIfActive(@Param("token") String token);
}
