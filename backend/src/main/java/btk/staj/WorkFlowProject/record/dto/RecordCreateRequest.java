package btk.staj.WorkFlowProject.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordCreateRequest {
    
    @NotBlank(message = "Başlık boş bırakılamaz")
    private String title;

    @NotBlank(message = "Açıklama boş bırakılamaz")
    private String description;

    @NotNull(message = "Kategori seçimi zorunludur")
    private Integer categoryId;
}