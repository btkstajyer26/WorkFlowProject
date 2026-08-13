package btk.staj.WorkFlowProject.user.repository;

import btk.staj.WorkFlowProject.user.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {
    Optional<Token> findByToken(String token);
    List<Token> findAllByUser_IdAndRevokedFalse(UUID userId);
    List<Token> findAllByExpiredFalseAndExpiresAtBefore(LocalDateTime now);
}