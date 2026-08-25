package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class SecurityCurrentActorProvider implements CurrentActorProvider {

    @Override
    public CurrentActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }
        if (!authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is not trusted");
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Anonymous authentication is not allowed");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || principal.getClass() != AuthenticatedUser.class) {
            throw new AuthenticationServiceException("Authenticated principal is malformed");
        }

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) principal;
        if (!readEnabled(authenticatedUser)) {
            throw new DisabledException("Authenticated user is disabled");
        }

        UUID id = readId(authenticatedUser);
        RoleName role = readRole(authenticatedUser);
        return new CurrentActor(id, role);
    }

    private static boolean readEnabled(AuthenticatedUser authenticatedUser) {
        try {
            return authenticatedUser.isEnabled();
        } catch (RuntimeException exception) {
            throw malformedPrincipal("Unable to read authenticated user state", exception);
        }
    }

    private static UUID readId(AuthenticatedUser authenticatedUser) {
        UUID id;
        try {
            id = authenticatedUser.getId();
        } catch (RuntimeException exception) {
            throw malformedPrincipal("Unable to read authenticated user id", exception);
        }
        if (id == null) {
            throw new AuthenticationServiceException("Authenticated user id is missing");
        }
        return id;
    }

    private static RoleName readRole(AuthenticatedUser authenticatedUser) {
        String roleName;
        try {
            roleName = authenticatedUser.getRoleName();
        } catch (RuntimeException exception) {
            throw malformedPrincipal("Unable to read authenticated user role", exception);
        }
        if (roleName == null) {
            throw new AuthenticationServiceException("Authenticated user role is missing");
        }

        try {
            return RoleName.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            throw malformedPrincipal("Authenticated user role is invalid", exception);
        }
    }

    private static AuthenticationServiceException malformedPrincipal(
            String message,
            RuntimeException cause) {
        return new AuthenticationServiceException(message, cause);
    }
}
