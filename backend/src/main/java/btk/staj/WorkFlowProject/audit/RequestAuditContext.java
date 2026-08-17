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

    public record Snapshot(String action, UUID userId, Integer roleId, String roleName) {
    }

    private static final ThreadLocal<Snapshot> HOLDER = new ThreadLocal<>();

    public void mark(String action, User user) {
        if (user == null) {
            mark(action, null, null, null);
            return;
        }
        Integer roleId = user.getRole() != null ? user.getRole().getId() : null;
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        mark(action, user.getId(), roleId, roleName);
    }

    public void mark(String action, UUID userId, Integer roleId, String roleName) {
        HOLDER.set(new Snapshot(action, userId, roleId, roleName));
    }

    public Snapshot peek() {
        return HOLDER.get();
    }

    public void clear() {
        HOLDER.remove();
    }
}
