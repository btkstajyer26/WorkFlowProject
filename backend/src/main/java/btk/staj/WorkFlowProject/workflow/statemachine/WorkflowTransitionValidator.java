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
        return validate(context, ruleSource.snapshot());
    }

    /** Both validation passes of an action must use its captured rule snapshot. */
    public TransitionDecision validate(TransitionContext context, TransitionRuleSource snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        // 1. Workflow aktoru olmayan roller (ADMIN) hicbir gecis yapamaz.
        if (!context.actorWorkflowActor()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_ROLE_NOT_ALLOWED);
        }

        // 2. Terminal kayit kilitlidir.
        if (context.currentStatus().isTerminal()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_RECORD_LOCKED);
        }

        // 3. Durum + aksiyon + rol birlesimi tabloda tanimli mi?
        Optional<TransitionRule> rule = snapshot.find(
                context.currentStatus(), context.action(), context.actorRoleId());
        if (rule.isEmpty()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION);
        }

        // 4. Aktor iliskisi ve gecisin gerekli permission'i birlikte saglanmali.
        if (!rule.get().actorRequirement().isSatisfiedBy(context.actorIsCreator(), context.actorHoldsAssignment())
                || !context.actorPermissionCodes().contains(rule.get().requiredPermissionCode())) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        }

        // 5. Aciklama zorunluysa dolu mu? (yalnizca bosluk kabul edilmez)
        if (context.action().isCommentRequired() && !context.hasComment()) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_COMMENT_REQUIRED);
        }

        // 6-7. Istekteki hedef alani, aksiyonun bekledigiyle uyusuyor mu?
        boolean anyTargetProvided = context.targetUserProvidedInRequest()
                || context.targetDepartmentProvidedInRequest();
        if (context.action().isTargetExpectedInRequest() && !anyTargetProvided) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_REQUIRED);
        }
        if ((context.targetUserProvidedInRequest() && !context.action().isTargetUserIdRequiredInRequest())
                || (context.targetDepartmentProvidedInRequest()
                    && !context.action().isTargetDepartmentIdRequiredInRequest())) {
            return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_NOT_ALLOWED);
        }

        // 8-9. Hedef kullanici gerektiren gecislerde hedefin rolu ve aktifligi.
        //
        // Beklenen rol aksiyonun degil GECISIN ozelligidir: ayni aksiyon farkli gecislerde
        // farkli hedefe gidebilir (DB-1 SS6.5). Ornegin CALISANA_GERI_GONDER hem Baskan
        // Yardimcisinin hem Baskanin kullandigi iki ayri satirda bulunur.
        // 8. Hedef gerektiren gecislerde hedefin cozulmesi beklenir.
        //
        // "Bu gecis hedef ister" bilgisi artik target_strategy'den turetilir, beklenen
        // hedef rol kolonundan degil (ADR-0008 K3). Kolon yalnizca ROLE stratejisinin
        // arama anahtaridir; CREATOR / CURRENT_ASSIGNEE / PREVIOUS_ACTOR satirlarinda
        // bostur ve hicbir dogrulamada okunmaz.
        if (requiresTargetUser(rule.get())) {
            if (context.targetResolutionPending()) {
                return TransitionDecision.pending();
            }

            // 9. Cozulen hedefin kimligi: yanlis rol yalnizca ROLE stratejisinde anlamlidir.
            RoleId expectedTargetRoleId = rule.get().expectedTargetRoleId();
            if (expectedTargetRoleId != null && !expectedTargetRoleId.equals(context.targetRoleId())) {
                return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID);
            }

            // 10. Hedefin yetenegi: kayit, onunla hicbir sey yapamayacak birine atanmasin.
            // Statik rol dayatmasinin yerine gecen kontrol budur (ADR-0008 K4).
            if (!context.targetActive()) {
                return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_INACTIVE);
            }
            if (!context.targetWorkflowActor()
                    || !canActInLandingStatus(rule.get(), context, snapshot)) {
                return TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_TARGET_CANNOT_ACT);
            }
        }

        return TransitionDecision.allowed(rule.get().to());
    }

    /**
     * Gecis bir hedef kullaniciya ihtiyac duyuyor mu.
     *
     * <p>{@code DEPARTMENT} muaftir: hedefi bir kullanici degil departmandir ve
     * uygunluk {@code department_routing_rules} uzerinden servis katmaninda cozulur.
     */
    private static boolean requiresTargetUser(TransitionRule rule) {
        return rule.targetStrategy() != TargetStrategy.NONE
                && rule.targetStrategy() != TargetStrategy.DEPARTMENT;
    }

    /**
     * Hedef, kaydin inecegi durumda en az bir islem yapabiliyor mu (ADR-0008 K4.3)?
     *
     * <p>Departman kolundaki {@code hasUsableRoutingInto} ile ayni sekildedir: kisi ve
     * departman kollari tek bir "hedef gercekten isleyebilir mi?" kuralinda bulusur.
     * Kayit, uzerinde hicbir sey yapamayacak birine atanirsa olu uca dusardi.
     *
     * <p>Hesap yalnizca kural snapshot'i ve hedefin permission kumesi uzerinden yapilir;
     * validator veritabani bagimliligi almaz (V1 kirmizi cizgi 1).
     */
    private static boolean canActInLandingStatus(TransitionRule rule, TransitionContext context,
                                                 TransitionRuleSource snapshot) {
        return snapshot.all().stream()
                .filter(candidate -> candidate.from() == rule.to())
                .filter(candidate -> candidate.actorRoleId().equals(context.targetRoleId()))
                .anyMatch(candidate ->
                        context.targetPermissionCodes().contains("RECORD_VIEW")
                                && context.targetPermissionCodes().contains(candidate.requiredPermissionCode())
                                // Gecisin yaratacagi atama hedefin uzerindedir, bu yuzden
                                // assignment kosulu tanim geregi saglanir.
                                && candidate.actorRequirement()
                                        .isSatisfiedBy(context.targetIsCreator(), true));
    }
}
