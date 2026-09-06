package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.model.DepartmentRoutingResolution;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.DepartmentRoutingPort;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowTransitionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kullanilabilir aksiyon hesabinin saf birim testleri (APP-9 SS1).
 *
 * <p>Spring, veritabani ve HTTP yok. Kural kaynagi {@code StaticTransitionRuleSource};
 * {@link TargetUserResolver} ve {@link DepartmentRoutingResolver} <em>gercek</em>
 * ornekleridir, yalnizca altlarindaki port'lar sahtedir. Boylece test, hedef cozumunun
 * ve routing degerlendirmesinin gercek davranisini kapsar.
 */
@DisplayName("Kullanilabilir aksiyon cozumu")
class AvailableActionResolverTest {

    private static final UUID RECORD = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID CREATOR = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID DEPUTY = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID CHAIR = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final int DEPARTMENT = 7;

    private final TransitionRuleSource rules = WorkflowRoleFixtures.rules();

    // ---------------------------------------------------------------
    // Regresyon: dry-run iyi bicimli istegi taklit etmeli
    // ---------------------------------------------------------------

    @Test
    @DisplayName("aciklamasi zorunlu aksiyon listede yer alir")
    void listsActionsThatRequireAComment() {
        // Dry-run'da istek govdesi yoktur. Baglam bos aciklamayla kurulsaydi validator
        // WORKFLOW_COMMENT_REQUIRED ile reddeder ve bu aksiyon hic gorunmezdi.
        List<WorkflowAction> actions = resolver(noDepartments(), activeUsers())
                .resolve(actor(DEPUTY, RoleName.BASKAN_YARDIMCISI),
                        record(RecordStatus.BSK_YRD_INCELEMESINDE, DEPUTY), rules);

        assertThat(actions).contains(WorkflowAction.CALISANA_GERI_GONDER);
    }

    @Test
    @DisplayName("departmana gonderme, kullanilabilir departman varken listelenir")
    void listsDepartmentSendWhenARoutableDepartmentExists() {
        // Ayni baglamda hem GONDER (ROLE) hem DEPARTMANA_GONDER (DEPARTMENT) gorunur;
        // ikincisi dry-run'da hedef departman alani doldurulmus sayilmasaydi elenirdi.
        List<WorkflowAction> actions = resolver(routableDepartment(), activeUsers())
                .resolve(actor(CREATOR, RoleName.CALISAN), record(RecordStatus.TASLAK, null), rules);

        assertThat(actions).contains(WorkflowAction.GONDER, WorkflowAction.DEPARTMANA_GONDER);
    }

    @Test
    @DisplayName("kullanilabilir departman yoksa departmana gonderme listelenmez")
    void hidesDepartmentSendWithoutAnyRoutableDepartment() {
        List<WorkflowAction> actions = resolver(noDepartments(), activeUsers())
                .resolve(actor(CREATOR, RoleName.CALISAN), record(RecordStatus.TASLAK, null), rules);

        assertThat(actions)
                .contains(WorkflowAction.GONDER)
                .doesNotContain(WorkflowAction.DEPARTMANA_GONDER);
    }

    // ---------------------------------------------------------------
    // Cozulemeyen hedef gizlenir
    // ---------------------------------------------------------------

    @Test
    @DisplayName("hedef rolde aktif kullanici yoksa aksiyon listelenmez")
    void hidesActionsWhoseTargetRoleHasNoActiveUser() {
        // Gercek gecis burada WORKFLOW_ROLE_NOT_CONFIGURED alirdi; olu dugme sunulmaz.
        Users users = activeUsers();
        users.byRole.put(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), List.of());

        assertThat(resolver(noDepartments(), users)
                .resolve(actor(CREATOR, RoleName.CALISAN), record(RecordStatus.TASLAK, null), rules))
                .doesNotContain(WorkflowAction.GONDER);
    }

    @Test
    @DisplayName("hedef rolde birden fazla aktif kullanici varsa aksiyon listelenmez")
    void hidesActionsWhoseTargetRoleIsAmbiguous() {
        Users users = activeUsers();
        users.byRole.put(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), List.of(
                WorkflowRoleFixtures.target(DEPUTY, RoleName.BASKAN_YARDIMCISI, true),
                WorkflowRoleFixtures.target(UUID.randomUUID(), RoleName.BASKAN_YARDIMCISI, true)));

        assertThat(resolver(noDepartments(), users)
                .resolve(actor(CREATOR, RoleName.CALISAN), record(RecordStatus.TASLAK, null), rules))
                .doesNotContain(WorkflowAction.GONDER);
    }

    @Test
    @DisplayName("cozulen hedef pasifse aksiyon listelenmez")
    void hidesActionsWhoseResolvedTargetIsInactive() {
        Users users = activeUsers();
        WorkflowUserSnapshot inactive =
                WorkflowRoleFixtures.target(CREATOR, RoleName.CALISAN, false);
        users.byId.put(CREATOR, inactive);

        // CALISANA_GERI_GONDER hedefi CREATOR stratejisiyle kaydi olusturana coker.
        assertThat(resolver(noDepartments(), users)
                .resolve(actor(DEPUTY, RoleName.BASKAN_YARDIMCISI),
                        record(RecordStatus.BSK_YRD_INCELEMESINDE, DEPUTY), rules))
                .doesNotContain(WorkflowAction.CALISANA_GERI_GONDER);
    }

    // ---------------------------------------------------------------
    // Yetki ve durum sinirlari
    // ---------------------------------------------------------------

    @Test
    @DisplayName("terminal kayitta liste bostur")
    void returnsEmptyForTerminalRecords() {
        assertThat(resolver(routableDepartment(), activeUsers())
                .resolve(actor(CHAIR, RoleName.BASKAN), record(RecordStatus.ONAYLANDI, CHAIR), rules))
                .isEmpty();
    }

    @Test
    @DisplayName("kayitla iliskisi olmayan aktor icin liste bostur, hata degil")
    void returnsEmptyForAnActorWithoutTheRequiredRelation() {
        UUID stranger = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

        assertThat(resolver(noDepartments(), activeUsers())
                .resolve(actor(stranger, RoleName.BASKAN_YARDIMCISI),
                        record(RecordStatus.BSK_YRD_INCELEMESINDE, DEPUTY), rules))
                .isEmpty();
    }

    @Test
    @DisplayName("workflow aktoru olmayan rol hicbir aksiyon gormez")
    void returnsEmptyForNonWorkflowActors() {
        assertThat(resolver(noDepartments(), activeUsers())
                .resolve(actor(CHAIR, RoleName.ADMIN),
                        record(RecordStatus.BASKAN_INCELEMESINDE, CHAIR), rules))
                .isEmpty();
    }

    @Test
    @DisplayName("Baskan hedef gerektirmeyen onay ve ret aksiyonlarini birlikte gorur")
    void listsTerminalActionsForTheChair() {
        assertThat(resolver(noDepartments(), activeUsers())
                .resolve(actor(CHAIR, RoleName.BASKAN),
                        record(RecordStatus.BASKAN_INCELEMESINDE, CHAIR), rules))
                .contains(WorkflowAction.ONAYLA, WorkflowAction.REDDET);
    }

    @Test
    @DisplayName("departman kuyrugundaki kaydi routing uygun uyeye acar")
    void listsActionsForAnEligibleDepartmentMember() {
        // Kayit kisiye degil departmana atali; aktorun iliskisi routing uzerinden kurulur.
        WorkflowRecordSnapshot queued = new WorkflowRecordSnapshot(
                RECORD, RecordStatus.BSK_YRD_INCELEMESINDE, CREATOR, null, DEPUTY, null, 3, DEPARTMENT);

        assertThat(resolver(routableDepartment(), activeUsers())
                .resolve(actor(DEPUTY, RoleName.BASKAN_YARDIMCISI), queued, rules))
                .contains(WorkflowAction.BASKANA_ILET);
    }

    // ---------------------------------------------------------------
    // Sahteler
    // ---------------------------------------------------------------

    private AvailableActionResolver resolver(DepartmentRoutingPort routing, Users users) {
        return new AvailableActionResolver(
                new WorkflowTransitionValidator(rules),
                new TargetUserResolver(users),
                new DepartmentRoutingResolver(routing));
    }

    /** Degistirilebilir sahte kullanici deposu; testler tek bir kaydi bozup sonucu olcer. */
    private static final class Users implements WorkflowUserPort {
        private final Map<UUID, WorkflowUserSnapshot> byId = new HashMap<>();
        private final Map<RoleId, List<WorkflowUserSnapshot>> byRole = new HashMap<>();

        @Override
        public Optional<WorkflowUserSnapshot> findById(UUID userId) {
            return Optional.ofNullable(byId.get(userId));
        }

        @Override
        public List<WorkflowUserSnapshot> findActiveByRole(RoleId roleId) {
            return new ArrayList<>(byRole.getOrDefault(roleId, List.of()));
        }
    }

    private static Users activeUsers() {
        Users users = new Users();
        put(users, CREATOR, RoleName.CALISAN);
        put(users, DEPUTY, RoleName.BASKAN_YARDIMCISI);
        put(users, CHAIR, RoleName.BASKAN);
        return users;
    }

    private static void put(Users users, UUID id, RoleName role) {
        WorkflowUserSnapshot snapshot = WorkflowRoleFixtures.target(id, role, true);
        users.byId.put(id, snapshot);
        users.byRole.put(WorkflowRoleFixtures.id(role), List.of(snapshot));
    }

    private static DepartmentRoutingPort noDepartments() {
        return new StubRouting(Set.of());
    }

    /** Tek aktif departman; hedef rolu Bsk. Yrd., uygun bir aktif uyesi var. */
    private static DepartmentRoutingPort routableDepartment() {
        return new StubRouting(Set.of(DEPARTMENT));
    }

    private record StubRouting(Set<Integer> activeIds) implements DepartmentRoutingPort {

        @Override
        public DepartmentRoutingResolution resolve(int departmentId, RecordStatus from, WorkflowAction action) {
            if (!activeIds.contains(departmentId)) {
                return new DepartmentRoutingResolution.RuleNotConfigured(departmentId);
            }
            return new DepartmentRoutingResolution.Resolved(
                    WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), Set.of(DEPUTY));
        }

        @Override
        public boolean isActiveDepartment(int departmentId) {
            return activeIds.contains(departmentId);
        }

        @Override
        public Set<Integer> activeDepartmentIdsFor(UUID userId) {
            return activeIds;
        }

        @Override
        public Set<Integer> activeDepartmentIds() {
            return activeIds;
        }

        @Override
        public boolean roleHasPermission(RoleId roleId, String permissionCode) {
            return AuthorizationFixtures.permissions(RoleName.BASKAN_YARDIMCISI).contains(permissionCode);
        }
    }

    private static CurrentActor actor(UUID id, RoleName role) {
        return new CurrentActor(id, WorkflowRoleFixtures.id(role),
                AuthorizationFixtures.workflowActor(role), AuthorizationFixtures.permissions(role));
    }

    private static WorkflowRecordSnapshot record(RecordStatus status, UUID assignedTo) {
        return new WorkflowRecordSnapshot(RECORD, status, CREATOR, assignedTo, DEPUTY, null, 3, null);
    }
}
