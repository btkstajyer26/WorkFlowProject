package btk.staj.WorkFlowProject.auth.controller;

import btk.staj.WorkFlowProject.auth.dto.LoginRequest;
import btk.staj.WorkFlowProject.auth.dto.LoginResponse;
import btk.staj.WorkFlowProject.auth.dto.LogoutRequest;
import btk.staj.WorkFlowProject.auth.dto.RefreshTokenRequest;
import btk.staj.WorkFlowProject.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public String logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return "Çıkış yapıldı";
    }
}