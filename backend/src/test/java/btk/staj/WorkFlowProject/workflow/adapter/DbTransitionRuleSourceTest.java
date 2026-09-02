package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DbTransitionRuleSource")
class DbTransitionRuleSourceTest {

    @Test
    @DisplayName("teknik satirlari mevcut domain kurallarina map eder")
    void mapsTechnicalRecordsToDomainRules() {
        DbTransitionRuleSource source = source(List.of(
                validRecord(),
                approvalRecord()));

        assertThat(source.all()).containsExactly(
                new TransitionRule(
                        RecordStatus.TASLAK,
                        WorkflowAction.GONDER,
                        RoleName.CALISAN,
                        ActorRequirement.CREATOR,
                        RecordStatus.BSK_YRD_INCELEMESINDE),
                new TransitionRule(
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.ONAYLA,
                        RoleName.BASKAN,
                        ActorRequirement.ASSIGNEE,
                        RecordStatus.ONAYLANDI));
    }

    @Test
    @DisplayName("composite key ile dogru kurali bulur")
    void findsRuleByCompositeKey() {
        DbTransitionRuleSource source = source(List.of(validRecord(), approvalRecord()));

        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN))
                .contains(new TransitionRule(
                        RecordStatus.TASLAK,
                        WorkflowAction.GONDER,
                        RoleName.CALISAN,
                        ActorRequirement.CREATOR,
                        RecordStatus.BSK_YRD_INCELEMESINDE));
    }

    @Test
    @DisplayName("tanimli olmayan composite key icin bos sonuc doner")
    void returnsEmptyForMissingCompositeKey() {
        DbTransitionRuleSource source = source(List.of(validRecord()));

        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.ONAYLA, RoleName.BASKAN))
                .isEmpty();
    }

    @Test
    @DisplayName("all immutable bir snapshot doner")
    void returnsImmutableSnapshot() {
        DbTransitionRuleSource source = source(List.of(validRecord()));

        assertThatThrownBy(() -> source.all().add(new TransitionRule(
                RecordStatus.BASKAN_INCELEMESINDE,
                WorkflowAction.ONAYLA,
                RoleName.BASKAN,
                ActorRequirement.ASSIGNEE,
                RecordStatus.ONAYLANDI)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("reader bir kez cagrilir ve sonraki input degisikligi snapshot'i etkilemez")
    void loadsReaderOnlyOnceAndDetachesSnapshotFromInput() {
        List<TransitionRuleRecord> records = new ArrayList<>(List.of(validRecord()));
        AtomicInteger callCount = new AtomicInteger();
        TransitionRuleRecordReader reader = () -> {
            callCount.incrementAndGet();
            return records;
        };

        DbTransitionRuleSource source = new DbTransitionRuleSource(reader);
        records.clear();

        assertThat(source.all()).hasSize(1);
        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN))
                .isPresent();
        assertThat(callCount).hasValue(1);
    }

    @Test
    @DisplayName("ayni composite key ikinci kez geldiginde fail-fast olur")
    void rejectsDuplicateCompositeKey() {
        TransitionRuleRecord duplicate = new TransitionRuleRecord(
                "TASLAK", "GONDER", "CALISAN", "ASSIGNEE", "ONAYLANDI");

        assertThatThrownBy(() -> source(List.of(validRecord(), duplicate)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("Duplicate")
                .hasMessageContaining("TASLAK")
                .hasMessageContaining("GONDER")
                .hasMessageContaining("CALISAN")
                .hasMessageContaining("row 2");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unknownEnumRecords")
    @DisplayName("bilinmeyen teknik enum degerlerini reddeder")
    void rejectsUnknownEnumValues(
            String fieldName,
            String enumDescription,
            String invalidValue,
            TransitionRuleRecord record) {

        assertThatThrownBy(() -> source(List.of(record)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining(fieldName)
                .hasMessageContaining(enumDescription)
                .hasMessageContaining(invalidValue)
                .hasMessageContaining("row 1");
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("missingFieldRecords")
    @DisplayName("null veya blank teknik alanlari reddeder")
    void rejectsNullOrBlankFields(
            String fieldName,
            String reason,
            TransitionRuleRecord record) {

        assertThatThrownBy(() -> source(List.of(record)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining(fieldName)
                .hasMessageContaining(reason)
                .hasMessageContaining("row 1");
    }

    @Test
    @DisplayName("null reader'i reddeder")
    void rejectsNullReader() {
        assertThatThrownBy(() -> new DbTransitionRuleSource(null))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("TransitionRuleRecordReader")
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("reader null liste dondururse reddeder")
    void rejectsNullReaderResult() {
        assertThatThrownBy(() -> new DbTransitionRuleSource(() -> null))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("findAllActive")
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("reader bos liste dondururse reddeder")
    void rejectsEmptyReaderResult() {
        assertThatThrownBy(() -> source(List.of()))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("no active transition rules");
    }

    @Test
    @DisplayName("aktif satir listesinde null elemani reddeder")
    void rejectsNullRecord() {
        assertThatThrownBy(() -> source(Collections.singletonList(null)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("row 1")
                .hasMessageContaining("null");
    }

    private static Stream<Arguments> unknownEnumRecords() {
        return Stream.of(
                Arguments.of(
                        "fromStatus",
                        "workflow status",
                        "UNKNOWN_FROM",
                        new TransitionRuleRecord(
                                "UNKNOWN_FROM", "GONDER", "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "action",
                        "workflow action",
                        "UNKNOWN_ACTION",
                        new TransitionRuleRecord(
                                "TASLAK", "UNKNOWN_ACTION", "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "actorRole",
                        "actor role",
                        "UNKNOWN_ROLE",
                        new TransitionRuleRecord(
                                "TASLAK", "GONDER", "UNKNOWN_ROLE", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "actorRequirement",
                        "actor requirement",
                        "UNKNOWN_REQUIREMENT",
                        new TransitionRuleRecord(
                                "TASLAK", "GONDER", "CALISAN", "UNKNOWN_REQUIREMENT", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "toStatus",
                        "workflow status",
                        "UNKNOWN_TO",
                        new TransitionRuleRecord(
                                "TASLAK", "GONDER", "CALISAN", "CREATOR", "UNKNOWN_TO")));
    }

    private static Stream<Arguments> missingFieldRecords() {
        return Stream.of(
                Arguments.of(
                        "fromStatus", "null",
                        new TransitionRuleRecord(null, "GONDER", "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "fromStatus", "blank",
                        new TransitionRuleRecord(" ", "GONDER", "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "action", "null",
                        new TransitionRuleRecord("TASLAK", null, "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "action", "blank",
                        new TransitionRuleRecord("TASLAK", " ", "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "actorRole", "null",
                        new TransitionRuleRecord("TASLAK", "GONDER", null, "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "actorRole", "blank",
                        new TransitionRuleRecord("TASLAK", "GONDER", " ", "CREATOR", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "actorRequirement", "null",
                        new TransitionRuleRecord("TASLAK", "GONDER", "CALISAN", null, "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "actorRequirement", "blank",
                        new TransitionRuleRecord("TASLAK", "GONDER", "CALISAN", " ", "BSK_YRD_INCELEMESINDE")),
                Arguments.of(
                        "toStatus", "null",
                        new TransitionRuleRecord("TASLAK", "GONDER", "CALISAN", "CREATOR", null)),
                Arguments.of(
                        "toStatus", "blank",
                        new TransitionRuleRecord("TASLAK", "GONDER", "CALISAN", "CREATOR", " ")));
    }

    private static DbTransitionRuleSource source(List<TransitionRuleRecord> records) {
        return new DbTransitionRuleSource(() -> records);
    }

    private static TransitionRuleRecord validRecord() {
        return new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                "CALISAN",
                "CREATOR",
                "BSK_YRD_INCELEMESINDE");
    }

    private static TransitionRuleRecord approvalRecord() {
        return new TransitionRuleRecord(
                "BASKAN_INCELEMESINDE",
                "ONAYLA",
                "BASKAN",
                "ASSIGNEE",
                "ONAYLANDI");
    }
}
