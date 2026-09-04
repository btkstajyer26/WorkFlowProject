package btk.staj.WorkFlowProject;

import btk.staj.WorkFlowProject.attachment.service.FileContentValidator;
import btk.staj.WorkFlowProject.attachment.service.FileService;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUserFactory;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.auth.service.AuthService;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.notification.service.PushNotificationService;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import btk.staj.WorkFlowProject.record.dto.RecordUpdateRequest;
import btk.staj.WorkFlowProject.record.service.RecordService;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import btk.staj.WorkFlowProject.workflow.adapter.RecordPortAdapter;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;
import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Review-only probes. Run against a disposable database; these assert intended behavior. */
@SpringBootTest
class ReviewRegressionProbeTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;
    @Autowired ApplicationEventPublisher events;
    @Autowired RecordService records;
    @Autowired RecordRepository recordRepository;
    @Autowired RecordPortAdapter recordPort;
    @Autowired RecordAccessPolicy recordPolicy;
    @Autowired ReloadableTransitionRuleSource rules;
    @Autowired WorkflowActionService workflow;
    @Autowired FileService files;
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired AuthenticatedUserFactory principals;
    @MockitoBean MailService mail;
    @MockitoBean PushNotificationService push;
    @MockitoBean FileStorageService storage;
    @MockitoBean FileContentValidator content;
    @MockitoSpyBean JwtUtil jwt;
    UUID creator;
    UUID deputy;
    UUID record;
    int category;
    int employeeRole;

    @BeforeEach void fixture() {
        assertThat(System.getenv("DB_NAME")).isEqualTo("workflow_review_20260904");
        employeeRole = jdbc.queryForObject("SELECT id FROM roles WHERE system_key='CALISAN'", Integer.class);
        creator = user(employeeRole);
        deputy = user(jdbc.queryForObject("SELECT id FROM roles WHERE system_key='BASKAN_YARDIMCISI'", Integer.class));
        category = jdbc.queryForObject("SELECT min(id) FROM categories", Integer.class);
        record = jdbc.queryForObject("INSERT INTO records(title,description,category_id,status,created_by) VALUES ('review probe','review probe',?,'TASLAK',?) RETURNING id", UUID.class, category, creator);
        var principal = principals.create(users.findById(creator).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(storage.loadAsResource(anyString())).thenReturn(new ByteArrayResource(new byte[] {1}));
        when(content.detectAndValidate(any())).thenReturn("application/pdf");
        when(content.extensionFor(anyString())).thenReturn(".pdf");
    }

    UUID user(int role) {
        return jdbc.queryForObject("INSERT INTO users(first_name,last_name,email,password_hash,role_id,is_active) VALUES ('Review','Probe',?,'review-only-unused-hash',?,true) RETURNING id", UUID.class, UUID.randomUUID()+"@review.invalid", role);
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void quickActionTokenMustPersistAfterWorkflowCommit() {
        new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            jdbc.update("UPDATE records SET status='BSK_YRD_INCELEMESINDE',assigned_to=? WHERE id=?", deputy, record);
            events.publishEvent(new WorkflowStatusChangedEvent(record, WorkflowAction.GONDER,
                    RecordStatus.TASLAK, RecordStatus.BSK_YRD_INCELEMESINDE, creator,
                    new RoleId(employeeRole), null, deputy, null, Instant.now()));
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mail_action_tokens WHERE record_id=?", Integer.class, record))
                .as("A committed workflow notification must have a usable persisted quick-action token")
                .isEqualTo(1);
    }

    @Test void deletedDraftMustRejectUpdate() {
        jdbc.update("UPDATE records SET deleted_at=current_timestamp WHERE id=?", record);
        RecordUpdateRequest request = new RecordUpdateRequest();
        request.setTitle("changed after deletion");
        request.setDescription("changed after deletion");
        request.setCategoryId(category);
        assertThatThrownBy(() -> records.updateRecord(record, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void frozenListedAttachmentMustRemainDownloadable() {
        jdbc.update("UPDATE records SET status='DUZENLEME_BEKLIYOR',assigned_to=?,last_deputy_id=?,snapshot_title=title,snapshot_description=description,snapshot_category_id=category_id,snapshot_at=current_timestamp-interval '10 minutes' WHERE id=?", creator, deputy, record);
        UUID file = jdbc.queryForObject("INSERT INTO files(record_id,original_name,stored_name,mime_type,file_size,uploaded_by,uploaded_at,deleted_at) VALUES (?,'review.pdf',?,'application/pdf',1,?,current_timestamp-interval '20 minutes',current_timestamp) RETURNING id", UUID.class, record, UUID.randomUUID()+".pdf", creator);
        var actor = VisibilityActor.from(principals.create(users.findById(deputy).orElseThrow()));
        assertThat(files.listByRecord(record, actor)).anyMatch(f -> f.getId().equals(file));
        assertThatCode(() -> files.downloadFile(file, actor)).doesNotThrowAnyException();
    }

    @Test void fileUploadMustNotCommitAfterRecordLeavesEditableStatus() throws Exception {
        CountDownLatch validatedRecord = new CountDownLatch(1);
        CountDownLatch changedRecord = new CountDownLatch(1);
        when(content.detectAndValidate(any())).thenAnswer(call -> {
            validatedRecord.countDown();
            if (!changedRecord.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("probe latch timeout");
            return "application/pdf";
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var upload = executor.submit(() -> files.uploadFile(new MockMultipartFile("files", "probe.pdf", "application/pdf", new byte[] {1}), record, creator));
            assertThat(validatedRecord.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                jdbc.update("UPDATE records SET status='BSK_YRD_INCELEMESINDE',assigned_to=?,version=version+1 WHERE id=?", deputy, record);
            } finally { changedRecord.countDown(); }
            upload.get(10, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM files WHERE record_id=?", Integer.class, record))
                    .as("Upload authorized against an obsolete editable status must be rolled back").isZero();
        }
    }

    @Test void concurrentRefreshMustOnlyConsumeOldTokenOnce() throws Exception {
        String token = "review-refresh-"+UUID.randomUUID();
        jdbc.update("INSERT INTO tokens(user_id,token,token_type,expires_at) VALUES (?,?,'REFRESH',current_timestamp+interval '1 day')", creator, token);
        CyclicBarrier bothReadToken = new CyclicBarrier(2);
        doAnswer(call -> { bothReadToken.await(10, TimeUnit.SECONDS); return call.callRealMethod(); })
                .when(jwt).generateAccessToken(eq(creator), anyString(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { try { auth.refresh(token); return true; } catch (Exception e) { return false; } });
            var second = executor.submit(() -> { try { auth.refresh(token); return true; } catch (Exception e) { return false; } });
            int accepted = (first.get(15, TimeUnit.SECONDS) ? 1 : 0)+(second.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(accepted).as("A refresh token must rotate exactly once").isEqualTo(1);
        }
    }

    @Test void frozenRecordSearchMustNotMatchHiddenLiveContent() {
        jdbc.update("UPDATE records SET status='DUZENLEME_BEKLIYOR',assigned_to=?,last_deputy_id=?,snapshot_title='public old title',snapshot_description='public old description',snapshot_category_id=category_id,snapshot_at=current_timestamp-interval '10 minutes',description='hiddenword8675309' WHERE id=?", creator, deputy, record);
        var actor = VisibilityActor.from(principals.create(users.findById(deputy).orElseThrow()));
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setQ("hiddenword8675309");
        assertThat(recordRepository.findAll(RecordSpecifications.withFilters(criteria, recordPolicy.scopeFor(actor))))
                .as("A viewer limited to the handoff snapshot must not infer hidden edits through search")
                .noneMatch(found -> found.getId().equals(record));
    }

    @Test void reassignmentMustInvalidateInFlightWorkflowSnapshot() throws Exception {
        UUID replacement = user(employeeRole);
        jdbc.update("UPDATE records SET status='BSK_YRD_INCELEMESINDE',assigned_to=?,last_deputy_id=? WHERE id=?", deputy, deputy, record);
        CountDownLatch readOldRecord = new CountDownLatch(1);
        CountDownLatch reassigned = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var staleWriter = executor.submit(() -> new TransactionTemplate(txManager).execute(tx -> {
                var old = recordPort.findById(record).orElseThrow();
                readOldRecord.countDown();
                try { if (!reassigned.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("probe latch timeout"); }
                catch (InterruptedException e) { throw new RuntimeException(e); }
                recordPort.update(new WorkflowRecordUpdate(record, RecordStatus.DUZENLEME_BEKLIYOR,
                        creator, old.lastDeputyId(), old.version(), Instant.now(), null));
                return true;
            }));
            assertThat(readOldRecord.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                new TransactionTemplate(txManager).executeWithoutResult(tx -> {
                    recordRepository.devretBekleyenIsleri(deputy, replacement);
                    recordRepository.updateLastDeputyId(deputy, replacement);
                });
            } finally { reassigned.countDown(); }
            assertThatThrownBy(() -> staleWriter.get(15, TimeUnit.SECONDS))
                    .as("A committed task reassignment must reject an older workflow write").isNotNull();
        }
    }

    @Test void dynamicDepartmentForwardMustSupportReturnToPreviousActor() {
        int dynamicRole = jdbc.queryForObject("INSERT INTO roles(name,is_active,is_workflow_actor) VALUES (?,true,true) RETURNING id", Integer.class, "review-"+UUID.randomUUID());
        UUID member = user(dynamicRole);
        UUID president = user(jdbc.queryForObject("SELECT id FROM roles WHERE system_key='BASKAN'", Integer.class));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) SELECT ?,id FROM permissions WHERE code IN ('RECORD_VIEW','RECORD_FORWARD','RECORD_RETURN')", dynamicRole);
        jdbc.update("INSERT INTO workflow_transitions(from_status_id,action_id,actor_role_id,actor_requirement,to_status_id,expected_target_role_id,target_strategy,required_permission_id) SELECT t.from_status_id,t.action_id,?,t.actor_requirement,t.to_status_id,t.expected_target_role_id,t.target_strategy,t.required_permission_id FROM workflow_transitions t JOIN roles r ON r.id=t.actor_role_id JOIN workflow_actions a ON a.id=t.action_id WHERE r.system_key='BASKAN_YARDIMCISI' AND a.name='BASKANA_ILET'", dynamicRole);
        int department = jdbc.queryForObject("INSERT INTO departments(name) VALUES (?) RETURNING id", Integer.class, "review-"+UUID.randomUUID());
        jdbc.update("INSERT INTO department_members(department_id,user_id) VALUES (?,?)", department, member);
        jdbc.update("INSERT INTO department_routing_rules(department_id,from_status_id,action_id,target_role_id) SELECT ?,s.id,a.id,? FROM workflow_statuses s,workflow_actions a WHERE s.name='BSK_YRD_INCELEMESINDE' AND a.name='BASKANA_ILET'", department,dynamicRole);
        jdbc.update("UPDATE records SET status='BSK_YRD_INCELEMESINDE',assigned_department_id=? WHERE id=?",department,record);
        rules.reload();
        var sender = principals.create(users.findById(member).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(sender,null,sender.getAuthorities()));
        workflow.performAction(record,new WorkflowActionRequest(WorkflowAction.BASKANA_ILET,null,null));
        assertThat(jdbc.queryForObject("SELECT last_deputy_id FROM records WHERE id=?",UUID.class,record)).isEqualTo(member);
        var approver = principals.create(users.findById(president).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(approver,null,approver.getAuthorities()));
        assertThatCode(() -> workflow.performAction(record,new WorkflowActionRequest(WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,null,"review return")))
                .as("Department forwarding must define a usable return to the previous actor").doesNotThrowAnyException();
    }
}
