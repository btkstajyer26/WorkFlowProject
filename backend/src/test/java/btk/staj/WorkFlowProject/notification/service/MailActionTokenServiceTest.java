package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.dto.MailActionPreview;
import btk.staj.WorkFlowProject.notification.entity.MailActionToken;
import btk.staj.WorkFlowProject.notification.exception.InvalidMailActionTokenException;
import btk.staj.WorkFlowProject.notification.repository.MailActionTokenRepository;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailActionTokenServiceTest {

    private static final int TTL_HOURS = 72;

    @Mock
    private MailActionTokenRepository mailActionTokenRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private WorkflowActionService workflowActionService;

    private MailActionTokenService service() {
        return new MailActionTokenService(
                mailActionTokenRepository, recordRepository, workflowActionService, TTL_HOURS);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- Alinacak aksiyonun durumdan turetilmesi ---

    @Test
    @DisplayName("her calisilabilir durum icin tek bir birincil aksiyon vardir")
    void primaryActionFor_calisilabilirDurumlar() {
        assertThat(MailActionTokenService.primaryActionFor(RecordStatus.BSK_YRD_INCELEMESINDE))
                .contains(WorkflowAction.BASKANA_ILET);
        assertThat(MailActionTokenService.primaryActionFor(RecordStatus.BASKAN_INCELEMESINDE))
                .contains(WorkflowAction.ONAYLA);
        assertThat(MailActionTokenService.primaryActionFor(RecordStatus.DUZENLEME_BEKLIYOR))
                .contains(WorkflowAction.TEKRAR_GONDER);
    }

    @Test
    @DisplayName("terminal ve taslak durumlarda hizli aksiyon onerilmez")
    void primaryActionFor_aksiyonsuzDurumlar() {
        assertThat(MailActionTokenService.primaryActionFor(RecordStatus.TASLAK)).isEmpty();
        assertThat(MailActionTokenService.primaryActionFor(RecordStatus.ONAYLANDI)).isEmpty();
        assertThat(MailActionTokenService.primaryActionFor(RecordStatus.REDDEDILDI)).isEmpty();
        assertThat(MailActionTokenService.primaryActionFor(null)).isEmpty();
    }

    // --- Uretim ---

    @Test
    @DisplayName("uretilen ham anahtar saklanmaz, yalniz SHA-256 ozeti yazilir")
    void issue_hamAnahtarSaklanmaz() {
        UUID recordId = UUID.randomUUID();
        User user = user(UUID.randomUUID());

        String rawToken = service().issue(recordId, user, WorkflowAction.ONAYLA);

        ArgumentCaptor<MailActionToken> captor = ArgumentCaptor.forClass(MailActionToken.class);
        verify(mailActionTokenRepository).save(captor.capture());
        MailActionToken saved = captor.getValue();

        assertThat(saved.getTokenHash())
                .isEqualTo(sha256(rawToken))
                .isNotEqualTo(rawToken)
                .hasSize(64);
        assertThat(saved.getRecordId()).isEqualTo(recordId);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getAction()).isEqualTo("ONAYLA");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(TTL_HOURS - 1));
    }

    @Test
    @DisplayName("yeni anahtar verilmeden once ayni evrak/kisi icin acik anahtarlar kapatilir")
    void issue_eskiAnahtarlariKapatir() {
        UUID recordId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        service().issue(recordId, user(userId), WorkflowAction.BASKANA_ILET);

        verify(mailActionTokenRepository).consumeOpenTokens(eq(recordId), eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("ayni cagri iki kez yapildiginda farkli anahtarlar uretilir")
    void issue_anahtarlarTekrarlanmaz() {
        UUID recordId = UUID.randomUUID();
        User user = user(UUID.randomUUID());
        MailActionTokenService service = service();

        assertThat(service.issue(recordId, user, WorkflowAction.ONAYLA))
                .isNotEqualTo(service.issue(recordId, user, WorkflowAction.ONAYLA));
    }

    // --- Onizleme ---

    @Test
    @DisplayName("onizleme anahtari tuketmez")
    void preview_anahtariTuketmez() {
        UUID recordId = UUID.randomUUID();
        MailActionToken entry = openToken(recordId, user(UUID.randomUUID()), WorkflowAction.ONAYLA);
        when(mailActionTokenRepository.findByTokenHash(sha256("ham-anahtar"))).thenReturn(Optional.of(entry));
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record(recordId)));

        MailActionPreview preview = service().preview("ham-anahtar");

        assertThat(preview.recordId()).isEqualTo(recordId);
        assertThat(preview.recordTitle()).isEqualTo("Bütçe teklifi");
        assertThat(preview.action()).isEqualTo("ONAYLA");
        assertThat(entry.getConsumedAt()).isNull();
        verify(mailActionTokenRepository, never()).save(any());
        verifyNoInteractions(workflowActionService);
    }

    // --- Tuketim ---

    @Test
    @DisplayName("gecerli anahtar tuketilir ve aksiyon anahtarin sahibi adina yurutulur")
    void consume_aksiyonuSahibiAdinaYurutur() {
        UUID recordId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User owner = user(ownerId);
        MailActionToken entry = openToken(recordId, owner, WorkflowAction.BASKANA_ILET);
        when(mailActionTokenRepository.findByTokenHash(sha256("ham-anahtar"))).thenReturn(Optional.of(entry));

        UUID result = service().consume("ham-anahtar");

        assertThat(result).isEqualTo(recordId);
        assertThat(entry.getConsumedAt()).isNotNull();
        verify(mailActionTokenRepository).save(entry);

        ArgumentCaptor<WorkflowActionRequest> captor = ArgumentCaptor.forClass(WorkflowActionRequest.class);
        verify(workflowActionService).performAction(eq(recordId), captor.capture());
        assertThat(captor.getValue().action()).isEqualTo(WorkflowAction.BASKANA_ILET);
        // Hedef kullanici hicbir aksiyonda istemciden gelmez; backend cozer.
        assertThat(captor.getValue().targetUserId()).isNull();
    }

    @Test
    @DisplayName("aksiyon yurutuldukten sonra onceki guvenlik baglami geri konur")
    void consume_guvenlikBaglaminiSizdirmaz() {
        UUID recordId = UUID.randomUUID();
        MailActionToken entry = openToken(recordId, user(UUID.randomUUID()), WorkflowAction.ONAYLA);
        when(mailActionTokenRepository.findByTokenHash(sha256("ham-anahtar"))).thenReturn(Optional.of(entry));

        service().consume("ham-anahtar");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("durum makinesi gecisi reddederse hata yutulmaz")
    void consume_durumMakinesiHatasiYutulmaz() {
        UUID recordId = UUID.randomUUID();
        MailActionToken entry = openToken(recordId, user(UUID.randomUUID()), WorkflowAction.ONAYLA);
        when(mailActionTokenRepository.findByTokenHash(sha256("ham-anahtar"))).thenReturn(Optional.of(entry));
        when(workflowActionService.performAction(eq(recordId), any()))
                .thenThrow(new IllegalStateException("WORKFLOW_INVALID_TRANSITION"));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> service().consume("ham-anahtar"));

        // Baglam yine de temizlenmeli; aksi halde sonraki istege sizardi.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("tuketilmis anahtar ikinci kez kullanilamaz")
    void consume_tuketilmisAnahtarReddedilir() {
        MailActionToken entry = openToken(UUID.randomUUID(), user(UUID.randomUUID()), WorkflowAction.ONAYLA);
        entry.setConsumedAt(LocalDateTime.now().minusMinutes(1));
        when(mailActionTokenRepository.findByTokenHash(sha256("ham-anahtar"))).thenReturn(Optional.of(entry));

        assertThatExceptionOfType(InvalidMailActionTokenException.class)
                .isThrownBy(() -> service().consume("ham-anahtar"));

        verifyNoInteractions(workflowActionService);
    }

    @Test
    @DisplayName("suresi dolmus anahtar kullanilamaz")
    void consume_suresiDolmusAnahtarReddedilir() {
        MailActionToken entry = openToken(UUID.randomUUID(), user(UUID.randomUUID()), WorkflowAction.ONAYLA);
        entry.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(mailActionTokenRepository.findByTokenHash(sha256("ham-anahtar"))).thenReturn(Optional.of(entry));

        assertThatExceptionOfType(InvalidMailActionTokenException.class)
                .isThrownBy(() -> service().consume("ham-anahtar"));

        verifyNoInteractions(workflowActionService);
    }

    @Test
    @DisplayName("bilinmeyen anahtar kullanilamaz")
    void consume_bilinmeyenAnahtarReddedilir() {
        when(mailActionTokenRepository.findByTokenHash(sha256("uydurma"))).thenReturn(Optional.empty());

        assertThatExceptionOfType(InvalidMailActionTokenException.class)
                .isThrownBy(() -> service().consume("uydurma"));

        verifyNoInteractions(workflowActionService);
    }

    @Test
    @DisplayName("bos anahtar depoya hic sorulmaz")
    void consume_bosAnahtarReddedilir() {
        MailActionTokenService service = service();

        assertThatExceptionOfType(InvalidMailActionTokenException.class)
                .isThrownBy(() -> service.consume("   "));
        assertThatExceptionOfType(InvalidMailActionTokenException.class)
                .isThrownBy(() -> service.consume(null));

        verify(mailActionTokenRepository, never()).findByTokenHash(any());
        verifyNoInteractions(workflowActionService);
    }

    @Test
    @DisplayName("bulunamama, sure dolmasi ve tuketilmislik ayni mesajla doner")
    void consume_hataMesajiAyrimYapmaz() {
        MailActionToken tuketilmis = openToken(UUID.randomUUID(), user(UUID.randomUUID()), WorkflowAction.ONAYLA);
        tuketilmis.setConsumedAt(LocalDateTime.now());
        when(mailActionTokenRepository.findByTokenHash(sha256("a"))).thenReturn(Optional.of(tuketilmis));
        when(mailActionTokenRepository.findByTokenHash(sha256("b"))).thenReturn(Optional.empty());

        MailActionTokenService service = service();
        String tuketilmisMesaj = catchMessage(() -> service.consume("a"));
        String bulunamayanMesaj = catchMessage(() -> service.consume("b"));

        assertThat(tuketilmisMesaj).isEqualTo(bulunamayanMesaj);
    }

    // --- Yardimcilar ---

    private static String catchMessage(Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("İstisna bekleniyordu");
        } catch (InvalidMailActionTokenException exception) {
            return exception.getMessage();
        }
    }

    private static User user(UUID id) {
        Role role = new Role();
        role.setId(3);
        role.setName("BASKAN");

        User user = new User();
        user.setId(id);
        user.setFirstName("Ayşe");
        user.setLastName("Kaya");
        user.setEmail("ayse@ornek.test");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setRole(role);
        return user;
    }

    private static Record record(UUID recordId) {
        Record record = new Record();
        record.setId(recordId);
        record.setTitle("Bütçe teklifi");
        record.setStatus(RecordStatus.BASKAN_INCELEMESINDE);
        return record;
    }

    private static MailActionToken openToken(UUID recordId, User user, WorkflowAction action) {
        return MailActionToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(sha256("ham-anahtar"))
                .recordId(recordId)
                .user(user)
                .action(action.name())
                .expiresAt(LocalDateTime.now().plusHours(TTL_HOURS))
                .build();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
