package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException.Reason;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskStatusCount;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.SystemActorProvider;
import btk.staj.WorkFlowProject.workflow.service.WorkflowApplicationService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parent/Subtask fan-out akisini, son terminal Subtask'tan sonra ana workflow'a
 * guvenli bicimde yeniden birlestirir.
 *
 * <p>Parent satiri sibling sayimindan once kilitlenir. Boylece es zamanli son
 * Subtask islemleri ayni Parent uzerinde siralanir ve yalniz biri workflow
 * gecisini gerceklestirebilir.</p>
 */
@Service
public class SubtaskJoinService {

    private static final WorkflowActionRequest JOIN_ACTION = new WorkflowActionRequest(
            WorkflowAction.ALT_GOREVLER_SONUCLANDI,
            null,
            null);

    private final RecordRepository records;
    private final SubtaskRepository subtasks;
    private final SystemActorProvider systemActorProvider;
    private final WorkflowApplicationService workflowApplicationService;

    public SubtaskJoinService(
            RecordRepository records,
            SubtaskRepository subtasks,
            SystemActorProvider systemActorProvider,
            WorkflowApplicationService workflowApplicationService) {
        this.records = Objects.requireNonNull(records, "records");
        this.subtasks = Objects.requireNonNull(subtasks, "subtasks");
        this.systemActorProvider = Objects.requireNonNull(systemActorProvider, "systemActorProvider");
        this.workflowApplicationService = Objects.requireNonNull(
                workflowApplicationService,
                "workflowApplicationService");
    }

    /**
     * Parent kilidi altinda sibling durumlarini yeniden sayar ve gerekiyorsa
     * SISTEM aktoruyle ana workflow gecisini calistirir.
     */
    @Transactional
    public JoinOutcome joinIfReady(UUID parentRecordId) {
        UUID requiredParentId = Objects.requireNonNull(parentRecordId, "parentRecordId");

        Record parent = records.findByIdForUpdate(requiredParentId)
                .filter(record -> record.getDeletedAt() == null)
                .orElseThrow(() -> error(
                        Reason.PARENT_NOT_FOUND,
                        "Parent kayıt bulunamadı: " + requiredParentId));

        if (parent.getStatus() == RecordStatus.KONTROL) {
            return JoinOutcome.ALREADY_JOINED;
        }
        if (parent.getStatus() != RecordStatus.ALT_GOREV_BEKLIYOR) {
            throw error(
                    Reason.PARENT_STATUS_INVALID,
                    "Parent kayıt alt görevlerin tamamlanmasını beklemiyor: " + parent.getStatus());
        }

        StatusCounts counts = statusCounts(requiredParentId);
        int requiredApprovals = validateAndGetRequiredApprovals(parent, counts.total());

        if (counts.terminal() != counts.total()) {
            return JoinOutcome.WAITING_FOR_SUBTASKS;
        }

        boolean approved = counts.completed() >= requiredApprovals;
        CurrentActor systemActor = Objects.requireNonNull(
                systemActorProvider.systemActor(),
                "systemActorProvider.systemActor()");
        workflowApplicationService.performAction(requiredParentId, JOIN_ACTION, systemActor);

        return approved
                ? JoinOutcome.TRANSITIONED_APPROVED
                : JoinOutcome.TRANSITIONED_REJECTED;
    }

    private StatusCounts statusCounts(UUID parentRecordId) {
        List<SubtaskStatusCount> rows = Objects.requireNonNull(
                subtasks.countStatusesByParentRecordId(parentRecordId),
                "subtasks.countStatusesByParentRecordId(parentRecordId)");
        Map<SubtaskStatus, Long> counts = new EnumMap<>(SubtaskStatus.class);
        long total = 0;
        for (SubtaskStatusCount row : rows) {
            SubtaskStatusCount requiredRow = Objects.requireNonNull(row, "subtask status count");
            if (counts.putIfAbsent(requiredRow.status(), requiredRow.count()) != null) {
                throw error(
                        Reason.INVALID_SUBTASK,
                        "Aynı alt görev durumu için birden fazla sayım sonucu döndü: "
                                + requiredRow.status());
            }
            total = Math.addExact(total, requiredRow.count());
        }

        if (total < 2) {
            throw error(
                    Reason.INVALID_SUBTASK,
                    "Parent kayıt en az iki alt göreve sahip olmalıdır");
        }

        long completed = counts.getOrDefault(SubtaskStatus.TAMAMLANDI, 0L);
        long rejected = counts.getOrDefault(SubtaskStatus.REDDEDILDI, 0L);
        return new StatusCounts(total, completed, rejected);
    }

    private static int validateAndGetRequiredApprovals(Record parent, long total) {
        SubtaskApprovalPolicy policy = parent.getSubtaskApprovalPolicy();
        if (policy == null) {
            throw error(
                    Reason.INVALID_APPROVAL_POLICY,
                    "Parent kayıt için alt görev onay politikası bulunamadı");
        }

        long expected = switch (policy) {
            case UNANIMOUS -> total;
            case MAJORITY -> (total / 2) + 1;
        };
        Integer stored = parent.getSubtaskRequiredApprovals();
        if (stored == null || stored <= 0 || stored.longValue() != expected) {
            throw error(
                    Reason.INVALID_REQUIRED_APPROVALS,
                    "Parent kayıt onay eşiği alt görev sayısı ve politika ile uyumsuz");
        }
        return stored;
    }

    private static SubtaskException error(Reason reason, String message) {
        return new SubtaskException(reason, message);
    }

    private record StatusCounts(long total, long completed, long rejected) {

        private long terminal() {
            return Math.addExact(completed, rejected);
        }
    }

    public enum JoinOutcome {
        WAITING_FOR_SUBTASKS,
        ALREADY_JOINED,
        TRANSITIONED_APPROVED,
        TRANSITIONED_REJECTED
    }
}
