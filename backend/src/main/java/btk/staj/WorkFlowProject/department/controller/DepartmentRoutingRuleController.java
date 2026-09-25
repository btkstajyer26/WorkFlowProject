package btk.staj.WorkFlowProject.department.controller;

import btk.staj.WorkFlowProject.department.dto.CreateDepartmentRoutingRuleRequest;
import btk.staj.WorkFlowProject.department.dto.DepartmentRoutingRuleResponse;
import btk.staj.WorkFlowProject.department.dto.UpdateDepartmentRoutingRuleRequest;
import btk.staj.WorkFlowProject.department.service.DepartmentRoutingRuleAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AP-5 departman routing kurallari. Okuma {@code DEPARTMENT_VIEW}, yazma
 * {@code DEPARTMENT_MANAGE} ister - AP-4 ile ayni ayrim.
 */
@RestController
@RequestMapping("/api/admin/departments/{departmentId}/routing-rules")
public class DepartmentRoutingRuleController {

    private final DepartmentRoutingRuleAdminService routingRuleAdminService;

    public DepartmentRoutingRuleController(DepartmentRoutingRuleAdminService routingRuleAdminService) {
        this.routingRuleAdminService = routingRuleAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public List<DepartmentRoutingRuleResponse> listRules(@PathVariable Integer departmentId) {
        return routingRuleAdminService.listRules(departmentId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentRoutingRuleResponse createRule(@PathVariable Integer departmentId,
                                                    @Valid @RequestBody CreateDepartmentRoutingRuleRequest request) {
        return routingRuleAdminService.create(departmentId, request);
    }

    @PatchMapping("/{ruleId}")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentRoutingRuleResponse updateRule(@PathVariable Integer departmentId, @PathVariable Integer ruleId,
                                                    @Valid @RequestBody UpdateDepartmentRoutingRuleRequest request) {
        return routingRuleAdminService.update(departmentId, ruleId, request);
    }
}
