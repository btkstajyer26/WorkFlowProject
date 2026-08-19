package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.audit.RequestAuditContext;
import btk.staj.WorkFlowProject.auth.entity.PasswordResetCode;
import btk.staj.WorkFlowProject.auth.exception.InvalidResetCodeException;
import btk.staj.WorkFlowProject.auth.exception.InvalidResetTokenException;
import btk.staj.WorkFlowProject.auth.exception.PasswordReuseException;
import btk.staj.WorkFlowProject.auth.repository.PasswordResetCodeRepository;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * "Şifremi unuttum" akışı: e-postaya 6 haneli kod gönderilir, kod doğrulanınca
 * tek kullanımlık bir anahtar verilir, şifre o anahtarla değiştirilir.
 *
 * <p>Akış oturum gerektirmediği için üç uç da herkese açıktır; kötüye kullanımı
 * sınırlayan önlemler burada toplanmıştır:
 * <ul>
 *   <li>Kod isteği hesabın varlığını sızdırmaz, her e-posta aynı cevabı alır.</li>
 *   <li>Aynı hesap için kısa aralıkla ikinci kod üretilmez (posta bombardımanı).</li>
 *   <li>Kod {@value #MAX_ATTEMPTS} yanlış denemeden sonra ölür (kaba kuvvet).</li>
 *   <li>Şifre değişince hesabın tüm refresh token'ları iptal edilir.</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** Kodun üst sınırı hariç tutulur: 000000-999999 arası altı hane. */
    private static final int CODE_BOUND = 1_000_000;

    private static final int MAX_ATTEMPTS = 5;

    private static final int RESET_TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Kod da anahtar da doğrulanamadığında dışarıya giden tek mesaj. */
    private static final String INVALID_CODE_MESSAGE = "Doğrulama kodu geçersiz veya süresi dolmuş";

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final RequestAuditContext requestAuditContext;
    private final int codeTtlMinutes;
    private final int tokenTtlMinutes;
    private final int resendCooldownSeconds;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetCodeRepository passwordResetCodeRepository,
                                TokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                RequestAuditContext requestAuditContext,
                                @Value("${app.password-reset.code-ttl-minutes:10}") int codeTtlMinutes,
                                @Value("${app.password-reset.token-ttl-minutes:15}") int tokenTtlMinutes,
                                @Value("${app.password-reset.resend-cooldown-seconds:60}") int resendCooldownSeconds) {
        this.userRepository = userRepository;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.requestAuditContext = requestAuditContext;
        this.codeTtlMinutes = codeTtlMinutes;
        this.tokenTtlMinutes = tokenTtlMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    /**
     * Hesap varsa yeni bir kod üretip e-postayla gönderir.
     *
     * <p>Hiçbir durumda hata fırlatmaz: bilinmeyen adres ile kayıtlı adres aynı
     * cevabı almalı, yoksa uç bir hesap listeleme aracına dönüşür.
     */
    @Transactional
    public void requestCode(String email) {
        LocalDateTime now = LocalDateTime.now();
        User user = findUserByEmail(email).orElse(null);

        if (user == null || !user.isActive()) {
            // Cevap aynı kalır; iz yalnızca sunucu tarafında bırakılır.
            requestAuditContext.mark("PASSWORD_RESET_REQUEST_IGNORED", user);
            return;
        }

        List<PasswordResetCode> openCodes =
                passwordResetCodeRepository.findAllByUser_IdAndConsumedAtIsNull(user.getId());

        boolean sentRecently = openCodes.stream().anyMatch(code ->
                code.getCreatedAt().isAfter(now.minusSeconds(resendCooldownSeconds)));
        if (sentRecently) {
            // Bekleme süresi dolmadan ikinci kod üretilirse hem kullanıcının
            // gelen kutusu dolar hem de hangi kodun geçerli olduğu karışır.
            requestAuditContext.mark("PASSWORD_RESET_REQUEST_THROTTLED", user);
            return;
        }

        // Aynı anda tek kod geçerli olsun: eskiler tüketilmiş sayılır.
        openCodes.forEach(code -> code.setConsumedAt(now));

        String code = generateCode();
        PasswordResetCode entry = new PasswordResetCode();
        entry.setUser(user);
        entry.setCodeHash(passwordEncoder.encode(code));
        entry.setCreatedAt(now);
        entry.setExpiresAt(now.plusMinutes(codeTtlMinutes));
        passwordResetCodeRepository.save(entry);

        mailService.sendPasswordResetCode(user.getEmail(), displayName(user), code, codeTtlMinutes);
        requestAuditContext.mark("PASSWORD_RESET_REQUESTED", user);
    }

    /**
     * Kodu doğrular ve şifre değiştirme ekranına taşınacak tek kullanımlık
     * anahtarı döndürür.
     *
     * <p>{@code noRollbackFor}: yanlış kod denemesi sayacı, istisna fırlatılsa
     * bile veritabanına yazılmalıdır; aksi halde deneme sınırı hiç işlemez.
     */
    @Transactional(noRollbackFor = InvalidResetCodeException.class)
    public String verifyCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode entry = findUserByEmail(email)
                .filter(User::isActive)
                .flatMap(user -> passwordResetCodeRepository
                        .findFirstByUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .orElseThrow(() -> new InvalidResetCodeException(INVALID_CODE_MESSAGE));

        if (entry.getExpiresAt().isBefore(now) || entry.getAttempts() >= MAX_ATTEMPTS) {
            throw new InvalidResetCodeException(INVALID_CODE_MESSAGE);
        }

        if (!passwordEncoder.matches(code, entry.getCodeHash())) {
            entry.setAttempts(entry.getAttempts() + 1);
            if (entry.getAttempts() >= MAX_ATTEMPTS) {
                entry.setConsumedAt(now);
                log.warn("Şifre sıfırlama kodu deneme sınırına takıldı: kullanıcı {}", entry.getUser().getId());
            }
            requestAuditContext.mark("PASSWORD_RESET_CODE_FAILED", entry.getUser());
            throw new InvalidResetCodeException(INVALID_CODE_MESSAGE);
        }

        String resetToken = generateResetToken();
        entry.setResetTokenHash(sha256(resetToken));
        entry.setResetTokenExpiresAt(now.plusMinutes(tokenTtlMinutes));
        entry.setVerifiedAt(now);
        // Kod doğrulandı ama şifre henüz değişmedi: satır tüketilmez, tüketim
        // resetPassword'da olur. Yanlış deneme sayacı da sıfırlanmaz.

        requestAuditContext.mark("PASSWORD_RESET_CODE_VERIFIED", entry.getUser());
        return resetToken;
    }

    /** Doğrulanmış anahtarın kalan ömrü; arayüz geri sayım gösterebilsin diye. */
    public long tokenTtlSeconds() {
        return Duration.ofMinutes(tokenTtlMinutes).toSeconds();
    }

    /**
     * Şifreyi değiştirir. Yeni şifre mevcut şifreyle aynı olamaz; aynıysa
     * anahtar tüketilmez ve kullanıcı başka bir şifreyle tekrar deneyebilir.
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode entry = passwordResetCodeRepository.findByResetTokenHash(sha256(resetToken))
                .orElseThrow(() -> new InvalidResetTokenException(
                        "Şifre sıfırlama anahtarı geçersiz veya süresi dolmuş"));

        if (entry.getConsumedAt() != null
                || entry.getResetTokenExpiresAt() == null
                || entry.getResetTokenExpiresAt().isBefore(now)) {
            throw new InvalidResetTokenException("Şifre sıfırlama anahtarı geçersiz veya süresi dolmuş");
        }

        User user = entry.getUser();
        if (!user.isActive()) {
            throw new InvalidResetTokenException("Şifre sıfırlama anahtarı geçersiz veya süresi dolmuş");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new PasswordReuseException("Yeni şifreniz mevcut şifrenizle aynı olamaz");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Sıfırlama, yönetici tarafından atanmış geçici şifrenin yerine de
        // geçer; kullanıcıdan bir kez daha değiştirmesi istenmez.
        user.setMustChangePassword(false);
        user.setUpdatedAt(now);
        userRepository.save(user);

        entry.setConsumedAt(now);

        // Şifre başkasının eline geçmiş olabilir: açık oturumlar kapatılır.
        tokenRepository.findAllByUser_IdAndRevokedFalse(user.getId())
                .forEach(token -> token.setRevoked(true));

        requestAuditContext.mark("PASSWORD_RESET_COMPLETED", user);
    }

    /**
     * E-posta adresi kullanıcıdan geldiği için önce yazıldığı gibi, bulunamazsa
     * küçük harfe indirilmiş hâliyle aranır.
     */
    private Optional<User> findUserByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        String trimmed = email.trim();
        return userRepository.findByEmail(trimmed)
                .or(() -> userRepository.findByEmail(trimmed.toLowerCase(Locale.ROOT)));
    }

    private static String displayName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }

    /** Baştaki sıfırlar korunmalı: 4213 değil 004213. */
    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(CODE_BOUND));
    }

    private static String generateResetToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Anahtar veritabanında özetiyle aranır. 256 bit rastgelelik kaba kuvvete
     * kapalı olduğu için tuzsuz SHA-256 yeterli; BCrypt kullanılsaydı özet her
     * seferinde farklı olacağı için satır sorguyla bulunamazdı.
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 bulunamadı", e);
        }
    }
}
