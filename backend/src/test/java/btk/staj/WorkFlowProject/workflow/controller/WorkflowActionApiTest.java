package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionResponse;
import jakarta.validation.Valid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowActionApi contract")
class WorkflowActionApiTest {

    @Test
    @DisplayName("declares the required POST endpoint")
    void declaresPostEndpoint() throws NoSuchMethodException {
        RequestMapping baseMapping = WorkflowActionApi.class.getAnnotation(RequestMapping.class);
        Method method = actionMethod();
        PostMapping postMapping = method.getAnnotation(PostMapping.class);

        assertThat(baseMapping).isNotNull();
        assertThat(baseMapping.value()).containsExactly("/api/records");
        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/{recordId}/workflow/actions");
        assertThat(method.getReturnType()).isEqualTo(WorkflowActionResponse.class);
    }

    @Test
    @DisplayName("binds record id and validates the request body")
    void bindsParameters() throws NoSuchMethodException {
        Parameter[] parameters = actionMethod().getParameters();

        assertThat(parameters).hasSize(2);
        assertThat(parameters[0].getType()).isEqualTo(UUID.class);
        assertThat(parameters[0].getAnnotation(PathVariable.class).value()).isEqualTo("recordId");
        assertThat(parameters[1].getType()).isEqualTo(WorkflowActionRequest.class);
        assertThat(parameters[1].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(parameters[1].isAnnotationPresent(Valid.class)).isTrue();
    }

    @Test
    @DisplayName("annotation reflection proves only the contract, not a registered endpoint")
    void doesNotRegisterSpringBean() {
        assertThat(WorkflowActionApi.class).isInterface();
        assertThat(WorkflowActionApi.class.isAnnotationPresent(RestController.class)).isFalse();
        assertThat(WorkflowActionApi.class.isAnnotationPresent(Component.class)).isFalse();
    }

    private static Method actionMethod() throws NoSuchMethodException {
        return WorkflowActionApi.class.getDeclaredMethod(
                "performAction", UUID.class, WorkflowActionRequest.class);
    }
}
