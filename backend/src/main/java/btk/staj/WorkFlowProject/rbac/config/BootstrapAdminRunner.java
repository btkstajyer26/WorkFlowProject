package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.email:admin@workflow.com}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:ChangeMe123!}")
    private String adminPassword;

    public BootstrapAdminRunner(UserRepository userRepository, RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) return;

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN rolü bulunamadı"));

        User admin = new User();
        admin.setFirstName("Bootstrap");
        admin.setLastName("Admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole);
        admin.setActive(true);
        admin.setMustChangePassword(true);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);
    }
}