package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Sistemde bir işlem gerçekleştiğinde bu metod çağrılır.
     * workflow ekibinin (Esra & Burak) her durum değişikliğinden
     * sonra bunu tetiklemesi gerekiyor.
     */
    public void logIslem(UUID recordId, UUID userId, Integer roleId, String action,
                         String previousStatus, String newStatus, String comment) {

        AuditLog log = AuditLog.builder()
                .recordId(recordId)
                .userId(userId)
                .roleId(roleId)
                .action(action)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .comment(comment)
                .build();

        auditLogRepository.save(log);
    }

    /**
     * Bir evrağın detay sayfasındaki "İşlem Geçmişi" tablosunu doldurmak için.
     */
    public List<AuditLog> getGecmis(UUID recordId) {
        return auditLogRepository.findByRecordIdOrderByCreatedAtAsc(recordId);
    }
}