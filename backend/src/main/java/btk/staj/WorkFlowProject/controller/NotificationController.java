package btk.staj.WorkFlowProject.controller;

import btk.staj.WorkFlowProject.entity.Notification;
import btk.staj.WorkFlowProject.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*") // Frontend entegrasyonunda CORS hatası yememek için
public class NotificationController {

    private final NotificationService notificationService;

    // Constructor Injection
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Kullanıcının okunmamış bildirimlerini listeler.
     * GET http://localhost:8080/api/notifications/unread/{userId}
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    /**
     * Bildirimi okundu olarak işaretler.
     * PUT http://localhost:8080/api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}