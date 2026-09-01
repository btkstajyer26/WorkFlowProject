package btk.staj.WorkFlowProject.workflow.model;

/**
 * Persistence katmanindan okunan aktif bir workflow gecisinin teknik
 * degerlerini tasiyan projection.
 *
 * <p>Alanlar bilerek {@code String} olarak tasinir. Mevcut domain enumlarina
 * donusum {@code DbTransitionRuleSource} sinirinda yapilir.
 */
public record TransitionRuleRecord(
        String fromStatus,
        String action,
        String actorRole,
        String actorRequirement,
        String toStatus) {
}
