package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.auth.dto.LoginRequest;
import btk.staj.WorkFlowProject.auth.dto.LoginResponse;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService için birim testleri.
 * Repository, PasswordEncoder ve JwtUtil bağımlılıkları mock'lanarak
 * servis katmanı izole şekilde test edilir.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private User user;

    @Mock
    private Role role;

    private AuthService authService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenRepository, passwordEncoder, jwtUtil);
        userId = UUID.randomUUID();
    }

    // ---------------- login ----------------

    @Test
    void login_gecerliBilgilerle_tokenlariDondurupRefreshTokenKaydetmeli() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("sifre123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("hashed-pass");
        when(passwordEncoder.matches("sifre123", "hashed-pass")).thenReturn(true);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn("USER");
        when(jwtUtil.generateAccessToken(userId, "test@example.com", "USER")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userId)).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository, times(1)).save(tokenCaptor.capture());

        Token savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertEquals("refresh-token", savedToken.getToken());
        assertEquals("REFRESH", savedToken.getTokenType());
        assertNotNull(savedToken.getCreatedAt());
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(savedToken.getCreatedAt()));
    }

    @Test
    void login_kullaniciBulunamazsa_exceptionFirlatmali() {
        LoginRequest request = new LoginRequest();
        request.setEmail("olmayan@example.com");
        request.setPassword("sifre123");

        when(userRepository.findByEmail("olmayan@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("Email veya şifre hatalı", ex.getMessage());
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_sifreYanlissa_exceptionFirlatmali() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("yanlis-sifre");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("hashed-pass");
        when(passwordEncoder.matches("yanlis-sifre", "hashed-pass")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("Email veya şifre hatalı", ex.getMessage());
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(jwtUtil);
    }

    // ---------------- refresh ----------------

    @Test
    void refresh_gecerliTokenIle_yeniAccessTokenDondurmeli() {
        Token storedToken = mock(Token.class);

        when(tokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(storedToken));
        when(storedToken.isRevoked()).thenReturn(false);
        when(storedToken.getExpiresAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(storedToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn("USER");
        when(jwtUtil.generateAccessToken(userId, "test@example.com", "USER")).thenReturn("new-access-token");

        LoginResponse response = authService.refresh("valid-refresh-token");

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("valid-refresh-token", response.getRefreshToken());
    }

    @Test
    void refresh_tokenBulunamazsa_exceptionFirlatmali() {
        when(tokenRepository.findByToken("olmayan-token")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refresh("olmayan-token"));

        assertEquals("Geçersiz refresh token", ex.getMessage());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void refresh_tokenRevokeEdilmisse_exceptionFirlatmali() {
        Token storedToken = mock(Token.class);

        when(tokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(storedToken));
        when(storedToken.isRevoked()).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refresh("revoked-token"));

        assertEquals("Refresh token süresi dolmuş veya geçersiz", ex.getMessage());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void refresh_tokenSuresiDolmussa_exceptionFirlatmali() {
        Token storedToken = mock(Token.class);

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(storedToken));
        when(storedToken.isRevoked()).thenReturn(false);
        when(storedToken.getExpiresAt()).thenReturn(LocalDateTime.now().minusDays(1));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refresh("expired-token"));

        assertEquals("Refresh token süresi dolmuş veya geçersiz", ex.getMessage());
        verifyNoInteractions(jwtUtil);
    }

    // ---------------- logout ----------------

    @Test
    void logout_tokenBulunursa_revokedTrueYapipKaydetmeli() {
        Token storedToken = mock(Token.class);

        when(tokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(storedToken));

        authService.logout("token-to-revoke");

        verify(storedToken, times(1)).setRevoked(true);
        verify(tokenRepository, times(1)).save(storedToken);
    }

    @Test
    void logout_tokenBulunamazsa_hicbirSeyYapmamali() {
        when(tokenRepository.findByToken("olmayan-token")).thenReturn(Optional.empty());

        authService.logout("olmayan-token");

        verify(tokenRepository, never()).save(any());
    }
}