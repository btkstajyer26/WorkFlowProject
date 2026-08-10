package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.entity.RecordStatus;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordLockValidator {

    private final RecordRepository recordRepository;

    // Terminal durumlar: bu durumdaki kayda yeni dosya eklenemez
    private static final Set<RecordStatus> TERMINAL_STATUSES = Set.of(
            RecordStatus.ONAYLANDI,
            RecordStatus.REDDEDILDI
    );

    public void assertUploadAllowed(UUID recordId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı: " + recordId));

        if (record.getDeletedAt() != null) {
            throw new IllegalArgumentException("Silinmiş bir kayda dosya eklenemez");
        }

        RecordStatus status = RecordStatus.valueOf(record.getStatus());
        if (TERMINAL_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Bu kayıt '" + status.getDisplayName() + "' durumunda, dosya eklenemez");
        }
    }
}