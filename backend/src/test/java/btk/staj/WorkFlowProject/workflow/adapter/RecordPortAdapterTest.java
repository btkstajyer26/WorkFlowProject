package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowRecordNotFoundException;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Kayit port adaptoru")
class RecordPortAdapterTest {

    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID CREATED_BY = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID ASSIGNED_TO = UUID.fromString("00000000-0000-0000-0000-000000000032");
    private static final UUID LAST_DEPUTY = UUID.fromString("00000000-0000-0000-0000-000000000033");

    private final RecordRepository recordRepository = mock(RecordRepository.class);
    private final RecordPortAdapter adapter = new RecordPortAdapter(recordRepository);

    @Test
    @DisplayName("kaydi cekirdegin bekledigi goruntuye cevirir")
    void mapsARecordOntoTheSnapshot() {
        when(recordRepository.findById(RECORD_ID))
                .thenReturn(Optional.of(record(RecordStatus.BASKAN_INCELEMESINDE, null)));

        WorkflowRecordSnapshot snapshot = adapter.findById(RECORD_ID).orElseThrow();

        assertThat(snapshot.id()).isEqualTo(RECORD_ID);
        assertThat(snapshot.status()).isEqualTo(RecordStatus.BASKAN_INCELEMESINDE);
        assertThat(snapshot.createdBy()).isEqualTo(CREATED_BY);
        assertThat(snapshot.assignedTo()).isEqualTo(ASSIGNED_TO);
        assertThat(snapshot.lastDeputyId()).isEqualTo(LAST_DEPUTY);
        assertThat(snapshot.version()).isZero();
        assertThat(snapshot.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("silinmis kaydi silinmis olarak isaretler")
    void marksASoftDeletedRecordAsDeleted() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 11, 10, 0);
        when(recordRepository.findById(RECORD_ID))
                .thenReturn(Optional.of(record(RecordStatus.TASLAK, deletedAt)));

        WorkflowRecordSnapshot snapshot = adapter.findById(RECORD_ID).orElseThrow();

        assertThat(snapshot.isDeleted()).isTrue();
        assertThat(snapshot.deletedAt())
                .isEqualTo(deletedAt.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("olmayan kayit icin bos doner")
    void returnsEmptyForAMissingRecord() {
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(RECORD_ID)).isEmpty();
    }

    @Test
    @DisplayName("gecis sonucunu kayda yazar")
    void appliesTheTransitionToTheRecord() {
        Record stored = record(RecordStatus.BSK_YRD_INCELEMESINDE, null);
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.of(stored));

        adapter.update(new WorkflowRecordUpdate(
                RECORD_ID, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNED_TO, LAST_DEPUTY,
                0, Instant.parse("2026-08-11T09:15:00Z")));

        ArgumentCaptor<Record> captor = ArgumentCaptor.forClass(Record.class);
        verify(recordRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RecordStatus.BASKAN_INCELEMESINDE);
        assertThat(captor.getValue().getAssignedTo()).isEqualTo(ASSIGNED_TO);
        assertThat(captor.getValue().getLastDeputyId()).isEqualTo(LAST_DEPUTY);
    }

    @Test
    @DisplayName("arada baskasi guncellediyse ustune yazmaz")
    void refusesToOverwriteAConcurrentlyChangedRecord() {
        Record stored = record(RecordStatus.BSK_YRD_INCELEMESINDE, null);
        stored.setVersion(4);
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.of(stored));

        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> adapter.update(new WorkflowRecordUpdate(
                        RECORD_ID, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNED_TO, LAST_DEPUTY,
                        0, Instant.parse("2026-08-11T09:15:00Z"))));

        verify(recordRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("olmayan kaydi guncellemeye calisirsa hata verir")
    void failsWhenUpdatingAMissingRecord() {
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(WorkflowRecordNotFoundException.class)
                .isThrownBy(() -> adapter.update(new WorkflowRecordUpdate(
                        RECORD_ID, RecordStatus.ONAYLANDI, null, null,
                        0, Instant.parse("2026-08-11T09:15:00Z"))));
    }

    @Test
    @DisplayName("null girdileri reddeder")
    void rejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> adapter.findById(null));
        assertThatNullPointerException().isThrownBy(() -> adapter.update(null));
    }

    private static Record record(RecordStatus status, LocalDateTime deletedAt) {
        Record record = new Record();
        record.setId(RECORD_ID);
        record.setStatus(status);
        record.setCreatedBy(CREATED_BY);
        record.setAssignedTo(ASSIGNED_TO);
        record.setLastDeputyId(LAST_DEPUTY);
        record.setVersion(0);
        record.setDeletedAt(deletedAt);
        return record;
    }
}
