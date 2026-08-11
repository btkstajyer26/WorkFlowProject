package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.UserAuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;
import btk.staj.WorkFlowProject.audit.repository.UserAuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Kullanici ve rol degisikliklerinin denetim izi. Onay akisindan bagimsizdir;
 * Admin islemlerini yapan taraf (auth/user modulu) cagirir.
 */
@Service
public class UserAuditLogService {

    private final UserAuditLogRepository userAuditLogRepository;

    public UserAuditLogService(UserAuditLogRepository userAuditLogRepository) {
        this.userAuditLogRepository = Objects.requireNonNull(
                userAuditLogRepository, "userAuditLogRepository");
    }

    /**
     * Kullanici/rol degisikligini yazar. Ornek aksiyonlar: rol degistirme,
     * hesabi etkinlestirme/pasiflestirme, bootstrap Admin olusturma.
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

    /** Bir kullanici uzerinde yapilan tum admin islemlerinin gecmisi. */
    public List<UserAuditLogResponse> getGecmis(UUID targetUserId) {
        Objects.requireNonNull(targetUserId, "targetUserId");
        return userAuditLogRepository.findByTargetUserIdOrderByCreatedAtAsc(targetUserId)
                .stream()
                .map(UserAuditLogResponse::from)
                .toList();
    }
}
