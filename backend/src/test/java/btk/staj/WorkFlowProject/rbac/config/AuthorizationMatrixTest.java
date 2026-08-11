package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.audit.repository.UserAuditLogRepository;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.CategoryRepository;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sartnamedeki rol bazli yetki matrisinin gercekten uygulandigini dogrular.
 * Veritabani gerektirmemesi icin JPA/Flyway autoconfig'leri kapatilir.
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
@DisplayName("Yetki matrisi")
class AuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private FileRepository fileRepository;
    @MockitoBean private TokenRepository tokenRepository;
    @MockitoBean private RecordRepository recordRepository;
    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private UserAuditLogRepository userAuditLogRepository;

    private static final String RECORD_JSON = """
            {"title":"Test","description":"Test","categoryId":1}
            """;

    private static final String ACTION_URL = "/api/records/{recordId}/workflow/actions";

    /**
     * Bu test JPA autoconfig'ini kapattigi icin context'te transaction manager
     * bulunmuyor; onay akisi ucu ise @Transactional bir servisten geciyor.
     * Yetki kontrolunu olcmek icin gercek bir transaction gerekmedigi icin
     * no-op bir yonetici veriliyor.
     */
    @TestConfiguration
    static class NoOpTransactionConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new PlatformTransactionManager() {
                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition) {
                    return new SimpleTransactionStatus();
                }

                @Override
                public void commit(TransactionStatus status) {
                }

                @Override
                public void rollback(TransactionStatus status) {
                }
            };
        }
    }

    private void givenRecord(UUID recordId, RecordStatus status) {
        Record record = new Record();
        record.setId(recordId);
        record.setStatus(status);
        record.setCreatedBy(UUID.randomUUID());
        record.setVersion(0);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
    }

    private ResultActions performAction(UUID recordId, RoleName role, String body) throws Exception {
        return mockMvc.perform(post(ACTION_URL, recordId)
                .with(user(actor(role)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** SecurityCurrentActorProvider gercek bir AuthenticatedUser bekler. */
    private static AuthenticatedUser actor(RoleName role) {
        Role roleEntity = new Role();
        roleEntity.setName(role.name());

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(role.name().toLowerCase() + "@ornek.test");
        user.setPasswordHash("x");
        user.setRole(roleEntity);
        user.setActive(true);

        return new AuthenticatedUser(user);
    }

    @Nested
    @DisplayName("Kimlik dogrulamasi olmadan")
    class TokenYokken {

        @Test
        @DisplayName("korumali uc 401 ve JSON hata govdesi doner")
        void korumaliUc401Doner() throws Exception {
            mockMvc.perform(get("/api/v1/records"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("giris ucu acik kalir")
        void girisUcuAcik() throws Exception {
            // 401 DONMEMELI; govde bos oldugu icin baska bir hata donebilir.
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is(not(401)));
        }
    }

    @Nested
    @DisplayName("Kayit olusturma yalnizca Calisan")
    class KayitOlusturma {

        @Test
        @WithMockUser(roles = "BASKAN")
        @DisplayName("Baskan kayit olusturamaz")
        void baskanOlusturamaz() throws Exception {
            mockMvc.perform(post("/api/v1/records")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RECORD_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @WithMockUser(roles = "BASKAN_YARDIMCISI")
        @DisplayName("Baskan Yardimcisi kayit olusturamaz")
        void baskanYrdOlusturamaz() throws Exception {
            mockMvc.perform(post("/api/v1/records")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RECORD_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CALISAN")
        @DisplayName("Calisan icin yetki engeli yoktur")
        void calisanEngellenmez() throws Exception {
            // Servis katmani mock oldugu icin sonuc basarili olmayabilir;
            // onemli olan istegin YETKI nedeniyle reddedilmemesi.
            mockMvc.perform(post("/api/v1/records")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RECORD_JSON))
                    .andExpect(status().is(not(403)));
        }
    }

    /**
     * Onay akisi aksiyonlari tek uctan gecer; hangi rolun hangi durumda hangi
     * aksiyonu alabilecegine durum makinesi karar verir. Tabloda karsiligi
     * olmayan rol/aksiyon birlesimi {@code WORKFLOW_INVALID_TRANSITION} ile
     * reddedilir. Workflow aktoru olmayan ADMIN ise daha erken, ayri bir kodla
     * ({@code WORKFLOW_ROLE_NOT_ALLOWED}) elenir ve 403 alir.
     */
    @Nested
    @DisplayName("Onay ve red yalnizca Baskan")
    class OnayVeRed {

        private final UUID recordId = UUID.randomUUID();

        @Test
        @DisplayName("Calisan onaylayamaz")
        void calisanOnaylayamaz() throws Exception {
            givenRecord(recordId, RecordStatus.BASKAN_INCELEMESINDE);

            performAction(recordId, RoleName.CALISAN, "{\"action\":\"ONAYLA\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("WORKFLOW_INVALID_TRANSITION"));
        }

        @Test
        @DisplayName("Baskan Yardimcisi reddedemez")
        void baskanYrdReddedemez() throws Exception {
            givenRecord(recordId, RecordStatus.BASKAN_INCELEMESINDE);

            performAction(recordId, RoleName.BASKAN_YARDIMCISI,
                    "{\"action\":\"REDDET\",\"comment\":\"Uygun değil\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("WORKFLOW_INVALID_TRANSITION"));
        }

        @Test
        @DisplayName("Baskana iletme Calisana kapali")
        void calisanIletemez() throws Exception {
            givenRecord(recordId, RecordStatus.BSK_YRD_INCELEMESINDE);

            performAction(recordId, RoleName.CALISAN, "{\"action\":\"BASKANA_ILET\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("WORKFLOW_INVALID_TRANSITION"));
        }

        @Test
        @DisplayName("Admin hicbir onay akisi aksiyonunu alamaz")
        void adminIslemYapamaz() throws Exception {
            givenRecord(recordId, RecordStatus.BASKAN_INCELEMESINDE);

            performAction(recordId, RoleName.ADMIN, "{\"action\":\"ONAYLA\"}")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("WORKFLOW_ROLE_NOT_ALLOWED"));
        }
    }

    @Nested
    @DisplayName("Kullanici yonetimi yalnizca Admin")
    class KullaniciYonetimi {

        @Test
        @WithMockUser(roles = "CALISAN")
        @DisplayName("Calisan kullanici olusturamaz")
        void calisanKullaniciOlusturamaz() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "BASKAN")
        @DisplayName("Baskan kullanici olusturamaz")
        void baskanKullaniciOlusturamaz() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Dosya yukleme yalnizca Calisan")
    class DosyaYukleme {

        @Test
        @WithMockUser(roles = "BASKAN")
        @DisplayName("Baskan dosya yukleyemez")
        void baskanYukleyemez() throws Exception {
            mockMvc.perform(multipart("/api/files/upload")
                            .file(new MockMultipartFile("file", "rapor.pdf",
                                    "application/pdf", "icerik".getBytes()))
                            .param("recordId", UUID.randomUUID().toString())
                            .param("uploadedBy", UUID.randomUUID().toString()))
                    .andExpect(status().isForbidden());
        }
    }
}
