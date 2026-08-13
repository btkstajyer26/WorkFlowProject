package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.dto.*;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.mapper.RecordMapper;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.stereotype.Service;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final RecordMapper recordMapper;

    public RecordServiceImpl(RecordRepository recordRepository, RecordMapper recordMapper) {
        this.recordRepository = recordRepository;
        this.recordMapper = recordMapper;
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Güvenlik Kontrolü: Oturum yoksa veya anonim bir kullanıcıysa hata fırlat
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Kullanıcı oturumu bulunamadı veya yetkisiz erişim!");
        }

        // Spring Security'nin tuttuğu oturum bilgisini takım arkadaşının yazdığı formata çeviriyoruz
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

        // Sınıfın içindeki hazır getId() metodunu kullanarak UUID'yi dönüyoruz
        return authenticatedUser.getId();
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

    // Listeleme burada degil: filtreleme ve gorunurluk kapsami RecordSearchService'e
    // tasindi (karar 2.2). Ayni erişim kuralının iki ayrı yerde uygulanmasını
    // onlemek icin tekil kaynak search modulu.

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