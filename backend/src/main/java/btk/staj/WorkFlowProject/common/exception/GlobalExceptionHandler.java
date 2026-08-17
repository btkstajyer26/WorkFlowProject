package btk.staj.WorkFlowProject.common.exception;

import btk.staj.WorkFlowProject.user.service.AdminLimitExceededException;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowRecordNotFoundException;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Uygulama hatalari ----------

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return build("FORBIDDEN", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build("RESOURCE_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
        return build("BUSINESS_RULE_VIOLATION", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // ---------- Kullanici yonetimi ----------

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiError> handleRoleNotFound(RoleNotFoundException ex) {
        return build("ROLE_NOT_FOUND", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AdminLimitExceededException.class)
    public ResponseEntity<ApiError> handleAdminLimit(AdminLimitExceededException ex) {
        return build("ADMIN_LIMIT_EXCEEDED", ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Benzersizlik kisiti ihlali; pratikte kayitli bir e-posta ile ikinci
     * hesap acilmasi. Ayrinti sizdirmamak icin mesaj sabittir.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build("CONFLICT", "Bu kayıt zaten mevcut", HttpStatus.CONFLICT);
    }

    /**
     * Emniyet agi: {@code @Version} tasiyan bir kayit, istek hazirlanirken baska
     * bir islem tarafindan degistirilmisse Hibernate flush aninda bu tipi uretir.
     * Bu handler olmasaydi genel Exception handler'ina duser ve gecici bir
     * catisma 500 olarak donerdi.
     *
     * <p>Onay akisi bu noktaya <strong>ulasmaz</strong>: {@code RecordPortAdapter}
     * istisnayi port siniri gecmeden {@code WORKFLOW_VERSION_CONFLICT} koduna
     * cevirir. Burasi {@code record} modulundeki dogrudan yazmalari korur.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        // Kural ihlali degil, es zamanli erisim; arizayi arastirmak icin WARN yeterli.
        log.warn("Sürüm çatışması: kayıt istek hazırlanırken değişmiş", ex);
        return build(
                "VERSION_CONFLICT",
                "Kayıt siz işlem yaparken değişti, sayfayı yenileyip tekrar deneyin",
                HttpStatus.CONFLICT);
    }

    // ---------- Guvenlik ----------

    /**
     * {@code @PreAuthorize} reddettiginde firlatilir. Bu handler olmasaydi genel
     * Exception handler'ina duser ve yetki hatasi 500 olarak donerdi.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build("FORBIDDEN", "Bu işlem için yetkiniz yok", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return build("UNAUTHORIZED", "Kimlik doğrulaması gerekli", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Login sirasinda hatali email/sifre veya refresh sirasinda gecersiz/suresi
     * dolmus token icin AuthService tarafindan firlatilir. Bu handler olmasaydi
     * genel Exception handler'ina duser ve normal bir giris hatasi 500 olarak
     * donerdi (ayrica log.error ile gereksiz yere loglanirdi).
     *
     * <p>Kod bilerek {@code UNAUTHORIZED} degil: o kod filtre zincirinin
     * "token yok/gecersiz" reddine ayrilmistir. Ikisi de 401 doner, ancak
     * arayuz "tekrar giris yap" ile "email veya sifre hatali" durumlarini
     * bu kodla ayirt eder.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build("INVALID_CREDENTIALS", ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // ---------- Is akisi ----------

    @ExceptionHandler(WorkflowRecordNotFoundException.class)
    public ResponseEntity<ApiError> handleWorkflowRecordNotFound(WorkflowRecordNotFoundException ex) {
        return build("RESOURCE_NOT_FOUND", "Kayıt bulunamadı: " + ex.recordId(), HttpStatus.NOT_FOUND);
    }

    /** Durum makinesi hata kodlarini anlamli HTTP durumlarina esler. */
    @ExceptionHandler(WorkflowApplicationException.class)
    public ResponseEntity<ApiError> handleWorkflow(WorkflowApplicationException ex) {
        WorkflowErrorCode code = ex.errorCode();
        HttpStatus status = switch (code) {
            case WORKFLOW_FORBIDDEN, WORKFLOW_ROLE_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            // Ucu de kural ihlali degil, gecici catisma: kayit kilitli, tekil rol
            // hedefi cozulemedi veya kayit istek hazirlanirken degismis.
            case WORKFLOW_RECORD_LOCKED, WORKFLOW_ROLE_NOT_CONFIGURED, WORKFLOW_VERSION_CONFLICT ->
                    HttpStatus.CONFLICT;
            case WORKFLOW_STATUS_NOT_CONFIGURED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };

        if (status.is5xxServerError()) {
            log.error("İş akışı yapılandırma hatası: {}", code, ex);
        } else if (code == WorkflowErrorCode.WORKFLOW_ROLE_NOT_CONFIGURED) {
            // Istemciye 4xx donuyor ama tekil rol invaryanti bozulmus demektir;
            // operasyonun bunu gormesi gerekir.
            log.warn("Tekil rol hedefi çözülemedi: {}", code, ex);
        }

        return build(code.name(), workflowMessage(code), status);
    }

    private String workflowMessage(WorkflowErrorCode code) {
        return switch (code) {
            case WORKFLOW_INVALID_TRANSITION -> "Bu durumda bu işlem yapılamaz";
            case WORKFLOW_FORBIDDEN -> "Bu kayıt üzerinde işlem yapma yetkiniz yok";
            case WORKFLOW_RECORD_LOCKED -> "Kayıt kilitli, üzerinde işlem yapılamaz";
            case WORKFLOW_COMMENT_REQUIRED -> "Bu işlem için açıklama zorunludur";
            case WORKFLOW_TARGET_REQUIRED -> "Hedef kullanıcı seçilmelidir";
            case WORKFLOW_TARGET_NOT_ALLOWED -> "Seçilen hedef kullanıcı bu işlem için uygun değil";
            case WORKFLOW_TARGET_ROLE_INVALID -> "Seçilen hedef kullanıcının rolü uygun değil";
            case WORKFLOW_TARGET_INACTIVE -> "Seçilen hedef kullanıcı pasif durumda";
            case WORKFLOW_ROLE_NOT_ALLOWED -> "Rolünüz bu işlemi yapamaz";
            case WORKFLOW_STATUS_NOT_CONFIGURED -> "İş akışı yapılandırması eksik";
            case WORKFLOW_ROLE_NOT_CONFIGURED ->
                    "İşlemi devralacak yetkili şu anda belirlenemedi, yöneticinize başvurun";
            // Kural ihlali degil: kayit bu istek hazirlanirken baskasi tarafindan
            // degistirilmis. Mesaj kullaniciyi tekrar denemeye yonlendirir.
            case WORKFLOW_VERSION_CONFLICT ->
                    "Kayıt siz işlem yaparken değişti, sayfayı yenileyip tekrar deneyin";
        };
    }

    // ---------- Dogrulama ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ApiError error = new ApiError(
                "VALIDATION_ERROR",
                "Girilen veriler geçersiz",
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                fieldErrors);

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * `?sort=` ile var olmayan bir alan istendiginde Spring Data bunu firlatir
     * (orn. Swagger UI'nin doldurulmamis "string" varsayilanini oldugu gibi
     * gonderme). Sayfalanan her uc (kullanici, kayit, bildirim, audit listesi)
     * ayni riski tasidigi icin handler ozel bir controller'a degil buraya
     * bagli. Istemci hatasidir, sunucu hatasi degil; 400 donmeli.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiError> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        return build("INVALID_SORT_FIELD", "Sıralama alanı geçersiz", HttpStatus.BAD_REQUEST);
    }

    /**
     * Servis katmani gecersiz girdi icin bu tipi kullaniyor. Sunucu hatasi degil,
     * istemci hatasidir; 400 donmeli.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Spring MVC'nin istek cozumleme hatalari. Bunlar acikca ele alinmazsa genel
     * Exception handler'ina duser ve istemci hatalari 500 olarak donerdi.
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return build("BAD_REQUEST", "İstek geçersiz: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build("METHOD_NOT_ALLOWED", ex.getMessage(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return build("UNSUPPORTED_MEDIA_TYPE", ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return build("NOT_FOUND", "Böyle bir uç bulunamadı", HttpStatus.NOT_FOUND);
    }

    // ---------- Son care ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        // Beklenmeyen hatanin izi kaybolmasin; istemciye ayrinti sizdirilmaz.
        log.error("Beklenmeyen hata", ex);
        return build("INTERNAL_ERROR", "Beklenmeyen bir hata oluştu", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> build(String code, String message, HttpStatus status) {
        return new ResponseEntity<>(
                new ApiError(code, message, status.value(), LocalDateTime.now()), status);
    }
}
