package btk.staj.WorkFlowProject.workflow.statemachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

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
        assertThat(TransitionRules.all()).hasSize(8);
    }

    @Test
    @DisplayName("ayni durum-aksiyon-rol birlesimi icin birden fazla kural yoktur")
    void tekrarEdenKuralYok() {
        Set<String> keys = new HashSet<>();

        for (TransitionRule rule : TransitionRules.all()) {
            String key = rule.from() + "|" + rule.action() + "|" + rule.actorRole();
            assertThat(keys.add(key))
                    .as("tekrar eden kural: %s", key)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("terminal durumdan cikan kural tanimli degildir")
    void terminalDurumdanCikisYok() {
        assertThat(TransitionRules.all())
                .filteredOn(rule -> rule.from().isTerminal())
                .as("terminal durumdan cikan gecis olmamali")
                .isEmpty();
    }

    @Test
    @DisplayName("her kuralin yetkili rolu workflow aktorudur")
    void aktorRolleriGecerli() {
        assertThat(TransitionRules.all())
                .filteredOn(rule -> !rule.actorRole().isWorkflowActor())
                .as("ADMIN gibi aktor olmayan roller icin kural tanimlanmamali")
                .isEmpty();
    }

    @Test
    @DisplayName("tabloda olmayan birlesim icin bos sonuc doner")
    void tanimsizBirlesim() {
        assertThat(TransitionRules.find(RecordStatus.TASLAK, WorkflowAction.ONAYLA, RoleName.BASKAN))
                .isEmpty();
    }

    @Test
    @DisplayName("tanimli birlesim icin dogru hedef durum doner")
    void tanimliBirlesim() {
        assertThat(TransitionRules.find(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN))
                .get()
                .extracting(TransitionRule::to)
                .isEqualTo(RecordStatus.BSK_YRD_INCELEMESINDE);
    }
}
