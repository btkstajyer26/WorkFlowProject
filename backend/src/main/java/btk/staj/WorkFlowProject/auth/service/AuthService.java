package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.auth.dto.LoginRequest;
import btk.staj.WorkFlowProject.auth.dto.LoginResponse;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
//
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email veya şifre hatalı"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Email veya şifre hatalı");
        }
        if (!user.isActive()) {
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

        return new LoginResponse(accessToken, refreshToken);
    }

    public LoginResponse refresh(String refreshToken) {
        Token storedToken = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Geçersiz refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token süresi dolmuş veya geçersiz");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());

        return new LoginResponse(newAccessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        tokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
        });

    }

}
