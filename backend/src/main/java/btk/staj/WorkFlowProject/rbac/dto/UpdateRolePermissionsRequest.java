package btk.staj.WorkFlowProject.rbac.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Rolun permission kumesi icin tam degisim (replace) istegi: gonderilen kume
 * rolun yeni hali olur, kismi PATCH degildir. Bos kume gecerlidir - rolun
 * butun yetkilerini kaldirmak istemcinin bilincli karari olabilir.
 */
public class UpdateRolePermissionsRequest {

    @NotNull(message = "permissionCodes alanı zorunludur")
    private Set<String> permissionCodes;

    public Set<String> getPermissionCodes() { return permissionCodes; }
    public void setPermissionCodes(Set<String> permissionCodes) { this.permissionCodes = permissionCodes; }
}
