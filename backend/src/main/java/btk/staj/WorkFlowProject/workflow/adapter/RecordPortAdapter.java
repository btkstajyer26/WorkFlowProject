package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowRecordNotFoundException;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;
import btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Onay akisinin kayitlari okuyup guncelledigi sinir. Cekirdek JPA'yi bilmez;
 * cevrim burada yapilir.
 */
@Component
public final class RecordPortAdapter implements WorkflowRecordPort {

    private final RecordRepository recordRepository;

    public RecordPortAdapter(RecordRepository recordRepository) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
    }

    @Override
    public Optional<WorkflowRecordSnapshot> findById(UUID recordId) {
        UUID requiredRecordId = Objects.requireNonNull(recordId, "recordId");

        Optional<Record> result = recordRepository.findById(requiredRecordId);
        if (result == null) {
            throw new IllegalStateException("RecordRepository.findById returned null");
        }
        return result.map(RecordPortAdapter::toSnapshot);
    }

    @Override
    public void update(WorkflowRecordUpdate update) {
        WorkflowRecordUpdate requiredUpdate = Objects.requireNonNull(update, "update");

        Record record = recordRepository.findById(requiredUpdate.recordId())
                .orElseThrow(() -> new WorkflowRecordNotFoundException(requiredUpdate.recordId()));

        // Cekirdek, kaydi hangi surumde okuduysa onu bildirir. Arada baskasi
        // guncellediyse gecis sessizce ustune yazmamali.
        //
        // Bu kontrol ikincil bir savunmadir: cagri ile ayni transaction icinde
        // yukaridaki findById persistence context'ten ayni managed entity'yi
        // dondurdugu icin surumler genellikle esit cikar. Asil koruma asagidaki
        // flush aninda Hibernate'in @Version kontroludur.
        int currentVersion = record.getVersion() == null ? 0 : record.getVersion();
        if (currentVersion != requiredUpdate.expectedVersion()) {
            throw new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_VERSION_CONFLICT);
        }

        // Kayit duzeltmeye dusuyorsa icerik bu anda dondurulur: geri gonderen
        // yetkili, Calisan yeniden gonderene kadar kendi biraktigi hali gorur.
        // Aksiyon adina degil hedef duruma bakilir; ayni duruma goturen yeni bir
        // aksiyon eklenirse kural kendiliginden gecerli kalir.
        //
        // Anlik goruntu durum degistirilmeden ONCE alinmali, cunku kopyalanan
        // degerler kaydin gecis oncesi halidir.
        if (requiredUpdate.newStatus() == RecordStatus.DUZENLEME_BEKLIYOR) {
            record.setSnapshotTitle(record.getTitle());
            record.setSnapshotDescription(record.getDescription());
            record.setSnapshotCategoryId(record.getCategoryId());
            record.setSnapshotAt(LocalDateTime.ofInstant(
                    requiredUpdate.updatedAt(), ZoneId.systemDefault()));
        }

        record.setStatus(requiredUpdate.newStatus());
        record.setAssignedTo(requiredUpdate.assignedTo());
        record.setLastDeputyId(requiredUpdate.lastDeputyId());
        // updatedAt'e dokunulmaz: entity'de @UpdateTimestamp var, Hibernate yazar.

        try {
            recordRepository.saveAndFlush(record);
        } catch (OptimisticLockingFailureException ex) {
            // Port sozlesmesi geregi altyapiya ozgu kilitleme istisnasi bu siniri
            // gecmez; cekirdek persistence teknolojisini tanimaz. Ust tip
            // yakalanir ki Hibernate'in urettigi alt tipler de kapsansin.
            throw new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_VERSION_CONFLICT, ex);
        }
    }

    private static WorkflowRecordSnapshot toSnapshot(Record record) {
        if (record == null) {
            throw new IllegalStateException("RecordRepository returned a null Record");
        }

        UUID id = record.getId();
        if (id == null) {
            throw new IllegalStateException("Repository Record has a null id");
        }

        RecordStatus status = record.getStatus();
        if (status == null) {
            throw new IllegalStateException("Repository Record has a null status");
        }

        UUID createdBy = record.getCreatedBy();
        if (createdBy == null) {
            throw new IllegalStateException("Repository Record has a null createdBy");
        }

        return new WorkflowRecordSnapshot(
                id,
                status,
                createdBy,
                record.getAssignedTo(),
                record.getLastDeputyId(),
                toInstant(record.getDeletedAt()),
                record.getVersion() == null ? 0 : record.getVersion());
    }

    private static Instant toInstant(java.time.LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
