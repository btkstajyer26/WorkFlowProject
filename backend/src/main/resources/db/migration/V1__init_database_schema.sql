-- =====================================================================
-- EBYS - Is Akisi ve Onay Yonetim Sistemi
-- Kanonik baslangic semasi
--
-- Bu dosya asagidakilerin birlesimidir:
--   * V1__init_database_schema.sql  (temel sema - record/auth ekipleri)
--   * V2__add_admin_role_and_user_audit.sql  (ADMIN paketi)
--   * V3__add_workflow_columns.sql  (workflow kolonlari)
-- Sema hicbir ortak veritabanina Flyway ile uygulanmadigi icin tek dosyada
-- birlestirilmistir. Bu dosya bir kez uygulandiktan sonra ASLA degistirilmez;
-- her yeni degisiklik icin yeni bir V dosyasi eklenir (Flyway checksum tutar).
--
-- PostgreSQL 15.18. gen_random_uuid() 13'ten beri cekirdekte oldugu icin
-- pgcrypto extension'ina gerek yoktur.
-- =====================================================================


-- =====================================================================
-- 1. BAGIMSIZ TABLOLAR
-- =====================================================================

-- Kullanici yetki seviyeleri. name degerleri Java'daki RoleName enum'u ile
-- birebir aynidir; sayisal id'ler koda tasinmaz.
CREATE TABLE roles (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Dinamik kategori/departman listesi.
CREATE TABLE categories (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);


-- =====================================================================
-- 2. KULLANICI VE OTURUM
-- =====================================================================

CREATE TABLE users (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name           VARCHAR(100) NOT NULL,
    last_name            VARCHAR(100) NOT NULL,
    email                VARCHAR(150) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    role_id              INT NOT NULL,
    -- Hesabin giris ve islem yapmaya acik olup olmadigi. Workflow kurali:
    -- pasif kullaniciya kayit atanamaz.
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    -- Bootstrap Admin veya parola sifirlama sonrasi ilk giriste parola
    -- degisimini zorunlu kilar.
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

-- JWT refresh token / blacklist yonetimi.
CREATE TABLE tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    token      VARCHAR(500) NOT NULL UNIQUE,
    token_type VARCHAR(50) NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    expired    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);


-- =====================================================================
-- 3. ANA KAYIT TABLOSU
-- =====================================================================

CREATE TABLE records (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title          VARCHAR(255) NOT NULL,
    description    TEXT NOT NULL,
    category_id    INT NOT NULL,
    -- Durum, Java'daki RecordStatus enum'unun adi olarak saklanir
    -- (@Enumerated(EnumType.STRING)). Ayri bir statuses tablosu yoktur.
    status         VARCHAR(50) NOT NULL,
    created_by     UUID NOT NULL,
    -- Kaydin o an islem bekledigi kullanici. Terminal durumda NULL olur.
    assigned_to    UUID,
    -- Kaydi Baskana ileten Baskan Yardimcisi. Yalnizca BASKANA_ILET
    -- aksiyonunda yazilir; Baskanin geri gonderme hedefi buradan bulunur.
    last_deputy_id UUID,
    -- Optimistic locking icin ayrilmistir (JPA @Version).
    version        INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    deleted_at     TIMESTAMP,
    CONSTRAINT fk_record_category    FOREIGN KEY (category_id)    REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_record_created_by  FOREIGN KEY (created_by)     REFERENCES users (id)      ON DELETE RESTRICT,
    CONSTRAINT fk_record_assigned_to FOREIGN KEY (assigned_to)    REFERENCES users (id)      ON DELETE SET NULL,
    CONSTRAINT fk_record_last_deputy FOREIGN KEY (last_deputy_id) REFERENCES users (id)      ON DELETE SET NULL,
    -- Yazim hatasinin veritabanina yazilmasini engeller. Yeni bir durum
    -- eklenirse bu constraint de yeni bir migration ile guncellenmelidir.
    CONSTRAINT chk_records_status CHECK (status IN (
        'TASLAK',
        'BSK_YRD_INCELEMESINDE',
        'BASKAN_INCELEMESINDE',
        'DUZENLEME_BEKLIYOR',
        'ONAYLANDI',
        'REDDEDILDI'
    ))
);


-- =====================================================================
-- 4. DENETIM IZI VE EKLER
-- =====================================================================

-- Kayit odakli denetim izi. Append-only kullanilir: guncellenmez, silinmez.
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id       UUID NOT NULL,
    user_id         UUID NOT NULL,
    role_id         INT NOT NULL,
    action          VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_record FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE RESTRICT,
    CONSTRAINT fk_audit_role   FOREIGN KEY (role_id)   REFERENCES roles (id)   ON DELETE RESTRICT
);

CREATE TABLE files (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id     UUID NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL UNIQUE,
    mime_type     VARCHAR(100) NOT NULL,
    file_size     INT NOT NULL,
    uploaded_by   UUID NOT NULL,
    uploaded_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_record   FOREIGN KEY (record_id)   REFERENCES records (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id)   ON DELETE RESTRICT
);


-- =====================================================================
-- 5. BILDIRIMLER
-- =====================================================================

CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    record_id  UUID NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_notification_record FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE
);


-- =====================================================================
-- 6. KULLANICI YONETIMI DENETIM IZI
-- =====================================================================

-- audit_logs.record_id zorunlu oldugu icin evraktan bagimsiz Admin islemleri
-- (kullanici olusturma, rol degistirme, hesap etkinlestirme) oraya yazilamaz.
-- Bu tablo da append-only kullanilir.
CREATE TABLE user_audit_logs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_user_id   UUID NOT NULL,
    -- Ilk Admin bootstrap islemi sistem tarafindan yapildigi icin NULL kalabilir
    -- (action = 'BOOTSTRAP_ADMIN_CREATED').
    performed_by     UUID,
    action           VARCHAR(50) NOT NULL,
    previous_role_id INT,
    new_role_id      INT,
    previous_active  BOOLEAN,
    new_active       BOOLEAN,
    comment          TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_audit_target       FOREIGN KEY (target_user_id)   REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_audit_performed_by FOREIGN KEY (performed_by)     REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_user_audit_prev_role    FOREIGN KEY (previous_role_id) REFERENCES roles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_audit_new_role     FOREIGN KEY (new_role_id)      REFERENCES roles (id) ON DELETE RESTRICT
);


-- =====================================================================
-- 7. INDEKSLER
-- =====================================================================

CREATE INDEX idx_users_role_id   ON users (role_id);
CREATE INDEX idx_users_is_active ON users (is_active);

CREATE INDEX idx_tokens_user_id ON tokens (user_id);

CREATE INDEX idx_records_status         ON records (status);
CREATE INDEX idx_records_created_by     ON records (created_by);
CREATE INDEX idx_records_assigned_to    ON records (assigned_to);
CREATE INDEX idx_records_category_id    ON records (category_id);
CREATE INDEX idx_records_last_deputy_id ON records (last_deputy_id);
-- Soft delete edilmemis kayitlarin listelenmesi en sik sorgudur.
CREATE INDEX idx_records_not_deleted    ON records (created_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_audit_record_id  ON audit_logs (record_id);
CREATE INDEX idx_audit_user_id    ON audit_logs (user_id);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at);

CREATE INDEX idx_files_record_id ON files (record_id);

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);

CREATE INDEX idx_user_audit_target_user_id ON user_audit_logs (target_user_id);
CREATE INDEX idx_user_audit_performed_by   ON user_audit_logs (performed_by);
CREATE INDEX idx_user_audit_created_at     ON user_audit_logs (created_at);


-- =====================================================================
-- 8. BASLANGIC VERISI
-- =====================================================================

-- name degerleri Java'daki RoleName enum'u ile birebir ayni olmalidir.
-- ADMIN bir yonetim rolodur; workflow gecislerinde aktor veya hedef olamaz.
INSERT INTO roles (name, description) VALUES
    ('CALISAN',           'Kayit olusturan, belge yukleyen ve taslak yoneten baslangic rolu'),
    ('BASKAN_YARDIMCISI', 'Ikinci seviye kontrol mercii'),
    ('BASKAN',            'Nihai onay mercii'),
    ('ADMIN',             'Kullanici ve rol yonetiminden sorumlu sistem yoneticisi');

INSERT INTO categories (name) VALUES
    ('İdari'),
    ('Mali'),
    ('İnsan Kaynakları'),
    ('Bilgi İşlem'),
    ('Teknik');
