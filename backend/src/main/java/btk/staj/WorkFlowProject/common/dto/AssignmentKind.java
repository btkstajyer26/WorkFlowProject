package btk.staj.WorkFlowProject.common.dto;

/**
 * Bir kaydin atama turu (B11).
 *
 * <p>Istemci atama turunu iki nullable alani karsilastirarak <strong>cikarsamaz</strong>;
 * bu ayrim uzerinden dallanir. DB'deki karsilikli dislama kisiti
 * ({@code chk_records_assignment_exclusive}) boylece tel biciminde de gorunur olur ve
 * "ikisi de bos" ile "departman kuyrugu" durumu istemcide karismaz.
 */
public enum AssignmentKind {
    /** Kayit bir kisiye atanmis. */
    USER,
    /** Kayit bir departman kuyrugunda. */
    DEPARTMENT,
    /** Atama yok; {@code TASLAK} ve terminal durumlar. Gecerli bir durumdur, hata degil. */
    NONE
}
