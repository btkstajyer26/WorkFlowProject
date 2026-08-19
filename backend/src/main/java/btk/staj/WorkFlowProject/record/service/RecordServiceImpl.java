package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.service.PermissionService;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.dto.*;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.mapper.RecordMapper;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final RecordMapper recordMapper;
    private final RecordAccessPolicy recordAccessPolicy;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;
    private final RecordContentView recordContentView;

    public RecordServiceImpl(RecordRepository recordRepository,
                             RecordMapper recordMapper,
                             RecordAccessPolicy recordAccessPolicy,
                             PermissionService permissionService,
                             AuditLogService auditLogService,
                             RecordContentView recordContentView) {
        this.recordRepository = recordRepository;
        this.recordMapper = recordMapper;
        this.recordAccessPolicy = recordAccessPolicy;
        this.permissionService = permissionService;
        this.auditLogService = auditLogService;
        this.recordContentView = recordContentView;
    }

    /**
     * SecurityContextHolder'daki giris yapmis kullaniciyi doner.
     *
     * <p>Bu noktaya ulasilmesi icin istek zaten SecurityConfig'teki
     * anyRequest().authenticated() kuralindan gecmis olmalidir; buradaki
     * kontrol savunma amaclidir.
     */
    private AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ForbiddenException("Kullanıcı oturumu bulunamadı veya yetkisiz erişim!");
        }

        return (AuthenticatedUser) authentication.getPrincipal();
    }

    private UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private RoleName getCurrentUserRole() {
        return RoleName.valueOf(getCurrentUser().getRoleName());
    }

    private Record findRecordOrThrow(UUID id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kayıt bulunamadı! ID: " + id));
    }

    // ---------------------------------------------------------------
    // CRUD İŞLEMLERİ
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public RecordResponse createRecord(RecordCreateRequest request) {
        RoleName role = getCurrentUserRole();

        if (!permissionService.canCreateRecord(role)) {
            throw new ForbiddenException("Kayıt oluşturma yetkiniz yok!");
        }

        Record record = recordMapper.toEntity(request, getCurrentUserId());
        record.setCreatedAt(LocalDateTime.now());

        Record savedRecord = recordRepository.save(record);

        auditLogService.recordLifecycleEvent(
                savedRecord.getId(),
                getCurrentUserId(),
                role,
                "RECORD_CREATED",
                savedRecord.getStatus(),
                "Kayıt oluşturuldu.");

        return recordMapper.toResponse(savedRecord);
    }

    @Override
    public RecordResponse getRecordById(UUID id) {
        Record record = findRecordOrThrow(id);
        RoleName role = getCurrentUserRole();
        UUID userId = getCurrentUserId();

        recordAccessPolicy.assertCanView(
                role,
                userId,
                record.getCreatedBy(),
                record.getAssignedTo(),
                record.getLastDeputyId(),
                record.getStatus());

        // Kaydi gorebilmek guncel icerigi gormek demek degil: geri gonderen
        // yetkiliye devir anindaki kopya gosterilir.
        return recordMapper.toResponse(record, recordContentView.visibleContent(record, role, userId));
    }

    // Listeleme burada degil: filtreleme ve gorunurluk kapsami RecordSearchService'e
    // tasindi (karar 2.2). Ayni erişim kuralının iki ayrı yerde uygulanmasını
    // onlemek icin tekil kaynak search modulu.

    @Override
    @Transactional
    public RecordResponse updateRecord(UUID id, RecordUpdateRequest request) {
        Record record = findRecordOrThrow(id);
        RoleName role = getCurrentUserRole();

        if (!permissionService.canEditOrDeleteDraft(role, record.getStatus())) {
            throw new BusinessRuleException("Bu kayıt şu anki durumunda düzenlenemez!");
        }

        if (!record.getCreatedBy().equals(getCurrentUserId())) {
            throw new ForbiddenException("Bu kaydı düzenleme yetkiniz yok!");
        }

        record.setTitle(request.getTitle());
        record.setDescription(request.getDescription());
        record.setCategoryId(request.getCategoryId());
        record.setUpdatedAt(LocalDateTime.now());

        Record updatedRecord = recordRepository.saveAndFlush(record);

        auditLogService.recordLifecycleEvent(
                updatedRecord.getId(),
                getCurrentUserId(),
                role,
                "RECORD_UPDATED",
                updatedRecord.getStatus(),
                "Başlık ve kategori güncellendi.");

        return recordMapper.toResponse(updatedRecord);
    }

    @Override
    @Transactional
    public void deleteRecord(UUID id) {
        Record record = findRecordOrThrow(id);
        RoleName role = getCurrentUserRole();

        // Silme, duzenlemeden daha dar: PermissionService.canEditOrDeleteDraft
        // hem TASLAK hem DUZENLEME_BEKLIYOR'da true donebilir, ama silme
        // yalnizca TASLAK durumunda gecerlidir (PermissionService javadoc'u).
        if (!permissionService.canEditOrDeleteDraft(role, record.getStatus())
                || record.getStatus() != RecordStatus.TASLAK) {
            throw new BusinessRuleException("Sadece taslak durumundaki kayıtlar silinebilir!");
        }

        if (!record.getCreatedBy().equals(getCurrentUserId())) {
            throw new ForbiddenException("Sadece kendi oluşturduğunuz taslakları silebilirsiniz!");
        }

        record.setDeletedAt(LocalDateTime.now());
        recordRepository.save(record);

        auditLogService.recordLifecycleEvent(
                record.getId(),
                getCurrentUserId(),
                role,
                "RECORD_DELETED",
                record.getStatus(),
                "Kayıt soft delete işlemiyle silindi.");
    }

    // Durum gecisleri bu sinifta degil: onay akisi WorkflowActionService
    // uzerinden durum makinesini calistirir. Buradaki setStatus cagrilari gecis
    // kurallarini, zorunlu aciklamayi ve denetim izini atliyordu.
}
