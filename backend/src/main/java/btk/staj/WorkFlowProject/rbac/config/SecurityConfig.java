package btk.staj.WorkFlowProject.rbac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // GECICI: Gelistirme asamasinda Swagger uzerinden endpoint denemek icin
    // kimlik dogrulama tamamen kapali. Bu haliyle PRODUCTION'A CIKMAMALI.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // JWT kullanilacagi icin sunucuda oturum tutulmaz.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO: Nisan/Sümeyye'nin auth/login, auth/refresh gibi public endpoint'leri netleşince
                // buraya .requestMatchers("/api/auth/**").permitAll() eklenecek
                // TODO: JwtAuthenticationFilter zincire eklenene kadar geçici olarak açık
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Hicbir ucta giris ekrani cikmasin diye kapatildi.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        // TODO: Nisan/Sümeyye'nin JwtAuthenticationFilter'ı tamamlanınca şu satır eklenecek:
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // UserService ve AuthService sifre hash'lemek icin bu bean'e ihtiyac duyuyor.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
