package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.DepartmentRoutingResolution;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.Set;
import java.util.UUID;

/** Persistence boundary; membership and mutable role state are read per operation. */
public interface DepartmentRoutingPort {
    DepartmentRoutingResolution resolve(int departmentId, RecordStatus from, WorkflowAction action);
    boolean isActiveDepartment(int departmentId);
    Set<Integer> activeDepartmentIdsFor(UUID userId);
    boolean roleHasPermission(RoleId roleId, String permissionCode);
}
