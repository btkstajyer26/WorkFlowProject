package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UserPortAdapterTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIRST_DEPUTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SECOND_DEPUTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserPortAdapter adapter = new UserPortAdapter(userRepository);

    @Test
    void findByIdMapsAnActiveUserFromEntityData() {
        User user = user(USER_ID, "CALISAN", true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Optional<WorkflowUserSnapshot> result = adapter.findById(USER_ID);

        assertThat(result).contains(new WorkflowUserSnapshot(USER_ID, RoleName.CALISAN, true));
        verify(userRepository).findById(USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void findByIdKeepsAnInactiveUserVisible() {
        User user = user(USER_ID, "BASKAN", false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Optional<WorkflowUserSnapshot> result = adapter.findById(USER_ID);

        assertThat(result).contains(new WorkflowUserSnapshot(USER_ID, RoleName.BASKAN, false));
    }

    @Test
    void findByIdReturnsEmptyWhenTheRepositoryDoesNotFindTheUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(USER_ID)).isEmpty();
    }

    @Test
    void requiredDependenciesAndArgumentsRejectNullBeforeRepositoryAccess() {
        assertThatNullPointerException()
                .isThrownBy(() -> new UserPortAdapter(null));
        assertThatNullPointerException()
                .isThrownBy(() -> adapter.findById(null));
        assertThatNullPointerException()
                .isThrownBy(() -> adapter.findActiveByRole(null));

        verifyNoInteractions(userRepository);
    }

    @Test
    void findByIdRejectsANullOptionalReturnedByTheRepository() {
        when(userRepository.findById(USER_ID)).thenReturn(null);

        assertThatIllegalStateException()
                .isThrownBy(() -> adapter.findById(USER_ID));

        verify(userRepository).findById(USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void findActiveByRoleRejectsANullListReturnedByTheRepository() {
        when(userRepository.findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue(RoleName.CALISAN.name())).thenReturn(null);

        assertThatIllegalStateException()
                .isThrownBy(() -> adapter.findActiveByRole(RoleName.CALISAN));

        verify(userRepository).findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue("CALISAN");
        verifyNoMoreInteractions(userRepository);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedUsers")
    void findActiveByRoleRejectsMalformedRepositoryUsers(String scenario, User malformedUser) {
        when(userRepository.findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue(RoleName.CALISAN.name()))
                .thenReturn(Arrays.asList(malformedUser));

        assertThatIllegalStateException()
                .isThrownBy(() -> adapter.findActiveByRole(RoleName.CALISAN));
    }

    @Test
    void findActiveByRoleUsesTheExactRepositoryQueryAndReturnsAnEmptyList() {
        when(userRepository.findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue(RoleName.BASKAN.name())).thenReturn(List.of());

        List<WorkflowUserSnapshot> result = adapter.findActiveByRole(RoleName.BASKAN);

        assertThat(result).isEmpty();
        verify(userRepository).findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue("BASKAN");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void findActiveByRoleMapsActualEntityRoleAndActiveValueWithoutFiltering() {
        User repositoryUser = user(USER_ID, "ADMIN", false);
        when(userRepository.findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue(RoleName.BASKAN.name()))
                .thenReturn(List.of(repositoryUser));

        List<WorkflowUserSnapshot> result = adapter.findActiveByRole(RoleName.BASKAN);

        assertThat(result).containsExactly(new WorkflowUserSnapshot(USER_ID, RoleName.ADMIN, false));
    }

    @Test
    void findActiveByRolePreservesRepositoryOrderAndSupportsMultipleActiveDeputies() {
        User first = user(FIRST_DEPUTY_ID, "BASKAN_YARDIMCISI", true);
        User second = user(SECOND_DEPUTY_ID, "BASKAN_YARDIMCISI", true);
        when(userRepository.findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue(RoleName.BASKAN_YARDIMCISI.name()))
                .thenReturn(List.of(first, second));

        List<WorkflowUserSnapshot> result = adapter.findActiveByRole(RoleName.BASKAN_YARDIMCISI);

        assertThat(result).containsExactly(
                new WorkflowUserSnapshot(FIRST_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true),
                new WorkflowUserSnapshot(SECOND_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true));
        verify(userRepository).findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue("BASKAN_YARDIMCISI");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void findActiveByRoleDoesNotDeduplicateRepositoryEntries() {
        User duplicate = user(FIRST_DEPUTY_ID, "BASKAN_YARDIMCISI", true);
        when(userRepository.findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue(RoleName.BASKAN_YARDIMCISI.name()))
                .thenReturn(List.of(duplicate, duplicate));

        List<WorkflowUserSnapshot> result = adapter.findActiveByRole(RoleName.BASKAN_YARDIMCISI);

        WorkflowUserSnapshot snapshot =
                new WorkflowUserSnapshot(FIRST_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        assertThat(result).containsExactly(snapshot, snapshot);
    }

    private static Stream<Arguments> malformedUsers() {
        User withoutRole = new User();
        withoutRole.setId(USER_ID);
        withoutRole.setActive(true);

        return Stream.of(
                arguments("null User", null),
                arguments("null User id", user(null, "CALISAN", true)),
                arguments("null User role", withoutRole),
                arguments("null role name", user(USER_ID, null, true)),
                arguments("unknown role name", user(USER_ID, "UNKNOWN", true)),
                arguments("lower-case role name", user(USER_ID, "calisan", true)));
    }

    private static User user(UUID id, String roleName, boolean active) {
        Role role = new Role();
        role.setName(roleName);
        role.setSystemKey(roleName);
        role.setActive(true);

        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
