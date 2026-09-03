package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;

/**
 * Tek bir durum gecisi kurali.
 *
 * <p>Kurallarin dogruluk kaynagi {@code workflow_transitions} tablosudur; bu tip yalnizca
 * bir satiri temsil eder. Test agacindaki {@code TransitionRules} ayni satirlari statik
 * olarak tutar ve parity testinin referansidir (TZ-1).
 *
 * @param from               gecisin uygulanabilecegi mevcut durum
 * @param action             uygulanan aksiyon
 * @param actorRoleId          aksiyonu yapabilecek rol
 * @param actorRequirement   aktorun kayitla kurmasi gereken iliski
 * @param to                 gecis basarili oldugunda kaydin alacagi durum
 * @param targetStrategy     hedef kullanicinin nasil cozulecegi
 * @param expectedTargetRoleId cozulen hedefin tasimasi gereken rol; hedef yoksa {@code null}
 */
public record TransitionRule(
        RecordStatus from,
        WorkflowAction action,
        RoleId actorRoleId,
        ActorRequirement actorRequirement,
        RecordStatus to,
        TargetStrategy targetStrategy,
        RoleId expectedTargetRoleId,
        String requiredPermissionCode) {

    public TransitionRule {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorRoleId, "actorRoleId");
        Objects.requireNonNull(actorRequirement, "actorRequirement");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(targetStrategy, "targetStrategy");
        if (requiredPermissionCode == null || requiredPermissionCode.isBlank()) {
            throw new IllegalArgumentException("requiredPermissionCode must not be blank");
        }

        // Hedef gerektiren her gecis beklenen rolu de tasimak zorundadir.
        //
        // Bu kural veritabanindaki CHECK'ten daha katidir: chk_transition_target_strategy
        // yalnizca ROLE icin rolu zorunlu kilar, CREATOR / CURRENT_ASSIGNEE /
        // PREVIOUS_ACTOR satirlarinda serbest birakir. Burada cift yonlu zorunlu tutmamizin
        // sebebi WorkflowApplicationService'in iki gecisli dogrulamasidir: hedef gerektiren
        // bir gecis, hedef henuz cozulmemisken WORKFLOW_TARGET_ROLE_INVALID ile
        // reddedilmelidir ve bu ancak beklenen rol doluysa gerceklesir. Rol bos olsaydi on
        // dogrulama gecisi kabul eder, servis de beklemedigi bir "Allowed" ile karsilasirdi.
        //
        // Seed edilmis sekiz gecisin tamami bu kosulu zaten saglar (DB-1 SS8).
        boolean targetExpected = targetStrategy != TargetStrategy.NONE;
        if (targetExpected && expectedTargetRoleId == null) {
            throw new IllegalArgumentException(
                    "targetStrategy " + targetStrategy + " requires expectedTargetRoleId");
        }
        if (!targetExpected && expectedTargetRoleId != null) {
            throw new IllegalArgumentException(
                    "targetStrategy NONE must not carry expectedTargetRoleId but was "
                            + expectedTargetRoleId);
        }
    }
}
