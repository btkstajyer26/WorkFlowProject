package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.common.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Token hic gelmedi, gecersiz veya suresi dolmus oldugunda (401 Unauthorized)
 * calisir. GlobalExceptionHandler'in dondurdugu ApiError formatiyla ayni
 * govdeyi uretir ki frontend tek bir hata semasi bekleyebilsin.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        ApiError apiError = new ApiError(
                "UNAUTHORIZED",
                "Kimlik dogrulama basarisiz: gecerli bir token gerekli.",
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }
}