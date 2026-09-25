package btk.staj.WorkFlowProject.rbac.dto;

import jakarta.validation.constraints.Size;

/**
 * Kismi guncelleme: yalniz gonderilen alanlar uygulanir, {@code null} alan
 * "degistirme" anlamina gelir. {@code systemKey} ve {@code isSystem} hicbir
 * kosulda istemciden degistirilemez.
 */
public class UpdateRoleRequest {

    @Size(max = 100, message = "Rol adı en fazla 100 karakter olabilir")
    private String name;

    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir")
    private String description;

    private Boolean workflowActor;

    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getWorkflowActor() { return workflowActor; }
    public void setWorkflowActor(Boolean workflowActor) { this.workflowActor = workflowActor; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
