package btk.staj.WorkFlowProject.auth.exception;

/**
 * Yeni şifre, hesabın hâlihazırdaki şifresiyle aynı.
 *
 * <p>Hem oturum içi şifre değiştirmede hem de "şifremi unuttum" akışında
 * uygulanır; arayüz bu kodu alanın kendi hatasına çevirebilsin diye
 * {@code BUSINESS_RULE_VIOLATION}'dan ayrı bir tip.
 */
public class PasswordReuseException extends RuntimeException {
    public PasswordReuseException(String message) {
        super(message);
    }
}
