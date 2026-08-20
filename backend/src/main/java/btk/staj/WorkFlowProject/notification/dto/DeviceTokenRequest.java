package btk.staj.WorkFlowProject.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DeviceTokenRequest {
    @NotBlank(message = "Token boş olamaz")
    private String token;

    @NotBlank(message = "Platform boş olamaz")
    @Pattern(regexp = "^(ANDROID|IOS)$", message = "Platform yalnız 'ANDROID' veya 'IOS' olabilir")
    private String platform;

    private String deviceName;
}