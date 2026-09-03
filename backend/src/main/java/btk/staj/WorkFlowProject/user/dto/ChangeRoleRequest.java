package btk.staj.WorkFlowProject.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
/**
 * Admin'in bir kullanicinin rolunu degistirme istegi.
 *
 * <p>Hesaplar daima Calisan rolüyle acilir; Baskan Yardimcisi, Baskan ve
 * Admin rolleri yalnizca bu ayri islemle atanir.
 */


@Schema(description = "roleId (pozitif) veya eski roleName alanlarından tam biri zorunludur. İkisi birlikte veya ikisi de eksikse 400 döner.")
public class ChangeRoleRequest {
    private UUID replacementBaskanYardimcisiId; // opsiyonel, sadece BAŞKAN_YARDIMCISI koltuğu boşalırken zorunlu

    public UUID getReplacementBaskanYardimcisiId() { return replacementBaskanYardimcisiId; }
    public void setReplacementBaskanYardimcisiId(UUID replacementBaskanYardimcisiId) {
        this.replacementBaskanYardimcisiId = replacementBaskanYardimcisiId;
    }


    @Schema(description = "Eski istemciler için rol adı; roleId ile birlikte gönderilemez", deprecated = true)
    private String roleName;

    @Min(value = 1, message = "Rol kimliği pozitif olmalıdır")
    @Schema(description = "Atanacak rolün kimliği; roleName ile birlikte gönderilemez")
    private Integer roleId;

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    @AssertTrue(message = "roleId veya roleName alanlarından yalnızca biri verilmelidir")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRoleSelectorValid() {
        return roleId != null ? roleName == null : roleName != null && !roleName.isBlank();
    }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
