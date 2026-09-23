package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskCreateItem;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitResponse;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException.Reason;
import btk.staj.WorkFlowProject.subtask.mapper.SubtaskViewMapper;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubtaskService {

    private final RecordRepository records;
    private final SubtaskRepository subtasks;
    private final UserRepository users;
    private final CurrentActorProvider currentActorProvider;
    private final WorkflowActionService workflowActionService;
    private final SubtaskViewMapper mapper;
    private final NotificationService notificationService;

    public SubtaskService(
            RecordRepository records,
            SubtaskRepository subtasks,
            UserRepository users,
            CurrentActorProvider currentActorProvider,
            WorkflowActionService workflowActionService,
            SubtaskViewMapper mapper,
            NotificationService notificationService) {
        this.records = Objects.requireNonNull(records, "records");
        this.subtasks = Objects.requireNonNull(subtasks, "subtasks");
        this.users = Objects.requireNonNull(users, "users");
        this.currentActorProvider = Objects.requireNonNull(currentActorProvider, "currentActorProvider");
        this.workflowActionService = Objects.requireNonNull(workflowActionService, "workflowActionService");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
    }

    /**
     * Creates all children and advances their Parent in one transaction.
     * A workflow failure therefore rolls back both the policy fields and children.
     */
    @Transactional
    public SubtaskSplitResponse split(UUID parentRecordId, SubtaskSplitRequest request) {
        UUID requiredParentId = Objects.requireNonNull(parentRecordId, "parentRecordId");
        SubtaskSplitRequest requiredRequest = Objects.requireNonNull(request, "request");

        Record parent = records.findByIdAndDeletedAtIsNull(requiredParentId)
                .orElseThrow(() -> error(
                        Reason.PARENT_NOT_FOUND,
                        "Parent kayıt bulunamadı: " + requiredParentId));

        assertNotPreviouslySplit(parent);
        if (parent.getStatus() != RecordStatus.BSK_YRD_INCELEMESINDE) {
            throw error(
                    Reason.PARENT_STATUS_INVALID,
                    "Parent kayıt alt görevlere ayrılabilecek durumda değil");
        }

        List<SubtaskCreateItem> items = requiredRequest.subtasks();
        if (items == null || items.size() < 2) {
            throw error(Reason.COUNT_TOO_LOW, "En az iki alt görev oluşturulmalıdır");
        }

        int requiredApprovals = requiredApprovals(requiredRequest.approvalPolicy(), items.size());
        Set<UUID> assigneeIds = distinctAssigneeIds(items);
        Map<UUID, User> assignees = loadAndValidateAssignees(items, assigneeIds);

        CurrentActor actor = Objects.requireNonNull(
                currentActorProvider.currentActor(),
                "currentActorProvider.currentActor()");
        User createdBy = users.findById(actor.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated actor user not found: " + actor.id()));

        parent.setSubtaskApprovalPolicy(requiredRequest.approvalPolicy());
        parent.setSubtaskRequiredApprovals(requiredApprovals);

        List<Subtask> created = new ArrayList<>(items.size());
        for (SubtaskCreateItem item : items) {
            created.add(Subtask.builder()
                    .parentRecord(parent)
                    .title(item.title())
                    .description(item.description())
                    .assignedTo(assignees.get(item.assignedTo()))
                    .status(SubtaskStatus.DEGERLENDIRME)
                    .createdBy(createdBy)
                    .resolutionComment(null)
                    .completedAt(null)
                    .build());
        }
        // Subtask'in @Version alani builder'da 0'a varsayilir, bu yuzden Spring Data JPA'nin
        // versiyon-tabanli isNew() sezgisi bu entity'leri "yeni degil" sanip persist() yerine
        // merge()'e yonlendirebilir - merge() FARKLI bir yonetilen kopya dondurur, orijinal
        // `created` referanslari hic mutasyona ugramaz. Bu yuzden id/createdAt'i asla
        // `created`'tan degil, saveAllAndFlush'in donus degerinden okuyoruz (AndFlush,
        // @CreationTimestamp'in de flush sirasinda dolmasini saglar).
        List<Subtask> saved = subtasks.saveAllAndFlush(created);

        workflowActionService.performAction(
                requiredParentId,
                new WorkflowActionRequest(WorkflowAction.ALT_GOREVLERE_AYIR, null, null));

        for (Subtask subtask : saved) {
            notificationService.create(
                    subtask.getAssignedTo().getId(),
                    requiredParentId,
                    "Size yeni bir alt görev atandı: " + subtask.getTitle(),
                    NotificationType.SUBTASK_ASSIGNED);
        }

        return new SubtaskSplitResponse(
                requiredParentId,
                requiredRequest.approvalPolicy(),
                requiredApprovals,
                saved.stream().map(mapper::toView).toList());
    }

    private void assertNotPreviouslySplit(Record parent) {
        if (parent.getSubtaskApprovalPolicy() != null
                || parent.getSubtaskRequiredApprovals() != null
                || subtasks.countByParentRecord_Id(parent.getId()) > 0) {
            throw error(Reason.ALREADY_SPLIT, "Parent kayıt daha önce alt görevlere ayrılmış");
        }
    }

    private static int requiredApprovals(SubtaskApprovalPolicy policy, int subtaskCount) {
        if (policy == null) {
            throw error(Reason.INVALID_APPROVAL_POLICY, "Alt görev onay politikası geçersiz");
        }

        int required = switch (policy) {
            case UNANIMOUS -> subtaskCount;
            case MAJORITY -> Math.floorDiv(subtaskCount, 2) + 1;
        };
        if (required < 1 || required > subtaskCount) {
            throw error(
                    Reason.INVALID_REQUIRED_APPROVALS,
                    "Gerekli alt görev onay sayısı hesaplanamadı");
        }
        return required;
    }

    private static Set<UUID> distinctAssigneeIds(List<SubtaskCreateItem> items) {
        Set<UUID> assigneeIds = new LinkedHashSet<>();
        for (SubtaskCreateItem item : items) {
            if (item == null || item.assignedTo() == null) {
                throw error(Reason.INVALID_SUBTASK, "Alt görev ve atanan kullanıcı zorunludur");
            }
            if (!assigneeIds.add(item.assignedTo())) {
                throw error(
                        Reason.DUPLICATE_ASSIGNEE,
                        "Aynı kullanıcı birden fazla alt göreve atanamaz: " + item.assignedTo());
            }
        }
        return assigneeIds;
    }

    private Map<UUID, User> loadAndValidateAssignees(
            List<SubtaskCreateItem> items,
            Set<UUID> assigneeIds) {
        Map<UUID, User> assignees = new LinkedHashMap<>();
        for (User user : users.findAllByIdIn(assigneeIds)) {
            assignees.put(user.getId(), user);
        }

        for (SubtaskCreateItem item : items) {
            UUID assigneeId = item.assignedTo();
            User assignee = assignees.get(assigneeId);
            if (assignee == null) {
                throw error(Reason.ASSIGNEE_NOT_FOUND, "Atanacak kullanıcı bulunamadı: " + assigneeId);
            }
            if (!assignee.isActive() || assignee.getRole() == null || !assignee.getRole().isActive()) {
                throw error(Reason.ASSIGNEE_INACTIVE, "Atanacak kullanıcı aktif değil: " + assigneeId);
            }
            if (SystemRoleKey.SISTEM.matches(assignee.getRole())) {
                throw error(Reason.ASSIGNEE_SYSTEM, "SİSTEM kullanıcısı alt göreve atanamaz");
            }
        }
        return assignees;
    }

    private static SubtaskException error(Reason reason, String message) {
        return new SubtaskException(reason, message);
    }
}
