package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentUserProvider;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.dto.CreateRoleRequest;
import btk.staj.WorkFlowProject.rbac.dto.RoleResponse;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRoleRequest;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAdminServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private RoleRepository roles;
    private UserRepository users;
    private UserAuditLogService audit;
    private RoleAdminService service;

    @BeforeEach
    void setUp() {
        roles = mock(RoleRepository.class);
        users = mock(UserRepository.class);
        audit = mock(UserAuditLogService.class);
        CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
        when(currentUser.currentUserId()).thenReturn(ADMIN_ID);
        service = new RoleAdminService(roles, users, audit, currentUser);
        when(roles.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Ad benzersizliği tüm katalogu Türkçe kurallarıyla tarar.
        when(roles.findAllByOrderByIdAsc()).thenReturn(List.of());
    }

    private static Role role(Integer id, String name, boolean system, String systemKey, boolean active) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setSystem(system);
        role.setSystemKey(systemKey);
        role.setActive(active);
        return role;
    }

    private static Role dynamicRole() {
        return role(9, "Mali İşler Uzmanı", false, null, true);
    }

    private static Role systemRole() {
        return role(1, "CALISAN", true, "CALISAN", true);
    }

    @Nested
    class Listeleme {

        @Test
        void varsayilan_cagri_yalniz_aktif_rolleri_dondurur() {
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(
                    systemRole(), role(9, "Pasif rol", false, null, false)));

            assertThat(service.listRoles(false)).extracting(RoleResponse::name).containsExactly("CALISAN");
        }

        @Test
        void yonetim_ekrani_pasif_rolleri_de_gorebilir() {
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(
                    systemRole(), role(9, "Pasif rol", false, null, false)));

            assertThat(service.listRoles(true))
                    .extracting(RoleResponse::name).containsExactly("CALISAN", "Pasif rol");
        }

        @Test
        void yanit_sistem_rolu_alanlarini_tasir() {
            Role role = systemRole();
            role.setWorkflowActor(true);
            role.setMaxUsers(1);
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(role));

            RoleResponse response = service.listRoles(false).getFirst();

            assertThat(response.systemKey()).isEqualTo("CALISAN");
            assertThat(response.system()).isTrue();
            assertThat(response.workflowActor()).isTrue();
            assertThat(response.maxUsers()).isEqualTo(1);
            assertThat(response.active()).isTrue();
        }
    }

    @Nested
    class Olusturma {

        private CreateRoleRequest request(String name) {
            CreateRoleRequest request = new CreateRoleRequest();
            request.setName(name);
            return request;
        }

        @Test
        void yeni_rol_daima_dinamik_ve_sinirsiz_kapasiteli_acilir() {
            CreateRoleRequest request = request("  Mali İşler Uzmanı  ");
            request.setDescription("  Bütçe evraklarını yürütür  ");
            request.setWorkflowActor(true);

            RoleResponse response = service.create(request);

            assertThat(response.name()).isEqualTo("Mali İşler Uzmanı");
            assertThat(response.description()).isEqualTo("Bütçe evraklarını yürütür");
            assertThat(response.systemKey()).isNull();
            assertThat(response.system()).isFalse();
            assertThat(response.maxUsers()).isNull();
            assertThat(response.active()).isTrue();
            // WF-8 baglama sartı: dinamik aktor rolu isaretlenebilmeli.
            assertThat(response.workflowActor()).isTrue();
        }

        @Test
        void ayni_adla_ikinci_rol_reddedilir() {
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(dynamicRole()));

            assertThatThrownBy(() -> service.create(request("Mali İşler Uzmanı")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("zaten kullanılıyor");
            verify(roles, never()).save(any());
        }

        @Test
        void yalniz_harf_buyuklugu_farkli_ad_da_reddedilir() {
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(role(9, "Muhasebe", false, null, true)));

            assertThatThrownBy(() -> service.create(request("muhasebe")))
                    .isInstanceOf(BusinessRuleException.class)
                    // Mesaj mevcut kaydın yazımını gösterir ki kullanıcı hangisiyle
                    // çakıştığını görebilsin.
                    .hasMessageContaining("Muhasebe");
            verify(roles, never()).save(any());
        }

        @Test
        void turkce_i_harfi_dogru_esitlenir() {
            // Varsayılan locale ile "idari".toUpperCase() = "IDARI" olur ve
            // "İdari" ile eşleşmezdi; Türkçe kuralında ikisi de "İDARİ".
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(role(9, "İdari İşler", false, null, true)));

            assertThatThrownBy(() -> service.create(request("idari işler")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("İdari İşler");
            verify(roles, never()).save(any());
        }

        @Test
        void noktasiz_i_farkli_bir_ad_sayilir() {
            // Türkçede "ı" ile "i" ayrı harflerdir; "Isıtma" ile "İsıtma" çakışmaz.
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(role(9, "İsıtma", false, null, true)));

            assertThatCode(() -> service.create(request("Isıtma"))).doesNotThrowAnyException();
        }

        @Test
        void pasif_rolun_adi_da_yeniden_kullanilamaz() {
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(role(9, "Arşiv", false, null, false)));

            assertThatThrownBy(() -> service.create(request("arşiv")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Arşiv");
        }

        @Test
        void bosluktan_ibaret_ad_reddedilir() {
            assertThatThrownBy(() -> service.create(request("   ")))
                    .isInstanceOf(BusinessRuleException.class);
            verify(roles, never()).save(any());
        }

        @Test
        void olusturma_audit_kaydi_yazar() {
            service.create(request("Mali İşler Uzmanı"));

            verify(audit).logIslem(isNull(), eq(ADMIN_ID), eq("ROLE_CREATED"), isNull(), any(),
                    isNull(), eq(true), any());
        }
    }

    @Nested
    class Guncelleme {

        private UpdateRoleRequest active(boolean value) {
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setActive(value);
            return request;
        }

        @Test
        void sistem_rolu_yeniden_adlandirilabilir_ama_system_key_degismez() {
            Role role = systemRole();
            when(roles.findByIdForUpdate(1)).thenReturn(Optional.of(role));
            when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(role));
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setName("Uzman Personel");

            RoleResponse response = service.update(1, request);

            assertThat(response.name()).isEqualTo("Uzman Personel");
            assertThat(response.systemKey()).isEqualTo("CALISAN");
            assertThat(response.system()).isTrue();
        }

        @Test
        void sistem_rolu_pasiflestirilemez() {
            when(roles.findByIdForUpdate(1)).thenReturn(Optional.of(systemRole()));

            assertThatThrownBy(() -> service.update(1, active(false)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Sistem rolü pasifleştirilemez");
            verify(users, never()).countByRole_IdAndActiveTrue(anyInt());
        }

        @Test
        void sistem_rolunun_workflow_aktorlugu_degistirilemez() {
            Role role = systemRole();
            role.setWorkflowActor(true);
            when(roles.findByIdForUpdate(1)).thenReturn(Optional.of(role));
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setWorkflowActor(false);

            assertThatThrownBy(() -> service.update(1, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("workflow aktörlüğü");
        }

        @Test
        void aktif_kullanicisi_olan_rol_pasiflestirilemez() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(dynamicRole()));
            when(users.countByRole_IdAndActiveTrue(9)).thenReturn(3L);

            assertThatThrownBy(() -> service.update(9, active(false)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("3 aktif kullanıcıda");
        }

        @Test
        void kullanicisi_olmayan_dinamik_rol_pasiflestirilir_ve_geri_acilir() {
            Role role = dynamicRole();
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(role));
            when(users.countByRole_IdAndActiveTrue(9)).thenReturn(0L);

            assertThat(service.update(9, active(false)).active()).isFalse();
            assertThat(service.update(9, active(true)).active()).isTrue();
        }

        @Test
        void bilinmeyen_rol_icin_rol_bulunamadi_hatasi_doner() {
            when(roles.findByIdForUpdate(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(404, active(true)))
                    .isInstanceOf(RoleNotFoundException.class);
        }

        @Test
        void degisiklik_yoksa_audit_kaydi_yazilmaz() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(dynamicRole()));

            assertThatCode(() -> service.update(9, active(true))).doesNotThrowAnyException();

            verify(audit, never()).logIslem(any(), any(), any(), any(), any(), any(), any(), any());
            verify(roles, never()).save(any());
        }

        @Test
        void guncelleme_audit_kaydi_onceki_ve_yeni_aktifligi_tasir() {
            when(roles.findByIdForUpdate(9)).thenReturn(Optional.of(dynamicRole()));
            when(users.countByRole_IdAndActiveTrue(9)).thenReturn(0L);

            service.update(9, active(false));

            verify(audit).logIslem(isNull(), eq(ADMIN_ID), eq("ROLE_UPDATED"), eq(9), eq(9),
                    eq(true), eq(false), any());
        }
    }
}
