package btk.staj.WorkFlowProject.subtask.controller;

import btk.staj.WorkFlowProject.common.exception.GlobalExceptionHandler;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskActionRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskAssignableUserView;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskAssignableUsersResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskListResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.service.SubtaskActionService;
import btk.staj.WorkFlowProject.subtask.service.SubtaskQueryService;
import btk.staj.WorkFlowProject.subtask.service.SubtaskService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubtaskControllerTest {

    private static final UUID PARENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID SUBTASK_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ASSIGNEE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Mock private SubtaskService subtaskService;
    @Mock private SubtaskQueryService queryService;
    @Mock private SubtaskActionService actionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SubtaskController(subtaskService, queryService, actionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void splitEndpointReturnsTheServiceResponse() throws Exception {
        SubtaskView view = view(SubtaskStatus.DEGERLENDIRME);
        when(subtaskService.split(eq(PARENT_ID), any(SubtaskSplitRequest.class)))
                .thenReturn(new SubtaskSplitResponse(
                        PARENT_ID, SubtaskApprovalPolicy.UNANIMOUS, 2, List.of(view)));

        mockMvc.perform(post("/api/records/{recordId}/subtasks/split", PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvalPolicy":"UNANIMOUS","subtasks":[
                                  {"title":"Bir","assignedTo":"30000000-0000-0000-0000-000000000003"},
                                  {"title":"İki","assignedTo":"40000000-0000-0000-0000-000000000004"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentRecordId").value(PARENT_ID.toString()))
                .andExpect(jsonPath("$.approvalPolicy").value("UNANIMOUS"))
                .andExpect(jsonPath("$.requiredApprovals").value(2));
    }

    @Test
    void listEndpointKeepsPolicyFieldsAtTheTopLevel() throws Exception {
        when(queryService.list(PARENT_ID)).thenReturn(new SubtaskListResponse(
                SubtaskApprovalPolicy.MAJORITY, 2, List.of(view(SubtaskStatus.ISLEM))));

        mockMvc.perform(get("/api/records/{recordId}/subtasks", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalPolicy").value("MAJORITY"))
                .andExpect(jsonPath("$.requiredApprovals").value(2))
                .andExpect(jsonPath("$.subtasks[0].status").value("ISLEM"))
                .andExpect(jsonPath("$.subtasks[0].approvalPolicy").doesNotExist())
                .andExpect(jsonPath("$.subtasks[0].requiredApprovals").doesNotExist());
    }

    @Test
    void listEndpointReturnsTheUnsplitContract() throws Exception {
        when(queryService.list(PARENT_ID)).thenReturn(SubtaskListResponse.unsplit());

        mockMvc.perform(get("/api/records/{recordId}/subtasks", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalPolicy").isEmpty())
                .andExpect(jsonPath("$.requiredApprovals").isEmpty())
                .andExpect(jsonPath("$.subtasks").isEmpty());
    }

    @Test
    void assignableUsersEndpointReturnsTheServiceResponse() throws Exception {
        when(queryService.assignableUsers(PARENT_ID)).thenReturn(new SubtaskAssignableUsersResponse(
                List.of(new SubtaskAssignableUserView(ASSIGNEE_ID, "Ada Lovelace"))));

        mockMvc.perform(get("/api/records/{recordId}/subtasks/assignable-users", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].id").value(ASSIGNEE_ID.toString()))
                .andExpect(jsonPath("$.users[0].fullName").value("Ada Lovelace"));
    }

    @Test
    void actionEndpointReturnsTheUpdatedSubtask() throws Exception {
        when(actionService.performAction(eq(SUBTASK_ID), any(SubtaskActionRequest.class)))
                .thenReturn(view(SubtaskStatus.ISLEM));

        mockMvc.perform(post("/api/subtasks/{subtaskId}/actions", SUBTASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DEGERLENDIRMEYI_TAMAMLA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SUBTASK_ID.toString()))
                .andExpect(jsonPath("$.status").value("ISLEM"));
    }

    @Test
    void invalidRequestReturnsTheExistingValidationError() throws Exception {
        mockMvc.perform(post("/api/subtasks/{subtaskId}/actions", SUBTASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(actionService);
    }

    @Test
    void assignedUserFailureIsReturnedAsForbidden() throws Exception {
        when(actionService.performAction(eq(SUBTASK_ID), any(SubtaskActionRequest.class)))
                .thenThrow(new SubtaskException(
                        SubtaskException.Reason.ACTION_FORBIDDEN,
                        "Bu alt görev üzerinde işlem yapma yetkiniz yok"));

        mockMvc.perform(post("/api/subtasks/{subtaskId}/actions", SUBTASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DEGERLENDIRMEYI_TAMAMLA\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUBTASK_ACTION_FORBIDDEN"));
    }

    private static SubtaskView view(SubtaskStatus status) {
        return new SubtaskView(
                SUBTASK_ID,
                PARENT_ID,
                "Alt görev",
                null,
                ASSIGNEE_ID,
                "Ada Lovelace",
                status,
                null,
                null,
                null);
    }
}
