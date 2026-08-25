package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowActionRequest validation")
class WorkflowActionRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("action is required")
    void actionIsRequired() {
        WorkflowActionRequest request = new WorkflowActionRequest(null, null, null);

        assertThat(propertyNames(validator.validate(request))).containsExactly("action");
    }

    @Test
    @DisplayName("target and comment are optional")
    void optionalFieldsMayBeNull() {
        WorkflowActionRequest request = new WorkflowActionRequest(WorkflowAction.ONAYLA, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("a 2000-character comment is accepted")
    void maximumLengthCommentIsAccepted() {
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.REDDET, null, "a".repeat(2000));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("a comment longer than 2000 characters is rejected")
    void overlongCommentIsRejected() {
        WorkflowActionRequest request = new WorkflowActionRequest(
                WorkflowAction.REDDET, null, "a".repeat(2001));

        assertThat(propertyNames(validator.validate(request))).containsExactly("comment");
    }

    private static Set<String> propertyNames(Set<ConstraintViolation<WorkflowActionRequest>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
