package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.dto.*;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.mapper.RecordMapper;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public RecordServiceImpl(RecordRepository recordRepository, RecordMapper recordMapper) {
        this.recordRepository = recordRepository;
        this.recordMapper = recordMapper;
    }

    /**
     * DİKKAT: Auth (Güvenlik) paketi tamamlanana kadar sistemi test edebilmen için
     * geçici bir kullanıcı ID'si üreten yardımcı metot.
     * İleride burası SecurityContextHolder üzerinden giriş yapmış kullanıcıyı alacak.
     */
    private UUID getCurrentUserId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111"); 
    }

    private Record findRecordOrThrow(UUID id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kayıt bulunamadı! ID: " + id));
    }

    // ---------------------------------------------------------------
    // CRUD İŞLEMLERİ
    // ---------------------------------------------------------------

    @Override
    public RecordResponse createRecord(RecordCreateRequest request) {
        Record record = recordMapper.toEntity(request, getCurrentUserId());
        record.setCreatedAt(LocalDateTime.now());
        
        Record savedRecord = recordRepository.save(record);
        return recordMapper.toResponse(savedRecord);
    }

    @Override
    public RecordResponse getRecordById(UUID id) {
        Record record = findRecordOrThrow(id);
        return recordMapper.toResponse(record);
    }

    @Override
    public Page<RecordResponse> getFilteredRecords(RecordStatus status, Integer categoryId, String keyword, Pageable pageable) {
        Specification<Record> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Soft delete kuralı: Silinmiş olanları getirme
            predicates.add(cb.isNull(root.get("deletedAt")));

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

        if (!record.getStatus().isEditableByCreator()) {
            throw new RuntimeException("Bu kayıt şu anki durumunda düzenlenemez!");
        }

        if (!record.getCreatedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Bu kaydı düzenleme yetkiniz yok!");
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

        if (record.getStatus() != RecordStatus.TASLAK) {
            throw new RuntimeException("Sadece taslak durumundaki kayıtlar silinebilir!");
        }

        if (!record.getCreatedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Sadece kendi oluşturduğunuz taslakları silebilirsiniz!");
        }

        record.setDeletedAt(LocalDateTime.now());
        recordRepository.save(record);
    }

    // Durum gecisleri bu sinifta degil: onay akisi WorkflowActionService
    // uzerinden durum makinesini calistirir. Buradaki setStatus cagrilari gecis
    // kurallarini, zorunlu aciklamayi ve denetim izini atliyordu.
}