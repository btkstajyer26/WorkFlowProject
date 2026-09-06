package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;

/**
 * Aktorun su an yapabilecegi tek bir aksiyon (APP-9 SS1).
 *
 * <p>{@code displayName} {@code workflow_actions.display_name} kolonundan gelir; istemci
 * aksiyon etiketlerini kendi sozlugunde tutmaz. Bayraklar istemcinin form davranisini
 * belirler ve validator'in uyguladigi ayni enum metadata'sindan uretilir.
 */
public record AvailableActionView(
        WorkflowAction action,
        String displayName,
        boolean commentRequired,
        boolean targetUserRequired,
        boolean targetDepartmentRequired) {
}
