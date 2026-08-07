package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;

/**
 * {@link WorkflowTransitionValidator} sonucu: gecis ya izinlidir ve bir hedef
 * durum tasir, ya da reddedilmistir ve bir hata kodu tasir.
 */
public sealed interface TransitionDecision {

    /** Gecis izinli; kayit {@link Allowed#targetStatus()} durumuna tasinabilir. */
    record Allowed(RecordStatus targetStatus) implements TransitionDecision {
        public Allowed {
            Objects.requireNonNull(targetStatus, "targetStatus");
        }
    }

    /** Gecis reddedildi; sebep {@link Rejected#errorCode()} ile bildirilir. */
    record Rejected(WorkflowErrorCode errorCode) implements TransitionDecision {
        public Rejected {
            Objects.requireNonNull(errorCode, "errorCode");
        }
    }

    static TransitionDecision allowed(RecordStatus targetStatus) {
        return new Allowed(targetStatus);
    }

    static TransitionDecision rejected(WorkflowErrorCode errorCode) {
        return new Rejected(errorCode);
    }

    default boolean isAllowed() {
        return this instanceof Allowed;
    }
}
