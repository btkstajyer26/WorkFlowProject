// Frontende veriyi göndermek için kullanılacak DTO sınıfı
package btk.staj.WorkFlowProject.record.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RecordNoteResponse {
    private UUID id;
    private UUID recordId;
    private UUID authorId;
    private Integer authorRoleId;
    private String body;
    private Integer version;
    private LocalDateTime updatedAt;
}