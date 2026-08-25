package btk.staj.WorkFlowProject.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDto {

    private UUID id;
    private UUID recordId;
    private String originalName;
    private String mimeType;
    private int fileSize;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;
}