package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;

/** Active transition projection. Role identity comes directly from foreign keys. */
public record TransitionRuleRow(
        String fromStatus,
        String action,
        Integer actorRoleId,
        ActorRequirement actorRequirement,
        String toStatus,
        String targetStrategy,
        Integer expectedTargetRoleId,
        String requiredPermissionCode) {
}
