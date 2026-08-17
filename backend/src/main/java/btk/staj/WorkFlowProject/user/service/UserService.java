package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.repository.RecordRepository; // Yeni eklendi
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
    private final RecordRepository recordRepository; // Yeni eklendi

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserAuditLogService userAuditLogService,
                       CurrentActorProvider currentActorProvider,
                       RecordRepository recordRepository) { // Constructor'a eklendi
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuditLogService = userAuditLogService;
        this.currentActorProvider = currentActorProvider;
        this.recordRepository = recordRepository; // Ataması yapıldı
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

    @Transactional
    public User changeRole(UUID userId, String newRoleName, UUID replacementBaskanYardimcisiId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + userId));

        Role previousRole = user.getRole();

        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + newRoleName));

        if (SINGLETON_ROLES.contains(newRole.getName())) {
            boolean alreadyHeldByAnother = userRepository.findByRole_NameAndActive(newRole.getName(), true)
                    .stream()
                    .anyMatch(existing -> !existing.getId().equals(userId));
            if (alreadyHeldByAnother) {
                throw new AdminLimitExceededException(
                        "Bu rol zaten başka bir kullanıcıya atanmış: " + newRole.getName());
            }
        }

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
     */
    @Transactional
    public void kullaniciIsleriniDevret(UUID eskiKullaniciId, UUID yeniKullaniciId) {
        int devredilenSayi = recordRepository.devretBekleyenIsleri(eskiKullaniciId, yeniKullaniciId);
        
        userAuditLogService.logIslem(
                yeniKullaniciId,
                currentActorProvider.currentActor().id(),
                "TASKS_REASSIGNED",
                null,
                null,
                null,
                null,
                devredilenSayi + " adet bekleyen evrak başarıyla yeni kullanıcıya devredildi.");
    }

    public PagedResponse<UserResponse> searchUsers(AdminUserSearchCriteria criteria, Pageable pageable) {
        Page<User> userPage = userRepository.findAll(UserSpecifications.withFilters(criteria), pageable);

        return new PagedResponse<>(
                userPage.getContent().stream().map(UserResponse::from).toList(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages());
    }

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
}