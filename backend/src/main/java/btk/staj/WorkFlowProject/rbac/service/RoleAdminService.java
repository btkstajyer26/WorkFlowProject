package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentUserProvider;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.common.exception.RoleInUseException;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.port.WorkflowRoleUsagePort;
import btk.staj.WorkFlowProject.rbac.dto.CreateRoleRequest;
import btk.staj.WorkFlowProject.rbac.dto.RoleResponse;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRoleRequest;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * AP-2 rol yonetimi. Yerlesik rolun anlamini tasiyan {@code system_key} ve
 * {@code is_system} istemciden hicbir kosulda degistirilemez; yeni rol daima
 * dinamik (sistem rolu olmayan) ve sinirsiz kapasiteli acilir.
 *
 * <p>Silme ucu yoktur: erisim {@code is_active=false} ile kapatilir. Kullanimda
 * olan rolun pasiflestirilmesi engellenir, boylece hicbir kullanici pasif bir
 * rolde birakilmaz.
 *
 * <p><strong>Kullanim iki anlama gelir.</strong> Rolde aktif kullanici olmasi
 * ilkidir; ikincisi rolun akista aktor olarak bagli oldugu gecislerde islem
 * bekleyen acik kayit bulunmasidir ({@link WorkflowRoleUsagePort}). Ikincisi
 * olmadan Admin, WF-8'in {@code unbind} uzerinde reddettigi sonucu bu ekrandan
 * elde edebilirdi: aktorlugu kapatilan rolun kullanicilari, dogrulayicinin ilk
 * kontrolune ({@code WORKFLOW_ROLE_NOT_ALLOWED}) takilir ve ellerindeki butun
 * acik kayitlar kilitlenir.
 */
@Service
public class RoleAdminService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserAuditLogService userAuditLogService;
    private final CurrentUserProvider currentUserProvider;
    private final WorkflowRoleUsagePort workflowRoleUsage;

    public RoleAdminService(RoleRepository roleRepository,
                            UserRepository userRepository,
                            UserAuditLogService userAuditLogService,
                            CurrentUserProvider currentUserProvider,
                            WorkflowRoleUsagePort workflowRoleUsage) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userAuditLogService = userAuditLogService;
        this.currentUserProvider = currentUserProvider;
        this.workflowRoleUsage = workflowRoleUsage;
    }

    /**
     * Varsayilan cagri atanabilir rolleri dondurur (yalniz aktif). Yonetim
     * ekrani pasif rolleri de gorebilmek icin {@code includeInactive} kullanir;
     * aksi halde pasiflestirilen rol listeden dusup geri acilamaz hale gelirdi.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles(boolean includeInactive) {
        return roleRepository.findAllByOrderByIdAsc().stream()
                .filter(role -> includeInactive || role.isActive())
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        String name = requireName(request.getName());
        assertNameAvailable(name, null);

        Role role = new Role();
        role.setName(name);
        role.setDescription(normalizeDescription(request.getDescription()));
        role.setSystemKey(null);
        role.setSystem(false);
        role.setWorkflowActor(request.isWorkflowActor());
        role.setMaxUsers(null);
        role.setActive(true);

        Role saved = roleRepository.save(role);

        userAuditLogService.logIslem(
                null,
                currentUserProvider.currentUserId(),
                "ROLE_CREATED",
                null,
                saved.getId(),
                null,
                true,
                "Yeni rol oluşturuldu: " + saved.getName());

        return RoleResponse.from(saved);
    }

    @Transactional
    public RoleResponse update(Integer id, UpdateRoleRequest request) {
        Role role = roleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + id));
        boolean previousActive = role.isActive();
        List<String> changes = new ArrayList<>();

        if (request.getName() != null) {
            String name = requireName(request.getName());
            if (!name.equals(role.getName())) {
                assertNameAvailable(name, role.getId());
                changes.add("ad: " + role.getName() + " → " + name);
                role.setName(name);
            }
        }

        if (request.getDescription() != null) {
            role.setDescription(normalizeDescription(request.getDescription()));
            changes.add("açıklama güncellendi");
        }

        if (request.getWorkflowActor() != null && request.getWorkflowActor() != role.isWorkflowActor()) {
            // Yerlesik rolun aktorlugu is akisinin temelidir; panelden degistirilemez.
            if (role.isSystem()) {
                throw new BusinessRuleException(
                        "Sistem rolünün workflow aktörlüğü değiştirilemez: " + role.getName());
            }
            // Aktorlugu kapatmak, rolun butun gecislerini bir anda kullanilamaz kilar;
            // acmak boyle bir riski tasimaz.
            if (!request.getWorkflowActor()) {
                assertNotUsedByWorkflow(role);
            }
            role.setWorkflowActor(request.getWorkflowActor());
            changes.add("workflow aktörlüğü: " + request.getWorkflowActor());
        }

        if (request.getActive() != null && request.getActive() != role.isActive()) {
            if (!request.getActive()) assertDeactivatable(role);
            role.setActive(request.getActive());
            changes.add(request.getActive() ? "etkinleştirildi" : "pasifleştirildi");
        }

        if (changes.isEmpty()) return RoleResponse.from(role);

        Role saved = roleRepository.save(role);

        userAuditLogService.logIslem(
                null,
                currentUserProvider.currentUserId(),
                "ROLE_UPDATED",
                saved.getId(),
                saved.getId(),
                previousActive,
                saved.isActive(),
                saved.getName() + " rolü güncellendi (" + String.join(", ", changes) + ")");

        return RoleResponse.from(saved);
    }

    private void assertDeactivatable(Role role) {
        if (role.isSystem()) {
            throw new BusinessRuleException("Sistem rolü pasifleştirilemez: " + role.getName());
        }
        long activeUsers = userRepository.countByRole_IdAndActiveTrue(role.getId());
        if (activeUsers > 0) {
            throw new BusinessRuleException("Bu rol " + activeUsers
                    + " aktif kullanıcıda; önce onların rolünü değiştirin: " + role.getName());
        }
        // Aktif kullanicisi olmayan bir rolde de acik kayit bulunabilir: kaydin
        // olusturucusu pasiflestirilmis olabilir. Etki analizi bu yuzden ayrica yapilir.
        assertNotUsedByWorkflow(role);
    }

    /**
     * WF-8'in kaldirma korumasiyla ayni etki analizi. Kontrol muhafazakardir:
     * kullanici/rol pasifligi veya permission eksikligi sorguyu daraltmaz.
     */
    private void assertNotUsedByWorkflow(Role role) {
        if (workflowRoleUsage.hasOpenWorkflowUsage(role.getId())) {
            throw new RoleInUseException("Bu rolün işlem bekleyen açık kayıtları var; "
                    + "önce o kayıtlar tamamlanmalı: " + role.getName());
        }
    }

    private String requireName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new BusinessRuleException("Rol adı boş olamaz");
        return name;
    }

    /**
     * Anlamli mesaj icin on kontrol; yaris durumunda {@code roles.name} benzersizlik
     * kisiti son sozu soyler ve 409 uretir.
     */
    private void assertNameAvailable(String name, Integer selfId) {
        roleRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Bu rol adı zaten kullanılıyor: " + name);
                });
    }

    private String normalizeDescription(String raw) {
        if (raw == null) return null;
        String description = raw.trim();
        return description.isEmpty() ? null : description;
    }
}
