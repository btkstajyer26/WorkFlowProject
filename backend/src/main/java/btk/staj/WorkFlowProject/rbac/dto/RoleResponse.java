package btk.staj.WorkFlowProject.rbac.dto;

import btk.staj.WorkFlowProject.rbac.Role;

/**
 * Rol katalogunun API yaniti. Panel hem rol atamasi hem de rol yonetimi icin
 * ayni modeli tuketir.
 *
 * <p>{@code systemKey} yerlesik rolun degismez teknik anahtaridir ve yalniz
 * okunur: {@code name} panelden degistirilebilir, {@code systemKey} asla
 * (V12 / DB_1 SS6.1). {@code system} true olan roller pasiflestirilemez ve
 * workflow aktorlugu degistirilemez.
 */
public record RoleResponse(Integer id,
                           String name,
                           String description,
                           String systemKey,
                           boolean system,
                           boolean workflowActor,
                           Integer maxUsers,
                           boolean active) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getSystemKey(),
                role.isSystem(),
                role.isWorkflowActor(),
                role.getMaxUsers(),
                role.isActive());
    }
}
