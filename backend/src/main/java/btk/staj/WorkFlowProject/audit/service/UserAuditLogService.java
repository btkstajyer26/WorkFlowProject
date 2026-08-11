package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;
import btk.staj.WorkFlowProject.audit.repository.UserAuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserAuditLogService {

    private final UserAuditLogRepository userAuditLogRepository;

    public UserAuditLogService(UserAuditLogRepository userAuditLogRepository) {
        this.userAuditLogRepository = userAuditLogRepository;
    }

    /**
     * Kullanıcı/rol değişikliği olduğunda (auth/user ekibi tarafından)
     * çağrılması gereken metod. Örn. rol değişikliği, aktif/pasif yapma.
     */
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
     * Bir kullanıcı üzerinde yapılan tüm admin işlemlerinin geçmişi.
     */
    public List<UserAuditLog> getGecmis(UUID targetUserId) {
        return userAuditLogRepository.findByTargetUserIdOrderByCreatedAtAsc(targetUserId);
    }
}
