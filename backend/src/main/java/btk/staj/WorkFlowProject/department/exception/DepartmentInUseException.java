package btk.staj.WorkFlowProject.department.exception;

/**
 * Departman acik kuyrukta kullanildigi icin istenen degisiklik reddedildi.
 *
 * <p>{@link btk.staj.WorkFlowProject.common.exception.BusinessRuleException}'dan
 * ayri bir tip olmasinin sebebi HTTP durum kodudur: bu gecici bir
 * <strong>catisma</strong> durumudur (kayitlar tamamlaninca istek basarili
 * olur), gecersiz bir istek degil - {@code RoleInUseException} ile ayni gerekce.
 */
public class DepartmentInUseException extends RuntimeException {
    public DepartmentInUseException(String message) {
        super(message);
    }
}
