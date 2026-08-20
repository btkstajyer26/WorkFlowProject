package btk.staj.WorkFlowProject.search.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecordSearchServiceImpl implements RecordSearchService {

    private final RecordRepository recordRepository;
    private final CurrentActorProvider currentActorProvider;
    private final RecordContentView recordContentView;
    private final UserRepository userRepository;

    public RecordSearchServiceImpl(RecordRepository recordRepository,
                                   CurrentActorProvider currentActorProvider,
                                   RecordContentView recordContentView,
                                   UserRepository userRepository) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
        this.currentActorProvider = Objects.requireNonNull(
                currentActorProvider, "currentActorProvider");
        this.recordContentView = Objects.requireNonNull(recordContentView, "recordContentView");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    /**
     * Yetki kapsami sorgunun icinde uygulanir: kullanici gormeye yetkili
     * olmadigi kaydi sonucta hic gormez, sayfa sayilarinda da yer almaz.
     *
     * <p>Kullanici kimligi yalnizca dogrulanmis oturumdan gelir. Kimlik
     * okunamiyorsa arama calismaz; varsayilan bir kullaniciya dusmek, kapsami
     * baskasinin kayitlarina acardi.
     */
    @Override
    public PagedResponse<RecordSearchResponse> search(
            RecordSearchCriteria criteria,
            Pageable pageable) {

        CurrentActor actor = currentActorProvider.currentActor();

        Page<Record> recordPage = recordRepository.findAll(
                RecordSpecifications.withFilters(criteria, actor.id(), actor.role()),
                pageable);

        Map<UUID, String> creatorNames = creatorNamesOf(recordPage.getContent());

        return new PagedResponse<>(
                recordPage.getContent().stream()
                        .map(record -> toResponse(record, actor, creatorNames))
                        .toList(),
                recordPage.getNumber(),
                recordPage.getSize(),
                recordPage.getTotalElements(),
                recordPage.getTotalPages());
    }

    /**
     * Sayfadaki kayitlarin olusturanlarini tek sorguda cozer. Kayit basina
     * arama yapmak N+1 olurdu; ayni kisi genelde birden cok kaydin sahibidir.
     */
    private Map<UUID, String> creatorNamesOf(List<Record> records) {
        Set<UUID> creatorIds = records.stream()
                .map(Record::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (creatorIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, RecordSearchServiceImpl::fullName));
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    /**
     * Liste de detayla ayni icerik kuralina uyar. Yalnizca detay dondurulsaydi
     * kaydi elinden cikarmis yardimci, guncellenen basligi listede gormeye
     * devam ederdi.
     */
    private RecordSearchResponse toResponse(Record record,
                                            CurrentActor actor,
                                            Map<UUID, String> creatorNames) {
        RecordContentView.Content content =
                recordContentView.visibleContent(record, actor.role(), actor.id());

        RecordSearchResponse response = new RecordSearchResponse();

        response.setId(record.getId());
        response.setTitle(content.title());
        response.setDescription(content.description());
        response.setCategoryId(content.categoryId());
        response.setStatus(record.getStatus());
        response.setCreatedBy(record.getCreatedBy());
        response.setCreatedByFullName(creatorNames.get(record.getCreatedBy()));
        response.setAssignedTo(record.getAssignedTo());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        return response;
    }
}
