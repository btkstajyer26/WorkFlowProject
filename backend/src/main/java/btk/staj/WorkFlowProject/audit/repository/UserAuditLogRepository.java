package btk.staj.WorkFlowProject.audit.repository;

import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, UUID> {

    // Belirli bir kullanıcı üzerinde yapılan tüm işlemleri getirir
    List<UserAuditLog> findByTargetUserIdOrderByCreatedAtAsc(UUID targetUserId);

    // Belirli bir yöneticinin yaptığı tüm işlemler
    List<UserAuditLog> findByPerformedByOrderByCreatedAtDesc(UUID performedBy);
}