package btk.staj.WorkFlowProject.subtask.exception;

import btk.staj.WorkFlowProject.common.exception.ApiError;
import btk.staj.WorkFlowProject.common.exception.GlobalExceptionHandler;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SubtaskExceptionContractTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @MethodSource("errorMappings")
    void exposesAStableCodeAndHttpStatus(
            SubtaskException.Reason reason,
            HttpStatus expectedStatus) {
        SubtaskException exception = new SubtaskException(reason, "mesaj");

        ResponseEntity<ApiError> response = handler.handleSubtask(exception);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("SUBTASK_" + reason.name());
        assertThat(response.getBody().getMessage()).isEqualTo("mesaj");
    }

    private static Stream<Arguments> errorMappings() {
        return Stream.of(
                Arguments.of(SubtaskException.Reason.PARENT_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(SubtaskException.Reason.ASSIGNEE_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(SubtaskException.Reason.PARENT_STATUS_INVALID, HttpStatus.CONFLICT),
                Arguments.of(SubtaskException.Reason.ALREADY_SPLIT, HttpStatus.CONFLICT),
                Arguments.of(SubtaskException.Reason.COUNT_TOO_LOW, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.INVALID_SUBTASK, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.DUPLICATE_ASSIGNEE, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.ASSIGNEE_INACTIVE, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.ASSIGNEE_SYSTEM, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.INVALID_APPROVAL_POLICY, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.INVALID_REQUIRED_APPROVALS, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(SubtaskException.Reason.ACTION_FORBIDDEN, HttpStatus.FORBIDDEN),
                Arguments.of(SubtaskException.Reason.INVALID_TRANSITION, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.REJECTION_COMMENT_REQUIRED, HttpStatus.BAD_REQUEST),
                Arguments.of(SubtaskException.Reason.TERMINAL, HttpStatus.CONFLICT));
    }
}
