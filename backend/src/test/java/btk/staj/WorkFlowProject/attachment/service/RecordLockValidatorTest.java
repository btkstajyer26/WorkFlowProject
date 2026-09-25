package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordLockValidator")
class RecordLockValidatorTest {

    @Mock
    private RecordRepository recordRepository;

    private RecordLockValidator validator() {
        return new RecordLockValidator(recordRepository);
    }

    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    private Record kayit(RecordStatus status) {
        Record record = new Record();
        record.setId(RECORD_ID);
        record.setCreatedBy(OWNER_ID);
        record.setStatus(status);
        return record;
    }

    @Test
    @DisplayName("kayıt yoksa ResourceNotFoundException")
    void kayitYoksaHataFirlatir() {
        when(recordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator().assertModifyAllowed(RECORD_ID, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("silinmiş kayda değişiklik BusinessRuleException")
    void silinmisKayitReddedilir() {
        Record record = kayit(RecordStatus.TASLAK);
        record.setDeletedAt(LocalDateTime.now());
        when(recordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> validator().assertModifyAllowed(RECORD_ID, OWNER_ID))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("sahibi olmayan kullanıcı ForbiddenException alır")
    void sahibiOlmayanReddedilir() {
        when(recordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(kayit(RecordStatus.TASLAK)));

        assertThatThrownBy(() -> validator().assertModifyAllowed(RECORD_ID, OTHER_USER_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("BSK_YRD_INCELEMESINDE durumunda değişiklik BusinessRuleException")
    void yanlisDurumdaReddedilir() {
        when(recordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(kayit(RecordStatus.BSK_YRD_INCELEMESINDE)));

        assertThatThrownBy(() -> validator().assertModifyAllowed(RECORD_ID, OWNER_ID))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("TASLAK durumunda sahibi değişiklik yapabilir")
    void taslakDurumundaSahibiIzinli() {
        when(recordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(kayit(RecordStatus.TASLAK)));

        assertThatCode(() -> validator().assertModifyAllowed(RECORD_ID, OWNER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DUZENLEME_BEKLIYOR durumunda sahibi değişiklik yapabilir")
    void duzenlemeBekliyorDurumundaSahibiIzinli() {
        when(recordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(kayit(RecordStatus.DUZENLEME_BEKLIYOR)));

        assertThatCode(() -> validator().assertModifyAllowed(RECORD_ID, OWNER_ID))
                .doesNotThrowAnyException();
    }

    /**
     * B04: dogrulama kilitli satir uzerinden yapilmali ve kilitli kayit
     * cagirana donmeli. Kilitsiz {@code findById} ile yapilan okuma, kontrol
     * ile dosya satirinin yazilmasi arasinda durumun degismesine izin veriyordu.
     */
    @Test
    @DisplayName("kayıt satır kilidiyle yüklenir ve çağırana döner")
    void kayitKilitliYuklenipDondurulur() {
        Record record = kayit(RecordStatus.TASLAK);
        when(recordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));

        Record locked = validator().assertModifyAllowed(RECORD_ID, OWNER_ID);

        assertThat(locked).isSameAs(record);
        verify(recordRepository).findByIdForUpdate(RECORD_ID);
        verify(recordRepository, never()).findById(RECORD_ID);
    }
}