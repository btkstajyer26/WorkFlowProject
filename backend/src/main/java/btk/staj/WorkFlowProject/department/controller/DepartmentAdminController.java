package btk.staj.WorkFlowProject.department.controller;

import btk.staj.WorkFlowProject.department.dto.AddDepartmentMemberRequest;
import btk.staj.WorkFlowProject.department.dto.CreateDepartmentRequest;
import btk.staj.WorkFlowProject.department.dto.DepartmentMembersResponse;
import btk.staj.WorkFlowProject.department.dto.DepartmentResponse;
import btk.staj.WorkFlowProject.department.dto.UpdateDepartmentRequest;
import btk.staj.WorkFlowProject.department.service.DepartmentAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * AP-4 departman ve uyelik yonetimi. Okuma {@code DEPARTMENT_VIEW}, yazma
 * {@code DEPARTMENT_MANAGE} ister - AP-2/AP-3 ile ayni ayrim.
 */
@RestController
@RequestMapping("/api/admin/departments")
public class DepartmentAdminController {

    private final DepartmentAdminService departmentAdminService;

    public DepartmentAdminController(DepartmentAdminService departmentAdminService) {
        this.departmentAdminService = departmentAdminService;
    }

    /** Varsayilan cagri yalniz aktif departmanlari dondurur. */
    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public List<DepartmentResponse> listDepartments(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return departmentAdminService.listDepartments(includeInactive);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentAdminService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentResponse updateDepartment(@PathVariable Integer id,
                                               @Valid @RequestBody UpdateDepartmentRequest request) {
        return departmentAdminService.update(id, request);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public DepartmentMembersResponse listMembers(@PathVariable Integer id) {
        return departmentAdminService.listMembers(id);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentMembersResponse addMember(@PathVariable Integer id,
                                               @Valid @RequestBody AddDepartmentMemberRequest request) {
        return departmentAdminService.addMember(id, request.getUserId());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentMembersResponse removeMember(@PathVariable Integer id, @PathVariable UUID userId) {
        return departmentAdminService.removeMember(id, userId);
    }
}
