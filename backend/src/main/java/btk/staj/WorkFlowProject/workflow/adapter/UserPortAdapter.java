package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public final class UserPortAdapter implements WorkflowUserPort {

    private final UserRepository userRepository;

    public UserPortAdapter(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
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
    public List<WorkflowUserSnapshot> findActiveByRole(RoleName role) {
        RoleName requiredRole = Objects.requireNonNull(role, "role");
        List<User> users = userRepository.findByRole_NameAndActive(requiredRole.name(), true);
        if (users == null) {
            throw new IllegalStateException("UserRepository.findByRole_NameAndActive returned null");
        }

        List<WorkflowUserSnapshot> snapshots = new ArrayList<>(users.size());
        for (User user : users) {
            snapshots.add(toSnapshot(user));
        }
        return List.copyOf(snapshots);
    }

    private static WorkflowUserSnapshot toSnapshot(User user) {
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

        String entityRoleName = entityRole.getName();
        if (entityRoleName == null) {
            throw new IllegalStateException("Repository User role has a null name");
        }

        RoleName roleName;
        try {
            roleName = RoleName.valueOf(entityRoleName);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Repository User has an unknown role name", exception);
        }

        return new WorkflowUserSnapshot(userId, roleName, user.isActive());
    }
}
