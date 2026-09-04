package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** All role occupancy writes serialize on the same role rows inside the caller's transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class RoleCapacityService {
    private final RoleRepository roles;
    private final UserRepository users;

    public RoleCapacityService(RoleRepository roles, UserRepository users) {
        this.roles = roles;
        this.users = users;
    }

    public Map<Integer, Role> lockRoles(Collection<Integer> ids) {
        Map<Integer, Role> result = new LinkedHashMap<>();
        ids.stream().filter(Objects::nonNull).distinct().sorted().forEach(id -> result.put(id,
                roles.findByIdForUpdate(id).orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + id))));
        return result;
    }

    public void assertAssignable(Role role) {
        if (!role.isActive()) throw new BusinessRuleException("Pasif rol atanamaz: " + role.getName());
    }

    /** Validate projected occupancy BEFORE mutating managed users (queries may auto-flush). */
    public void validate(Map<Integer, Role> lockedRoles, List<Change> changes) {
        Map<Integer, Integer> deltas = new HashMap<>();
        for (Change change : changes) {
            if (change.previousActive()) deltas.merge(change.previousRoleId(), -1, Integer::sum);
            if (change.nextActive()) deltas.merge(change.nextRoleId(), 1, Integer::sum);
        }
        for (var entry : deltas.entrySet()) {
            Role role = Objects.requireNonNull(lockedRoles.get(entry.getKey()), "Role must be locked");
            // Removing users must remain possible if an externally lowered limit is already exceeded.
            if (entry.getValue() <= 0 || role.getMaxUsers() == null) continue;
            long projected = users.countByRole_IdAndActiveTrue(role.getId()) + entry.getValue();
            if (projected > role.getMaxUsers()) {
                throw new AdminLimitExceededException(role.getMaxUsers() == 1
                        ? "Bu rol zaten başka bir kullanıcıya atanmış: " + role.getName()
                        : "Rolün aktif kullanıcı sınırına ulaşıldı: " + role.getName() + " (" + role.getMaxUsers() + ")");
            }
        }
    }

    public record Change(Integer previousRoleId, boolean previousActive, Integer nextRoleId, boolean nextActive) {
        public static Change of(User user, Role target, boolean active) {
            return new Change(user.getRole().getId(), user.isActive(), target.getId(), active);
        }
        public static Change create(Role target) { return new Change(null, false, target.getId(), true); }
    }
}
