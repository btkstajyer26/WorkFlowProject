package btk.staj.WorkFlowProject.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Panelden acilan dinamik rol. {@code systemKey}, {@code isSystem} ve
 * {@code maxUsers} istekte tasinmaz: yeni rol daima sistem rolu olmayan ve
 * sinirsiz kapasiteli olarak acilir (ADR-0007).
 */
public class CreateRoleRequest {

    @NotBlank(message = "Rol adı boş olamaz")
    @Size(max = 100, message = "Rol adı en fazla 100 karakter olabilir")
    private String name;

    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir")
    private String description;

    /** Rolun mevcut gecislere aktor olarak baglanabilmesi (WF-8 / AP-8 sartı). */
    private boolean workflowActor;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isWorkflowActor() { return workflowActor; }
    public void setWorkflowActor(boolean workflowActor) { this.workflowActor = workflowActor; }
}
