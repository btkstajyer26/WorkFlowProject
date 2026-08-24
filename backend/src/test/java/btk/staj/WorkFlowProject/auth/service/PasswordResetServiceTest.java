package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.audit.RequestAuditContext;
import btk.staj.WorkFlowProject.auth.entity.PasswordResetCode;
import btk.staj.WorkFlowProject.auth.exception.InvalidResetCodeException;
import btk.staj.WorkFlowProject.auth.exception.InvalidResetTokenException;
import btk.staj.WorkFlowProject.auth.exception.PasswordReuseException;
import btk.staj.WorkFlowProject.auth.repository.PasswordResetCodeRepository;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * "Şifremi unuttum" akışının birim testleri.
 *
 * <p>Kod ve anahtar özetleri gerçekten hesaplandığı için {@link PasswordEncoder}
 * mock'u basit bir "önek + değer" kuralıyla taklit edilir; testler böylece
 * BCrypt'in maliyetine katlanmadan doğru değerin özetlendiğini doğrular.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    private static final String EMAIL = "john.doe@kurum.gov.tr";
    private static final int CODE_TTL_MINUTES = 10;
    private static final int TOKEN_TTL_MINUTES = 15;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetCodeRepository passwordResetCodeRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Mock
    private RequestAuditContext requestAuditContext;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, passwordResetCodeRepository, tokenRepository,
                passwordEncoder, mailService, requestAuditContext,
                CODE_TTL_MINUTES, TOKEN_TTL_MINUTES, RESEND_COOLDOWN_SECONDS);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail(EMAIL);
        user.setPasswordHash("hash:EskiSifre123");
        user.setActive(true);

        when(passwordEncoder.encode(anyString())).thenAnswer(call -> "hash:" + call.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(call -> ("hash:" + call.getArgument(0)).equals(call.getArgument(1)));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordResetCodeRepository.findAllByUser_IdAndConsumedAtIsNull(user.getId()))
                .thenReturn(List.of());
    }

    // ---------------- requestCode ----------------

    @Test
    @DisplayName("Aktif kullanıcı için 6 haneli kod üretilip e-postayla gönderilmeli")
    void requestCode_aktifKullanici_kodUretipMailGondermeli() {
        service.requestCode(EMAIL);

        ArgumentCaptor<PasswordResetCode> saved = ArgumentCaptor.forClass(PasswordResetCode.class);
        verify(passwordResetCodeRepository).save(saved.capture());

        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetCode(eq(EMAIL), eq("John Doe"), sentCode.capture(), eq(CODE_TTL_MINUTES));

        assertTrue(sentCode.getValue().matches("\\d{6}"), "Kod 6 haneli olmalı");
        // Kod açık değil, özetiyle saklanmalı.
        assertEquals("hash:" + sentCode.getValue(), saved.getValue().getCodeHash());
        assertNotEquals(sentCode.getValue(), saved.getValue().getCodeHash());
        assertEquals(user, saved.getValue().getUser());
        assertEquals(0, saved.getValue().getAttempts());
        assertNull(saved.getValue().getResetTokenHash());
    }

    @Test
    @DisplayName("Bilinmeyen e-posta için kod üretilmemeli ama hata da fırlatılmamalı")
    void requestCode_bilinmeyenEposta_sessizceGecmeli() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.requestCode("yok@kurum.gov.tr"));

        verify(passwordResetCodeRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Pasif hesap için kod üretilmemeli")
    void requestCode_pasifKullanici_kodUretmemeli() {
        user.setActive(false);

        service.requestCode(EMAIL);

        verify(passwordResetCodeRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Bekleme süresi dolmadan ikinci kod üretilmemeli")
    void requestCode_bekleneSuresiDolmadan_ikinciKodUretmemeli() {
        PasswordResetCode existing = openCode(LocalDateTime.now().minusSeconds(5));
        when(passwordResetCodeRepository.findAllByUser_IdAndConsumedAtIsNull(user.getId()))
                .thenReturn(List.of(existing));

        service.requestCode(EMAIL);

        verify(passwordResetCodeRepository, never()).save(any());
        verifyNoInteractions(mailService);
        assertNull(existing.getConsumedAt(), "Var olan kod geçerliliğini korumalı");
    }

    @Test
    @DisplayName("Yeni kod üretilirken kullanıcının açık kodları geçersiz kılınmalı")
    void requestCode_yeniKod_eskiKodlariTuketmeli() {
        PasswordResetCode stale = openCode(LocalDateTime.now().minusMinutes(5));
        when(passwordResetCodeRepository.findAllByUser_IdAndConsumedAtIsNull(user.getId()))
                .thenReturn(List.of(stale));

        service.requestCode(EMAIL);

        assertNotNull(stale.getConsumedAt());
        verify(passwordResetCodeRepository).save(any(PasswordResetCode.class));
    }

    // ---------------- verifyCode ----------------

    @Test
    @DisplayName("Doğru kod tek kullanımlık anahtar üretmeli")
    void verifyCode_dogruKod_anahtarUretmeli() {
        PasswordResetCode entry = openCode(LocalDateTime.now());
        entry.setCodeHash("hash:123456");
        when(passwordResetCodeRepository.findFirstByUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(entry));

        String resetToken = service.verifyCode(EMAIL, "123456");

        assertNotNull(resetToken);
        assertFalse(resetToken.isBlank());
        assertNotNull(entry.getVerifiedAt());
        assertNotNull(entry.getResetTokenExpiresAt());
        // Anahtarın kendisi değil, özeti saklanmalı.
        assertNotEquals(resetToken, entry.getResetTokenHash());
        assertEquals(64, entry.getResetTokenHash().length(), "SHA-256 onaltılık gösterimi 64 karakter");
        assertNull(entry.getConsumedAt(), "Şifre henüz değişmedi, satır tüketilmemeli");
    }

    @Test
    @DisplayName("Yanlış kod deneme sayacını artırmalı ve hata döndürmeli")
    void verifyCode_yanlisKod_denemeSayaciniArtirmali() {
        PasswordResetCode entry = openCode(LocalDateTime.now());
        entry.setCodeHash("hash:123456");
        when(passwordResetCodeRepository.findFirstByUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(entry));

        assertThrows(InvalidResetCodeException.class, () -> service.verifyCode(EMAIL, "000000"));

        assertEquals(1, entry.getAttempts());
        assertNull(entry.getConsumedAt());
    }

    @Test
    @DisplayName("Deneme sınırına ulaşan kod tüketilmeli")
    void verifyCode_denemeSiniri_koduTuketmeli() {
        PasswordResetCode entry = openCode(LocalDateTime.now());
        entry.setCodeHash("hash:123456");
        entry.setAttempts(4);
        when(passwordResetCodeRepository.findFirstByUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(entry));

        assertThrows(InvalidResetCodeException.class, () -> service.verifyCode(EMAIL, "000000"));

        assertEquals(5, entry.getAttempts());
        assertNotNull(entry.getConsumedAt(), "Sınırı aşan kod bir daha kullanılamamalı");
    }

    @Test
    @DisplayName("Süresi dolmuş kod doğru girilse bile kabul edilmemeli")
    void verifyCode_suresiDolmusKod_kabulEtmemeli() {
        PasswordResetCode entry = openCode(LocalDateTime.now().minusHours(1));
        entry.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        entry.setCodeHash("hash:123456");
        when(passwordResetCodeRepository.findFirstByUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(entry));

        assertThrows(InvalidResetCodeException.class, () -> service.verifyCode(EMAIL, "123456"));

        assertNull(entry.getResetTokenHash());
    }

    @Test
    @DisplayName("Hiç kod istenmemiş hesap için doğrulama hata vermeli")
    void verifyCode_kodYok_hataVermeli() {
        when(passwordResetCodeRepository.findFirstByUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(InvalidResetCodeException.class, () -> service.verifyCode(EMAIL, "123456"));
    }

    // ---------------- resetPassword ----------------

    @Test
    @DisplayName("Geçerli anahtarla yeni şifre özetlenerek kaydedilmeli ve oturumlar kapatılmalı")
    void resetPassword_gecerliAnahtar_sifreyiHashleyipKaydetmeli() {
        PasswordResetCode entry = verifiedCode();
        Token activeToken = new Token();
        when(passwordResetCodeRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(entry));
        when(tokenRepository.findAllByUser_IdAndRevokedFalse(user.getId())).thenReturn(List.of(activeToken));

        service.resetPassword("gecerli-anahtar", "YeniSifre123");

        assertEquals("hash:YeniSifre123", user.getPasswordHash());
        assertFalse(user.isMustChangePassword());
        assertNotNull(user.getUpdatedAt());
        assertNotNull(entry.getConsumedAt(), "Anahtar tek kullanımlık olmalı");
        assertTrue(activeToken.isRevoked(), "Şifre değişince açık oturumlar kapatılmalı");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Yeni şifre eskisiyle aynıysa reddedilmeli ve anahtar tüketilmemeli")
    void resetPassword_ayniSifre_reddetmeli() {
        PasswordResetCode entry = verifiedCode();
        when(passwordResetCodeRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(entry));

        assertThrows(PasswordReuseException.class,
                () -> service.resetPassword("gecerli-anahtar", "EskiSifre123"));

        assertEquals("hash:EskiSifre123", user.getPasswordHash());
        assertNull(entry.getConsumedAt(), "Kullanıcı başka bir şifreyle tekrar deneyebilmeli");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bilinmeyen anahtar reddedilmeli")
    void resetPassword_bilinmeyenAnahtar_reddetmeli() {
        when(passwordResetCodeRepository.findByResetTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidResetTokenException.class,
                () -> service.resetPassword("uydurma", "YeniSifre123"));
    }

    @Test
    @DisplayName("Daha önce kullanılmış anahtar ikinci kez çalışmamalı")
    void resetPassword_tuketilmisAnahtar_reddetmeli() {
        PasswordResetCode entry = verifiedCode();
        entry.setConsumedAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetCodeRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(entry));

        assertThrows(InvalidResetTokenException.class,
                () -> service.resetPassword("gecerli-anahtar", "YeniSifre123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Süresi dolmuş anahtar reddedilmeli")
    void resetPassword_suresiDolmusAnahtar_reddetmeli() {
        PasswordResetCode entry = verifiedCode();
        entry.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetCodeRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(entry));

        assertThrows(InvalidResetTokenException.class,
                () -> service.resetPassword("gecerli-anahtar", "YeniSifre123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Doğrulanmamış satırın anahtarı yoksa şifre değiştirilememeli")
    void resetPassword_dogrulanmamisSatir_reddetmeli() {
        PasswordResetCode entry = openCode(LocalDateTime.now());
        when(passwordResetCodeRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(entry));

        assertThrows(InvalidResetTokenException.class,
                () -> service.resetPassword("gecerli-anahtar", "YeniSifre123"));
    }

    private PasswordResetCode openCode(LocalDateTime createdAt) {
        PasswordResetCode entry = new PasswordResetCode();
        entry.setUser(user);
        entry.setCodeHash("hash:999999");
        entry.setCreatedAt(createdAt);
        entry.setExpiresAt(createdAt.plusMinutes(CODE_TTL_MINUTES));
        return entry;
    }

    private PasswordResetCode verifiedCode() {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode entry = openCode(now.minusMinutes(1));
        entry.setVerifiedAt(now.minusMinutes(1));
        entry.setResetTokenHash("ozet");
        entry.setResetTokenExpiresAt(now.plusMinutes(TOKEN_TTL_MINUTES));
        return entry;
    }
}
