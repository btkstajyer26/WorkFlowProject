package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.RecordStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionServiceTest {

    private final PermissionService permissionService = new PermissionService();

    @Test
    void calisanKayitOlusturabilmeli() {
        assertTrue(permissionService.canCreateRecord(Role.CALISAN));
    }

    @Test
    void baskanKayitOlusturamamali() {
        assertFalse(permissionService.canCreateRecord(Role.BASKAN));
    }

    @Test
    void baskanYardimcisiKayitOlusturamamali() {
        assertFalse(permissionService.canCreateRecord(Role.BASKAN_YARDIMCISI));
    }

    @Test
    void calisanTaslagiIncelemeyeGonderebilmeli() {
        assertTrue(permissionService.canSendToReview(Role.CALISAN, RecordStatus.TASLAK));
    }

    @Test
    void calisanOnaylanmisKaydiTekrarGonderemeMeli() {
        assertFalse(permissionService.canSendToReview(Role.CALISAN, RecordStatus.ONAYLANDI));
    }

    @Test
    void baskanNihaiOnayVerebilmeli() {
        assertTrue(permissionService.canApprove(Role.BASKAN, RecordStatus.BASKAN_INCELEMESINDE));
    }

    @Test
    void calisanNihaiOnayVeremeMeli() {
        assertFalse(permissionService.canApprove(Role.CALISAN, RecordStatus.BASKAN_INCELEMESINDE));
    }

    @Test
    void baskanYardimcisiCalisanaGeriGonderebilmeli() {
        assertTrue(permissionService.canReturnToCalisan(Role.BASKAN_YARDIMCISI));
    }

    @Test
    void calisanBaskaCalisanaGeriGonderemeMeli() {
        assertFalse(permissionService.canReturnToCalisan(Role.CALISAN));
    }

    @Test
    void calisanTasladiDuzenleyebilmeli() {
        assertTrue(permissionService.canEditOrDeleteDraft(Role.CALISAN, RecordStatus.TASLAK));
    }

    @Test
    void calisanOnaylanmisTasladiDuzenleyemeMeli() {
        assertFalse(permissionService.canEditOrDeleteDraft(Role.CALISAN, RecordStatus.ONAYLANDI));
    }

    @Test
    void calisanGeriGonderilenKaydiDuzenleyipGonderebilmeli() {
        assertTrue(permissionService.canEditAndResendReturnedRecord(Role.CALISAN, RecordStatus.DUZENLEME_BEKLIYOR));
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