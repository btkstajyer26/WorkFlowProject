package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.SystemActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the seeded SISTEM identity without using the HTTP security context. */
@Component
public class RepositorySystemActorProvider implements SystemActorProvider {

    private final UserRepository users;
    private final RolePermissionRepository rolePermissions;

    public RepositorySystemActorProvider(
            UserRepository users,
            RolePermissionRepository rolePermissions) {
        this.users = Objects.requireNonNull(users, "users");
        this.rolePermissions = Objects.requireNonNull(rolePermissions, "rolePermissions");
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentActor systemActor() {
        List<User> candidates = users.findAllByRole_SystemKey(SystemRoleKey.SISTEM.name());
        if (candidates == null) {
            throw configurationFailure("UserRepository returned null");
        }
        if (candidates.size() != 1) {
            throw configurationFailure(
                    "Expected exactly one SISTEM user but found " + candidates.size());
        }

        User user = candidates.getFirst();
        if (user == null) {
            throw configurationFailure("Repository returned a null SISTEM user");
        }
        if (user.getId() == null) {
            throw configurationFailure("SISTEM user id is missing");
        }
        if (!user.isActive()) {
            throw configurationFailure("SISTEM user is inactive");
        }

        Role role = user.getRole();
        if (role == null || !SystemRoleKey.SISTEM.matches(role)) {
            throw configurationFailure("SISTEM user does not have the SISTEM role");
        }
        if (role.getId() == null) {
            throw configurationFailure("SISTEM role id is missing");
        }
        if (!role.isActive()) {
            throw configurationFailure("SISTEM role is inactive");
        }
        if (!role.isSystem() || !role.isWorkflowActor()) {
            throw configurationFailure("SISTEM role is not a system workflow actor");
        }

        List<String> permissionCodes = rolePermissions.findActiveCodesByRoleId(role.getId());
        if (permissionCodes == null) {
            throw configurationFailure("RolePermissionRepository returned null");
        }

        try {
            return new CurrentActor(
                    user.getId(),
                    new RoleId(role.getId()),
                    true,
                    Set.copyOf(permissionCodes));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Invalid SISTEM actor configuration", exception);
        }
    }

    private static IllegalStateException configurationFailure(String detail) {
        return new IllegalStateException("Invalid SISTEM actor configuration: " + detail);
    }
}
