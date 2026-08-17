package btk.staj.WorkFlowProject.audit.controller;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
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
    private final CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
    private final AuditLogController controller = new AuditLogController(
            auditLogService, recordRepository, new RecordAccessPolicy(), currentActorProvider);

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

    @Test
    @DisplayName("onay asamasindaki kaydin gecmisini Baskan gorur")
    void thePresidentCanReadARecordAwaitingApproval() {
        givenActor(UUID.randomUUID(), RoleName.BASKAN);
        givenRecord(OWNER_ID, DEPUTY_ID, RecordStatus.BASKAN_INCELEMESINDE);
        when(auditLogService.getGecmis(RECORD_ID)).thenReturn(List.of(row()));

        assertThat(controller.getGecmis(RECORD_ID)).hasSize(1);
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

    private void givenActor(UUID userId, RoleName role) {
        when(currentActorProvider.currentActor()).thenReturn(new CurrentActor(userId, role));
    }

    private void givenRecord(UUID createdBy, UUID assignedTo, RecordStatus status) {
        Record record = new Record();
        record.setId(RECORD_ID);
        record.setCreatedBy(createdBy);
        record.setAssignedTo(assignedTo);
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
