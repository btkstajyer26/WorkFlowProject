package btk.staj.WorkFlowProject.subtask.statemachine;

import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException.Reason;
import btk.staj.WorkFlowProject.subtask.model.SubtaskAction;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Fixed, lightweight state machine for Subtasks; independent of the Parent workflow engine. */
@Component
public final class SubtaskStateMachine {

    public SubtaskStatus transition(
            SubtaskStatus currentStatus,
            SubtaskAction action,
            String comment) {
        SubtaskStatus current = Objects.requireNonNull(currentStatus, "currentStatus");

        if (current.isTerminal()) {
            throw error(Reason.TERMINAL, "Terminal alt görev üzerinde işlem yapılamaz");
        }
        if (action == null) {
            throw error(Reason.INVALID_TRANSITION, "Alt görev aksiyonu geçersiz");
        }

        return switch (current) {
            case DEGERLENDIRME -> require(
                    action,
                    SubtaskAction.DEGERLENDIRMEYI_TAMAMLA,
                    SubtaskStatus.ISLEM,
                    current);
            case ISLEM -> require(
                    action,
                    SubtaskAction.ISLEMI_TAMAMLA,
                    SubtaskStatus.ONAY,
                    current);
            case ONAY -> transitionFromApproval(action, comment);
            case TAMAMLANDI, REDDEDILDI -> throw new IllegalStateException(
                    "Terminal status passed the terminal guard: " + current);
        };
    }

    private static SubtaskStatus transitionFromApproval(SubtaskAction action, String comment) {
        if (action == SubtaskAction.ONAYLA) {
            return SubtaskStatus.TAMAMLANDI;
        }
        if (action == SubtaskAction.REDDET) {
            if (comment == null || comment.isBlank()) {
                throw error(
                        Reason.REJECTION_COMMENT_REQUIRED,
                        "Alt görev reddi için açıklama zorunludur");
            }
            return SubtaskStatus.REDDEDILDI;
        }
        throw invalidTransition(SubtaskStatus.ONAY, action);
    }

    private static SubtaskStatus require(
            SubtaskAction actual,
            SubtaskAction expected,
            SubtaskStatus target,
            SubtaskStatus current) {
        if (actual != expected) {
            throw invalidTransition(current, actual);
        }
        return target;
    }

    private static SubtaskException invalidTransition(
            SubtaskStatus current,
            SubtaskAction action) {
        return error(
                Reason.INVALID_TRANSITION,
                "Alt görev geçişi geçersiz: " + current + " + " + action);
    }

    private static SubtaskException error(Reason reason, String message) {
        return new SubtaskException(reason, message);
    }
}
