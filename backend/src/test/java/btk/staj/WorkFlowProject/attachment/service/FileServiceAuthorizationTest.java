package btk.staj.WorkFlowProject.attachment.service;

import static btk.staj.WorkFlowProject.support.AuthorizationFixtures.visibility;

import btk.staj.WorkFlowProject.attachment.dto.FileResponseDto;
import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.record.view.RecordContentView;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceAuthorizationTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private FileContentValidator fileContentValidator;
    @Mock
    private RecordLockValidator recordLockValidator;
    @Mock
    private RecordRepository recordRepository;
    @Mock
    private RecordAccessPolicy recordAccessPolicy;

    /**
     * Mock degil gercek: icerik gorunurlugu kurali kendi RecordAccessPolicy'si
     * uzerinden calissin. Mock'lansaydi her cagri null doner ve testler
     * kuralin dogru uygulandigini degil, yalnizca cagrildigini gosterirdi.
     */
    @Spy
    private RecordContentView recordContentView = new RecordContentView(new RecordAccessPolicy());

    @InjectMocks
    private FileService fileService;

    private UUID recordId;
    private UUID fileId;
    private UUID ownerId;
    private UUID otherUserId;
    private FileEntity fileEntity;
    private Record recordEntity;

    @BeforeEach
    void setUp() {
        recordId = UUID.randomUUID();
        fileId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        recordEntity = new Record();
        recordEntity.setId(recordId);
        recordEntity.setCreatedBy(ownerId);
        recordEntity.setStatus(RecordStatus.TASLAK);

        fileEntity = new FileEntity();
        fileEntity.setId(fileId);
        fileEntity.setRecordId(recordId);
        fileEntity.setOriginalName("test.pdf");
        fileEntity.setStoredName("stored.pdf");
        fileEntity.setMimeType("application/pdf");
    }

    @Test
    @DisplayName("Başkasının kaydındaki dosyayı indirme -> ForbiddenException")
    void downloadFile_WhenUnauthorized_ShouldThrowForbiddenException() {
        when(fileRepository.findByIdAndDeletedAtIsNull(fileId)).thenReturn(Optional.of(fileEntity));
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(recordEntity));
        doThrow(new ForbiddenException("Görüntüleme yetkisi yok"))
                .when(recordAccessPolicy).assertCanView(eq(visibility(RoleName.CALISAN, otherUserId)), eq(recordEntity));

        assertThrows(ForbiddenException.class, () ->
                fileService.downloadFile(fileId, visibility(RoleName.CALISAN, otherUserId))
        );
    }

    @Test
    @DisplayName("Başkasının kaydına upload -> ForbiddenException")
    void uploadFile_WhenNotOwner_ShouldThrowForbiddenException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        doThrow(new ForbiddenException("Yalnızca oluşturan yükleyebilir"))
                .when(recordLockValidator).assertModifyAllowed(recordId, otherUserId);

        assertThrows(ForbiddenException.class, () ->
                fileService.uploadFile(file, recordId, otherUserId)
        );
    }

    @Test
    @DisplayName("Başkasının dosyasını silme -> ForbiddenException")
    void deleteFile_WhenNotOwner_ShouldThrowForbiddenException() {
        when(fileRepository.findByIdAndDeletedAtIsNull(fileId)).thenReturn(Optional.of(fileEntity));
        doThrow(new ForbiddenException("Yalnızca oluşturan silebilir"))
                .when(recordLockValidator).assertModifyAllowed(recordId, otherUserId);

        assertThrows(ForbiddenException.class, () ->
                fileService.deleteFile(fileId, otherUserId)
        );
    }

    @Test
    @DisplayName("BASKAN_YARDIMCISI kendisine atanan kaydın dosyasını indirebiliyor")
    void downloadFile_WhenBaskanYardimcisiAssigned_ShouldSucceed() {
        recordEntity.setStatus(RecordStatus.BSK_YRD_INCELEMESINDE);
        recordEntity.setAssignedTo(otherUserId);

        when(fileRepository.findByIdAndDeletedAtIsNull(fileId)).thenReturn(Optional.of(fileEntity));
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(recordEntity));
        when(fileStorageService.loadAsResource(fileEntity.getStoredName())).thenReturn(new ByteArrayResource("pdf".getBytes()));

        assertDoesNotThrow(() ->
                fileService.downloadFile(fileId, visibility(RoleName.BASKAN_YARDIMCISI, otherUserId))
        );

        verify(recordAccessPolicy).assertCanView(visibility(RoleName.BASKAN_YARDIMCISI, otherUserId), recordEntity);
    }

    @Test
    @DisplayName("BASKAN_ONAYINDA durumundaki kayda upload -> BusinessRuleException")
    void uploadFile_WhenStatusBaskanOnayinda_ShouldThrowBusinessRuleException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        doThrow(new BusinessRuleException("Bu durumda dosya yüklenemez"))
                .when(recordLockValidator).assertModifyAllowed(recordId, ownerId);

        assertThrows(BusinessRuleException.class, () ->
                fileService.uploadFile(file, recordId, ownerId)
        );
    }

    @Test
    @DisplayName("Kaydı görebilen kullanıcı listeyi alıyor, silinmiş dosyalar listede yok")
    void listByRecord_WhenAuthorized_ShouldReturnFiles() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(recordEntity));
        when(fileRepository.findAllByRecordIdAndDeletedAtIsNull(recordId)).thenReturn(List.of(fileEntity));

        List<FileResponseDto> result = fileService.listByRecord(recordId, visibility(RoleName.CALISAN, ownerId));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(fileId, result.get(0).getId());
        verify(recordAccessPolicy).assertCanView(visibility(RoleName.CALISAN, ownerId), recordEntity);
    }

    @Test
    @DisplayName("Düzeltmedeki kayıtta Bsk. Yrd. yalnızca devir anındaki ekleri görür")
    void listByRecord_WhenRecordIsBeingCorrected_ShouldFreezeTheAttachmentList() {
        UUID deputyId = UUID.randomUUID();
        LocalDateTime handoff = LocalDateTime.of(2026, 8, 19, 10, 0);
        recordEntity.setStatus(RecordStatus.DUZENLEME_BEKLIYOR);
        recordEntity.setAssignedTo(ownerId);
        recordEntity.setSnapshotAt(handoff);
        recordEntity.setSnapshotTitle("Gönderilen başlık");

        // Devirde duran ek.
        fileEntity.setUploadedAt(handoff.minusMinutes(5));

        // Çalışanın düzeltme sırasında eklediği ek: yardımcıya görünmemeli.
        FileEntity addedDuringCorrection = new FileEntity();
        addedDuringCorrection.setId(UUID.randomUUID());
        addedDuringCorrection.setRecordId(recordId);
        addedDuringCorrection.setOriginalName("sonradan.pdf");
        addedDuringCorrection.setStoredName("sonradan-stored.pdf");
        addedDuringCorrection.setMimeType("application/pdf");
        addedDuringCorrection.setUploadedAt(handoff.plusMinutes(30));

        // Devirde duran ama sonradan silinen ek: yardımcıda hâlâ görünmeli.
        FileEntity deletedDuringCorrection = new FileEntity();
        deletedDuringCorrection.setId(UUID.randomUUID());
        deletedDuringCorrection.setRecordId(recordId);
        deletedDuringCorrection.setOriginalName("silinen.pdf");
        deletedDuringCorrection.setStoredName("silinen-stored.pdf");
        deletedDuringCorrection.setMimeType("application/pdf");
        deletedDuringCorrection.setUploadedAt(handoff.minusMinutes(10));
        deletedDuringCorrection.setDeletedAt(handoff.plusMinutes(20));

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(recordEntity));
        when(fileRepository.findAllByRecordId(recordId))
                .thenReturn(List.of(fileEntity, addedDuringCorrection, deletedDuringCorrection));

        List<FileResponseDto> result =
                fileService.listByRecord(recordId, visibility(RoleName.BASKAN_YARDIMCISI, deputyId));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "test.pdf".equals(dto.getOriginalName())));
        assertTrue(result.stream().anyMatch(dto -> "silinen.pdf".equals(dto.getOriginalName())));
        assertTrue(result.stream().noneMatch(dto -> "sonradan.pdf".equals(dto.getOriginalName())));
    }

    @Test
    @DisplayName("Göremeyen kullanıcı dosya listesi istediğinde -> ForbiddenException")
    void listByRecord_WhenUnauthorized_ShouldThrowForbiddenException() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(recordEntity));
        doThrow(new ForbiddenException("Görüntüleme yetkisi yok"))
                .when(recordAccessPolicy).assertCanView(eq(visibility(RoleName.CALISAN, otherUserId)), eq(recordEntity));

        assertThrows(ForbiddenException.class, () ->
                fileService.listByRecord(recordId, visibility(RoleName.CALISAN, otherUserId))
        );
    }
}