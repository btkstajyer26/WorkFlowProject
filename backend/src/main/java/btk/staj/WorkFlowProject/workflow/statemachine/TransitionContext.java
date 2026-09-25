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
 *                                (WF-6). Validator ayrimi gormez; yetkinin
 *                                kaynagi servis katmanindaki cozumleyici sonucunda durur
 * @param comment                 istekteki aciklama; yoksa {@code null}
 * @param targetUserProvidedInRequest istemci targetUserId gonderdi mi
 * @param targetDepartmentProvidedInRequest istemci targetDepartmentId gonderdi mi
 * @param targetRoleId              servis tarafindan cozulen hedef kullanicinin rolu;
 *                                hedef yoksa veya cozulemediyse {@code null}
 * @param targetActive            cozulen hedef kullanicinin {@code is_active} degeri
 * @param actorWorkflowActor      aktorun rolu workflow aktoru olabilir mi
 * @param actorPermissionCodes    aktorun aktif permission kodlari
 * @param targetResolutionPending hedef henuz cozulmedi mi. Iki asamali dogrulamanin
 *                                ilk asamasinda {@code true}; validator bu durumda
 *                                hedefe bagli kontrolleri atlar ve
 *                                {@link TransitionDecision.Pending} doner (ADR-0008 K3)
 * @param targetIsCreator         cozulen hedef {@code records.created_by} mu. Hedefin
 *                                iniş durumundaki yetenegi hesaplanirken gerekir:
 *                                {@code actor_requirement} CREATOR olan bir gecis
 *                                yalnizca olusturan icin saglanir (K4.3)
 * @param targetWorkflowActor     cozulen hedefin rolu workflow aktoru olabilir mi (K4.2)
 * @param targetPermissionCodes   cozulen hedefin aktif permission kodlari (K4.3)
 */
public record TransitionContext(
        RecordStatus currentStatus,
        WorkflowAction action,
        RoleId actorRoleId,
        boolean actorIsCreator,
        boolean actorHoldsAssignment,
        String comment,
        boolean targetUserProvidedInRequest,
        boolean targetDepartmentProvidedInRequest,
        RoleId targetRoleId,
        boolean targetActive,
        boolean actorWorkflowActor,
        Set<String> actorPermissionCodes,
        // ADR-0008 ile eklenen hedef alanlari. Mevcut bilesenlerin arasina degil SONUNA
        // eklendiler: aradaki dort boolean'in sirasi degisseydi cagri yerleri sessizce
        // yanlis anlam kazanirdi, derleyici yakalayamazdi.
        boolean targetResolutionPending,
        boolean targetIsCreator,
        boolean targetWorkflowActor,
        Set<String> targetPermissionCodes) {

    public TransitionContext {
        Objects.requireNonNull(currentStatus, "currentStatus");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorRoleId, "actorRoleId");
        actorPermissionCodes = Set.copyOf(actorPermissionCodes);
        targetPermissionCodes = Set.copyOf(targetPermissionCodes);
    }

    /** Aciklamanin dolu olup olmadigi. Yalnizca bosluktan olusan metin bos sayilir. */
    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }
}
