package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.auth.security.CurrentUserProvider;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class SecurityCurrentActorProvider implements CurrentActorProvider, CurrentUserProvider {

    @Override
    public CurrentActor currentActor() {
        AuthenticatedUser authenticatedUser = currentUser();
        return new CurrentActor(readId(authenticatedUser), readRole(authenticatedUser),
                authenticatedUser.isWorkflowActor(), authenticatedUser.getPermissionCodes());
    }

    @Override
    public UUID currentUserId() {
        return readId(currentUser());
    }

    private static AuthenticatedUser currentUser() {
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

        return authenticatedUser;
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
            roleName = authenticatedUser.getSystemKey();
        } catch (RuntimeException exception) {
            throw malformedPrincipal("Unable to read authenticated user role", exception);
        }
        if (roleName == null) {
            // Dynamic roles authenticate normally; workflow RoleId support belongs to WF-2D2.
            throw new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_ROLE_NOT_ALLOWED);
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
