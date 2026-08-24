package btk.staj.WorkFlowProject.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyResetCodeRequest {

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi girin")
    private String email;

    @NotBlank(message = "Doğrulama kodu boş olamaz")
    @Pattern(regexp = "^\\d{6}$", message = "Doğrulama kodu 6 haneli olmalıdır")
    private String code;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
