package btk.staj.WorkFlowProject.rbac.port;

/**
 * Rol yonetiminin ihtiyac duydugu tek workflow gercegi: bu rol su anda akista
 * kullaniliyor mu?
 *
 * <p>Arayuzu <strong>tuketici</strong> ({@code rbac}) tanimlar, altyapiyi
 * {@code workflow} uygular. Boylece {@code rbac} baska bir modulun repository
 * katmanina bagimli olmaz (architecture.md "Katmanlama kurallari") ve
 * {@code workflow -> rbac} yonunde zaten var olan bagimliliga yeni bir dongu
 * kenari eklenmez. Ayni desenin tersi kod tabaninda mevcuttur:
 * {@code workflow/port/AuditService} arayuzunu {@code audit} modulu uygular.
 */
public interface WorkflowRoleUsagePort {

    /**
     * Rolun aktor olarak bagli oldugu aktif gecislerden en az birinde islem
     * bekleyen acik kayit var mi.
     *
     * <p>Kontrol WF-8'in kaldirma korumasiyla ayni sorguyu kullanir ve ayni
     * sekilde muhafazakardir: kullanici/rol pasifligi veya permission eksikligi
     * sonucu daraltmaz, boylece gecici yetki kaldirma ile korunma asilamaz.
     *
     * @param roleId rol kimligi
     * @return en az bir acik kayit varsa {@code true}
     */
    boolean hasOpenWorkflowUsage(int roleId);
}
