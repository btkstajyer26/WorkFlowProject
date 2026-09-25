-- =====================================================================
-- EBYS - workflow_transitions
--
-- Kaynak: DB_1_VERI_MODELI_SOZLESMESI.md SS6.6 / SS7 / SS8
-- (durum, aksiyon, aktor-rolu) uclusunun hangi hedef duruma gectigini,
-- hedef kullanicinin nasil cozulecegini (target_strategy) ve gerekli
-- permission'i tek satirda tanimlar.
-- =====================================================================

CREATE TABLE workflow_transitions (
    id                        SERIAL PRIMARY KEY,
    from_status_id            INT NOT NULL,
    action_id                 INT NOT NULL,
    actor_role_id             INT NOT NULL,
    -- Rol yetkili olsa bile aktorun kayitla iliskisi ayrica kontrol edilir.
    actor_requirement         VARCHAR(40) NOT NULL,
    to_status_id              INT NOT NULL,
    -- Yalniz target_strategy = 'ROLE' oldugunda dolu olmasi ZORUNLU;
    -- diger stratejilerde bilgi amacli doldurulabilir (bkz. asagidaki
    -- CHECK - sadece ROLE icin NOT NULL, NONE icin NULL zorunlu tutar).
    expected_target_role_id   INT,
    -- Hedef kullanicinin cozum primitive'i: NONE / ROLE / CREATOR /
    -- CURRENT_ASSIGNEE / PREVIOUS_ACTOR. bkz. SS7.2.
    target_strategy           VARCHAR(40) NOT NULL,
    -- 8 aktif gecisin hepsinde dolu olmasi kabul kriteri (SS17); NULL'a
    -- izin verilmesi yalniz kontrollu gelecek genisleme icin.
    required_permission_id    INT,
    is_active                 BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_transition_from_status  FOREIGN KEY (from_status_id)          REFERENCES workflow_statuses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_to_status    FOREIGN KEY (to_status_id)            REFERENCES workflow_statuses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_action       FOREIGN KEY (action_id)               REFERENCES workflow_actions (id)  ON DELETE RESTRICT,
    CONSTRAINT fk_transition_actor_role   FOREIGN KEY (actor_role_id)           REFERENCES roles (id)             ON DELETE RESTRICT,
    CONSTRAINT fk_transition_target_role  FOREIGN KEY (expected_target_role_id) REFERENCES roles (id)             ON DELETE RESTRICT,
    CONSTRAINT fk_transition_permission   FOREIGN KEY (required_permission_id)  REFERENCES permissions (id)       ON DELETE RESTRICT,

    CONSTRAINT uq_transition_from_action_role UNIQUE (from_status_id, action_id, actor_role_id),

    CONSTRAINT chk_transition_actor_requirement CHECK (actor_requirement IN (
        'CREATOR', 'ASSIGNEE', 'CREATOR_AND_ASSIGNEE'
    )),

    CONSTRAINT chk_transition_target_strategy CHECK (target_strategy IN (
        'NONE', 'ROLE', 'CREATOR', 'CURRENT_ASSIGNEE', 'PREVIOUS_ACTOR'
    )),

    -- NONE -> hedef rol yok. ROLE -> hedef rol zorunlu. Digerleri serbest.
    CONSTRAINT chk_transition_target_strategy_role CHECK (
        (target_strategy = 'NONE' AND expected_target_role_id IS NULL) OR
        (target_strategy = 'ROLE' AND expected_target_role_id IS NOT NULL) OR
        (target_strategy IN ('CREATOR', 'CURRENT_ASSIGNEE', 'PREVIOUS_ACTOR'))
    )
);

CREATE INDEX idx_workflow_transitions_action           ON workflow_transitions (action_id);
CREATE INDEX idx_workflow_transitions_actor_role        ON workflow_transitions (actor_role_id);
CREATE INDEX idx_workflow_transitions_to_status         ON workflow_transitions (to_status_id);
CREATE INDEX idx_workflow_transitions_expected_target   ON workflow_transitions (expected_target_role_id);
CREATE INDEX idx_workflow_transitions_required_permission ON workflow_transitions (required_permission_id);


-- =====================================================================
-- Baglayici 8 gecis seed'i - sozlesme SS8 tablosuyla birebir.
-- expected_target_role_id: yalniz target_strategy='ROLE' icin FK kisiti
-- geregi zorunlu; CREATOR/PREVIOUS_ACTOR satirlarinda da SS8 tablosundaki
-- "beklenen hedef rol" bilgisi amacli dolduruldu (CHECK bunu yasaklamiyor).
-- NONE satirlarinda (ONAYLA/REDDET) NULL.
-- =====================================================================

INSERT INTO workflow_transitions (
    from_status_id, action_id, actor_role_id, actor_requirement,
    to_status_id, expected_target_role_id, target_strategy, required_permission_id
)
SELECT
    fs.id, a.id, ar.id, v.actor_requirement,
    ts.id, tr.id, v.target_strategy, p.id
FROM (VALUES
    -- from                    action                              actor_key            actor_requirement        to                        target_strategy    target_key            permission_code
    ('TASLAK',                'GONDER',                          'CALISAN',           'CREATOR',               'BSK_YRD_INCELEMESINDE', 'ROLE',            'BASKAN_YARDIMCISI', 'RECORD_FORWARD'),
    ('DUZENLEME_BEKLIYOR',    'TEKRAR_GONDER',                   'CALISAN',           'CREATOR_AND_ASSIGNEE',  'BSK_YRD_INCELEMESINDE', 'ROLE',            'BASKAN_YARDIMCISI', 'RECORD_FORWARD'),
    ('BSK_YRD_INCELEMESINDE', 'BASKANA_ILET',                    'BASKAN_YARDIMCISI', 'ASSIGNEE',              'BASKAN_INCELEMESINDE',  'ROLE',            'BASKAN',             'RECORD_FORWARD'),
    ('BSK_YRD_INCELEMESINDE', 'CALISANA_GERI_GONDER',            'BASKAN_YARDIMCISI', 'ASSIGNEE',              'DUZENLEME_BEKLIYOR',    'CREATOR',         'CALISAN',            'RECORD_RETURN'),
    ('BASKAN_INCELEMESINDE',  'ONAYLA',                          'BASKAN',            'ASSIGNEE',              'ONAYLANDI',              'NONE',            NULL,                 'RECORD_APPROVE'),
    ('BASKAN_INCELEMESINDE',  'REDDET',                          'BASKAN',            'ASSIGNEE',              'REDDEDILDI',             'NONE',            NULL,                 'RECORD_REJECT'),
    ('BASKAN_INCELEMESINDE',  'CALISANA_GERI_GONDER',            'BASKAN',            'ASSIGNEE',              'DUZENLEME_BEKLIYOR',    'CREATOR',         'CALISAN',            'RECORD_RETURN'),
    ('BASKAN_INCELEMESINDE',  'BASKAN_YARDIMCISINA_GERI_GONDER', 'BASKAN',            'ASSIGNEE',              'BSK_YRD_INCELEMESINDE', 'PREVIOUS_ACTOR',  'BASKAN_YARDIMCISI', 'RECORD_RETURN')
) AS v(from_name, action_name, actor_key, actor_requirement, to_name, target_strategy, target_key, permission_code)
JOIN workflow_statuses fs ON fs.name        = v.from_name
JOIN workflow_actions  a  ON a.name         = v.action_name
JOIN roles              ar ON ar.system_key = v.actor_key
JOIN workflow_statuses ts ON ts.name        = v.to_name
JOIN permissions        p  ON p.code        = v.permission_code
LEFT JOIN roles          tr ON tr.system_key = v.target_key;