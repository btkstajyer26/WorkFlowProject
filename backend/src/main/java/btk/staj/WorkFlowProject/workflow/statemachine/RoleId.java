package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.Objects;

/** Environment-specific relational role identity; never a built-in role constant. */
public record RoleId(Integer value) {
    public RoleId {
        Objects.requireNonNull(value, "value");
        if (value <= 0) {
            throw new IllegalArgumentException("role id must be positive");
        }
    }
}
