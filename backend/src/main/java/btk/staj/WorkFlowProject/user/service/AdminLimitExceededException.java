package btk.staj.WorkFlowProject.user.service;

public class AdminLimitExceededException extends RuntimeException {
    public AdminLimitExceededException(String message) {
        super(message);
    }
}