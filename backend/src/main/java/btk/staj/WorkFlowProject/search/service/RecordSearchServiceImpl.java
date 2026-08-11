package btk.staj.WorkFlowProject.search.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
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

    public RecordSearchServiceImpl(RecordRepository recordRepository,
                                   CurrentActorProvider currentActorProvider) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
        this.currentActorProvider = Objects.requireNonNull(
                currentActorProvider, "currentActorProvider");
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
                recordPage.getContent().stream().map(RecordSearchServiceImpl::toResponse).toList(),
                recordPage.getNumber(),
                recordPage.getSize(),
                recordPage.getTotalElements(),
                recordPage.getTotalPages());
    }

    private static RecordSearchResponse toResponse(Record record) {
        RecordSearchResponse response = new RecordSearchResponse();

        response.setId(record.getId());
        response.setTitle(record.getTitle());
        response.setDescription(record.getDescription());
        response.setCategoryId(record.getCategoryId());
        response.setStatus(record.getStatus());
        response.setCreatedBy(record.getCreatedBy());
        response.setAssignedTo(record.getAssignedTo());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        return response;
    }
}
