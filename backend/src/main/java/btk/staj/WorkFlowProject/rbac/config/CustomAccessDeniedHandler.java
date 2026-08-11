package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.common.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Token gecerli ama kullanicinin yetkisi (rol/RBAC) bu ucu cagirmaya
 * yetmedigi durumda (403 Forbidden) calisir. ForbiddenException ile ayni
 * "FORBIDDEN" kodunu kullanir, boylece GlobalExceptionHandler'in ic servis
 * katmaninda firlattigi ForbiddenException ile buradaki filtre seviyesindeki
 * red ayni gorunumde doner.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        ApiError apiError = new ApiError(
                "FORBIDDEN",
                "Bu islem icin yetkiniz yok.",
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }
}