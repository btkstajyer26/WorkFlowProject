-- =====================================================================
-- EBYS - workflow_statuses ve workflow_actions
--
-- Kaynak: DB_1_VERI_MODELI_SOZLESMESI.md SS6.4 / SS6.5
-- Sozlesme SS13.2 madde 3: bu iki tablo ayni adimda olusturulur ve
-- seed edilir.
--
-- NOT: workflow_actions'ta target_strategy VE expected_target_role_id
-- YOKTUR (SS6.5: "Ayni aksiyon farkli gecislerde farkli hedefe
-- gidebilir"). Bu ikisi Bolum 5'te workflow_transitions'a eklenecek.
-- comment_required de bu tabloda - workflow_transitions'a DEGIL
-- (yorum zorunlulugu aksiyona ait, gecise degil).
-- =====================================================================

CREATE TABLE workflow_statuses (
    id                      SERIAL PRIMARY KEY,
    -- Degismez teknik anahtar. records.status ve RecordStatus enum'u ile
    -- birebir ayni kalir (Bolum 6'da FK baglanacak).
    name                    VARCHAR(50) NOT NULL UNIQUE,
    display_name            VARCHAR(100) NOT NULL,
    is_terminal             BOOLEAN NOT NULL DEFAULT FALSE,
    is_editable_by_creator  BOOLEAN NOT NULL DEFAULT FALSE,
    display_order           INTEGER NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_workflow_statuses_display_order CHECK (display_order >= 0)
);

CREATE TABLE workflow_actions (
    id                SERIAL PRIMARY KEY,
    name              VARCHAR(60) NOT NULL UNIQUE,
    display_name      VARCHAR(120) NOT NULL,
    comment_required  BOOLEAN NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE
);


-- =====================================================================
-- Baslangic seed'i - SS6.4 ve SS6.5 tablolariyla birebir.
-- =====================================================================

INSERT INTO workflow_statuses (name, display_name, is_terminal, is_editable_by_creator, display_order) VALUES
    ('TASLAK',                'Taslak',                              FALSE, TRUE,  10),
    ('BSK_YRD_INCELEMESINDE', 'Başkan Yardımcısı İncelemesinde',     FALSE, FALSE, 20),
    ('BASKAN_INCELEMESINDE',  'Başkan İncelemesinde',                FALSE, FALSE, 30),
    ('DUZENLEME_BEKLIYOR',    'Düzenleme Bekliyor',                   FALSE, TRUE,  40),
    ('ONAYLANDI',             'Onaylandı',                            TRUE,  FALSE, 50),
    ('REDDEDILDI',            'Reddedildi',                           TRUE,  FALSE, 60);

INSERT INTO workflow_actions (name, display_name, comment_required) VALUES
    ('GONDER',                          'Gönder',                          FALSE),
    ('TEKRAR_GONDER',                   'Tekrar Gönder',                   FALSE),
    ('BASKANA_ILET',                    'Başkana İlet',                    FALSE),
    ('CALISANA_GERI_GONDER',            'Çalışana Geri Gönder',            TRUE),
    ('BASKAN_YARDIMCISINA_GERI_GONDER', 'Başkan Yardımcısına Geri Gönder', TRUE),
    ('ONAYLA',                          'Onayla',                          FALSE),
    ('REDDET',                          'Reddet',                          TRUE);