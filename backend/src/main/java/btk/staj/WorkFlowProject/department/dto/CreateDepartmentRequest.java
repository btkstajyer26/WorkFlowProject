package btk.staj.WorkFlowProject.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateDepartmentRequest {

    @NotBlank(message = "Departman adı boş olamaz")
    @Size(max = 150, message = "Departman adı en fazla 150 karakter olabilir")
    private String name;

    /** Ust departman; yeni departmanin kendisi henuz var olmadigi icin dongu olusamaz. */
    private Integer parentDepartmentId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getParentDepartmentId() { return parentDepartmentId; }
    public void setParentDepartmentId(Integer parentDepartmentId) { this.parentDepartmentId = parentDepartmentId; }
}
