package btk.staj.WorkFlowProject.auth.exception;
import btk.staj.WorkFlowProject.user.controller.AdminController;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import btk.staj.WorkFlowProject.auth.controller.AuthController;
import btk.staj.WorkFlowProject.auth.service.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

}
