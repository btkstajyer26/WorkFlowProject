package btk.staj.WorkFlowProject.department.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateDepartmentRoutingRuleRequest {

    @NotNull(message = "fromStatusId zorunludur")
    @Positive(message = "fromStatusId pozitif olmalı")
    private Integer fromStatusId;

    @NotNull(message = "actionId zorunludur")
    @Positive(message = "actionId pozitif olmalı")
    private Integer actionId;

    @NotNull(message = "targetRoleId zorunludur")
    @Positive(message = "targetRoleId pozitif olmalı")
    private Integer targetRoleId;

    public Integer getFromStatusId() { return fromStatusId; }
    public void setFromStatusId(Integer fromStatusId) { this.fromStatusId = fromStatusId; }
    public Integer getActionId() { return actionId; }
    public void setActionId(Integer actionId) { this.actionId = actionId; }
    public Integer getTargetRoleId() { return targetRoleId; }
    public void setTargetRoleId(Integer targetRoleId) { this.targetRoleId = targetRoleId; }
}
