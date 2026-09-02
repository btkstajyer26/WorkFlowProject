package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement.ASSIGNEE;
import static btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement.CREATOR;
import static btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement.CREATOR_AND_ASSIGNEE;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.BASKAN_INCELEMESINDE;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.BSK_YRD_INCELEMESINDE;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.DUZENLEME_BEKLIYOR;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.ONAYLANDI;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.REDDEDILDI;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.TASLAK;
import static btk.staj.WorkFlowProject.workflow.statemachine.RoleName.BASKAN;
import static btk.staj.WorkFlowProject.workflow.statemachine.RoleName.BASKAN_YARDIMCISI;
import static btk.staj.WorkFlowProject.workflow.statemachine.RoleName.CALISAN;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.BASKANA_ILET;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.CALISANA_GERI_GONDER;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.GONDER;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.ONAYLA;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.REDDET;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.TEKRAR_GONDER;

/**
 * Gecis tablosunun statik kopyasi.
 *
 * <p><strong>Bu tablo artik production kaynagi degildir.</strong> Kurallar
 * {@code workflow_transitions} tablosundan okunur ({@code DbTransitionRuleSource}); burasi
 * yalnizca parity testinin karsilastirdigi referans ve gerekirse geri donulecek yoldur.
 *
 * <p>Dolayisiyla satirlar {@code V15} seed'iyle <strong>birebir ayni</strong> olmak
 * zorundadir; ayrisirlarsa {@code TransitionRuleSourceParityTest} kirmizi yanar ve bu
 * dogru davranistir. Yeni bir gecis eklerken once migration, sonra bu tablo guncellenir.
 *
 * <p>Tabloda bulunmayan her durum&ndash;aksiyon&ndash;rol birlesimi gecersizdir.
 */
public final class TransitionRules {

    private static final List<TransitionRule> RULES = List.of(
            //                 mevcut durum           aksiyon                          yetkili rol         kayit iliskisi         hedef durum            hedef stratejisi              beklenen hedef rol
            new TransitionRule(TASLAK,                GONDER,                          CALISAN,            CREATOR,               BSK_YRD_INCELEMESINDE, TargetStrategy.ROLE,           BASKAN_YARDIMCISI, "RECORD_FORWARD"),
            new TransitionRule(DUZENLEME_BEKLIYOR,    TEKRAR_GONDER,                   CALISAN,            CREATOR_AND_ASSIGNEE,  BSK_YRD_INCELEMESINDE, TargetStrategy.ROLE,           BASKAN_YARDIMCISI, "RECORD_FORWARD"),
            new TransitionRule(BSK_YRD_INCELEMESINDE, BASKANA_ILET,                    BASKAN_YARDIMCISI,  ASSIGNEE,              BASKAN_INCELEMESINDE,  TargetStrategy.ROLE,           BASKAN, "RECORD_FORWARD"),
            new TransitionRule(BSK_YRD_INCELEMESINDE, CALISANA_GERI_GONDER,            BASKAN_YARDIMCISI,  ASSIGNEE,              DUZENLEME_BEKLIYOR,    TargetStrategy.CREATOR,        CALISAN, "RECORD_RETURN"),
            new TransitionRule(BASKAN_INCELEMESINDE,  ONAYLA,                          BASKAN,             ASSIGNEE,              ONAYLANDI,             TargetStrategy.NONE,           null, "RECORD_APPROVE"),
            new TransitionRule(BASKAN_INCELEMESINDE,  REDDET,                          BASKAN,             ASSIGNEE,              REDDEDILDI,            TargetStrategy.NONE,           null, "RECORD_REJECT"),
            new TransitionRule(BASKAN_INCELEMESINDE,  CALISANA_GERI_GONDER,            BASKAN,             ASSIGNEE,              DUZENLEME_BEKLIYOR,    TargetStrategy.CREATOR,        CALISAN, "RECORD_RETURN"),
            new TransitionRule(BASKAN_INCELEMESINDE,  BASKAN_YARDIMCISINA_GERI_GONDER, BASKAN,             ASSIGNEE,              BSK_YRD_INCELEMESINDE, TargetStrategy.PREVIOUS_ACTOR, BASKAN_YARDIMCISI, "RECORD_RETURN")
    );

    private static final Map<Key, TransitionRule> INDEX = buildIndex();

    private TransitionRules() {
    }

    /**
     * Verilen birlesime karsilik gelen kurali arar.
     *
     * @return kural varsa dolu, tabloda tanimli degilse bos {@code Optional}
     */
    public static Optional<TransitionRule> find(RecordStatus from, WorkflowAction action, RoleName actorRole) {
        return Optional.ofNullable(INDEX.get(new Key(from, action, actorRole)));
    }

    /** Tanimli butun gecis kurallari. Test ve dokumantasyon amaclidir. */
    public static List<TransitionRule> all() {
        return RULES;
    }

    private static Map<Key, TransitionRule> buildIndex() {
        Map<Key, TransitionRule> index = new HashMap<>();
        for (TransitionRule rule : RULES) {
            Key key = new Key(rule.from(), rule.action(), rule.actorRole());
            TransitionRule previous = index.put(key, rule);
            if (previous != null) {
                throw new IllegalStateException("Ayni durum-aksiyon-rol birlesimi icin birden fazla kural tanimli: " + key);
            }
        }
        return Map.copyOf(index);
    }

    private record Key(RecordStatus from, WorkflowAction action, RoleName actorRole) {
    }
}
