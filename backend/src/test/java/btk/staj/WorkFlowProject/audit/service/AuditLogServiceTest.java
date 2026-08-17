package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AuditLogService auditLogService;

    @Test
    void logIslem_kayitOlusturuluncaRepositorySaveCagrilir() {
        auditLogService = new AuditLogService(auditLogRepository, jdbcTemplate);

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

    @Test
    void recordLifecycleEvent_dogruAuditLogSatiriniKurar() {
        auditLogService = new AuditLogService(auditLogRepository, jdbcTemplate);

        UUID recordId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(jdbcTemplate.queryForObject(
                any(String.class), eq(Integer.class), eq("CALISAN")))
                .thenReturn(1);

        auditLogService.recordLifecycleEvent(recordId, actorId, RoleName.CALISAN,
                "RECORD_CREATED", RecordStatus.TASLAK, "Kayit olusturuldu");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog kaydedilenLog = captor.getValue();
        assertThat(kaydedilenLog.getRecordId()).isEqualTo(recordId);
        assertThat(kaydedilenLog.getUserId()).isEqualTo(actorId);
        assertThat(kaydedilenLog.getAction()).isEqualTo("RECORD_CREATED");
        assertThat(kaydedilenLog.getPreviousStatus()).isNull();
        assertThat(kaydedilenLog.getNewStatus()).isEqualTo("TASLAK");
    }

    @Test
    void recordLifecycleEvent_rolBulunamazsaIllegalStateExceptionFirlatir() {
        auditLogService = new AuditLogService(auditLogRepository, jdbcTemplate);

        UUID recordId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(jdbcTemplate.queryForObject(
                any(String.class), eq(Integer.class), eq("CALISAN")))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(() -> auditLogService.recordLifecycleEvent(
                recordId, actorId, RoleName.CALISAN,
                "RECORD_CREATED", RecordStatus.TASLAK, "Kayit olusturuldu"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CALISAN");
    }
}