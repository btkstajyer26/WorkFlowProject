package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.dto.DeviceTokenRequest;
import btk.staj.WorkFlowProject.notification.entity.DeviceToken;
import btk.staj.WorkFlowProject.notification.repository.DeviceTokenRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    @Test
    @DisplayName("Upsert: Aynı token farklı bir kullanıcıyla geldiğinde user_id güncellenmeli")
    void whenSameTokenRegisteredByDifferentUser_thenUserIdMustChange() {
        UUID oldUserId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        String tokenStr = "fcm-device-token-123";

        User oldUser = new User();
        oldUser.setId(oldUserId);

        User newUser = new User();
        newUser.setId(newUserId);

        DeviceToken existingToken = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(oldUser)
                .token(tokenStr)
                .platform("ANDROID")
                .active(false)
                .build();

        DeviceTokenRequest request = new DeviceTokenRequest();
        request.setToken(tokenStr);
        request.setPlatform("IOS");
        request.setDeviceName("Melih iPhone");

        when(userRepository.findById(newUserId)).thenReturn(Optional.of(newUser));
        when(deviceTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(existingToken));

        deviceTokenService.registerOrUpdateToken(newUserId, request);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());

        DeviceToken saved = captor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(newUserId);
        assertThat(saved.getPlatform()).isEqualTo("IOS");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}