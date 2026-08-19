package btk.staj.WorkFlowProject.search.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RecordSearchServiceImpl implements RecordSearchService {

    private final RecordRepository recordRepository;
    private final CurrentActorProvider currentActorProvider;
    private final RecordContentView recordContentView;

    public RecordSearchServiceImpl(RecordRepository recordRepository,
                                   CurrentActorProvider currentActorProvider,
                                   RecordContentView recordContentView) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
        this.currentActorProvider = Objects.requireNonNull(
                currentActorProvider, "currentActorProvider");
        this.recordContentView = Objects.requireNonNull(recordContentView, "recordContentView");
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

        return new PagedResponse<>(
                recordPage.getContent().stream()
                        .map(record -> toResponse(record, actor))
                        .toList(),
                recordPage.getNumber(),
                recordPage.getSize(),
                recordPage.getTotalElements(),
                recordPage.getTotalPages());
    }

    /**
     * Liste de detayla ayni icerik kuralina uyar. Yalnizca detay dondurulsaydi
     * kaydi elinden cikarmis yardimci, guncellenen basligi listede gormeye
     * devam ederdi.
     */
    private RecordSearchResponse toResponse(Record record, CurrentActor actor) {
        RecordContentView.Content content =
                recordContentView.visibleContent(record, actor.role(), actor.id());

        RecordSearchResponse response = new RecordSearchResponse();

        response.setId(record.getId());
        response.setTitle(content.title());
        response.setDescription(content.description());
        response.setCategoryId(content.categoryId());
        response.setStatus(record.getStatus());
        response.setCreatedBy(record.getCreatedBy());
        response.setAssignedTo(record.getAssignedTo());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        return response;
    }
}
