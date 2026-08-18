package btk.staj.WorkFlowProject.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Durum degisikligi e-postasini uretir ve gonderir.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String NO_EXPLANATION = "—";
    private static final String TEMPLATE = "mail/workflow-status";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail-from:ebys@ornek.local}")
    private String mailFrom;

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.templateEngine = Objects.requireNonNull(templateEngine, "templateEngine");
    }

    @Async
    public void sendStatusChangeMail(String toEmail,
                                     String recipientName,
                                     UUID recordId,
                                     String title,
                                     String status,
                                     String reason) {
        try {
            log.info("E-posta gönderimi başlatılıyor. Alıcı: {}, Evrak: {}", toEmail, recordId);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject(subject(recordId));
            helper.setText(render(recipientName, recordId, title, status, reason), true);

            mailSender.send(mimeMessage);
            log.info("E-posta başarıyla gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("E-posta gönderilirken hata oluştu! Alıcı: {}, Hata: {}", toEmail, e.getMessage());
        }
    }

    private String render(String recipientName, UUID recordId, String title, String status, String reason) {
        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("recordId", recordId);
        context.setVariable("title", title);
        context.setVariable("status", status);
        context.setVariable("explanation",
                (reason == null || reason.isBlank()) ? NO_EXPLANATION : reason);
        context.setVariable("deepLink", frontendUrl + "/records/" + recordId);
        return templateEngine.process(TEMPLATE, context);
    }

    private static String subject(UUID recordId) {
        String id = recordId.toString();
        return "EBYS - Evrak Durum Değişikliği Bildirimi [#" + id.substring(0, 8) + "]";
    }
}