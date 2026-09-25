package btk.staj.WorkFlowProject.rbac.controller;

import btk.staj.WorkFlowProject.rbac.dto.PermissionResponse;
import btk.staj.WorkFlowProject.rbac.dto.RolePermissionsResponse;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRolePermissionsRequest;
import btk.staj.WorkFlowProject.rbac.service.PermissionAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AP-3 rol <-> permission matrisi. Katalog ve rolun mevcut atamalari okuma
 * icin {@code ROLE_VIEW}, matrisi degistirmek icin {@code ROLE_MANAGE} ister
 * - rol yonetimiyle ayni capability'ler, cunku permission atamasi rol
 * yonetiminin bir parcasidir (bkz. ROLE_MANAGE aciklamasi: "...yetki atar").
 */
@RestController
@RequestMapping("/api/admin")
public class PermissionAdminController {

    private final PermissionAdminService permissionAdminService;

    public PermissionAdminController(PermissionAdminService permissionAdminService) {
        this.permissionAdminService = permissionAdminService;
    }

    /** Kapali capability katalogunun tamami (aktif + pasif). */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public List<PermissionResponse> listPermissions() {
        return permissionAdminService.listPermissions();
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public RolePermissionsResponse getRolePermissions(@PathVariable Integer id) {
        return permissionAdminService.getRolePermissions(id);
    }

    /**
     * Tam degisim (replace): govdedeki kume rolun yeni permission listesi
     * olur, PATCH degildir.
     */
    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RolePermissionsResponse updateRolePermissions(@PathVariable Integer id,
                                                          @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return permissionAdminService.updateRolePermissions(id, request);
    }
}
