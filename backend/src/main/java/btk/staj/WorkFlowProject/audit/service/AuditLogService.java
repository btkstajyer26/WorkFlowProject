package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditLogService implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Sistemde bir işlem gerçekleştiğinde bu metod çağrılır.
     * workflow ekibinin (Esra & Burak) her durum değişikliğinden
     * sonra bunu tetiklemesi gerekiyor.
     */
    @Override
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
    public List<AuditLogResponse> getGecmis(UUID recordId) {
        return auditLogRepository.findByRecordIdOrderByCreatedAtAsc(recordId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .recordId(log.getRecordId())
                .userId(log.getUserId())
                .roleId(log.getRoleId())
                .action(log.getAction())
                .previousStatus(log.getPreviousStatus())
                .newStatus(log.getNewStatus())
                .comment(log.getComment())
                .createdAt(log.getCreatedAt())
                .build();
    }
}