package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecordService {
    
    // 1. Yeni Kayıt Oluşturma
    Record createRecord(Record record, UUID userId);

    // 2. Mevcut Kaydı Düzenleme
    Record updateRecord(UUID recordId, Record updatedRecord, UUID userId);

    // 3. Taslak Silme (Soft Delete)
    void softDeleteDraft(UUID recordId, UUID userId);

    // 4. Kayıtlarım Listesi
    Page<Record> getMyRecords(UUID userId, Pageable pageable);
}