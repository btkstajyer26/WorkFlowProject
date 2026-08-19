package btk.staj.WorkFlowProject.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResetPasswordRequest {

    @NotBlank(message = "Sıfırlama anahtarı boş olamaz")
    private String token;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermeli"
    )
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
