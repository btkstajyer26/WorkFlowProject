package btk.staj.WorkFlowProject.audit.controller;



import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Islem gecmisi gorunurluk kapsami")
class AuditLogControllerTest {

    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID DEPUTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000023");

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final RecordRepository recordRepository = mock(RecordRepository.class);
    private final CurrentVisibilityActorProvider currentVisibilityActorProvider = mock(CurrentVisibilityActorProvider.class);
    private final AuditLogController controller = new AuditLogController(
            auditLogService, recordRepository, new RecordAccessPolicy(), currentVisibilityActorProvider);

    @Test
    @DisplayName("kaydin sahibi kendi evraginin gecmisini gorur")
    void theOwnerCanReadTheHistoryOfTheirOwnRecord() {
        givenActor(OWNER_ID, RoleName.CALISAN);
        givenRecord(OWNER_ID, DEPUTY_ID, RecordStatus.BSK_YRD_INCELEMESINDE);
        when(auditLogService.getGecmis(RECORD_ID)).thenReturn(List.of(row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(1);
    }

    @Test
    @DisplayName("baska bir Calisan gecmisi goremez")
    void anotherEmployeeIsRejected() {
        givenActor(OTHER_EMPLOYEE_ID, RoleName.CALISAN);
        givenRecord(OWNER_ID, DEPUTY_ID, RecordStatus.BSK_YRD_INCELEMESINDE);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> controller.getGecmis(RECORD_ID));

        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("kendisine atanmamis kaydi Bsk. Yrd. de goremez")
    void aDeputyWithoutTheAssignmentIsRejected() {
        givenActor(DEPUTY_ID, RoleName.BASKAN_YARDIMCISI);
        givenRecord(OWNER_ID, null, RecordStatus.TASLAK);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> controller.getGecmis(RECORD_ID));

        verifyNoInteractions(auditLogService);
    }

    /**
     * Baskan kaydi gorur ama gecmisin tamamini gormez: evrak kendisine
     * ulasmadan onceki Calisan-Bsk. Yrd. trafigi ona kapali.
     */
    @Test
    @DisplayName("onay asamasindaki kaydin gecmisini Baskan iletimden itibaren gorur")
    void thePresidentCanReadARecordAwaitingApproval() {
        givenActor(UUID.randomUUID(), RoleName.BASKAN);
        givenRecord(OWNER_ID, DEPUTY_ID, RecordStatus.BASKAN_INCELEMESINDE);
        when(auditLogService.getGecmisIletimdenItibaren(RECORD_ID)).thenReturn(List.of(row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(1);

        // Kirpilmamis gecmis hic istenmemeli.
        verify(auditLogService, never()).getGecmis(RECORD_ID);
    }

    @Test
    @DisplayName("sonuclandirdigi kaydin gecmisini de Baskan iletimden itibaren gorur")
    void thePresidentStillGetsTheTrimmedHistoryAfterDeciding() {
        givenActor(UUID.randomUUID(), RoleName.BASKAN);
        givenRecord(OWNER_ID, null, RecordStatus.REDDEDILDI);
        when(auditLogService.getGecmisIletimdenItibaren(RECORD_ID)).thenReturn(List.of(row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(1);

        verify(auditLogService, never()).getGecmis(RECORD_ID);
    }

    /**
     * ONAYLA/REDDET assignedTo'yu bosaltir; kapsam durum uzerinden acik
     * yazilmasaydi Baskan kendi verdigi karardan sonra kaydi kaybederdi.
     */
    @Test
    @DisplayName("reddettigi kaydin gecmisine Baskan karardan sonra da erisir")
    void thePresidentKeepsAccessAfterRejecting() {
        givenActor(UUID.randomUUID(), RoleName.BASKAN);
        givenRecord(OWNER_ID, null, RecordStatus.REDDEDILDI);

        assertThat(controller.getGecmis(RECORD_ID)).isNotNull();
    }

    /**
     * Kayit Baskana iletilince assignedTo Baskana gecer; yardimci onu yalnizca
     * lastDeputyId sayesinde izlemeye devam eder.
     */
    @Test
    @DisplayName("Baskana ilettigi kaydin gecmisini Bsk. Yrd. gormeye devam eder")
    void theDeputyKeepsReadingTheHistoryOfARecordTheyForwarded() {
        givenActor(DEPUTY_ID, RoleName.BASKAN_YARDIMCISI);
        givenRecord(OWNER_ID, UUID.randomUUID(), DEPUTY_ID, RecordStatus.BASKAN_INCELEMESINDE);
        when(auditLogService.getGecmis(RECORD_ID)).thenReturn(List.of(row(), row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(2);
    }

    @Test
    @DisplayName("olmayan kayit icin 404 anlamli hata doner")
    void aMissingRecordIsReportedAsNotFound() {
        givenActor(OWNER_ID, RoleName.CALISAN);
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> controller.getGecmis(RECORD_ID));

        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("geri gonderdigi kaydin gecmisini Bsk. Yrd. yalnizca devre kadar gorur")
    void theDeputyOnlySeesTheHistoryUpToTheHandoff() {
        givenActor(DEPUTY_ID, RoleName.BASKAN_YARDIMCISI);
        givenRecord(OWNER_ID, OWNER_ID, RecordStatus.DUZENLEME_BEKLIYOR);
        when(auditLogService.getGecmisDevreKadar(RECORD_ID)).thenReturn(List.of(row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(1);

        // Kirpilmamis gecmis hic istenmemeli.
        verify(auditLogService, never()).getGecmis(RECORD_ID);
    }

    @Test
    @DisplayName("kayit kendisine geri atandiginda Bsk. Yrd. gecmisin tamamini gorur")
    void theDeputySeesTheWholeHistoryOnceTheRecordComesBack() {
        givenActor(DEPUTY_ID, RoleName.BASKAN_YARDIMCISI);
        givenRecord(OWNER_ID, DEPUTY_ID, RecordStatus.BSK_YRD_INCELEMESINDE);
        when(auditLogService.getGecmis(RECORD_ID)).thenReturn(List.of(row(), row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(2);

        verify(auditLogService, never()).getGecmisDevreKadar(RECORD_ID);
    }

    @Test
    @DisplayName("kaydin sahibi Calisan duzeltme sirasinda kendi gecmisini eksiksiz gorur")
    void theOwnerKeepsTheWholeHistoryWhileCorrecting() {
        givenActor(OWNER_ID, RoleName.CALISAN);
        givenRecord(OWNER_ID, OWNER_ID, RecordStatus.DUZENLEME_BEKLIYOR);
        when(auditLogService.getGecmis(RECORD_ID)).thenReturn(List.of(row(), row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(2);

        verify(auditLogService, never()).getGecmisDevreKadar(RECORD_ID);
    }

    private void givenActor(UUID userId, RoleName role) {
        when(currentVisibilityActorProvider.currentVisibilityActor()).thenReturn(new VisibilityActor(userId, role));
    }

    private void givenRecord(UUID createdBy, UUID assignedTo, RecordStatus status) {
        givenRecord(createdBy, assignedTo, null, status);
    }

    private void givenRecord(UUID createdBy, UUID assignedTo, UUID lastDeputyId, RecordStatus status) {
        Record record = new Record();
        record.setId(RECORD_ID);
        record.setCreatedBy(createdBy);
        record.setAssignedTo(assignedTo);
        record.setLastDeputyId(lastDeputyId);
        record.setStatus(status);
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
    }

    private static AuditLogResponse row() {
        return new AuditLogResponse(
                UUID.randomUUID(), RECORD_ID, OWNER_ID, "Ahmet Yılmaz", 1, "CALISAN",
                "GONDER", "TASLAK", "BSK_YRD_INCELEMESINDE",
                "Onayınıza sunulmuştur.", null, null, null, null, LocalDateTime.now());
    }
}
