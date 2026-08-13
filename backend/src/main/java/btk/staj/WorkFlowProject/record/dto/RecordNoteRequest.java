// Gelen veriyi karşılamak için kullanılacak DTO sınıfı
package btk.staj.WorkFlowProject.record.dto;
    
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecordNoteRequest {
    
    @NotBlank(message = "Çalışma notu boş bırakılamaz")
    @Size(max = 1000, message = "Çalışma notu en fazla 1000 karakter olabilir")
    private String body;

    @NotNull(message = "Versiyon bilgisi zorunludur")
    private Integer version;
}