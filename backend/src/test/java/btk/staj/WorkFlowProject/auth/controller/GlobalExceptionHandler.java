package btk.staj.WorkFlowProject.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Auth ile ilgili controller'lardan fırlatılan RuntimeException'ları
 * anlamlı HTTP status kodlarına çevirir.
 */
@RestControllerAdvice(basePackages = "btk.staj.WorkFlowProject.auth.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleAuthException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
}