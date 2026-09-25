package btk.staj.WorkFlowProject.department.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddDepartmentMemberRequest {

    @NotNull(message = "userId zorunludur")
    private UUID userId;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
