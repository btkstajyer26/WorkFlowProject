package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Temporary WF-2D2 bridge, removed when workflow actors carry RoleId in PR 2. */
@Component
public final class LegacyWorkflowRoleMapping implements Supplier<Map<RoleId, RoleName>> {
    private final RoleRepository roles;

    public LegacyWorkflowRoleMapping(RoleRepository roles) {
        this.roles = Objects.requireNonNull(roles, "roles");
    }

    @Override
    public Map<RoleId, RoleName> get() {
        Map<RoleId, RoleName> mapping = new HashMap<>();
        for (var role : roles.findAll()) {
            SystemRoleKey.from(role.getSystemKey()).ifPresent(key ->
                    mapping.put(new RoleId(role.getId()), key.legacyRole()));
        }
        return Map.copyOf(mapping);
    }
}
