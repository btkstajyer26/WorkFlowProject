package btk.staj.WorkFlowProject.rbac.port;

import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.visibility.RecordVisibilityScope.DepartmentStatus;
import java.util.Set;

public interface DepartmentVisibilityPort {
    Set<DepartmentStatus> scopesFor(VisibilityActor actor);
}
