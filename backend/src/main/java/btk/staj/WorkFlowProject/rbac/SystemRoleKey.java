package btk.staj.WorkFlowProject.rbac;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import java.util.Optional;

/** Built-in behavior keys only; dynamic roles are identified by roles.id. */
public enum SystemRoleKey {
    CALISAN, BASKAN_YARDIMCISI, BASKAN, ADMIN;

    public boolean matches(Role role) {
        return role != null && name().equals(role.getSystemKey());
    }

    public static Optional<SystemRoleKey> from(String key) {
        if (key == null) return Optional.empty();
        try { return Optional.of(valueOf(key)); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    /** Temporary seam until WF-2D2 / WF-2C2 remove legacy workflow and visibility roles. */
    public RoleName legacyRole() { return RoleName.valueOf(name()); }
}
