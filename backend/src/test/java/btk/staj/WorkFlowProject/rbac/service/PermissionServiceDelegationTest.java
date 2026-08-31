package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRules;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.BiPredicate;

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
            new PermissionService(new StaticTransitionRuleSource());

    private void tumMatrisiKarsilastir(WorkflowAction action, BiPredicate<RoleName, RecordStatus> metot) {
        for (RecordStatus status : RecordStatus.values()) {
            for (RoleName role : RoleName.values()) {
                boolean tablodaVar = TransitionRules.find(status, action, role).isPresent();

                assertThat(metot.test(role, status))
                        .as("%s / %s / %s birlesimi tabloyla ayni sonucu vermeli", action, status, role)
                        .isEqualTo(tablodaVar);
            }
        }
    }

    @Test
    @DisplayName("gonderme kararlari tabloyla birebir ortusur")
    void gonderme() {
        tumMatrisiKarsilastir(WorkflowAction.GONDER, permissionService::canSendToReview);
    }

    @Test
    @DisplayName("tekrar gonderme kararlari tabloyla birebir ortusur")
    void tekrarGonderme() {
        tumMatrisiKarsilastir(WorkflowAction.TEKRAR_GONDER, permissionService::canEditAndResendReturnedRecord);
    }

    @Test
    @DisplayName("Baskana iletme kararlari tabloyla birebir ortusur")
    void baskanaIletme() {
        tumMatrisiKarsilastir(WorkflowAction.BASKANA_ILET, permissionService::canForwardToBaskan);
    }

    @Test
    @DisplayName("onaylama kararlari tabloyla birebir ortusur")
    void onaylama() {
        tumMatrisiKarsilastir(WorkflowAction.ONAYLA, permissionService::canApprove);
    }

    @Test
    @DisplayName("reddetme kararlari tabloyla birebir ortusur")
    void reddetme() {
        tumMatrisiKarsilastir(WorkflowAction.REDDET, permissionService::canReject);
    }

    @Test
    @DisplayName("Calisana geri gonderme kararlari tabloyla birebir ortusur")
    void calisanaGeriGonderme() {
        tumMatrisiKarsilastir(WorkflowAction.CALISANA_GERI_GONDER, permissionService::canReturnToCalisan);
    }

    @Test
    @DisplayName("Baskan Yardimcisina geri gonderme kararlari tabloyla birebir ortusur")
    void baskanYrdGeriGonderme() {
        tumMatrisiKarsilastir(
                WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, permissionService::canReturnToBaskanYrd);
    }

    // ---------- Sartnameden dogrudan gelen beklentiler ----------

    @Test
    @DisplayName("Baskan, kayit Bsk. Yrd. incelemesindeyken onaylayamaz")
    void baskanErkenOnaylayamaz() {
        assertThat(permissionService.canApprove(RoleName.BASKAN, RecordStatus.BSK_YRD_INCELEMESINDE))
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(RecordStatus.class)
    @DisplayName("Calisan hicbir durumda onaylayamaz")
    void calisanHicbirDurumdaOnaylayamaz(RecordStatus status) {
        assertThat(permissionService.canApprove(RoleName.CALISAN, status)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RecordStatus.class)
    @DisplayName("ADMIN hicbir is akisi aksiyonu yapamaz")
    void adminIsAkisiAksiyonuYapamaz(RecordStatus status) {
        assertThat(permissionService.canApprove(RoleName.ADMIN, status)).isFalse();
        assertThat(permissionService.canReject(RoleName.ADMIN, status)).isFalse();
        assertThat(permissionService.canSendToReview(RoleName.ADMIN, status)).isFalse();
        assertThat(permissionService.canForwardToBaskan(RoleName.ADMIN, status)).isFalse();
        assertThat(permissionService.canCreateRecord(RoleName.ADMIN)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = RecordStatus.class, names = {"ONAYLANDI", "REDDEDILDI"})
    @DisplayName("terminal durumda hicbir rol hicbir gecis yapamaz")
    void terminalDurumdaGecisYok(RecordStatus terminal) {
        assertThat(permissionService.isRecordLocked(terminal)).isTrue();

        for (RoleName role : RoleName.values()) {
            assertThat(permissionService.canSendToReview(role, terminal)).isFalse();
            assertThat(permissionService.canApprove(role, terminal)).isFalse();
            assertThat(permissionService.canReject(role, terminal)).isFalse();
            assertThat(permissionService.canReturnToCalisan(role, terminal)).isFalse();
            assertThat(permissionService.canEditOrDeleteDraft(role, terminal)).isFalse();
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
