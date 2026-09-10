package btk.staj.WorkFlowProject.department.dto;

import jakarta.validation.constraints.Positive;

/**
 * Kismi guncelleme: {@code null} alan "degistirme" demektir. {@code fromStatusId}/
 * {@code actionId} kuralin sabit anahtaridir ve burada degistirilemez - farkli bir
 * (durum, aksiyon) icin yeni bir kural olusturulur.
 */
public class UpdateDepartmentRoutingRuleRequest {

    @Positive(message = "targetRoleId pozitif olmalı")
    private Integer targetRoleId;

    private Boolean active;

    public Integer getTargetRoleId() { return targetRoleId; }
    public void setTargetRoleId(Integer targetRoleId) { this.targetRoleId = targetRoleId; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
