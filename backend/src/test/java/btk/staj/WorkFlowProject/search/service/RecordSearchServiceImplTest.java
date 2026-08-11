package btk.staj.WorkFlowProject.search.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Kayit aramasi")
class RecordSearchServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");

    private final RecordRepository recordRepository = mock(RecordRepository.class);
    private final CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
    private final RecordSearchServiceImpl service =
            new RecordSearchServiceImpl(recordRepository, currentActorProvider);

    @Test
    @DisplayName("sayfalama bilgisini oldugu gibi aktarir")
    void passesThroughThePagingMetadata() {
        givenActor(RoleName.CALISAN);
        Pageable pageable = PageRequest.of(0, 10);
        when(recordRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record()), pageable, 25));

        PagedResponse<RecordSearchResponse> result =
                service.search(new RecordSearchCriteria(), pageable);

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent()).singleElement()
                .satisfies(row -> assertThat(row.getTitle()).isEqualTo("Bütçe talebi"));
    }

    @Test
    @DisplayName("kimlik dogrulanamiyorsa arama yapilmaz")
    void doesNotSearchWithoutAnAuthenticatedActor() {
        when(currentActorProvider.currentActor())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"));

        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
                .isThrownBy(() -> service.search(new RecordSearchCriteria(), PageRequest.of(0, 10)));

        // Kimlik cozulemedigi halde sorgu calissaydi, kapsam disi kayitlar donerdi.
        verifyNoInteractions(recordRepository);
    }

    private void givenActor(RoleName role) {
        when(currentActorProvider.currentActor()).thenReturn(new CurrentActor(USER_ID, role));
    }

    private static Record record() {
        Record record = new Record();
        record.setId(UUID.randomUUID());
        record.setTitle("Bütçe talebi");
        record.setDescription("Açıklama");
        record.setCategoryId(1);
        record.setStatus(RecordStatus.TASLAK);
        record.setCreatedBy(USER_ID);
        return record;
    }
}
