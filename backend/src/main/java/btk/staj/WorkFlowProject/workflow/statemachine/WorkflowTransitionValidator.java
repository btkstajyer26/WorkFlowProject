package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;
import java.util.Optional;

/**
 * Durum makinesinin kural motoru. Bir gecis denemesinin gecerli olup olmadigina
 * karar verir ve gecerliyse hedef durumu doner.
 *
 * <p>Bu sinifin hicbir repository, Spring veya HTTP bagimliligi yoktur;
 * {@code new} ile orneklenip altyapi olmadan test edilebilir. Butun gecis
 * kurallari yalnizca burada uygulanir &ndash; servis katmaninda tekrar edilmemelidir.
 *
 * <p>Gecis tablosuna dogrudan degil, {@link TransitionRuleSource} portu
 * uzerinden erisir; kurallarin nereden geldigini (statik tablo, veritabani)
 * bilmez.
 *
 * <p>Kontrol sirasi bilerek sabittir: daha genel ve daha ucuz kontroller once
 * calisir, boylece dondurulen hata kodu her zaman en anlamli sebebi gosterir.
 */
public class WorkflowTransitionValidator {

    private final TransitionRuleSource ruleSource;

    public WorkflowTransitionValidator(TransitionRuleSource ruleSource) {
        this.ruleSource = Objects.requireNonNull(ruleSource, "ruleSource");
    }

    /**
     * Verilen baglamdaki gecisi dogrular.
     *
     * @return izinliyse hedef durumu tasiyan {@link TransitionDecision.Allowed},
     *         degilse sebebi tasiyan {@link TransitionDecision.Rejected}
     */
    public TransitionDecision validate(TransitionContext context) {

        // 1. Workflow aktoru olmayan roller (ADMIN) hicbir gecis yapamaz.
        if (!context.actorRole().isWorkflowActor()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_ROLE_NOT_ALLOWED);
        }

        // 2. Terminal kayit kilitlidir.
        if (context.currentStatus().isTerminal()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_RECORD_LOCKED);
        }

        // 3. Durum + aksiyon + rol birlesimi tabloda tanimli mi?
        Optional<TransitionRule> rule = ruleSource.find(
                context.currentStatus(), context.action(), context.actorRole());
        if (rule.isEmpty()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION);
        }

        // 4. Aktor kaydin sahibi ve/veya atanani mi?
        if (!rule.get().actorRequirement().isSatisfiedBy(context.actorIsCreator(), context.actorIsAssignee())) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        }

        // 5. Aciklama zorunluysa dolu mu? (yalnizca bosluk kabul edilmez)
        if (context.action().isCommentRequired() && !context.hasComment()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_COMMENT_REQUIRED);
        }

        // 6-7. Istekteki hedef alani, aksiyonun bekledigiyle uyusuyor mu?
        boolean targetExpectedInRequest = context.action().isTargetUserIdRequiredInRequest();
        if (targetExpectedInRequest && !context.targetProvidedInRequest()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_REQUIRED);
        }
        if (!targetExpectedInRequest && context.targetProvidedInRequest()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_NOT_ALLOWED);
        }

        // 8-9. Hedef kullanici gerektiren aksiyonlarda hedefin rolu ve aktifligi.
        RoleName expectedTargetRole = context.action().getExpectedTargetRole();
        if (expectedTargetRole != null) {
            // ADMIN veya yanlis roldeki hedef burada elenir. Hedef cozulememisse
            // (null) yine gecersiz sayilir; servis bu durumu zaten daha once
            // WORKFLOW_ROLE_NOT_CONFIGURED ile durdurmus olmalidir.
            if (context.targetRole() != expectedTargetRole) {
                return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID);
            }
            if (!context.targetActive()) {
                return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_INACTIVE);
            }
        }

        return TransitionDecision.allowed(rule.get().to());
    }
}
