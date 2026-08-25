package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;

/**
 * Tek bir durum gecisi kurali.
 *
 * <p>Kurallarin tamami {@link TransitionRules} icindeki merkezi tabloda tutulur;
 * bu tip yalnizca bir satiri temsil eder.
 *
 * @param from             gecisin uygulanabilecegi mevcut durum
 * @param action           uygulanan aksiyon
 * @param actorRole        aksiyonu yapabilecek rol
 * @param actorRequirement aktorun kayitla kurmasi gereken iliski
 * @param to               gecis basarili oldugunda kaydin alacagi durum
 */
public record TransitionRule(
        RecordStatus from,
        WorkflowAction action,
        RoleName actorRole,
        ActorRequirement actorRequirement,
        RecordStatus to) {

    public TransitionRule {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorRole, "actorRole");
        Objects.requireNonNull(actorRequirement, "actorRequirement");
        Objects.requireNonNull(to, "to");
    }
}
