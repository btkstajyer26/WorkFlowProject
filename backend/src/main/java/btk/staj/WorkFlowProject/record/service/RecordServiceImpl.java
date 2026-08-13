package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.service.PermissionService;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.dto.*;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.mapper.RecordMapper;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final RecordMapper recordMapper;
    private final RecordAccessPolicy recordAccessPolicy;
    private final PermissionService permissionService;

    public RecordServiceImpl(RecordRepository recordRepository,
                             RecordMapper recordMapper,
                             RecordAccessPolicy recordAccessPolicy,
                             PermissionService permissionService) {
        this.recordRepository = recordRepository;
        this.recordMapper = recordMapper;
        this.recordAccessPolicy = recordAccessPolicy;
        this.permissionService = permissionService;
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
    public RecordResponse createRecord(RecordCreateRequest request) {
        RoleName role = getCurrentUserRole();

        if (!permissionService.canCreateRecord(role)) {
            throw new ForbiddenException("Kayıt oluşturma yetkiniz yok!");
        }

        Record record = recordMapper.toEntity(request, getCurrentUserId());
        record.setCreatedAt(LocalDateTime.now());

        Record savedRecord = recordRepository.save(record);
        return recordMapper.toResponse(savedRecord);
    }

    @Override
    public RecordResponse getRecordById(UUID id) {
        Record record = findRecordOrThrow(id);

        recordAccessPolicy.assertCanView(
                getCurrentUserRole(),
                getCurrentUserId(),
                record.getCreatedBy(),
                record.getAssignedTo(),
                record.getStatus());

        return recordMapper.toResponse(record);
    }

    @Override
    public Page<RecordResponse> getFilteredRecords(RecordStatus status, Integer categoryId, String keyword, Pageable pageable) {
        RoleName role = getCurrentUserRole();
        UUID currentUserId = getCurrentUserId();

        Specification<Record> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Soft delete kuralı: Silinmiş olanları getirme
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Gorunurluk kapsami: RecordAccessPolicy.canView ile ayni kural,
            // burada listeleme sorgusuna predicate olarak uygulanir.
            switch (role) {
                case CALISAN -> predicates.add(cb.equal(root.get("createdBy"), currentUserId));
                case BASKAN_YARDIMCISI -> predicates.add(cb.equal(root.get("assignedTo"), currentUserId));
                case BASKAN -> predicates.add(cb.or(
                        cb.equal(root.get("status"), RecordStatus.BASKAN_INCELEMESINDE),
                        cb.equal(root.get("assignedTo"), currentUserId)));
                // ADMIN yalnizca kullanici/rol yonetiminden sorumludur; evrak goremez.
                case ADMIN -> predicates.add(cb.disjunction());
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Record> recordPage = recordRepository.findAll(spec, pageable);
        return recordPage.map(recordMapper::toResponse);
    }

    @Override
    public RecordResponse updateRecord(UUID id, RecordUpdateRequest request) {
        Record record = findRecordOrThrow(id);
        RoleName role = getCurrentUserRole();

        if (!permissionService.canEditOrDeleteDraft(role, record.getStatus())) {
            throw new BusinessRuleException("Bu kayıt şu anki durumunda düzenlenemez!");
        }

        if (!record.getCreatedBy().equals(getCurrentUserId())) {
            throw new ForbiddenException("Bu kaydı düzenleme yetkiniz yok!");
        }

        // Standart Lombok getter kullanımları
        record.setTitle(request.getTitle());
        record.setDescription(request.getDescription());
        record.setCategoryId(request.getCategoryId());
        record.setUpdatedAt(LocalDateTime.now());

        Record updatedRecord = recordRepository.save(record);
        return recordMapper.toResponse(updatedRecord);
    }

    @Override
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
    }

    // Durum gecisleri bu sinifta degil: onay akisi WorkflowActionService
    // uzerinden durum makinesini calistirir. Buradaki setStatus cagrilari gecis
    // kurallarini, zorunlu aciklamayi ve denetim izini atliyordu.
}