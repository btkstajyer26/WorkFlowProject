package btk.staj.WorkFlowProject.record.mapper;

import btk.staj.WorkFlowProject.record.dto.RecordCreateRequest;
import btk.staj.WorkFlowProject.record.dto.RecordResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
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

    public RecordResponse toResponse(Record record) {
        RecordResponse response = new RecordResponse();
        response.setId(record.getId());
        response.setTitle(record.getTitle());
        response.setDescription(record.getDescription());
        response.setCategoryId(record.getCategoryId());
        response.setStatus(record.getStatus());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }
}