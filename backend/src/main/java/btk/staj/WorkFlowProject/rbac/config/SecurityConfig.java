package btk.staj.WorkFlowProject.rbac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // TODO: Nisan/Sümeyye'nin auth/login, auth/refresh gibi public endpoint'leri netleşince
                        // buraya .requestMatchers("/api/auth/**").permitAll() eklenecek
                        // TODO: JwtAuthenticationFilter zincire eklenene kadar geçici olarak açık
                        .anyRequest().permitAll()
                );

        // TODO: Nisan/Sümeyye'nin JwtAuthenticationFilter'ı tamamlanınca şu satır eklenecek:
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}