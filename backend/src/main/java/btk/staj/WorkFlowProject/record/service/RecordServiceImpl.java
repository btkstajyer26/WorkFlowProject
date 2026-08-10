package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;

    public RecordServiceImpl(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Override
    public Record createRecord(Record record, UUID userId) {
        // Oluşturula
        record.setStatus(RecordStatus.TASLAK);
        record.setCreatedBy(userId);
        record.setCreatedAt(LocalDateTime.now());
        
        return recordRepository.save(record);
    }

    @Override
    public Record updateRecord(UUID recordId, Record updatedRecord, UUID userId) {
        Record existingRecord = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Kayıt bulunamadı!"));

        // Eğer durum TASLAK veya DUZENLEME_BEKLIYOR değilse, bu metot false dönecek ve hata fırlatacak.
        if (!existingRecord.getStatus().isEditableByCreator()) {
            throw new RuntimeException("Bu kayıt şu anki durumunda düzenlenemez!");
        }

        if (!existingRecord.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Bu kaydı düzenleme yetkiniz yok!");
        }

        existingRecord.setTitle(updatedRecord.getTitle());
        existingRecord.setDescription(updatedRecord.getDescription());
        existingRecord.setCategoryId(updatedRecord.getCategoryId());
        existingRecord.setUpdatedAt(LocalDateTime.now());

        return recordRepository.save(existingRecord);
    }

    @Override
    public void softDeleteDraft(UUID recordId, UUID userId) {
        Record existingRecord = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Kayıt bulunamadı!"));

        // Sadece sıfırdan oluşturulmuş ve henüz onaya gitmemiş taslaklar silinebilir
        if (existingRecord.getStatus() != RecordStatus.TASLAK) {
            throw new RuntimeException("Sadece taslak durumundaki kayıtlar silinebilir!");
        }

        if (!existingRecord.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Sadece kendi oluşturduğunuz taslakları silebilirsiniz!");
        }

        existingRecord.setDeletedAt(LocalDateTime.now());
        recordRepository.save(existingRecord);
    }

    @Override
    public Page<Record> getMyRecords(UUID userId, Pageable pageable) {
        return recordRepository.findByCreatedByAndDeletedAtIsNull(userId, pageable);
    }
}