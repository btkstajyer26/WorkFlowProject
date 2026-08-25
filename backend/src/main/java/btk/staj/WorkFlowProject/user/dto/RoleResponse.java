package btk.staj.WorkFlowProject.user.dto;

import btk.staj.WorkFlowProject.rbac.Role;

/**
 * Admin'in bir kullaniciya atayabilecegi rollerin listesi icin API yaniti.
 */
public record RoleResponse(Integer id, String name, String description) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription());
    }
}