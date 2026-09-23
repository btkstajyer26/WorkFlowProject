package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskAssignableUserView;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskAssignableUsersResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskListResponse;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.mapper.SubtaskViewMapper;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubtaskQueryService {

    private final RecordRepository records;
    private final SubtaskRepository subtasks;
    private final UserRepository users;
    private final SubtaskViewMapper mapper;
    private final RecordAccessPolicy recordAccessPolicy;
    private final CurrentVisibilityActorProvider visibilityActorProvider;

    public SubtaskQueryService(
            RecordRepository records,
            SubtaskRepository subtasks,
            UserRepository users,
            SubtaskViewMapper mapper,
            RecordAccessPolicy recordAccessPolicy,
            CurrentVisibilityActorProvider visibilityActorProvider) {
        this.records = Objects.requireNonNull(records, "records");
        this.subtasks = Objects.requireNonNull(subtasks, "subtasks");
        this.users = Objects.requireNonNull(users, "users");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.recordAccessPolicy = Objects.requireNonNull(recordAccessPolicy, "recordAccessPolicy");
        this.visibilityActorProvider = Objects.requireNonNull(
                visibilityActorProvider,
                "visibilityActorProvider");
    }

    public SubtaskListResponse list(UUID parentRecordId) {
        UUID requiredParentId = Objects.requireNonNull(parentRecordId, "parentRecordId");
        Record parent = records.findByIdAndDeletedAtIsNull(requiredParentId)
                .orElseThrow(() -> new SubtaskException(
                        SubtaskException.Reason.PARENT_NOT_FOUND,
                        "Parent kayıt bulunamadı: " + requiredParentId));

        recordAccessPolicy.assertCanView(
                visibilityActorProvider.currentVisibilityActor(),
                parent);

        if (parent.getSubtaskApprovalPolicy() == null
                && parent.getSubtaskRequiredApprovals() == null) {
            return SubtaskListResponse.unsplit();
        }

        return new SubtaskListResponse(
                parent.getSubtaskApprovalPolicy(),
                parent.getSubtaskRequiredApprovals(),
                subtasks.findAllByParentRecord_IdOrderByCreatedAtAscIdAsc(requiredParentId)
                        .stream()
                        .map(mapper::toView)
                        .toList());
    }

    /**
     * Candidates the split screen may assign a Subtask to. Gated the same way
     * as {@link #list(UUID)} - visibility of the Parent, not a dedicated
     * capability permission - so the Başkan Yardımcısı who is allowed to
     * trigger {@code ALT_GOREVLERE_AYIR} (enforced by the workflow engine
     * itself when split is called) can always discover who to assign, without
     * needing the unrelated, Admin-only {@code USER_VIEW} permission.
     */
    public SubtaskAssignableUsersResponse assignableUsers(UUID parentRecordId) {
        UUID requiredParentId = Objects.requireNonNull(parentRecordId, "parentRecordId");
        Record parent = records.findByIdAndDeletedAtIsNull(requiredParentId)
                .orElseThrow(() -> new SubtaskException(
                        SubtaskException.Reason.PARENT_NOT_FOUND,
                        "Parent kayıt bulunamadı: " + requiredParentId));

        recordAccessPolicy.assertCanView(
                visibilityActorProvider.currentVisibilityActor(),
                parent);

        List<SubtaskAssignableUserView> candidates = users
                .findAssignableSubtaskCandidates(SystemRoleKey.SISTEM.name())
                .stream()
                .map(user -> new SubtaskAssignableUserView(user.getId(), fullName(user)))
                .toList();
        return new SubtaskAssignableUsersResponse(candidates);
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
