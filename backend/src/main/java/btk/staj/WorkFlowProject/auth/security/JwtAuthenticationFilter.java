package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.common.exception.ApiErrorWriter;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ApiErrorWriter apiErrorWriter;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService userDetailsService,
                                   ApiErrorWriter apiErrorWriter) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Parola degisimi bekleyen kullanici yalnizca uc uca erisebilir;
                // diger her istek 403 ile kesilir (kural arayuzde degil burada zorlanir).
                if (userDetails instanceof AuthenticatedUser authenticatedUser
                        && authenticatedUser.getUser().isMustChangePassword()
                        && !isAllowedWhilePasswordChangeRequired(request)) {

                    apiErrorWriter.write(response, HttpStatus.FORBIDDEN,
                            "PASSWORD_CHANGE_REQUIRED",
                            "Devam etmeden önce parolanızı değiştirmelisiniz");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parola degisimi bekleyen kullanicinin cagirabilecegi uclar: parolayi
     * degistirmek, cikis yapmak ve kendi kimligini okumak.
     */
    private boolean isAllowedWhilePasswordChangeRequired(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        return ("POST".equals(method) && "/api/auth/change-password".equals(path))
                || ("POST".equals(method) && "/api/auth/logout".equals(path))
                || ("GET".equals(method) && "/api/users/me".equals(path));
    }
}