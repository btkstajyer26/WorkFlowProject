package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.auth.dto.LoginRequest;
import btk.staj.WorkFlowProject.auth.dto.LoginResponse;
import btk.staj.WorkFlowProject.audit.RequestAuditContext;
import btk.staj.WorkFlowProject.auth.exception.PasswordReuseException;
import btk.staj.WorkFlowProject.common.exception.InvalidCredentialsException;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.time.LocalDateTime;
import btk.staj.WorkFlowProject.notification.repository.DeviceTokenRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RequestAuditContext requestAuditContext;
    private final DeviceTokenRepository deviceTokenRepository;

    public AuthService(UserRepository userRepository, TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       RequestAuditContext requestAuditContext,
                       DeviceTokenRepository deviceTokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.requestAuditContext = requestAuditContext;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            requestAuditContext.mark("LOGIN_FAILED", null, null, null);
            throw new InvalidCredentialsException("Email veya şifre hatalı");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            requestAuditContext.mark("LOGIN_FAILED", user);
            throw new InvalidCredentialsException("Email veya şifre hatalı");
        }

        if (!user.isActive()) {
            requestAuditContext.mark("LOGIN_FAILED", user);
            throw new InvalidCredentialsException("Hesap pasif durumda");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        Token tokenEntity = new Token();
        tokenEntity.setUser(user);
        tokenEntity.setToken(refreshToken);
        tokenEntity.setTokenType("REFRESH");
        tokenEntity.setCreatedAt(LocalDateTime.now());
        tokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        tokenRepository.save(tokenEntity);

        requestAuditContext.mark("LOGIN", user);
        return new LoginResponse(accessToken, refreshToken, user.isMustChangePassword());
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        Token storedToken = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    requestAuditContext.mark("TOKEN_REFRESH_FAILED", null, null, null);
                    return new InvalidCredentialsException("Geçersiz refresh token");
                });

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            requestAuditContext.mark("TOKEN_REFRESH_FAILED", storedToken.getUser());
            throw new InvalidCredentialsException("Refresh token süresi dolmuş veya geçersiz");
        }

        User user = storedToken.getUser();

        if (!user.isActive()) {
            requestAuditContext.mark("TOKEN_REFRESH_FAILED", user);
            throw new InvalidCredentialsException("Hesap pasif durumda");
        }

        storedToken.setRevoked(true);
        tokenRepository.save(storedToken);

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        Token newTokenEntity = new Token();
        newTokenEntity.setUser(user);
        newTokenEntity.setToken(newRefreshToken);
        newTokenEntity.setTokenType("REFRESH");
        newTokenEntity.setCreatedAt(LocalDateTime.now());
        newTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        tokenRepository.save(newTokenEntity);

        requestAuditContext.mark("TOKEN_REFRESH", user);
        return new LoginResponse(newAccessToken, newRefreshToken, user.isMustChangePassword());
    }

    @Transactional
    public void logout(String refreshToken, String deviceToken) {
        tokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
            requestAuditContext.mark("LOGOUT", token.getUser());

            deactivateDeviceTokenIfOwned(deviceToken, token.getUser().getId());
        });
    }

    /**
     * Cikis yapan cihazin push token'ini pasiflestirir. Token oturumdaki
     * kullaniciya ait degilse sessizce yok sayilir: hata donmek, baskasina ait
     * bir token'in varligini dogrulamis olurdu.
     */
    private void deactivateDeviceTokenIfOwned(String deviceToken, UUID userId) {
        if (deviceToken == null || deviceToken.isBlank()) {
            return;
        }

        deviceTokenRepository.findByToken(deviceToken)
                .filter(device -> device.getUser().getId().equals(userId))
                .ifPresent(device -> deviceTokenRepository.deactivateByToken(deviceToken));
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Mevcut şifre yanlış");
        }

        // Arayüz de aynı kuralı uyguluyor, ancak zorunlu şifre değişimi buradan
        // geçtiği için kural sunucuda da tutulmalı: aksi halde geçici şifre
        // kendisiyle "değiştirilip" mustChangePassword kapatılabilirdi.
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new PasswordReuseException("Yeni şifreniz mevcut şifrenizle aynı olamaz");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        tokenRepository.findAllByUser_IdAndRevokedFalse(userId)
                .forEach(token -> token.setRevoked(true));

        requestAuditContext.mark("PASSWORD_CHANGED", user);
    }
}