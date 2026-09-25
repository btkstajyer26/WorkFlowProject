package btk.staj.WorkFlowProject.workflow.model;

/** Raw persistence values, validated and converted at the rule-source boundary. */
public record TransitionRuleRecord(
        String fromStatus,
        String action,
        Integer actorRoleId,
        String actorRequirement,
        String toStatus,
        String targetStrategy,
        Integer expectedTargetRoleId,
        String requiredPermissionCode) {
}
