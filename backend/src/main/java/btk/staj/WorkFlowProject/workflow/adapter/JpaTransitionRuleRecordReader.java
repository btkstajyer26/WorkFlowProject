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
                row.toStatus(),
                row.targetStrategy(),
                expectedTargetKeyOf(row, rowNumber));
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

    /**
     * Beklenen hedef rolun degismez anahtarini dondurur; hedef yoksa {@code null}.
     *
     * <p>Bos {@code system_key}'in iki farkli sebebi olabilir ve ikisi ayirt edilmelidir:
     *
     * <ul>
     *   <li>FK hic dolu degil &rarr; gecis hedef gerektirmiyor, {@code null} dogru cevap;</li>
     *   <li>FK dolu ama {@code system_key} bos &rarr; hedef <strong>dinamik</strong> bir rol.
     *       Sessizce {@code null} donmek olurdu; o zaman validator hedefin rolunu
     *       {@code null} ile karsilastirir ve gecis her zaman reddedilirdi. Bu yuzden
     *       acikca hata verilir.</li>
     * </ul>
     */
    private static String expectedTargetKeyOf(TransitionRuleRow row, int rowNumber) {
        if (row.expectedTargetRoleId() == null) {
            return null;
        }

        String systemKey = row.expectedTargetRoleSystemKey();
        if (systemKey == null || systemKey.isBlank()) {
            throw new TransitionRuleConfigurationException(
                    "Transition configuration row " + rowNumber
                            + " expects a target role without system_key"
                            + " (role id: " + row.expectedTargetRoleId() + ")."
                            + " Dynamic roles cannot be a transition target until"
                            + " workflow roles move to RoleId (WF-2D2)");
        }
        return systemKey;
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
