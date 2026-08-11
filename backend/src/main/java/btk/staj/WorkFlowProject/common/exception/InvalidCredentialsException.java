package btk.staj.WorkFlowProject.common.exception;

/**
 * Giris bilgileri (email/sifre) hatali oldugunda veya refresh token
 * gecersiz/suresi dolmus oldugunda firlatilir. GlobalExceptionHandler
 * bunu 401 Unauthorized olarak esler.
 *
 * <p>Not: ForbiddenException'dan (403 - yetkisiz islem) farklidir; bu
 * exception kimligin dogrulanamadigi durumlar icindir.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}