-- Giriş/çıkış ve her HTTP isteği mevcut log tablolarına yazılabilsin diye
-- evrak zorunluluğu kaldırılır; hata kodu, istek yolu ve HTTP durumu eklenir.
--
-- Yönlendirme kuralı (uygulama katmanı):
--   ADMIN işlemleri  -> audit_logs
--   diğer kullanıcılar -> user_audit_logs

ALTER TABLE audit_logs
    ALTER COLUMN record_id DROP NOT NULL,
    ALTER COLUMN user_id DROP NOT NULL,
    ALTER COLUMN role_id DROP NOT NULL,
    ALTER COLUMN new_status DROP NOT NULL;

ALTER TABLE audit_logs
    ADD COLUMN http_method VARCHAR(10),
    ADD COLUMN request_path VARCHAR(512),
    ADD COLUMN http_status INT,
    ADD COLUMN error_code VARCHAR(80);

ALTER TABLE user_audit_logs
    ALTER COLUMN target_user_id DROP NOT NULL;

ALTER TABLE user_audit_logs
    ADD COLUMN http_method VARCHAR(10),
    ADD COLUMN request_path VARCHAR(512),
    ADD COLUMN http_status INT,
    ADD COLUMN error_code VARCHAR(80);

CREATE INDEX idx_audit_http_status ON audit_logs (http_status);
CREATE INDEX idx_user_audit_http_status ON user_audit_logs (http_status);
