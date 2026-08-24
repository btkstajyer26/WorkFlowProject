package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.dto.AdminUserSearchCriteria;
import btk.staj.WorkFlowProject.user.dto.RoleResponse;
import btk.staj.WorkFlowProject.user.dto.UserResponse;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.user.specification.UserSpecifications;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Set<String> SINGLETON_ROLES = Set.of("ADMIN", "BASKAN", "BASKAN_YARDIMCISI");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditLogService userAuditLogService;
    private final CurrentActorProvider currentActorProvider;
    private final RecordRepository recordRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserAuditLogService userAuditLogService,
                       CurrentActorProvider currentActorProvider,
                       RecordRepository recordRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuditLogService = userAuditLogService;
        this.currentActorProvider = currentActorProvider;
        this.recordRepository = recordRepository;
    }

    /**
     * Hesap daima Calisan rolüyle acilir; baslangic rolu disaridan
     * secilemez. Diger roller yalnizca {@link #changeRole} ile atanir.
     */
    @Transactional
    public User createUser(String firstName, String lastName, String email, String rawPassword) {

        Role role = roleRepository.findByName("CALISAN")
                .orElseThrow(() -> new RoleNotFoundException("Varsayılan rol (CALISAN) bulunamadı"));

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        userAuditLogService.logIslem(
                saved.getId(),
                currentActorProvider.currentActor().id(),
                "USER_CREATED",
                null,
                role.getId(),
                null,
                true,
                "Çalışan rolüyle yeni kullanıcı hesabı oluşturuldu");

        return saved;
    }

    /**
     * Kurumsal gorevlendirme degistiginde rolu gunceller.
     *
     * <p>ADMIN, BASKAN ve BASKAN_YARDIMCISI rolleri "tekil" roller: ayni anda
     * yalnizca bir kisi tutabilir. Bu rollerden birine atama yapilirken, o rol
     * zaten baska bir kullaniciya aitse istek reddedilir.
     *
     * <p>Bir kullanici BASKAN_YARDIMCISI rolundeyken baska bir tekil role
     * (BASKAN veya ADMIN) geciyorsa, BASKAN_YARDIMCISI koltugu bosalir.
     * Bu durumda istekte {@code replacementBaskanYardimcisiId} zorunludur;
     * belirtilen kullanici ayni istekte yeni BASKAN_YARDIMCISI yapilir.
     * Otomatik/rastgele atama yapilmaz — devir Admin'in acikca sectigi
     * kullaniciya, ayni transaction icinde uygulanir. Eski yardimcinin
     * uzerindeki kayitlar da yeni yardimciya atanir.
     */
    @Transactional
    public User changeRole(UUID userId, String newRoleName, UUID replacementBaskanYardimcisiId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + userId));

        Role previousRole = user.getRole();

        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + newRoleName));

        ensureSingletonRoleAvailable(newRole.getName(), userId);

        boolean wasBaskanYardimcisi = previousRole != null && "BASKAN_YARDIMCISI".equals(previousRole.getName());
        boolean leavingBaskanYardimcisi = wasBaskanYardimcisi && !"BASKAN_YARDIMCISI".equals(newRole.getName());

        if (leavingBaskanYardimcisi && replacementBaskanYardimcisiId == null) {
            throw new BusinessRuleException(
                    "Başkan Yardımcısı koltuğu boşalıyor; aynı istekte yerine atanacak kullanıcı (replacementBaskanYardimcisiId) belirtilmeli");
        }

        user.setRole(newRole);
        User saved = userRepository.save(user);

        userAuditLogService.logIslem(
                userId,
                currentActorProvider.currentActor().id(),
                "ROLE_CHANGED",
                previousRole != null ? previousRole.getId() : null,
                newRole.getId(),
                null,
                null,
                "Kullanıcı rolü " + (previousRole != null ? previousRole.getName() : "?")
                        + " → " + newRole.getName() + " olarak değiştirildi");

        if (leavingBaskanYardimcisi) {
            assignBaskanYardimcisi(replacementBaskanYardimcisiId, userId);
        }

        return saved;
    }

    private void assignBaskanYardimcisi(UUID replacementUserId, UUID previousHolderId) {
        User replacement = userRepository.findById(replacementUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Belirtilen kullanıcı bulunamadı: " + replacementUserId));

        if (replacement.getId().equals(previousHolderId)) {
            throw new BusinessRuleException("Yerine atanacak kullanıcı, koltuğu boşaltan kişiyle aynı olamaz");
        }
        if (!replacement.isActive()) {
            throw new BusinessRuleException("Pasif bir kullanıcı Başkan Yardımcısı yapılamaz");
        }

        if (!"CALISAN".equals(replacement.getRole().getName())) {
            throw new BusinessRuleException(
                    "Başkan Yardımcısı yalnızca Çalışan rolündeki bir kullanıcıya devredilebilir");
        }

        Role baskanYardimcisiRole = roleRepository.findByName("BASKAN_YARDIMCISI")
                .orElseThrow(() -> new RoleNotFoundException("BASKAN_YARDIMCISI rolü bulunamadı"));

        replacement.setRole(baskanYardimcisiRole);
        userRepository.save(replacement);

        // --- YENİ EKLENEN KISIM: OTOMATİK GÖREV DEVRİ ---
        // Başkan Yardımcısı değiştiğinde eski Bşk. Yrd. üzerindeki tüm kayıtları yenisine devrediyoruz.
        kullaniciIsleriniDevret(previousHolderId, replacementUserId);

        userAuditLogService.logIslem(
                replacement.getId(),
                currentActorProvider.currentActor().id(),
                "ROLE_CHANGED",
                null,
                baskanYardimcisiRole.getId(),
                null,
                null,
                "Başkan Yardımcısı koltuğu boşaldığı için admin tarafından atandı ve görev devri yapıldı");
    }

    /**
     * Eski kullanıcının üzerindeki tüm evrakları yeni kullanıcıya devreder.
     * Bu metot otomatik olarak çalışabileceği gibi, Admin API'si üzerinden Başkan değişimi
     * gibi senaryolarda manuel (REST ucuyla) de tetiklenebilir.
     *
     * NOT (İş M5 fix): Sadece assigned_to devri yeterli değil. "last_deputy_id"
     * alanı eski kullanıcıda kalırsa, kayıt üzerinde BASKAN_YARDIMCISINA_GERI_GONDER
     * işlemi eski (artık Bşk. Yrd. olmayan) kullanıcıyı hedeflemeye çalışıp patlıyor.
     * Bu yüzden last_deputy_id de aynı işlemde yeni kullanıcıya güncelleniyor.
     */
    @Transactional
    public void kullaniciIsleriniDevret(UUID eskiKullaniciId, UUID yeniKullaniciId) {
        int devredilenSayi = recordRepository.devretBekleyenIsleri(eskiKullaniciId, yeniKullaniciId);
        int lastDeputyGuncellenenSayi = recordRepository.updateLastDeputyId(eskiKullaniciId, yeniKullaniciId);

        userAuditLogService.logIslem(
                yeniKullaniciId,
                currentActorProvider.currentActor().id(),
                "TASKS_REASSIGNED",
                null,
                null,
                null,
                null,
                devredilenSayi + " adet bekleyen evrak başarıyla yeni kullanıcıya devredildi ("
                        + lastDeputyGuncellenenSayi + " kayıtta last_deputy_id güncellendi).");
    }

    /**
     * Admin kullanici listesi: arama (ad/soyad/e-posta), rol ve aktiflik
     * filtreleri ile sayfali sonuc. Filtreler bos birakilirsa etkisizdir.
     */
    public PagedResponse<UserResponse> searchUsers(AdminUserSearchCriteria criteria, Pageable pageable) {
        Page<User> userPage = userRepository.findAll(UserSpecifications.withFilters(criteria), pageable);

        return new PagedResponse<>(
                userPage.getContent().stream().map(UserResponse::from).toList(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages());
    }

    /**
     * Hesap etkinlestirme/pasiflestirme. Admin hesabi bu yoldan
     * pasiflestirilemez; Baskan Yardimcisi de rolu once {@link #changeRole}
     * ile devredilmeden pasiflestirilemez (aksi halde o rol bosta kalir).
     * Pasiflestirmede kullanicinin aktif refresh token'lari da iptal edilir.
     */
    @Transactional
    public User setActive(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + userId));

        if ("ADMIN".equals(user.getRole().getName())) {
            throw new BusinessRuleException("Admin hesabı bu ekrandan pasifleştirilemez");
        }

        if (user.isActive() == active) {
            return user;
        }

        if (!active && "BASKAN_YARDIMCISI".equals(user.getRole().getName())) {
            throw new BusinessRuleException(
                    "Önce Başkan Yardımcısı rolünü başka bir aktif kullanıcıya devredin");
        }

        if (active) {
            ensureSingletonRoleAvailable(user.getRole().getName(), userId);
        }

        boolean previousActive = user.isActive();
        user.setActive(active);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        if (!active) {
            tokenRepository.findAllByUser_IdAndRevokedFalse(userId)
                    .forEach(token -> token.setRevoked(true));
        }

        userAuditLogService.logIslem(
                userId,
                currentActorProvider.currentActor().id(),
                active ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED",
                null,
                null,
                previousActive,
                active,
                active
                        ? "Kullanıcının sisteme erişimi yeniden açıldı"
                        : "Kullanıcının sisteme erişimi kapatıldı");

        return saved;
    }

    public List<RoleResponse> listAssignableRoles() {
        return roleRepository.findAllByOrderByIdAsc().stream()
                .map(RoleResponse::from)
                .toList();
    }

    /**
     * {@code roleName} tekil bir rolse (bkz. {@link #SINGLETON_ROLES}), o
     * rolde {@code excludingUserId} disinda aktif baska bir kullanici
     * olmadigini dogrular. Hem yeni rol atamada ({@link #changeRole}) hem
     * de pasif bir tekil rol sahibini yeniden aktiflestirirken
     * ({@link #setActive}) kullanilir — ikisi de ayni invariant'i korur:
     * bir tekil rolde ayni anda en fazla bir aktif kullanici olabilir.
     */
    private void ensureSingletonRoleAvailable(String roleName, UUID excludingUserId) {
        if (!SINGLETON_ROLES.contains(roleName)) {
            return;
        }

        boolean alreadyHeldByAnother = userRepository.findByRole_NameAndActive(roleName, true)
                .stream()
                .anyMatch(existing -> !existing.getId().equals(excludingUserId));

        if (alreadyHeldByAnother) {
            throw new AdminLimitExceededException(
                    "Bu rol zaten başka bir kullanıcıya atanmış: " + roleName);
        }
    }
}