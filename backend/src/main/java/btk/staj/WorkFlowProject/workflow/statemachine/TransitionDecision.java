package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;

/**
 * {@link WorkflowTransitionValidator} sonucu: gecis izinli olabilir ve bir hedef durum
 * tasiyabilir, reddedilmis olabilir ve bir hata kodu tasiyabilir, ya da hedefin
 * cozulmesini bekliyor olabilir.
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

    /**
     * Karar verilemedi: gecis bir hedef kullanici gerektiriyor ve hedef henuz cozulmedi.
     *
     * <p>Iki asamali dogrulamanin ic sinyalidir, bir sonuc degildir; disariya <strong>hicbir
     * zaman</strong> cikmaz. Servis katmani bunu gorunce hedefi cozer ve dogrulamayi ikinci
     * kez calistirir.
     *
     * <p>Ayri bir varyant olmasinin sebebi: eskiden bu sinyal {@code WORKFLOW_TARGET_ROLE_INVALID}
     * hata koduyla tasiniyordu ve sinyal ile gercek ret ayirt edilemiyordu. Hedef rolun bos
     * oldugu her durumda ret uretilemedigi icin de kolon, yalnizca nobetci gorevi gorsun diye
     * dolu tutulmak zorunda kaliyordu &mdash; {@code B02}'nin kok sebebi buydu (ADR-0008 K3).
     */
    record Pending() implements TransitionDecision {
    }

    static TransitionDecision allowed(RecordStatus targetStatus) {
        return new Allowed(targetStatus);
    }

    static TransitionDecision rejected(WorkflowErrorCode errorCode) {
        return new Rejected(errorCode);
    }

    static TransitionDecision pending() {
        return new Pending();
    }

    default boolean isAllowed() {
        return this instanceof Allowed;
    }
}
