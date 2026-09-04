package btk.staj.WorkFlowProject.support;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kural kumesinin kaynaktan bagimsiz tutarlilik kontrolleri.
 *
 * <p>TZ-1 oncesinde bu kontroller yalnizca statik tabloya bakiyordu. Statik tablo test
 * agacina tasindiginda ayni sorularin veritabani kaynagina da sorulabilmesi icin
 * kaynak-agnostik hale getirildiler; {@link AbstractTransitionRuleInvariants} bunlari
 * ayri {@code @Test}'ler olarak kosturur.
 *
 * <p><strong>Kapsam durustlugu:</strong> dordunun ikisi veritabani kaynaginda yapisal
 * olarak yanlislanamaz. Kural tekilligi {@code DbTransitionRuleSource} kurucusunda ve
 * {@code uq_transition_from_action_role} UNIQUE kisitinda, strateji/hedef tutarliligi ise
 * {@code TransitionRule} compact constructor'inda zaten zorunlu; oradan gecen bir kume
 * bu ikisini asla dusuremez. Gercek kazanc diger ikisidir: terminal durumdan cikis ve
 * aktor olmayan role baglanmis kural &mdash; {@code V15} bunlari engelleyen bir kisit
 * icermiyor.
 */
public final class TransitionRuleInvariants {

    private TransitionRuleInvariants() { }

    /** Ayni {@code (from, action, actorRoleId)} birlesimi icin tek kural bulunmalidir. */
    public static void assertNoDuplicateRuleKeys(TransitionRuleSource source) {
        Set<String> keys = new HashSet<>();

        for (TransitionRule rule : source.all()) {
            String key = rule.from() + "|" + rule.action() + "|" + rule.actorRoleId();
            assertThat(keys.add(key))
                    .as("tekrar eden kural: %s", key)
                    .isTrue();
        }
    }

    /** Terminal durum kilitlidir; oradan cikan bir gecis tanimli olmamalidir. */
    public static void assertNoTransitionLeavesTerminalStatus(TransitionRuleSource source) {
        assertThat(source.all())
                .filteredOn(rule -> rule.from().isTerminal())
                .as("terminal durumdan cikan gecis olmamali")
                .isEmpty();
    }

    /** Workflow aktoru olmayan role (ornegin ADMIN) kural baglanmamalidir. */
    public static void assertNoRuleAssignedTo(TransitionRuleSource source, RoleId nonActorRoleId) {
        assertThat(source.all())
                .filteredOn(rule -> rule.actorRoleId().equals(nonActorRoleId))
                .as("aktor olmayan role (%s) kural tanimlanmamali", nonActorRoleId)
                .isEmpty();
    }

    /**
     * {@code NONE} ve {@code DEPARTMENT} hedef rol tasimaz; diger butun stratejiler tasir.
     *
     * <p>{@code DEPARTMENT}'in hedefi bir kullanici degil departmandir; hedef rol
     * geciste degil {@code department_routing_rules}'ta durur (ADR-0006). V23'teki
     * {@code chk_transition_target_strategy_role} de ayni sarti kosar.
     */
    public static void assertTargetStrategyAndExpectedRoleAgree(TransitionRuleSource source) {
        for (TransitionRule rule : source.all()) {
            assertThat(rule.targetStrategy())
                    .as("%s + %s + %s icin hedef stratejisi", rule.from(), rule.action(), rule.actorRoleId())
                    .isNotNull();

            if (rule.targetStrategy() == TargetStrategy.NONE
                    || rule.targetStrategy() == TargetStrategy.DEPARTMENT) {
                assertThat(rule.expectedTargetRoleId())
                        .as("%s + %s %s stratejisinde hedef rol tasimamali",
                                rule.from(), rule.action(), rule.targetStrategy())
                        .isNull();
            } else {
                assertThat(rule.expectedTargetRoleId())
                        .as("%s + %s %s stratejisinde hedef rol tasimali",
                                rule.from(), rule.action(), rule.targetStrategy())
                        .isNotNull();
            }
        }
    }
}
