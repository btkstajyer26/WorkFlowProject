package btk.staj.WorkFlowProject.common.exception;

/**
 * Rol akista kullanildigi icin istenen degisiklik reddedildi.
 *
 * <p>{@link BusinessRuleException}'dan ayri bir tip olmasinin sebebi HTTP durum
 * kodudur: bu, gecici bir <strong>catisma</strong> durumudur (kayitlar
 * tamamlaninca istek basarili olur), gecersiz bir istek degil. WF-8'in ayni
 * anlamdaki {@code BINDING_IN_USE} hatasi da {@code 409} donuyor; iki yolun ayni
 * durumda farkli kod donmesi sozlesme tutarsizligi olurdu.
 */
public class RoleInUseException extends RuntimeException {
    public RoleInUseException(String message) {
        super(message);
    }
}
