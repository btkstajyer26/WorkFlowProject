package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionResponse;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Sartnamedeki butun onay akisi aksiyonlarinin tek ucu
 * ({@code POST /api/records/{recordId}/workflow/actions}). HTTP sozlesmesi
 * {@link WorkflowActionApi} arayuzunde tanimlidir.
 *
 * <p>Burada bilerek {@code @PreAuthorize} yok. Kimin hangi durumda hangi
 * aksiyonu alabilecegi tek bir yerde, durum makinesinde tanimli; rol kontrolunu
 * burada tekrarlamak kurali ikiye bolerdi. Yetkisiz rol denemesi durum
 * makinesinden {@code WORKFLOW_ROLE_NOT_ALLOWED} ile doner ve
 * {@code GlobalExceptionHandler} bunu 403'e cevirir.
 */
@RestController
public class WorkflowActionController implements WorkflowActionApi {

    private final WorkflowActionService workflowActionService;

    public WorkflowActionController(WorkflowActionService workflowActionService) {
        this.workflowActionService = Objects.requireNonNull(
                workflowActionService, "workflowActionService");
    }

    @Override
    public WorkflowActionResponse performAction(UUID recordId, WorkflowActionRequest request) {
        return workflowActionService.performAction(recordId, request);
    }
}
