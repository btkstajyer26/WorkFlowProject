package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.auth.repository.PasswordResetCodeRepository;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.notification.repository.DeviceTokenRepository;
import btk.staj.WorkFlowProject.notification.repository.MailActionTokenRepository;
import btk.staj.WorkFlowProject.notification.repository.NotificationRepository;
import btk.staj.WorkFlowProject.audit.repository.UserAuditLogRepository;
import btk.staj.WorkFlowProject.record.repository.CategoryRepository;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.StaticTransitionRuleReaderConfiguration;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger arayuzunun kimlik dogrulama istemeden acilabildigini dogrular.
 * Veritabani gerektirmemesi icin JPA/Flyway autoconfig'leri kapatilir ve
 * repository'ler mock'lanir.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
@DisplayName("Swagger erisimi")
@Import(StaticTransitionRuleReaderConfiguration.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private FileRepository fileRepository;

    @MockitoBean
    private TokenRepository tokenRepository;

    @MockitoBean
    private PasswordResetCodeRepository passwordResetCodeRepository;

    @MockitoBean
    private RecordRepository recordRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private UserAuditLogRepository userAuditLogRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private DeviceTokenRepository deviceTokenRepository;
    @MockitoBean private MailActionTokenRepository mailActionTokenRepository;
    @MockitoBean private WorkflowTransitionRepository workflowTransitionRepository;
    // Swagger/security checks run without JPA; WF-8 has a separate PostgreSQL acceptance suite.
    @MockitoBean private btk.staj.WorkFlowProject.workflow.service.WorkflowActorBindingService workflowActorBindingService;
    @MockitoBean private btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository rolePermissionRepository;

    @Test
    @DisplayName("swagger-ui.html giris istemeden yonlendirme doner")
    void swaggerUiKimlikDogrulamaIstemez() throws Exception {
        // Sadece 3xx yeterli degil: kimlik dogrulama acikken de login sayfasina
        // 302 donerdi. Yonlendirmenin hedefi Swagger arayuzu olmali.
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    @DisplayName("OpenAPI semasi giris istemeden 200 doner")
    void apiDocsKimlikDogrulamaIstemez() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("swagger-ui statik dosyalari giris istemeden acilir")
    void swaggerStatikDosyalariAcilir() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    /**
     * Reverse proxy ve container healthcheck'i bu ucu token'siz yoklar.
     *
     * <p>Test ayni zamanda {@code management.health.mail.enabled=false}
     * ayarinin dogrulugunu kanitlar: mail saglik gostergesi acik kalsaydi
     * SMTP'ye baglanmayi deneyip basarisiz olur ve uc 200 yerine 503 donerdi.
     */
    @Test
    @DisplayName("actuator health giris istemeden 200 doner")
    void healthUcuKimlikDogrulamaIstemez() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
