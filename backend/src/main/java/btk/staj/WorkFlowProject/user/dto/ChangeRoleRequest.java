package btk.staj.WorkFlowProject.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
/**
 * Admin'in bir kullanicinin rolunu degistirme istegi.
 *
 * <p>Hesaplar daima Calisan rolüyle acilir; Baskan Yardimcisi, Baskan ve
 * Admin rolleri yalnizca bu ayri islemle atanir.
 */


public class ChangeRoleRequest {
    private UUID replacementBaskanYardimcisiId; // opsiyonel, sadece BAŞKAN_YARDIMCISI koltuğu boşalırken zorunlu

    public UUID getReplacementBaskanYardimcisiId() { return replacementBaskanYardimcisiId; }
    public void setReplacementBaskanYardimcisiId(UUID replacementBaskanYardimcisiId) {
        this.replacementBaskanYardimcisiId = replacementBaskanYardimcisiId;
    }


    @NotBlank(message = "Rol adı boş olamaz")
    private String roleName;

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
