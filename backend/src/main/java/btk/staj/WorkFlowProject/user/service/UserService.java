package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.dto.AdminUserSearchCriteria;
import btk.staj.WorkFlowProject.user.dto.RoleResponse;
import btk.staj.WorkFlowProject.user.dto.UserResponse;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.user.specification.UserSpecifications;
import btk.staj.WorkFlowProject.auth.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditLogService userAuditLogService;
    private final CurrentUserProvider currentUserProvider;
    private final RecordRepository recordRepository;
    private final RoleCapacityService roleCapacity;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserAuditLogService userAuditLogService,
                       CurrentUserProvider currentUserProvider,
                       RecordRepository recordRepository,
                       RoleCapacityService roleCapacity) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuditLogService = userAuditLogService;
        this.currentUserProvider = currentUserProvider;
        this.recordRepository = recordRepository;
        this.roleCapacity = roleCapacity;
    }

    /**
     * Hesap daima Calisan rolüyle acilir; baslangic rolu disaridan
     * secilemez. Diger roller yalnizca {@link #changeRole} ile atanir.
     */
    @Transactional
    public User createUser(String firstName, String lastName, String email, String rawPassword) {

        Role defaultRole = roleRepository.findBySystemKey(SystemRoleKey.CALISAN.name())
                .orElseThrow(() -> new RoleNotFoundException("Varsayılan rol (CALISAN) bulunamadı"));

        Role role = roleCapacity.lockRoles(List.of(defaultRole.getId())).get(defaultRole.getId());
        roleCapacity.assertAssignable(role);
        roleCapacity.validate(Map.of(role.getId(), role), List.of(RoleCapacityService.Change.create(role)));

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
                currentUserProvider.currentUserId(),
                "USER_CREATED",
                null,
                role.getId(),
                null,
                true,
                "Çalışan rolüyle yeni kullanıcı hesabı oluşturuldu");

        return saved;
    }

    /** Legacy API boundary: resolve a display name once, then use role identity. */
    @Transactional
    public User changeRole(UUID userId, String roleName, UUID replacementId) {
        Integer roleId = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + roleName)).getId();
        return changeRole(userId, roleId, replacementId);
    }

    /** Locks existing users first, then all affected role rows in a deterministic order. */
    @Transactional
    public User changeRole(UUID userId, Integer roleId, UUID replacementId) {
        Map<UUID, User> lockedUsers = lockUsers(userId, replacementId);
        User user = requireUser(lockedUsers, userId);
        Role previousRole = user.getRole();
        List<Integer> roleIds = new ArrayList<>();
        roleIds.add(roleId);
        lockedUsers.values().forEach(u -> roleIds.add(u.getRole().getId()));
        Map<Integer, Role> lockedRoles = roleCapacity.lockRoles(roleIds);
        Role newRole = lockedRoles.get(roleId);
        roleCapacity.assertAssignable(newRole);

        boolean leavingDeputy = SystemRoleKey.BASKAN_YARDIMCISI.matches(previousRole)
                && !SystemRoleKey.BASKAN_YARDIMCISI.matches(newRole);
        User replacement = null;
        Role replacementPreviousRole = null;
        List<RoleCapacityService.Change> changes = new ArrayList<>();
        changes.add(RoleCapacityService.Change.of(user, newRole, user.isActive()));
        if (leavingDeputy) {
            if (replacementId == null) throw new BusinessRuleException(
                    "Başkan Yardımcısı koltuğu boşalıyor; aynı istekte yerine atanacak kullanıcı (replacementBaskanYardimcisiId) belirtilmeli");
            if (replacementId.equals(userId)) throw new BusinessRuleException(
                    "Yerine atanacak kullanıcı, koltuğu boşaltan kişiyle aynı olamaz");
            replacement = requireUser(lockedUsers, replacementId);
            if (!replacement.isActive()) throw new BusinessRuleException("Pasif bir kullanıcı Başkan Yardımcısı yapılamaz");
            if (!SystemRoleKey.CALISAN.matches(replacement.getRole())) throw new BusinessRuleException(
                    "Başkan Yardımcısı yalnızca Çalışan rolündeki bir kullanıcıya devredilebilir");
            roleCapacity.assertAssignable(previousRole);
            replacementPreviousRole = replacement.getRole();
            changes.add(RoleCapacityService.Change.of(replacement, previousRole, true));
        }
        roleCapacity.validate(lockedRoles, changes);

        user.setRole(newRole);
        User saved = userRepository.save(user);
        userAuditLogService.logIslem(userId, currentUserProvider.currentUserId(), "ROLE_CHANGED",
                previousRole.getId(), newRole.getId(), null, null,
                "Kullanıcı rolü " + previousRole.getName() + " → " + newRole.getName() + " olarak değiştirildi");
        if (replacement != null) {
            replacement.setRole(previousRole);
            userRepository.save(replacement);
            kullaniciIsleriniDevret(userId, replacementId);
            userAuditLogService.logIslem(replacementId, currentUserProvider.currentUserId(), "ROLE_CHANGED",
                    replacementPreviousRole.getId(), previousRole.getId(), null, null,
                    "Başkan Yardımcısı koltuğu boşaldığı için admin tarafından atandı ve görev devri yapıldı");
        }
        return saved;
    }

    private Map<UUID, User> lockUsers(UUID userId, UUID replacementId) {
        Map<UUID, User> result = new LinkedHashMap<>();
        Stream.of(userId, replacementId).filter(Objects::nonNull).distinct().sorted()
                .forEach(id -> userRepository.findByIdForUpdate(id).ifPresent(user -> result.put(id, user)));
        return result;
    }

    private User requireUser(Map<UUID, User> users, UUID id) {
        User user = users.get(id);
        if (user == null) throw new ResourceNotFoundException("Kullanıcı bulunamadı: " + id);
        return user;
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
                currentUserProvider.currentUserId(),
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
        User user = requireUser(lockUsers(userId, null), userId);
        Map<Integer, Role> lockedRoles = roleCapacity.lockRoles(List.of(user.getRole().getId()));

        if (SystemRoleKey.ADMIN.matches(user.getRole())) {
            throw new BusinessRuleException("Admin hesabı bu ekrandan pasifleştirilemez");
        }

        if (user.isActive() == active) {
            return user;
        }

        if (!active && SystemRoleKey.BASKAN_YARDIMCISI.matches(user.getRole())) {
            throw new BusinessRuleException(
                    "Önce Başkan Yardımcısı rolünü başka bir aktif kullanıcıya devredin");
        }

        if (active) {
            roleCapacity.assertAssignable(user.getRole());
            roleCapacity.validate(lockedRoles, List.of(RoleCapacityService.Change.of(user, user.getRole(), true)));
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
                currentUserProvider.currentUserId(),
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
                .filter(Role::isActive)
                .map(RoleResponse::from)
                .toList();
    }

}
