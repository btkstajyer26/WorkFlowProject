package btk.staj.WorkFlowProject.notification.repository;

import btk.staj.WorkFlowProject.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    @Query("SELECT d.token FROM DeviceToken d WHERE d.user.id = :userId AND d.active = true")
    List<String> findActiveTokensByUserId(@Param("userId") UUID userId);

    @Query("SELECT d FROM DeviceToken d WHERE d.user.id = :userId AND d.active = true")
    List<DeviceToken> findAllActiveByUserId(@Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("UPDATE DeviceToken d SET d.active = false, d.updatedAt = CURRENT_TIMESTAMP WHERE d.token = :token AND d.user.id = :userId")
    int deactivateByTokenAndUserId(@Param("token") String token, @Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("UPDATE DeviceToken d SET d.active = false, d.updatedAt = CURRENT_TIMESTAMP WHERE d.token = :token")
    void deactivateByToken(@Param("token") String token);
}