package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;

/**
 * {@code workflow_transitions} satirinin, katalog tablolariyla birlestirilmis
 * teknik goruntusu. Yalnizca
 * {@link WorkflowTransitionRepository#findActiveRuleRows()} sorgusunun sonuc
 * tipidir; domain kurali degildir.
 *
 * <p>Entity FK'leri sayisal id tutar ({@code from_status_id},
 * {@code actor_role_id}, ...); port ise teknik <em>ad</em> bekler. Donusum bu
 * projection'in join'leriyle yapilir.
 *
 * @param fromStatus       {@code workflow_statuses.name}
 * @param action           {@code workflow_actions.name}
 * @param actorSystemKey   {@code roles.system_key}. Rol adi degil: {@code name}
 *                         yonetim panelinden degistirilebilir,
 *                         {@code system_key} degismez (DB-1 SS4). Dinamik
 *                         rollerde {@code null} olabilir
 * @param actorRoleName    {@code roles.name}; yalnizca hata mesajini okunur
 *                         kilmak icin tasinir, esleme kararinda kullanilmaz
 * @param actorRequirement aktorun kayitla kurmasi gereken iliski
 * @param toStatus         hedef {@code workflow_statuses.name}
 */
public record TransitionRuleRow(
        String fromStatus,
        String action,
        String actorSystemKey,
        String actorRoleName,
        ActorRequirement actorRequirement,
        String toStatus) {
}
