package btk.staj.WorkFlowProject.department.service;

import btk.staj.WorkFlowProject.audit.model.RequestAccessEvent;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.department.dto.CreateDepartmentRequest;
import btk.staj.WorkFlowProject.department.dto.DepartmentMembersResponse;
import btk.staj.WorkFlowProject.department.dto.DepartmentResponse;
import btk.staj.WorkFlowProject.department.dto.UpdateDepartmentRequest;
import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;
import btk.staj.WorkFlowProject.department.entity.DepartmentMemberEntity;
import btk.staj.WorkFlowProject.department.exception.DepartmentInUseException;
import btk.staj.WorkFlowProject.department.exception.DepartmentNotFoundException;
import btk.staj.WorkFlowProject.department.port.DepartmentOpenUsagePort;
import btk.staj.WorkFlowProject.department.repository.DepartmentMemberRepository;
import btk.staj.WorkFlowProject.department.repository.DepartmentRepository;
import btk.staj.WorkFlowProject.user.dto.UserResponse;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * AP-4 departman ve uyelik yonetimi. Silme ucu yoktur: erisim
 * {@code is_active=false} ile kapatilir (AP-2 rol yonetimiyle ayni sozlesme).
 *
 * <p>Departman hiyerarsisi ({@code parentDepartmentId}) yalniz yapisal
 * bilgidir; hedef cozumunde veya otomatik eskalasyonda kullanilmaz
 * (WORKFLOW_V1_V2_PLANI.md SS14). Satir ici DB CHECK'i yalniz 1 adimlik
 * kendine referansi engeller (V22); daha uzun ata dongulerini bu servis,
 * parent'i yazan transaction icinde ata zincirini yuruyerek reddeder
 * (DB_1_VERI_MODELI_SOZLESMESI.md SS15).
 *
 * <p>Uyelik kendi rolunu tasimaz ve kendi aktiflik bayragi yoktur - ikili
 * bir iliskidir (bkz. {@link DepartmentMemberRepository}); bu yuzden uye
 * ekleme/cikarma acik kuyruk kontrolu gerektirmez: routing "uygun uye yok"
 * durumunu zaten sessizce, hata uretmeden karsilar. Departmanin kendisini
 * pasiflestirmek daha genis etkilidir (butun routing'i kapatir), bu yuzden
 * yalniz o adim {@link DepartmentOpenUsagePort} ile korunur.
 */
@Service
public class DepartmentAdminService {

    private final DepartmentRepository departments;
    private final DepartmentMemberRepository members;
    private final UserRepository users;
    private final DepartmentOpenUsagePort openUsage;
    private final CurrentVisibilityActorProvider actors;
    private final AuditLogService auditLogs;
    private final UserAuditLogService userAuditLogs;

    public DepartmentAdminService(DepartmentRepository departments,
                                  DepartmentMemberRepository members,
                                  UserRepository users,
                                  DepartmentOpenUsagePort openUsage,
                                  CurrentVisibilityActorProvider actors,
                                  AuditLogService auditLogs,
                                  UserAuditLogService userAuditLogs) {
        this.departments = departments;
        this.members = members;
        this.users = users;
        this.openUsage = openUsage;
        this.actors = actors;
        this.auditLogs = auditLogs;
        this.userAuditLogs = userAuditLogs;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments(boolean includeInactive) {
        List<DepartmentEntity> all = departments.findAllByOrderByNameAsc();
        return all.stream()
                .filter(d -> includeInactive || d.isActive())
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        String name = requireName(request.getName());
        assertNameAvailable(name, null);

        Integer parentId = request.getParentDepartmentId();
        if (parentId != null) {
            requireExisting(parentId);
        }

        DepartmentEntity department = new DepartmentEntity();
        department.setName(name);
        department.setParentDepartmentId(parentId);
        department.setActive(true);

        DepartmentEntity saved = departments.save(department);
        audit("DEPARTMENT_CREATED", "departmentId=" + saved.getId()
                + ";name=" + saved.getName() + ";parentDepartmentId=" + saved.getParentDepartmentId());

        return DepartmentResponse.from(saved);
    }

    @Transactional
    public DepartmentResponse update(Integer id, UpdateDepartmentRequest request) {
        DepartmentEntity department = departments.findByIdForUpdate(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Departman bulunamadı: " + id));
        boolean previousActive = department.isActive();
        List<String> changes = new ArrayList<>();

        if (request.getName() != null) {
            String name = requireName(request.getName());
            if (!name.equals(department.getName())) {
                assertNameAvailable(name, department.getId());
                changes.add("ad: " + department.getName() + " → " + name);
                department.setName(name);
            }
        }

        if (request.isClearParent()) {
            if (department.getParentDepartmentId() != null) {
                changes.add("üst departman kaldırıldı");
                department.setParentDepartmentId(null);
            }
        } else if (request.getParentDepartmentId() != null
                && !request.getParentDepartmentId().equals(department.getParentDepartmentId())) {
            Integer newParentId = request.getParentDepartmentId();
            requireExisting(newParentId);
            assertNoCycle(department.getId(), newParentId);
            changes.add("üst departman: " + department.getParentDepartmentId() + " → " + newParentId);
            department.setParentDepartmentId(newParentId);
        }

        if (request.getActive() != null && request.getActive() != department.isActive()) {
            if (!request.getActive()) assertDeactivatable(department);
            department.setActive(request.getActive());
            changes.add(request.getActive() ? "etkinleştirildi" : "pasifleştirildi");
        }

        if (changes.isEmpty()) return DepartmentResponse.from(department);

        DepartmentEntity saved = departments.save(department);
        audit("DEPARTMENT_UPDATED", "departmentId=" + saved.getId() + ";previousActive=" + previousActive
                + ";active=" + saved.isActive() + ";değişiklikler=" + String.join(", ", changes));

        return DepartmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public DepartmentMembersResponse listMembers(Integer departmentId) {
        DepartmentEntity department = departments.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Departman bulunamadı: " + departmentId));
        List<UUID> userIds = members.findAllByIdDepartmentId(departmentId).stream()
                .map(m -> m.getId().getUserId())
                .toList();
        List<UserResponse> memberResponses = users.findAllById(userIds).stream()
                .map(UserResponse::from)
                .sorted((a, b) -> {
                    int cmp = a.lastName().compareToIgnoreCase(b.lastName());
                    return cmp != 0 ? cmp : a.firstName().compareToIgnoreCase(b.firstName());
                })
                .toList();
        return new DepartmentMembersResponse(department.getId(), department.getName(), memberResponses);
    }

    @Transactional
    public DepartmentMembersResponse addMember(Integer departmentId, UUID userId) {
        DepartmentEntity department = departments.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Departman bulunamadı: " + departmentId));
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Kullanıcı bulunamadı: " + userId));

        if (!members.existsByIdDepartmentIdAndIdUserId(departmentId, userId)) {
            members.save(new DepartmentMemberEntity(departmentId, userId));
            audit("DEPARTMENT_MEMBER_ADDED", "departmentId=" + departmentId + ";userId=" + userId);
        }

        return listMembers(department.getId());
    }

    @Transactional
    public DepartmentMembersResponse removeMember(Integer departmentId, UUID userId) {
        DepartmentEntity department = departments.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Departman bulunamadı: " + departmentId));

        if (members.existsByIdDepartmentIdAndIdUserId(departmentId, userId)) {
            members.deleteById(new DepartmentMemberEntity.Id(departmentId, userId));
            audit("DEPARTMENT_MEMBER_REMOVED", "departmentId=" + departmentId + ";userId=" + userId);
        }

        return listMembers(department.getId());
    }

    private void assertDeactivatable(DepartmentEntity department) {
        if (openUsage.hasOpenRecords(department.getId())) {
            throw new DepartmentInUseException("Bu departmana atanmış işlem bekleyen açık kayıtlar var; "
                    + "önce o kayıtlar tamamlanmalı: " + department.getName());
        }
    }

    /**
     * Yeni ust'un ata zincirini yurur; departmanin kendisi zincirde
     * gorunuyorsa dongu vardir. Bozuk (zaten dongulu) veri karsisinda sonsuz
     * dongu olusmamasi icin ziyaret edilen kimlikler ayrica izlenir.
     */
    private void assertNoCycle(Integer departmentId, Integer newParentId) {
        Set<Integer> visited = new HashSet<>();
        Integer current = newParentId;
        while (current != null) {
            if (current.equals(departmentId)) {
                throw new BusinessRuleException("Üst departman döngü oluşturuyor");
            }
            if (!visited.add(current)) return;
            current = departments.findById(current).map(DepartmentEntity::getParentDepartmentId).orElse(null);
        }
    }

    private void requireExisting(Integer departmentId) {
        if (!departments.existsById(departmentId)) {
            throw new DepartmentNotFoundException("Üst departman bulunamadı: " + departmentId);
        }
    }

    private String requireName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new BusinessRuleException("Departman adı boş olamaz");
        return name;
    }

    private void assertNameAvailable(String name, Integer selfId) {
        departments.findByName(name)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Bu departman adı zaten kullanılıyor: " + existing.getName());
                });
    }

    private void audit(String action, String comment) {
        VisibilityActor actor = actors.currentVisibilityActor();
        RequestAccessEvent event = new RequestAccessEvent(action, actor.id(), actor.roleId().value(),
                actor.systemRole().map(Enum::name).orElse(null), null, null, null, null, comment);
        if (event.adminActor()) auditLogs.recordAccess(event);
        else userAuditLogs.recordAccess(event);
    }
}
