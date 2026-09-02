package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.repository.TransitionRuleRow;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link TransitionRuleRecordReader} portunun JPA implementasyonu.
 *
 * <p>Sorumlulugu tektir: {@code workflow_transitions} satirlarini teknik
 * anahtarlara cevirmek. Enum cozumu, satir numarali hata mesajlari ve
 * tekillik kontrolu {@code DbTransitionRuleSource} tarafinda yapilir; burada
 * tekrar edilmez.
 *
 * <h2>Aktor rolu neden {@code system_key}?</h2>
 * {@code DbTransitionRuleSource} aldigi metni {@code RoleName.valueOf(...)}
 * ile cozer ve bunu <em>constructor</em> icinde, yani uygulama acilirken
 * yapar. {@code V12} ile {@code roles.name} yonetim panelinden
 * degistirilebilir hale geldi; kural kimligi oradan okunsaydi, bir rol
 * yeniden adlandirildigi anda uygulama acilista hata verirdi.
 * {@code roles.system_key} degismezdir (DB-1 SS4) ve {@code V12} backfill'i
 * onu {@code RoleName} sabitleriyle ayni degerlerle doldurmustur.
 *
 * <h2>Bu adapter'in bilincli siniri</h2>
 * {@code RoleName} yalnizca dort yerlesik rolu temsil edebilir. Yonetim
 * panelinden olusturulan dinamik bir rol ({@code system_key IS NULL}) bir
 * gecise aktor olarak atanirsa, o satir bu compatibility seam ile temsil
 * edilemez ve burada acik bir hatayla reddedilir. Sinir, workflow aktor
 * rolunun {@code RoleId}'ye tasindigi WF-2D ile kalkar.
 */
@Component
public final class JpaTransitionRuleRecordReader implements TransitionRuleRecordReader {

    private final WorkflowTransitionRepository transitionRepository;

    public JpaTransitionRuleRecordReader(WorkflowTransitionRepository transitionRepository) {
        this.transitionRepository = Objects.requireNonNull(
                transitionRepository, "transitionRepository");
    }

    @Override
    public List<TransitionRuleRecord> findAllActive() {
        List<TransitionRuleRow> rows = transitionRepository.findActiveRuleRows();
        if (rows == null) {
            throw new IllegalStateException(
                    "WorkflowTransitionRepository.findActiveRuleRows() returned null");
        }

        List<TransitionRuleRecord> records = new ArrayList<>(rows.size());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            records.add(toRecord(rows.get(rowIndex), rowIndex + 1));
        }
        return List.copyOf(records);
    }

    private static TransitionRuleRecord toRecord(TransitionRuleRow row, int rowNumber) {
        if (row == null) {
            throw new IllegalStateException(
                    "WorkflowTransitionRepository returned a null row at row " + rowNumber);
        }

        return new TransitionRuleRecord(
                row.fromStatus(),
                row.action(),
                actorKeyOf(row, rowNumber),
                requirementOf(row, rowNumber),
                row.toStatus());
    }

    /**
     * Aktor rolunun degismez anahtarini dondurur.
     *
     * <p>Bos gelmesinin tek gercekci sebebi, gecise dinamik bir rolun aktor
     * yapilmasidir; mesaj bu yuzden rolun gosterilen adini da tasir.
     */
    private static String actorKeyOf(TransitionRuleRow row, int rowNumber) {
        String actorSystemKey = row.actorSystemKey();
        if (actorSystemKey == null || actorSystemKey.isBlank()) {
            throw new TransitionRuleConfigurationException(
                    "Transition configuration row " + rowNumber
                            + " has an actor role without system_key"
                            + " (role name: " + row.actorRoleName() + ")."
                            + " Dynamic roles cannot act in a transition until"
                            + " workflow actor roles move to RoleId (WF-2D)");
        }
        return actorSystemKey;
    }

    /** {@code actor_requirement} DB'de {@code NOT NULL}; kontrol savunma amaclidir. */
    private static String requirementOf(TransitionRuleRow row, int rowNumber) {
        if (row.actorRequirement() == null) {
            throw new TransitionRuleConfigurationException(
                    "Transition configuration row " + rowNumber
                            + " has a null actor_requirement");
        }
        return row.actorRequirement().name();
    }
}
