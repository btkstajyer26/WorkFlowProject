package btk.staj.WorkFlowProject.record.mapper;

import btk.staj.WorkFlowProject.record.dto.RecordCreateRequest;
import btk.staj.WorkFlowProject.record.dto.RecordResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RecordMapper {

    /**
     * DTO'dan yeni bir Record entity'si oluşturur.
     * createdBy bilgisi mapper'ın sorumluluğunda DEĞİLDİR;
     * service katmanından (ileride SecurityContext'ten) parametre olarak alınır.
     */
    public Record toEntity(RecordCreateRequest request, UUID createdBy) {
        return Record.builder()
                .title(request.title())
                .description(request.description())
                .categoryId(request.categoryId())
                .status(RecordStatus.TASLAK)
                .createdBy(createdBy)
                .build();
    }

    public RecordResponse toResponse(Record record) {
        return new RecordResponse(
                record.getId(),
                record.getTitle(),
                record.getDescription(),
                record.getCategoryId(),
                record.getStatus(),
                record.getCreatedAt()
        );
    }
}