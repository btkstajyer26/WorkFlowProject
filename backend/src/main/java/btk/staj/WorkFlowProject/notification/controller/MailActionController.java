package btk.staj.WorkFlowProject.notification.controller;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/notification")
public class MailActionController {

    private static final Logger log = LoggerFactory.getLogger(MailActionController.class);

    private final WorkflowActionService workflowActionService;
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public MailActionController(WorkflowActionService workflowActionService,
                                RecordRepository recordRepository,
                                UserRepository userRepository) {
        this.workflowActionService = workflowActionService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/quick-action")
    public ResponseEntity<Void> handleQuickAction(
            @RequestParam UUID recordId,
            @RequestParam String action) {

        log.info(">>> E-posta hızlı onay tetiklendi -> Evrak: {}, Aksiyon Parametresi: {}", recordId, action);

        try {
            Optional<Record> recordOpt = recordRepository.findById(recordId);
            if (recordOpt.isPresent()) {
                Record record = recordOpt.get();
                String act = action.toUpperCase();

                UUID actorId = null;
                WorkflowAction workflowAction = null;

                // 1. BAŞKANIN ONAYLAMASI
                if ("APPROVE".equals(act) || "ONAYLA".equals(act)) {
                    workflowAction = WorkflowAction.ONAYLA;
                    actorId = record.getAssignedTo();
                }
                // 2. BAŞKAN YARDIMCISININ BAŞKANA İLETMESİ
                else if ("FORWARD".equals(act) || "BASKANA_ILET".equals(act)) {
                    workflowAction = WorkflowAction.BASKANA_ILET;
                    actorId = record.getAssignedTo() != null ? record.getAssignedTo() : record.getLastDeputyId();
                }
                // 3. ÇALIŞANIN SUNMASI
                else if ("SUBMIT".equals(act) || "GONDER".equals(act)) {
                    workflowAction = (record.getStatus() == RecordStatus.DUZENLEME_BEKLIYOR)
                            ? WorkflowAction.TEKRAR_GONDER
                            : WorkflowAction.GONDER;
                    actorId = record.getCreatedBy();
                }

                if (actorId != null && workflowAction != null) {
                    Optional<User> actorOpt = userRepository.findById(actorId);
                    if (actorOpt.isPresent()) {
                        User actor = actorOpt.get();

                        String roleName = String.valueOf(actor.getRole());
                        if (!roleName.startsWith("ROLE_")) {
                            roleName = "ROLE_" + roleName;
                        }

                        // Principal olarak aktörün kendisini (veya ID/Email) geçiyoruz
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                actor,
                                null,
                                List.of(new SimpleGrantedAuthority(roleName))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        WorkflowActionRequest request = new WorkflowActionRequest(
                                workflowAction,
                                null,
                                "E-posta üzerinden hızlı onaylama işlemi gerçekleştirildi."
                        );

                        workflowActionService.performAction(recordId, request);
                        log.info(">>> [BAŞARILI] Workflow aksiyonu başarıyla tamamlandı: Evrak: {}, Aksiyon: {}, Aktör: {}", recordId, workflowAction, actor.getEmail());
                    } else {
                        log.warn(">>> Aktör kullanıcı bulunamadı! ID: {}", actorId);
                    }
                } else {
                    log.warn(">>> Aktör veya aksiyon çözümlenemedi. RecordId: {}, Action: {}", recordId, action);
                }
            } else {
                log.warn(">>> Evrak bulunamadı: {}", recordId);
            }
        } catch (Exception e) {
            log.error(">>> E-posta hızlı aksiyon yürütülürken hata:", e);
        } finally {
            SecurityContextHolder.clearContext();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendUrl + "/records/" + recordId));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}