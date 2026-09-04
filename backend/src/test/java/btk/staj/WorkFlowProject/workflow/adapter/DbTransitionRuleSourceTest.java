package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
                        WorkflowRoleFixtures.id(RoleName.CALISAN),
                        ActorRequirement.CREATOR,
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        TargetStrategy.ROLE,
                        WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI),
                        AuthorizationFixtures.requiredPermission(WorkflowAction.GONDER)),
                new TransitionRule(
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.ONAYLA,
                        WorkflowRoleFixtures.id(RoleName.BASKAN),
                        ActorRequirement.ASSIGNEE,
                        RecordStatus.ONAYLANDI,
                        TargetStrategy.NONE,
                        null,
                        AuthorizationFixtures.requiredPermission(WorkflowAction.ONAYLA)));
    }

    @Test
    @DisplayName("composite key ile dogru kurali bulur")
    void findsRuleByCompositeKey() {
        DbTransitionRuleSource source = source(List.of(validRecord(), approvalRecord()));

        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, WorkflowRoleFixtures.id(RoleName.CALISAN)))
                .contains(new TransitionRule(
                        RecordStatus.TASLAK,
                        WorkflowAction.GONDER,
                        WorkflowRoleFixtures.id(RoleName.CALISAN),
                        ActorRequirement.CREATOR,
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        TargetStrategy.ROLE,
                        WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI),
                        AuthorizationFixtures.requiredPermission(WorkflowAction.GONDER)));
    }

    @Test
    @DisplayName("tanimli olmayan composite key icin bos sonuc doner")
    void returnsEmptyForMissingCompositeKey() {
        DbTransitionRuleSource source = source(List.of(validRecord()));

        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.ONAYLA, WorkflowRoleFixtures.id(RoleName.BASKAN)))
                .isEmpty();
    }

    @Test
    @DisplayName("all immutable bir snapshot doner")
    void returnsImmutableSnapshot() {
        DbTransitionRuleSource source = source(List.of(validRecord()));

        assertThatThrownBy(() -> source.all().add(new TransitionRule(
                RecordStatus.BASKAN_INCELEMESINDE,
                WorkflowAction.ONAYLA,
                WorkflowRoleFixtures.id(RoleName.BASKAN),
                ActorRequirement.ASSIGNEE,
                RecordStatus.ONAYLANDI,
                TargetStrategy.NONE,
                null,
                AuthorizationFixtures.requiredPermission(WorkflowAction.ONAYLA))))
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
        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, WorkflowRoleFixtures.id(RoleName.CALISAN)))
                .isPresent();
        assertThat(callCount).hasValue(1);
    }

    @Test
    @DisplayName("ayni composite key ikinci kez geldiginde fail-fast olur")
    void rejectsDuplicateCompositeKey() {
        TransitionRuleRecord duplicate = new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                WorkflowRoleFixtures.value(RoleName.CALISAN),
                "ASSIGNEE",
                "ONAYLANDI",
                "NONE",
                null,
                AuthorizationFixtures.requiredPermission("GONDER"));

        assertThatThrownBy(() -> source(List.of(validRecord(), duplicate)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("Duplicate")
                .hasMessageContaining("TASLAK")
                .hasMessageContaining("GONDER")
                .hasMessageContaining(WorkflowRoleFixtures.value(RoleName.CALISAN).toString())
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
                                "UNKNOWN_FROM",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "action",
                        "workflow action",
                        "UNKNOWN_ACTION",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "UNKNOWN_ACTION",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("UNKNOWN_ACTION"))),
                Arguments.of(
                        "actorRoleId",
                        "positive",
                        "-999",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                -999,
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "actorRequirement",
                        "actor requirement",
                        "UNKNOWN_REQUIREMENT",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "UNKNOWN_REQUIREMENT",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "toStatus",
                        "workflow status",
                        "UNKNOWN_TO",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                "UNKNOWN_TO",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))));
    }

    private static Stream<Arguments> missingFieldRecords() {
        return Stream.of(
                Arguments.of(
                        "fromStatus", "null",
                        new TransitionRuleRecord(
                                null,
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "fromStatus", "blank",
                        new TransitionRuleRecord(
                                " ",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "action", "null",
                        new TransitionRuleRecord(
                                "TASLAK",
                                null,
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission(null))),
                Arguments.of(
                        "action", "blank",
                        new TransitionRuleRecord(
                                "TASLAK",
                                " ",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission(" "))),
                Arguments.of(
                        "actorRoleId", "null",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                null,
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "actorRoleId", "positive",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                0,
                                "CREATOR",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "actorRequirement", "null",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                null,
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "actorRequirement", "blank",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                " ",
                                "BSK_YRD_INCELEMESINDE",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "toStatus", "null",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                null,
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))),
                Arguments.of(
                        "toStatus", "blank",
                        new TransitionRuleRecord(
                                "TASLAK",
                                "GONDER",
                                WorkflowRoleFixtures.value(RoleName.CALISAN),
                                "CREATOR",
                                " ",
                                "ROLE",
                                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                                AuthorizationFixtures.requiredPermission("GONDER"))));
    }

    @Test
    @DisplayName("hedefsiz gecis beklenen hedef rol tasiyorsa satir numarasiyla reddedilir")
    void rejectsNoneStrategyCarryingExpectedTargetRole() {
        TransitionRuleRecord inconsistent = new TransitionRuleRecord(
                "BASKAN_INCELEMESINDE",
                "ONAYLA",
                WorkflowRoleFixtures.value(RoleName.BASKAN),
                "ASSIGNEE",
                "ONAYLANDI",
                "NONE",
                WorkflowRoleFixtures.value(RoleName.CALISAN),
                AuthorizationFixtures.requiredPermission("ONAYLA"));

        assertThatThrownBy(() -> source(List.of(inconsistent)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("row 1")
                .hasMessageContaining("NONE");
    }

    @Test
    @DisplayName("hedef gerektiren gecis beklenen hedef rolsuz kalirsa reddedilir")
    void rejectsTargetStrategyWithoutExpectedTargetRole() {
        // Bu kombinasyon veritabani CHECK'ini gecerdi (CHECK yalniz ROLE icin rolu zorunlu
        // kilar) ama servisin sentinel protokolunu sessizce kirardi.
        TransitionRuleRecord inconsistent = new TransitionRuleRecord(
                "BSK_YRD_INCELEMESINDE",
                "CALISANA_GERI_GONDER",
                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                "ASSIGNEE",
                "DUZENLEME_BEKLIYOR",
                "CREATOR",
                null,
                AuthorizationFixtures.requiredPermission("CALISANA_GERI_GONDER"));

        assertThatThrownBy(() -> source(List.of(inconsistent)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("row 1")
                .hasMessageContaining("CREATOR");
    }

    @Test
    @DisplayName("bilinmeyen hedef stratejisini reddeder")
    void rejectsUnknownTargetStrategy() {
        TransitionRuleRecord unknown = new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                WorkflowRoleFixtures.value(RoleName.CALISAN),
                "CREATOR",
                "BSK_YRD_INCELEMESINDE",
                "UNKNOWN_STRATEGY",
                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                AuthorizationFixtures.requiredPermission("GONDER"));

        assertThatThrownBy(() -> source(List.of(unknown)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("targetStrategy")
                .hasMessageContaining("UNKNOWN_STRATEGY");
    }

    @Test
    @DisplayName("hedef stratejisi bos gelirse reddeder")
    void rejectsMissingTargetStrategy() {
        TransitionRuleRecord missing = new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                WorkflowRoleFixtures.value(RoleName.CALISAN),
                "CREATOR",
                "BSK_YRD_INCELEMESINDE",
                null,
                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                AuthorizationFixtures.requiredPermission("GONDER"));

        assertThatThrownBy(() -> source(List.of(missing)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("targetStrategy");
    }

    @Test
    @DisplayName("alti hedef stratejisinin tamamini cozer")
    void mapsEveryTargetStrategy() {
        assertThat(TargetStrategy.values()).hasSize(6);
        for (TargetStrategy strategy : TargetStrategy.values()) {
            Integer role = strategy == TargetStrategy.NONE || strategy == TargetStrategy.DEPARTMENT
                    ? null : WorkflowRoleFixtures.value(RoleName.CALISAN);
            DbTransitionRuleSource source = source(List.of(new TransitionRuleRecord(
                    "TASLAK",
                    "GONDER",
                    WorkflowRoleFixtures.value(RoleName.CALISAN),
                    "CREATOR",
                    "BSK_YRD_INCELEMESINDE",
                    strategy.name(),
                    role,
                    AuthorizationFixtures.requiredPermission("GONDER"))));

            assertThat(source.all()).singleElement()
                    .extracting(TransitionRule::targetStrategy)
                    .isEqualTo(strategy);
        }
    }

    @Test
    void resolvesIdsFromAnotherEnvironmentWithoutNumericRoleConstants() {
        var actorId = new RoleId(1001);
        var targetId = new RoleId(2002);
        var record = new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                actorId.value(),
                "CREATOR",
                "BSK_YRD_INCELEMESINDE",
                "ROLE",
                targetId.value(),
                "RECORD_FORWARD");
        var source = new DbTransitionRuleSource(() -> List.of(record));
        var rule = source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, new RoleId(1001)).orElseThrow();
        assertThat(rule.actorRoleId()).isEqualTo(new RoleId(1001));
        assertThat(rule.expectedTargetRoleId()).isEqualTo(new RoleId(2002));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveActorIdWithRowNumber(int id) {
        var row = new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                id,
                "CREATOR",
                "BSK_YRD_INCELEMESINDE",
                "ROLE",
                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                "RECORD_FORWARD");
        assertThatThrownBy(() -> source(List.of(validRecord(), row)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("actorRoleId").hasMessageContaining("row 2").hasMessageContaining("positive");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveTargetIdWithRowNumber(int id) {
        var row = new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                WorkflowRoleFixtures.value(RoleName.CALISAN),
                "CREATOR",
                "BSK_YRD_INCELEMESINDE",
                "ROLE",
                id,
                "RECORD_FORWARD");
        assertThatThrownBy(() -> source(List.of(validRecord(), row)))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("expectedTargetRoleId").hasMessageContaining("row 2").hasMessageContaining("positive");
    }

    @Test
    void dynamicActorAndTargetAreIndexedByTheirIds() {
        var row = new TransitionRuleRecord("TASLAK", "GONDER", 7001, "CREATOR",
                "BSK_YRD_INCELEMESINDE", "ROLE", 7007, "RECORD_FORWARD");
        var source = source(List.of(row));
        var rule = source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, new RoleId(7001)).orElseThrow();
        assertThat(rule.expectedTargetRoleId()).isEqualTo(new RoleId(7007));
        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, new RoleId(7002))).isEmpty();
    }

    private static DbTransitionRuleSource source(List<TransitionRuleRecord> records) {
        return new DbTransitionRuleSource(() -> records);
    }

    private static TransitionRuleRecord validRecord() {
        return new TransitionRuleRecord(
                "TASLAK",
                "GONDER",
                WorkflowRoleFixtures.value(RoleName.CALISAN),
                "CREATOR",
                "BSK_YRD_INCELEMESINDE",
                "ROLE",
                WorkflowRoleFixtures.value(RoleName.BASKAN_YARDIMCISI),
                AuthorizationFixtures.requiredPermission("GONDER"));
    }

    private static TransitionRuleRecord approvalRecord() {
        return new TransitionRuleRecord(
                "BASKAN_INCELEMESINDE",
                "ONAYLA",
                WorkflowRoleFixtures.value(RoleName.BASKAN),
                "ASSIGNEE",
                "ONAYLANDI",
                "NONE",
                null,
                AuthorizationFixtures.requiredPermission("ONAYLA"));
    }
}
