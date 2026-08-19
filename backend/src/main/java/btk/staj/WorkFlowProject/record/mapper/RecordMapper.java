package btk.staj.WorkFlowProject.record.mapper;

import btk.staj.WorkFlowProject.record.dto.RecordCreateRequest;
import btk.staj.WorkFlowProject.record.dto.RecordResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RecordMapper {

    public Record toEntity(RecordCreateRequest request, UUID createdBy) {
        return Record.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .status(RecordStatus.TASLAK)
                .createdBy(createdBy)
                .build();
    }

    /** Kaydin guncel halini doner; icerigi kendi goren kullanicilar icin. */
    public RecordResponse toResponse(Record record) {
        return toResponse(record, RecordContentView.live(record));
    }

    /**
     * Icerigi disaridan verilen haliyle doner. Kaydi elinden cikarmis Baskan
     * Yardimcisina devir anindaki kopya gosterilir; kalan alanlar (durum,
     * olusturulma zamani) her zaman guncel kaydindan okunur.
     */
    public RecordResponse toResponse(Record record, RecordContentView.Content content) {
        RecordResponse response = new RecordResponse();
        response.setId(record.getId());
        response.setTitle(content.title());
        response.setDescription(content.description());
        response.setCategoryId(content.categoryId());
        response.setStatus(record.getStatus());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }
}