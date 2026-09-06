package btk.staj.WorkFlowProject.rbac.visibility;

import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Infrastructure-free scope shared by the in-memory decision and SQL adapter.
 * A non-deleted record is visible when any relation or status matches.
 * Adapters translate this data; they must not select rules by role.
 */
public record RecordVisibilityScope(UUID actorId, Set<Relation> relations, Set<RecordStatus> statuses,
        Set<DepartmentStatus> departmentScopes) {
    public record DepartmentStatus(int departmentId, RecordStatus status) {
        public DepartmentStatus { Objects.requireNonNull(status, "status"); }
    }
    public enum Relation { CREATOR, ASSIGNEE, PREVIOUS_DEPUTY }

    public RecordVisibilityScope {
        Objects.requireNonNull(actorId, "actorId");
        relations = Set.copyOf(relations);
        statuses = Set.copyOf(statuses);
        departmentScopes = Set.copyOf(departmentScopes);
    }

    public static RecordVisibilityScope forActor(VisibilityActor actor, Set<DepartmentStatus> departmentScopes) {
        Objects.requireNonNull(actor, "actor");
        if (!actor.permissionCodes().contains("RECORD_VIEW") || actor.hasSystemRole(SystemRoleKey.ADMIN)) {
            return new RecordVisibilityScope(actor.id(), Set.of(), Set.of(), Set.of());
        }
        // PREVIOUS_DEPUTY herkese verilir cunku ILISKI kendini sinirlar: yalnizca
        // actorId == lastDeputyId oldugunda eslesir ve o alana yazilmanin tek yolu
        // kaydi Baskana iletmis olmaktir. "Ilettigim kaydi gormeye devam ederim"
        // bir rol ayricaligi degil, kayitla kurulan bir iliskidir - dinamik rol de
        // ayni iliskiyi kurar (B13).
        var relations = EnumSet.of(Relation.CREATOR, Relation.ASSIGNEE, Relation.PREVIOUS_DEPUTY);
        var statuses = EnumSet.noneOf(RecordStatus.class);
        if (actor.hasSystemRole(SystemRoleKey.BASKAN_YARDIMCISI)) {
            // Bu DURUM izni ise rol geneli bir kuyruk ayricaligidir: yerlesik yardimci
            // duzeltmedeki HER kaydi gorur, ilettigi kayitlari degil. Tek bir yerlesik
            // koltuga ozgudur ve her dinamik role dagitilmaz.
            statuses.add(RecordStatus.DUZENLEME_BEKLIYOR);
        }
        if (actor.hasSystemRole(SystemRoleKey.BASKAN)) {
            statuses.addAll(EnumSet.of(RecordStatus.BASKAN_INCELEMESINDE,
                    RecordStatus.ONAYLANDI, RecordStatus.REDDEDILDI));
        }
        return new RecordVisibilityScope(actor.id(), relations, statuses, departmentScopes);
    }

    public boolean allows(UUID createdBy, UUID assignedTo, UUID lastDeputyId,
                          RecordStatus status, LocalDateTime deletedAt, Integer assignedDepartmentId) {
        if (deletedAt != null) return false;
        return (assignedDepartmentId != null && departmentScopes.contains(new DepartmentStatus(assignedDepartmentId, status)))
                || statuses.contains(Objects.requireNonNull(status, "status"))
                || relations.stream().anyMatch(relation -> actorId.equals(switch (relation) {
                    case CREATOR -> createdBy;
                    case ASSIGNEE -> assignedTo;
                    case PREVIOUS_DEPUTY -> lastDeputyId;
                }));
    }
}
