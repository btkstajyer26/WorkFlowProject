package btk.staj.WorkFlowProject.workflow.exception;

/** Bozuk veya eksik workflow gecis konfigurasyonu icin fail-fast hata. */
public class TransitionRuleConfigurationException extends RuntimeException {

    public TransitionRuleConfigurationException(String message) {
        super(message);
    }

    public TransitionRuleConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
