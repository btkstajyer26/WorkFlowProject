package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowTransitionAudit;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Onay akisinin {@link AuditService} portunun denetim izi tarafindaki
 * karsiligi. Onay akisi kendi modelini ({@link WorkflowTransitionAudit})
 * gonderir; bu sinif onu {@code audit_logs} satirina cevirir.
 *
 * <p>Cevrimdeki tek gercek is rol esleme: onay akisi rolu {@link RoleName}
 * enum'u olarak tasir, tablo ise {@code roles(id)}'ye FK tutar. Esleme projedeki
 * yerlesik kurala gore {@code roles.name} uzerinden yapilir.
 */
@Service
public class AuditLogService implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final RoleRepository roleRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, RoleRepository roleRepository) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
    }

    /**
     * Basarili bir durum gecisini denetim izine yazar. Onay akisi bu cagriyi
     * kayit guncellemesiyle ayni transaction icinde yapar; buradan firlayan
     * hata gecisi de geri alir.
     */
    @Override
    public void record(WorkflowTransitionAudit audit) {
        Objects.requireNonNull(audit, "audit");

        AuditLog log = AuditLog.builder()
                .recordId(audit.recordId())
                .userId(audit.actorId())
                .roleId(resolveRoleId(audit.actorRole()))
                .action(audit.action().name())
                .previousStatus(audit.previousStatus().name())
                .newStatus(audit.newStatus().name())
                .comment(audit.comment())
                .createdAt(LocalDateTime.ofInstant(audit.performedAt(), ZoneId.systemDefault()))
                .build();

        auditLogRepository.save(log);
    }

    /** Bir evragin detay sayfasindaki "Islem Gecmisi" tablosunu doldurur. */
    public List<AuditLogResponse> getGecmis(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId");
        return auditLogRepository.findHistoryByRecordId(recordId);
    }

    private Integer resolveRoleId(RoleName role) {
        return roleRepository.findByName(role.name())
                .map(Role::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "roles tablosunda '" + role.name() + "' rolu bulunamadi"));
    }
}
