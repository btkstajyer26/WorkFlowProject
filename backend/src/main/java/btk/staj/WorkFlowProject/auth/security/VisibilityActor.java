package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import org.springframework.security.authentication.DisabledException;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Current authenticated identity for reading records, independent of workflow eligibility. */
public record VisibilityActor(UUID id, RoleId roleId, Optional<SystemRoleKey> systemRole,
                              Set<String> permissionCodes) {
    public VisibilityActor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(systemRole, "systemRole");
        permissionCodes = Set.copyOf(permissionCodes);
    }

    public static VisibilityActor from(AuthenticatedUser user) {
        Objects.requireNonNull(user, "user");
        if (!user.isEnabled()) throw new DisabledException("Authenticated user is disabled");
        String systemKey = user.getSystemKey();
        return new VisibilityActor(user.getId(), new RoleId(user.getRoleId()),
                systemKey == null ? Optional.empty() : Optional.of(SystemRoleKey.valueOf(systemKey)),
                user.getPermissionCodes());
    }

    public boolean hasSystemRole(SystemRoleKey role) {
        return systemRole.filter(role::equals).isPresent();
    }
}
