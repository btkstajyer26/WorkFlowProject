package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowTransitionAudit;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Kayit yasam dongusu olaylari (olusturma/guncelleme/silme); durum gecisi
     * yoktur. {@code record} modulunun {@code createRecord}/{@code updateRecord}/
     * {@code deleteRecord} icinde cagirmasi icin acilmis giris noktasi.
     *
     * <p>{@code previous_status} bos birakilir (kolon nullable); {@code new_status}
     * NOT NULL oldugu icin kaydin o anki durumu yazilir. Sema degismez.
     */
    public void recordLifecycleEvent(UUID recordId,
                                     UUID actorId,
                                     RoleName actorRole,
                                     String action,
                                     RecordStatus currentStatus,
                                     String comment) {

        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(actorRole, "actorRole");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(currentStatus, "currentStatus");

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
     * Admin aktörünün giriş/çıkış ve HTTP istekleri. record_id yoktur;
     * evrak geçmişi sorgusu bu satırları görmez.
     */
    public void recordAccess(RequestAccessEvent event) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(event.action(), "action");

        AuditLog log = AuditLog.builder()
                .recordId(null)
                .userId(event.userId())
                .roleId(event.roleId())
                .action(event.action())
                .previousStatus(null)
                .newStatus(null)
                .comment(event.comment())
                .httpMethod(event.httpMethod())
                .requestPath(event.requestPath())
                .httpStatus(event.httpStatus())
                .errorCode(event.errorCode())
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    public PagedResponse<AuditLogResponse> listAll(Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepository.findAllWithNames(pageable);
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** Bir evragin detay sayfasindaki "Islem Gecmisi" tablosunu doldurmak icin. */
    public List<AuditLogResponse> getGecmis(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId");
        return auditLogRepository.findHistoryByRecordId(recordId);
    }

    /**
     * Ayni gecmisin, evragin su anki sahibine devredildigi ana kadar kirpilmis
     * hali. Devirden sonraki satirlar donmez.
     *
     * <p>Kaydi elinden cikaran ama onu izlemeye devam edebilen kullanici icindir
     * (bkz. {@code RecordAccessPolicy.seesRecordAsOfHandoff}). Kirpma
     * sunucuda yapilir: istemcide filtrelemek satirlarin yine de tel uzerinden
     * gitmesi demek olurdu.
     *
     * <p>Devir ani, kaydi {@code DUZENLEME_BEKLIYOR} durumuna sokan son
     * <em>gecis</em> satiridir. Aksiyon adina degil duruma bakilir; boylece ayni
     * duruma goturen yeni bir aksiyon eklenirse kural kendiliginden gecerli
     * kalir. {@code previousStatus} kontrolu sart: olusturma/guncelleme
     * satirlari da {@code newStatus} olarak kaydin o anki durumunu tasir ama
     * gecis degildir, dolayisiyla devir ani sayilamazlar.
     */
    public List<AuditLogResponse> getGecmisDevreKadar(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId");
        List<AuditLogResponse> history = auditLogRepository.findHistoryByRecordId(recordId);

        LocalDateTime handoff = null;
        for (AuditLogResponse row : history) {
            if (row.previousStatus() != null
                    && RecordStatus.DUZENLEME_BEKLIYOR.name().equals(row.newStatus())) {
                // Sorgu createdAt'e gore artan sirali; dongu sonunda elde kalan
                // en son devirdir.
                handoff = row.createdAt();
            }
        }

        if (handoff == null) {
            // Kayit duzeltme bekliyor gorunuyor ama bunu aciklayan gecis satiri
            // yok: veri tutarsiz. Bu halde tamamini donmek, gizlenmesi gereken
            // satirlari acmak olurdu; bilerek bos donuluyor.
            return List.of();
        }

        LocalDateTime cutoff = handoff;
        return history.stream()
                .filter(row -> !row.createdAt().isAfter(cutoff))
                .toList();
    }

    private Integer resolveRoleId(RoleName role) {
        return roleRepository.findByName(role.name())
                .map(Role::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "roles tablosunda '" + role.name() + "' rolu bulunamadi"));
    }
}
