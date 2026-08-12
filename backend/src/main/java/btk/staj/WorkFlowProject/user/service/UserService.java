package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Role;
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
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditLogService userAuditLogService;
    private final CurrentActorProvider currentActorProvider;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserAuditLogService userAuditLogService,
                       CurrentActorProvider currentActorProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuditLogService = userAuditLogService;
        this.currentActorProvider = currentActorProvider;
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
     * Kurumsal gorevlendirme degistiginde rolu gunceller. Sistemde tek bir
     * aktif Admin bulunur; ikinci Admin atamasi reddedilir.
     */
    @Transactional
    public User changeRole(UUID userId, String newRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + userId));

        Role previousRole = user.getRole();

        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + newRoleName));

        if (newRole.getName().equals("ADMIN")) {
            boolean baskaAdminVar = userRepository.findByRole_NameAndActive("ADMIN", true).stream()
                    .anyMatch(mevcut -> !mevcut.getId().equals(userId));
            if (baskaAdminVar) {
                throw new AdminLimitExceededException("Sistemde zaten bir ADMIN var, ikinci ADMIN atanamaz");
            }
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

        return saved;
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
     * pasiflestirilemez; Baskan Yardimcisi de rolu once devredilmeden
     * pasiflestirilemez (aksi halde o rol bosta kalir). Pasiflestirmede
     * kullanicinin aktif refresh token'lari da iptal edilir; erisim
     * token'lari zaten her istekte veritabanindaki guncel duruma bakildigi
     * icin (JwtAuthenticationFilter) ayrica islem gerektirmez.
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

    /**
     * Sistemde tanimli tum roller (id sirasiyla); Admin'in bir kullaniciya
     * atayabilecegi rol seceneklerini olusturur.
     */
    public List<RoleResponse> listAssignableRoles() {
        return roleRepository.findAllByOrderByIdAsc().stream()
                .map(RoleResponse::from)
                .toList();
    }
}