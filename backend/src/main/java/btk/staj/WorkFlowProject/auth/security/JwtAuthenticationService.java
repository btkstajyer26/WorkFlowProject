package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.auth.service.CustomUserDetailsService;
import btk.staj.WorkFlowProject.rbac.config.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Creates the current Spring Security identity from a signed access token. */
@Service
public class JwtAuthenticationService {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationService(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    public Optional<Authentication> authenticate(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            return Optional.empty();
        }

        String email = jwtUtil.extractEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!userDetails.isEnabled()) {
            return Optional.empty();
        }

        return Optional.of(new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()));
    }
}
