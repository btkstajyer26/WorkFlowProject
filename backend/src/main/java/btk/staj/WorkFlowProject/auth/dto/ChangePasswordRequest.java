package btk.staj.WorkFlowProject.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class ChangePasswordRequest {
    @NotBlank(message = "Mevcut şifre boş olamaz")
    private String currentPassword;
    

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermeli"
    )
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}