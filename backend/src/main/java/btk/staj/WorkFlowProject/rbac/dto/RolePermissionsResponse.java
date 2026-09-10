package btk.staj.WorkFlowProject.rbac.dto;

import java.util.List;

/**
 * Matrisin tek satiri: bir rolun su anda tasidigi permission kodlari. Kod
 * pasiflesmis olsa bile burada gorunur (bkz.
 * {@code RolePermissionRepository#findAllCodesByRoleId}); panel bunu "pasif
 * permission, kaldirmayi dusun" olarak gosterebilir.
 */
public record RolePermissionsResponse(Integer roleId,
                                      String roleName,
                                      List<String> permissionCodes) {
}
