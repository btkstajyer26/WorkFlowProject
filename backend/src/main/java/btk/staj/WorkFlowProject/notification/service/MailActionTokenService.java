package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.notification.dto.MailActionPreview;
import btk.staj.WorkFlowProject.notification.entity.MailActionToken;
import btk.staj.WorkFlowProject.notification.exception.InvalidMailActionTokenException;
import btk.staj.WorkFlowProject.notification.repository.MailActionTokenRepository;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * E-posta bildirimindeki tek tikla aksiyon baglantisinin uretimi ve tuketimi.
 *
 * <p>Akis bilerek iki adimdir:
 * <ul>
 *   <li>{@link #preview(String)} anahtari <strong>degistirmeden</strong> okur.
 *       Kullaniciya "neyi onaylamak uzeresiniz" ekrani bundan cizilir. Posta
 *       tarayicilari ve baglanti onizleyicileri baglantiyi kendiliginden
 *       getirdigi icin bu adim durum degistirmemek zorundadir.</li>
 *   <li>{@link #consume(String)} anahtari tuketir ve aksiyonu yurutur.
 *       Yalnizca kullanicinin acik onayiyla cagrilir.</li>
 * </ul>
 *
 * <p>Anahtar bir kimlik bilgisidir; aktor anahtardan cozulur. Evragin o anki
 * {@code assignedTo} alanindan <em>turetilmez</em> — turetilseydi anahtar,
 * koltuk devredildikten sonra yeni kisinin adina is yapardi.
 *
 * <p>Anahtar yetkiyi tek basina vermez: {@link #consume(String)} gercek durum
 * makinesini yeniden calistirir. Evrak arada ilerlediyse gecis oradan
 * reddedilir ve anahtarin tuketimi de geri alinir (ayni transaction).
 */
@Service
public class MailActionTokenService {

    private static final Logger log = LoggerFactory.getLogger(MailActionTokenService.class);

    /** 256 bit; sozlukten tahmin edilemez, SHA-256 ozeti carpismasiz sayilir. */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Anahtar gecersiz, suresi dolmus veya tuketilmis olabilir; disariya giden
     * mesaj her uc durumda ayni. Ayrim yapmak, gecerli bir anahtarin varligini
     * dogrulayan bir kanal acardi.
     */
    private static final String INVALID_TOKEN_MESSAGE =
            "Bağlantı geçersiz, süresi dolmuş veya daha önce kullanılmış";

    private final MailActionTokenRepository mailActionTokenRepository;
    private final RecordRepository recordRepository;
    private final WorkflowActionService workflowActionService;
    private final int ttlHours;

    public MailActionTokenService(MailActionTokenRepository mailActionTokenRepository,
                                  RecordRepository recordRepository,
                                  WorkflowActionService workflowActionService,
                                  @Value("${app.mail-action-token-ttl-hours:72}") int ttlHours) {
        this.mailActionTokenRepository = Objects.requireNonNull(mailActionTokenRepository, "mailActionTokenRepository");
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
        this.workflowActionService = Objects.requireNonNull(workflowActionService, "workflowActionService");
        this.ttlHours = ttlHours;
    }

    /**
     * Alicinin evragin yeni durumunda alabilecegi birincil aksiyon.
     *
     * <p>Rolden degil durumdan turetilir: bildirim zaten yalnizca evragin
     * atandigi kisiye gider, dolayisiyla durum aliciyi tek bir aksiyona
     * baglar. Terminal durumlarda ({@code ONAYLANDI}, {@code REDDEDILDI})
     * alinacak aksiyon yoktur.
     *
     * <p>Geri gonderme ve ret bilerek disaridadir: ikisinde de aciklama
     * zorunludur, tek tikla yapilamaz.
     */
    public static Optional<WorkflowAction> primaryActionFor(RecordStatus status) {
        if (status == null) {
            return Optional.empty();
        }
        return switch (status) {
            case BSK_YRD_INCELEMESINDE -> Optional.of(WorkflowAction.BASKANA_ILET);
            case BASKAN_INCELEMESINDE -> Optional.of(WorkflowAction.ONAYLA);
            case DUZENLEME_BEKLIYOR -> Optional.of(WorkflowAction.TEKRAR_GONDER);
            case TASLAK, ONAYLANDI, REDDEDILDI -> Optional.empty();
        };
    }

    /**
     * Yeni anahtar uretir ve <strong>ham</strong> degerini dondurur; cagiran
     * onu yalnizca e-posta govdesine koyar. Ham deger hicbir yerde saklanmaz
     * ve loglanmaz.
     *
     * <p>Ayni evrak/kisi icin acik kalmis eski anahtarlar once kapatilir.
     */
    @Transactional
    public String issue(UUID recordId, User user, WorkflowAction action) {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(action, "action");

        LocalDateTime now = LocalDateTime.now();
        mailActionTokenRepository.consumeOpenTokens(recordId, user.getId(), now);

        String rawToken = generateToken();
        mailActionTokenRepository.save(MailActionToken.builder()
                .tokenHash(sha256(rawToken))
                .recordId(recordId)
                .user(user)
                .action(action.name())
                .expiresAt(now.plusHours(ttlHours))
                .build());

        return rawToken;
    }

    /** Anahtarin gecerlilik suresi; arayuz "bu baglanti N saat gecerli" yazabilsin diye. */
    public int ttlHours() {
        return ttlHours;
    }

    /**
     * Anahtari <strong>degistirmeden</strong> okur ve onay ekraninin
     * ihtiyaci olan bilgiyi dondurur.
     */
    @Transactional(readOnly = true)
    public MailActionPreview preview(String rawToken) {
        MailActionToken entry = requireUsableToken(rawToken);
        Record record = recordRepository.findById(entry.getRecordId())
                .orElseThrow(() -> new InvalidMailActionTokenException(INVALID_TOKEN_MESSAGE));

        return new MailActionPreview(
                entry.getRecordId(),
                record.getTitle(),
                record.getStatus() == null ? null : record.getStatus().name(),
                entry.getAction(),
                fullName(entry.getUser()),
                entry.getExpiresAt());
    }

    /**
     * Anahtari tuketir ve aksiyonu anahtarin sahibi adina yurutur.
     *
     * <p>Tuketim aksiyondan <em>once</em> yazilir: aksiyon durum makinesinden
     * donerse transaction geri alinir ve anahtar tuketilmemis kalir. Aksiyon
     * basariliysa ikisi birlikte kalicilasir. Boylece "tuketildi ama is
     * yapilmadi" ya da "is yapildi ama anahtar acik kaldi" araligi olusmaz.
     */
    @Transactional
    public UUID consume(String rawToken) {
        MailActionToken entry = requireUsableToken(rawToken);

        entry.setConsumedAt(LocalDateTime.now());
        mailActionTokenRepository.save(entry);

        WorkflowAction action = parseAction(entry.getAction());
        User actor = entry.getUser();

        // Durum makinesi aktoru SecurityContext'ten okur. Anahtar dogrulandigi
        // icin burada kimlik yazmak, JWT filtresinin yaptigi isin ayni yetkiye
        // dayanan esdegeridir; taklit degildir. Onceki baglam korunur ve
        // finally'de aynen geri konur.
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            AuthenticatedUser principal = new AuthenticatedUser(actor);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            workflowActionService.performAction(entry.getRecordId(), new WorkflowActionRequest(
                    action,
                    null,
                    "E-posta bağlantısı üzerinden onaylandı."));
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }

        log.info("E-posta bağlantısıyla workflow aksiyonu yürütüldü. Evrak: {}, Aksiyon: {}, Aktör: {}",
                entry.getRecordId(), action, actor.getId());
        return entry.getRecordId();
    }

    /**
     * Anahtari bulur ve uc sinirin ucunu de dogrular. Bulunamama, sure dolmasi
     * ve tuketilmis olma ayni istisnayla doner.
     */
    private MailActionToken requireUsableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidMailActionTokenException(INVALID_TOKEN_MESSAGE);
        }

        MailActionToken entry = mailActionTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new InvalidMailActionTokenException(INVALID_TOKEN_MESSAGE));

        if (entry.getConsumedAt() != null) {
            throw new InvalidMailActionTokenException(INVALID_TOKEN_MESSAGE);
        }
        if (entry.getExpiresAt() == null || entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidMailActionTokenException(INVALID_TOKEN_MESSAGE);
        }
        return entry;
    }

    private static WorkflowAction parseAction(String action) {
        try {
            return WorkflowAction.valueOf(action);
        } catch (IllegalArgumentException exception) {
            // Satir elle veya eski bir surumle yazilmis olabilir; disariya yine
            // ayni mesaj gider.
            throw new InvalidMailActionTokenException(INVALID_TOKEN_MESSAGE);
        }
    }

    private static String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        return (first + " " + last).trim();
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algoritması bulunamadı", exception);
        }
    }
}
