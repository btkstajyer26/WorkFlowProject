package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;

import java.util.Optional;
import java.util.UUID;

/** Boundary through which the workflow core reads and updates records. */
public interface WorkflowRecordPort {

    Optional<WorkflowRecordSnapshot> findById(UUID recordId);

    /**
     * Kaydi verilen komuta gore gunceller.
     *
     * <p>Komut, kaydin okundugu andaki surumunu tasir. Kayit o andan beri baska
     * bir islem tarafindan degistirilmisse guncelleme uygulanmamali ve
     * {@link WorkflowApplicationException} ile
     * {@link WorkflowErrorCode#WORKFLOW_VERSION_CONFLICT} firlatilmalidir.
     * Uygulama katmani bu hatayi ozel olarak ele almaz; hata yukari
     * yayilir, boylece denetim izi ve bildirim adimlari hic calismaz.
     *
     * <p>Altyapiya ozgu kilitleme istisnalari (ornegin Spring'in
     * {@code OptimisticLockingFailureException} tipi) bu sinira gecmeden
     * yukaridaki koda cevrilmelidir; workflow cekirdegi persistence
     * teknolojisini tanimaz.
     *
     * @throws WorkflowApplicationException surum catismasi halinde
     *         {@code WORKFLOW_VERSION_CONFLICT} kodu ile
     */
    void update(WorkflowRecordUpdate update);
}
