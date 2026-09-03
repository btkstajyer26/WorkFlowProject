package btk.staj.WorkFlowProject.search.service;



import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Kayit aramasi")
class RecordSearchServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");

    private final RecordRepository recordRepository = mock(RecordRepository.class);
    private final CurrentVisibilityActorProvider currentVisibilityActorProvider = mock(CurrentVisibilityActorProvider.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RecordSearchServiceImpl service =
            new RecordSearchServiceImpl(recordRepository, currentVisibilityActorProvider,
                    new RecordContentView(new RecordAccessPolicy()), userRepository);

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
        when(currentVisibilityActorProvider.currentVisibilityActor())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"));

        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
                .isThrownBy(() -> service.search(new RecordSearchCriteria(), PageRequest.of(0, 10)));

        // Kimlik cozulemedigi halde sorgu calissaydi, kapsam disi kayitlar donerdi.
        verifyNoInteractions(recordRepository);
    }

    /**
     * Ad cevaba konmasaydi istemci onu denetim izinden turetmek zorunda
     * kalirdi; gecmisi kirpilan roller (Baskan) olusturma satirini gormedigi
     * icin o yol yanlis kisiyi gosteriyordu.
     */
    @Test
    @DisplayName("olusturanin adini cevaba koyar")
    void resolvesTheCreatorName() {
        givenActor(RoleName.CALISAN);
        Pageable pageable = PageRequest.of(0, 10);
        when(recordRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record()), pageable, 1));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(USER_ID, "Ahmet", "Yılmaz")));

        PagedResponse<RecordSearchResponse> result =
                service.search(new RecordSearchCriteria(), pageable);

        assertThat(result.getContent()).singleElement()
                .satisfies(row -> assertThat(row.getCreatedByFullName()).isEqualTo("Ahmet Yılmaz"));
    }

    @Test
    @DisplayName("ayni sayfadaki olusturanlar tek sorguda cozulur")
    void resolvesAllCreatorsInASingleQuery() {
        givenActor(RoleName.CALISAN);
        Pageable pageable = PageRequest.of(0, 10);
        when(recordRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record(), record(), record()), pageable, 3));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(USER_ID, "Ahmet", "Yılmaz")));

        service.search(new RecordSearchCriteria(), pageable);

        // Kayit basina arama N+1 olurdu.
        verify(userRepository, times(1)).findAllById(any());
    }

    @Test
    @DisplayName("olusturan kullanici bulunamazsa ad bos kalir, cevap bozulmaz")
    void leavesTheNameEmptyWhenTheCreatorIsGone() {
        givenActor(RoleName.CALISAN);
        Pageable pageable = PageRequest.of(0, 10);
        when(recordRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record()), pageable, 1));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        PagedResponse<RecordSearchResponse> result =
                service.search(new RecordSearchCriteria(), pageable);

        assertThat(result.getContent()).singleElement().satisfies(row -> {
            assertThat(row.getCreatedByFullName()).isNull();
            assertThat(row.getCreatedBy()).isEqualTo(USER_ID);
        });
    }

    private static User user(UUID id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private void givenActor(RoleName role) {
        when(currentVisibilityActorProvider.currentVisibilityActor()).thenReturn(new VisibilityActor(USER_ID, role));
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
