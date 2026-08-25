package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.auth.repository.PasswordResetCodeRepository;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDateTime;

@Component
public class TokenCleanupJob {

    /**
     * Süresi dolan şifre sıfırlama satırları hemen değil, bir gün sonra silinir:
     * denetim izinde "kod istendi ama kullanılmadı" durumu bir süre görünür kalsın.
     */
    private static final int PASSWORD_RESET_RETENTION_DAYS = 1;

    private final TokenRepository tokenRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;

    public TokenCleanupJob(TokenRepository tokenRepository,
                           PasswordResetCodeRepository passwordResetCodeRepository) {
        this.tokenRepository = tokenRepository;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
    }

    // Her gece 03:00'te çalışır
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void markExpiredTokens() {
        List<Token> expired = tokenRepository.findAllByExpiredFalseAndExpiresAtBefore(LocalDateTime.now());
        expired.forEach(token -> token.setExpired(true));
        tokenRepository.saveAll(expired);

        passwordResetCodeRepository.deleteByExpiresAtBefore(
                LocalDateTime.now().minusDays(PASSWORD_RESET_RETENTION_DAYS));
    }
}
