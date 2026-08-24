package btk.staj.WorkFlowProject.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi girin")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
