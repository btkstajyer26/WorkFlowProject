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
 * @param targetStrategy   {@code workflow_transitions.target_strategy}
 * @param expectedTargetRoleId
 *        {@code workflow_transitions.expected_target_role_id}. Bos olabilir; hedef
 *        gerektirmeyen gecislerde bos olmasi beklenir
 * @param expectedTargetRoleSystemKey
 *        beklenen hedef rolun {@code roles.system_key} degeri. Bos gelmesinin
 *        <strong>iki farkli</strong> anlami vardir ve ayirt edilmeleri gerekir:
 *        FK hic dolu degilse hedef yoktur (mesru); FK doluyken bos geliyorsa hedef
 *        dinamik bir roldur ve bu compatibility seam ile temsil edilemez.
 *        {@code expectedTargetRoleId} tam da bu ayrimi yapabilmek icin tasinir
 */
public record TransitionRuleRow(
        String fromStatus,
        String action,
        String actorSystemKey,
        String actorRoleName,
        ActorRequirement actorRequirement,
        String toStatus,
        String targetStrategy,
        Integer expectedTargetRoleId,
        String expectedTargetRoleSystemKey,
        String requiredPermissionCode) {
}
