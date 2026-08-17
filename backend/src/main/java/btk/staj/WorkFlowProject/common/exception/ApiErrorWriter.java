package btk.staj.WorkFlowProject.common.exception;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Guvenlik filtre zincirinde ve JwtAuthenticationFilter'da olusan hatalarin
 * ayni ApiError govdesiyle istemciye donmesini saglayan ortak bilesen.
 */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response,
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