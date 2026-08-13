package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

        return userRepository.save(user);
    }
    /**
     * Kurumsal gorevlendirme degistiginde rolu gunceller. Sistemde tek bir
     * aktif Admin bulunur; ikinci Admin atamasi reddedilir.
     */
    @Transactional
    public User changeRole(UUID userId, String newRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + userId));

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
        return userRepository.save(user);
    }
}