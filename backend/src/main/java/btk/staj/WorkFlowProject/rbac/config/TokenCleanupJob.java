package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.time.LocalDateTime;

@Component
public class TokenCleanupJob {

    private final TokenRepository tokenRepository;

    public TokenCleanupJob(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    // Her gece 03:00'te çalışır
    @Scheduled(cron = "0 0 3 * * *")
    public void markExpiredTokens() {
        List<Token> expired = tokenRepository.findAllByExpiredFalseAndExpiresAtBefore(LocalDateTime.now());
        expired.forEach(token -> token.setExpired(true));
        tokenRepository.saveAll(expired);
    }
}