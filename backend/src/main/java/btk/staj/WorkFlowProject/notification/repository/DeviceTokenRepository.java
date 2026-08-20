package btk.staj.WorkFlowProject.notification.repository;

import btk.staj.WorkFlowProject.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    @Query("SELECT dt FROM DeviceToken dt WHERE dt.user.id = :userId AND dt.active = true")
    List<DeviceToken> findAllActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false, dt.updatedAt = CURRENT_TIMESTAMP WHERE dt.token = :token")
    void deactivateByToken(@Param("token") String token);
}