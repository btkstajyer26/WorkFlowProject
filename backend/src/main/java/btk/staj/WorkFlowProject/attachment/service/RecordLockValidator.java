package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
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

    /**
     * Kaydi satir kilidiyle yukler ve dosya degisikligine izin verilip
     * verilmedigini dogrular (B04).
     *
     * <p>Onceden kilitsiz bir {@code findById} yapiliyor ve yuklenen kayit
     * atiliyordu; kontrol ile dosya satirinin yazilmasi arasinda workflow durumu
     * degisirse islem eski izinle devam ediyordu. Artik kilit cagiranin
     * transaction'i boyunca tutulur ve kilitli kayit doner, boylece cagiran
     * ayni ornek uzerinden calisir.
     *
     * @return degisiklige acik oldugu dogrulanmis, kilitli kayit
     */
    public Record assertModifyAllowed(UUID recordId, UUID currentUserId) {
        Record record = recordRepository.findByIdForUpdate(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Kayıt bulunamadı: " + recordId));

        if (record.getDeletedAt() != null) {
            throw new BusinessRuleException("Silinmiş bir kayda dosya eklenemez veya dosya silinemez.");
        }

        if (!currentUserId.equals(record.getCreatedBy())) {
            throw new ForbiddenException("Bu kaydın dosyalarını yalnızca kaydı oluşturan kullanıcı değiştirebilir.");
        }

        RecordStatus status = record.getStatus();
        if (status != RecordStatus.TASLAK && status != RecordStatus.DUZENLEME_BEKLIYOR) {
            throw new BusinessRuleException("Yalnızca TASLAK veya DUZENLEME_BEKLIYOR durumundaki kayıtlarda dosya değişikliği yapılabilir. Mevcut durum: " + status);
        }

        return record;
    }
}