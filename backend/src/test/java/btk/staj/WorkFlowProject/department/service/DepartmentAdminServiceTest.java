package btk.staj.WorkFlowProject.department.service;

import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.department.dto.AddDepartmentMemberRequest;
import btk.staj.WorkFlowProject.department.dto.CreateDepartmentRequest;
import btk.staj.WorkFlowProject.department.dto.DepartmentMembersResponse;
import btk.staj.WorkFlowProject.department.dto.DepartmentResponse;
import btk.staj.WorkFlowProject.department.dto.UpdateDepartmentRequest;
import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;
import btk.staj.WorkFlowProject.department.entity.DepartmentMemberEntity;
import btk.staj.WorkFlowProject.department.exception.DepartmentInUseException;
import btk.staj.WorkFlowProject.department.exception.DepartmentNotFoundException;
import btk.staj.WorkFlowProject.department.port.DepartmentOpenUsagePort;
import btk.staj.WorkFlowProject.department.repository.DepartmentMemberRepository;
import btk.staj.WorkFlowProject.department.repository.DepartmentRepository;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentAdminServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private DepartmentRepository departments;
    private DepartmentMemberRepository members;
    private UserRepository users;
    private DepartmentOpenUsagePort openUsage;
    private AuditLogService auditLogs;
    private UserAuditLogService userAuditLogs;
    private DepartmentAdminService service;

    @BeforeEach
    void setUp() {
        departments = mock(DepartmentRepository.class);
        members = mock(DepartmentMemberRepository.class);
        users = mock(UserRepository.class);
        openUsage = mock(DepartmentOpenUsagePort.class);
        auditLogs = mock(AuditLogService.class);
        userAuditLogs = mock(UserAuditLogService.class);
        CurrentVisibilityActorProvider actors = mock(CurrentVisibilityActorProvider.class);
        when(actors.currentVisibilityActor()).thenReturn(new VisibilityActor(
                ADMIN_ID, new RoleId(1), Optional.of(SystemRoleKey.ADMIN), Set.of("DEPARTMENT_MANAGE")));
        service = new DepartmentAdminService(departments, members, users, openUsage, actors, auditLogs, userAuditLogs);
        when(departments.save(any(DepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(departments.findAllByOrderByNameAsc()).thenReturn(List.of());
    }

    private static DepartmentEntity department(Integer id, String name, Integer parentId, boolean active) {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(id);
        department.setName(name);
        department.setParentDepartmentId(parentId);
        department.setActive(active);
        return department;
    }

    @Nested
    class Listeleme {

        @Test
        void varsayilan_cagri_yalniz_aktif_departmanlari_dondurur() {
            when(departments.findAllByOrderByNameAsc()).thenReturn(List.of(
                    department(1, "Hukuk", null, true), department(2, "Arşiv", null, false)));

            assertThat(service.listDepartments(false)).extracting(DepartmentResponse::name).containsExactly("Hukuk");
        }

        @Test
        void yonetim_ekrani_pasif_departmanlari_da_gorebilir() {
            when(departments.findAllByOrderByNameAsc()).thenReturn(List.of(
                    department(1, "Hukuk", null, true), department(2, "Arşiv", null, false)));

            assertThat(service.listDepartments(true))
                    .extracting(DepartmentResponse::name).containsExactly("Hukuk", "Arşiv");
        }
    }

    @Nested
    class Olusturma {

        private CreateDepartmentRequest request(String name) {
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName(name);
            return request;
        }

        @Test
        void yeni_departman_aktif_acilir() {
            DepartmentResponse response = service.create(request("Hukuk"));

            assertThat(response.name()).isEqualTo("Hukuk");
            assertThat(response.active()).isTrue();
            assertThat(response.parentDepartmentId()).isNull();
        }

        @Test
        void ayni_adla_ikinci_departman_reddedilir() {
            when(departments.findByName("Hukuk")).thenReturn(Optional.of(department(1, "Hukuk", null, true)));

            assertThatThrownBy(() -> service.create(request("Hukuk")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("zaten kullanılıyor");
            verify(departments, never()).save(any());
        }

        @Test
        void bosluktan_ibaret_ad_reddedilir() {
            assertThatThrownBy(() -> service.create(request("   ")))
                    .isInstanceOf(BusinessRuleException.class);
            verify(departments, never()).save(any());
        }

        @Test
        void var_olmayan_ust_departman_reddedilir() {
            CreateDepartmentRequest request = request("Hukuk");
            request.setParentDepartmentId(99);
            when(departments.existsById(99)).thenReturn(false);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(DepartmentNotFoundException.class);
            verify(departments, never()).save(any());
        }

        @Test
        void olusturma_audit_kaydi_yazar() {
            service.create(request("Hukuk"));

            verify(auditLogs).recordAccess(argThatActionIs("DEPARTMENT_CREATED"));
        }
    }

    @Nested
    class Guncelleme {

        @Test
        void bilinmeyen_departman_icin_bulunamadi_hatasi_doner() {
            when(departments.findByIdForUpdate(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(404, new UpdateDepartmentRequest()))
                    .isInstanceOf(DepartmentNotFoundException.class);
        }

        @Test
        void ad_degistirilebilir() {
            when(departments.findByIdForUpdate(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setName("Hukuk İşleri");

            assertThat(service.update(1, request).name()).isEqualTo("Hukuk İşleri");
        }

        @Test
        void dogrudan_kendine_referans_reddedilir() {
            when(departments.findByIdForUpdate(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(departments.existsById(1)).thenReturn(true);
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setParentDepartmentId(1);

            assertThatThrownBy(() -> service.update(1, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("döngü");
        }

        /**
         * Mevcut zincir: A(1) &lt;- B(2, ustu A) &lt;- C(3, ustu B). A'nin ustunu
         * C yapmak A -&gt; C -&gt; B -&gt; A dongusu kurar; dogrudan kendine referans
         * degildir, ata zincirini yurumeden yakalanamaz.
         */
        @Test
        void dolayli_ata_dongusu_reddedilir() {
            when(departments.findByIdForUpdate(1)).thenReturn(Optional.of(department(1, "A", null, true)));
            when(departments.existsById(3)).thenReturn(true);
            when(departments.findById(3)).thenReturn(Optional.of(department(3, "C", 2, true)));
            when(departments.findById(2)).thenReturn(Optional.of(department(2, "B", 1, true)));
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setParentDepartmentId(3);

            assertThatThrownBy(() -> service.update(1, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("döngü");
        }

        @Test
        void ust_departman_kaldirilabilir() {
            when(departments.findByIdForUpdate(2)).thenReturn(Optional.of(department(2, "B", 1, true)));
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setClearParent(true);

            assertThat(service.update(2, request).parentDepartmentId()).isNull();
        }

        @Test
        void acik_kaydi_olan_departman_pasiflestirilemez() {
            when(departments.findByIdForUpdate(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(openUsage.hasOpenRecords(1)).thenReturn(true);
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setActive(false);

            assertThatThrownBy(() -> service.update(1, request))
                    .isInstanceOf(DepartmentInUseException.class)
                    .hasMessageContaining("işlem bekleyen açık kayıtlar var");
            verify(departments, never()).save(any());
        }

        @Test
        void kullanimda_olmayan_departman_pasiflestirilir() {
            when(departments.findByIdForUpdate(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(openUsage.hasOpenRecords(1)).thenReturn(false);
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setActive(false);

            assertThat(service.update(1, request).active()).isFalse();
        }

        @Test
        void degisiklik_yoksa_kayit_yapilmaz() {
            when(departments.findByIdForUpdate(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));

            assertThatCode(() -> service.update(1, new UpdateDepartmentRequest())).doesNotThrowAnyException();
            verify(departments, never()).save(any());
        }
    }

    @Nested
    class Uyelik {

        @Test
        void uye_eklenir() {
            when(departments.findById(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(users.findById(MEMBER_ID)).thenReturn(Optional.of(new User()));
            when(members.existsByIdDepartmentIdAndIdUserId(1, MEMBER_ID)).thenReturn(false);
            when(members.findAllByIdDepartmentId(1)).thenReturn(List.of());
            when(users.findAllById(any())).thenReturn(List.of());

            service.addMember(1, MEMBER_ID);

            verify(members).save(any(DepartmentMemberEntity.class));
            verify(auditLogs).recordAccess(argThatActionIs("DEPARTMENT_MEMBER_ADDED"));
        }

        @Test
        void zaten_uye_olan_kullanici_tekrar_eklenmez() {
            when(departments.findById(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(users.findById(MEMBER_ID)).thenReturn(Optional.of(new User()));
            when(members.existsByIdDepartmentIdAndIdUserId(1, MEMBER_ID)).thenReturn(true);
            when(members.findAllByIdDepartmentId(1)).thenReturn(List.of());
            when(users.findAllById(any())).thenReturn(List.of());

            service.addMember(1, MEMBER_ID);

            verify(members, never()).save(any());
            verify(auditLogs, never()).recordAccess(any());
        }

        @Test
        void var_olmayan_kullanici_eklenemez() {
            when(departments.findById(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(users.findById(MEMBER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addMember(1, MEMBER_ID))
                    .isInstanceOf(BusinessRuleException.class);
            verify(members, never()).save(any());
        }

        @Test
        void uye_cikarilir() {
            when(departments.findById(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(members.existsByIdDepartmentIdAndIdUserId(1, MEMBER_ID)).thenReturn(true);
            when(members.findAllByIdDepartmentId(1)).thenReturn(List.of());
            when(users.findAllById(any())).thenReturn(List.of());

            service.removeMember(1, MEMBER_ID);

            verify(members).deleteById(new DepartmentMemberEntity.Id(1, MEMBER_ID));
            verify(auditLogs).recordAccess(argThatActionIs("DEPARTMENT_MEMBER_REMOVED"));
        }

        @Test
        void uye_olmayan_kullanicinin_cikarilmasi_sessizce_gecer() {
            when(departments.findById(1)).thenReturn(Optional.of(department(1, "Hukuk", null, true)));
            when(members.existsByIdDepartmentIdAndIdUserId(1, MEMBER_ID)).thenReturn(false);
            when(members.findAllByIdDepartmentId(1)).thenReturn(List.of());
            when(users.findAllById(any())).thenReturn(List.of());

            DepartmentMembersResponse response = service.removeMember(1, MEMBER_ID);

            assertThat(response.departmentId()).isEqualTo(1);
            verify(members, never()).deleteById(any());
            verify(auditLogs, never()).recordAccess(any());
        }

        @Test
        void bilinmeyen_departman_icin_bulunamadi_hatasi_doner() {
            when(departments.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listMembers(404)).isInstanceOf(DepartmentNotFoundException.class);
        }
    }

    private static btk.staj.WorkFlowProject.audit.model.RequestAccessEvent argThatActionIs(String action) {
        return org.mockito.ArgumentMatchers.argThat(event -> event != null && action.equals(event.action()));
    }
}
