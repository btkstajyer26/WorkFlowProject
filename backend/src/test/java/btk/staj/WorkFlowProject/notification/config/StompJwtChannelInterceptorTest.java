package btk.staj.WorkFlowProject.notification.config;

import btk.staj.WorkFlowProject.auth.security.JwtAuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompJwtChannelInterceptorTest {

    @Mock private JwtAuthenticationService authenticationService;
    @Mock private MessageChannel channel;

    @Test
    void connectUsesOnlyNativeAuthorizationHeaderAndSetsPrincipal() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "principal", null, java.util.List.of());
        when(authenticationService.authenticate("valid-token")).thenReturn(Optional.of(authentication));
        Message<byte[]> message = connectMessage("Bearer valid-token");

        Message<?> result = interceptor().preSend(message, channel);

        assertSame(message, result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertSame(authentication, resultAccessor.getUser());
    }

    @Test
    void connectWithoutAuthorizationHeaderIsRejected() {
        assertThrows(BadCredentialsException.class,
                () -> interceptor().preSend(connectMessage(null), channel));
        verify(authenticationService, never()).authenticate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void connectWithInvalidTokenIsRejected() {
        when(authenticationService.authenticate("invalid")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> interceptor().preSend(connectMessage("Bearer invalid"), channel));
    }

    @Test
    void queryStringTokenCannotReplaceConnectHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("token", "query-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(BadCredentialsException.class, () -> interceptor().preSend(message, channel));
        verify(authenticationService, never()).authenticate("query-token");
    }

    @Test
    void nonConnectFramesAreNotAuthenticatedAgain() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertEquals(message, interceptor().preSend(message, channel));
        verify(authenticationService, never()).authenticate(org.mockito.ArgumentMatchers.anyString());
    }

    private StompJwtChannelInterceptor interceptor() {
        return new StompJwtChannelInterceptor(authenticationService);
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
