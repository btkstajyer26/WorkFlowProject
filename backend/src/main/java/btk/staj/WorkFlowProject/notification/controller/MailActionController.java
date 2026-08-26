package btk.staj.WorkFlowProject.notification.controller;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.notification.service.MailActionTokenService;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class MailActionController {

    private static final Logger log = LoggerFactory.getLogger(MailActionController.class);

    private final WorkflowActionService workflowActionService;
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final MailActionTokenService mailActionTokenService;

    public MailActionController(WorkflowActionService workflowActionService,
                                RecordRepository recordRepository,
                                UserRepository userRepository,
                                MailActionTokenService mailActionTokenService) {
        this.workflowActionService = workflowActionService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.mailActionTokenService = mailActionTokenService;
    }

    @GetMapping(value = "/api/public/notification/quick-action", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleQuickAction(
            @RequestParam UUID recordId,
            @RequestParam(required = false) String action) {

        log.info("E-posta hızlı işlem: recordId={}, action={}", recordId, action);

        try {
            Optional<Record> recordOpt = recordRepository.findById(recordId);
            if (recordOpt.isEmpty()) {
                return ResponseEntity.ok(renderHtml("Evrak Bulunamadı", "İşlem yapılmak istenen evrak bulunamadı.", false));
            }

            Record record = recordOpt.get();
            RecordStatus status = record.getStatus();

            UUID actorId = null;
            WorkflowAction workflowAction = null;
            String successMessage = "";

            if (status == RecordStatus.BASKAN_INCELEMESINDE) {
                workflowAction = WorkflowAction.ONAYLA;
                actorId = record.getAssignedTo();
                successMessage = "Evrak başarıyla onaylandı.";
            } else if (status == RecordStatus.BSK_YRD_INCELEMESINDE) {
                workflowAction = WorkflowAction.BASKANA_ILET;
                actorId = record.getAssignedTo();
                successMessage = "Evrak başarıyla Başkan'a iletildi.";
            } else if (status == RecordStatus.TASLAK) {
                workflowAction = WorkflowAction.GONDER;
                actorId = record.getCreatedBy();
                successMessage = "Evrak incelemeye sunuldu.";
            } else if (status == RecordStatus.DUZENLEME_BEKLIYOR) {
                workflowAction = WorkflowAction.TEKRAR_GONDER;
                actorId = record.getCreatedBy();
                successMessage = "Evrak tekrar incelemeye sunuldu.";
            } else {
                return ResponseEntity.ok(renderHtml("İşlem Yapılamaz", "Bu evrak zaten sonuçlandırılmış (" + status + ").", false));
            }

            if (actorId == null) {
                return ResponseEntity.ok(renderHtml("Hata", "İşlem yetkilisi tespit edilemedi.", false));
            }

            Optional<User> actorOpt = userRepository.findById(actorId);
            if (actorOpt.isEmpty()) {
                return ResponseEntity.ok(renderHtml("Hata", "İşlem yetkilisi sistemde kayıtlı değil.", false));
            }

            User actor = actorOpt.get();
            AuthenticatedUser authenticatedUser = new AuthenticatedUser(actor);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    authenticatedUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            WorkflowActionRequest request = new WorkflowActionRequest(
                    workflowAction,
                    null,
                    "E-posta üzerinden otomatik onaylandı."
            );

            workflowActionService.performAction(recordId, request);

            return ResponseEntity.ok(renderHtml("İşlem Başarılı", successMessage, true));

        } catch (Exception e) {
            log.error("E-posta hızlı işlem hatası: ", e);
            return ResponseEntity.ok(renderHtml("Hata", "İşlem sırasında hata oluştu: " + e.getMessage(), false));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @GetMapping("/api/public/mail-actions/preview")
    public ResponseEntity<?> preview(@RequestParam(name = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "TOKEN_GEREKLI", "message", "Token gerekli"));
        }
        try {
            var preview = mailActionTokenService.preview(token);
            return ResponseEntity.ok(preview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "GECERSIZ_ANAHTAR", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code", "SUNUCU_HATASI", "message", e.getMessage()));
        }
    }

    @PostMapping("/api/public/mail-actions/consume")
    public ResponseEntity<?> consume(@RequestParam(name = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "TOKEN_GEREKLI", "message", "Token gerekli"));
        }
        try {
            var result = mailActionTokenService.consume(token);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "GECERSIZ_ANAHTAR", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code", "SUNUCU_HATASI", "message", e.getMessage()));
        }
    }

    private String renderHtml(String title, String message, boolean isSuccess) {
        String icon = isSuccess ? "&#10004;" : "&#10008;";
        String color = isSuccess ? "#2e7d32" : "#d32f2f";
        String bgColor = isSuccess ? "#e8f5e9" : "#ffebee";

        return "<!DOCTYPE html>"
                + "<html lang='tr'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>" + title + "</title>"
                + "<style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f4f6f8; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }"
                + ".card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.08); text-align: center; max-width: 440px; width: 90%; }"
                + ".icon { width: 64px; height: 64px; border-radius: 50%; background-color: " + bgColor + "; color: " + color + "; font-size: 32px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; font-weight: bold; }"
                + "h2 { color: #1a1a1a; margin-bottom: 12px; font-size: 22px; }"
                + "p { color: #555555; font-size: 15px; line-height: 1.5; margin-bottom: 24px; }"
                + ".footer-note { font-size: 12px; color: #888888; border-top: 1px solid #eeeeee; padding-top: 16px; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='card'>"
                + "<div class='icon'>" + icon + "</div>"
                + "<h2>" + title + "</h2>"
                + "<p>" + message + "</p>"
                + "<div class='footer-note'>Bu sekmeyi güvenle kapatabilirsiniz.</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}