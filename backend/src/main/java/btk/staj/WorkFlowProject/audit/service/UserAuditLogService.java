package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.UserAuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;
import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.repository.UserAuditLogRepository;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Service
public class UserAuditLogService {

    private final UserAuditLogRepository userAuditLogRepository;

    public UserAuditLogService(UserAuditLogRepository userAuditLogRepository) {
        this.userAuditLogRepository = Objects.requireNonNull(
                userAuditLogRepository, "userAuditLogRepository");
    }


    public void logIslem(UUID targetUserId, UUID performedBy, String action,
                         Integer previousRoleId, Integer newRoleId,
                         Boolean previousActive, Boolean newActive, String comment) {

        UserAuditLog log = UserAuditLog.builder()
                .targetUserId(targetUserId)
                .performedBy(performedBy)
                .action(action)
                .previousRoleId(previousRoleId)
                .newRoleId(newRoleId)
                .previousActive(previousActive)
                .newActive(newActive)
                .comment(comment)
                .build();

        userAuditLogRepository.save(log);
    }

    /**
     * Admin olmayan kullanicilarin giris/cikis ve HTTP istekleri.
     * target_user_id ve performed_by ayni aktordur (bilinmeyen giriste ikisi de bos).
     */
    public void recordAccess(RequestAccessEvent event) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(event.action(), "action");

        UserAuditLog log = UserAuditLog.builder()
                .targetUserId(event.userId())
                .performedBy(event.userId())
                .action(event.action())
                .comment(event.comment())
                .httpMethod(event.httpMethod())
                .requestPath(event.requestPath())
                .httpStatus(event.httpStatus())
                .errorCode(event.errorCode())
                .build();

        userAuditLogRepository.save(log);
    }


    public List<UserAuditLogResponse> getGecmis(UUID targetUserId) {
        Objects.requireNonNull(targetUserId, "targetUserId");
        return userAuditLogRepository.findByTargetUserIdOrderByCreatedAtAsc(targetUserId)
                .stream()
                .map(UserAuditLogResponse::from)
                .toList();
    }


    public PagedResponse<UserAuditLogResponse> listAll(Pageable pageable) {
        Page<UserAuditLogResponse> page = userAuditLogRepository.findAllWithNames(pageable);

        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}