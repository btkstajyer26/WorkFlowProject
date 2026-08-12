package btk.staj.WorkFlowProject.user.service;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import java.time.LocalDateTime;

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
    public User changeRole(java.util.UUID userId, String newRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + newRoleName));

        if (newRole.getName().equals("ADMIN")) {
            long existingAdminCount = userRepository.findByRole_NameAndActive("ADMIN", true).size();
            if (existingAdminCount >= 1) {
                throw new AdminLimitExceededException("Sistemde zaten bir ADMIN var, ikinci ADMIN atanamaz");
            }
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    public class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(String message) { super(message); }
    }
}