package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.port.DepartmentVisibilityPort;
import btk.staj.WorkFlowProject.rbac.visibility.RecordVisibilityScope.DepartmentStatus;
import btk.staj.WorkFlowProject.workflow.model.DepartmentRoutingResolution;
import btk.staj.WorkFlowProject.workflow.port.DepartmentRoutingPort;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Supplies the same department/status pairs to both policy and SQL. */
@Component
public class DepartmentVisibilityAdapter implements DepartmentVisibilityPort {
    private final DepartmentRoutingPort routing;
    private final TransitionRuleSource rules;

    public DepartmentVisibilityAdapter(DepartmentRoutingPort routing, TransitionRuleSource rules) {
        this.routing = routing;
        this.rules = rules;
    }

    @Override
    public Set<DepartmentStatus> scopesFor(VisibilityActor actor) {
        var snapshot = rules.snapshot();
        Set<DepartmentStatus> scopes = new HashSet<>();
        for (int departmentId : routing.activeDepartmentIdsFor(actor.id())) {
            for (var rule : snapshot.all()) {
                // Creator-dependent transitions are already covered by the CREATOR relation.
                if (rule.from().isTerminal() || rule.actorRequirement() != ActorRequirement.ASSIGNEE
                        || !rule.actorRoleId().equals(actor.roleId())
                        || !actor.permissionCodes().contains(rule.requiredPermissionCode())) continue;
                if (routing.resolve(departmentId, rule.from(), rule.action())
                        instanceof DepartmentRoutingResolution.Resolved resolved
                        && resolved.targetRoleId().equals(actor.roleId())
                        && resolved.eligibleUserIds().contains(actor.id())) {
                    scopes.add(new DepartmentStatus(departmentId, rule.from()));
                }
            }
        }
        return Set.copyOf(scopes);
    }
}
