package btk.staj.WorkFlowProject.audit.repository;

import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code audit_logs} gibi append-only'dir; silme metotlari bilerek
 * tanimlanmamistir (bkz. {@link AuditLogRepository}).
 */
public interface UserAuditLogRepository extends Repository<UserAuditLog, UUID> {

    UserAuditLog save(UserAuditLog userAuditLog);

    Optional<UserAuditLog> findById(UUID id);

    /** Belirli bir kullanici uzerinde yapilan tum islemler, eskiden yeniye. */
    List<UserAuditLog> findByTargetUserIdOrderByCreatedAtAsc(UUID targetUserId);

    /** Belirli bir yoneticinin yaptigi islemler; sinirsiz buyuyebilir, sayfalanir. */
    Page<UserAuditLog> findByPerformedByOrderByCreatedAtDesc(UUID performedBy, Pageable pageable);
}
