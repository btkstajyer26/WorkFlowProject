package btk.staj.WorkFlowProject.rbac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // GECICI: Gelistirme asamasinda Swagger uzerinden endpoint denemek icin
    // kimlik dogrulama tamamen kapatildi. DB tabanli UserDetailsService
    // yazildiginda buraya rol bazli kurallar eklenecek.
    // Bu haliyle PRODUCTION'A CIKMAMALI.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .build();
    }

    // UserService sifre hash'lemek icin bu bean'e ihtiyac duyuyor.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
