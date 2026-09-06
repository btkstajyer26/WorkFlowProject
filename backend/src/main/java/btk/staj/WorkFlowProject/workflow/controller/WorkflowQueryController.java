package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.workflow.dto.AvailableActionsResponse;
import btk.staj.WorkFlowProject.workflow.dto.TargetDepartmentsResponse;
import btk.staj.WorkFlowProject.workflow.service.WorkflowQueryService;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * {@link WorkflowQueryApi}'nin uygulamasi.
 *
 * <p>{@code @PreAuthorize} <strong>yoktur</strong> ve bu bilincli: hangi aksiyonun kime
 * acik oldugu durum makinesinin karari, sabit bir yetki anotasyonunun degil. Kaydi
 * gorebilme siniri {@link WorkflowQueryService} icinde kayit detay ucuyla ayni policy
 * uzerinden uygulanir; islem yetkisi olmayan kullanici 403 degil bos liste alir.
 */
@RestController
public class WorkflowQueryController implements WorkflowQueryApi {

    private final WorkflowQueryService workflowQueryService;

    public WorkflowQueryController(WorkflowQueryService workflowQueryService) {
        this.workflowQueryService = Objects.requireNonNull(workflowQueryService, "workflowQueryService");
    }

    @Override
    public AvailableActionsResponse availableActions(UUID recordId) {
        return workflowQueryService.availableActions(recordId);
    }

    @Override
    public TargetDepartmentsResponse targetDepartments(UUID recordId) {
        return workflowQueryService.targetDepartments(recordId);
    }
}
