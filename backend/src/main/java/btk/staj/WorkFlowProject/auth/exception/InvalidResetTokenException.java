package btk.staj.WorkFlowProject.auth.exception;

/**
 * Kod doğrulandıktan sonra verilen tek kullanımlık sıfırlama anahtarı geçersiz,
 * kullanılmış veya süresi dolmuş.
 */
public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException(String message) {
        super(message);
    }
}
