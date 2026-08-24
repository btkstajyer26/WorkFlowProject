package btk.staj.WorkFlowProject.auth.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal ;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.auth.dto.LoginRequest;
import btk.staj.WorkFlowProject.auth.dto.LoginResponse;
import btk.staj.WorkFlowProject.auth.dto.LogoutRequest;
import btk.staj.WorkFlowProject.auth.dto.RefreshTokenRequest;
import btk.staj.WorkFlowProject.auth.service.AuthService;
import btk.staj.WorkFlowProject.auth.service.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import btk.staj.WorkFlowProject.auth.dto.ChangePasswordRequest;
import btk.staj.WorkFlowProject.auth.dto.ForgotPasswordRequest;
import btk.staj.WorkFlowProject.auth.dto.ResetPasswordRequest;
import btk.staj.WorkFlowProject.auth.dto.VerifyResetCodeRequest;
import btk.staj.WorkFlowProject.auth.dto.VerifyResetCodeResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
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
        authService.logout(request.getRefreshToken(), request.getDeviceToken());
        return "Çıkış yapıldı";
    }
    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                 @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
        return "Şifre değiştirildi";
    }

    /**
     * Şifre sıfırlama kodu ister.
     *
     * <p>Adres kayıtlı olsun olmasın 202 döner: farklı bir cevap, ucu kayıtlı
     * e-postaları keşfetmek için kullanılabilir hâle getirirdi.
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestCode(request.getEmail());
    }

    /** E-postayla gelen 6 haneli kodu doğrular ve şifre değiştirme anahtarını verir. */
    @PostMapping("/verify-reset-code")
    public VerifyResetCodeResponse verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        String resetToken = passwordResetService.verifyCode(request.getEmail(), request.getCode());
        return new VerifyResetCodeResponse(resetToken, passwordResetService.tokenTtlSeconds());
    }

    /** Doğrulanmış anahtarla yeni şifreyi kaydeder (oturum gerektirmez). */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }
}