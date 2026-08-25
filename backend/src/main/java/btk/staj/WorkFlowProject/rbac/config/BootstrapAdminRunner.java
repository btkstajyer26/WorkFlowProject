package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditLogService userAuditLogService;

    @Value("${bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:}")
    private String adminPassword;

    public BootstrapAdminRunner(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                UserAuditLogService userAuditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuditLogService = userAuditLogService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        // Kosul "hic kullanici yok" degil "hic Admin yok": Admin silinip
        // kullanicilar dururken sistem yonetimsiz kalmasin.
        if (!userRepository.findByRole_NameAndActive("ADMIN", true).isEmpty()) {
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            log.warn("ADMIN rolü bulunamadı; bootstrap atlandı");
            return;
        }

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

        userAuditLogService.logIslem(
                admin.getId(),
                null,
                "BOOTSTRAP_ADMIN_CREATED",
                null,
                adminRole.getId(),
                null,
                true,
                "İlk Admin hesabı sistem tarafından oluşturuldu");

        log.info("Ilk Admin hesabi olusturuldu: {}. Parola ilk giriste degistirilmelidir.", adminEmail);
    }
}
