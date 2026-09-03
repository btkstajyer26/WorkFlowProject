package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement.ASSIGNEE;
import static btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement.CREATOR;
import static btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement.CREATOR_AND_ASSIGNEE;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.BASKAN_INCELEMESINDE;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.BSK_YRD_INCELEMESINDE;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.DUZENLEME_BEKLIYOR;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.ONAYLANDI;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.REDDEDILDI;
import static btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus.TASLAK;
import static btk.staj.WorkFlowProject.workflow.statemachine.RoleName.BASKAN;
import static btk.staj.WorkFlowProject.workflow.statemachine.RoleName.BASKAN_YARDIMCISI;
import static btk.staj.WorkFlowProject.workflow.statemachine.RoleName.CALISAN;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.BASKANA_ILET;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.CALISANA_GERI_GONDER;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.GONDER;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.ONAYLA;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.REDDET;
import static btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction.TEKRAR_GONDER;

/**
 * Eight seeded transition templates, kept in the test tree as the parity reference (TZ-1).
 * Production reads the same rows from {@code workflow_transitions}; this table exists only
 * so the parity test has an independent second source.
 * Role IDs must be supplied by the caller: production IDs are environment-specific.
 * Tests without a database supply their own synthetic mapping.
 */
public final class TransitionRules {
    private static final List<RuleTemplate> TEMPLATES = List.of(
            //                 mevcut durum           aksiyon                          yetkili rol         kayit iliskisi         hedef durum            hedef stratejisi              beklenen hedef rol
            new RuleTemplate(TASLAK,                GONDER,                          CALISAN,            CREATOR,               BSK_YRD_INCELEMESINDE, TargetStrategy.ROLE,           BASKAN_YARDIMCISI, "RECORD_FORWARD"),
            new RuleTemplate(DUZENLEME_BEKLIYOR,    TEKRAR_GONDER,                   CALISAN,            CREATOR_AND_ASSIGNEE,  BSK_YRD_INCELEMESINDE, TargetStrategy.ROLE,           BASKAN_YARDIMCISI, "RECORD_FORWARD"),
            new RuleTemplate(BSK_YRD_INCELEMESINDE, BASKANA_ILET,                    BASKAN_YARDIMCISI,  ASSIGNEE,              BASKAN_INCELEMESINDE,  TargetStrategy.ROLE,           BASKAN, "RECORD_FORWARD"),
            new RuleTemplate(BSK_YRD_INCELEMESINDE, CALISANA_GERI_GONDER,            BASKAN_YARDIMCISI,  ASSIGNEE,              DUZENLEME_BEKLIYOR,    TargetStrategy.CREATOR,        CALISAN, "RECORD_RETURN"),
            new RuleTemplate(BASKAN_INCELEMESINDE,  ONAYLA,                          BASKAN,             ASSIGNEE,              ONAYLANDI,             TargetStrategy.NONE,           null, "RECORD_APPROVE"),
            new RuleTemplate(BASKAN_INCELEMESINDE,  REDDET,                          BASKAN,             ASSIGNEE,              REDDEDILDI,            TargetStrategy.NONE,           null, "RECORD_REJECT"),
            new RuleTemplate(BASKAN_INCELEMESINDE,  CALISANA_GERI_GONDER,            BASKAN,             ASSIGNEE,              DUZENLEME_BEKLIYOR,    TargetStrategy.CREATOR,        CALISAN, "RECORD_RETURN"),
            new RuleTemplate(BASKAN_INCELEMESINDE,  BASKAN_YARDIMCISINA_GERI_GONDER, BASKAN,             ASSIGNEE,              BSK_YRD_INCELEMESINDE, TargetStrategy.PREVIOUS_ACTOR, BASKAN_YARDIMCISI, "RECORD_RETURN")
    );

    private TransitionRules() { }

    public static List<TransitionRule> all(Map<RoleName, RoleId> roleIds) {
        Map<RoleName, RoleId> identities = Map.copyOf(roleIds);
        return TEMPLATES.stream().map(template -> template.resolve(identities)).toList();
    }

    private record RuleTemplate(RecordStatus from, WorkflowAction action, RoleName actorRole,
            ActorRequirement actorRequirement, RecordStatus to, TargetStrategy targetStrategy,
            RoleName expectedTargetRole, String requiredPermissionCode) {
        TransitionRule resolve(Map<RoleName, RoleId> ids) {
            RoleId actorId = Objects.requireNonNull(ids.get(actorRole), "Missing role ID for " + actorRole);
            RoleId targetId = expectedTargetRole == null ? null
                    : Objects.requireNonNull(ids.get(expectedTargetRole), "Missing role ID for " + expectedTargetRole);
            return new TransitionRule(from, action, actorId, actorRequirement, to, targetStrategy,
                    targetId, requiredPermissionCode);
        }
    }
}
