package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class UserPortAdapter implements WorkflowUserPort {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public UserPortAdapter(UserRepository userRepository,
                           RolePermissionRepository rolePermissionRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.rolePermissionRepository =
                Objects.requireNonNull(rolePermissionRepository, "rolePermissionRepository");
    }

    @Override
    public Optional<WorkflowUserSnapshot> findById(UUID userId) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId");
        Optional<User> result = userRepository.findById(requiredUserId);
        if (result == null) {
            throw new IllegalStateException("UserRepository.findById returned null");
        }
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toSnapshot(result.get()));
    }

    @Override
    public List<WorkflowUserSnapshot> findActiveByRole(RoleId roleId) {
        RoleId requiredRole = Objects.requireNonNull(roleId, "roleId");
        List<User> users = userRepository.findByRole_IdAndRole_ActiveTrueAndActiveTrue(requiredRole.value());
        if (users == null) {
            throw new IllegalStateException("UserRepository.findByRole_IdAndRole_ActiveTrueAndActiveTrue returned null");
        }

        List<WorkflowUserSnapshot> snapshots = new ArrayList<>(users.size());
        for (User user : users) {
            snapshots.add(toSnapshot(user));
        }
        return List.copyOf(snapshots);
    }

    private WorkflowUserSnapshot toSnapshot(User user) {
        if (user == null) {
            throw new IllegalStateException("UserRepository returned a null User");
        }

        UUID userId = user.getId();
        if (userId == null) {
            throw new IllegalStateException("Repository User has a null id");
        }

        Role entityRole = user.getRole();
        if (entityRole == null) {
            throw new IllegalStateException("Repository User has a null role");
        }

        RoleId roleId;
        try {
            roleId = new RoleId(entityRole.getId());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Repository User has a missing or invalid role id", exception);
        }

        // Pasif rolun permission'lari okunmaz: aktif olmayan rol icin kod listesi
        // anlamsizdir ve zaten yetenek kontrolu once aktiflige bakar.
        Set<String> permissionCodes = entityRole.isActive()
                ? Set.copyOf(rolePermissionRepository.findActiveCodesByRoleId(entityRole.getId()))
                : Set.of();

        return new WorkflowUserSnapshot(
                userId,
                roleId,
                user.isActive() && entityRole.isActive(),
                entityRole.isWorkflowActor(),
                permissionCodes);
    }
}
