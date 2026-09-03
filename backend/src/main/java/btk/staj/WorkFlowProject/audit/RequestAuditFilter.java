package btk.staj.WorkFlowProject.audit;

import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * {@code /api/**} altındaki her isteği ilgili log tablosuna yazar.
 * ADMIN → {@code audit_logs}, diğerleri → {@code user_audit_logs}.
 * Login/logout eylemi {@link RequestAuditContext} üzerinden AuthService'den gelir.
 */
@Component
public class RequestAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditFilter.class);
    private static final int PATH_LIMIT = 512;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RequestAuditContext requestAuditContext;
    private final AuditLogService auditLogService;
    private final UserAuditLogService userAuditLogService;

    public RequestAuditFilter(RequestAuditContext requestAuditContext,
                              AuditLogService auditLogService,
                              UserAuditLogService userAuditLogService) {
        this.requestAuditContext = requestAuditContext;
        this.auditLogService = auditLogService;
        this.userAuditLogService = userAuditLogService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null
                || !path.startsWith("/api/")
                || path.startsWith("/v3/api-docs")
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        boolean cacheBody = !request.getRequestURI().contains("/files");
        HttpServletResponse downstream = cacheBody
                ? new ContentCachingResponseWrapper(response)
                : response;

        try {
            filterChain.doFilter(request, downstream);
        } finally {
            try {
                persist(request, downstream);
            } catch (Exception ex) {
                log.warn("İstek denetim izi yazılamadı: {} {}", request.getMethod(), request.getRequestURI(), ex);
            } finally {
                requestAuditContext.clear();
                if (downstream instanceof ContentCachingResponseWrapper wrapped) {
                    wrapped.copyBodyToResponse();
                }
            }
        }
    }

    private void persist(HttpServletRequest request, HttpServletResponse response) {
        RequestAuditContext.Snapshot snapshot = requestAuditContext.peek();
        AuthenticatedUser authenticated = currentUser();

        String action = snapshot != null ? snapshot.action() : "HTTP_REQUEST";
        UUID userId = snapshot != null && snapshot.userId() != null
                ? snapshot.userId()
                : (authenticated != null ? authenticated.getId() : null);
        Integer roleId = snapshot != null && snapshot.roleId() != null
                ? snapshot.roleId()
                : (authenticated != null ? authenticated.getRoleId() : null);
        String systemKey = snapshot != null && snapshot.systemKey() != null
                ? snapshot.systemKey()
                : (authenticated != null ? authenticated.getSystemKey() : null);

        int status = response.getStatus();
        String errorCode = status >= 400 ? extractErrorCode(response) : "OK";
        String path = truncate(fullPath(request), PATH_LIMIT);
        String comment = request.getMethod() + " " + path + " → " + status
                + (errorCode != null && !"OK".equals(errorCode) ? " (" + errorCode + ")" : "");

        RequestAccessEvent event = new RequestAccessEvent(
                action,
                userId,
                roleId,
                systemKey,
                request.getMethod(),
                path,
                status,
                errorCode,
                comment);

        if (event.adminActor()) {
            auditLogService.recordAccess(event);
        } else {
            userAuditLogService.recordAccess(event);
        }
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthenticatedUser user ? user : null;
    }

    private String fullPath(HttpServletRequest request) {
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        String query = request.getQueryString();
        return query == null || query.isBlank() ? uri : uri + "?" + query;
    }

    private String extractErrorCode(HttpServletResponse response) {
        if (!(response instanceof ContentCachingResponseWrapper wrapped)) {
            return String.valueOf(response.getStatus());
        }
        byte[] body = wrapped.getContentAsByteArray();
        if (body.length == 0) {
            return String.valueOf(wrapped.getStatus());
        }
        try {
            Charset charset = wrapped.getCharacterEncoding() != null
                    ? Charset.forName(wrapped.getCharacterEncoding())
                    : StandardCharsets.UTF_8;
            JsonNode node = OBJECT_MAPPER.readTree(new String(body, charset));
            if (node != null && node.hasNonNull("code")) {
                return node.get("code").asText();
            }
        } catch (Exception ignored) {
            // Gövde JSON ApiError değilse HTTP durumunu kod olarak kullan.
        }
        return String.valueOf(response.getStatus());
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
