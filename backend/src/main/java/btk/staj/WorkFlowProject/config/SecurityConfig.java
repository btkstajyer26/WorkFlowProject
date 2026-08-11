package btk.staj.WorkFlowProject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .permitAll()
            )
            // 🍪 GERÇEK VERİTABANI BAĞLANDIĞINDA ÇALIŞACAK ÇEREZ MEKANİZMASI
            .rememberMe(remember -> remember
                .key("workflowProjectSecretKey")
                .tokenValiditySeconds(86400 * 30) // 30 Günlük Oturum Çerezi
                .rememberMeParameter("remember-me")
            );

        return http.build();
    }
}