package btk.staj.WorkFlowProject.record.dto;

public record RecordUpdateRequest(
    String title,
    String description,
    Integer categoryId
) {}