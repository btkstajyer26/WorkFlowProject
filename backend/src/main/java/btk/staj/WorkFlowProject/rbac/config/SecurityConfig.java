package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.audit.RequestAuditFilter;
import btk.staj.WorkFlowProject.auth.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Kimlik dogrulamasi istemeyen uclar.
     *
     * <p>Swagger arayuzu ve OpenAPI semasi acik tutulur ki dokumantasyon token
     * olmadan okunabilsin; arayuzdeki <em>Authorize</em> butonuyla token girilerek
     * korumali uclar denenir. Uclarin kendisi acik degildir.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            // "Sifremi unuttum" akisi tanimi geregi oturumsuz calisir: kullanici
            // sifresini bilmedigi icin token uretemez.
            "/api/auth/forgot-password",
            "/api/auth/verify-reset-code",
            "/api/auth/reset-password",
            // E-posta bildirimindeki tek tiklik aksiyon baglantisi. Oturum
            // yoktur cunku kullanici postadan gelir; kimlik istekte tasinan tek
            // kullanimlik anahtardan cozulur (bkz. MailActionTokenService).
            // Joker yol (/api/public/**) BILEREK kullanilmadi: boyle bir kalip,
            // ileride o onekle eklenen her ucu sessizce herkese acardi.
            "/api/public/mail-actions/preview",
            "/api/public/mail-actions/consume",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            // Servlet konteyneri hatayi /error'a yonlendirir; burasi kapali
            // kalirsa istemci gercek hata yerine 401 gorur.
            "/error",
            // Reverse proxy ve container healthcheck'i bu ucu token'siz
            // yoklar; kapali kalirsa 401 gorup servisi "sagliksiz" sayarlar.
            // show-details=never oldugu icin yalnizca UP/DOWN disari cikar.
            "/actuator/health"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestAuditFilter requestAuditFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RequestAuditFilter requestAuditFilter,
                          AuthenticationEntryPoint authenticationEntryPoint,
                          AccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestAuditFilter = requestAuditFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestAuditFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Servlet konteynerinin filtreyi ikinci kez (JWT'den once) calistirmasini
     * engeller; yalnizca SecurityFilterChain icindeki sira gecerlidir.
     */
    @Bean
    public FilterRegistrationBean<RequestAuditFilter> disableDuplicateRequestAuditFilter(
            RequestAuditFilter requestAuditFilter) {
        FilterRegistrationBean<RequestAuditFilter> registration = new FilterRegistrationBean<>(requestAuditFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}