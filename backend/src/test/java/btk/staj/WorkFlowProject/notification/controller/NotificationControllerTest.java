package btk.staj.WorkFlowProject.notification.controller;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bu ucun tasidigi tek kural: bildirimler her zaman oturumdaki kullaniciya ait
 * olmali. Kullanici kimligi istekten degil {@link CurrentActorProvider}'dan
 * gelmelidir.
 */
@DisplayName("Bildirim ucu oturumdaki kullaniciyla calisir")
class NotificationControllerTest {

    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000070");

    private final NotificationService notificationService = mock(NotificationService.class);
    private final CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
    private final NotificationController controller =
            new NotificationController(notificationService, currentActorProvider);

    NotificationControllerTest() {
        when(currentActorProvider.currentActor())
                .thenReturn(new CurrentActor(CURRENT_USER_ID, RoleName.CALISAN));
    }

    @Test
    @DisplayName("gecmis listesi oturumdaki kullanicinin kimligiyle sorulur")
    void theHistoryIsScopedToTheCurrentUser() {
        Pageable pageable = PageRequest.of(2, 10);
        when(notificationService.getAll(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PagedResponse<>(List.of(), 2, 10, 0, 0));

        controller.getAll(pageable);

        verify(notificationService).getAll(CURRENT_USER_ID, pageable);
    }

    @Test
    @DisplayName("okunmamis listesi oturumdaki kullanicinin kimligiyle sorulur")
    void theUnreadListIsScopedToTheCurrentUser() {
        when(notificationService.getUnread(CURRENT_USER_ID)).thenReturn(List.of());

        assertThat(controller.getUnread()).isEmpty();
        verify(notificationService).getUnread(CURRENT_USER_ID);
    }

    @Test
    @DisplayName("okundu isaretleme oturumdaki kullanicinin kimligiyle yapilir")
    void markingAsReadIsScopedToTheCurrentUser() {
        UUID notificationId = UUID.randomUUID();

        controller.markAsRead(notificationId);

        verify(notificationService).markAsRead(notificationId, CURRENT_USER_ID);
    }
}
