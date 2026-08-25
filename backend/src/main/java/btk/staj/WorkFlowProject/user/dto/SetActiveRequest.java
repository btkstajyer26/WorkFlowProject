package btk.staj.WorkFlowProject.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Admin'in bir kullanicinin hesap durumunu (aktif/pasif) degistirme istegi.
 */
public class SetActiveRequest {

    @NotNull(message = "active alanı boş olamaz")
    private Boolean active;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}