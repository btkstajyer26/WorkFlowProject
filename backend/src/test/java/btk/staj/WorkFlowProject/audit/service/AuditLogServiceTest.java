package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @Test
    void logIslem_kayitOlusturuluncaRepositorySaveCagrilir() {
        auditLogService = new AuditLogService(auditLogRepository);

        UUID recordId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        auditLogService.logIslem(recordId, userId, 1, "ONAYLANDI",
                "BASKAN_INCELEMESINDE", "ONAYLANDI", "Uygun bulunmustur");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog kaydedilenLog = captor.getValue();
        assertThat(kaydedilenLog.getRecordId()).isEqualTo(recordId);
        assertThat(kaydedilenLog.getUserId()).isEqualTo(userId);
        assertThat(kaydedilenLog.getAction()).isEqualTo("ONAYLANDI");
        assertThat(kaydedilenLog.getNewStatus()).isEqualTo("ONAYLANDI");
    }
}