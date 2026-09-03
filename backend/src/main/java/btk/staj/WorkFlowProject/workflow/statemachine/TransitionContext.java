package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;
import java.util.Set;

/**
 * Bir gecis denemesinin dogrulanmasi icin gereken butun girdiler.
 *
 * <p>Servis katmani veriyi toplar (kayit, aktor, cozulmus hedef kullanici),
 * karari {@link WorkflowTransitionValidator} verir. Bu tipin veritabani veya
 * HTTP ile hicbir baglantisi yoktur.
 *
 * @param currentStatus           kaydin gecis oncesindeki durumu
 * @param action                  uygulanmak istenen aksiyon
 * @param actorRoleId               aksiyonu yapan kullanicinin rolu
 * @param actorIsCreator          aktor {@code records.created_by} mu
 * @param actorHoldsAssignment    atama aktorde mi. Bugun yalnizca
 *                                {@code records.assigned_to} esitligi; ADR-0005 geregi
 *                                departmana atanmis kayitta aktorun departman uyeligi ve
 *                                akis kuralinin isaret ettigi rol de bu bayragi dogru yapar
 *                                (WF-6 ile gelir). Validator ayrimi gormez; yetkinin
 *                                kaynagi servis katmanindaki cozumleyici sonucunda durur
 * @param comment                 istekteki aciklama; yoksa {@code null}
 * @param targetProvidedInRequest istemci istekte {@code targetUserId} gonderdi mi
 * @param targetRoleId              servis tarafindan cozulen hedef kullanicinin rolu;
 *                                hedef yoksa veya cozulemediyse {@code null}
 * @param targetActive            cozulen hedef kullanicinin {@code is_active} degeri
 */
public record TransitionContext(
        RecordStatus currentStatus,
        WorkflowAction action,
        RoleId actorRoleId,
        boolean actorIsCreator,
        boolean actorHoldsAssignment,
        String comment,
        boolean targetProvidedInRequest,
        RoleId targetRoleId,
        boolean targetActive,
        boolean actorWorkflowActor,
        Set<String> actorPermissionCodes) {

    public TransitionContext {
        Objects.requireNonNull(currentStatus, "currentStatus");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorRoleId, "actorRoleId");
        actorPermissionCodes = Set.copyOf(actorPermissionCodes);
    }

    /** Aciklamanin dolu olup olmadigi. Yalnizca bosluktan olusan metin bos sayilir. */
    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }
}
