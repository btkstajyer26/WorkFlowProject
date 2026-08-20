package btk.staj.WorkFlowProject.auth.repository;

import btk.staj.WorkFlowProject.auth.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    /** Kullanıcının henüz tüketilmemiş kodları; yeni kod istendiğinde iptal edilir. */
    List<PasswordResetCode> findAllByUser_IdAndConsumedAtIsNull(UUID userId);

    /** Doğrulama sırasında geçerli sayılan tek kod: en son üretilen ve tüketilmemiş olan. */
    Optional<PasswordResetCode> findFirstByUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<PasswordResetCode> findByResetTokenHash(String resetTokenHash);

    /** Süresi geçmiş satırların temizliği (bkz. TokenCleanupJob). */
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
