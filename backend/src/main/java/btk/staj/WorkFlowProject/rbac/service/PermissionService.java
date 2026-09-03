package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Sartnamedeki rol bazli yetki matrisinin sorgulanabilir hali.
 *
 * <p>Bu sinif <b>kural tasimaz</b>. Hangi rolun hangi durumda hangi aksiyonu
 * yapabilecegi bilgisi tek bir yerde, durum makinesinin gecis tablosunda
 * tutulur; buradaki metotlar o tabloya {@link TransitionRuleSource} portu
 * uzerinden sorar. Boylece yeni bir gecis eklendiginde iki ayri yerin
 * guncellenmesi gerekmez ve kurallarin kaynagi degistiginde (statik tablo,
 * veritabani) bu sinif etkilenmez.
 *
 * <p><b>Kapsam:</b> Permission ve "durum + aksiyon + rol" birlesiminin tabloda
 * tanimli olup olmadigini soyler. Aktorun kayitla kurmasi gereken iliski
 * (olusturan/atanan), aciklama zorunlulugu ve kilit kontrolu burada
 * dogrulanmaz. Nihai karar mercii daima
 * {@code WorkflowTransitionValidator}'dir; bu sinif arayuzde butonu gizlemek
 * veya controller seviyesinde erken elemek gibi amaclar icindir.
 */
@Component
public class PermissionService {

    private final TransitionRuleSource ruleSource;

    public PermissionService(TransitionRuleSource ruleSource) {
        this.ruleSource = Objects.requireNonNull(ruleSource, "ruleSource");
    }

    // ---------- Gecis gerektiren yetkiler ----------

    /** Calisanin taslagi Baskan Yardimcisina gondermesi. */
    public boolean canSendToReview(RoleId roleId, RecordStatus currentStatus, Set<String> permissions) {
        return isTransitionDefined(currentStatus, WorkflowAction.GONDER, roleId, permissions);
    }

    /** Calisanin geri donen kaydi duzeltip yeniden gondermesi. */
    public boolean canEditAndResendReturnedRecord(RoleId roleId, RecordStatus currentStatus, Set<String> permissions) {
        return isTransitionDefined(currentStatus, WorkflowAction.TEKRAR_GONDER, roleId, permissions);
    }

    /** Baskan Yardimcisinin kaydi Baskana iletmesi. */
    public boolean canForwardToBaskan(RoleId roleId, RecordStatus currentStatus, Set<String> permissions) {
        return isTransitionDefined(currentStatus, WorkflowAction.BASKANA_ILET, roleId, permissions);
    }

    /** Baskanin nihai onayi. */
    public boolean canApprove(RoleId roleId, RecordStatus currentStatus, Set<String> permissions) {
        return isTransitionDefined(currentStatus, WorkflowAction.ONAYLA, roleId, permissions);
    }

    /** Baskanin nihai reddi. */
    public boolean canReject(RoleId roleId, RecordStatus currentStatus, Set<String> permissions) {
        return isTransitionDefined(currentStatus, WorkflowAction.REDDET, roleId, permissions);
    }

    /** Kaydin olusturan Calisana geri gonderilmesi. */
    public boolean canReturnToCalisan(RoleId roleId, RecordStatus currentStatus, Set<String> permissions) {
        return isTransitionDefined(currentStatus, WorkflowAction.CALISANA_GERI_GONDER, roleId, permissions);
    }

    /** Baskanin kaydi ileten Baskan Yardimcisina geri gondermesi. */
    public boolean canReturnToBaskanYrd(RoleId roleId, RecordStatus currentStatus, Set<String> permissions) {
        return isTransitionDefined(currentStatus, WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, roleId, permissions);
    }

    // ---------- Durum bilinmeden yalnizca rol sorgulandiginda ----------

    /**
     * Bu rolun herhangi bir durumda Calisana geri gonderme yetkisi olup
     * olmadigi. Belirli bir kayit icin karar verirken durum bilgisini de alan
     * {@link #canReturnToCalisan(RoleId, RecordStatus, Set)} tercih edilmelidir.
     */
    public boolean canReturnToCalisan(RoleId roleId, Set<String> permissions) {
        return hasAnyTransition(WorkflowAction.CALISANA_GERI_GONDER, roleId, permissions);
    }

    /** Bu rolun herhangi bir durumda Baskan Yardimcisina geri gonderme yetkisi. */
    public boolean canReturnToBaskanYrd(RoleId roleId, Set<String> permissions) {
        return hasAnyTransition(WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, roleId, permissions);
    }

    // ---------- Gecis olmayan yetkiler ----------

    /**
     * Yeni kayit olusturma bir durum gecisi degildir; kayit dogrudan
     * {@code TASLAK} olarak dogar. Bu yuzden kural tablosunda karsiligi yoktur
     * ve capability kontrolu burada yapilir.
     */
    public boolean canCreateRecord(Set<String> permissions) {
        return permissions.contains("RECORD_CREATE");
    }

    /**
     * Kaydin icerigini olusturan Calisanin duzenleyebilecegi durumlar. Kilit
     * kurali durum makinesinde tutulur.
     *
     * <p>Silme islemi ayrica yalnizca {@code TASLAK} durumunda gecerlidir; bunu
     * record modulu kendi servisinde daraltir.
     */
    public boolean canEditRecord(Set<String> permissions, RecordStatus currentStatus) {
        return permissions.contains("RECORD_EDIT") && currentStatus.isEditableByCreator();
    }

    public boolean canDeleteRecord(Set<String> permissions, RecordStatus currentStatus) {
        return permissions.contains("RECORD_DELETE") && currentStatus == RecordStatus.TASLAK;
    }

    /** Terminal durumdaki kayit kilitlidir. */
    public boolean isRecordLocked(RecordStatus currentStatus) {
        return currentStatus.isTerminal();
    }

    // ---------- Aciklama zorunlulugu ----------

    /** Aksiyonun aciklama zorunlulugu; bilgi durum makinesinden gelir. */
    public boolean isCommentRequired(WorkflowAction action) {
        return action.isCommentRequired();
    }

    /**
     * Verilen aciklamanin geri gonderme icin yeterli olup olmadigi.
     * Yalnizca bosluk iceren metin kabul edilmez.
     */
    public boolean isCommentRequiredForReturn(String comment) {
        return comment != null && !comment.trim().isEmpty();
    }

    // ---------- Tabloya sorgu ----------

    private boolean isTransitionDefined(RecordStatus from, WorkflowAction action, RoleId roleId, Set<String> permissions) {
        return ruleSource.find(from, action, roleId)
                .filter(rule -> permissions.contains(rule.requiredPermissionCode())).isPresent();
    }

    private boolean hasAnyTransition(WorkflowAction action, RoleId roleId, Set<String> permissions) {
        return ruleSource.all().stream()
                .anyMatch(rule -> rule.action() == action && rule.actorRoleId().equals(roleId)
                        && permissions.contains(rule.requiredPermissionCode()));
    }
}
