package btk.staj.WorkFlowProject.record.dto;

public record RecordCreateRequest(
    String title,
    String description,
    Integer categoryId
) {}