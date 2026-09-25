package btk.staj.WorkFlowProject.notification.repository;

import btk.staj.WorkFlowProject.notification.entity.MailActionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface MailActionTokenRepository extends JpaRepository<MailActionToken, UUID> {

    Optional<MailActionToken> findByTokenHash(String tokenHash);

    /**
     * Ayni evrak/kisi icin acik kalmis eski anahtarlari kapatir.
     *
     * <p>Evrak her el degistirdiginde yeni bir bildirim gider; onceki
     * bildirimdeki baglanti da hala acik kalirsa kullanici eski postadaki
     * dugmeye basip beklemedigi bir aksiyonu tetikleyebilir. Yeni anahtar
     * verilmeden once eskiler tuketilmis sayilir.
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE MailActionToken t
               SET t.consumedAt = :now
             WHERE t.recordId = :recordId
               AND t.user.id = :userId
               AND t.consumedAt IS NULL
            """)
    int consumeOpenTokens(@Param("recordId") UUID recordId,
                          @Param("userId") UUID userId,
                          @Param("now") LocalDateTime now);

    /**
     * Suresi gecmis anahtarlari siler.
     *
     * <p>{@code V11} {@code idx_mail_action_tokens_expires_at} indeksini tam bu toplu
     * temizlik icin acmisti. Silinen satirlar zaten kullanilamaz durumda: suresi gecmis
     * bir anahtar tuketilemez. Tuketilmis anahtarlar da suresi dolunca ayni sorguyla
     * gider; ayri bir kosula gerek yoktur.
     *
     * <p>Bu bir denetim kaydi degildir; kim ne zaman ne yapti bilgisi {@code audit_logs}
     * icinde tutulur ve buradan silme onu etkilemez.
     *
     * @return silinen satir sayisi
     */
    @Transactional
    @Modifying
    int deleteByExpiresAtBefore(LocalDateTime threshold);
}
