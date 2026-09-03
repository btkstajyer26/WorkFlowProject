package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Static parity reference resolved against an explicit, environment-specific role mapping. */
public final class StaticTransitionRuleSource implements TransitionRuleSource {
    private final List<TransitionRule> snapshot;

    public StaticTransitionRuleSource(Map<RoleName, RoleId> roleIds) {
        this.snapshot = TransitionRules.all(roleIds);
    }

    @Override
    public Optional<TransitionRule> find(RecordStatus from, WorkflowAction action, RoleName actorRole) {
        return snapshot.stream().filter(rule -> rule.from() == from && rule.action() == action
                && rule.actorRole() == actorRole).findFirst();
    }

    @Override
    public List<TransitionRule> all() {
        return snapshot;
    }
}
