package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.workflow.dto.AvailableActionsResponse;
import btk.staj.WorkFlowProject.workflow.dto.TargetDepartmentsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Workflow'un okuma sozlesmesi (APP-9). Yazma tarafi {@link WorkflowActionApi}'dedir.
 *
 * <p>Ayri arayuz olmasinin sebebi komut/sorgu ayrimi: aksiyon uygulamak ile aksiyon
 * listelemek farkli sozlesmelerdir ve farkli yanit tipleri dondururler.
 */
@RequestMapping("/api/records")
public interface WorkflowQueryApi {

    @GetMapping("/{recordId}/workflow/available-actions")
    AvailableActionsResponse availableActions(@PathVariable("recordId") UUID recordId);

    @GetMapping("/{recordId}/workflow/target-departments")
    TargetDepartmentsResponse targetDepartments(@PathVariable("recordId") UUID recordId);
}
