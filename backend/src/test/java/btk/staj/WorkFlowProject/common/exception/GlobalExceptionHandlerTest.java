package btk.staj.WorkFlowProject.common.exception;

import btk.staj.WorkFlowProject.user.service.AdminLimitExceededException;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ForbiddenException -> 403 FORBIDDEN")
    void forbidden_returns403() {
        ResponseEntity<ApiError> response = handler.handleForbidden(new ForbiddenException("yasak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().getMessage()).isEqualTo("yasak");
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404 NOT_FOUND")
    void resourceNotFound_returns404() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("bulunamadı"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("BusinessRuleException -> 400 BAD_REQUEST, kod BUSINESS_RULE_VIOLATION")
    void businessRule_returns400() {
        ResponseEntity<ApiError> response =
                handler.handleBusinessRule(new BusinessRuleException("kural ihlali"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    @DisplayName("RoleNotFoundException -> 400 BAD_REQUEST, kod ROLE_NOT_FOUND")
    void roleNotFound_returns400() {
        ResponseEntity<ApiError> response =
                handler.handleRoleNotFound(new RoleNotFoundException("rol yok"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("ROLE_NOT_FOUND");
    }

    @Test
    @DisplayName("AdminLimitExceededException -> 409 CONFLICT")
    void adminLimit_returns409() {
        ResponseEntity<ApiError> response =
                handler.handleAdminLimit(new AdminLimitExceededException("zaten var"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("ADMIN_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409 CONFLICT, detay sizdirmaz")
    void dataIntegrity_returns409WithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Bu kayıt zaten mevcut");
    }

    /**
     * Emniyet agi: onay akisi disindaki yazmalarda (ornegin kayit guncelleme)
     * olusan surum catismasi. Bu handler olmasaydi genel Exception handler'ina
     * duser ve gecici bir catisma 500 olarak donerdi.
     */
    @Test
    @DisplayName("OptimisticLockingFailureException -> 409 CONFLICT, kod VERSION_CONFLICT")
    void optimisticLocking_returns409() {
        ResponseEntity<ApiError> response = handler.handleOptimisticLocking(
                new OptimisticLockingFailureException("row was updated by another transaction"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("VERSION_CONFLICT");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Kayıt siz işlem yaparken değişti, sayfayı yenileyip tekrar deneyin");
    }

    /**
     * Onay akisi yolu: {@code RecordPortAdapter} catismayi bu koda cevirir.
     * Kod kalici bir kural ihlali degil gecici bir catisma bildirir; 400 degil
     * 409 donmeli ki istemci kaydi yenileyip tekrar deneyebilsin.
     */
    @Test
    @DisplayName("WORKFLOW_VERSION_CONFLICT -> 409 CONFLICT")
    void workflowVersionConflict_returns409() {
        ResponseEntity<ApiError> response = handler.handleWorkflow(
                new WorkflowApplicationException(WorkflowErrorCode.WORKFLOW_VERSION_CONFLICT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("WORKFLOW_VERSION_CONFLICT");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Kayıt siz işlem yaparken değişti, sayfayı yenileyip tekrar deneyin");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 FORBIDDEN, sabit mesaj")
    void accessDenied_returns403() {
        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException("yetkisiz"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Bu işlem için yetkiniz yok");
    }

    @Test
    @DisplayName("InvalidCredentialsException -> 401 UNAUTHORIZED, kod INVALID_CREDENTIALS")
    void invalidCredentials_returns401WithOwnCode() {
        ResponseEntity<ApiError> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException("email veya şifre hatalı"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("InvalidDataAccessApiUsageException -> 400 BAD_REQUEST, kod INVALID_SORT_FIELD")
    void invalidSort_returns400() {
        ResponseEntity<ApiError> response = handler.handleInvalidDataAccess(
                new InvalidDataAccessApiUsageException("bilinmeyen alan"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_SORT_FIELD");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 BAD_REQUEST, orijinal mesaj korunur")
    void illegalArgument_returns400WithOriginalMessage() {
        ResponseEntity<ApiError> response =
                handler.handleIllegalArgument(new IllegalArgumentException("geçersiz parametre"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("geçersiz parametre");
    }

    @Test
    @DisplayName("Genel Exception -> 500 INTERNAL_ERROR, detay sizdirmaz")
    void genericException_returns500WithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleGeneric(new RuntimeException("gizli detay"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Beklenmeyen bir hata oluştu");
        // Kritik: gercek exception mesaji ("gizli detay") disari sizmamali
        assertThat(response.getBody().getMessage()).doesNotContain("gizli detay");
    }
}