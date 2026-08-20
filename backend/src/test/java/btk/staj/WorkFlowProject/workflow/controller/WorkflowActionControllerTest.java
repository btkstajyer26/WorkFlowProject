package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.audit.repository.UserAuditLogRepository;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.notification.repository.NotificationRepository;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.CategoryRepository;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.auth.repository.PasswordResetCodeRepository;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onay akisi ucunun uctan uca kablolamasini dogrular: HTTP -> transaction
 * siniri -> durum makinesi -> kayit guncellemesi + denetim izi.
 *
 * <p>Bu testin asil degeri parcalarin tek tek dogru olmasi degil, birbirine
 * bagli olmasi. Daha once uclar durum makinesini hic cagirmiyordu.
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
@DisplayName("Onay akisi ucu")
class WorkflowActionControllerTest {

    private static final String ACTION_URL = "/api/records/{recordId}/workflow/actions";
    private static final int BASKAN_ROLE_ID = 3;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private FileRepository fileRepository;
    @MockitoBean private TokenRepository tokenRepository;
    @MockitoBean private PasswordResetCodeRepository passwordResetCodeRepository;
    @MockitoBean private RecordRepository recordRepository;
    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private UserAuditLogRepository userAuditLogRepository;
    @MockitoBean private NotificationRepository notificationRepository;

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

    @Test
    @DisplayName("Baskan kendisine atanan kaydi onaylar, kayit ve denetim izi birlikte yazilir")
    void anApprovalUpdatesTheRecordAndWritesTheAuditTrail() throws Exception {
        UUID recordId = UUID.randomUUID();
        AuthenticatedUser baskan = actor(RoleName.BASKAN);
        givenRecord(recordId, RecordStatus.BASKAN_INCELEMESINDE, baskan.getId());
        givenRole(RoleName.BASKAN, BASKAN_ROLE_ID);

        mockMvc.perform(post(ACTION_URL, recordId)
                        .with(user(baskan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"ONAYLA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousStatus").value("BASKAN_INCELEMESINDE"))
                .andExpect(jsonPath("$.newStatus").value("ONAYLANDI"))
                .andExpect(jsonPath("$.performedBy").value(baskan.getId().toString()));

        ArgumentCaptor<Record> savedRecord = ArgumentCaptor.forClass(Record.class);
        verify(recordRepository).saveAndFlush(savedRecord.capture());
        assertThat(savedRecord.getValue().getStatus()).isEqualTo(RecordStatus.ONAYLANDI);

        ArgumentCaptor<AuditLog> savedAudit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(savedAudit.capture());
        assertThat(savedAudit.getValue().getAction()).isEqualTo("ONAYLA");
        assertThat(savedAudit.getValue().getNewStatus()).isEqualTo("ONAYLANDI");
        assertThat(savedAudit.getValue().getRoleId()).isEqualTo(BASKAN_ROLE_ID);
        assertThat(savedAudit.getValue().getUserId()).isEqualTo(baskan.getId());
    }

    @Test
    @DisplayName("kaydin atanani olmayan Baskan onaylayamaz ve hicbir yazma olmaz")
    void anUnassignedPresidentIsRejectedWithoutAnyWrite() throws Exception {
        UUID recordId = UUID.randomUUID();
        givenRecord(recordId, RecordStatus.BASKAN_INCELEMESINDE, UUID.randomUUID());

        mockMvc.perform(post(ACTION_URL, recordId)
                        .with(user(actor(RoleName.BASKAN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"ONAYLA\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKFLOW_FORBIDDEN"));

        verify(recordRepository).findById(recordId);
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("zorunlu aciklama olmadan geri gonderme reddedilir")
    void aReturnWithoutTheRequiredCommentIsRejected() throws Exception {
        UUID recordId = UUID.randomUUID();
        AuthenticatedUser baskan = actor(RoleName.BASKAN);
        givenRecord(recordId, RecordStatus.BASKAN_INCELEMESINDE, baskan.getId());

        mockMvc.perform(post(ACTION_URL, recordId)
                        .with(user(baskan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"CALISANA_GERI_GONDER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKFLOW_COMMENT_REQUIRED"));

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("kilitli kayit uzerinde islem yapilamaz")
    void aTerminalRecordIsLocked() throws Exception {
        UUID recordId = UUID.randomUUID();
        AuthenticatedUser baskan = actor(RoleName.BASKAN);
        givenRecord(recordId, RecordStatus.ONAYLANDI, baskan.getId());

        mockMvc.perform(post(ACTION_URL, recordId)
                        .with(user(baskan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"ONAYLA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_RECORD_LOCKED"));

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("olmayan kayit 404 doner")
    void aMissingRecordIsReportedAsNotFound() throws Exception {
        UUID recordId = UUID.randomUUID();
        when(recordRepository.findById(recordId)).thenReturn(Optional.empty());

        mockMvc.perform(post(ACTION_URL, recordId)
                        .with(user(actor(RoleName.BASKAN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"ONAYLA\"}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(auditLogRepository);
    }

    /**
     * Hedefi backend cozdugu icin aktif yardimci yoksa istek bir sunucu hatasi
     * degil, gecici bir catisma olarak raporlanir: Calisan bunu "Gonder"
     * tusunda gorebilir (ornegin yardimci devri sirasinda).
     */
    @Test
    @DisplayName("tek aktif Baskan Yardimcisi yoksa gonderme 409 doner")
    void sendingWithoutASingleActiveDeputyIsReportedAsConflict() throws Exception {
        UUID recordId = UUID.randomUUID();
        AuthenticatedUser calisan = actor(RoleName.CALISAN);
        givenOwnedRecord(recordId, RecordStatus.TASLAK, calisan.getId());
        when(userRepository.findByRole_NameAndActive(RoleName.BASKAN_YARDIMCISI.name(), true))
                .thenReturn(List.of());

        mockMvc.perform(post(ACTION_URL, recordId)
                        .with(user(calisan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"GONDER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_ROLE_NOT_CONFIGURED"));

        verifyNoInteractions(auditLogRepository);
    }

    /** Karar 4: istemci yine hedef gonderirse sessizce yok sayilmaz. */
    @Test
    @DisplayName("gonderme isteginde targetUserId gonderilirse reddedilir")
    void aSendRequestCarryingATargetIsRejected() throws Exception {
        UUID recordId = UUID.randomUUID();
        AuthenticatedUser calisan = actor(RoleName.CALISAN);
        givenOwnedRecord(recordId, RecordStatus.TASLAK, calisan.getId());

        mockMvc.perform(post(ACTION_URL, recordId)
                        .with(user(calisan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"GONDER\",\"targetUserId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TARGET_NOT_ALLOWED"));

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("aksiyonu olmayan istek dogrulamada elenir")
    void aRequestWithoutAnActionFailsValidation() throws Exception {
        mockMvc.perform(post(ACTION_URL, UUID.randomUUID())
                        .with(user(actor(RoleName.BASKAN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(auditLogRepository);
    }

    private void givenRecord(UUID recordId, RecordStatus status, UUID assignedTo) {
        Record record = new Record();
        record.setId(recordId);
        record.setStatus(status);
        record.setCreatedBy(UUID.randomUUID());
        record.setAssignedTo(assignedTo);
        record.setVersion(0);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
    }

    /** Gonderme aksiyonlarinda aktorun kaydin sahibi olmasi gerekir. */
    private void givenOwnedRecord(UUID recordId, RecordStatus status, UUID createdBy) {
        Record record = new Record();
        record.setId(recordId);
        record.setStatus(status);
        record.setCreatedBy(createdBy);
        record.setVersion(0);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
    }

    private void givenRole(RoleName roleName, int roleId) {
        Role role = new Role();
        role.setId(roleId);
        role.setName(roleName.name());
        when(roleRepository.findByName(roleName.name())).thenReturn(Optional.of(role));
    }

    private static AuthenticatedUser actor(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName.name());

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(roleName.name().toLowerCase() + "@ornek.test");
        user.setPasswordHash("x");
        user.setRole(role);
        user.setActive(true);

        return new AuthenticatedUser(user);
    }
}
