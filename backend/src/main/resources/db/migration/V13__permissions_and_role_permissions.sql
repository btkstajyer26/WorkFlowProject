-- =====================================================================
-- EBYS - permissions ve role_permissions
--
-- Kaynak: DB_1_VERI_MODELI_SOZLESMESI.md SS6.2 / SS6.3
-- Amac: Yetkilendirmenin rol adina bagli hasRole(...) yerine permission
-- tabanli hasAuthority(...) kontrolune tasinabilmesi icin sabit
-- capability katalogu ve rol-yetki eslemesi.
-- =====================================================================

CREATE TABLE permissions (
    id           SERIAL PRIMARY KEY,
    -- Degismez capability anahtari. Yeni kod yalniz backend destegi ve
    -- Flyway seed migration'i ile eklenir - admin yeni kod uretemez.
    code         VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    description  VARCHAR(255),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_permissions_code_upper CHECK (code = UPPER(code))
);

CREATE TABLE role_permissions (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles (id)       ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- Birlesik PK yalniz role_id ile baslayan sorguyu verimli karsilar;
-- permission_id -> hangi roller diye ters sorgu icin ayri indeks gerekir.
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);


-- =====================================================================
-- Baslangic capability katalogu (SS6.2 - en az bu 15 kod).
-- =====================================================================

INSERT INTO permissions (code, display_name, description) VALUES
    ('RECORD_CREATE',       'Kayıt Oluşturma',          'Yeni taslak kayıt oluşturur'),
    ('RECORD_VIEW',         'Kayıt Görüntüleme',        'Kayıt içeriğini görüntüler'),
    ('RECORD_EDIT',         'Kayıt Düzenleme',          'Taslak veya düzenleme bekleyen kaydı düzenler'),
    ('RECORD_FORWARD',      'Kayıt İletme',             'Kaydı bir sonraki incelemeye gönderir/iletir'),
    ('RECORD_RETURN',       'Kayıt Geri Gönderme',      'Kaydı önceki adıma geri gönderir'),
    ('RECORD_APPROVE',      'Kayıt Onaylama',           'Kaydı nihai onaylar'),
    ('RECORD_REJECT',       'Kayıt Reddetme',           'Kaydı reddeder'),
    ('USER_VIEW',           'Kullanıcı Görüntüleme',    'Kullanıcı listesini ve detayını görüntüler'),
    ('USER_MANAGE',         'Kullanıcı Yönetimi',       'Kullanıcı oluşturur, düzenler, etkinlik durumunu değiştirir'),
    ('ROLE_VIEW',           'Rol Görüntüleme',          'Rol listesini görüntüler'),
    ('ROLE_MANAGE',         'Rol Yönetimi',             'Rol oluşturur, düzenler, yetki atar'),
    ('DEPARTMENT_VIEW',     'Departman Görüntüleme',    'Departman listesini görüntüler'),
    ('DEPARTMENT_MANAGE',   'Departman Yönetimi',       'Departman oluşturur, üyelik ve akış kuralını düzenler'),
    ('WORKFLOW_VIEW',       'Workflow Görüntüleme',     'Durum/aksiyon/geçiş tanımlarını görüntüler'),
    ('WORKFLOW_MANAGE',     'Workflow Yönetimi',        'Durum/aksiyon/geçiş tanımlarını düzenler'),
    ('ADMIN_PANEL_ACCESS',  'Admin Paneli Erişimi',     'Admin paneline erişim ön koşulu');


-- =====================================================================
-- Baslangic rol-permission eslemesi (SS6.3 tablosu).
-- Onemli: ADMIN'in workflow KAYIT permission'i (RECORD_*) YOKTUR -
-- ADMIN evrak gormez, workflow islemi yapamaz (mevcut davranis).
-- =====================================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('CALISAN',            'RECORD_CREATE'),
    ('CALISAN',            'RECORD_VIEW'),
    ('CALISAN',            'RECORD_EDIT'),
    ('CALISAN',            'RECORD_FORWARD'),

    ('BASKAN_YARDIMCISI',  'RECORD_VIEW'),
    ('BASKAN_YARDIMCISI',  'RECORD_FORWARD'),
    ('BASKAN_YARDIMCISI',  'RECORD_RETURN'),

    ('BASKAN',             'RECORD_VIEW'),
    ('BASKAN',             'RECORD_APPROVE'),
    ('BASKAN',             'RECORD_REJECT'),
    ('BASKAN',             'RECORD_RETURN'),

    ('ADMIN',              'USER_VIEW'),
    ('ADMIN',              'USER_MANAGE'),
    ('ADMIN',              'ROLE_VIEW'),
    ('ADMIN',              'ROLE_MANAGE'),
    ('ADMIN',              'DEPARTMENT_VIEW'),
    ('ADMIN',              'DEPARTMENT_MANAGE'),
    ('ADMIN',              'WORKFLOW_VIEW'),
    ('ADMIN',              'WORKFLOW_MANAGE'),
    ('ADMIN',              'ADMIN_PANEL_ACCESS')
) AS v(system_key, code)
JOIN roles       r ON r.system_key = v.system_key
JOIN permissions p ON p.code       = v.code;