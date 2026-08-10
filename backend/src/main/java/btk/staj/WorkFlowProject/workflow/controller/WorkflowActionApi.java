package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/** HTTP contract only; a concrete controller is added when production ports are wired. */
@RequestMapping("/api/records")
public interface WorkflowActionApi {

    @PostMapping("/{recordId}/workflow/actions")
    WorkflowActionResponse performAction(
            @PathVariable("recordId") UUID recordId,
            @Valid @RequestBody WorkflowActionRequest request);
}
