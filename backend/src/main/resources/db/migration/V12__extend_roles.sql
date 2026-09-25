-- =====================================================================
-- EBYS - roles tablosu genisletme
--
-- Kaynak: DB_1_VERI_MODELI_SOZLESMESI.md SS6.1
-- Amac: Rol yonetiminin admin panelinden yapilabilmesi icin roles
-- tablosuna sistem rolu isaretleme, workflow aktorlugu ve koltuk siniri
-- alanlari eklemek. Mevcut roles.id iliskisel kimlik olarak kalir;
-- yerlesik rolun degismez anlami yeni system_key kolonundadir.
-- =====================================================================

ALTER TABLE roles
    ALTER COLUMN name TYPE VARCHAR(100);

ALTER TABLE roles
    ADD COLUMN system_key         VARCHAR(50),
    ADD COLUMN is_system          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_workflow_actor  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN max_users          INTEGER,
    ADD COLUMN is_active          BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE roles
    ADD CONSTRAINT uq_roles_system_key UNIQUE (system_key);

ALTER TABLE roles
    ADD CONSTRAINT chk_roles_max_users
        CHECK (max_users IS NULL OR max_users >= 1);

-- is_system = TRUE ise system_key dolu, degilse bos olmali (SS6.1).
ALTER TABLE roles
    ADD CONSTRAINT chk_roles_system_key_consistency
        CHECK (
            (is_system = TRUE  AND system_key IS NOT NULL) OR
            (is_system = FALSE AND system_key IS NULL)
        );

-- =====================================================================
-- Yerlesik 4 rolun backfill'i (SS6.1 tablosu ile birebir).
-- name degerleri V1'de zaten CALISAN / BASKAN_YARDIMCISI / BASKAN / ADMIN
-- olarak seed edilmisti; burada sadece yeni kolonlar dolduruluyor.
-- =====================================================================

UPDATE roles SET
    system_key        = 'CALISAN',
    is_system          = TRUE,
    is_workflow_actor  = TRUE,
    max_users          = NULL,
    is_active          = TRUE
WHERE name = 'CALISAN';

UPDATE roles SET
    system_key        = 'BASKAN_YARDIMCISI',
    is_system          = TRUE,
    is_workflow_actor  = TRUE,
    max_users          = 1,
    is_active          = TRUE
WHERE name = 'BASKAN_YARDIMCISI';

UPDATE roles SET
    system_key        = 'BASKAN',
    is_system          = TRUE,
    is_workflow_actor  = TRUE,
    max_users          = 1,
    is_active          = TRUE
WHERE name = 'BASKAN';

UPDATE roles SET
    system_key        = 'ADMIN',
    is_system          = TRUE,
    is_workflow_actor  = FALSE,
    max_users          = 1,
    is_active          = TRUE
WHERE name = 'ADMIN';