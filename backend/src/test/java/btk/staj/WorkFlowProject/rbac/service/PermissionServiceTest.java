package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.rbac.RoleName;
import btk.staj.WorkFlowProject.rbac.RecordStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionServiceTest {

    private final PermissionService permissionService = new PermissionService();

    @Test
    void calisanKayitOlusturabilmeli() {
        assertTrue(permissionService.canCreateRecord(RoleName.CALISAN));
    }

    @Test
    void baskanKayitOlusturamamali() {
        assertFalse(permissionService.canCreateRecord(RoleName.BASKAN));
    }

    @Test
    void baskanYardimcisiKayitOlusturamamali() {
        assertFalse(permissionService.canCreateRecord(RoleName.BASKAN_YARDIMCISI));
    }

    @Test
    void calisanTaslagiIncelemeyeGonderebilmeli() {
        assertTrue(permissionService.canSendToReview(RoleName.CALISAN, RecordStatus.TASLAK));
    }

    @Test
    void calisanOnaylanmisKaydiTekrarGonderemeMeli() {
        assertFalse(permissionService.canSendToReview(RoleName.CALISAN, RecordStatus.ONAYLANDI));
    }

    @Test
    void baskanNihaiOnayVerebilmeli() {
        assertTrue(permissionService.canApprove(RoleName.BASKAN, RecordStatus.BASKAN_INCELEMESINDE));
    }

    @Test
    void calisanNihaiOnayVeremeMeli() {
        assertFalse(permissionService.canApprove(RoleName.CALISAN, RecordStatus.BASKAN_INCELEMESINDE));
    }

    @Test
    void baskanYardimcisiCalisanaGeriGonderebilmeli() {
        assertTrue(permissionService.canReturnToCalisan(RoleName.BASKAN_YARDIMCISI));
    }

    @Test
    void calisanBaskaCalisanaGeriGonderemeMeli() {
        assertFalse(permissionService.canReturnToCalisan(RoleName.CALISAN));
    }

    @Test
    void calisanTasladiDuzenleyebilmeli() {
        assertTrue(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.TASLAK));
    }

    @Test
    void calisanOnaylanmisTasladiDuzenleyemeMeli() {
        assertFalse(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.ONAYLANDI));
    }

    @Test
    void calisanGeriGonderilenKaydiDuzenleyipGonderebilmeli() {
        assertTrue(permissionService.canEditAndResendReturnedRecord(RoleName.CALISAN, RecordStatus.DUZENLEME_BEKLIYOR));
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