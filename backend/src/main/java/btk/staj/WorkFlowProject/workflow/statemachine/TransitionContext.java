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
 * @param actorRole               aksiyonu yapan kullanicinin rolu
 * @param actorIsCreator          aktor {@code records.created_by} mu
 * @param actorIsAssignee         aktor {@code records.assigned_to} mu
 * @param comment                 istekteki aciklama; yoksa {@code null}
 * @param targetProvidedInRequest istemci istekte {@code targetUserId} gonderdi mi
 * @param targetRole              servis tarafindan cozulen hedef kullanicinin rolu;
 *                                hedef yoksa veya cozulemediyse {@code null}
 * @param targetActive            cozulen hedef kullanicinin {@code is_active} degeri
 */
public record TransitionContext(
        RecordStatus currentStatus,
        WorkflowAction action,
        RoleName actorRole,
        boolean actorIsCreator,
        boolean actorIsAssignee,
        String comment,
        boolean targetProvidedInRequest,
        RoleName targetRole,
        boolean targetActive,
        boolean actorWorkflowActor,
        Set<String> actorPermissionCodes) {

    public TransitionContext {
        Objects.requireNonNull(currentStatus, "currentStatus");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorRole, "actorRole");
        actorPermissionCodes = Set.copyOf(actorPermissionCodes);
    }

    /** Aciklamanin dolu olup olmadigi. Yalnizca bosluktan olusan metin bos sayilir. */
    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }
}
