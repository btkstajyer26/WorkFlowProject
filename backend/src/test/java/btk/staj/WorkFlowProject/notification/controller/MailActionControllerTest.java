package btk.staj.WorkFlowProject.notification.controller;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailActionControllerTest {

    private WorkflowActionService workflowActionService;
    private RecordRepository recordRepository;
    private UserRepository userRepository;
    private MailActionController controller;

    @BeforeEach
    void setUp() {
        workflowActionService = mock(WorkflowActionService.class);
        recordRepository = mock(RecordRepository.class);
        userRepository = mock(UserRepository.class);
        controller = new MailActionController(workflowActionService, recordRepository, userRepository);
    }

    @Test
    void handleQuickAction_approvesRecordSuccessfully() {
        UUID recordId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Record record = new Record();
        record.setId(recordId);
        record.setStatus(RecordStatus.BASKAN_INCELEMESINDE);
        record.setAssignedTo(userId);

        Role role = new Role();
        role.setName("BASKAN");

        User user = new User();
        user.setId(userId);
        user.setEmail("baskan@ornek.local");
        user.setRole(role);

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ResponseEntity<String> response = controller.handleQuickAction(recordId, "ONAYLA");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Evrak başarıyla onaylandı.");

        verify(workflowActionService).performAction(eq(recordId), any(WorkflowActionRequest.class));
    }
}