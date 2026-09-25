package btk.staj.WorkFlowProject.support;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TransitionRuleInvariants} kontrollerini bir kural kaynagina uygulayan taban sinif.
 *
 * <p>Kontroller tek bir {@code @Test} icinde toplanmaz: ayrisma oldugunda hangi
 * invariantin dustugu test adindan okunabilmelidir.
 *
 * <p>Alt siniflar kaynagi {@link #ruleSource()} ile verir. Kaynak <strong>her cagrida
 * taze</strong> kurulmalidir: {@code DbTransitionRuleSource} veriyi constructor'da okur,
 * dolayisiyla ayni test icinde yapilan bir veri degisikligi ancak yeni ornekte gorunur.
 */
public abstract class AbstractTransitionRuleInvariants {

    /** Denetlenecek kural kaynagi; her cagrida yeniden kurulmalidir. */
    protected abstract TransitionRuleSource ruleSource();

    /** Workflow aktoru olmayan rolun kimligi; bu kaynakta ADMIN karsiligi. */
    protected abstract RoleId nonActorRoleId();

    @Test
    @DisplayName("ayni durum-aksiyon-rol birlesimi icin birden fazla kural yoktur")
    protected void noDuplicateRuleKeys() {
        TransitionRuleInvariants.assertNoDuplicateRuleKeys(ruleSource());
    }

    @Test
    @DisplayName("terminal durumdan cikan kural tanimli degildir")
    protected void noTransitionLeavesTerminalStatus() {
        TransitionRuleInvariants.assertNoTransitionLeavesTerminalStatus(ruleSource());
    }

    @Test
    @DisplayName("aktor olmayan role kural baglanmamistir")
    protected void noRuleAssignedToNonActorRole() {
        TransitionRuleInvariants.assertNoRuleAssignedTo(ruleSource(), nonActorRoleId());
    }

    @Test
    @DisplayName("hedef stratejisi ile beklenen hedef rol tutarlidir")
    protected void targetStrategyAndExpectedRoleAgree() {
        TransitionRuleInvariants.assertTargetStrategyAndExpectedRoleAgree(ruleSource());
    }
}
