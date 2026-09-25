package btk.staj.WorkFlowProject.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * AP-8 bağlama isteği. Yalnız hangi şablon geçişine hangi rolün bağlanacağını
 * taşır - aktör kimliği, hedef rol, durum, aksiyon veya permission taşımaz
 * (WF8_AP8 sözleşmesi SS "Java servis sözleşmesi").
 */
public class BindActorRequest {

    @NotNull(message = "templateTransitionId zorunludur")
    @Positive(message = "templateTransitionId pozitif olmalı")
    private Integer templateTransitionId;

    @NotNull(message = "actorRoleId zorunludur")
    @Positive(message = "actorRoleId pozitif olmalı")
    private Integer actorRoleId;

    public Integer getTemplateTransitionId() { return templateTransitionId; }
    public void setTemplateTransitionId(Integer templateTransitionId) { this.templateTransitionId = templateTransitionId; }
    public Integer getActorRoleId() { return actorRoleId; }
    public void setActorRoleId(Integer actorRoleId) { this.actorRoleId = actorRoleId; }
}
