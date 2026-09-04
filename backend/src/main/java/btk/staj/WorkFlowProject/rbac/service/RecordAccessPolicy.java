package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.rbac.port.DepartmentVisibilityPort;
import java.util.Set;
import btk.staj.WorkFlowProject.rbac.visibility.RecordVisibilityScope;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.UUID;

/** Record access and the separate, preserved system-role content/history views. */
@Component
public class RecordAccessPolicy {
    private final DepartmentVisibilityPort departmentVisibility;

    public RecordAccessPolicy(DepartmentVisibilityPort departmentVisibility) {
        this.departmentVisibility = Objects.requireNonNull(departmentVisibility, "departmentVisibility");
    }

    public RecordVisibilityScope scopeFor(VisibilityActor actor) {
        Objects.requireNonNull(actor, "actor");
        var departments = !actor.permissionCodes().contains("RECORD_VIEW") || actor.hasSystemRole(SystemRoleKey.ADMIN)
                ? Set.<RecordVisibilityScope.DepartmentStatus>of() : departmentVisibility.scopesFor(actor);
        return RecordVisibilityScope.forActor(actor, departments);
    }
    public boolean canView(VisibilityActor actor, Record record) {
        return scopeFor(actor).allows(record.getCreatedBy(), record.getAssignedTo(),
                record.getLastDeputyId(), record.getStatus(), record.getDeletedAt(), record.getAssignedDepartmentId());
    }

    public void assertCanView(VisibilityActor actor, Record record) {
        if (!canView(actor, record)) throw new ForbiddenException("Bu kaydı görüntüleme yetkiniz yok");
    }

    /** Returned records retain the deputy's handoff snapshot until reassigned. */
    public boolean seesRecordAsOfHandoff(VisibilityActor actor, UUID assignedTo, RecordStatus status) {
        return actor.hasSystemRole(SystemRoleKey.BASKAN_YARDIMCISI)
                && status == RecordStatus.DUZENLEME_BEKLIYOR
                && !Objects.equals(actor.id(), assignedTo);
    }

    /** The president's history begins at the first handover, not the latest one. */
    public boolean seesHistoryFromPresidentHandover(VisibilityActor actor) {
        return actor.hasSystemRole(SystemRoleKey.BASKAN);
    }
}
