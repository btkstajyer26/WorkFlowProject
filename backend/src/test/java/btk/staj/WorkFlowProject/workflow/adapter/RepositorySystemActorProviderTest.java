package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositorySystemActorProviderTest {

    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000901");

    @Mock
    private UserRepository users;

    @Mock
    private RolePermissionRepository rolePermissions;

    private RepositorySystemActorProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RepositorySystemActorProvider(users, rolePermissions);
    }

    @Test
    void returnsCurrentActorForTheConfiguredSystemUser() {
        User user = systemUser(true, systemRole());
        when(users.findAllByRole_SystemKey("SISTEM")).thenReturn(List.of(user));
        when(rolePermissions.findActiveCodesByRoleId(91)).thenReturn(List.of("SYSTEM_EXECUTE"));

        CurrentActor actor = provider.systemActor();

        assertThat(actor).isEqualTo(new CurrentActor(
                SYSTEM_USER_ID,
                new RoleId(91),
                true,
                java.util.Set.of("SYSTEM_EXECUTE")));
        verify(users).findAllByRole_SystemKey("SISTEM");
        verify(rolePermissions).findActiveCodesByRoleId(91);
    }

    @Test
    void failsFastWhenSystemUserIsMissing() {
        when(users.findAllByRole_SystemKey("SISTEM")).thenReturn(List.of());

        assertThatThrownBy(provider::systemActor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one SISTEM user")
                .hasMessageContaining("0");
        verifyNoInteractions(rolePermissions);
    }

    @Test
    void failsFastWhenSystemUserIsInactive() {
        when(users.findAllByRole_SystemKey("SISTEM"))
                .thenReturn(List.of(systemUser(false, systemRole())));

        assertThatThrownBy(provider::systemActor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SISTEM user is inactive");
        verifyNoInteractions(rolePermissions);
    }

    @Test
    void failsFastWhenUserHasAnotherRole() {
        Role role = systemRole();
        role.setSystemKey("CALISAN");
        when(users.findAllByRole_SystemKey("SISTEM"))
                .thenReturn(List.of(systemUser(true, role)));

        assertThatThrownBy(provider::systemActor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not have the SISTEM role");
        verifyNoInteractions(rolePermissions);
    }

    private static User systemUser(boolean active, Role role) {
        User user = new User();
        user.setId(SYSTEM_USER_ID);
        user.setActive(active);
        user.setRole(role);
        return user;
    }

    private static Role systemRole() {
        Role role = new Role();
        role.setId(91);
        role.setName("SISTEM");
        role.setSystemKey("SISTEM");
        role.setSystem(true);
        role.setWorkflowActor(true);
        role.setActive(true);
        return role;
    }
}
