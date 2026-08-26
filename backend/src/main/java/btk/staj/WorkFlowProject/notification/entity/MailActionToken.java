package btk.staj.WorkFlowProject.notification.entity;

import btk.staj.WorkFlowProject.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * E-posta bildirimindeki tek tikla aksiyon baglantisinin arkasindaki
 * tek kullanimlik anahtar.
 *
 * <p>Anahtarin kendisi saklanmaz; yalnizca SHA-256 ozeti tutulur. Satir
 * anahtarin <em>kime</em>, <em>hangi evrak</em> ve <em>hangi aksiyon</em> icin
 * verildigini tasir. Tuketimde aktor {@link #user} alanindan cozulur; evragin
 * o anki {@code assignedTo} alanindan turetilmez.
 *
 * <p>Satir yetki kaynagi degildir: tuketimde gercek durum makinesi yeniden
 * calisir ve gecis oradan da gecmek zorundadir.
 */
@Entity
@Table(name = "mail_action_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailActionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
