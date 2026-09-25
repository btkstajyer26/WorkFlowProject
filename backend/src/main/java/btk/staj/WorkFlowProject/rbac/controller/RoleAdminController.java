package btk.staj.WorkFlowProject.rbac.controller;

import btk.staj.WorkFlowProject.rbac.dto.CreateRoleRequest;
import btk.staj.WorkFlowProject.rbac.dto.RoleResponse;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRoleRequest;
import btk.staj.WorkFlowProject.rbac.service.RoleAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rol katalogunun tamami: okuma {@code ROLE_VIEW}, yazma {@code ROLE_MANAGE}
 * ister. Her uc kendi capability'siyle korunur; {@code ADMIN_PANEL_ACCESS} tek
 * basina yetmez.
 */
@RestController
@RequestMapping("/api/admin/roles")
public class RoleAdminController {

    private final RoleAdminService roleAdminService;

    public RoleAdminController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    /**
     * Varsayilan cagri atanabilir (aktif) rolleri dondurur; yonetim ekrani
     * pasifleri de gormek icin {@code includeInactive=true} gonderir.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public List<RoleResponse> listRoles(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return roleAdminService.listRoles(includeInactive);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse createRole(@Valid @RequestBody CreateRoleRequest request) {
        return roleAdminService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse updateRole(@PathVariable Integer id,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        return roleAdminService.update(id, request);
    }
}
