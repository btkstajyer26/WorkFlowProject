package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.auth.repository.PasswordResetCodeRepository;
import btk.staj.WorkFlowProject.notification.repository.MailActionTokenRepository;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Süresi dolmuş kimlik ve aksiyon anahtarlarının gecelik temizliği.
 *
 * <p>Üç ayrı tabloya bakar; ortak nokta hepsinin <em>süreli</em> olması ve süresi
 * geçtikten sonra hiçbirinin işe yaramaması. Denetim kaydı burada tutulmaz —
 * "kim ne zaman ne yaptı" bilgisi {@code audit_logs} içindedir ve bu iş ona dokunmaz.
 */
@Component
public class TokenCleanupJob {

    /**
     * Süresi dolan şifre sıfırlama satırları hemen değil, bir gün sonra silinir:
     * denetim izinde "kod istendi ama kullanılmadı" durumu bir süre görünür kalsın.
     */
    private static final int PASSWORD_RESET_RETENTION_DAYS = 1;

    /**
     * Mail aksiyon anahtarları da aynı gerekçeyle bir gün bekletilir: "bağlantı gönderildi
     * ama kullanılmadı" durumu destek sorularında işe yarıyor.
     *
     * <p>Bu tablo bugüne kadar hiç temizlenmiyordu; yalnız kayıt veya kullanıcı silinince
     * FK cascade ile azalıyordu, yani sınırsız büyüyordu. {@code V11} temizlik için gereken
     * {@code expires_at} indeksini zaten açmıştı.
     */
    private static final int MAIL_ACTION_RETENTION_DAYS = 1;

    private final TokenRepository tokenRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final MailActionTokenRepository mailActionTokenRepository;

    public TokenCleanupJob(TokenRepository tokenRepository,
                           PasswordResetCodeRepository passwordResetCodeRepository,
                           MailActionTokenRepository mailActionTokenRepository) {
        this.tokenRepository = Objects.requireNonNull(tokenRepository, "tokenRepository");
        this.passwordResetCodeRepository = Objects.requireNonNull(
                passwordResetCodeRepository, "passwordResetCodeRepository");
        this.mailActionTokenRepository = Objects.requireNonNull(
                mailActionTokenRepository, "mailActionTokenRepository");
    }

    // Her gece 03:00'te çalışır
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanUpExpiredCredentials() {
        LocalDateTime now = LocalDateTime.now();

        List<Token> expired = tokenRepository.findAllByExpiredFalseAndExpiresAtBefore(now);
        expired.forEach(token -> token.setExpired(true));
        tokenRepository.saveAll(expired);

        passwordResetCodeRepository.deleteByExpiresAtBefore(
                now.minusDays(PASSWORD_RESET_RETENTION_DAYS));

        mailActionTokenRepository.deleteByExpiresAtBefore(
                now.minusDays(MAIL_ACTION_RETENTION_DAYS));
    }
}
