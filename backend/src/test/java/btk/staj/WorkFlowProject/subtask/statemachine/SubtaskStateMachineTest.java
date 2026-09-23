package btk.staj.WorkFlowProject.subtask.statemachine;

import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.model.SubtaskAction;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubtaskStateMachineTest {

    private final SubtaskStateMachine stateMachine = new SubtaskStateMachine();

    @ParameterizedTest
    @MethodSource("validTransitions")
    void allowsOnlyTheFixedTransitionTable(
            SubtaskStatus current,
            SubtaskAction action,
            String comment,
            SubtaskStatus expected) {
        assertThat(stateMachine.transition(current, action, comment)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void rejectsActionsOutsideTheTransitionTable(
            SubtaskStatus current,
            SubtaskAction action) {
        assertReason(
                () -> stateMachine.transition(current, action, "açıklama"),
                SubtaskException.Reason.INVALID_TRANSITION);
    }

    @ParameterizedTest
    @MethodSource("terminalStatuses")
    void rejectsEveryActionOnATerminalSubtask(SubtaskStatus terminalStatus) {
        assertReason(
                () -> stateMachine.transition(
                        terminalStatus,
                        SubtaskAction.DEGERLENDIRMEYI_TAMAMLA,
                        null),
                SubtaskException.Reason.TERMINAL);
    }

    @ParameterizedTest
    @MethodSource("blankComments")
    void rejectionRequiresANonblankComment(String comment) {
        assertReason(
                () -> stateMachine.transition(SubtaskStatus.ONAY, SubtaskAction.REDDET, comment),
                SubtaskException.Reason.REJECTION_COMMENT_REQUIRED);
    }

    @Test
    void aMissingActionIsAnInvalidTransition() {
        assertReason(
                () -> stateMachine.transition(SubtaskStatus.ISLEM, null, null),
                SubtaskException.Reason.INVALID_TRANSITION);
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(
                        SubtaskStatus.DEGERLENDIRME,
                        SubtaskAction.DEGERLENDIRMEYI_TAMAMLA,
                        null,
                        SubtaskStatus.ISLEM),
                Arguments.of(
                        SubtaskStatus.ISLEM,
                        SubtaskAction.ISLEMI_TAMAMLA,
                        null,
                        SubtaskStatus.ONAY),
                Arguments.of(
                        SubtaskStatus.ONAY,
                        SubtaskAction.ONAYLA,
                        null,
                        SubtaskStatus.TAMAMLANDI),
                Arguments.of(
                        SubtaskStatus.ONAY,
                        SubtaskAction.REDDET,
                        "Eksik belge",
                        SubtaskStatus.REDDEDILDI));
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(SubtaskStatus.DEGERLENDIRME, SubtaskAction.ISLEMI_TAMAMLA),
                Arguments.of(SubtaskStatus.DEGERLENDIRME, SubtaskAction.ONAYLA),
                Arguments.of(SubtaskStatus.ISLEM, SubtaskAction.DEGERLENDIRMEYI_TAMAMLA),
                Arguments.of(SubtaskStatus.ISLEM, SubtaskAction.REDDET),
                Arguments.of(SubtaskStatus.ONAY, SubtaskAction.ISLEMI_TAMAMLA));
    }

    private static Stream<SubtaskStatus> terminalStatuses() {
        return Stream.of(SubtaskStatus.TAMAMLANDI, SubtaskStatus.REDDEDILDI);
    }

    private static Stream<String> blankComments() {
        return Stream.of(null, "", " ", "\t\n");
    }

    private static void assertReason(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation,
            SubtaskException.Reason expectedReason) {
        assertThatThrownBy(invocation)
                .isInstanceOfSatisfying(
                        SubtaskException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(expectedReason));
    }
}
