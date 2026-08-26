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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String NO_EXPLANATION = "—";
    private static final String TEMPLATE = "mail/workflow-status";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl = "http://localhost:5173";

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl = "http://localhost:8080";

    @Value("${app.mail-from:ebys@ornek.local}")
    private String mailFrom = "ebys@ornek.local";

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.templateEngine = Objects.requireNonNull(templateEngine, "templateEngine");
    }

    @Async
    public void sendPasswordResetCode(String toEmail, String recipientName, String code, int ttlMinutes) {
        try {
            log.info("Sıfırlama kodu gönderiliyor. Alıcı: {}", toEmail);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom != null ? mailFrom : "ebys@ornek.local");
            helper.setTo(toEmail);
            helper.setSubject("EBYS - Şifre Sıfırlama Kodu");
            
            String text = "Sayın " + recipientName + ",\n\n"
                    + "Şifre sıfırlama talebiniz için oluşturulan doğrulama kodunuz aşağıdadır:\n\n"
                    + code + "\n\n"
                    + "Bu kod " + ttlMinutes + " dakika boyunca geçerlidir.\n"
                    + "Talebi siz yapmadıysanız bu e-postayı dikkate almayınız.";
            
            helper.setText(text, false);

            mailSender.send(mimeMessage);
            log.info("Sıfırlama kodu başarıyla gönderildi: {}", toEmail);
        } catch (Exception e) {
            log.error("Sıfırlama kodu gönderilirken hata oluştu: " + toEmail, e);
        }
    }

    @Async
    public void sendStatusChangeMail(String toEmail,
                                     String recipientName,
                                     UUID recordId,
                                     String title,
                                     String status,
                                     String reason) {
        try {
            log.info("E-posta gönderimi başlatılıyor. Alıcı: {}, Evrak: {}, Durum: {}", toEmail, recordId, status);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom != null ? mailFrom : "ebys@ornek.local");
            helper.setTo(toEmail);
            helper.setSubject(subject(recordId));
            helper.setText(render(recipientName, recordId, title, status, reason), true);

            mailSender.send(mimeMessage);
            log.info("E-posta başarıyla gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("E-posta gönderilirken hata oluştu! Alıcı: " + toEmail, e);
            throw new RuntimeException("E-posta gönderilemedi", e);
        }
    }

    String render(String recipientName, UUID recordId, String title, String status, String reason) {
        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("recordId", recordId);
        context.setVariable("title", title);
        context.setVariable("status", status != null ? status : "İncelemede");
        context.setVariable("explanation", (reason == null || reason.isBlank()) ? NO_EXPLANATION : reason);
        context.setVariable("deepLink", (frontendUrl != null ? frontendUrl : "http://localhost:5173") + "/records/" + recordId);

        String upper = status != null ? status.trim().toUpperCase(Locale.ENGLISH) : "";
        String base = (backendUrl != null ? backendUrl : "http://localhost:8080") + "/api/public/notification/quick-action?recordId=" + recordId + "&action=";

        if (upper.contains("ONAYLANDI") || upper.contains("REDDEDILDI") || upper.contains("APPROV") || upper.contains("REJECT")) {
            context.setVariable("showActionBtn", false);
        } else if (upper.contains("BASKAN_INCELEMESINDE") || upper.contains("FORWARD") || upper.contains("PRESIDENT")) {
            context.setVariable("showActionBtn", true);
            context.setVariable("actionText", "Onayla");
            context.setVariable("actionLink", base + "ONAYLA");
        } else if (upper.contains("BSK_YRD_INCELEMESINDE") || upper.contains("SUBMIT") || upper.contains("DEPUTY")) {
            context.setVariable("showActionBtn", true);
            context.setVariable("actionText", "Onayla");
            context.setVariable("actionLink", base + "BASKANA_ILET");
        } else {
            context.setVariable("showActionBtn", true);
            context.setVariable("actionText", "Onayla");
            context.setVariable("actionLink", base + "GONDER");
        }

        return templateEngine.process(TEMPLATE, context);
    }

    private static String subject(UUID recordId) {
        String id = recordId.toString();
        return "EBYS - Evrak Durum Değişikliği Bildirimi [#" + id.substring(0, 8) + "]";
    }
}