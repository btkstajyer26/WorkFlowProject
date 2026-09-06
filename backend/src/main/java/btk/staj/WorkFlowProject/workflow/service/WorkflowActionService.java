package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.common.dto.AssignmentView;
import btk.staj.WorkFlowProject.record.view.AssignmentViewResolver;
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
    private final AssignmentViewResolver assignmentViewResolver;

    public WorkflowActionService(WorkflowApplicationService workflowApplicationService,
                                 AssignmentViewResolver assignmentViewResolver) {
        this.workflowApplicationService = Objects.requireNonNull(
                workflowApplicationService, "workflowApplicationService");
        this.assignmentViewResolver = Objects.requireNonNull(
                assignmentViewResolver, "assignmentViewResolver");
    }

    @Transactional
    public WorkflowActionResponse performAction(UUID recordId, WorkflowActionRequest request) {
        WorkflowActionResponse response = workflowApplicationService.performAction(recordId, request);

        // Cekirdek atamayi yalniz kimliklerle kurar; gosterim adlari burada eklenir.
        // Ad cozumu bir depo erisimi gerektirdigi icin saf cekirdege tasinamaz.
        AssignmentView assignment = assignmentViewResolver.resolve(
                response.assignment().userId(), response.assignment().departmentId());
        return response.withAssignmentNames(assignment.userFullName(), assignment.departmentName());
    }
}
