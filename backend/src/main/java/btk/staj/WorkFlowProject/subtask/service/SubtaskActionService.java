package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskActionRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException.Reason;
import btk.staj.WorkFlowProject.subtask.mapper.SubtaskViewMapper;
import btk.staj.WorkFlowProject.subtask.model.SubtaskAction;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.subtask.statemachine.SubtaskStateMachine;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubtaskActionService {

    private final SubtaskRepository subtasks;
    private final CurrentActorProvider currentActorProvider;
    private final SubtaskStateMachine stateMachine;
    private final SubtaskViewMapper mapper;
    private final SubtaskJoinService joinService;
    private final NotificationService notificationService;
    private final Clock clock;

    public SubtaskActionService(
            SubtaskRepository subtasks,
            CurrentActorProvider currentActorProvider,
            SubtaskStateMachine stateMachine,
            SubtaskViewMapper mapper,
            SubtaskJoinService joinService,
            NotificationService notificationService,
            Clock clock) {
        this.subtasks = Objects.requireNonNull(subtasks, "subtasks");
        this.currentActorProvider = Objects.requireNonNull(currentActorProvider, "currentActorProvider");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.joinService = Objects.requireNonNull(joinService, "joinService");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public SubtaskView performAction(UUID subtaskId, SubtaskActionRequest request) {
        UUID requiredSubtaskId = Objects.requireNonNull(subtaskId, "subtaskId");
        SubtaskActionRequest requiredRequest = Objects.requireNonNull(request, "request");

        Subtask subtask = subtasks.findOneById(requiredSubtaskId)
                .orElseThrow(() -> error(
                        Reason.NOT_FOUND,
                        "Alt görev bulunamadı: " + requiredSubtaskId));
        CurrentActor actor = Objects.requireNonNull(
                currentActorProvider.currentActor(),
                "currentActorProvider.currentActor()");
        assertAssignedHuman(actor, subtask);

        SubtaskStatus target = stateMachine.transition(
                subtask.getStatus(),
                requiredRequest.action(),
                requiredRequest.comment());
        subtask.setStatus(target);

        if (target.isTerminal()) {
            subtask.setCompletedAt(LocalDateTime.now(clock));
        }
        if (requiredRequest.action() == SubtaskAction.REDDET) {
            subtask.setResolutionComment(requiredRequest.comment());
        }

        Subtask saved = Objects.requireNonNull(
                subtasks.saveAndFlush(subtask),
                "subtasks.saveAndFlush(subtask)");
        if (target.isTerminal()) {
            joinService.joinIfReady(saved.getParentRecord().getId());
        }
        notificationService.create(
                saved.getCreatedBy().getId(),
                saved.getParentRecord().getId(),
                notificationMessage(saved, target),
                notificationType(target));
        return mapper.toView(saved);
    }

    private static NotificationType notificationType(SubtaskStatus status) {
        return switch (status) {
            case ISLEM, ONAY -> NotificationType.SUBTASK_UPDATED;
            case TAMAMLANDI -> NotificationType.SUBTASK_COMPLETED;
            case REDDEDILDI -> NotificationType.SUBTASK_REJECTED;
            case DEGERLENDIRME -> throw new IllegalArgumentException(
                    "No action notification for initial Subtask status");
        };
    }

    private static String notificationMessage(Subtask subtask, SubtaskStatus status) {
        String message = switch (status) {
            case ISLEM -> "Alt görev işleme alındı: " + subtask.getTitle();
            case ONAY -> "Alt görev onaya gönderildi: " + subtask.getTitle();
            case TAMAMLANDI -> "Alt görev tamamlandı: " + subtask.getTitle();
            case REDDEDILDI -> "Alt görev reddedildi: " + subtask.getTitle()
                    + ": " + subtask.getResolutionComment();
            case DEGERLENDIRME -> throw new IllegalArgumentException(
                    "No action notification for initial Subtask status");
        };
        return message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }

    private static void assertAssignedHuman(CurrentActor actor, Subtask subtask) {
        User assignedTo = Objects.requireNonNull(subtask.getAssignedTo(), "subtask.assignedTo");
        boolean assignedActor = actor.id().equals(assignedTo.getId());
        boolean humanActor = assignedTo.isActive()
                && assignedTo.getRole() != null
                && assignedTo.getRole().isActive()
                && !SystemRoleKey.SISTEM.matches(assignedTo.getRole());
        if (!assignedActor || !humanActor) {
            throw error(
                    Reason.ACTION_FORBIDDEN,
                    "Bu alt görev üzerinde işlem yapma yetkiniz yok");
        }
    }

    private static SubtaskException error(Reason reason, String message) {
        return new SubtaskException(reason, message);
    }
}
