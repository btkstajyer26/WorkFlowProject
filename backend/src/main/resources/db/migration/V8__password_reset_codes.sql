-- Şifremi unuttum akışı: e-posta ile gönderilen 6 haneli doğrulama kodu.
--
-- Akış iki adımlıdır ve tek satırda izlenir:
--   1) /forgot-password  -> satır oluşur, code_hash yazılır
--   2) /verify-reset-code -> kod doğrulanır, reset_token_hash üretilir
--   3) /reset-password    -> token tüketilir, consumed_at yazılır
--
-- Kod yalnızca 10^6 olasılık taşıdığı için özet BCrypt ile alınır (kaba kuvvet
-- pahalı olsun); reset token 256 bit rastgele olduğundan SHA-256 yeterlidir ve
-- tek yönlü olmasına rağmen sorguda aranabilir.
--
-- tokens tablosuna eklenmedi: oradaki satırlar refresh token yaşam döngüsüne
-- (revoked/expired) aittir; deneme sayacı ve iki aşamalı doğrulama o modele
-- sığmıyordu.

CREATE TABLE password_reset_codes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    -- 6 haneli kodun BCrypt özeti. Kodun kendisi hiçbir yerde saklanmaz.
    code_hash               VARCHAR(255) NOT NULL,
    -- Kaba kuvvet denemesini sınırlar; MAX_ATTEMPTS'e ulaşınca kod ölür.
    attempts                INT NOT NULL DEFAULT 0,
    -- Kod doğrulandıktan sonra üretilen tek kullanımlık sıfırlama anahtarının
    -- SHA-256 özeti. Doğrulama yapılana kadar NULL.
    reset_token_hash        VARCHAR(64) UNIQUE,
    reset_token_expires_at  TIMESTAMP,
    verified_at             TIMESTAMP,
    -- Dolu ise satır artık kullanılamaz: şifre değişti, yeni kod istendi veya
    -- deneme hakkı bitti.
    consumed_at             TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at              TIMESTAMP NOT NULL,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Kullanıcının açık kodunu bulmak en sık sorgudur.
CREATE INDEX idx_password_reset_user_open ON password_reset_codes (user_id, created_at)
    WHERE consumed_at IS NULL;
CREATE INDEX idx_password_reset_expires_at ON password_reset_codes (expires_at);
