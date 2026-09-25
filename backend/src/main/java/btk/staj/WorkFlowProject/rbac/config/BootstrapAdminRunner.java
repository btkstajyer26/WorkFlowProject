package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.user.service.RoleCapacityService;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
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
    private final RoleCapacityService roleCapacity;

    @Value("${bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:}")
    private String adminPassword;

    public BootstrapAdminRunner(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                UserAuditLogService userAuditLogService, RoleCapacityService roleCapacity) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuditLogService = userAuditLogService;
        this.roleCapacity = roleCapacity;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        Role candidate = roleRepository.findBySystemKey(SystemRoleKey.ADMIN.name()).orElse(null);
        if (candidate == null) {
            log.warn("ADMIN rolü bulunamadı; bootstrap atlandı");
            return;
        }
        Role adminRole = roleCapacity.lockRoles(List.of(candidate.getId())).get(candidate.getId());
        if (userRepository.countByRole_IdAndActiveTrue(adminRole.getId()) > 0) return;
        roleCapacity.assertAssignable(adminRole);
        roleCapacity.validate(Map.of(adminRole.getId(), adminRole), List.of(RoleCapacityService.Change.create(adminRole)));

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
