package btk.staj.WorkFlowProject.common.exception;

import java.time.LocalDateTime;
import java.util.List;

public class ApiError {

    private String code;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private List<FieldError> fieldErrors;

    public ApiError(String code, String message, int status, LocalDateTime timestamp) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public ApiError(String code, String message, int status, LocalDateTime timestamp, List<FieldError> fieldErrors) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
        this.fieldErrors = fieldErrors;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}