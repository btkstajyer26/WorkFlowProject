package btk.staj.WorkFlowProject.notification.listener;

import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.service.MailActionTokenService;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.notification.service.PushNotificationService;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@DisplayName("Durum degisikligi bildirimi")
class WorkflowStatusChangedListenerTest {

    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000041");
    private static final UUID ASSIGNEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000043");

    private final NotificationService notificationService = mock(NotificationService.class);
    private final MailService mailService = mock(MailService.class);
    private final MailActionTokenService mailActionTokenService = mock(MailActionTokenService.class);
    private final PushNotificationService pushNotificationService = mock(PushNotificationService.class);
    private final RecordRepository recordRepository = mock(RecordRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final WorkflowStatusChangedListener listener = new WorkflowStatusChangedListener(
            notificationService, mailService, pushNotificationService, mailActionTokenService,
            recordRepository, userRepository);

    @Test
    @DisplayName("bildirimi sirasi gelen kisiye yazar")
    void notifiesTheUserWhoseTurnItIs() {
        listener.createInAppNotification(event(
                WorkflowAction.BASKANA_ILET, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNEE_ID, null));

        verify(notificationService).create(
                eq(ASSIGNEE_ID), eq(RECORD_ID), any(), eq(NotificationType.RECORD_FORWARDED));
    }

    @Test
    @DisplayName("onay sonrasi kayit kimseye atanmadigi icin evragi olusturana yazar")
    void fallsBackToTheCreatorWhenNobodyIsAssigned() {
        givenRecord();

        listener.createInAppNotification(event(
                WorkflowAction.ONAYLA, RecordStatus.ONAYLANDI, null, null));

        verify(notificationService).create(
                eq(CREATOR_ID), eq(RECORD_ID), any(), eq(NotificationType.RECORD_APPROVED));
    }

    @Test
    @DisplayName("aciklama varsa bildirim metnine eklenir")
    void includesTheCommentInTheMessage() {
        listener.createInAppNotification(event(
                WorkflowAction.CALISANA_GERI_GONDER, RecordStatus.DUZENLEME_BEKLIYOR,
                CREATOR_ID, "Bütçe kalemini ekleyiniz."));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(
                eq(CREATOR_ID), eq(RECORD_ID), message.capture(), eq(NotificationType.RECORD_RETURNED));
        assertThat(message.getValue()).contains("Bütçe kalemini ekleyiniz.");
    }

    @Test
    @DisplayName("cok uzun aciklama message kolonuna sigacak sekilde kisaltilir")
    void truncatesAnOverlongComment() {
        listener.createInAppNotification(event(
                WorkflowAction.CALISANA_GERI_GONDER, RecordStatus.DUZENLEME_BEKLIYOR,
                CREATOR_ID, "x".repeat(2000)));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(any(), any(), message.capture(), any());
        assertThat(message.getValue()).hasSizeLessThanOrEqualTo(500);
    }

    @ParameterizedTest
    @CsvSource({
            "GONDER,RECORD_SUBMITTED",
            "TEKRAR_GONDER,RECORD_SUBMITTED",
            "BASKANA_ILET,RECORD_FORWARDED",
            "ONAYLA,RECORD_APPROVED",
            "REDDET,RECORD_REJECTED",
            "CALISANA_GERI_GONDER,RECORD_RETURNED",
            "BASKAN_YARDIMCISINA_GERI_GONDER,RECORD_RETURNED"
    })
    @DisplayName("her aksiyon dogru bildirim turune eslenir")
    void mapsEveryActionToItsNotificationType(WorkflowAction action, NotificationType expected) {
        assertThat(NotificationType.of(action)).isEqualTo(expected);
    }

    @Test
    @DisplayName("durum degistiginde FCM push bildirimi tetiklenir")
    void sendsPushNotificationToAssignedUser() {
        givenRecord();

        listener.sendMail(event(
                WorkflowAction.BASKANA_ILET, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNEE_ID, "Uygun görüldü"));

        verify(pushNotificationService).sendPushNotification(
                eq(ASSIGNEE_ID),
                eq("Bütçe talebi"),
                any(),
                eq(RECORD_ID),
                eq(NotificationType.RECORD_FORWARDED));
    }

    @Test
    @DisplayName("e-posta alicinin gercek adresine gider")
    void sendsTheMailToTheResolvedRecipient() {
        givenRecord();
        User assignee = user(ASSIGNEE_ID, "Mehmet", "Demir", "mehmet@ornek.test");
        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));

        listener.sendMail(event(
                WorkflowAction.BASKANA_ILET, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNEE_ID, "Uygun görüldü"));

        verify(mailService).sendStatusChangeMail(
                eq("mehmet@ornek.test"), eq("Mehmet Demir"), eq(RECORD_ID),
                eq("Bütçe talebi"), eq("BASKAN_INCELEMESINDE"), eq("Uygun görüldü"), any());
    }

    @Test
    @DisplayName("atanan kisiye tek kullanimlik hizli islem anahtari uretilir")
    void issuesAQuickActionTokenForTheAssignee() {
        givenRecord();
        User assignee = user(ASSIGNEE_ID, "Mehmet", "Demir", "mehmet@ornek.test");
        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
        when(mailActionTokenService.issue(RECORD_ID, assignee, WorkflowAction.ONAYLA))
                .thenReturn("ham-anahtar");

        listener.sendMail(event(
                WorkflowAction.BASKANA_ILET, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNEE_ID, null));

        verify(mailActionTokenService).issue(RECORD_ID, assignee, WorkflowAction.ONAYLA);
        verify(mailService).sendStatusChangeMail(
                any(), any(), eq(RECORD_ID), any(), any(), any(), eq("ham-anahtar"));
    }

    @Test
    @DisplayName("terminal durumda hizli islem anahtari uretilmez")
    void doesNotIssueAQuickActionTokenForTerminalStatuses() {
        givenRecord();
        User assignee = user(ASSIGNEE_ID, "Mehmet", "Demir", "mehmet@ornek.test");
        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));

        listener.sendMail(event(
                WorkflowAction.ONAYLA, RecordStatus.ONAYLANDI, ASSIGNEE_ID, null));

        verifyNoInteractions(mailActionTokenService);
        verify(mailService).sendStatusChangeMail(
                any(), any(), eq(RECORD_ID), any(), any(), any(), isNull());
    }

    @Test
    @DisplayName("anahtar uretimi patlarsa e-posta yine de dugmesiz gider")
    void stillSendsTheMailWhenTokenIssuingFails() {
        givenRecord();
        User assignee = user(ASSIGNEE_ID, "Mehmet", "Demir", "mehmet@ornek.test");
        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
        when(mailActionTokenService.issue(any(), any(), any()))
                .thenThrow(new IllegalStateException("veritabani yok"));

        listener.sendMail(event(
                WorkflowAction.BASKANA_ILET, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNEE_ID, null));

        verify(mailService).sendStatusChangeMail(
                any(), any(), eq(RECORD_ID), any(), any(), any(), isNull());
    }

    @Test
    @DisplayName("alici kullanici bulunamazsa e-posta gonderilmez")
    void doesNotSendMailWhenTheRecipientIsUnknown() {
        givenRecord();
        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.empty());

        listener.sendMail(event(
                WorkflowAction.BASKANA_ILET, RecordStatus.BASKAN_INCELEMESINDE, ASSIGNEE_ID, null));

        verifyNoInteractions(mailService);
    }

    private void givenRecord() {
        Record record = new Record();
        record.setId(RECORD_ID);
        record.setTitle("Bütçe talebi");
        record.setCreatedBy(CREATOR_ID);
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
    }

    private static User user(UUID id, String firstName, String lastName, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        return user;
    }

    private static WorkflowStatusChangedEvent event(WorkflowAction action,
                                                    RecordStatus newStatus,
                                                    UUID assignedTo,
                                                    String comment) {
        return new WorkflowStatusChangedEvent(
                RECORD_ID,
                action,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                newStatus,
                ACTOR_ID,
                WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI),
                null,
                assignedTo,
                comment,
                Instant.parse("2026-08-11T09:15:00Z"));
    }
}