package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.common.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Filtre zincirinde olusan guvenlik hatalari controller'a hic ulasmaz, bu yuzden
 * {@code GlobalExceptionHandler} tarafindan yakalanamaz. Buradaki iki bean, o
 * durumlarda da istemcinin ayni {@link ApiError} govdesini almasini saglar.
 */
@Configuration
public class SecurityErrorHandlers {

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) ->
                write(objectMapper, response, HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED", "Kimlik doğrulaması gerekli");
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) ->
                write(objectMapper, response, HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "Bu işlem için yetkiniz yok");
    }

    private void write(ObjectMapper objectMapper,
                       HttpServletResponse response,
                       HttpStatus status,
                       String code,
                       String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = new ApiError(code, message, status.value(), LocalDateTime.now());
        objectMapper.writeValue(response.getWriter(), error);
    }
}
