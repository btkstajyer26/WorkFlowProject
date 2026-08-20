package btk.staj.WorkFlowProject.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceTokenDeleteRequest {
    @NotBlank(message = "Token boş olamaz")
    private String token;
}