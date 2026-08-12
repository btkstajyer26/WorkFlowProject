package btk.staj.WorkFlowProject.auth.exception;

import btk.staj.WorkFlowProject.user.controller.AdminController;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import btk.staj.WorkFlowProject.user.service.AdminLimitExceededException;
@RestControllerAdvice(basePackageClasses = AdminController.class)
public class AdminExceptionHandler {

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<String> handleRoleNotFound(RoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDuplicate(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Bu email zaten kayıtlı");
    }
    @ExceptionHandler(AdminLimitExceededException.class)
    public ResponseEntity<String> handleAdminLimit(AdminLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}