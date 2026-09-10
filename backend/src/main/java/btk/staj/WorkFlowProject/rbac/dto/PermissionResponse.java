package btk.staj.WorkFlowProject.rbac.dto;

import btk.staj.WorkFlowProject.rbac.Permission;

/**
 * Kapali capability katalogunun API yaniti. Admin yeni kod uretemez; bu yanit
 * yalniz backend destekli, Flyway seed'iyle gelen kodlari listeler (DB_1
 * SS6.2).
 */
public record PermissionResponse(Integer id,
                                 String code,
                                 String displayName,
                                 String description,
                                 boolean active) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getDisplayName(),
                permission.getDescription(),
                permission.isActive());
    }
}
