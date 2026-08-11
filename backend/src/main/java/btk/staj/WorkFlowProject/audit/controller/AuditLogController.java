package btk.staj.WorkFlowProject.audit.controller;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/record/{recordId}")
    public List<AuditLogResponse> getGecmis(@PathVariable UUID recordId) {
        return auditLogService.getGecmis(recordId);
    }
}