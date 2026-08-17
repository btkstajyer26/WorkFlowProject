package btk.staj.WorkFlowProject.attachment.service;

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
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

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
                .when(recordAccessPolicy).assertCanView(eq(RoleName.CALISAN), eq(otherUserId), eq(ownerId), any(), eq(RecordStatus.TASLAK));

        assertThrows(ForbiddenException.class, () ->
                fileService.downloadFile(fileId, RoleName.CALISAN, otherUserId)
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
                fileService.downloadFile(fileId, RoleName.BASKAN_YARDIMCISI, otherUserId)
        );

        verify(recordAccessPolicy).assertCanView(RoleName.BASKAN_YARDIMCISI, otherUserId, ownerId, otherUserId, RecordStatus.BSK_YRD_INCELEMESINDE);
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

        List<FileResponseDto> result = fileService.listByRecord(recordId, RoleName.CALISAN, ownerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(fileId, result.get(0).getId());
        verify(recordAccessPolicy).assertCanView(RoleName.CALISAN, ownerId, ownerId, null, RecordStatus.TASLAK);
    }

    @Test
    @DisplayName("Göremeyen kullanıcı dosya listesi istediğinde -> ForbiddenException")
    void listByRecord_WhenUnauthorized_ShouldThrowForbiddenException() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(recordEntity));
        doThrow(new ForbiddenException("Görüntüleme yetkisi yok"))
                .when(recordAccessPolicy).assertCanView(eq(RoleName.CALISAN), eq(otherUserId), eq(ownerId), any(), eq(RecordStatus.TASLAK));

        assertThrows(ForbiddenException.class, () ->
                fileService.listByRecord(recordId, RoleName.CALISAN, otherUserId)
        );
    }
}