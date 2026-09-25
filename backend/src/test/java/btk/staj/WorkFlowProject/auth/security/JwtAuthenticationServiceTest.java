package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;

    @Test
    void validTokenCreatesAuthenticationFromCurrentUserDetails() {
        JwtAuthenticationService service = new JwtAuthenticationService(jwtUtil, userDetailsService);
        UserDetails principal = new User("user@example.com", "password",
                List.of(new SimpleGrantedAuthority("NOTIFICATION_READ")));
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractEmail("token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(principal);

        var authentication = service.authenticate("token").orElseThrow();

        assertEquals(principal, authentication.getPrincipal());
        assertTrue(authentication.isAuthenticated());
        assertEquals(
                principal.getAuthorities().stream().map(authority -> authority.getAuthority()).toList(),
                authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
    }

    @Test
    void invalidTokenDoesNotLoadAUser() {
        JwtAuthenticationService service = new JwtAuthenticationService(jwtUtil, userDetailsService);
        when(jwtUtil.isTokenValid("invalid")).thenReturn(false);

        assertTrue(service.authenticate("invalid").isEmpty());

        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void disabledCurrentUserIsRejected() {
        JwtAuthenticationService service = new JwtAuthenticationService(jwtUtil, userDetailsService);
        UserDetails principal = new User("user@example.com", "password", false,
                true, true, true, List.of());
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractEmail("token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(principal);

        assertTrue(service.authenticate("token").isEmpty());
    }
}
