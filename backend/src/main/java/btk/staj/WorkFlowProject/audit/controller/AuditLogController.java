package btk.staj.WorkFlowProject.audit.controller;

import btk.staj.WorkFlowProject.audit.entity.AuditLog;
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

    // Bir kaydın (evrağın) işlem geçmişini getirir.
    // Örnek kullanım: GET /api/audit-logs/record/{recordId}
    @GetMapping("/record/{recordId}")
    public List<AuditLog> getGecmis(@PathVariable UUID recordId) {
        return auditLogService.getGecmis(recordId);
    }
}