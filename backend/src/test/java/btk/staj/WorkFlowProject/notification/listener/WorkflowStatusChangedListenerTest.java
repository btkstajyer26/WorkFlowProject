package btk.staj.WorkFlowProject.notification.listener;

import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.notification.service.PushNotificationService;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowStatusChangedListenerTest {

    private NotificationService notificationService;
    private MailService mailService;
    private PushNotificationService pushNotificationService;
    private RecordRepository recordRepository;
    private UserRepository userRepository;
    private WorkflowStatusChangedListener listener;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        mailService = mock(MailService.class);
        pushNotificationService = mock(PushNotificationService.class);
        recordRepository = mock(RecordRepository.class);
        userRepository = mock(UserRepository.class);

        listener = new WorkflowStatusChangedListener(
                notificationService,
                mailService,
                pushNotificationService,
                recordRepository,
                userRepository
        );
    }

    @Test
    void sendMail_sendsMailSuccessfully() {
        UUID recordId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Record record = new Record();
        record.setId(recordId);
        record.setTitle("Test Evrak");
        record.setCreatedBy(userId);

        User user = new User();
        user.setId(userId);
        user.setFirstName("Ahmet");
        user.setLastName("Yilmaz");
        user.setEmail("user@example.com");

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        WorkflowStatusChangedEvent event = new WorkflowStatusChangedEvent(
                recordId,
                WorkflowAction.GONDER,
                RecordStatus.TASLAK,
                RecordStatus.BASKAN_INCELEMESINDE,
                userId,
                RoleName.CALISAN,
                userId,
                null,
                "İncelemeye sunuldu.",
                Instant.now()
        );

        listener.sendMail(event);

        verify(mailService).sendStatusChangeMail(
                eq("user@example.com"),
                eq("Ahmet Yilmaz"),
                eq(recordId),
                eq("Test Evrak"),
                eq("BASKAN_INCELEMESINDE"),
                eq("İncelemeye sunuldu.")
        );
    }
}