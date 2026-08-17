package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link UserService#changeRole} ve {@link UserService#setActive} icin birim
 * testleri. Baskan Yardimcisi "tekil rol" kuralina ve koltuk devrine
 * odaklanir; bu davranis daha once dokumante edilmis bir test bosluguydu.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID ADMIN_ACTOR_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserAuditLogService userAuditLogService;
    @Mock
    private CurrentActorProvider currentActorProvider;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, roleRepository, tokenRepository,
                passwordEncoder, userAuditLogService, currentActorProvider);
        lenient().when(currentActorProvider.currentActor())
                .thenReturn(new CurrentActor(ADMIN_ACTOR_ID, RoleName.ADMIN));
    }

    private static Role role(Integer id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private static User user(UUID id, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    // ---------------- changeRole: tekil rol kurali ----------------

    @Test
    @DisplayName("bir rol baskasina aitken ikinci kez atanamaz")
    void changeRole_tekilRolBaskasindaysaReddedilir() {
        UUID targetId = UUID.randomUUID();
        Role calisan = role(1, "CALISAN");
        Role baskan = role(3, "BASKAN");
        User target = user(targetId, calisan, true);
        User existingBaskan = user(UUID.randomUUID(), baskan, true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByName("BASKAN")).thenReturn(Optional.of(baskan));
        when(userRepository.findByRole_NameAndActive("BASKAN", true)).thenReturn(List.of(existingBaskan));

        assertThatExceptionOfType(AdminLimitExceededException.class)
                .isThrownBy(() -> userService.changeRole(targetId, "BASKAN", null));

        verify(userRepository, org.mockito.Mockito.never()).save(target);
    }

    @Test
    @DisplayName("kendi uzerindeki tekil role yeniden atama celiski sayilmaz")
    void changeRole_ayniKullaniciyaTekrarAtamaCelismezSayilmaz() {
        UUID targetId = UUID.randomUUID();
        Role baskan = role(3, "BASKAN");
        User target = user(targetId, baskan, true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByName("BASKAN")).thenReturn(Optional.of(baskan));
        when(userRepository.findByRole_NameAndActive("BASKAN", true)).thenReturn(List.of(target));
        when(userRepository.save(target)).thenReturn(target);

        User result = userService.changeRole(targetId, "BASKAN", null);

        assertThat(result.getRole().getName()).isEqualTo("BASKAN");
    }

    // ---------------- changeRole: Baskan Yardimcisi koltuk devri ----------------

    @Test
    @DisplayName("Baskan Yardimcisi baska tekil role gecerken replacement zorunlu")
    void changeRole_bskYrdKoltuguBosalirkenReplacementZorunlu() {
        UUID targetId = UUID.randomUUID();
        Role bskYrd = role(2, "BASKAN_YARDIMCISI");
        Role baskan = role(3, "BASKAN");
        User target = user(targetId, bskYrd, true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByName("BASKAN")).thenReturn(Optional.of(baskan));
        when(userRepository.findByRole_NameAndActive("BASKAN", true)).thenReturn(List.of());

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> userService.changeRole(targetId, "BASKAN", null))
                .withMessageContaining("replacementBaskanYardimcisiId");

        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("replacement verildiginde koltuk devri ayni islemde uygulanir")
    void changeRole_replacementVerilinceKoltukDevrediliyor() {
        UUID targetId = UUID.randomUUID();
        UUID replacementId = UUID.randomUUID();
        Role bskYrd = role(2, "BASKAN_YARDIMCISI");
        Role baskan = role(3, "BASKAN");
        User target = user(targetId, bskYrd, true);
        User replacement = user(replacementId, role(1, "CALISAN"), true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findById(replacementId)).thenReturn(Optional.of(replacement));
        when(roleRepository.findByName("BASKAN")).thenReturn(Optional.of(baskan));
        when(roleRepository.findByName("BASKAN_YARDIMCISI")).thenReturn(Optional.of(bskYrd));
        when(userRepository.findByRole_NameAndActive("BASKAN", true)).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.changeRole(targetId, "BASKAN", replacementId);

        assertThat(result.getRole().getName()).isEqualTo("BASKAN");
        assertThat(replacement.getRole().getName()).isEqualTo("BASKAN_YARDIMCISI");
    }

    @Test
    @DisplayName("koltugu bosaltan kisi kendi yerine atanamaz")
    void changeRole_kendiYerineAtanamaz() {
        UUID targetId = UUID.randomUUID();
        Role bskYrd = role(2, "BASKAN_YARDIMCISI");
        Role baskan = role(3, "BASKAN");
        User target = user(targetId, bskYrd, true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByName("BASKAN")).thenReturn(Optional.of(baskan));
        when(userRepository.findByRole_NameAndActive("BASKAN", true)).thenReturn(List.of());
        when(userRepository.save(target)).thenReturn(target);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> userService.changeRole(targetId, "BASKAN", targetId));
    }

    @Test
    @DisplayName("pasif kullanici Baskan Yardimcisi yapilamaz")
    void changeRole_pasifKullaniciReplacementOlamaz() {
        UUID targetId = UUID.randomUUID();
        UUID replacementId = UUID.randomUUID();
        Role bskYrd = role(2, "BASKAN_YARDIMCISI");
        Role baskan = role(3, "BASKAN");
        User target = user(targetId, bskYrd, true);
        User inactiveReplacement = user(replacementId, role(1, "CALISAN"), false);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findById(replacementId)).thenReturn(Optional.of(inactiveReplacement));
        when(roleRepository.findByName("BASKAN")).thenReturn(Optional.of(baskan));
        when(userRepository.findByRole_NameAndActive("BASKAN", true)).thenReturn(List.of());
        when(userRepository.save(target)).thenReturn(target);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> userService.changeRole(targetId, "BASKAN", replacementId))
                .withMessageContaining("Pasif");
    }

    // ---------------- setActive ----------------

    @Test
    @DisplayName("aktif Baskan Yardimcisi devir yapilmadan pasiflestirilemez")
    void setActive_aktifBskYrdDevirsizPasiflestirilemez() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, role(2, "BASKAN_YARDIMCISI"), true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> userService.setActive(targetId, false))
                .withMessageContaining("devredin");
    }

    @Test
    @DisplayName("Admin hesabi bu yoldan pasiflestirilemez")
    void setActive_adminPasiflestirilemez() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, role(4, "ADMIN"), true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> userService.setActive(targetId, false));
    }

    @Test
    @DisplayName("var olmayan kullanici icin ResourceNotFoundException firlatir")
    void setActive_kullaniciBulunamazsaHataFirlatir() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> userService.setActive(targetId, false));
    }

    @Test
    @DisplayName("ayni e-posta ile kayit denemesinde DB kisiti ihlali yukari firlatilir")
    void createUser_ayniEpostaDbKisitiIhlaliniFirlatir() {
        when(roleRepository.findByName("CALISAN")).thenReturn(Optional.of(role(1, "CALISAN")));
        when(passwordEncoder.encode("sifre123")).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> userService.createUser("Ad", "Soyad", "mevcut@example.com", "sifre123"));

        verifyNoInteractions(userAuditLogService);
    }
}
