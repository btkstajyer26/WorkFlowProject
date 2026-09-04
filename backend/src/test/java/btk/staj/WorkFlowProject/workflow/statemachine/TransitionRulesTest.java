package btk.staj.WorkFlowProject.workflow.statemachine;

import btk.staj.WorkFlowProject.support.AbstractTransitionRuleInvariants;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Merkezi gecis tablosunun tutarlilik testleri. Bu testler tablonun kendi
 * icinde celismedigini garanti eder; kural mantigi
 * {@link WorkflowTransitionValidatorTest} icinde dogrulanir.
 *
 * <p>Kaynaktan bagimsiz invariantlar {@link AbstractTransitionRuleInvariants}'ten
 * miras alinir ve ayni kontroller veritabani kaynagi uzerinde de kosar (TZ-1).
 * Burada kalanlar baslangic seed'ine ozgu olanlardir.
 */
@DisplayName("TransitionRules")
class TransitionRulesTest extends AbstractTransitionRuleInvariants {

    @Override
    protected TransitionRuleSource ruleSource() {
        return WorkflowRoleFixtures.rules();
    }

    @Override
    protected RoleId nonActorRoleId() {
        return WorkflowRoleFixtures.id(RoleName.ADMIN);
    }

    @Test
    @DisplayName("gecis matrisindeki sekiz kural tanimlidir")
    void kuralSayisi() {
        assertThat(WorkflowRoleFixtures.rules().all()).hasSize(8);
    }

    @Test
    @DisplayName("tabloda olmayan birlesim icin bos sonuc doner")
    void tanimsizBirlesim() {
        assertThat(WorkflowRoleFixtures.rules().find(RecordStatus.TASLAK, WorkflowAction.ONAYLA, WorkflowRoleFixtures.id(RoleName.BASKAN)))
                .isEmpty();
    }

    @Test
    @DisplayName("tanimli birlesim icin dogru hedef durum doner")
    void tanimliBirlesim() {
        assertThat(WorkflowRoleFixtures.rules().find(RecordStatus.TASLAK, WorkflowAction.GONDER, WorkflowRoleFixtures.id(RoleName.CALISAN)))
                .get()
                .extracting(TransitionRule::to)
                .isEqualTo(RecordStatus.BSK_YRD_INCELEMESINDE);
    }

    @Test
    @DisplayName("ayni aksiyon farkli gecislerde ayni hedefe gitmek zorunda degil")
    void sameActionCanCarryDifferentTargetsAcrossTransitions() {
        // CALISANA_GERI_GONDER iki ayri satirda kullaniliyor. Hedef bilgisi aksiyonda
        // tutulsaydi bu iki satir ayrisamazdi; testin varlik sebebi bunu sabitlemek.
        assertThat(WorkflowRoleFixtures.rules().all())
                .filteredOn(rule -> rule.action() == WorkflowAction.CALISANA_GERI_GONDER)
                .hasSize(2)
                .allSatisfy(rule -> assertThat(rule.targetStrategy()).isEqualTo(TargetStrategy.CREATOR));
    }
}
