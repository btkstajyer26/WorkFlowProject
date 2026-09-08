package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUserFactory;
import btk.staj.WorkFlowProject.notification.exception.InvalidMailActionTokenException;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real commits, workflow, listener, repository and mail template; no external SMTP.
 * Uses the repository's DB_* PostgreSQL configuration (including CI's service).
 * Each context uses a fresh schema to isolate committed fixtures from other tests.
 */
@SpringBootTest(properties = {
        "app.frontend-url=http://b01.invalid", "bootstrap.admin.email=", "bootstrap.admin.password=",
        "fcm.project-id=", "fcm.client-email=", "fcm.private-key="
})
// This unique-schema context cannot be reused; release its pool before other integration tests.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MailActionTokenIntegrationTest {
    private static final String SCHEMA = "b01_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () ->
                "jdbc:postgresql://" + env("DB_HOST", "localhost") + ":" + env("DB_PORT", "5432")
                        + "/" + env("DB_NAME", "workflowdb") + "?currentSchema=" + SCHEMA);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    private static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired WorkflowActionService workflow;
    @Autowired MailActionTokenService tokens;
    @Autowired UserRepository users;
    @Autowired AuthenticatedUserFactory principals;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean JavaMailSender sender;
    @MockitoBean PushNotificationService push;
    @MockitoSpyBean AuditLogService audit;

    private final BlockingQueue<MimeMessage> sent = new LinkedBlockingQueue<>();
    private UUID creator;
    private UUID deputy;
    private UUID president;
    private UUID record;

    @BeforeEach
    void fixture() {
        creator = insertUser("CALISAN");
        deputy = insertUser("BASKAN_YARDIMCISI");
        president = insertUser("BASKAN");
        record = jdbc.queryForObject("""
                INSERT INTO records(title,description,category_id,status,created_by,assigned_to)
                VALUES ('B01 record','B01 test',(SELECT min(id) FROM categories),'BSK_YRD_INCELEMESINDE',?,?)
                RETURNING id
                """, UUID.class, creator, deputy);
        var principal = principals.create(users.findById(deputy).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((jakarta.mail.Session) null));
        doAnswer(invocation -> { sent.add(invocation.getArgument(0)); return null; })
                .when(sender).send(any(MimeMessage.class));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM mail_action_tokens WHERE record_id=?", record);
        jdbc.update("DELETE FROM notifications WHERE record_id=?", record);
        jdbc.update("DELETE FROM audit_logs WHERE record_id=?", record);
        jdbc.update("DELETE FROM records WHERE id=?", record);
        for (UUID id : new UUID[]{creator, deputy, president}) {
            jdbc.update("DELETE FROM users WHERE id=?", id);
        }
    }

    @Test
    void committedMailTokenSupportsPreviewAndSingleConsumption() throws Exception {
        String raw = commitAndReadMailToken();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
        assertThat(jdbc.queryForObject("SELECT token_hash FROM mail_action_tokens WHERE record_id=?",
                String.class, record)).isEqualTo(hash);
        assertThat(tokens.preview(raw).action()).isEqualTo("ONAYLA");
        assertThat(tokens.preview(raw).recordId()).isEqualTo(record);
        assertThat(consumedAt()).isNull();
        assertThat(status()).isEqualTo("BASKAN_INCELEMESINDE");
        assertThat(tokens.consume(raw)).isEqualTo(record);
        assertThat(consumedAt()).isNotNull();
        assertThat(status()).isEqualTo("ONAYLANDI");
        assertThatThrownBy(() -> tokens.consume(raw)).isInstanceOf(InvalidMailActionTokenException.class);
        assertThat(approvalCount()).isEqualTo(1);
        // Terminal mails go to creator and deputy; finish async work before fixture cleanup.
        assertThat(sent.poll(10, TimeUnit.SECONDS)).isNotNull();
        assertThat(sent.poll(10, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void failedActionRollsBackConsumptionAndRecordTogether() throws Exception {
        String raw = commitAndReadMailToken();
        doThrow(new IllegalStateException("B01 audit failure")).when(audit).record(any());
        assertThatThrownBy(() -> tokens.consume(raw)).isInstanceOf(IllegalStateException.class);
        assertThat(consumedAt()).isNull();
        assertThat(status()).isEqualTo("BASKAN_INCELEMESINDE");
        assertThat(tokens.preview(raw).recordId()).isEqualTo(record);
        assertThat(approvalCount()).isZero();
    }

    @Test
    void rolledBackWorkflowDoesNotIssueTokenOrSendMail() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            forward();
            assertThat(tokenCount()).isZero();
            verifyNoInteractions(sender);
            tx.setRollbackOnly();
        });
        assertThat(status()).isEqualTo("BSK_YRD_INCELEMESINDE");
        assertThat(tokenCount()).isZero();
        verifyNoInteractions(sender);
    }

    private String commitAndReadMailToken() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            forward();
            assertThat(tokenCount()).isZero();
            verifyNoInteractions(sender);
        }); // Actual commit dispatches the production AFTER_COMMIT listener.
        // JDBC outside the completed transaction cannot see uncommitted JPA state.
        assertThat(status()).isEqualTo("BASKAN_INCELEMESINDE");
        assertThat(tokenCount()).isEqualTo(1);
        MimeMessage mail = sent.poll(10, TimeUnit.SECONDS);
        assertThat(mail).isNotNull();
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo(president + "@b01.invalid");
        var matcher = Pattern.compile("http://b01.invalid/hizli-islem#token=([A-Za-z0-9_-]+)")
                .matcher(mail.getContent().toString());
        assertThat(matcher.find()).as("Rendered mail must contain an action link").isTrue();
        return matcher.group(1);
    }

    private void forward() {
        workflow.performAction(record, new WorkflowActionRequest(WorkflowAction.BASKANA_ILET, null, "B01"));
    }

    private int tokenCount() {
        return jdbc.queryForObject("SELECT count(*) FROM mail_action_tokens WHERE record_id=?", Integer.class, record);
    }

    private int approvalCount() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE record_id=? AND action='ONAYLA'",
                Integer.class, record);
    }

    private Object consumedAt() {
        return jdbc.queryForMap("SELECT consumed_at FROM mail_action_tokens WHERE record_id=?", record).get("consumed_at");
    }

    private String status() {
        return jdbc.queryForObject("SELECT status FROM records WHERE id=?", String.class, record);
    }

    private UUID insertUser(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users(id,first_name,last_name,email,password_hash,role_id,is_active)
                VALUES (?,'B01','Test',?,'unused',(SELECT id FROM roles WHERE system_key=?),true)
                """, id, id + "@b01.invalid", role);
        return id;
    }
}
