package btk.staj.WorkFlowProject.auth.exception;

/**
 * E-postayla gönderilen 6 haneli kod yanlış, süresi dolmuş, tüketilmiş veya
 * deneme hakkı bitmiş.
 *
 * <p>Mesaj bilerek tek ve ayrıntısız: hangi sebebin geçerli olduğunu söylemek
 * (özellikle "böyle bir talep yok") hesap varlığını sızdırır.
 */
public class InvalidResetCodeException extends RuntimeException {
    public InvalidResetCodeException(String message) {
        super(message);
    }
}
