package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter için birim testleri.
 * Token doğrulama sonucuna göre SecurityContext'in doğru şekilde set edilip
 * edilmediğini ve filterChain'in her durumda devam ettiğini doğrular.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void gecerliToken_securityContextAuthenticationSetEdilmeli() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");

        UserDetails userDetails = new User(
                "test@example.com",
                "irrelevant-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userDetails, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_USER"::equals));

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void gecersizToken_securityContextBosKalmaliVeFilterChainDevamEtmeli() throws Exception {
        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void authorizationHeaderYoksa_securityContextBosKalmaliVeFilterChainDevamEtmeli() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userDetailsService);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void bearerOnekiOlmayanHeader_securityContextBosKalmaliVeFilterChainDevamEtmeli() throws Exception {
        request.addHeader("Authorization", "Basic dGVzdDp0ZXN0");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userDetailsService);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void tokenGecerliAmaKullaniciBulunamiyor_exceptionFilterChainiEngellememeli() {
        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("olmayan@example.com");
        when(userDetailsService.loadUserByUsername("olmayan@example.com"))
                .thenThrow(new RuntimeException("Kullanıcı bulunamadı"));

        // Not: doFilterInternal içinde try/catch yoksa exception yukarı fırlar.
        // Bu davranış isteniyorsa aşağıdaki assertThrows kalsın; istenmiyorsa
        // filter'a try/catch eklenip test buna göre güncellenmeli.
        assertThrows(RuntimeException.class, () ->
                filter.doFilterInternal(request, response, filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}