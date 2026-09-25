package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.attachment.controller.FileController;
import btk.staj.WorkFlowProject.attachment.service.FileService;
import btk.staj.WorkFlowProject.audit.controller.UserAuditLogController;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.department.controller.DepartmentAdminController;
import btk.staj.WorkFlowProject.department.controller.DepartmentRoutingRuleController;
import btk.staj.WorkFlowProject.department.dto.AddDepartmentMemberRequest;
import btk.staj.WorkFlowProject.department.dto.CreateDepartmentRequest;
import btk.staj.WorkFlowProject.department.dto.CreateDepartmentRoutingRuleRequest;
import btk.staj.WorkFlowProject.department.dto.UpdateDepartmentRequest;
import btk.staj.WorkFlowProject.department.dto.UpdateDepartmentRoutingRuleRequest;
import btk.staj.WorkFlowProject.department.service.DepartmentAdminService;
import btk.staj.WorkFlowProject.department.service.DepartmentRoutingRuleAdminService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.controller.PermissionAdminController;
import btk.staj.WorkFlowProject.rbac.controller.RoleAdminController;
import btk.staj.WorkFlowProject.rbac.dto.CreateRoleRequest;
import btk.staj.WorkFlowProject.rbac.dto.RolePermissionsResponse;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRolePermissionsRequest;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRoleRequest;
import btk.staj.WorkFlowProject.rbac.service.PermissionAdminService;
import btk.staj.WorkFlowProject.rbac.service.RoleAdminService;
import btk.staj.WorkFlowProject.record.controller.RecordController;
import btk.staj.WorkFlowProject.record.service.RecordService;
import btk.staj.WorkFlowProject.search.service.RecordSearchService;
import btk.staj.WorkFlowProject.user.controller.AdminController;
import btk.staj.WorkFlowProject.user.dto.*;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.service.UserService;
import btk.staj.WorkFlowProject.workflow.controller.WorkflowActorBindingController;
import btk.staj.WorkFlowProject.workflow.dto.BindActorRequest;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActorBindingService;
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
    @Import({AdminController.class, RoleAdminController.class, PermissionAdminController.class,
            WorkflowActorBindingController.class, DepartmentAdminController.class,
            DepartmentRoutingRuleController.class, RecordController.class,
            FileController.class, UserAuditLogController.class})
    static class Config {
        @Bean UserService users() { return mock(UserService.class); }
        @Bean RoleAdminService roleAdmin() { return mock(RoleAdminService.class); }
        @Bean PermissionAdminService permissionAdmin() { return mock(PermissionAdminService.class); }
        @Bean WorkflowActorBindingService actorBindings() { return mock(WorkflowActorBindingService.class); }
        @Bean DepartmentAdminService departmentAdmin() { return mock(DepartmentAdminService.class); }
        @Bean DepartmentRoutingRuleAdminService departmentRoutingRuleAdmin() { return mock(DepartmentRoutingRuleAdminService.class); }
        @Bean RecordService records() { return mock(RecordService.class); }
        @Bean RecordSearchService search() { return mock(RecordSearchService.class); }
        @Bean FileService files() { return mock(FileService.class); }
        @Bean AuditLogService audit() { return mock(AuditLogService.class); }
        @Bean UserAuditLogService userAudit() { return mock(UserAuditLogService.class); }
    }

    @Autowired AdminController admin;
    @Autowired RoleAdminController roleAdmin;
    @Autowired RoleAdminService roleAdminService;
    @Autowired PermissionAdminController permissionAdmin;
    @Autowired PermissionAdminService permissionAdminService;
    @Autowired WorkflowActorBindingController actorBindingController;
    @Autowired WorkflowActorBindingService actorBindingService;
    @Autowired DepartmentAdminController departmentAdmin;
    @Autowired DepartmentAdminService departmentAdminService;
    @Autowired DepartmentRoutingRuleController routingRuleAdmin;
    @Autowired DepartmentRoutingRuleAdminService routingRuleAdminService;
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
        reset(userService, roleAdminService, permissionAdminService, actorBindingService, departmentAdminService,
                routingRuleAdminService, recordService, fileService, auditService, userAuditService);
        when(userService.createUser(any(), any(), any(), any())).thenReturn(new User());
        when(userService.changeRole(any(), any(Integer.class), any())).thenReturn(new User());
        when(userService.setActive(any(), anyBoolean())).thenReturn(new User());
        when(permissionAdminService.getRolePermissions(any()))
                .thenReturn(new RolePermissionsResponse(1, "role", List.of()));
        when(permissionAdminService.updateRolePermissions(any(), any()))
                .thenReturn(new RolePermissionsResponse(1, "role", List.of()));
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
                new String[]{"permission-list", "ROLE_VIEW"}, new String[]{"role-permissions-get", "ROLE_VIEW"},
                new String[]{"role-permissions-update", "ROLE_MANAGE"},
                new String[]{"actor-binding-list", "WORKFLOW_VIEW"}, new String[]{"actor-binding-bind", "WORKFLOW_MANAGE"},
                new String[]{"actor-binding-unbind", "WORKFLOW_MANAGE"},
                new String[]{"department-list", "DEPARTMENT_VIEW"}, new String[]{"department-create", "DEPARTMENT_MANAGE"},
                new String[]{"department-update", "DEPARTMENT_MANAGE"}, new String[]{"department-members-list", "DEPARTMENT_VIEW"},
                new String[]{"department-member-add", "DEPARTMENT_MANAGE"}, new String[]{"department-member-remove", "DEPARTMENT_MANAGE"},
                new String[]{"routing-rule-list", "DEPARTMENT_VIEW"}, new String[]{"routing-rule-create", "DEPARTMENT_MANAGE"},
                new String[]{"routing-rule-update", "DEPARTMENT_MANAGE"},
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
            assertThat(Stream.of(userService, roleAdminService, permissionAdminService, actorBindingService,
                            departmentAdminService, routingRuleAdminService, recordService, fileService, auditService,
                            userAuditService)
                    .mapToInt(service -> mockingDetails(service).getInvocations().size()).sum()).isPositive();
        } else {
            assertThatThrownBy(call::run).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(userService, roleAdminService, permissionAdminService, actorBindingService,
                    departmentAdminService, routingRuleAdminService, recordService, fileService, auditService,
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
            case "permission-list" -> permissionAdmin.listPermissions();
            case "role-permissions-get" -> permissionAdmin.getRolePermissions(1);
            case "role-permissions-update" -> {
                UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
                request.setPermissionCodes(Set.of());
                permissionAdmin.updateRolePermissions(1, request);
            }
            case "actor-binding-list" -> actorBindingController.list();
            case "actor-binding-bind" -> {
                BindActorRequest request = new BindActorRequest();
                request.setTemplateTransitionId(1);
                request.setActorRoleId(2);
                actorBindingController.bind(request);
            }
            case "actor-binding-unbind" -> actorBindingController.unbind(1);
            case "department-list" -> departmentAdmin.listDepartments(false);
            case "department-create" -> {
                CreateDepartmentRequest request = new CreateDepartmentRequest();
                request.setName("Hukuk");
                departmentAdmin.createDepartment(request);
            }
            case "department-update" -> departmentAdmin.updateDepartment(1, new UpdateDepartmentRequest());
            case "department-members-list" -> departmentAdmin.listMembers(1);
            case "department-member-add" -> {
                AddDepartmentMemberRequest request = new AddDepartmentMemberRequest();
                request.setUserId(ID);
                departmentAdmin.addMember(1, request);
            }
            case "department-member-remove" -> departmentAdmin.removeMember(1, ID);
            case "routing-rule-list" -> routingRuleAdmin.listRules(1);
            case "routing-rule-create" -> {
                CreateDepartmentRoutingRuleRequest request = new CreateDepartmentRoutingRuleRequest();
                request.setFromStatusId(1);
                request.setActionId(1);
                request.setTargetRoleId(1);
                routingRuleAdmin.createRule(1, request);
            }
            case "routing-rule-update" -> routingRuleAdmin.updateRule(1, 1, new UpdateDepartmentRoutingRuleRequest());
            case "audit-list" -> admin.listAuditLogs("USER", Pageable.unpaged());
            case "user-history" -> userAudit.getGecmis(ID);
            default -> throw new IllegalArgumentException(endpoint);
        }
    }
}
