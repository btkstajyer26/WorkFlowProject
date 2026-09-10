package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.auth.security.CurrentUserProvider;
import btk.staj.WorkFlowProject.common.exception.BusinessRuleException;
import btk.staj.WorkFlowProject.rbac.Permission;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.RolePermission;
import btk.staj.WorkFlowProject.rbac.dto.PermissionResponse;
import btk.staj.WorkFlowProject.rbac.dto.RolePermissionsResponse;
import btk.staj.WorkFlowProject.rbac.dto.UpdateRolePermissionsRequest;
import btk.staj.WorkFlowProject.rbac.repository.PermissionRepository;
import btk.staj.WorkFlowProject.rbac.repository.RolePermissionRepository;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.service.RoleNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AP-3 rol <-> permission matrisi. Admin yalniz mevcut kapali katalogdan
 * secim yapar; yeni capability kodu bu katmandan uretilemez (DB_1 SS6.2).
 *
 * <p>Rol satiri {@link RoleRepository#findByIdForUpdate} ile kilitlenir -
 * WF-8/AP-8 sozlesmesinin ({@code WF8_AP8_AKTOR_ROL_BAGLAMA_SOZLESMESI.md})
 * gerektirdigi gibi AP-2/AP-3 yazicilari ayni rol satirini ayni sirayla
 * kilitler, boylece esali reload/bind/unbind ile yarismaz.
 *
 * <p>Bir permission'in kaldirilmasi, o an acik bir gecis onu gerektiriyor
 * olsa bile <strong>engellenmez</strong>: yetki kontrolu runtime'da canli
 * yapilir (plan SS8), rol pasiflestirme/aktorluk kapatmadan farkli olarak
 * burada "kilitlenen kayit" riski yoktur - yalniz o rolun kullanicilari
 * ilgili aksiyonu bir sonraki denemede goremez/yapamaz.
 */
@Service
public class PermissionAdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserAuditLogService userAuditLogService;
    private final CurrentUserProvider currentUserProvider;

    public PermissionAdminService(RoleRepository roleRepository,
                                  PermissionRepository permissionRepository,
                                  RolePermissionRepository rolePermissionRepository,
                                  UserAuditLogService userAuditLogService,
                                  CurrentUserProvider currentUserProvider) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userAuditLogService = userAuditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAllByOrderByIdAsc().stream()
                .map(PermissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolePermissionsResponse getRolePermissions(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + roleId));
        return toResponse(role, rolePermissionRepository.findAllCodesByRoleId(roleId));
    }

    @Transactional
    public RolePermissionsResponse updateRolePermissions(Integer roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findByIdForUpdate(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + roleId));

        Set<String> requestedCodes = normalize(request.getPermissionCodes());
        Map<String, Permission> catalog = permissionRepository.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));

        for (String code : requestedCodes) {
            if (!catalog.containsKey(code)) {
                throw new BusinessRuleException("Bilinmeyen permission kodu: " + code);
            }
        }

        Set<String> currentCodes = new HashSet<>(rolePermissionRepository.findAllCodesByRoleId(roleId));

        Set<String> toAdd = new HashSet<>(requestedCodes);
        toAdd.removeAll(currentCodes);
        Set<String> toRemove = new HashSet<>(currentCodes);
        toRemove.removeAll(requestedCodes);

        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            return toResponse(role, new ArrayList<>(currentCodes).stream().sorted().toList());
        }

        for (String code : toAdd) {
            Permission permission = catalog.get(code);
            // Katalogda var ama pasif olan bir kod yeni atanamaz; kaldirma
            // (toRemove) icin bu kisit uygulanmaz, cunku o kod zaten atanmisti.
            if (!permission.isActive()) {
                throw new BusinessRuleException("Pasif permission role atanamaz: " + code);
            }
            rolePermissionRepository.save(new RolePermission(roleId, permission.getId()));
        }

        for (String code : toRemove) {
            Permission permission = catalog.get(code);
            rolePermissionRepository.deleteById(new RolePermission.Id(roleId, permission.getId()));
        }

        List<String> finalCodes = rolePermissionRepository.findAllCodesByRoleId(roleId);

        userAuditLogService.logIslem(
                null,
                currentUserProvider.currentUserId(),
                "ROLE_PERMISSIONS_UPDATED",
                role.getId(),
                role.getId(),
                role.isActive(),
                role.isActive(),
                role.getName() + " rolünün yetkileri güncellendi ("
                        + describeDiff(toAdd, toRemove) + ")");

        return toResponse(role, finalCodes);
    }

    private String describeDiff(Set<String> added, Set<String> removed) {
        StringBuilder sb = new StringBuilder();
        if (!added.isEmpty()) sb.append("eklendi: ").append(String.join(", ", new TreeSet<>(added)));
        if (!removed.isEmpty()) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append("kaldırıldı: ").append(String.join(", ", new TreeSet<>(removed)));
        }
        return sb.toString();
    }

    private Set<String> normalize(Set<String> raw) {
        Set<String> normalized = new HashSet<>();
        if (raw == null) return normalized;
        for (String code : raw) {
            if (code == null) continue;
            String trimmed = code.trim().toUpperCase(Locale.ROOT);
            if (!trimmed.isEmpty()) normalized.add(trimmed);
        }
        return normalized;
    }

    private RolePermissionsResponse toResponse(Role role, List<String> codes) {
        return new RolePermissionsResponse(role.getId(), role.getName(), codes);
    }
}
