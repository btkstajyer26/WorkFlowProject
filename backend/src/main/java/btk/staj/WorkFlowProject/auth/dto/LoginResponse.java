package btk.staj.WorkFlowProject.auth.dto;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private boolean mustChangePassword;

    public LoginResponse(String accessToken, String refreshToken, boolean mustChangePassword) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.mustChangePassword = mustChangePassword;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
