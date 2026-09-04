package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.attachment.controller.FileController;
import btk.staj.WorkFlowProject.attachment.service.FileService;
import btk.staj.WorkFlowProject.audit.controller.UserAuditLogController;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.controller.RoleAdminController;
import btk.staj.WorkFlowProject.rbac.dto.CreateRoleRequest;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRoleRequest;
import btk.staj.WorkFlowProject.rbac.service.RoleAdminService;
import btk.staj.WorkFlowProject.record.controller.RecordController;
import btk.staj.WorkFlowProject.record.service.RecordService;
import btk.staj.WorkFlowProject.search.service.RecordSearchService;
import btk.staj.WorkFlowProject.user.controller.AdminController;
import btk.staj.WorkFlowProject.user.dto.*;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real method-security proxies, with no role or ADMIN_PANEL_ACCESS fallback. */
@SpringJUnitConfig(EndpointPermissionMatrixTest.Config.class)
class EndpointPermissionMatrixTest {
    @Configuration
    @EnableMethodSecurity
    @Import({AdminController.class, RoleAdminController.class, RecordController.class, FileController.class,
            UserAuditLogController.class})
    static class Config {
        @Bean UserService users() { return mock(UserService.class); }
        @Bean RoleAdminService roleAdmin() { return mock(RoleAdminService.class); }
        @Bean RecordService records() { return mock(RecordService.class); }
        @Bean RecordSearchService search() { return mock(RecordSearchService.class); }
        @Bean FileService files() { return mock(FileService.class); }
        @Bean AuditLogService audit() { return mock(AuditLogService.class); }
        @Bean UserAuditLogService userAudit() { return mock(UserAuditLogService.class); }
    }

    @Autowired AdminController admin;
    @Autowired RoleAdminController roleAdmin;
    @Autowired RoleAdminService roleAdminService;
    @Autowired RecordController records;
    @Autowired FileController files;
    @Autowired UserAuditLogController userAudit;
    @Autowired UserService userService;
    @Autowired RecordService recordService;
    @Autowired FileService fileService;
    @Autowired AuditLogService auditService;
    @Autowired UserAuditLogService userAuditService;
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach void prepare() {
        reset(userService, roleAdminService, recordService, fileService, auditService, userAuditService);
        when(userService.createUser(any(), any(), any(), any())).thenReturn(new User());
        when(userService.changeRole(any(), any(Integer.class), any())).thenReturn(new User());
        when(userService.setActive(any(), anyBoolean())).thenReturn(new User());
        clearInvocations(userService);
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    static Stream<Arguments> matrix() {
        return Stream.of(
                new String[]{"record-create", "RECORD_CREATE"}, new String[]{"record-edit", "RECORD_EDIT"},
                new String[]{"record-delete", "RECORD_DELETE"}, new String[]{"file-upload", "FILE_MANAGE"},
                new String[]{"file-delete", "FILE_MANAGE"}, new String[]{"user-list", "USER_VIEW"},
                new String[]{"user-create", "USER_MANAGE"}, new String[]{"user-role", "USER_MANAGE"},
                new String[]{"user-active", "USER_MANAGE"}, new String[]{"role-list", "ROLE_VIEW"},
                new String[]{"role-create", "ROLE_MANAGE"}, new String[]{"role-update", "ROLE_MANAGE"},
                new String[]{"audit-list", "AUDIT_VIEW"}, new String[]{"user-history", "AUDIT_VIEW"})
                .flatMap(row -> Stream.of(Arguments.of(row[0], row[1], true),
                        Arguments.of(row[0], "", false), Arguments.of(row[0], "ADMIN_PANEL_ACCESS", false)));
    }

    @ParameterizedTest(name = "{0} authority={1} allowed={2}")
    @MethodSource("matrix")
    void capabilityIsNecessaryAndSufficient(String endpoint, String authority, boolean allowed) {
        Role role = new Role(); role.setName("Dynamic role"); role.setActive(true);
        User user = new User(); user.setId(ID); user.setRole(role);
        AuthenticatedUser principal = new AuthenticatedUser(user, authority.isEmpty() ? Set.of() : Set.of(authority));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Runnable call = () -> invoke(endpoint, principal);
        if (allowed) {
            assertThatCode(call::run).doesNotThrowAnyException();
            assertThat(Stream.of(userService, roleAdminService, recordService, fileService, auditService,
                            userAuditService)
                    .mapToInt(service -> mockingDetails(service).getInvocations().size()).sum()).isPositive();
        } else {
            assertThatThrownBy(call::run).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(userService, roleAdminService, recordService, fileService, auditService,
                    userAuditService);
        }
    }

    private void invoke(String endpoint, AuthenticatedUser principal) {
        switch (endpoint) {
            case "record-create" -> records.createRecord(null);
            case "record-edit" -> records.updateRecord(ID, null);
            case "record-delete" -> records.deleteRecord(ID);
            case "file-upload" -> files.uploadFiles(ID, null, principal);
            case "file-delete" -> files.deleteFile(ID, principal);
            case "user-list" -> admin.listUsers(new AdminUserSearchCriteria(), Pageable.unpaged());
            case "user-create" -> admin.createUser(new CreateUserRequest());
            case "user-role" -> {
                ChangeRoleRequest request = new ChangeRoleRequest(); request.setRoleId(1);
                admin.changeRole(ID, request);
            }
            case "user-active" -> {
                SetActiveRequest request = new SetActiveRequest(); request.setActive(true);
                admin.setActive(ID, request);
            }
            case "role-list" -> roleAdmin.listRoles(false);
            case "role-create" -> roleAdmin.createRole(new CreateRoleRequest());
            case "role-update" -> roleAdmin.updateRole(1, new UpdateRoleRequest());
            case "audit-list" -> admin.listAuditLogs("USER", Pageable.unpaged());
            case "user-history" -> userAudit.getGecmis(ID);
            default -> throw new IllegalArgumentException(endpoint);
        }
    }
}
