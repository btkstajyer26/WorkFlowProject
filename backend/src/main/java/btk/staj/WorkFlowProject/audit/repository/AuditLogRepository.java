package btk.staj.WorkFlowProject.audit.repository;

import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Bir kaydın (evrağın) tüm işlem geçmişini getirir.
    List<AuditLog> findByRecordIdOrderByCreatedAtAsc(UUID recordId);

    // Belirli bir kullanıcının yaptığı tüm işlemler
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}