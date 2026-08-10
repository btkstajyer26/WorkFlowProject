package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordLockValidator {

    private final RecordRepository recordRepository;

    public void assertUploadAllowed(UUID recordId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı: " + recordId));

        if (record.getDeletedAt() != null) {
            throw new IllegalArgumentException("Silinmiş bir kayda dosya eklenemez");
        }

        // Terminal durum listesi burada tekrar tanimlanmaz; kural durum makinesinde
        // tek noktada tutulur (bkz. RecordStatus#isTerminal).
        RecordStatus status = record.getStatus();
        if (status != null && status.isTerminal()) {
            throw new IllegalArgumentException(
                    "Bu kayıt '" + status.name() + "' durumunda, dosya eklenemez");
        }
    }
}
