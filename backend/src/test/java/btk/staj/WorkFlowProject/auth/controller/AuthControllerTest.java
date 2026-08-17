package btk.staj.WorkFlowProject.auth.controller;

import btk.staj.WorkFlowProject.auth.dto.LoginRequest;
import btk.staj.WorkFlowProject.auth.dto.LoginResponse;
import btk.staj.WorkFlowProject.auth.dto.LogoutRequest;
import btk.staj.WorkFlowProject.auth.dto.RefreshTokenRequest;
import btk.staj.WorkFlowProject.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import btk.staj.WorkFlowProject.common.exception.GlobalExceptionHandler;
import btk.staj.WorkFlowProject.common.exception.InvalidCredentialsException;

/**
 * AuthController için MockMvc standalone testleri.
 *
 * <p>Uygulamanın gerçek {@link GlobalExceptionHandler}'ı devreye alınır;
 * böylece testler hata gövdesinin üretimdeki {@code ApiError} sözleşmesiyle
 * aynı olduğunu doğrular. Spring Security context'i devreye girmez, sadece
 * controller + hata yönetimi katmanı test edilir.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ==================== LOGIN ====================

    @Test
    @DisplayName("Geçerli istekle login yapılırsa 200 ve tokenlar dönmeli")
    void login_gecerliIstekle_200veTokenlariDonmeli() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("sifre123");

        LoginResponse expectedResponse = new LoginResponse("access-token", "refresh-token", false);
        when(authService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService, times(1)).login(captor.capture());
        assertEquals("test@example.com", captor.getValue().getEmail());
        assertEquals("sifre123", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("Geçersiz bilgiyle login yapılırsa 401 ve hata mesajı dönmeli")
    void login_gecersizBilgiyle_401veHataMesajiDonmeli() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("yanlis@example.com");
        request.setPassword("yanlis");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Email veya şifre hatalı"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                // Filtre zincirinin UNAUTHORIZED reddinden ayrilan is katmani kodu.
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email veya şifre hatalı"))
                .andExpect(jsonPath("$.status").value(401));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Content-Type belirtilmeden login isteği atılırsa 415 dönmeli")
    void login_contentTypeBelirtilmezse_415Donmeli() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("sifre123");

        mockMvc.perform(post("/api/auth/login")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Bozuk JSON gövdesiyle login isteği atılırsa 400 dönmeli")
    void login_bozukJsonIle_400Donmeli() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ bozuk-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(authService);
    }

    // ==================== REFRESH ====================

    @Test
    @DisplayName("Geçerli refresh token ile yeni access token dönmeli")
    void refresh_gecerliTokenIle_yeniAccessTokenDonmeli() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        LoginResponse expectedResponse = new LoginResponse("new-access-token", "valid-refresh-token",false);
        when(authService.refresh(anyString())).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));

        verify(authService, times(1)).refresh("valid-refresh-token");
    }

    @Test
    @DisplayName("Geçersiz refresh token ile 401 ve hata mesajı dönmeli")
    void refresh_gecersizTokenIle_401veHataMesajiDonmeli() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        when(authService.refresh("invalid-refresh-token"))
                .thenThrow(new InvalidCredentialsException("Geçersiz refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Geçersiz refresh token"));

        verify(authService, times(1)).refresh("invalid-refresh-token");
    }

    @Test
    @DisplayName("Süresi dolmuş refresh token ile 401 dönmeli")
    void refresh_suresiDolmusTokenIle_401Donmeli() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        when(authService.refresh("expired-token"))
                .thenThrow(new InvalidCredentialsException("Refresh token süresi dolmuş veya geçersiz"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Refresh token süresi dolmuş veya geçersiz"));
    }

    // ==================== LOGOUT ====================

    @Test
    @DisplayName("Geçerli token ile logout yapılırsa 200 ve başarı mesajı dönmeli")
    void logout_gecerliTokenIle_basariMesajiDonmeli() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("token-to-revoke");

        doNothing().when(authService).logout("token-to-revoke");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Çıkış yapıldı"));

        verify(authService, times(1)).logout("token-to-revoke");
    }

    @Test
    @DisplayName("Var olmayan token ile logout yapılsa bile 200 dönmeli (idempotent davranış)")
    void logout_olmayanTokenIle_yineDe200Donmeli() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("olmayan-token");

        // AuthService.logout, token bulunamazsa sessizce hiçbir şey yapmıyor (exception fırlatmıyor)
        doNothing().when(authService).logout("olmayan-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Çıkış yapıldı"));

        verify(authService, times(1)).logout("olmayan-token");
    }

    @Test
    @DisplayName("logout sırasında beklenmeyen hata olursa 500 dönmeli ve ayrıntı sızmamalı")
    void logout_beklenmeyenHataFirlatirsa_500Donmeli() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("bad-token");

        // AuthService.logout kimlik dogrulamaz, yalnizca token'i iptal eder.
        // Buradan gelen bir hata kimlik hatasi degil sunucu hatasidir.
        doThrow(new RuntimeException("Token işlenemedi"))
                .when(authService).logout("bad-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Beklenmeyen bir hata oluştu"));
    }
}