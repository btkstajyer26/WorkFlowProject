package btk.staj.WorkFlowProject.department.port;

/**
 * Departman yonetiminin ihtiyac duydugu tek workflow gercegi: bu departmana
 * atanmis, hala islem bekleyen bir kayit var mi.
 *
 * <p>Arayuzu <strong>tuketici</strong> ({@code department}) tanimlar, altyapiyi
 * {@code workflow} uygular - {@code rbac.port.WorkflowRoleUsagePort} ile ayni
 * gerekce (architecture.md "Katmanlama kurallari").
 */
public interface DepartmentOpenUsagePort {

    /**
     * Departmana atanmis, silinmemis ve terminal olmayan durumda en az bir
     * kayit var mi. Kontrol muhafazakardir: departmanin aktifligi veya
     * routing kuralinin varligi sonucu daraltmaz.
     *
     * @param departmentId departman kimligi
     * @return en az bir acik kayit varsa {@code true}
     */
    boolean hasOpenRecords(int departmentId);
}
