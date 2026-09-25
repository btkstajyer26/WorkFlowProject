package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SecurityCurrentActorProviderTest {

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");

    private final SecurityCurrentActorProvider provider = new SecurityCurrentActorProvider();

    @BeforeEach
    void clearSecurityContextBeforeTest() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContextAfterTest() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(RoleName.class)
    void mapsEveryKnownRoleIncludingAdmin(RoleName role) {
        authenticate(authenticatedUser(USER_ID, role.name(), true));

        CurrentActor actor = provider.currentActor();

        assertThat(actor.id()).isEqualTo(USER_ID);
        assertThat(actor.roleId()).isEqualTo(new RoleId(1001));
        assertThat(actor.workflowActor()).isEqualTo(AuthorizationFixtures.workflowActor(role));
        assertThat(provider.currentVisibilityActor().systemRole()).contains(btk.staj.WorkFlowProject.rbac.SystemRoleKey.valueOf(role.name()));
        assertThat(provider.currentVisibilityActor().roleId()).isEqualTo(new RoleId(1001));
        assertThat(provider.currentVisibilityActor().id()).isEqualTo(USER_ID);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsUnauthenticatedTokenBeforeInspectingPrincipal() {
        AuthenticatedUser inactiveMalformedPrincipal = authenticatedUser(null, null, false);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(inactiveMalformedPrincipal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(authentication.isAuthenticated()).isFalse();
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsAnonymousTokenEvenWhenItIsAuthenticated() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsNullPrincipal() {
        authenticate(null);

        assertThatExceptionOfType(AuthenticationServiceException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsWrongPrincipalType() {
        authenticate("not-an-authenticated-user");

        assertThatExceptionOfType(AuthenticationServiceException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsAuthenticatedUserSubtypeBecausePrincipalMustBeExactClass() {
        authenticate(new DerivedAuthenticatedUser(user(USER_ID, RoleName.CALISAN.name(), true)));

        assertThatExceptionOfType(AuthenticationServiceException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsInactiveUserBeforeInspectingIdOrRole() {
        authenticate(authenticatedUser(null, null, false));

        assertThatExceptionOfType(DisabledException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsNullBackingUserAtConstruction() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> AuthorizationFixtures.authenticated(null));
    }

    @Test
    void rejectsNullUserId() {
        authenticate(authenticatedUser(null, RoleName.CALISAN.name(), true));

        assertThatExceptionOfType(AuthenticationServiceException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void rejectsMissingRoleAsDisabled() {
        User user = user(USER_ID, RoleName.CALISAN.name(), true);
        user.setRole(null);
        authenticate(AuthorizationFixtures.authenticated(user));

        assertThatExceptionOfType(DisabledException.class)
                .isThrownBy(provider::currentActor);
    }

    @Test
    void dynamicRoleSuppliesWorkflowIdentityEligibilityAndPermissions() {
        User user = user(USER_ID, null, true);
        user.getRole().setName("Dynamic reviewer");
        user.getRole().setWorkflowActor(true);
        authenticate(new AuthenticatedUser(user, Set.of("RECORD_FORWARD")));

        assertThat(provider.currentVisibilityActor().systemRole()).isEmpty();
        assertThat(provider.currentVisibilityActor().permissionCodes()).containsExactly("RECORD_FORWARD");
        assertThat(provider.currentUserId()).isEqualTo(USER_ID);
        assertThat(provider.currentActor()).isEqualTo(
                new CurrentActor(USER_ID, new RoleId(1001), true, Set.of("RECORD_FORWARD")));
    }

    @Test
    void rejectsZeroRoleId() {
        assertInvalidRoleId(0);
    }

    @Test
    void rejectsNegativeRoleId() {
        assertInvalidRoleId(-1);
    }

    @Test
    void rejectsMissingRoleId() {
        assertInvalidRoleId(null);
    }

    private void assertInvalidRoleId(Integer roleId) {
        User user = user(USER_ID, "CALISAN", true);
        user.getRole().setId(roleId);
        authenticate(AuthorizationFixtures.authenticated(user));
        assertThatExceptionOfType(AuthenticationServiceException.class)
                .isThrownBy(provider::currentActor)
                .withMessageContaining("role id");
    }

    private static void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static AuthenticatedUser authenticatedUser(UUID id, String roleName, boolean active) {
        return AuthorizationFixtures.authenticated(user(id, roleName, active));
    }

    private static User user(UUID id, String roleName, boolean active) {
        Role role = new Role();
        role.setId(1001);
        role.setName(roleName);
        role.setActive(true);
        role.setSystemKey(roleName);
        role.setWorkflowActor(AuthorizationFixtures.workflowActor(roleName));

        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    private static final class DerivedAuthenticatedUser extends AuthenticatedUser {

        private DerivedAuthenticatedUser(User user) {
            super(user, java.util.Set.of());
        }
    }
}
