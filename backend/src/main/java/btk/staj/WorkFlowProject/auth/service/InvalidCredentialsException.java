package btk.staj.WorkFlowProject.auth.service; // AuthService ile aynı paket

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}