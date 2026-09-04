package btk.staj.WorkFlowProject.user.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.user.dto.AdminUserSearchCriteria;
import btk.staj.WorkFlowProject.user.dto.ChangeRoleRequest;
import btk.staj.WorkFlowProject.user.dto.CreateUserRequest;
import btk.staj.WorkFlowProject.user.dto.RoleResponse;
import btk.staj.WorkFlowProject.user.dto.SetActiveRequest;
import btk.staj.WorkFlowProject.user.dto.UserResponse;
import btk.staj.WorkFlowProject.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final UserAuditLogService userAuditLogService;
    private final AuditLogService auditLogService;

    public AdminController(UserService userService,
                           UserAuditLogService userAuditLogService,
                           AuditLogService auditLogService) {
        this.userService = userService;
        this.userAuditLogService = userAuditLogService;
        this.auditLogService = auditLogService;
    }

    /**
     * Hesap her zaman Calisan rolüyle acilir; baslangic rolu istekte
     * secilemez (sartname: rol yalnizca ayri bir islemle degistirilir).
     */
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword()
        ));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request) {
        return UserResponse.from(request.getRoleId() != null
                ? userService.changeRole(id, request.getRoleId(), request.getReplacementBaskanYardimcisiId())
                : userService.changeRole(id, request.getRoleName(), request.getReplacementBaskanYardimcisiId()));
    }

    /**
     * Kullanici listesi: q (ad/soyad/e-posta), role ve active query
     * parametreleriyle filtrelenir; page/size/sort Spring Data'nin
     * standart Pageable baglamasindan gelir (orn. ?page=0&size=10).
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public PagedResponse<UserResponse> listUsers(AdminUserSearchCriteria criteria, Pageable pageable) {
        return userService.searchUsers(criteria, pageable);
    }

    @PatchMapping("/users/{id}/active")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse setActive(@PathVariable UUID id,
                                  @Valid @RequestBody SetActiveRequest request) {
        return UserResponse.from(userService.setActive(id, request.getActive()));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public List<RoleResponse> listRoles() {
        return userService.listAssignableRoles();
    }

    /**
     * Admin paneli log listesi.
     *
     * <p>{@code type=USER} (varsayılan): kullanıcı yönetimi + normal kullanıcı
     * giriş/çıkış/HTTP istekleri ({@code user_audit_logs}).
     * {@code type=RECORD}: evrak geçişleri + admin giriş/çıkış/HTTP istekleri
     * ({@code audit_logs}).
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public PagedResponse<?> listAuditLogs(
            @RequestParam(name = "type", defaultValue = "USER") String type,
            Pageable pageable) {
        if ("RECORD".equalsIgnoreCase(type)) {
            return auditLogService.listAll(pageable);
        }
        return userAuditLogService.listAll(pageable);
    }
}
