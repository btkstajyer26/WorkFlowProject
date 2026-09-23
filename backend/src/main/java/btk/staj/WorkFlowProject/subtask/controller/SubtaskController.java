package btk.staj.WorkFlowProject.subtask.controller;

import btk.staj.WorkFlowProject.subtask.dto.SubtaskActionRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskAssignableUsersResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskListResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.service.SubtaskActionService;
import btk.staj.WorkFlowProject.subtask.service.SubtaskQueryService;
import btk.staj.WorkFlowProject.subtask.service.SubtaskService;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public final class SubtaskController {

    private final SubtaskService subtaskService;
    private final SubtaskQueryService queryService;
    private final SubtaskActionService actionService;

    public SubtaskController(
            SubtaskService subtaskService,
            SubtaskQueryService queryService,
            SubtaskActionService actionService) {
        this.subtaskService = Objects.requireNonNull(subtaskService, "subtaskService");
        this.queryService = Objects.requireNonNull(queryService, "queryService");
        this.actionService = Objects.requireNonNull(actionService, "actionService");
    }

    @PostMapping("/records/{recordId}/subtasks/split")
    public SubtaskSplitResponse split(
            @PathVariable UUID recordId,
            @Valid @RequestBody SubtaskSplitRequest request) {
        return subtaskService.split(recordId, request);
    }

    @GetMapping("/records/{recordId}/subtasks")
    public SubtaskListResponse list(@PathVariable UUID recordId) {
        return queryService.list(recordId);
    }

    @GetMapping("/records/{recordId}/subtasks/assignable-users")
    public SubtaskAssignableUsersResponse assignableUsers(@PathVariable UUID recordId) {
        return queryService.assignableUsers(recordId);
    }

    @PostMapping("/subtasks/{subtaskId}/actions")
    public SubtaskView performAction(
            @PathVariable UUID subtaskId,
            @Valid @RequestBody SubtaskActionRequest request) {
        return actionService.performAction(subtaskId, request);
    }
}
