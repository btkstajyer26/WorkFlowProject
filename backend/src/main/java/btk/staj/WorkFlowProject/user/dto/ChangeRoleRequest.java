package btk.staj.WorkFlowProject.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin'in bir kullanicinin rolunu degistirme istegi.
 *
 * <p>Hesaplar daima Calisan rolüyle acilir; Baskan Yardimcisi, Baskan ve
 * Admin rolleri yalnizca bu ayri islemle atanir.
 */
public class ChangeRoleRequest {

    @NotBlank(message = "Rol adı boş olamaz")
    private String roleName;

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
