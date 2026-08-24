package btk.staj.WorkFlowProject.auth.entity;

import btk.staj.WorkFlowProject.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * "Şifremi unuttum" akışındaki tek bir sıfırlama girişimi.
 *
 * <p>Aynı satır iki aşamayı da tutar: önce e-postayla gönderilen 6 haneli
 * kodun özeti ({@code codeHash}), kod doğrulandıktan sonra da şifre değiştirme
 * ekranına taşınan tek kullanımlık anahtarın özeti ({@code resetTokenHash}).
 * Kod ve anahtar hiçbir zaman açık saklanmaz.
 */
@Entity
@Table(name = "password_reset_codes")
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "reset_token_hash", unique = true, length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public PasswordResetCode() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetTokenHash(String resetTokenHash) { this.resetTokenHash = resetTokenHash; }
    public LocalDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
