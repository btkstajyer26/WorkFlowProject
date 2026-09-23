package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskListResponse;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.mapper.SubtaskViewMapper;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubtaskQueryService {

    private final RecordRepository records;
    private final SubtaskRepository subtasks;
    private final SubtaskViewMapper mapper;
    private final RecordAccessPolicy recordAccessPolicy;
    private final CurrentVisibilityActorProvider visibilityActorProvider;

    public SubtaskQueryService(
            RecordRepository records,
            SubtaskRepository subtasks,
            SubtaskViewMapper mapper,
            RecordAccessPolicy recordAccessPolicy,
            CurrentVisibilityActorProvider visibilityActorProvider) {
        this.records = Objects.requireNonNull(records, "records");
        this.subtasks = Objects.requireNonNull(subtasks, "subtasks");
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
}
