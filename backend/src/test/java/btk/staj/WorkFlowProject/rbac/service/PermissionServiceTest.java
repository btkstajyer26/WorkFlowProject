package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionServiceTest {

    private final PermissionService permissionService =
            new PermissionService(new StaticTransitionRuleSource(WorkflowRoleFixtures.roleIds()));

    @Test
    void calisanKayitOlusturabilmeli() {
        assertTrue(permissionService.canCreateRecord(AuthorizationFixtures.permissions(RoleName.CALISAN)));
    }

    @Test
    void baskanKayitOlusturamamali() {
        assertFalse(permissionService.canCreateRecord(AuthorizationFixtures.permissions(RoleName.BASKAN)));
    }

    @Test
    void baskanYardimcisiKayitOlusturamamali() {
        assertFalse(permissionService.canCreateRecord(AuthorizationFixtures.permissions(RoleName.BASKAN_YARDIMCISI)));
    }

    @Test
    void calisanTaslagiIncelemeyeGonderebilmeli() {
        assertTrue(permissionService.canSendToReview(WorkflowRoleFixtures.id(RoleName.CALISAN), RecordStatus.TASLAK, AuthorizationFixtures.permissions(RoleName.CALISAN)));
    }

    @Test
    void calisanOnaylanmisKaydiTekrarGonderemeMeli() {
        assertFalse(permissionService.canSendToReview(WorkflowRoleFixtures.id(RoleName.CALISAN), RecordStatus.ONAYLANDI, AuthorizationFixtures.permissions(RoleName.CALISAN)));
    }

    @Test
    void baskanNihaiOnayVerebilmeli() {
        assertTrue(permissionService.canApprove(WorkflowRoleFixtures.id(RoleName.BASKAN), RecordStatus.BASKAN_INCELEMESINDE, AuthorizationFixtures.permissions(RoleName.BASKAN)));
    }

    @Test
    void calisanNihaiOnayVeremeMeli() {
        assertFalse(permissionService.canApprove(WorkflowRoleFixtures.id(RoleName.CALISAN), RecordStatus.BASKAN_INCELEMESINDE, AuthorizationFixtures.permissions(RoleName.CALISAN)));
    }

    @Test
    void baskanYardimcisiCalisanaGeriGonderebilmeli() {
        assertTrue(permissionService.canReturnToCalisan(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), AuthorizationFixtures.permissions(RoleName.BASKAN_YARDIMCISI)));
    }

    @Test
    void calisanBaskaCalisanaGeriGonderemeMeli() {
        assertFalse(permissionService.canReturnToCalisan(WorkflowRoleFixtures.id(RoleName.CALISAN), AuthorizationFixtures.permissions(RoleName.CALISAN)));
    }

    @Test
    void calisanTasladiDuzenleyebilmeli() {
        assertTrue(permissionService.canEditRecord(AuthorizationFixtures.permissions(RoleName.CALISAN), RecordStatus.TASLAK));
    }

    @Test
    void calisanOnaylanmisTasladiDuzenleyemeMeli() {
        assertFalse(permissionService.canEditRecord(AuthorizationFixtures.permissions(RoleName.CALISAN), RecordStatus.ONAYLANDI));
    }

    @Test
    void calisanGeriGonderilenKaydiDuzenleyipGonderebilmeli() {
        assertTrue(permissionService.canEditAndResendReturnedRecord(WorkflowRoleFixtures.id(RoleName.CALISAN), RecordStatus.DUZENLEME_BEKLIYOR, AuthorizationFixtures.permissions(RoleName.CALISAN)));
    }

    @Test
    void bosAciklamaGeriGondermeyeYetmeMeli() {
        assertFalse(permissionService.isCommentRequiredForReturn("   "));
    }

    @Test
    void doluAciklamaGeriGondermeyeYetmeli() {
        assertTrue(permissionService.isCommentRequiredForReturn("Evrakta eksik bilgi var"));
    }

    @Test
    void onaylanmisKayitKilitliOlmali() {
        assertTrue(permissionService.isRecordLocked(RecordStatus.ONAYLANDI));
    }

    @Test
    void taslakKayitKilitliOlmamali() {
        assertFalse(permissionService.isRecordLocked(RecordStatus.TASLAK));
    }
}