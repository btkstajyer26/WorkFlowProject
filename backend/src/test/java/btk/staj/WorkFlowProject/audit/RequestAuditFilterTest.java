package btk.staj.WorkFlowProject.audit;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;

import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("HTTP istek denetim filtresi")
class RequestAuditFilterTest {

    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UserAuditLogService userAuditLogService;

    private final RequestAuditContext context = new RequestAuditContext();

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
        context.clear();
    }

    private RequestAuditFilter filter() {
        return new RequestAuditFilter(context, auditLogService, userAuditLogService);
    }

    @Test
    @DisplayName("admin girisi audit_logs tablosuna yazilir")
    void adminLoginGoesToAuditLogs() throws Exception {
        User admin = user("ADMIN");
        context.mark("LOGIN", admin);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200);

        filter().doFilter(request, response, chain);

        ArgumentCaptor<RequestAccessEvent> captor = ArgumentCaptor.forClass(RequestAccessEvent.class);
        verify(auditLogService).recordAccess(captor.capture());
        verifyNoInteractions(userAuditLogService);

        RequestAccessEvent event = captor.getValue();
        assertThat(event.action()).isEqualTo("LOGIN");
        assertThat(event.systemKey()).isEqualTo("ADMIN");
        assertThat(event.httpStatus()).isEqualTo(200);
        assertThat(event.errorCode()).isEqualTo("OK");
    }

    @Test
    @DisplayName("calisan girisi user_audit_logs tablosuna yazilir")
    void employeeLoginGoesToUserAuditLogs() throws Exception {
        User calisan = user("CALISAN");
        context.mark("LOGIN", calisan);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200);

        filter().doFilter(request, response, chain);

        ArgumentCaptor<RequestAccessEvent> captor = ArgumentCaptor.forClass(RequestAccessEvent.class);
        verify(userAuditLogService).recordAccess(captor.capture());
        verifyNoInteractions(auditLogService);
        assertThat(captor.getValue().action()).isEqualTo("LOGIN");
        assertThat(captor.getValue().systemKey()).isEqualTo("CALISAN");
    }

    @Test
    @DisplayName("hata kodu JSON govdeden okunur")
    void errorCodeIsReadFromApiErrorBody() throws Exception {
        User calisan = user("CALISAN");
        AuthenticatedUser authenticatedUser = AuthorizationFixtures.authenticated(calisan);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/records");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) res;
            http.setStatus(400);
            http.setContentType("application/json");
            http.getWriter().write("{\"code\":\"VALIDATION_ERROR\",\"message\":\"Girilen veriler geçersiz\"}");
        };

        filter().doFilter(request, response, chain);

        ArgumentCaptor<RequestAccessEvent> captor = ArgumentCaptor.forClass(RequestAccessEvent.class);
        verify(userAuditLogService).recordAccess(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("HTTP_REQUEST");
        assertThat(captor.getValue().errorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(captor.getValue().httpStatus()).isEqualTo(400);
        assertThat(captor.getValue().comment()).contains("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("swagger istekleri loglanmaz")
    void swaggerIsSkipped() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, (req, res) -> {});

        verifyNoInteractions(auditLogService, userAuditLogService);
    }

    private User user(String roleName) {
        Role role = new Role();
        role.setId("ADMIN".equals(roleName) ? 4 : 1);
        role.setName(roleName);
        role.setActive(true);
        role.setSystemKey(roleName);
        role.setWorkflowActor(AuthorizationFixtures.workflowActor(roleName));
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    @Test
    void renamedAdminStillUsesAdminAuditTable() throws Exception {
        User admin = user("ADMIN");
        admin.getRole().setName("Sistem Yöneticisi");
        context.mark("LOGIN", admin);
        filter().doFilter(new MockHttpServletRequest("POST", "/api/auth/login"),
                new MockHttpServletResponse(), (req, res) -> {});
        ArgumentCaptor<RequestAccessEvent> captor = ArgumentCaptor.forClass(RequestAccessEvent.class);
        verify(auditLogService).recordAccess(captor.capture());
        verifyNoInteractions(userAuditLogService);
        assertThat(captor.getValue().systemKey()).isEqualTo("ADMIN");
    }
}
