package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;

/** One binding row. AP-8 can group rows by their fixed transition metadata. */
public record WorkflowActorBindingView(
        Integer bindingId,
        Integer fromStatusId, String fromStatus, String fromStatusDisplayName,
        Integer actionId, String action, String actionDisplayName,
        Integer toStatusId, String toStatus, String toStatusDisplayName,
        Integer actorRoleId, String actorRoleName,
        ActorRequirement actorRequirement, String targetStrategy,
        Integer expectedTargetRoleId, Integer requiredPermissionId, String requiredPermissionCode,
        boolean active, boolean protectedBinding) {}
