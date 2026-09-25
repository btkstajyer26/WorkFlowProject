package btk.staj.WorkFlowProject.audit;

import btk.staj.WorkFlowProject.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aynı HTTP isteği içinde AuthService'in (login/logout/refresh) ürettiği
 * iş eylemini filtreye taşır. ThreadLocal, istek bitince temizlenir.
 */
@Component
public class RequestAuditContext {

    public record Snapshot(String action, UUID userId, Integer roleId, String systemKey) {
    }

    private static final ThreadLocal<Snapshot> HOLDER = new ThreadLocal<>();

    public void mark(String action, User user) {
        if (user == null) {
            mark(action, null, null, null);
            return;
        }
        Integer roleId = user.getRole() != null ? user.getRole().getId() : null;
        String systemKey = user.getRole() != null ? user.getRole().getSystemKey() : null;
        mark(action, user.getId(), roleId, systemKey);
    }

    public void mark(String action, UUID userId, Integer roleId, String systemKey) {
        HOLDER.set(new Snapshot(action, userId, roleId, systemKey));
    }

    public Snapshot peek() {
        return HOLDER.get();
    }

    public void clear() {
        HOLDER.remove();
    }
}
