package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.common.exception.ApiErrorWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Filtre zincirinde olusan guvenlik hatalari controller'a hic ulasmaz, bu yuzden
 * {@code GlobalExceptionHandler} tarafindan yakalanamaz. Buradaki iki bean,
 * ortak {@link ApiErrorWriter} uzerinden ayni govdeyi uretir.
 */
@Configuration
public class SecurityErrorHandlers {

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint(ApiErrorWriter apiErrorWriter) {
        return (request, response, authException) ->
                apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED", "Kimlik doğrulaması gerekli");
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler(ApiErrorWriter apiErrorWriter) {
        return (request, response, accessDeniedException) ->
                apiErrorWriter.write(response, HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "Bu işlem için yetkiniz yok");
    }
}