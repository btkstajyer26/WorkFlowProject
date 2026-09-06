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

    /**
     * Returned records retain the forwarding actor's handoff snapshot until reassigned.
     *
     * <p>Iki kol da korunur (B13): yerlesik Baskan Yardimcisi duzeltmedeki her kaydi
     * dondurulmus gorur (rol geneli kuyruk gorunumu), ve kaydi ileten <em>kim olursa
     * olsun</em> kendi biraktigi hali gorur. Ikinci kol olmadan dinamik rol, geri
     * dondurulen kaydin guncel icerigini gorurdu - yerlesik rolle ayni konumda
     * farkli davranis.
     */
    public boolean seesRecordAsOfHandoff(VisibilityActor actor, UUID assignedTo,
                                         UUID lastDeputyId, RecordStatus status) {
        boolean forwardedByActor = actor.hasSystemRole(SystemRoleKey.BASKAN_YARDIMCISI)
                || Objects.equals(actor.id(), lastDeputyId);
        return forwardedByActor
                && status == RecordStatus.DUZENLEME_BEKLIYOR
                && !Objects.equals(actor.id(), assignedTo);
    }

    /** The president's history begins at the first handover, not the latest one. */
    public boolean seesHistoryFromPresidentHandover(VisibilityActor actor) {
        return actor.hasSystemRole(SystemRoleKey.BASKAN);
    }
}
