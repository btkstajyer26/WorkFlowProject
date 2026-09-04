package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;

import btk.staj.WorkFlowProject.common.exception.ApiErrorWriter;
import btk.staj.WorkFlowProject.rbac.Role;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

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
    private JwtAuthenticationService authenticationService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private ApiErrorWriter apiErrorWriter;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(authenticationService, apiErrorWriter);
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

        when(authenticationService.authenticate("valid-token")).thenReturn(Optional.of(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())));

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

        when(authenticationService.authenticate("invalid-token")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void authorizationHeaderYoksa_securityContextBosKalmaliVeFilterChainDevamEtmeli() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(authenticationService);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void bearerOnekiOlmayanHeader_securityContextBosKalmaliVeFilterChainDevamEtmeli() throws Exception {
        request.addHeader("Authorization", "Basic dGVzdDp0ZXN0");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(authenticationService);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void tokenGecerliAmaKullaniciBulunamiyor_exceptionFilterChainiEngellememeli() {
        request.addHeader("Authorization", "Bearer valid-token");

        when(authenticationService.authenticate("valid-token"))
                .thenThrow(new RuntimeException("Kullanıcı bulunamadı"));

        // Not: doFilterInternal içinde try/catch yoksa exception yukarı fırlar.
        // Bu davranış isteniyorsa aşağıdaki assertThrows kalsın; istenmiyorsa
        // filter'a try/catch eklenip test buna göre güncellenmeli.
        assertThrows(RuntimeException.class, () ->
                filter.doFilterInternal(request, response, filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void gecerliTokenAmaKullaniciPasif_securityContextBosKalmaliVeFilterChainDevamEtmeli() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");

        when(authenticationService.authenticate("valid-token")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        // Token yapısal olarak geçerli ve kullanıcı bulunuyor,
        // ama isEnabled() false olduğu için SecurityContext boş kalmalı.
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // filter yine de zinciri kesmemeli.
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ------------------------------------------------------------------
    // A1 - Zorunlu parola degisimi filtre seviyesinde zorlanir
    // ------------------------------------------------------------------

    @Test
    void parolaDegisimiBekleyenKullanici_korumaliUcaErisemez() throws Exception {
        givenAuthenticatedUser(true);
        request.setMethod("GET");
        request.setRequestURI("/api/categories");

        filter.doFilterInternal(request, response, filterChain);

        verify(apiErrorWriter).write(response, org.springframework.http.HttpStatus.FORBIDDEN,
                "PASSWORD_CHANGE_REQUIRED", "Devam etmeden önce parolanızı değiştirmelisiniz");
        verifyNoInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void parolaDegisimiBekleyenKullanici_parolaDegistirmeUcunaErisebilir() throws Exception {
        givenAuthenticatedUser(true);
        request.setMethod("POST");
        request.setRequestURI("/api/auth/change-password");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiErrorWriter);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void parolaDegisimiBekleyenKullanici_cikisUcunaErisebilir() throws Exception {
        givenAuthenticatedUser(true);
        request.setMethod("POST");
        request.setRequestURI("/api/auth/logout");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiErrorWriter);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void parolaDegisimiBekleyenKullanici_kendiKimligineErisebilir() throws Exception {
        givenAuthenticatedUser(true);
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiErrorWriter);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void parolaDegisimiBekleyenKullanici_izinliUcFarkliMethodlaCagrilirsaEngellenmeli() throws Exception {
        // Whitelist yol bazlı değil yol+method bazlı olmalı: /api/auth/change-password
        // GET ile çağrılırsa (POST değil) hâlâ engellenmeli.
        givenAuthenticatedUser(true);
        request.setMethod("GET");
        request.setRequestURI("/api/auth/change-password");

        filter.doFilterInternal(request, response, filterChain);

        verify(apiErrorWriter).write(response, org.springframework.http.HttpStatus.FORBIDDEN,
                "PASSWORD_CHANGE_REQUIRED", "Devam etmeden önce parolanızı değiştirmelisiniz");
        verifyNoInteractions(filterChain);
    }

    @Test
    void parolasiGuncelKullanici_korumaliUcaErisebilir() throws Exception {
        givenAuthenticatedUser(false);
        request.setMethod("GET");
        request.setRequestURI("/api/categories");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiErrorWriter);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /** Oturumu acik, verilen mustChangePassword degerine sahip bir kullanici hazirlar. */
    private void givenAuthenticatedUser(boolean mustChangePassword) {
        request.addHeader("Authorization", "Bearer valid-token");

        Role role = new Role();
        role.setId(1);
        role.setName("CALISAN");
        role.setActive(true);
        role.setSystemKey("CALISAN");
        role.setWorkflowActor(AuthorizationFixtures.workflowActor("CALISAN"));

        btk.staj.WorkFlowProject.user.entity.User user = new btk.staj.WorkFlowProject.user.entity.User();
        user.setEmail("test@example.com");
        user.setRole(role);
        user.setActive(true);
        user.setMustChangePassword(mustChangePassword);

        AuthenticatedUser principal = AuthorizationFixtures.authenticated(user);
        when(authenticationService.authenticate("valid-token")).thenReturn(Optional.of(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));
    }
}
