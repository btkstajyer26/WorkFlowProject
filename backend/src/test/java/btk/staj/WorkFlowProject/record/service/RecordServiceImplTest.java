package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.service.PermissionService;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.dto.RecordCreateRequest;
import btk.staj.WorkFlowProject.record.dto.RecordResponse;
import btk.staj.WorkFlowProject.record.dto.RecordUpdateRequest;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.mapper.RecordMapper;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private RecordMapper recordMapper;

    @Mock
    private RecordAccessPolicy recordAccessPolicy;

    @Mock
    private PermissionService permissionService;

    @Mock
    private AuditLogService auditLogService;

    private final UUID recordId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    /**
     * RecordContentView mock'lanmaz: icerik gorunurlugu kurali gercek
     * RecordAccessPolicy uzerinden calissin, boylece testler kaydin dogru
     * icerikle donduruldugunu de dogrular.
     */
    private RecordServiceImpl service() {
        return new RecordServiceImpl(recordRepository, recordMapper, recordAccessPolicy, permissionService,
                auditLogService, new RecordContentView(new RecordAccessPolicy()));
    }

    /** Verilen kullanici id/rolunu SecurityContextHolder'a giris yapmis kullanici olarak kaydeder. */
    private void girisYapmisKullaniciOlustur(UUID userId, RoleName role) {
        AuthenticatedUser authenticatedUser = mock(AuthenticatedUser.class);
        // lenient: bazi testlerde exception, getId()/getRoleName() cagrilmadan
        // once firlar (or. yetki reddi) - Mockito bu durumda mock'u "kullanilmadi"
        // diye hataya cevirir, lenient bunu engeller.
        lenient().when(authenticatedUser.getId()).thenReturn(userId);
        lenient().when(authenticatedUser.getRoleName()).thenReturn(role.name());

        var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Record ornekKayit(RecordStatus status, UUID createdBy) {
        Record record = new Record();
        record.setId(recordId);
        record.setStatus(status);
        record.setCreatedBy(createdBy);
        return record;
    }

    @AfterEach
    void securityContextTemizle() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // createRecord
    // ---------------------------------------------------------------

    @Test
    void calisanRoluOlmayanKayitOlusturamamali() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.BASKAN);
        when(permissionService.canCreateRecord(RoleName.BASKAN)).thenReturn(false);

        RecordCreateRequest request = new RecordCreateRequest();

        assertThrows(ForbiddenException.class, () -> service().createRecord(request));
        verify(recordRepository, never()).save(any());
        verify(auditLogService, never()).recordLifecycleEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void calisanKayitOlusturabilmeli() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.CALISAN);
        when(permissionService.canCreateRecord(RoleName.CALISAN)).thenReturn(true);

        RecordCreateRequest request = new RecordCreateRequest();
        Record yeniKayit = ornekKayit(RecordStatus.TASLAK, ownerId);
        when(recordMapper.toEntity(request, ownerId)).thenReturn(yeniKayit);
        when(recordRepository.save(yeniKayit)).thenReturn(yeniKayit);
        when(recordMapper.toResponse(yeniKayit)).thenReturn(new RecordResponse());

        assertNotNull(service().createRecord(request));

        verify(recordRepository).save(yeniKayit);
        verify(auditLogService).recordLifecycleEvent(
                yeniKayit.getId(),
                ownerId,
                RoleName.CALISAN,
                "RECORD_CREATED",
                RecordStatus.TASLAK,
                "Kayıt oluşturuldu.");
    }

    // ---------------------------------------------------------------
    // getRecordById
    // ---------------------------------------------------------------

    @Test
    void olmayanKayitIcinResourceNotFoundFirlatilmali() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.CALISAN);
        when(recordRepository.findById(recordId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service().getRecordById(recordId));
    }

    @Test
    void erisimPolicyReddedersegetRecordByIdForbiddenFirlatmali() {
        girisYapmisKullaniciOlustur(otherUserId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.TASLAK, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));

        doThrow(new ForbiddenException("Bu kaydı görüntüleme yetkiniz yok"))
                .when(recordAccessPolicy).assertCanView(RoleName.CALISAN, otherUserId, ownerId, null, null, RecordStatus.TASLAK);

        assertThrows(ForbiddenException.class, () -> service().getRecordById(recordId));
    }

    @Test
    void erisimPolicyIzinVerirsegetRecordByIdSonucDonmeli() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.TASLAK, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));
        when(recordMapper.toResponse(eq(kayit), any(RecordContentView.Content.class)))
                .thenReturn(new RecordResponse());

        assertNotNull(service().getRecordById(recordId));
        verify(recordAccessPolicy).assertCanView(RoleName.CALISAN, ownerId, ownerId, null, null, RecordStatus.TASLAK);
    }

    // ---------------------------------------------------------------
    // updateRecord
    // ---------------------------------------------------------------

    @Test
    void duzenlenemeyenDurumdaBusinessRuleFirlatilmali() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.ONAYLANDI, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));
        when(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.ONAYLANDI)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service().updateRecord(recordId, new RecordUpdateRequest()));

        verify(recordRepository, never()).save(any());
        verify(recordRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).recordLifecycleEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sahibiOlmayanKullaniciDuzenleyemeMeli() {
        girisYapmisKullaniciOlustur(otherUserId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.TASLAK, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));
        when(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.TASLAK)).thenReturn(true);

        assertThrows(ForbiddenException.class,
                () -> service().updateRecord(recordId, new RecordUpdateRequest()));

        verify(recordRepository, never()).save(any());
        verify(recordRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).recordLifecycleEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sahibiTaslagiDuzenleyebilmeli() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.TASLAK, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));
        when(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.TASLAK)).thenReturn(true);
        when(recordRepository.saveAndFlush(kayit)).thenReturn(kayit);
        when(recordMapper.toResponse(kayit)).thenReturn(new RecordResponse());

        RecordUpdateRequest request = new RecordUpdateRequest();
        assertNotNull(service().updateRecord(recordId, request));

        verify(recordRepository).saveAndFlush(kayit);
        verify(auditLogService).recordLifecycleEvent(
                recordId,
                ownerId,
                RoleName.CALISAN,
                "RECORD_UPDATED",
                RecordStatus.TASLAK,
                "Başlık ve kategori güncellendi.");
    }

    // ---------------------------------------------------------------
    // deleteRecord
    // ---------------------------------------------------------------

    @Test
    void taslakOlmayanKayitSilinemeMeli() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.DUZENLEME_BEKLIYOR, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));
        // DUZENLEME_BEKLIYOR duzenlemeye acik olabilir ama silmeye acik degildir.
        when(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.DUZENLEME_BEKLIYOR)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service().deleteRecord(recordId));
        verify(recordRepository, never()).save(any());
        verify(auditLogService, never()).recordLifecycleEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sahibiOlmayanKullaniciSilemeMeli() {
        girisYapmisKullaniciOlustur(otherUserId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.TASLAK, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));
        when(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.TASLAK)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> service().deleteRecord(recordId));
        verify(recordRepository, never()).save(any());
        verify(auditLogService, never()).recordLifecycleEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sahibiTaslagiSilebilmeli() {
        girisYapmisKullaniciOlustur(ownerId, RoleName.CALISAN);
        Record kayit = ornekKayit(RecordStatus.TASLAK, ownerId);
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(kayit));
        when(permissionService.canEditOrDeleteDraft(RoleName.CALISAN, RecordStatus.TASLAK)).thenReturn(true);

        service().deleteRecord(recordId);

        assertNotNull(kayit.getDeletedAt());
        verify(recordRepository).save(kayit);
        verify(auditLogService).recordLifecycleEvent(
                recordId,
                ownerId,
                RoleName.CALISAN,
                "RECORD_DELETED",
                RecordStatus.TASLAK,
                "Kayıt soft delete işlemiyle silindi.");
    }
}