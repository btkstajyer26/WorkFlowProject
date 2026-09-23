package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskActionRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.mapper.SubtaskViewMapper;
import btk.staj.WorkFlowProject.subtask.model.SubtaskAction;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.subtask.statemachine.SubtaskStateMachine;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtaskActionServiceTest {

    private static final UUID SUBTASK_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PARENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ASSIGNEE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_USER_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID CREATED_BY_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final Instant COMPLETED_INSTANT = Instant.parse("2026-09-22T12:34:56Z");
    private static final LocalDateTime COMPLETED_AT =
            LocalDateTime.ofInstant(COMPLETED_INSTANT, ZoneOffset.UTC);
    private static final Clock FIXED_CLOCK = Clock.fixed(COMPLETED_INSTANT, ZoneOffset.UTC);

    @Mock
    private SubtaskRepository subtasks;
    @Mock
    private CurrentActorProvider currentActorProvider;
    @Mock
    private SubtaskJoinService joinService;
    @Mock
    private NotificationService notificationService;

    private SubtaskActionService service;

    @BeforeEach
    void setUp() {
        service = new SubtaskActionService(
                subtasks,
                currentActorProvider,
                new SubtaskStateMachine(),
                new SubtaskViewMapper(),
                joinService,
                notificationService,
                FIXED_CLOCK);
    }

    @Test
    void assignedHumanCanAdvanceWithoutWritingTerminalFields() {
        Subtask subtask = subtask(SubtaskStatus.DEGERLENDIRME, human(ASSIGNEE_ID));
        arrangeSuccess(subtask, actor(ASSIGNEE_ID, 22));

        SubtaskView result = service.performAction(
                SUBTASK_ID,
                new SubtaskActionRequest(
                        SubtaskAction.DEGERLENDIRMEYI_TAMAMLA,
                        "Ara geçiş açıklaması kaydedilmemeli"));

        assertThat(result.status()).isEqualTo(SubtaskStatus.ISLEM);
        assertThat(result.resolutionComment()).isNull();
        assertThat(result.completedAt()).isNull();
        assertThat(result.assignedTo()).isEqualTo(ASSIGNEE_ID);
        assertThat(subtask.getVersion()).isEqualTo(7);
        verify(subtasks).saveAndFlush(subtask);
        verifyNoInteractions(joinService);
        verify(notificationService).create(
                CREATED_BY_ID,
                PARENT_ID,
                "Alt görev işleme alındı: Alt görev",
                NotificationType.SUBTASK_UPDATED);
    }

    @Test
    void approvalMarksCompletionWithoutWritingAResolutionComment() {
        Subtask subtask = subtask(SubtaskStatus.ONAY, human(ASSIGNEE_ID));
        arrangeSuccess(subtask, actor(ASSIGNEE_ID, 22));

        SubtaskView result = service.performAction(
                SUBTASK_ID,
                new SubtaskActionRequest(SubtaskAction.ONAYLA, "kaydedilmemeli"));

        assertThat(result.status()).isEqualTo(SubtaskStatus.TAMAMLANDI);
        assertThat(result.completedAt()).isEqualTo(COMPLETED_AT);
        assertThat(result.resolutionComment()).isNull();

        InOrder order = inOrder(subtasks, joinService);
        order.verify(subtasks).saveAndFlush(subtask);
        order.verify(joinService).joinIfReady(PARENT_ID);
        verify(notificationService).create(
                CREATED_BY_ID,
                PARENT_ID,
                "Alt görev tamamlandı: Alt görev",
                NotificationType.SUBTASK_COMPLETED);
    }

    @Test
    void rejectionStoresTheCommentAndMarksCompletion() {
        Subtask subtask = subtask(SubtaskStatus.ONAY, human(ASSIGNEE_ID));
        arrangeSuccess(subtask, actor(ASSIGNEE_ID, 22));

        SubtaskView result = service.performAction(
                SUBTASK_ID,
                new SubtaskActionRequest(SubtaskAction.REDDET, "Eksik belge"));

        assertThat(result.status()).isEqualTo(SubtaskStatus.REDDEDILDI);
        assertThat(result.completedAt()).isEqualTo(COMPLETED_AT);
        assertThat(result.resolutionComment()).isEqualTo("Eksik belge");

        InOrder order = inOrder(subtasks, joinService);
        order.verify(subtasks).saveAndFlush(subtask);
        order.verify(joinService).joinIfReady(PARENT_ID);
        verify(notificationService).create(
                CREATED_BY_ID,
                PARENT_ID,
                "Alt görev reddedildi: Alt görev: Eksik belge",
                NotificationType.SUBTASK_REJECTED);
    }

    @Test
    void rejectsAnotherAuthenticatedUser() {
        Subtask subtask = subtask(SubtaskStatus.DEGERLENDIRME, human(ASSIGNEE_ID));
        when(subtasks.findOneById(SUBTASK_ID)).thenReturn(Optional.of(subtask));
        when(currentActorProvider.currentActor()).thenReturn(actor(OTHER_USER_ID, 22));

        assertReason(
                () -> service.performAction(
                        SUBTASK_ID,
                        new SubtaskActionRequest(SubtaskAction.DEGERLENDIRMEYI_TAMAMLA, null)),
                SubtaskException.Reason.ACTION_FORBIDDEN);

        verify(subtasks, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTheSystemActorEvenIfAssignedByInvalidStoredData() {
        User systemUser = user(ASSIGNEE_ID, 99, "SISTEM");
        Subtask subtask = subtask(SubtaskStatus.DEGERLENDIRME, systemUser);
        when(subtasks.findOneById(SUBTASK_ID)).thenReturn(Optional.of(subtask));
        when(currentActorProvider.currentActor()).thenReturn(actor(ASSIGNEE_ID, 99));

        assertReason(
                () -> service.performAction(
                        SUBTASK_ID,
                        new SubtaskActionRequest(SubtaskAction.DEGERLENDIRMEYI_TAMAMLA, null)),
                SubtaskException.Reason.ACTION_FORBIDDEN);

        verify(subtasks, never()).saveAndFlush(any());
    }

    @Test
    void reportsAMissingSubtaskBeforeResolvingTheActor() {
        when(subtasks.findOneById(SUBTASK_ID)).thenReturn(Optional.empty());

        assertReason(
                () -> service.performAction(
                        SUBTASK_ID,
                        new SubtaskActionRequest(SubtaskAction.DEGERLENDIRMEYI_TAMAMLA, null)),
                SubtaskException.Reason.NOT_FOUND);

        verifyNoInteractions(currentActorProvider);
        verify(subtasks, never()).saveAndFlush(any());
    }

    @Test
    void propagatesOptimisticLockingFailuresWithoutChangingTheVersionManually() {
        Subtask subtask = subtask(SubtaskStatus.ISLEM, human(ASSIGNEE_ID));
        when(subtasks.findOneById(SUBTASK_ID)).thenReturn(Optional.of(subtask));
        when(currentActorProvider.currentActor()).thenReturn(actor(ASSIGNEE_ID, 22));
        OptimisticLockingFailureException failure =
                new OptimisticLockingFailureException("version conflict");
        when(subtasks.saveAndFlush(subtask)).thenThrow(failure);

        assertThatThrownBy(() -> service.performAction(
                SUBTASK_ID,
                new SubtaskActionRequest(SubtaskAction.ISLEMI_TAMAMLA, null)))
                .isSameAs(failure);
        assertThat(subtask.getVersion()).isEqualTo(7);
        verifyNoInteractions(joinService);
        verifyNoInteractions(notificationService);
    }

    @Test
    void performActionDefinesTheTransactionBoundary() throws Exception {
        Method method = SubtaskActionService.class.getMethod(
                "performAction", UUID.class, SubtaskActionRequest.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

    private void arrangeSuccess(Subtask subtask, CurrentActor actor) {
        when(subtasks.findOneById(SUBTASK_ID)).thenReturn(Optional.of(subtask));
        when(currentActorProvider.currentActor()).thenReturn(actor);
        when(subtasks.saveAndFlush(subtask)).thenReturn(subtask);
    }

    private static Subtask subtask(SubtaskStatus status, User assignedTo) {
        Record parent = new Record();
        parent.setId(PARENT_ID);
        return Subtask.builder()
                .id(SUBTASK_ID)
                .parentRecord(parent)
                .title("Alt görev")
                .description("Açıklama")
                .assignedTo(assignedTo)
                .createdBy(human(CREATED_BY_ID))
                .status(status)
                .version(7)
                .build();
    }

    private static User human(UUID id) {
        return user(id, 22, "CALISAN");
    }

    private static User user(UUID id, int roleId, String systemKey) {
        Role role = new Role();
        role.setId(roleId);
        role.setName(systemKey);
        role.setSystem(true);
        role.setSystemKey(systemKey);
        role.setActive(true);

        User user = new User();
        user.setId(id);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setActive(true);
        user.setRole(role);
        return user;
    }

    private static CurrentActor actor(UUID id, int roleId) {
        return new CurrentActor(id, new RoleId(roleId), true, Set.of());
    }

    private static void assertReason(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation,
            SubtaskException.Reason expectedReason) {
        assertThatThrownBy(invocation)
                .isInstanceOfSatisfying(
                        SubtaskException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(expectedReason));
    }
}
