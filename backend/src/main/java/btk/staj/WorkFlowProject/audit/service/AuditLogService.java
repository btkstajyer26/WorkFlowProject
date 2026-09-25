package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.common.dto.AssignmentView;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.view.AssignmentViewResolver;
import btk.staj.WorkFlowProject.workflow.model.WorkflowTransitionAudit;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Onay akisinin {@link AuditService} portunun denetim izi tarafindaki
 * karsiligi. Onay akisi kendi modelini ({@link WorkflowTransitionAudit})
 * gonderir; bu sinif onu {@code audit_logs} satirina cevirir.
 *
 * <p>Workflow supplies the relational actor role ID directly; no role-name lookup is needed.
 */
@Service
public class AuditLogService implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AssignmentViewResolver assignmentViewResolver;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           AssignmentViewResolver assignmentViewResolver) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.assignmentViewResolver =
                Objects.requireNonNull(assignmentViewResolver, "assignmentViewResolver");
    }


    @Override
    public void record(WorkflowTransitionAudit audit) {
        Objects.requireNonNull(audit, "audit");

        AuditLog log = AuditLog.builder()
                .recordId(audit.recordId())
                .userId(audit.actorId())
                .roleId(audit.actorRoleId().value())
                .action(audit.action().name())
                .previousStatus(audit.previousStatus().name())
                .newStatus(audit.newStatus().name())
                .comment(audit.comment())
                .previousAssignedTo(audit.previousAssignedTo())
                .previousAssignedDepartmentId(audit.previousAssignedDepartmentId())
                .newAssignedTo(audit.newAssignedTo())
                .newAssignedDepartmentId(audit.newAssignedDepartmentId())
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
                                     Integer actorRoleId,
                                     String action,
                                     RecordStatus currentStatus,
                                     String comment) {

        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(actorRoleId, "actorRoleId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(currentStatus, "currentStatus");

        AuditLog log = AuditLog.builder()
                .recordId(recordId)
                .userId(actorId)
                .roleId(actorRoleId)
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
                withAssignmentNames(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** Bir evragin detay sayfasindaki "Islem Gecmisi" tablosunu doldurmak icin. */
    public List<AuditLogResponse> getGecmis(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId");
        return withAssignmentNames(auditLogRepository.findHistoryByRecordId(recordId));
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
        // Zenginlestirme KIRPMADAN SONRA yapilir: gizlenen satirlardaki kisi ve
        // departman adlari yanita hic girmez, bosuna da sorgulanmaz.
        return withAssignmentNames(history.stream()
                .filter(row -> !row.createdAt().isAfter(cutoff))
                .toList());
    }

    /**
     * Ayni gecmisin, evragin Baskana ilk iletildigi andan itibaren baslayan
     * hali. Oncesindeki satirlar donmez.
     *
     * <p>{@link #getGecmisDevreKadar} ile ayni fikrin ters yonu: orada gecmis
     * devirde <em>kesilir</em>, burada devirde <em>baslar</em>. Karar yine
     * {@code RecordAccessPolicy.seesHistoryFromPresidentHandover}'in, kirpma
     * burasinin isi; gizlenen satirlar cevaba hic konmaz.
     *
     * <p>Baslangic ani, kaydi {@code BASKAN_INCELEMESINDE} durumuna sokan
     * <em>ilk</em> gecis satiridir. Sonuncusu degil: Baskan evraki yardimciya
     * geri gonderip tekrar aldiginda son iletime gore kirpmak, kendi yazdigi
     * geri gonderme gerekcesini de gizlerdi. {@code previousStatus} kontrolu
     * sart; olusturma/guncelleme satirlari da {@code newStatus} olarak kaydin o
     * anki durumunu tasir ama gecis degildir.
     */
    public List<AuditLogResponse> getGecmisIletimdenItibaren(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId");
        List<AuditLogResponse> history = auditLogRepository.findHistoryByRecordId(recordId);

        LocalDateTime handover = null;
        for (AuditLogResponse row : history) {
            if (row.previousStatus() != null
                    && RecordStatus.BASKAN_INCELEMESINDE.name().equals(row.newStatus())) {
                // Sorgu createdAt'e gore artan sirali; ilk eslesme ilk iletimdir.
                handover = row.createdAt();
                break;
            }
        }

        if (handover == null) {
            // Kayit Baskanin kapsaminda gorunuyor ama bunu aciklayan iletim
            // satiri yok: veri tutarsiz. Tamamini donmek, gizlenmesi gereken
            // satirlari acmak olurdu; bilerek bos donuluyor.
            return List.of();
        }

        LocalDateTime cutoff = handover;
        // Kirpmadan sonra; gerekcesi getGecmisDevreKadar ile ayni.
        return withAssignmentNames(history.stream()
                .filter(row -> !row.createdAt().isBefore(cutoff))
                .toList());
    }

    /**
     * Atama gosterim adlarini TOPLU cozer (B12 / ADR-0009 K4).
     *
     * <p>Satir basina {@code resolve(...)} cagirmak gecmis uzunlugu kadar sorgu
     * acardi (N+1); {@code resolveAll} tam bunun icin vardir ve butun listeyi
     * en fazla iki sorguda karsilar. Atamasiz gecmislerde hic sorgu acilmaz:
     * {@code resolveAll} bos kumede erken doner.
     */
    private List<AuditLogResponse> withAssignmentNames(List<AuditLogResponse> rows) {
        if (rows.isEmpty()) return rows;

        List<UUID> userIds = new ArrayList<>();
        List<Integer> departmentIds = new ArrayList<>();
        for (AuditLogResponse row : rows) {
            userIds.add(row.previousAssignment().userId());
            userIds.add(row.newAssignment().userId());
            departmentIds.add(row.previousAssignment().departmentId());
            departmentIds.add(row.newAssignment().departmentId());
        }

        AssignmentViewResolver.Names names = assignmentViewResolver.resolveAll(userIds, departmentIds);

        return rows.stream()
                .map(row -> row.withAssignments(
                        assignmentWithName(names, row.previousAssignment()),
                        assignmentWithName(names, row.newAssignment())))
                .toList();
    }

    private static AssignmentView assignmentWithName(AssignmentViewResolver.Names names,
                                                     AssignmentView assignment) {
        return names.assignmentFor(assignment.userId(), assignment.departmentId());
    }

}
