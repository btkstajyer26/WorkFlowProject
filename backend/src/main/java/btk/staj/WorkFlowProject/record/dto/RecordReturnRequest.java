package btk.staj.WorkFlowProject.record.dto;

import lombok.Data;

@Data
public class RecordReturnRequest {
    private String target; // "CALISAN" veya "BASKAN_YARDIMCISI"
    private String comment; // Başkan'ın geri gönderme açıklaması
}