package btk.staj.WorkFlowProject.notification.config;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.auth.security.JwtAuthenticationService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAuthenticationService authenticationService;

    public StompJwtChannelInterceptor(JwtAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("STOMP CONNECT requires a Bearer token");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BadCredentialsException("STOMP CONNECT Bearer token is empty");
        }

        var authentication = authenticationService.authenticate(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid STOMP access token"));

        if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser
                && authenticatedUser.getUser().isMustChangePassword()) {
            throw new BadCredentialsException("Password change is required before STOMP CONNECT");
        }

        accessor.setUser(authentication);
        return message;
    }
}
