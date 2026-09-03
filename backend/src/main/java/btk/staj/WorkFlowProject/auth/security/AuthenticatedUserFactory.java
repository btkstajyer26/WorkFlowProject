package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import org.springframework.stereotype.Component;

/** Shared by password/JWT authentication and transactional mail actions. */
@Component
public class AuthenticatedUserFactory {
    private final RolePermissionRepository permissions;

    public AuthenticatedUserFactory(RolePermissionRepository permissions) {
        this.permissions = permissions;
    }

    public AuthenticatedUser create(User user) {
        var codes = user.getRole() == null || !user.getRole().isActive()
                ? java.util.List.<String>of()
                : permissions.findActiveCodesByRoleId(user.getRole().getId());
        return new AuthenticatedUser(user, codes);
    }
}
