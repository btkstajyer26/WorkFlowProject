package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.support.TransitionRuleFixtures;
import btk.staj.WorkFlowProject.support.TransitionRuleInvariants;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantlarin bos bir garanti olmadiginin kaniti.
 *
 * <p>{@code AbstractTransitionRuleInvariants} bugun her kaynakta yesil doner. Bu testler
 * kasten ihlal eden kural satirlari uretip ayni kontrollerin <strong>kirmiziya dondugunu</strong>
 * dogrular; aksi halde yesil sonuclar bir sey kanitlamazdi. Repodaki
 * {@code deactivatedTransitionBreaksParity} ile ayni desen.
 *
 * <p>Kaynak gercek {@link DbTransitionRuleSource}'tur, yalnizca reader sahtedir: boylece
 * ihlaller kaynagin kendi kurucusundan gecerek olusur ve Spring/PostgreSQL gerekmez.
 *
 * <p>Buradaki iki ihlal bilerek secildi: kural tekilligi ve strateji/hedef tutarliligi
 * {@code DbTransitionRuleSource} ile {@code TransitionRule} kurucularinda zaten
 * engellendigi icin ihlal edilmis bir kume hic olusturulamaz. Yanlislanabilir olan
 * invariantlar bunlardir.
 */
@DisplayName("Kural invariantlari ihlali yakalar")
class TransitionRuleInvariantMutationTest {

    @Test
    @DisplayName("kurallara uyan kume butun invariantlari gecer")
    void validRuleSetPassesEveryInvariant() {
        TransitionRuleSource source = sourceOf(seedRecords());

        TransitionRuleInvariants.assertNoDuplicateRuleKeys(source);
        TransitionRuleInvariants.assertNoTransitionLeavesTerminalStatus(source);
        TransitionRuleInvariants.assertNoRuleAssignedTo(source, WorkflowRoleFixtures.id(RoleName.ADMIN));
        TransitionRuleInvariants.assertTargetStrategyAndExpectedRoleAgree(source);

        assertThat(source.all()).hasSize(10);
    }

    @Test
    @DisplayName("terminal durumdan cikan kural eklenirse invariant duser")
    void transitionLeavingTerminalStatusIsCaught() {
        List<TransitionRuleRecord> records = seedRecords();
        records.add(new TransitionRuleRecord(
                "ONAYLANDI",
                "GONDER",
                WorkflowRoleFixtures.value(RoleName.CALISAN),
                "CREATOR",
                "TASLAK",
                "NONE",
                null,
                "RECORD_FORWARD"));

        TransitionRuleSource source = sourceOf(records);

        assertThatThrownBy(() -> TransitionRuleInvariants.assertNoTransitionLeavesTerminalStatus(source))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("terminal durumdan cikan gecis olmamali");
    }

    @Test
    @DisplayName("aktor olmayan role kural baglanirsa invariant duser")
    void ruleAssignedToNonActorRoleIsCaught() {
        List<TransitionRuleRecord> records = seedRecords();
        records.add(new TransitionRuleRecord(
                "TASLAK",
                "ONAYLA",
                WorkflowRoleFixtures.value(RoleName.ADMIN),
                "CREATOR",
                "ONAYLANDI",
                "NONE",
                null,
                "RECORD_APPROVE"));

        TransitionRuleSource source = sourceOf(records);

        assertThatThrownBy(() -> TransitionRuleInvariants.assertNoRuleAssignedTo(
                source, WorkflowRoleFixtures.id(RoleName.ADMIN)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("kural tanimlanmamali");
    }

    private static TransitionRuleSource sourceOf(List<TransitionRuleRecord> records) {
        return new DbTransitionRuleSource(TransitionRuleFixtures.readerOfRecords(records));
    }

    /** Baslangic seed'inin satir karsiligi; mutasyon bunun uzerine eklenir. */
    private static List<TransitionRuleRecord> seedRecords() {
        return new ArrayList<>(WorkflowRoleFixtures.rules().all().stream()
                .map(TransitionRuleFixtures::toRecord)
                .toList());
    }
}
