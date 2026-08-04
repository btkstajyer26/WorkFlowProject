-- Flyway kurulumunu doğrulamak için başlangıç migration'ı.
-- Entity'ler yazıldıkça bu dosya genişletilebilir ya da V2, V3... ile yeni dosyalar eklenir.

CREATE TABLE app_user (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(200) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    role        VARCHAR(30)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_app_user_role
        CHECK (role IN ('CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN'))
);

CREATE INDEX idx_app_user_role ON app_user (role);
