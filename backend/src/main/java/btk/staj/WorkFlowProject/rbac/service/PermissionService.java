package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
 * <p><b>Kapsam:</b> Yalnizca "durum + aksiyon + rol" birlesiminin tabloda
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
    public boolean canSendToReview(RoleName role, RecordStatus currentStatus) {
        return isTransitionDefined(currentStatus, WorkflowAction.GONDER, role);
    }

    /** Calisanin geri donen kaydi duzeltip yeniden gondermesi. */
    public boolean canEditAndResendReturnedRecord(RoleName role, RecordStatus currentStatus) {
        return isTransitionDefined(currentStatus, WorkflowAction.TEKRAR_GONDER, role);
    }

    /** Baskan Yardimcisinin kaydi Baskana iletmesi. */
    public boolean canForwardToBaskan(RoleName role, RecordStatus currentStatus) {
        return isTransitionDefined(currentStatus, WorkflowAction.BASKANA_ILET, role);
    }

    /** Baskanin nihai onayi. */
    public boolean canApprove(RoleName role, RecordStatus currentStatus) {
        return isTransitionDefined(currentStatus, WorkflowAction.ONAYLA, role);
    }

    /** Baskanin nihai reddi. */
    public boolean canReject(RoleName role, RecordStatus currentStatus) {
        return isTransitionDefined(currentStatus, WorkflowAction.REDDET, role);
    }

    /** Kaydin olusturan Calisana geri gonderilmesi. */
    public boolean canReturnToCalisan(RoleName role, RecordStatus currentStatus) {
        return isTransitionDefined(currentStatus, WorkflowAction.CALISANA_GERI_GONDER, role);
    }

    /** Baskanin kaydi ileten Baskan Yardimcisina geri gondermesi. */
    public boolean canReturnToBaskanYrd(RoleName role, RecordStatus currentStatus) {
        return isTransitionDefined(currentStatus, WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, role);
    }

    // ---------- Durum bilinmeden yalnizca rol sorgulandiginda ----------

    /**
     * Bu rolun herhangi bir durumda Calisana geri gonderme yetkisi olup
     * olmadigi. Belirli bir kayit icin karar verirken durum bilgisini de alan
     * {@link #canReturnToCalisan(RoleName, RecordStatus)} tercih edilmelidir.
     */
    public boolean canReturnToCalisan(RoleName role) {
        return hasAnyTransition(WorkflowAction.CALISANA_GERI_GONDER, role);
    }

    /** Bu rolun herhangi bir durumda Baskan Yardimcisina geri gonderme yetkisi. */
    public boolean canReturnToBaskanYrd(RoleName role) {
        return hasAnyTransition(WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, role);
    }

    // ---------- Gecis olmayan yetkiler ----------

    /**
     * Yeni kayit olusturma bir durum gecisi degildir; kayit dogrudan
     * {@code TASLAK} olarak dogar. Bu yuzden kural tablosunda karsiligi yoktur
     * ve rol kontrolu burada yapilir.
     */
    public boolean canCreateRecord(RoleName role) {
        return role == RoleName.CALISAN;
    }

    /**
     * Kaydin icerigini olusturan Calisanin duzenleyebilecegi durumlar. Kilit
     * kurali durum makinesinde tutulur.
     *
     * <p>Silme islemi ayrica yalnizca {@code TASLAK} durumunda gecerlidir; bunu
     * record modulu kendi servisinde daraltir.
     */
    public boolean canEditOrDeleteDraft(RoleName role, RecordStatus currentStatus) {
        return role == RoleName.CALISAN && currentStatus.isEditableByCreator();
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

    private boolean isTransitionDefined(RecordStatus from, WorkflowAction action, RoleName role) {
        return ruleSource.find(from, action, role).isPresent();
    }

    private boolean hasAnyTransition(WorkflowAction action, RoleName role) {
        return ruleSource.all().stream()
                .anyMatch(rule -> rule.action() == action && rule.actorRole() == role);
    }
}
