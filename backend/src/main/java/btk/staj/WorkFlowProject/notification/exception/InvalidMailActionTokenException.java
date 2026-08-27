package btk.staj.WorkFlowProject.notification.exception;

/**
 * E-posta aksiyon anahtari kullanilamaz durumda: bulunamadi, suresi doldu veya
 * daha once tuketildi.
 *
 * <p>Uc durum bilerek tek istisnaya indirgenmistir; ayrim yapmak, gecerli bir
 * anahtarin varligini dogrulayan bir kanal acardi.
 */
public class InvalidMailActionTokenException extends RuntimeException {

    public InvalidMailActionTokenException(String message) {
        super(message);
    }
}
