package btk.staj.WorkFlowProject.department.dto;

import jakarta.validation.constraints.Size;

/**
 * Kismi guncelleme: yalniz gonderilen alanlar uygulanir, {@code null} alan
 * "degistirme" anlamina gelir (AP-2 UpdateRoleRequest ile ayni kalip).
 *
 * <p>{@code parentDepartmentId} icin bu kural tek basina yetersizdir: {@code null}
 * hem "degistirme" hem de "ust departmani kaldir (kok yap)" anlamina gelebilir.
 * Belirsizligi gidermek icin ayri bir {@code clearParent} bayragi vardir -
 * {@code true} ise govdedeki {@code parentDepartmentId} yok sayilir ve alan
 * {@code null}'a cekilir.
 */
public class UpdateDepartmentRequest {

    @Size(max = 150, message = "Departman adı en fazla 150 karakter olabilir")
    private String name;

    private Integer parentDepartmentId;

    private boolean clearParent;

    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getParentDepartmentId() { return parentDepartmentId; }
    public void setParentDepartmentId(Integer parentDepartmentId) { this.parentDepartmentId = parentDepartmentId; }
    public boolean isClearParent() { return clearParent; }
    public void setClearParent(boolean clearParent) { this.clearParent = clearParent; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
