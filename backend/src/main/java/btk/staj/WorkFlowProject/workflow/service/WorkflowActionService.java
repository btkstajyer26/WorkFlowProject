package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * {@link WorkflowApplicationService} icin transaction siniri.
 *
 * <p>Cekirdek servis bir gecis sirasinda kaydi gunceller ve denetim izini yazar;
 * bu ikisi ya birlikte olmali ya da hic olmamali. Cekirdek Spring bilmedigi icin
 * transaction'i kendisi acamaz, controller'dan dogrudan cagrilmasi da bu
 * atomikligi saglamaz. Sinir bu yuzden burada.
 */
@Service
public class WorkflowActionService {

    private final WorkflowApplicationService workflowApplicationService;

    public WorkflowActionService(WorkflowApplicationService workflowApplicationService) {
        this.workflowApplicationService = Objects.requireNonNull(
                workflowApplicationService, "workflowApplicationService");
    }

    @Transactional
    public WorkflowActionResponse performAction(UUID recordId, WorkflowActionRequest request) {
        return workflowApplicationService.performAction(recordId, request);
    }
}
