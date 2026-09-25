package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;
import java.util.Set;

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
 * @param expectedTargetRoleId ROLE stratejisinde hedefin aranacagi rol; diger butun
 *                             stratejilerde {@code null}
 * @param requiredPermissionCode ek capability permission; yalniz SYSTEM aktor
 *                               gereksiniminde {@code null} olabilir
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
        if (requiredPermissionCode != null && requiredPermissionCode.isBlank()) {
            throw new IllegalArgumentException("requiredPermissionCode must not be blank");
        }
        if (actorRequirement != ActorRequirement.SYSTEM && requiredPermissionCode == null) {
            throw new IllegalArgumentException(
                    "requiredPermissionCode must not be null for human actor requirements");
        }

        // expected_target_role_id YALNIZ ROLE stratejisinin arama anahtaridir (ADR-0008 K2).
        //
        // V24'teki chk_transition_target_strategy_role'un Java karsiligidir; kisit ile
        // invariant artik birebir ayni seyi soyler.
        //
        // Eskiden kolon CREATOR / CURRENT_ASSIGNEE / PREVIOUS_ACTOR icin de zorunluydu,
        // ama gerekcesi hedefin rolu degildi: iki asamali dogrulamanin nobetcisi bu kolona
        // dayaniyordu. Nobetci artik target_strategy'den turetilen
        // TransitionDecision.Pending oldugu icin o zorunluluk ortadan kalkti - ve onunla
        // birlikte, calisma zamaninda belirlenen bir hedefe yerlesik rol dayatan B02 de.
        if (targetStrategy == TargetStrategy.ROLE && expectedTargetRoleId == null) {
            throw new IllegalArgumentException(
                    "targetStrategy ROLE requires expectedTargetRoleId");
        }
        if (targetStrategy != TargetStrategy.ROLE && expectedTargetRoleId != null) {
            throw new IllegalArgumentException(
                    "targetStrategy " + targetStrategy
                            + " must not carry expectedTargetRoleId but was "
                            + expectedTargetRoleId);
        }
    }

    /**
     * Gecisin ek capability kosulunu aktor permission'lariyla karsilastirir.
     * Yalniz {@link ActorRequirement#SYSTEM} kurali permission tasimayabilir;
     * bu durumda rol eslesmesi ve workflow aktorlugu validator'da korunur.
     */
    public boolean hasRequiredPermission(Set<String> permissionCodes) {
        Objects.requireNonNull(permissionCodes, "permissionCodes");
        return requiredPermissionCode == null || permissionCodes.contains(requiredPermissionCode);
    }
}
