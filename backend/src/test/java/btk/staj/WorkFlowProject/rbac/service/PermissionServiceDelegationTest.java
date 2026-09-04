package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PermissionService'in kendi kural kopyasini tutmadigini, kararlari durum
 * makinesinin gecis tablosundan okudugunu dogrular.
 *
 * <p>Biri ileride buraya sabit bir durum/rol listesi geri yazarsa bu testler
 * duser; iki kaynagin sessizce ayrismasini engeller.
 */
@DisplayName("Yetki sorgusu durum makinesine delege edilir")
class PermissionServiceDelegationTest {

    private final PermissionService permissionService =
            new PermissionService(new StaticTransitionRuleSource(WorkflowRoleFixtures.roleIds()));

    private void tumMatrisiKarsilastir(WorkflowAction action, BiPredicate<RoleName, RecordStatus> metot) {
        for (RecordStatus status : RecordStatus.values()) {
            for (RoleName role : RoleName.values()) {
                boolean tablodaVar = WorkflowRoleFixtures.rules().find(status, action, WorkflowRoleFixtures.id(role)).isPresent();

                assertThat(metot.test(role, status))
                        .as("%s / %s / %s birlesimi tabloyla ayni sonucu vermeli", action, status, role)
                        .isEqualTo(tablodaVar);
            }
        }
    }

    @Test
    @DisplayName("gonderme kararlari tabloyla birebir ortusur")
    void gonderme() {
        tumMatrisiKarsilastir(WorkflowAction.GONDER, (role, status) -> permissionService.canSendToReview(WorkflowRoleFixtures.id(role), status, AuthorizationFixtures.permissions(role)));
    }

    @Test
    @DisplayName("tekrar gonderme kararlari tabloyla birebir ortusur")
    void tekrarGonderme() {
        tumMatrisiKarsilastir(WorkflowAction.TEKRAR_GONDER, (role, status) -> permissionService.canEditAndResendReturnedRecord(WorkflowRoleFixtures.id(role), status, AuthorizationFixtures.permissions(role)));
    }

    @Test
    @DisplayName("Baskana iletme kararlari tabloyla birebir ortusur")
    void baskanaIletme() {
        tumMatrisiKarsilastir(WorkflowAction.BASKANA_ILET, (role, status) -> permissionService.canForwardToBaskan(WorkflowRoleFixtures.id(role), status, AuthorizationFixtures.permissions(role)));
    }

    @Test
    @DisplayName("onaylama kararlari tabloyla birebir ortusur")
    void onaylama() {
        tumMatrisiKarsilastir(WorkflowAction.ONAYLA, (role, status) -> permissionService.canApprove(WorkflowRoleFixtures.id(role), status, AuthorizationFixtures.permissions(role)));
    }

    @Test
    @DisplayName("reddetme kararlari tabloyla birebir ortusur")
    void reddetme() {
        tumMatrisiKarsilastir(WorkflowAction.REDDET, (role, status) -> permissionService.canReject(WorkflowRoleFixtures.id(role), status, AuthorizationFixtures.permissions(role)));
    }

    @Test
    @DisplayName("Calisana geri gonderme kararlari tabloyla birebir ortusur")
    void calisanaGeriGonderme() {
        tumMatrisiKarsilastir(WorkflowAction.CALISANA_GERI_GONDER, (role, status) -> permissionService.canReturnToCalisan(WorkflowRoleFixtures.id(role), status, AuthorizationFixtures.permissions(role)));
    }

    @Test
    @DisplayName("Baskan Yardimcisina geri gonderme kararlari tabloyla birebir ortusur")
    void baskanYrdGeriGonderme() {
        tumMatrisiKarsilastir(
                WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, (role, status) -> permissionService.canReturnToBaskanYrd(WorkflowRoleFixtures.id(role), status, AuthorizationFixtures.permissions(role)));
    }

    // ---------- Sartnameden dogrudan gelen beklentiler ----------

    @Test
    @DisplayName("Baskan, kayit Bsk. Yrd. incelemesindeyken onaylayamaz")
    void baskanErkenOnaylayamaz() {
        assertThat(permissionService.canApprove(WorkflowRoleFixtures.id(RoleName.BASKAN), RecordStatus.BSK_YRD_INCELEMESINDE, AuthorizationFixtures.permissions(RoleName.BASKAN)))
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(RecordStatus.class)
    @DisplayName("Calisan hicbir durumda onaylayamaz")
    void calisanHicbirDurumdaOnaylayamaz(RecordStatus status) {
        assertThat(permissionService.canApprove(WorkflowRoleFixtures.id(RoleName.CALISAN), status, AuthorizationFixtures.permissions(RoleName.CALISAN))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RecordStatus.class)
    @DisplayName("ADMIN hicbir is akisi aksiyonu yapamaz")
    void adminIsAkisiAksiyonuYapamaz(RecordStatus status) {
        assertThat(permissionService.canApprove(WorkflowRoleFixtures.id(RoleName.ADMIN), status, AuthorizationFixtures.permissions(RoleName.ADMIN))).isFalse();
        assertThat(permissionService.canReject(WorkflowRoleFixtures.id(RoleName.ADMIN), status, AuthorizationFixtures.permissions(RoleName.ADMIN))).isFalse();
        assertThat(permissionService.canSendToReview(WorkflowRoleFixtures.id(RoleName.ADMIN), status, AuthorizationFixtures.permissions(RoleName.ADMIN))).isFalse();
        assertThat(permissionService.canForwardToBaskan(WorkflowRoleFixtures.id(RoleName.ADMIN), status, AuthorizationFixtures.permissions(RoleName.ADMIN))).isFalse();
        assertThat(permissionService.canCreateRecord(AuthorizationFixtures.permissions(RoleName.ADMIN))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = RecordStatus.class, names = {"ONAYLANDI", "REDDEDILDI"})
    @DisplayName("terminal durumda hicbir rol hicbir gecis yapamaz")
    void terminalDurumdaGecisYok(RecordStatus terminal) {
        assertThat(permissionService.isRecordLocked(terminal)).isTrue();

        for (RoleName role : RoleName.values()) {
            assertThat(permissionService.canSendToReview(WorkflowRoleFixtures.id(role), terminal, AuthorizationFixtures.permissions(role))).isFalse();
            assertThat(permissionService.canApprove(WorkflowRoleFixtures.id(role), terminal, AuthorizationFixtures.permissions(role))).isFalse();
            assertThat(permissionService.canReject(WorkflowRoleFixtures.id(role), terminal, AuthorizationFixtures.permissions(role))).isFalse();
            assertThat(permissionService.canReturnToCalisan(WorkflowRoleFixtures.id(role), terminal, AuthorizationFixtures.permissions(role))).isFalse();
            assertThat(permissionService.canEditRecord(AuthorizationFixtures.permissions(role), terminal)).isFalse();
        }
    }

    @Test
    @DisplayName("aciklama zorunlulugu aksiyonun kendi tanimindan okunur")
    void aciklamaZorunlulugu() {
        for (WorkflowAction action : WorkflowAction.values()) {
            assertThat(permissionService.isCommentRequired(action))
                    .as("%s icin aciklama zorunlulugu", action)
                    .isEqualTo(action.isCommentRequired());
        }
    }
}
