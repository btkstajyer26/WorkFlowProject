package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.notification.dto.NotificationResponse;
import btk.staj.WorkFlowProject.notification.entity.Notification;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Bildirim listeleme ve okundu isaretleme")
class NotificationServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final UUID NOTIFICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000053");

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(notificationRepository);

    @Test
    @DisplayName("gecmis okunmus ve okunmamis bildirimleri birlikte, sayfali doner")
    void theHistoryIsReturnedPaged() {
        Notification read = notification();
        read.markAsRead();
        Notification unread = notification();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(read, unread), PageRequest.of(1, 2), 6));

        PagedResponse<NotificationResponse> response = service.getAll(USER_ID, PageRequest.of(1, 2));

        assertThat(response.getContent()).extracting(NotificationResponse::read)
                .containsExactly(true, false);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(6);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("istemcinin gonderdigi siralama yok sayilir, sira her zaman en yeniden eskiye")
    void theRequestedSortIsIgnored() {
        givenEmptyPage();

        service.getAll(USER_ID, PageRequest.of(0, 20, Sort.by("message")));

        assertThat(capturedPageable().getSort().isSorted()).isFalse();
    }

    @Test
    @DisplayName("sayfa boyutu ustten sinirlanir")
    void thePageSizeIsCapped() {
        givenEmptyPage();

        service.getAll(USER_ID, PageRequest.of(0, 5_000));

        assertThat(capturedPageable().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("baskasinin bildirimi okundu isaretlenemez")
    void anotherUsersNotificationCannotBeMarked() {
        Notification foreign = new Notification(
                OTHER_USER_ID, RECORD_ID, "mesaj", NotificationType.RECORD_APPROVED);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(foreign));

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> service.markAsRead(NOTIFICATION_ID, USER_ID));

        assertThat(foreign.isRead()).isFalse();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("olmayan bildirim 404 uretir")
    void aMissingNotificationIsReported() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.markAsRead(NOTIFICATION_ID, USER_ID));
    }

    @Test
    @DisplayName("kendi bildirimi okundu isaretlenir")
    void theOwnNotificationIsMarked() {
        Notification own = notification();
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(own));

        service.markAsRead(NOTIFICATION_ID, USER_ID);

        assertThat(own.isRead()).isTrue();
        verify(notificationRepository).save(own);
    }

    private void givenEmptyPage() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(USER_ID), captor.capture());
        return captor.getValue();
    }

    private static Notification notification() {
        return new Notification(USER_ID, RECORD_ID, "mesaj", NotificationType.RECORD_SUBMITTED);
    }
}
