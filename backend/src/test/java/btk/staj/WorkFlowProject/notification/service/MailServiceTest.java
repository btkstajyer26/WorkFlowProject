package btk.staj.WorkFlowProject.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        mailService = new MailService(mailSender, templateEngine);
    }

    @Test
    void sendStatusChangeMail_sendsSuccessfully() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(any(String.class), any(IContext.class))).thenReturn("<html>Test</html>");

        mailService.sendStatusChangeMail(
                "test@example.com",
                "Evrak Durumu",
                UUID.randomUUID(),
                "Taslak",
                "Başkan İncelemesinde",
                "Açıklama"
        );

        verify(mailSender).send(mimeMessage);
    }
}