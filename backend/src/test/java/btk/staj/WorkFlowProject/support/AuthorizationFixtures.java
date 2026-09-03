package btk.staj.WorkFlowProject.support;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import java.util.Set;

/** Explicit baseline permission data for tests; never a production authorization fallback. */
public final class AuthorizationFixtures {
    private AuthorizationFixtures() { }

    public static Set<String> permissions(RoleName role) {
        return role == null ? Set.of() : permissions(role.name());
    }

    public static Set<String> permissions(String role) {
        if (role == null) return Set.of();
        return switch (role) {
            case "CALISAN" -> Set.of("RECORD_CREATE", "RECORD_VIEW", "RECORD_EDIT", "RECORD_FORWARD", "FILE_MANAGE", "RECORD_DELETE");
            case "BASKAN_YARDIMCISI" -> Set.of("RECORD_VIEW", "RECORD_FORWARD", "RECORD_RETURN");
            case "BASKAN" -> Set.of("RECORD_VIEW", "RECORD_APPROVE", "RECORD_REJECT", "RECORD_RETURN");
            case "ADMIN" -> Set.of("USER_VIEW", "USER_MANAGE", "ROLE_VIEW", "ROLE_MANAGE", "DEPARTMENT_VIEW",
                    "DEPARTMENT_MANAGE", "WORKFLOW_VIEW", "WORKFLOW_MANAGE", "ADMIN_PANEL_ACCESS", "AUDIT_VIEW");
            default -> Set.of();
        };
    }

    public static boolean workflowActor(RoleName role) { return role != null && role.isWorkflowActor(); }
    public static boolean workflowActor(String role) {
        return "CALISAN".equals(role) || "BASKAN".equals(role) || "BASKAN_YARDIMCISI".equals(role);
    }

    public static String requiredPermission(Object action) {
        return switch (String.valueOf(action)) {
            case "ONAYLA" -> "RECORD_APPROVE";
            case "REDDET" -> "RECORD_REJECT";
            case "CALISANA_GERI_GONDER", "BASKAN_YARDIMCISINA_GERI_GONDER" -> "RECORD_RETURN";
            default -> "RECORD_FORWARD";
        };
    }

    public static AuthenticatedUser authenticated(User user) {
        return new AuthenticatedUser(user, user == null || user.getRole() == null
                ? Set.of() : permissions(user.getRole().getName()));
    }
}
