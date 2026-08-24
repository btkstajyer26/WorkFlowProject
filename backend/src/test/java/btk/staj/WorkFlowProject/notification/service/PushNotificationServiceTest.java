package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.repository.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private PushNotificationService pushNotificationService;

    @Test
    @DisplayName("FCM UNREGISTERED döndüğünde token pasifleştirilmeli")
    void whenFcmReturnsUnregistered_thenDeactivateToken() {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        ReflectionTestUtils.invokeMethod(pushNotificationService, "handleFcmError", exception, "fcm-invalid-token");

        verify(deviceTokenRepository, times(1)).deactivateByToken("fcm-invalid-token");
    }

    @Test
    @DisplayName("FCM INVALID_ARGUMENT döndüğünde token pasifleştirilmeli")
    void whenFcmReturnsInvalidArgument_thenDeactivateToken() {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);

        ReflectionTestUtils.invokeMethod(pushNotificationService, "handleFcmError", exception, "fcm-bad-token");

        verify(deviceTokenRepository, times(1)).deactivateByToken("fcm-bad-token");
    }

    @Test
    @DisplayName("FCM UNAVAILABLE döndüğünde geçici hata sayılmalı ve token pasifleştirilmemeli")
    void whenFcmReturnsUnavailable_thenDoNotDeactivateToken() {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);

        ReflectionTestUtils.invokeMethod(pushNotificationService, "handleFcmError", exception, "fcm-temp-token");

        verify(deviceTokenRepository, never()).deactivateByToken(anyString());
    }
}