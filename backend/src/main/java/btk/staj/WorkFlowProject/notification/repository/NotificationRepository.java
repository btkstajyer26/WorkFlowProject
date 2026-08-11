package btk.staj.WorkFlowProject.notification.repository;

import btk.staj.WorkFlowProject.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Okunmamislar; okundukca listeden dustugu icin sinirli kalir. */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    /** Tum bildirim gecmisi zamanla buyur, sayfalanarak donulur (sartname §6.2). */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);
}
