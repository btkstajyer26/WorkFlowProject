package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditLogService implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(AuditLogRepository auditLogRepository, JdbcTemplate jdbcTemplate) {
        this.auditLogRepository = auditLogRepository;
        this.jdbcTemplate = jdbcTemplate;
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
     * Kayit yasam dongusu olaylari (olusturma/guncelleme/silme); durum gecisi yoktur.
     * record modulunun (Alperen & Fevzi) createRecord/updateRecord/deleteRecord
     * icinde cagirmasi icin acilmis giris noktasi.
     */
    public void recordLifecycleEvent(UUID recordId,
                                     UUID actorId,
                                     RoleName actorRole,
                                     String action,
                                     RecordStatus currentStatus,
                                     String comment) {

        AuditLog log = AuditLog.builder()
                .recordId(recordId)
                .userId(actorId)
                .roleId(resolveRoleId(actorRole))
                .action(action)
                .previousStatus(null)
                .newStatus(currentStatus.name())
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    /**
     * RoleName enum degerini, roles tablosundaki karsilik gelen id'ye cevirir.
     * roles.name kolonu RoleName enum'uyla birebir ayni yazilir.
     */
    private Integer resolveRoleId(RoleName roleName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM roles WHERE name = ?",
                    Integer.class,
                    roleName.name()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("Rol bulunamadi: " + roleName.name(), e);
        }
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