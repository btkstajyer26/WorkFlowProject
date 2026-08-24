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

    private static final String NO_EXPLANATION = "â€”";
    private static final String TEMPLATE = "mail/workflow-status";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Value("${app.mail-from:ebys@ornek.local}")
    private String mailFrom;

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.templateEngine = Objects.requireNonNull(templateEngine, "templateEngine");
    }

    @Async
    public void sendPasswordResetCode(String toEmail, String recipientName, String code, int ttlMinutes) {
        try {
            log.info("Åifre sÄ±fÄ±rlama kodu gÃ¶nderiliyor. AlÄ±cÄ±: {}", toEmail);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject("EBYS - Åifre SÄ±fÄ±rlama Kodu");
            helper.setText("SayÄ±n " + recipientName + ",\n\nÅifre sÄ±fÄ±rlama doÄŸrulama kodunuz: " + code + "\nBu kod " + ttlMinutes + " dakika boyunca geÃ§erlidir.", false);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Åifre sÄ±fÄ±rlama kodu gÃ¶nderilirken hata oluÅŸtu: " + toEmail, e);
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
            log.info("E-posta gÃ¶nderimi baÅŸlatÄ±lÄ±yor. AlÄ±cÄ±: {}, Evrak: {}, Durum: {}", toEmail, recordId, status);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject(subject(recordId));
            helper.setText(render(recipientName, recordId, title, status, reason), true);

            mailSender.send(mimeMessage);
            log.info("E-posta baÅŸarÄ±yla gÃ¶nderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("E-posta gÃ¶nderilirken hata oluÅŸtu! AlÄ±cÄ±: " + toEmail, e);
        }
    }

    String render(String recipientName, UUID recordId, String title, String status, String reason) {
        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("recordId", recordId);
        context.setVariable("title", title);

        // Testler doÄŸrudan gelen status string'inin (Ã¶rn: ONAYLANDI) ÅŸablonda yer almasÄ±nÄ± bekler
        context.setVariable("status", status != null ? status : "Ä°ncelemede");

        context.setVariable("explanation", (reason == null || reason.isBlank()) ? NO_EXPLANATION : reason);
        context.setVariable("deepLink", frontendUrl + "/records/" + recordId);

        String upper = status != null ? status.trim().toUpperCase(Locale.ENGLISH) : "";
        String quickActionBase = backendUrl + "/api/public/notification/quick-action?recordId=" + recordId + "&action=";

        // 1. Nihai durumlar -> Buton gÃ¶sterilmez
        if (upper.contains("ONAYLANDI") || upper.contains("REDDEDILDI") || upper.contains("APPROV") || upper.contains("REJECT")) {
            context.setVariable("showActionBtn", false);
        }
        // 2. BaÅŸkana gelen bildirim -> ONAYLA aksiyonu
        else if (upper.contains("BASKAN_INCELEMESINDE") || upper.contains("FORWARD") || upper.contains("PRESIDENT")) {
            context.setVariable("showActionBtn", true);
            context.setVariable("actionText", "Onayla");
            context.setVariable("actionLink", quickActionBase + "ONAYLA");
        }
        // 3. BaÅŸkan YardÄ±mcÄ±sÄ±na gelen bildirim -> BASKANA_ILET aksiyonu
        else if (upper.contains("BSK_YRD_INCELEMESINDE") || upper.contains("SUBMIT") || upper.contains("DEPUTY")) {
            context.setVariable("showActionBtn", true);
            context.setVariable("actionText", "Onayla");
            context.setVariable("actionLink", quickActionBase + "BASKANA_ILET");
        }
        // 4. Ã‡alÄ±ÅŸana gelen bildirim -> GONDER aksiyonu
        else {
            context.setVariable("showActionBtn", true);
            context.setVariable("actionText", "Onayla");
            context.setVariable("actionLink", quickActionBase + "GONDER");
        }

        return templateEngine.process(TEMPLATE, context);
    }

    private static String subject(UUID recordId) {
        String id = recordId.toString();
        return "EBYS - Evrak Durum DeÄŸiÅŸikliÄŸi Bildirimi [#" + id.substring(0, 8) + "]";
    }
}