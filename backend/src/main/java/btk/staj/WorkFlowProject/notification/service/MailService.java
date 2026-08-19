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
 *
 * <p>Govde {@code templates/mail/workflow-status.html} sablonundan uretilir;
 * bicim degisikligi Java kodunu degistirmeyi gerektirmez ve kullanicidan gelen
 * degerler (evrak basligi, aciklama) sablon motoru tarafindan kacisli yazilir.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    /** Bos aciklama sablonda "null" gorunmemeli. */
    private static final String NO_EXPLANATION = "—";

    private static final String TEMPLATE = "mail/workflow-status";

    private static final String PASSWORD_RESET_TEMPLATE = "mail/password-reset-code";

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

    /**
     * E-posta gonderimi cagiran akisi bilerek etkilemez: gonderim
     * {@code @Async} calisir ve hata yalnizca loglanir. Onay akisi disaridaki
     * SMTP sunucusuna bagli kalmamalidir.
     */
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

    /**
     * "Şifremi unuttum" kodunu gönderir.
     *
     * <p>Durum bildirimlerinden farklı olarak hata yutulamaz: kod ulaşmazsa
     * kullanıcı akışı tamamlayamaz. Yine de {@code @Async} kalır ve istisna
     * yalnızca loglanır &mdash; uca dönen cevap, hesabın varlığını sızdırmamak
     * için her koşulda aynı olmalıdır.
     */
    @Async
    public void sendPasswordResetCode(String toEmail, String recipientName, String code, int validForMinutes) {
        try {
            log.info("Şifre sıfırlama kodu gönderiliyor. Alıcı: {}", toEmail);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject("EBYS - Şifre Sıfırlama Doğrulama Kodu");
            helper.setText(renderPasswordResetCode(recipientName, code, validForMinutes), true);

            mailSender.send(mimeMessage);
            log.info("Şifre sıfırlama kodu gönderildi: {}", toEmail);

        } catch (Exception e) {
            // Kodun kendisi loglanmaz: log dosyası okuyan biri hesabı ele geçirebilirdi.
            log.error("Şifre sıfırlama kodu gönderilemedi! Alıcı: {}, Hata: {}", toEmail, e.getMessage());
        }
    }

    private String renderPasswordResetCode(String recipientName, String code, int validForMinutes) {
        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("code", code);
        context.setVariable("validForMinutes", validForMinutes);
        return templateEngine.process(PASSWORD_RESET_TEMPLATE, context);
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

    /** Konu basligindaki kisa kimlik; UUID'nin tamami basliga sigmiyor. */
    private static String subject(UUID recordId) {
        String id = recordId.toString();
        return "EBYS - Evrak Durum Değişikliği Bildirimi [#" + id.substring(0, 8) + "]";
    }
}
