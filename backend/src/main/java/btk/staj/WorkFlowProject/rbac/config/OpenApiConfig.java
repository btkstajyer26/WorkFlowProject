package btk.staj.WorkFlowProject.rbac.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EBYS - İş Akışı ve Onay Yönetim Sistemi API")
                        .version("v1")
                        .description("""
                                Uçların çoğu kimlik doğrulaması ister.

                                Kullanım: `/api/auth/login` ucundan e-posta ve şifreyle giriş yapın,
                                dönen `accessToken` değerini sağ üstteki **Authorize** butonuna yapıştırın.
                                Bundan sonraki isteklere `Authorization: Bearer <token>` başlığı otomatik eklenir.
                                """))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Giriş sonrası dönen accessToken")))
                // Varsayilan olarak tum uclar korumali kabul edilir; /api/auth
                // uclari kendi tanimlarinda bu gereksinimi bosaltir.
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
