package btk.staj.WorkFlowProject.audit.controller;

import btk.staj.WorkFlowProject.audit.dto.UserAuditLogResponse;
import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-audit-logs")
public class UserAuditLogController {

    private final UserAuditLogService userAuditLogService;

    public UserAuditLogController(UserAuditLogService userAuditLogService) {
        this.userAuditLogService = userAuditLogService;
    }

    // Yalnizca ADMIN erisebilir. Sadece GET var - degistirme/silme ucu yok.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{targetUserId}")
    public List<UserAuditLogResponse> getGecmis(@PathVariable UUID targetUserId) {
        return userAuditLogService.getGecmis(targetUserId);
    }
}
