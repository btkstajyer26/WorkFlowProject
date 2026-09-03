package btk.staj.WorkFlowProject.support;

import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import java.util.List;

/**
 * Domain kurallarini {@link TransitionRuleRecord} projeksiyonuna ceviren test yardimcisi.
 *
 * <p>Bu donusum daha once {@code StaticTransitionRuleReaderConfiguration} icinde
 * gomuluydu; {@code DbTransitionRuleSource}'u sahte bir reader ile kuran testler de
 * ayni seye ihtiyac duydugu icin buraya cikarildi.
 *
 * <p>Permission kodu kuralin kendi {@code requiredPermissionCode} degerinden alinir,
 * aksiyondan turetilmez: turetilmis bicim kayiplidir ve aksiyonun varsayilanindan sapan
 * bir permission kodunu sessizce yeniden yazardi.
 */
public final class TransitionRuleFixtures {

    private TransitionRuleFixtures() { }

    /** Verilen kurallari donduren, altyapisiz bir {@link TransitionRuleRecordReader}. */
    public static TransitionRuleRecordReader reader(List<TransitionRule> rules) {
        List<TransitionRuleRecord> records = rules.stream()
                .map(TransitionRuleFixtures::toRecord)
                .toList();
        return () -> records;
    }

    /** Ham satirlari dogrudan donduren reader; invariant ihlali uretmek icin kullanilir. */
    public static TransitionRuleRecordReader readerOfRecords(List<TransitionRuleRecord> records) {
        return () -> records;
    }

    public static TransitionRuleRecord toRecord(TransitionRule rule) {
        return new TransitionRuleRecord(
                rule.from().name(),
                rule.action().name(),
                rule.actorRoleId().value(),
                rule.actorRequirement().name(),
                rule.to().name(),
                rule.targetStrategy().name(),
                rule.expectedTargetRoleId() == null ? null : rule.expectedTargetRoleId().value(),
                rule.requiredPermissionCode());
    }
}
