package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionResponse;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowDataIntegrityException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowRecordNotFoundException;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;
import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.model.WorkflowTransitionAudit;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.port.WorkflowEventPublisher;
import btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowApplicationService")
class WorkflowApplicationServiceTest {

    private static final UUID RECORD_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID CREATOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID TARGET_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID LAST_DEPUTY_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID SUPPLIED_TARGET_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant PERFORMED_AT = Instant.parse("2026-08-07T12:34:56Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(PERFORMED_AT, ZoneOffset.UTC);

    @Mock
    private WorkflowRecordPort recordPort;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private TargetUserResolver targetUserResolver;

    @Mock
    private AuditService auditService;

    @Mock
    private WorkflowEventPublisher eventPublisher;

    private WorkflowApplicationService service;

    @BeforeEach
    void setUp() {
        // Validator ve servis AYNI kaynagi gorur; aksi halde ikisi farkli kural kumesine
        // bakar ve servisin "validator izin verdigi gecisi kaynak tanimiyor" korumasi
        // yanlis yere tetiklenirdi.
        TransitionRuleSource ruleSource = new StaticTransitionRuleSource(WorkflowRoleFixtures.roleIds());
        service = new WorkflowApplicationService(
                recordPort,
                currentActorProvider,
                targetUserResolver,
                new WorkflowTransitionValidator(ruleSource),
                ruleSource,
                auditService,
                eventPublisher,
                FIXED_CLOCK);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allowedTransitions")
    @DisplayName("merkezi tablodaki sekiz gecisi dogru uygulama komutlarina donusturur")
    void appliesAllEightAllowedTransitions(AllowedCase scenario) {
        WorkflowRecordSnapshot record = scenario.record();
        WorkflowActionRequest request = scenario.request();
        arrange(record, scenario.actorRole());
        when(targetUserResolver.resolve(any(), any(), eq(request.targetUserId()), eq(record)))
                .thenReturn(scenario.resolution());

        WorkflowActionResponse response = service.performAction(RECORD_ID, request);

        assertThat(response).isEqualTo(new WorkflowActionResponse(
                RECORD_ID,
                scenario.action(),
                scenario.previousStatus(),
                scenario.newStatus(),
                scenario.expectedAssignedTo(),
                ACTOR_ID,
                PERFORMED_AT));
        verify(recordPort).update(new WorkflowRecordUpdate(
                RECORD_ID,
                scenario.newStatus(),
                scenario.expectedAssignedTo(),
                scenario.expectedLastDeputyId(),
                scenario.version(),
                PERFORMED_AT));
        verify(auditService).record(new WorkflowTransitionAudit(
                RECORD_ID,
                scenario.action(),
                scenario.previousStatus(),
                scenario.newStatus(),
                ACTOR_ID,
                scenario.actorRole(),
                scenario.expectedAssignedTo(),
                scenario.comment(),
                PERFORMED_AT));
        verify(eventPublisher).publish(new WorkflowStatusChangedEvent(
                RECORD_ID,
                scenario.action(),
                scenario.previousStatus(),
                scenario.newStatus(),
                ACTOR_ID,
                scenario.actorRole(),
                scenario.previousAssignedTo(),
                scenario.expectedAssignedTo(),
                scenario.comment(),
                PERFORMED_AT));
    }

    @Test
    @DisplayName("ADMIN reddi Baskan yapilandirmasini sorgulamadan once gelir")
    void rejectsAdminBeforeResolvingPresident() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BSK_YRD_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        arrange(record, RoleName.ADMIN);

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(WorkflowAction.BASKANA_ILET, null, null)));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_ROLE_NOT_ALLOWED);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("actionsThatRejectRequestTarget")
    @DisplayName("backend tarafindan cozulen aksiyonlarda gonderilen hedef resolverdan once reddedilir")
    void rejectsForbiddenSuppliedTargetBeforeResolution(ForbiddenTargetCase scenario) {
        WorkflowRecordSnapshot record = activeRecord(
                scenario.status(),
                scenario.createdBy(),
                scenario.assignedTo(),
                LAST_DEPUTY_ID);
        arrange(record, scenario.actorRole());

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(
                                scenario.action(),
                                SUPPLIED_TARGET_ID,
                                scenario.comment())));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_TARGET_NOT_ALLOWED);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    @Test
    @DisplayName("terminal kayit hatasi hedef cozumlemesinden once gelir")
    void rejectsTerminalRecordBeforeResolvingTarget() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.ONAYLANDI, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        arrange(record, RoleName.BASKAN_YARDIMCISI);

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(WorkflowAction.BASKANA_ILET, null, null)));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_RECORD_LOCKED);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    @Test
    @DisplayName("gecersiz gecis hatasi hedef cozumlemesinden once gelir")
    void rejectsInvalidTransitionBeforeResolvingTarget() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.TASLAK, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        arrange(record, RoleName.BASKAN_YARDIMCISI);

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(WorkflowAction.BASKANA_ILET, null, null)));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    @Test
    @DisplayName("zorunlu aciklama hatasi hedef veri tutarsizligindan once gelir")
    void rejectsMissingCommentBeforeResolvingTarget() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, null);
        arrange(record, RoleName.BASKAN);

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(
                                WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,
                                null,
                                null)));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_COMMENT_REQUIRED);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    @Test
    @DisplayName("kayit iliskisi hatasi gonderilmemesi gereken hedef alanindan once gelir")
    void preservesForbiddenPrecedenceOverSuppliedTarget() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BSK_YRD_INCELEMESINDE, CREATOR_ID, LAST_DEPUTY_ID, LAST_DEPUTY_ID);
        arrange(record, RoleName.BASKAN_YARDIMCISI);

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(
                                WorkflowAction.BASKANA_ILET,
                                SUPPLIED_TARGET_ID,
                                null)));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    /**
     * Hedefi artik her aksiyon icin backend cozuyor, yani bu varyanti ureten
     * bir kol kalmadi. Sealed tipte durdugu surece servisin onu dogru
     * esledigi dogrulanmaya devam eder.
     */
    @Test
    @DisplayName("bulunamayan request hedefi varyanti hedef rol hatasina eslenir")
    void mapsMissingRequestTargetUserToTargetRoleInvalid() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.TASLAK, ACTOR_ID, null, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.GONDER, null, null);
        arrange(record, RoleName.CALISAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.RequestTargetNotFound(SUPPLIED_TARGET_ID));

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, request));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID);
        assertNoMutation();
    }

    @Test
    @DisplayName("pasif hedef final validator tarafindan reddedilir")
    void rejectsInactiveResolvedTarget() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.TASLAK, ACTOR_ID, null, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.GONDER, null, null);
        arrange(record, RoleName.CALISAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.Resolved(
                        new WorkflowUserSnapshot(TARGET_ID, RoleName.BASKAN_YARDIMCISI, false)));

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, request));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_TARGET_INACTIVE);
        assertNoMutation();
    }

    @Test
    @DisplayName("yanlis roldeki hedef final validator tarafindan reddedilir")
    void rejectsResolvedTargetWithWrongRole() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.TASLAK, ACTOR_ID, null, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.GONDER, null, null);
        arrange(record, RoleName.CALISAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.Resolved(
                        new WorkflowUserSnapshot(TARGET_ID, RoleName.ADMIN, true)));

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, request));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID);
        assertNoMutation();
    }

    @Test
    @DisplayName("tek aktif Baskan Yardimcisi yoksa gonderme role not configured ile durur")
    void mapsDeputyConfigurationFailure() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.TASLAK, ACTOR_ID, null, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.GONDER, null, null);
        arrange(record, RoleName.CALISAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.RoleNotConfigured(RoleName.BASKAN_YARDIMCISI, 0));

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, request));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_ROLE_NOT_CONFIGURED);
        assertNoMutation();
    }

    @Test
    @DisplayName("tek aktif Baskan yoksa role not configured hatasi korunur")
    void mapsPresidentConfigurationFailure() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BSK_YRD_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.BASKANA_ILET, null, null);
        arrange(record, RoleName.BASKAN_YARDIMCISI);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.RoleNotConfigured(RoleName.BASKAN, 0));

        WorkflowApplicationException exception = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, request));

        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_ROLE_NOT_CONFIGURED);
        assertNoMutation();
    }

    @Test
    @DisplayName("hedef referans tutarsizligi public hata kodu icat etmeden tasinir")
    void exposesDataIntegrityFailureSeparately() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, null);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, null, "duzeltiniz");
        TargetResolution.DataIntegrityFailure failure = new TargetResolution.DataIntegrityFailure(
                TargetResolution.DataIntegrityReason.LAST_DEPUTY_ID_MISSING,
                null);
        arrange(record, RoleName.BASKAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(failure);

        WorkflowDataIntegrityException exception = assertThrows(
                WorkflowDataIntegrityException.class,
                () -> service.performAction(RECORD_ID, request));

        assertThat(exception.reason()).isEqualTo(failure.reason());
        assertThat(exception.referencedUserId()).isNull();
        assertNoMutation();
    }

    @Test
    @DisplayName("soft-delete edilmis kayit bulunamamis gibi davranir")
    void rejectsSoftDeletedRecordWithoutResolutionOrSideEffects() {
        WorkflowRecordSnapshot record = new WorkflowRecordSnapshot(
                RECORD_ID,
                RecordStatus.TASLAK,
                ACTOR_ID,
                null,
                LAST_DEPUTY_ID,
                PERFORMED_AT.minusSeconds(1),
                7);
        arrange(record, RoleName.CALISAN);

        WorkflowRecordNotFoundException exception = assertThrows(
                WorkflowRecordNotFoundException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(WorkflowAction.GONDER, TARGET_ID, null)));

        assertThat(exception.recordId()).isEqualTo(RECORD_ID);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    @Test
    @DisplayName("bulunamayan kayit resolver veya yan etki cagrisi yapmadan reddedilir")
    void rejectsMissingRecordWithoutResolutionOrSideEffects() {
        when(currentActorProvider.currentActor()).thenReturn(new CurrentActor(ACTOR_ID, RoleName.CALISAN, AuthorizationFixtures.workflowActor(RoleName.CALISAN), AuthorizationFixtures.permissions(RoleName.CALISAN)));
        when(recordPort.findById(RECORD_ID)).thenReturn(Optional.empty());

        WorkflowRecordNotFoundException exception = assertThrows(
                WorkflowRecordNotFoundException.class,
                () -> service.performAction(
                        RECORD_ID,
                        new WorkflowActionRequest(WorkflowAction.GONDER, TARGET_ID, null)));

        assertThat(exception.recordId()).isEqualTo(RECORD_ID);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    @Test
    @DisplayName("response alanlari kayit aktor resolver ve sabit Clock uzerinden hesaplanir")
    void calculatesResponseFromBackendStateAndClock() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BSK_YRD_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.BASKANA_ILET, null, null);
        arrange(record, RoleName.BASKAN_YARDIMCISI);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.Resolved(
                        new WorkflowUserSnapshot(TARGET_ID, RoleName.BASKAN, true)));

        WorkflowActionResponse response = service.performAction(RECORD_ID, request);

        assertThat(response.recordId()).isEqualTo(record.id());
        assertThat(response.action()).isEqualTo(request.action());
        assertThat(response.previousStatus()).isEqualTo(record.status());
        assertThat(response.newStatus()).isEqualTo(RecordStatus.BASKAN_INCELEMESINDE);
        assertThat(response.assignedTo()).isEqualTo(TARGET_ID);
        assertThat(response.performedBy()).isEqualTo(ACTOR_ID);
        assertThat(response.performedAt()).isEqualTo(PERFORMED_AT);
    }

    @Test
    @DisplayName("basarili geciste update audit event sirasiyla ve birer kez calisir")
    void performsSuccessfulSideEffectsInOrder() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.ONAYLA, null, null);
        arrange(record, RoleName.BASKAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.NotProvided());

        service.performAction(RECORD_ID, request);

        InOrder sideEffects = inOrder(recordPort, auditService, eventPublisher);
        sideEffects.verify(recordPort).update(any(WorkflowRecordUpdate.class));
        sideEffects.verify(auditService).record(any(WorkflowTransitionAudit.class));
        sideEffects.verify(eventPublisher).publish(any(WorkflowStatusChangedEvent.class));
        sideEffects.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("record update hatasinda audit ve event calismaz")
    void stopsSideEffectsWhenUpdateFails() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.ONAYLA, null, null);
        arrange(record, RoleName.BASKAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.NotProvided());
        doThrow(new IllegalStateException("optimistic lock"))
                .when(recordPort).update(any(WorkflowRecordUpdate.class));

        assertThrows(
                IllegalStateException.class,
                () -> service.performAction(RECORD_ID, request));

        verifyNoInteractions(auditService, eventPublisher);
    }

    @Test
    @DisplayName("surum catismasinda islem durur, audit ve event calismaz")
    void stopsWhenRecordPortReportsVersionConflict() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.ONAYLA, null, null);
        arrange(record, RoleName.BASKAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.NotProvided());
        doThrow(new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_VERSION_CONFLICT))
                .when(recordPort).update(any(WorkflowRecordUpdate.class));

        WorkflowApplicationException thrown = assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, request));

        // Kod sarmalanmadan yukari tasinmali; istemci catismayi ayirt edebilmeli.
        assertThat(thrown.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_VERSION_CONFLICT);

        // Kayit yazilamadiysa denetim izi ve bildirim de hic olusmamali:
        // basarisiz bir gecis gecmiste iz birakmaz.
        verifyNoInteractions(auditService, eventPublisher);
    }

    @Test
    @DisplayName("surum catismasi gecis kurallarindan sonra, yalnizca yazma aninda olusur")
    void versionConflictSurfacesOnlyAtWriteTime() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.ONAYLA, null, null);
        arrange(record, RoleName.BASKAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.NotProvided());
        doThrow(new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_VERSION_CONFLICT))
                .when(recordPort).update(any(WorkflowRecordUpdate.class));

        assertThrows(
                WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, request));

        // Gecis kurallari acisindan istek gecerliydi: dogrulama asamasi
        // gecilmis, hedef cozulmus ve update komutu kaydin okundugu surumle
        // birlikte porta ulasmistir. Catisma yalnizca yazma aninda ortaya cikar.
        // ONAYLA gecisinin stratejisi NONE; servis bunu kuraldan okuyup resolver'a gecirir.
        verify(targetUserResolver).resolve(TargetStrategy.NONE, null, null, record);
        verify(recordPort).update(new WorkflowRecordUpdate(
                RECORD_ID,
                RecordStatus.ONAYLANDI,
                null,
                LAST_DEPUTY_ID,
                record.version(),
                PERFORMED_AT));
    }

    @Test
    @DisplayName("audit hatasinda event yayinlanmaz")
    void doesNotPublishEventWhenAuditFails() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.ONAYLA, null, null);
        arrange(record, RoleName.BASKAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.NotProvided());
        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditService).record(any(WorkflowTransitionAudit.class));

        assertThrows(
                IllegalStateException.class,
                () -> service.performAction(RECORD_ID, request));

        verify(recordPort).update(any(WorkflowRecordUpdate.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("event hatasi propagate edilir ve onceki yan etkiler birer kez calismistir")
    void propagatesEventFailureAfterUpdateAndAudit() {
        WorkflowRecordSnapshot record = activeRecord(
                RecordStatus.BASKAN_INCELEMESINDE, CREATOR_ID, ACTOR_ID, LAST_DEPUTY_ID);
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.ONAYLA, null, null);
        arrange(record, RoleName.BASKAN);
        when(targetUserResolver.resolve(any(), any(), eq(null), eq(record)))
                .thenReturn(new TargetResolution.NotProvided());
        doThrow(new IllegalStateException("event unavailable"))
                .when(eventPublisher).publish(any(WorkflowStatusChangedEvent.class));

        assertThrows(
                IllegalStateException.class,
                () -> service.performAction(RECORD_ID, request));

        verify(recordPort).update(any(WorkflowRecordUpdate.class));
        verify(auditService).record(any(WorkflowTransitionAudit.class));
        verify(eventPublisher).publish(any(WorkflowStatusChangedEvent.class));
    }

    @Test
    void missingPermissionStopsBeforeTargetLookupAndAllWrites() {
        when(currentActorProvider.currentActor()).thenReturn(
                new CurrentActor(ACTOR_ID, RoleName.CALISAN, true, java.util.Set.of()));
        when(recordPort.findById(RECORD_ID)).thenReturn(Optional.of(
                activeRecord(RecordStatus.TASLAK, ACTOR_ID, null, null)));
        WorkflowApplicationException exception = assertThrows(WorkflowApplicationException.class,
                () -> service.performAction(RECORD_ID, new WorkflowActionRequest(WorkflowAction.GONDER, null, null)));
        assertThat(exception.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        verifyNoInteractions(targetUserResolver);
        assertNoMutation();
    }

    private void arrange(WorkflowRecordSnapshot record, RoleName actorRole) {
        when(currentActorProvider.currentActor()).thenReturn(new CurrentActor(ACTOR_ID, actorRole, AuthorizationFixtures.workflowActor(actorRole), AuthorizationFixtures.permissions(actorRole)));
        when(recordPort.findById(RECORD_ID)).thenReturn(Optional.of(record));
    }

    private void assertNoMutation() {
        verify(recordPort, never()).update(any());
        verifyNoInteractions(auditService, eventPublisher);
    }

    private static WorkflowRecordSnapshot activeRecord(
            RecordStatus status,
            UUID createdBy,
            UUID assignedTo,
            UUID lastDeputyId) {
        return new WorkflowRecordSnapshot(
                RECORD_ID,
                status,
                createdBy,
                assignedTo,
                lastDeputyId,
                null,
                7);
    }

    private static Stream<AllowedCase> allowedTransitions() {
        return Stream.of(
                new AllowedCase(
                        "Calisan taslagi Baskan Yardimcisina gonderir",
                        RecordStatus.TASLAK,
                        WorkflowAction.GONDER,
                        RoleName.CALISAN,
                        ACTOR_ID,
                        null,
                        LAST_DEPUTY_ID,
                        null,
                        null,
                        new TargetResolution.Resolved(
                                new WorkflowUserSnapshot(TARGET_ID, RoleName.BASKAN_YARDIMCISI, true)),
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        TARGET_ID,
                        LAST_DEPUTY_ID),
                new AllowedCase(
                        "Calisan duzenlenen kaydi yeniden gonderir",
                        RecordStatus.DUZENLEME_BEKLIYOR,
                        WorkflowAction.TEKRAR_GONDER,
                        RoleName.CALISAN,
                        ACTOR_ID,
                        ACTOR_ID,
                        LAST_DEPUTY_ID,
                        null,
                        null,
                        new TargetResolution.Resolved(
                                new WorkflowUserSnapshot(TARGET_ID, RoleName.BASKAN_YARDIMCISI, true)),
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        TARGET_ID,
                        LAST_DEPUTY_ID),
                new AllowedCase(
                        "Baskan Yardimcisi Baskana iletir",
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        WorkflowAction.BASKANA_ILET,
                        RoleName.BASKAN_YARDIMCISI,
                        CREATOR_ID,
                        ACTOR_ID,
                        LAST_DEPUTY_ID,
                        null,
                        null,
                        new TargetResolution.Resolved(
                                new WorkflowUserSnapshot(TARGET_ID, RoleName.BASKAN, true)),
                        RecordStatus.BASKAN_INCELEMESINDE,
                        TARGET_ID,
                        ACTOR_ID),
                new AllowedCase(
                        "Baskan Yardimcisi Calisana geri gonderir",
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        WorkflowAction.CALISANA_GERI_GONDER,
                        RoleName.BASKAN_YARDIMCISI,
                        CREATOR_ID,
                        ACTOR_ID,
                        LAST_DEPUTY_ID,
                        null,
                        "duzeltiniz",
                        new TargetResolution.Resolved(
                                new WorkflowUserSnapshot(CREATOR_ID, RoleName.CALISAN, true)),
                        RecordStatus.DUZENLEME_BEKLIYOR,
                        CREATOR_ID,
                        LAST_DEPUTY_ID),
                new AllowedCase(
                        "Baskan onaylar ve atamayi temizler",
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.ONAYLA,
                        RoleName.BASKAN,
                        CREATOR_ID,
                        ACTOR_ID,
                        LAST_DEPUTY_ID,
                        null,
                        null,
                        new TargetResolution.NotProvided(),
                        RecordStatus.ONAYLANDI,
                        null,
                        LAST_DEPUTY_ID),
                new AllowedCase(
                        "Baskan reddeder ve atamayi temizler",
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.REDDET,
                        RoleName.BASKAN,
                        CREATOR_ID,
                        ACTOR_ID,
                        LAST_DEPUTY_ID,
                        null,
                        "uygun degil",
                        new TargetResolution.NotProvided(),
                        RecordStatus.REDDEDILDI,
                        null,
                        LAST_DEPUTY_ID),
                new AllowedCase(
                        "Baskan Calisana geri gonderir",
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.CALISANA_GERI_GONDER,
                        RoleName.BASKAN,
                        CREATOR_ID,
                        ACTOR_ID,
                        LAST_DEPUTY_ID,
                        null,
                        "duzeltiniz",
                        new TargetResolution.Resolved(
                                new WorkflowUserSnapshot(CREATOR_ID, RoleName.CALISAN, true)),
                        RecordStatus.DUZENLEME_BEKLIYOR,
                        CREATOR_ID,
                        LAST_DEPUTY_ID),
                new AllowedCase(
                        "Baskan kaydi son Baskan Yardimcisina geri gonderir",
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,
                        RoleName.BASKAN,
                        CREATOR_ID,
                        ACTOR_ID,
                        LAST_DEPUTY_ID,
                        null,
                        "tekrar inceleyiniz",
                        new TargetResolution.Resolved(
                                new WorkflowUserSnapshot(LAST_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true)),
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        LAST_DEPUTY_ID,
                        LAST_DEPUTY_ID));
    }

    private static Stream<ForbiddenTargetCase> actionsThatRejectRequestTarget() {
        return Stream.of(
                new ForbiddenTargetCase(
                        "Gonder request hedefini reddeder",
                        RecordStatus.TASLAK,
                        WorkflowAction.GONDER,
                        RoleName.CALISAN,
                        ACTOR_ID,
                        null,
                        null),
                new ForbiddenTargetCase(
                        "Tekrar gonder request hedefini reddeder",
                        RecordStatus.DUZENLEME_BEKLIYOR,
                        WorkflowAction.TEKRAR_GONDER,
                        RoleName.CALISAN,
                        ACTOR_ID,
                        ACTOR_ID,
                        null),
                new ForbiddenTargetCase(
                        "Baskana ilet request hedefini reddeder",
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        WorkflowAction.BASKANA_ILET,
                        RoleName.BASKAN_YARDIMCISI,
                        CREATOR_ID,
                        ACTOR_ID,
                        null),
                new ForbiddenTargetCase(
                        "Calisana geri gonder request hedefini reddeder",
                        RecordStatus.BSK_YRD_INCELEMESINDE,
                        WorkflowAction.CALISANA_GERI_GONDER,
                        RoleName.BASKAN_YARDIMCISI,
                        CREATOR_ID,
                        ACTOR_ID,
                        "duzeltiniz"),
                new ForbiddenTargetCase(
                        "Baskan Yardimcisina geri gonder request hedefini reddeder",
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,
                        RoleName.BASKAN,
                        CREATOR_ID,
                        ACTOR_ID,
                        "tekrar inceleyiniz"),
                new ForbiddenTargetCase(
                        "Onay request hedefini reddeder",
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.ONAYLA,
                        RoleName.BASKAN,
                        CREATOR_ID,
                        ACTOR_ID,
                        null),
                new ForbiddenTargetCase(
                        "Ret request hedefini reddeder",
                        RecordStatus.BASKAN_INCELEMESINDE,
                        WorkflowAction.REDDET,
                        RoleName.BASKAN,
                        CREATOR_ID,
                        ACTOR_ID,
                        "uygun degil"));
    }

    private record ForbiddenTargetCase(
            String name,
            RecordStatus status,
            WorkflowAction action,
            RoleName actorRole,
            UUID createdBy,
            UUID assignedTo,
            String comment) {

        @Override
        public String toString() {
            return name;
        }
    }

    private record AllowedCase(
            String name,
            RecordStatus previousStatus,
            WorkflowAction action,
            RoleName actorRole,
            UUID createdBy,
            UUID previousAssignedTo,
            UUID previousLastDeputyId,
            UUID requestedTargetId,
            String comment,
            TargetResolution resolution,
            RecordStatus newStatus,
            UUID expectedAssignedTo,
            UUID expectedLastDeputyId) {

        WorkflowRecordSnapshot record() {
            return new WorkflowRecordSnapshot(
                    RECORD_ID,
                    previousStatus,
                    createdBy,
                    previousAssignedTo,
                    previousLastDeputyId,
                    null,
                    version());
        }

        WorkflowActionRequest request() {
            return new WorkflowActionRequest(action, requestedTargetId, comment);
        }

        int version() {
            return 7;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
