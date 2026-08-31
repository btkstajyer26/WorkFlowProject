package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.List;
import java.util.Optional;

/**
 * {@link TransitionRules} icindeki merkezi statik tabloyu
 * {@link TransitionRuleSource} portu arkasina saran adapter.
 *
 * <p>Kurallarin dogruluk kaynagi bu iterasyonda hala {@code TransitionRules}
 * tablosudur; bu sinif yalnizca cagriyi ona delege eder ve hicbir kural
 * tanimi tasimaz.
 */
public final class StaticTransitionRuleSource implements TransitionRuleSource {

    @Override
    public Optional<TransitionRule> find(RecordStatus from, WorkflowAction action, RoleName actorRole) {
        return TransitionRules.find(from, action, actorRole);
    }

    @Override
    public List<TransitionRule> all() {
        return List.copyOf(TransitionRules.all());
    }
}
