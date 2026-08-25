package btk.staj.WorkFlowProject.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    private String refreshToken;

    /**
     * Mobil istemcinin FCM cihaz token'i. Opsiyoneldir: web bu alani
     * gondermez ve gondermemesi bir hata degildir. Gonderildiginde cihaz
     * token'i ayni islemde pasiflestirilir, boylece cikis yapan cihaza
     * bildirim gitmez.
     */
    private String deviceToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
}