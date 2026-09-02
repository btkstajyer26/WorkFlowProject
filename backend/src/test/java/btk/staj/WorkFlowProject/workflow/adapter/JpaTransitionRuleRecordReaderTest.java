package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.repository.TransitionRuleRow;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JpaTransitionRuleRecordReader")
class JpaTransitionRuleRecordReaderTest {

    @Test
    @DisplayName("satirlari port sozlesmesindeki teknik anahtarlara cevirir")
    void mapsRowsToTechnicalRecords() {
        List<TransitionRuleRecord> records = readerReturning(
                row("TASLAK", "GONDER", "CALISAN", "Calisan", ActorRequirement.CREATOR,
                        "BSK_YRD_INCELEMESINDE"),
                row("BASKAN_INCELEMESINDE", "ONAYLA", "BASKAN", "Baskan", ActorRequirement.ASSIGNEE,
                        "ONAYLANDI"))
                .findAllActive();

        assertThat(records).containsExactly(
                new TransitionRuleRecord("TASLAK", "GONDER", "CALISAN", "CREATOR",
                        "BSK_YRD_INCELEMESINDE"),
                new TransitionRuleRecord("BASKAN_INCELEMESINDE", "ONAYLA", "BASKAN", "ASSIGNEE",
                        "ONAYLANDI"));
    }

    @Test
    @DisplayName("aktor rolunu system_key'den okur, gosterilen rol adindan degil")
    void readsActorRoleFromSystemKeyNotDisplayName() {
        // Rol yonetim panelinden "Calisan Personel" olarak yeniden adlandirildi;
        // system_key degismedi. Kural kimligi degismeyen anahtardan gelmeli.
        List<TransitionRuleRecord> records = readerReturning(
                row("TASLAK", "GONDER", "CALISAN", "Calisan Personel", ActorRequirement.CREATOR,
                        "BSK_YRD_INCELEMESINDE"))
                .findAllActive();

        assertThat(records).singleElement()
                .extracting(TransitionRuleRecord::actorRole)
                .isEqualTo("CALISAN");
    }

    @Test
    @DisplayName("dinamik rol aktor yapilmissa rol adini iceren acik hata verir")
    void rejectsActorRoleWithoutSystemKey() {
        assertThatThrownBy(() -> readerReturning(
                row("TASLAK", "GONDER", null, "Sube Muduru", ActorRequirement.CREATOR,
                        "BSK_YRD_INCELEMESINDE"))
                .findAllActive())
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("row 1")
                .hasMessageContaining("system_key")
                .hasMessageContaining("Sube Muduru");
    }

    @Test
    @DisplayName("bos system_key de null gibi reddedilir")
    void rejectsBlankSystemKey() {
        assertThatThrownBy(() -> readerReturning(
                row("TASLAK", "GONDER", "   ", "Sube Muduru", ActorRequirement.CREATOR,
                        "BSK_YRD_INCELEMESINDE"))
                .findAllActive())
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("row 1");
    }

    @Test
    @DisplayName("hatali satirin numarasini bildirir")
    void reportsFailingRowNumber() {
        assertThatThrownBy(() -> readerReturning(
                row("TASLAK", "GONDER", "CALISAN", "Calisan", ActorRequirement.CREATOR,
                        "BSK_YRD_INCELEMESINDE"),
                row("BASKAN_INCELEMESINDE", "ONAYLA", null, "Sube Muduru",
                        ActorRequirement.ASSIGNEE, "ONAYLANDI"))
                .findAllActive())
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("row 2");
    }

    @Test
    @DisplayName("actor_requirement bos gelirse reddeder")
    void rejectsNullActorRequirement() {
        assertThatThrownBy(() -> readerReturning(
                row("TASLAK", "GONDER", "CALISAN", "Calisan", null, "BSK_YRD_INCELEMESINDE"))
                .findAllActive())
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("actor_requirement");
    }

    @Test
    @DisplayName("bos sonuc bos liste dondurur; kural yoklugu karari kaynagin isidir")
    void returnsEmptyListWhenNoActiveRows() {
        assertThat(readerReturning().findAllActive()).isEmpty();
    }

    @Test
    @DisplayName("repository null dondurursa acik hata verir")
    void rejectsNullRepositoryResult() {
        WorkflowTransitionRepository repository = mock(WorkflowTransitionRepository.class);
        when(repository.findActiveRuleRows()).thenReturn(null);

        assertThatIllegalStateException()
                .isThrownBy(() -> new JpaTransitionRuleRecordReader(repository).findAllActive())
                .withMessageContaining("findActiveRuleRows");
    }

    @Test
    @DisplayName("repository olmadan olusturulamaz")
    void rejectsNullRepository() {
        assertThatNullPointerException()
                .isThrownBy(() -> new JpaTransitionRuleRecordReader(null))
                .withMessageContaining("transitionRepository");
    }

    private static JpaTransitionRuleRecordReader readerReturning(TransitionRuleRow... rows) {
        WorkflowTransitionRepository repository = mock(WorkflowTransitionRepository.class);
        when(repository.findActiveRuleRows()).thenReturn(Arrays.asList(rows));
        return new JpaTransitionRuleRecordReader(repository);
    }

    private static TransitionRuleRow row(String fromStatus,
                                         String action,
                                         String actorSystemKey,
                                         String actorRoleName,
                                         ActorRequirement actorRequirement,
                                         String toStatus) {

        return new TransitionRuleRow(
                fromStatus, action, actorSystemKey, actorRoleName, actorRequirement, toStatus);
    }
}
