package btk.staj.WorkFlowProject.auth.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal ;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.auth.dto.LoginRequest;
import btk.staj.WorkFlowProject.auth.dto.LoginResponse;
import btk.staj.WorkFlowProject.auth.dto.LogoutRequest;
import btk.staj.WorkFlowProject.auth.dto.RefreshTokenRequest;
import btk.staj.WorkFlowProject.auth.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import btk.staj.WorkFlowProject.auth.dto.ChangePasswordRequest;

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

    /**
     * Charset acikca bildirilmezse Spring'in String donusturucusu ISO-8859-1
     * kullanir ve mesajdaki Turkce karakterler bozulur.
     */
    @PostMapping(value = "/logout", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return "Çıkış yapıldı";
    }
    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                 @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
        return "Şifre değiştirildi";
    }
}