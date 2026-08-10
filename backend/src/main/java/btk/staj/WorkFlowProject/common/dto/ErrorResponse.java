package btk.staj.WorkFlowProject.common.dto;

import java.time.LocalDateTime;

public class ErrorResponse {

    private String errorCode;
    private String message;
    private int status;
    private LocalDateTime timestamp;

    public ErrorResponse(String errorCode, String message, int status, LocalDateTime timestamp) {
        this.errorCode = errorCode;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}