package btk.staj.WorkFlowProject.workflow.statemachine;

import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Merkezi gecis tablosunun tutarlilik testleri. Bu testler tablonun kendi
 * icinde celismedigini garanti eder; kural mantigi
 * {@link WorkflowTransitionValidatorTest} icinde dogrulanir.
 */
@DisplayName("TransitionRules")
class TransitionRulesTest {

    @Test
    @DisplayName("gecis matrisindeki sekiz kural tanimlidir")
    void kuralSayisi() {
        assertThat(WorkflowRoleFixtures.rules().all()).hasSize(8);
    }

    @Test
    @DisplayName("ayni durum-aksiyon-rol birlesimi icin birden fazla kural yoktur")
    void tekrarEdenKuralYok() {
        Set<String> keys = new HashSet<>();

        for (TransitionRule rule : WorkflowRoleFixtures.rules().all()) {
            String key = rule.from() + "|" + rule.action() + "|" + rule.actorRoleId();
            assertThat(keys.add(key))
                    .as("tekrar eden kural: %s", key)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("terminal durumdan cikan kural tanimli degildir")
    void terminalDurumdanCikisYok() {
        assertThat(WorkflowRoleFixtures.rules().all())
                .filteredOn(rule -> rule.from().isTerminal())
                .as("terminal durumdan cikan gecis olmamali")
                .isEmpty();
    }

    @Test
    @DisplayName("her kuralin yetkili rolu workflow aktorudur")
    void aktorRolleriGecerli() {
        assertThat(WorkflowRoleFixtures.rules().all())
                .filteredOn(rule -> rule.actorRoleId().equals(WorkflowRoleFixtures.id(RoleName.ADMIN)))
                .as("ADMIN gibi aktor olmayan roller icin kural tanimlanmamali")
                .isEmpty();
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
    @DisplayName("her kural hedef stratejisi tasir ve strateji ile hedef rol tutarlidir")
    void everyRuleCarriesConsistentTargetMetadata() {
        for (TransitionRule rule : WorkflowRoleFixtures.rules().all()) {
            assertThat(rule.targetStrategy())
                    .as("%s + %s + %s icin hedef stratejisi", rule.from(), rule.action(), rule.actorRoleId())
                    .isNotNull();

            // TransitionRule'un invariantinin statik tabloda da tuttugunu dogrular:
            // hedef gerektiren gecis beklenen rolu tasir, gerektirmeyen tasimaz.
            if (rule.targetStrategy() == TargetStrategy.NONE) {
                assertThat(rule.expectedTargetRoleId()).isNull();
            } else {
                assertThat(rule.expectedTargetRoleId()).isNotNull();
            }
        }
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
