package btk.staj.WorkFlowProject.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sablonun gercek dosyadan islenmesini dogrular; sahte bir motor kullanilsaydi
 * {@code templates/mail/workflow-status.html} bozuldugunda test yine gecerdi.
 */
@DisplayName("Durum degisikligi e-postasi")
class MailServiceTest {

    private static final UUID RECORD_ID = UUID.fromString("1a2b3c4d-0000-0000-0000-000000000060");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MailService mailService = new MailService(mailSender, templateEngine());

    MailServiceTest() {
        ReflectionTestUtils.setField(mailService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(mailService, "mailFrom", "ebys@ornek.local");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
    }

    @Test
    @DisplayName("evrak bilgileri ve derin baglanti govdeye islenir")
    void theRecordDetailsAreRendered() throws Exception {
        mailService.sendStatusChangeMail(
                "alici@ornek.local", "Ayşe Yılmaz", RECORD_ID, "Bütçe teklifi", "ONAYLANDI", "Uygun görüldü");

        String body = sentBody();
        assertThat(body).contains("Ayşe Yılmaz");
        assertThat(body).contains("Bütçe teklifi");
        assertThat(body).contains("ONAYLANDI");
        assertThat(body).contains("Uygun görüldü");
        assertThat(body).contains("http://localhost:5173/records/" + RECORD_ID);
    }

    @Test
    @DisplayName("aciklama bos birakildiginda govdede 'null' gorunmez")
    void aBlankExplanationIsReplaced() throws Exception {
        mailService.sendStatusChangeMail(
                "alici@ornek.local", "Ayşe Yılmaz", RECORD_ID, "Bütçe teklifi", "ONAYLANDI", "  ");

        assertThat(sentBody()).doesNotContain("null").contains("—");
    }

    @Test
    @DisplayName("evrak basligindaki HTML kacisli yazilir")
    void theTitleIsEscaped() throws Exception {
        mailService.sendStatusChangeMail(
                "alici@ornek.local", "Ayşe Yılmaz", RECORD_ID, "<script>alert(1)</script>", "ONAYLANDI", null);

        assertThat(sentBody()).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("SMTP hatasi cagiran akisa yansimaz")
    void anSmtpFailureIsSwallowed() {
        when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("SMTP kapali"));

        mailService.sendStatusChangeMail(
                "alici@ornek.local", "Ayşe Yılmaz", RECORD_ID, "Bütçe teklifi", "ONAYLANDI", null);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private String sentBody() throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue().getContent().toString();
    }

    /**
     * Uygulamada Spring Boot'un kurdugu motorun aynisi kullanilir: sade
     * {@code TemplateEngine} ifade degerlendirmesi icin OGNL bekler, starter ise
     * SpEL kullanan {@code SpringTemplateEngine}'i kurar ve OGNL'i getirmez.
     */
    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
