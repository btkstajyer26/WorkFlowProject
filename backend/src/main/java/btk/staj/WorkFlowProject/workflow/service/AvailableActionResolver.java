package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionContext;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionDecision;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowTransitionValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bir aktorun bir kayit uzerinde su an yapabilecegi aksiyonlari hesaplar (APP-9 SS1).
 *
 * <p>Ikinci bir kural motoru <strong>degildir</strong>: {@link WorkflowApplicationService}
 * ile ayni {@link WorkflowTransitionValidator}'i ve ayni kural snapshot'ini kullanan,
 * yazma yapmayan bir kosumdur. Kural degisirse iki yol birlikte degisir.
 *
 * <p><strong>Liste bir taahhut degildir.</strong> Yetkili tek yol
 * {@code performAction}'dir ve kendi dogrulamasini bastan yapar; iki cagri arasinda rol,
 * permission, routing, uyelik veya kayit surumu degisebilir. Bu sinif UI gorunurlugu
 * icindir, authorization degildir.
 *
 * <p>Kullanilamayan aksiyon listeye <em>girmez</em> ve neden giremedigi disari
 * cikmaz (SS1.1): "su yetkin eksik" ipucu vermek yetki sizdirmaktir.
 */
public final class AvailableActionResolver {

    /**
     * Dry-run istegi <strong>iyi bicimli</strong> olmak zorundadir.
     *
     * <p>Aciklama zorunlu aksiyonlarda ({@code CALISANA_GERI_GONDER},
     * {@code BASKAN_YARDIMCISINA_GERI_GONDER}, {@code REDDET}) bos aciklamayla kurulan
     * baglam validator'in 5. adiminda {@code WORKFLOW_COMMENT_REQUIRED} ile reddedilir ve
     * aksiyon listede hic gorunmezdi. Burasi kullanicinin aciklamayi yazacagini varsayar;
     * istemci zorunlulugu yanittaki {@code commentRequired} bayragindan ogrenir.
     *
     * <p>Deger disariya cikmaz, kaydedilmez ve gercek gecise girmez.
     */
    private static final String DRY_RUN_COMMENT = "-";

    private final WorkflowTransitionValidator validator;
    private final TargetUserResolver targetUserResolver;
    private final DepartmentRoutingResolver departmentRoutingResolver;

    public AvailableActionResolver(WorkflowTransitionValidator validator,
                                   TargetUserResolver targetUserResolver,
                                   DepartmentRoutingResolver departmentRoutingResolver) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.targetUserResolver = Objects.requireNonNull(targetUserResolver, "targetUserResolver");
        this.departmentRoutingResolver =
                Objects.requireNonNull(departmentRoutingResolver, "departmentRoutingResolver");
    }

    /**
     * @return aktorun kayit uzerinde yapabilecegi aksiyonlar; hicbiri yoksa bos liste.
     *         Bos liste hata degildir (SS1.3).
     */
    public List<WorkflowAction> resolve(CurrentActor actor, WorkflowRecordSnapshot record,
                                        TransitionRuleSource snapshot) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(snapshot, "snapshot");

        // Terminal kayitta tanimli gecis yoktur (SS1.5). Validator da ayni sonucu verirdi;
        // bu kisa devre gereksiz routing/kullanici sorgularini bastan onler.
        if (record.status().isTerminal()) {
            return List.of();
        }

        List<WorkflowAction> available = new ArrayList<>();
        // (from, action, actorRoleId) tekil oldugu icin aksiyon basina en fazla bir kural var.
        for (TransitionRule rule : snapshot.all()) {
            if (rule.from() != record.status() || !rule.actorRoleId().equals(actor.roleId())) {
                continue;
            }
            if (isAvailable(rule, actor, record, snapshot)) {
                available.add(rule.action());
            }
        }
        return List.copyOf(available);
    }

    private boolean isAvailable(TransitionRule rule, CurrentActor actor,
                                WorkflowRecordSnapshot record, TransitionRuleSource snapshot) {
        WorkflowAction action = rule.action();
        TransitionContext context = dryRunContext(action, actor, record);
        TransitionDecision first = validator.validate(context, snapshot);

        if (requiresTargetUser(rule)) {
            // Hedef henuz cozulmedigi icin bu asamada tek kabul edilebilir ret,
            // WorkflowApplicationService'in de yuttugu nobetcidir.
            if (first instanceof TransitionDecision.Rejected rejected
                    && rejected.errorCode() != WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID) {
                return false;
            }

            TargetResolution resolution = targetUserResolver.resolve(
                    rule.targetStrategy(), rule.expectedTargetRoleId(), null, record);
            // Cozulemeyen hedef = tiklandiginda kesin hata verecek olu dugme. Sessizce
            // elenir; performAction ayni durumda gerekcesiyle birlikte hata dondurur.
            if (!(resolution instanceof TargetResolution.Resolved resolved)) {
                return false;
            }

            WorkflowUserSnapshot target = resolved.user();
            return validator.validate(
                    withResolvedTarget(context, target), snapshot).isAllowed();
        }

        if (rule.targetStrategy() == TargetStrategy.DEPARTMENT) {
            // Gonderilebilecek departman yoksa aksiyonu sunmak, bos bir secici acmaktir.
            return first.isAllowed()
                    && !departmentRoutingResolver
                            .usableTargetDepartments(rule.to(), record.createdBy(), snapshot)
                            .isEmpty();
        }

        return first.isAllowed();
    }

    /** Hedef gerektiren stratejiler; {@code WorkflowApplicationService} ile ayni tanim. */
    private static boolean requiresTargetUser(TransitionRule rule) {
        return rule.targetStrategy() != TargetStrategy.NONE
                && rule.targetStrategy() != TargetStrategy.DEPARTMENT;
    }

    private TransitionContext dryRunContext(WorkflowAction action, CurrentActor actor,
                                            WorkflowRecordSnapshot record) {
        return new TransitionContext(
                record.status(),
                action,
                actor.roleId(),
                actor.id().equals(record.createdBy()),
                departmentRoutingResolver.actorHoldsAssignment(actor, record, action),
                action.isCommentRequired() ? DRY_RUN_COMMENT : null,
                action.isTargetUserIdRequiredInRequest(),
                action.isTargetDepartmentIdRequiredInRequest(),
                null,
                false,
                actor.workflowActor(),
                actor.permissionCodes());
    }

    private static TransitionContext withResolvedTarget(TransitionContext context,
                                                        WorkflowUserSnapshot target) {
        return new TransitionContext(
                context.currentStatus(),
                context.action(),
                context.actorRoleId(),
                context.actorIsCreator(),
                context.actorHoldsAssignment(),
                context.comment(),
                context.targetUserProvidedInRequest(),
                context.targetDepartmentProvidedInRequest(),
                target.roleId(),
                target.active(),
                context.actorWorkflowActor(),
                context.actorPermissionCodes());
    }
}
