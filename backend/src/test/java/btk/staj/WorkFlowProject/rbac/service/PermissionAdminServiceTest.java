package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentUserProvider;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Permission;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.RolePermission;
import btk.staj.WorkFlowProject.rbac.dto.RolePermissionsResponse;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRolePermissionsRequest;
import btk.staj.WorkFlowProject.rbac.repository.PermissionRepository;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionAdminServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private RoleRepository roles;
    private PermissionRepository permissions;
    private RolePermissionRepository rolePermissions;
    private UserAuditLogService audit;
    private PermissionAdminService service;

    @BeforeEach
    void setUp() {
        roles = mock(RoleRepository.class);
        permissions = mock(PermissionRepository.class);
        rolePermissions = mock(RolePermissionRepository.class);
        audit = mock(UserAuditLogService.class);
        CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
        when(currentUser.currentUserId()).thenReturn(ADMIN_ID);
        service = new PermissionAdminService(roles, permissions, rolePermissions, audit, currentUser);

        when(permissions.findAllByOrderByIdAsc()).thenReturn(List.of(
                permission(1, "RECORD_VIEW", true),
                permission(2, "RECORD_EDIT", true),
                permission(3, "ROLE_MANAGE", true),
                permission(4, "DEPARTMENT_MANAGE", false)));
    }

    private static Permission permission(Integer id, String code, boolean active) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setDisplayName(code);
        permission.setActive(active);
        return permission;
    }

    private static Role role(Integer id, String name, boolean active) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setActive(active);
        return role;
    }

    @Nested
    class Listeleme {

        @Test
        void katalog_aktif_ve_pasif_kodlari_birlikte_dondurur() {
            List<String> codes = service.listPermissions().stream()
                    .map(btk.staj.WorkFlowProject.rbac.dto.PermissionResponse::code).toList();

            assertThat(codes).containsExactly("RECORD_VIEW", "RECORD_EDIT", "ROLE_MANAGE", "DEPARTMENT_MANAGE");
        }

        @Test
        void rolun_atanmis_kodlari_dondurulur() {
            when(roles.findById(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9)).thenReturn(List.of("RECORD_EDIT", "RECORD_VIEW"));

            RolePermissionsResponse response = service.getRolePermissions(9);

            assertThat(response.roleId()).isEqualTo(9);
            assertThat(response.roleName()).isEqualTo("Mali İşler Uzmanı");
            assertThat(response.permissionCodes()).containsExactly("RECORD_EDIT", "RECORD_VIEW");
        }

        @Test
        void bilinmeyen_rol_icin_rol_bulunamadi_hatasi_doner() {
            when(roles.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRolePermissions(404))
                    .isInstanceOf(RoleNotFoundException.class);
        }
    }

    @Nested
    class Guncelleme {

        private UpdateRolePermissionsRequest request(String... codes) {
            UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
            request.setPermissionCodes(Set.of(codes));
            return request;
        }

        @Test
        void bilinmeyen_rol_icin_rol_bulunamadi_hatasi_doner() {
            when(roles.findByIdForUpdate(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateRolePermissions(404, request("RECORD_VIEW")))
                    .isInstanceOf(RoleNotFoundException.class);
        }

        @Test
        void kayitli_olmayan_kod_reddedilir() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));

            assertThatThrownBy(() -> service.updateRolePermissions(9, request("UYDURMA_KOD")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Bilinmeyen permission kodu");
            verify(rolePermissions, never()).save(any());
        }

        @Test
        void pasif_permission_yeni_atanamaz() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9)).thenReturn(List.of());

            assertThatThrownBy(() -> service.updateRolePermissions(9, request("DEPARTMENT_MANAGE")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Pasif permission role atanamaz");
            verify(rolePermissions, never()).save(any());
        }

        @Test
        void yeni_kod_eklenir_ve_kayitli_kalan_kod_dokunulmaz() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9))
                    .thenReturn(List.of("RECORD_VIEW"))
                    .thenReturn(List.of("RECORD_VIEW", "ROLE_MANAGE"));

            RolePermissionsResponse response = service.updateRolePermissions(9, request("RECORD_VIEW", "ROLE_MANAGE"));

            ArgumentCaptor<RolePermission> saved = ArgumentCaptor.forClass(RolePermission.class);
            verify(rolePermissions).save(saved.capture());
            assertThat(saved.getValue().getId()).isEqualTo(new RolePermission.Id(9, 3));
            verify(rolePermissions, never()).deleteById(any());
            assertThat(response.permissionCodes()).containsExactly("RECORD_VIEW", "ROLE_MANAGE");
        }

        @Test
        void istekten_dusen_kod_kaldirilir() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9))
                    .thenReturn(List.of("RECORD_VIEW", "RECORD_EDIT"))
                    .thenReturn(List.of("RECORD_VIEW"));

            service.updateRolePermissions(9, request("RECORD_VIEW"));

            verify(rolePermissions).deleteById(new RolePermission.Id(9, 2));
            verify(rolePermissions, never()).save(any());
        }

        /**
         * Kod pasiflesmis olsa da zaten atanmissa kaldirma isteginde
         * engellenmez - yalniz YENI atama pasif kod icin reddedilir.
         */
        @Test
        void zaten_atanmis_pasif_kod_kaldirilabilir() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9))
                    .thenReturn(List.of("DEPARTMENT_MANAGE"))
                    .thenReturn(List.of());

            assertThatCode(() -> service.updateRolePermissions(9, request()))
                    .doesNotThrowAnyException();
            verify(rolePermissions).deleteById(new RolePermission.Id(9, 4));
        }

        @Test
        void degisiklik_yoksa_yazma_yapilmaz_ve_audit_atilmaz() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9)).thenReturn(List.of("RECORD_VIEW"));

            service.updateRolePermissions(9, request("RECORD_VIEW"));

            verify(rolePermissions, never()).save(any());
            verify(rolePermissions, never()).deleteById(any());
            verify(audit, never()).logIslem(any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void degisiklik_audit_kaydi_yazar() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9))
                    .thenReturn(List.of("RECORD_VIEW"))
                    .thenReturn(List.of("ROLE_MANAGE"));

            service.updateRolePermissions(9, request("ROLE_MANAGE"));

            verify(audit).logIslem(isNull(), eq(ADMIN_ID), eq("ROLE_PERMISSIONS_UPDATED"), eq(9), eq(9),
                    eq(true), eq(true), any());
        }

        /** Kilit sirasi WF8_AP8 sozlesmesinin gerektirdigi protokoldur. */
        @Test
        void rol_satiri_guncelleme_icin_kilitli_okunur() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9)).thenReturn(List.of());

            service.updateRolePermissions(9, request("RECORD_VIEW"));

            verify(roles).findByIdForUpdate(9);
        }

        @Test
        void bos_kume_rolun_butun_yetkilerini_kaldirir() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role(9, "Mali İşler Uzmanı", true)));
            when(rolePermissions.findAllCodesByRoleId(9))
                    .thenReturn(List.of("RECORD_VIEW", "RECORD_EDIT"))
                    .thenReturn(List.of());

            RolePermissionsResponse response = service.updateRolePermissions(9, request());

            assertThat(response.permissionCodes()).isEmpty();
            verify(rolePermissions).deleteById(new RolePermission.Id(9, 1));
            verify(rolePermissions).deleteById(new RolePermission.Id(9, 2));
        }
    }
}
