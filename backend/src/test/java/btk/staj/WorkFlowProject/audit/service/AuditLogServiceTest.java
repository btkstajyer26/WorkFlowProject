package btk.staj.WorkFlowProject.audit.service;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.repository.AuditLogRepository;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowTransitionAudit;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Denetim izi yazimi")
class AuditLogServiceTest {

    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID ASSIGNED_TO = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final Instant PERFORMED_AT = Instant.parse("2026-08-11T09:15:00Z");

    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final AuditLogService service = new AuditLogService(auditLogRepository, roleRepository);

    @Test
    @DisplayName("onay akisinin port sozlesmesini uygular")
    void implementsTheWorkflowAuditPort() {
        assertThat(service).isInstanceOf(AuditService.class);
    }

    @Test
    @DisplayName("gecis bilgisini audit_logs satirina cevirir")
    void mapsEveryTransitionFieldOntoTheRow() {
        givenRole("BASKAN_YARDIMCISI", 2);

        service.record(transition(
                WorkflowAction.BASKANA_ILET,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                RecordStatus.BASKAN_INCELEMESINDE,
                RoleName.BASKAN_YARDIMCISI,
                "Uygun görülerek onayınıza sunulmuştur."));

        AuditLog saved = captureSaved();
        assertThat(saved.getRecordId()).isEqualTo(RECORD_ID);
        assertThat(saved.getUserId()).isEqualTo(ACTOR_ID);
        assertThat(saved.getRoleId()).isEqualTo(2);
        assertThat(saved.getAction()).isEqualTo("BASKANA_ILET");
        assertThat(saved.getPreviousStatus()).isEqualTo("BSK_YRD_INCELEMESINDE");
        assertThat(saved.getNewStatus()).isEqualTo("BASKAN_INCELEMESINDE");
        assertThat(saved.getComment()).isEqualTo("Uygun görülerek onayınıza sunulmuştur.");
    }

    @Test
    @DisplayName("islem zamani olarak gecisin gerceklestigi ani kullanir")
    void keepsTheTransitionInstantInsteadOfTheWriteTime() {
        givenRole("BASKAN", 3);

        service.record(transition(
                WorkflowAction.ONAYLA,
                RecordStatus.BASKAN_INCELEMESINDE,
                RecordStatus.ONAYLANDI,
                RoleName.BASKAN,
                null));

        LocalDateTime expected = LocalDateTime.ofInstant(PERFORMED_AT, ZoneId.systemDefault());
        assertThat(captureSaved().getCreatedAt()).isEqualTo(expected);
    }

    @Test
    @DisplayName("aciklamasiz gecisi de yazar")
    void acceptsATransitionWithoutAComment() {
        givenRole("CALISAN", 1);

        service.record(transition(
                WorkflowAction.GONDER,
                RecordStatus.TASLAK,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                RoleName.CALISAN,
                null));

        assertThat(captureSaved().getComment()).isNull();
    }

    @Test
    @DisplayName("rol adini roles tablosundaki id'ye cevirir")
    void resolvesTheRoleIdByRoleName() {
        givenRole("CALISAN", 7);

        service.record(transition(
                WorkflowAction.GONDER,
                RecordStatus.TASLAK,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                RoleName.CALISAN,
                null));

        verify(roleRepository).findByName("CALISAN");
        assertThat(captureSaved().getRoleId()).isEqualTo(7);
    }

    @Test
    @DisplayName("rol roles tablosunda yoksa yazmaz ve hata firlatir")
    void failsWithoutWritingWhenTheRoleRowIsMissing() {
        when(roleRepository.findByName("BASKAN")).thenReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> service.record(transition(
                        WorkflowAction.ONAYLA,
                        RecordStatus.BASKAN_INCELEMESINDE,
                        RecordStatus.ONAYLANDI,
                        RoleName.BASKAN,
                        null)))
                .withMessageContaining("BASKAN");

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("null gecis bilgisini reddeder")
    void rejectsANullAudit() {
        assertThatNullPointerException().isThrownBy(() -> service.record(null));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("islem gecmisini adlari cozulmus halde dondurur")
    void returnsTheHistoryWithResolvedNames() {
        AuditLogResponse row = new AuditLogResponse(
                UUID.randomUUID(), RECORD_ID, ACTOR_ID, "Ahmet Yılmaz", 1, "CALISAN",
                "GONDER", "TASLAK", "BSK_YRD_INCELEMESINDE",
                "Onayınıza sunulmuştur.", null, null, null, null, LocalDateTime.now());
        when(auditLogRepository.findHistoryByRecordId(RECORD_ID)).thenReturn(List.of(row));

        assertThat(service.getGecmis(RECORD_ID))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.userFullName()).isEqualTo("Ahmet Yılmaz");
                    assertThat(result.roleName()).isEqualTo("CALISAN");
                });
    }

    // ---------------- devre kadar kirpilmis gecmis ----------------

    @Test
    @DisplayName("geri gonderme sonrasi yapilan duzenlemeleri gecmisten cikarir")
    void hidesTheEditsMadeAfterTheRecordWasHandedBack() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 19, 9, 0);
        when(auditLogRepository.findHistoryByRecordId(RECORD_ID)).thenReturn(List.of(
                transitionRow("GONDER", "TASLAK", "BSK_YRD_INCELEMESINDE", start),
                transitionRow("CALISANA_GERI_GONDER", "BSK_YRD_INCELEMESINDE",
                        "DUZENLEME_BEKLIYOR", start.plusMinutes(10)),
                lifecycleRow("RECORD_UPDATED", "DUZENLEME_BEKLIYOR", start.plusMinutes(20)),
                lifecycleRow("RECORD_UPDATED", "DUZENLEME_BEKLIYOR", start.plusMinutes(30))));

        assertThat(service.getGecmisDevreKadar(RECORD_ID))
                .extracting(AuditLogResponse::action)
                .containsExactly("GONDER", "CALISANA_GERI_GONDER");
    }

    @Test
    @DisplayName("kirpma yalnizca son geri gondermeye gore yapilir")
    void trimsAtTheMostRecentHandoff() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 19, 9, 0);
        when(auditLogRepository.findHistoryByRecordId(RECORD_ID)).thenReturn(List.of(
                transitionRow("CALISANA_GERI_GONDER", "BSK_YRD_INCELEMESINDE",
                        "DUZENLEME_BEKLIYOR", start),
                lifecycleRow("RECORD_UPDATED", "DUZENLEME_BEKLIYOR", start.plusMinutes(5)),
                transitionRow("TEKRAR_GONDER", "DUZENLEME_BEKLIYOR",
                        "BSK_YRD_INCELEMESINDE", start.plusMinutes(10)),
                transitionRow("CALISANA_GERI_GONDER", "BSK_YRD_INCELEMESINDE",
                        "DUZENLEME_BEKLIYOR", start.plusMinutes(20)),
                lifecycleRow("RECORD_UPDATED", "DUZENLEME_BEKLIYOR", start.plusMinutes(25))));

        // Ikinci turdaki duzenleme gizlenir; kapanmis onceki turun tamami acik kalir.
        assertThat(service.getGecmisDevreKadar(RECORD_ID))
                .extracting(AuditLogResponse::action)
                .containsExactly("CALISANA_GERI_GONDER", "RECORD_UPDATED", "TEKRAR_GONDER",
                        "CALISANA_GERI_GONDER");
    }

    @Test
    @DisplayName("devir anini aciklayan gecis yoksa hicbir satir donmez")
    void returnsNothingWhenTheHandoffCannotBeLocated() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 19, 9, 0);
        when(auditLogRepository.findHistoryByRecordId(RECORD_ID)).thenReturn(List.of(
                lifecycleRow("RECORD_CREATED", "TASLAK", start),
                lifecycleRow("RECORD_UPDATED", "DUZENLEME_BEKLIYOR", start.plusMinutes(5))));

        assertThat(service.getGecmisDevreKadar(RECORD_ID)).isEmpty();
    }

    @Test
    @DisplayName("kirpilmamis gecmis butun satirlari dondurmeye devam eder")
    void theUntrimmedHistoryStillReturnsEverything() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 19, 9, 0);
        when(auditLogRepository.findHistoryByRecordId(RECORD_ID)).thenReturn(List.of(
                transitionRow("CALISANA_GERI_GONDER", "BSK_YRD_INCELEMESINDE",
                        "DUZENLEME_BEKLIYOR", start),
                lifecycleRow("RECORD_UPDATED", "DUZENLEME_BEKLIYOR", start.plusMinutes(5))));

        assertThat(service.getGecmis(RECORD_ID)).hasSize(2);
    }

    private static AuditLogResponse transitionRow(String action, String previousStatus,
                                                  String newStatus, LocalDateTime createdAt) {
        return historyRow(action, previousStatus, newStatus, createdAt);
    }

    /** Olusturma/guncelleme satiri: gecis olmadigi icin previous_status tasimaz. */
    private static AuditLogResponse lifecycleRow(String action, String currentStatus,
                                                 LocalDateTime createdAt) {
        return historyRow(action, null, currentStatus, createdAt);
    }

    private static AuditLogResponse historyRow(String action, String previousStatus,
                                               String newStatus, LocalDateTime createdAt) {
        return new AuditLogResponse(
                UUID.randomUUID(), RECORD_ID, ACTOR_ID, "Ahmet Yılmaz", 1, "CALISAN",
                action, previousStatus, newStatus, null, null, null, null, null, createdAt);
    }

    private void givenRole(String roleName, Integer roleId) {
        Role role = new Role();
        role.setId(roleId);
        role.setName(roleName);
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(role));
    }

    private AuditLog captureSaved() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static WorkflowTransitionAudit transition(WorkflowAction action,
                                                      RecordStatus previousStatus,
                                                      RecordStatus newStatus,
                                                      RoleName actorRole,
                                                      String comment) {
        return new WorkflowTransitionAudit(
                RECORD_ID, action, previousStatus, newStatus,
                ACTOR_ID, actorRole, ASSIGNED_TO, comment, PERFORMED_AT);
    }

    // ------------------------------------------------------------------
    // Kayit yasam dongusu olaylari (record modulunun cagirdigi giris noktasi)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("yasam dongusu olayini durum gecisi olmadan yazar")
    void writesALifecycleEventWithoutATransition() {
        givenRole("CALISAN", 1);

        service.recordLifecycleEvent(RECORD_ID, ACTOR_ID, RoleName.CALISAN,
                "RECORD_CREATED", RecordStatus.TASLAK, "Kayit olusturuldu");

        AuditLog saved = captureSaved();
        assertThat(saved.getRecordId()).isEqualTo(RECORD_ID);
        assertThat(saved.getUserId()).isEqualTo(ACTOR_ID);
        assertThat(saved.getRoleId()).isEqualTo(1);
        assertThat(saved.getAction()).isEqualTo("RECORD_CREATED");
        assertThat(saved.getComment()).isEqualTo("Kayit olusturuldu");
        assertThat(saved.getPreviousStatus())
                .as("yasam dongusu olayinda onceki durum yoktur")
                .isNull();
        assertThat(saved.getNewStatus())
                .as("new_status NOT NULL oldugu icin kaydin o anki durumu yazilir")
                .isEqualTo("TASLAK");
    }

    @Test
    @DisplayName("yasam dongusu olayinda rol roles tablosunda yoksa yazmaz")
    void failsWithoutWritingWhenTheLifecycleRoleRowIsMissing() {
        when(roleRepository.findByName("CALISAN")).thenReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> service.recordLifecycleEvent(
                        RECORD_ID, ACTOR_ID, RoleName.CALISAN,
                        "RECORD_CREATED", RecordStatus.TASLAK, null))
                .withMessageContaining("CALISAN");

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("admin HTTP erisimini record_id olmadan yazar")
    void writesAdminAccessWithoutARecord() {
        service.recordAccess(new RequestAccessEvent(
                "LOGIN", ACTOR_ID, 4, "ADMIN",
                "POST", "/api/auth/login", 200, "OK",
                "POST /api/auth/login → 200"));

        AuditLog saved = captureSaved();
        assertThat(saved.getRecordId()).isNull();
        assertThat(saved.getAction()).isEqualTo("LOGIN");
        assertThat(saved.getHttpStatus()).isEqualTo(200);
        assertThat(saved.getErrorCode()).isEqualTo("OK");
        assertThat(saved.getRequestPath()).isEqualTo("/api/auth/login");
    }
}
