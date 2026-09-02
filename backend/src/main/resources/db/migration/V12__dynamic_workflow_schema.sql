-- =====================================================================
-- EBYS - Dinamik Workflow Semasi (TASLAK v2 - DB-1)
--
-- Amac: records.status kolonundaki sabit CHECK listesini ve Java
-- tarafinda TransitionRules.java icinde hardcoded olan 8 gecis kuralini
-- veritabanina tasimak.
--
-- Bu dosya HENUZ UYGULANMAMIS bir TASLAKTIR. Port imzasi toplantisinda
-- (Cars 2 Eylul) uzerinde konusulup kesinlestikten sonra gercek
-- V12__dynamic_workflow_schema.sql olarak yazilacak.
--
-- ISARETLI ACIK SORULAR asagida [TOPLANTIDA KARAR] etiketiyle gecer.
-- =====================================================================


-- =====================================================================
-- 1. WORKFLOW_STATUSES
--
-- records.status bugun VARCHAR + CHECK ile sabitlenmis 6 deger tutuyor
-- (bkz. V1, chk_records_status). Bu tablo o listeyi veri haline getirir.
-- DB-5'te chk_records_status kaldirilip bu tabloya FK baglanacak; ancak
-- records.status kolonu VARCHAR olarak kalacak (status_id'ye donusmuyor).
-- =====================================================================

CREATE TABLE workflow_statuses (
    id            SERIAL PRIMARY KEY,
    -- V1'deki chk_records_status listesiyle BIREBIR ayni olmali.
    name          VARCHAR(50) NOT NULL UNIQUE,
    description   VARCHAR(255),
    -- ONAYLANDI ve REDDEDILDI'nin disina gecis yok (RULES listesinde
    -- bu ikisi hicbir kuralin "from" alani olarak gecmiyor).
    is_terminal   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_workflow_statuses_name ON workflow_statuses (name);


-- =====================================================================
-- 2. WORKFLOW_ACTIONS
--
-- TransitionRules.java'daki WorkflowAction enum degerlerinin birebir
-- karsiligi (7 aksiyon).
-- =====================================================================

CREATE TABLE workflow_actions (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE INDEX idx_workflow_actions_name ON workflow_actions (name);


-- =====================================================================
-- 3. WORKFLOW_TRANSITIONS
--
-- (durum, aksiyon, rol) uclusunun hangi yeni duruma gectigini tanimlar.
-- actor_requirement, TransitionRules.java'daki ActorRequirement enum'unun
-- karsiligidir: rol yetkili olsa bile kullanicinin kayitla iliskisi
-- (yaratici / atanan / ikisi birden) de kontrol edilir. records.status
-- gibi bu da kucuk, sabit bir enum oldugu icin ayri tabloya cikarilmadan
-- VARCHAR + CHECK olarak tutuluyor.
-- [TOPLANTIDA KARAR: requires_comment kolonu asagida var ama hangi
-- gecislerin yorum zorunlu tuttugu bu dosyada gorunmuyor - muhtemelen
-- WorkflowTransitionValidator veya baska bir siniftadir, o dosya da
-- incelenmeli.]
-- =====================================================================

CREATE TABLE workflow_transitions (
    id                 SERIAL PRIMARY KEY,
    from_status_id     INT NOT NULL,
    action_id          INT NOT NULL,
    role_id            INT NOT NULL,
    to_status_id       INT NOT NULL,
    actor_requirement  VARCHAR(30) NOT NULL,
    requires_comment   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_transition_from_status FOREIGN KEY (from_status_id) REFERENCES workflow_statuses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_action      FOREIGN KEY (action_id)      REFERENCES workflow_actions (id)  ON DELETE RESTRICT,
    CONSTRAINT fk_transition_role        FOREIGN KEY (role_id)        REFERENCES roles (id)             ON DELETE RESTRICT,
    CONSTRAINT fk_transition_to_status   FOREIGN KEY (to_status_id)   REFERENCES workflow_statuses (id) ON DELETE RESTRICT,
    CONSTRAINT uq_transition_from_action_role UNIQUE (from_status_id, action_id, role_id),
    CONSTRAINT chk_transition_actor_requirement CHECK (actor_requirement IN (
        'CREATOR',
        'ASSIGNEE',
        'CREATOR_AND_ASSIGNEE'
    ))
);

CREATE INDEX idx_workflow_transitions_from_status ON workflow_transitions (from_status_id);
CREATE INDEX idx_workflow_transitions_action      ON workflow_transitions (action_id);
CREATE INDEX idx_workflow_transitions_role        ON workflow_transitions (role_id);


-- =====================================================================
-- 4. BASLANGIC VERISI
-- =====================================================================

INSERT INTO workflow_statuses (name, description, is_terminal) VALUES
    ('TASLAK',                  'Calisan tarafindan olusturulmus, henuz gonderilmemis', FALSE),
    ('BSK_YRD_INCELEMESINDE',   'Baskan Yardimcisi incelemesinde',                       FALSE),
    ('BASKAN_INCELEMESINDE',    'Baskan incelemesinde',                                  FALSE),
    ('DUZENLEME_BEKLIYOR',      'Geri gonderilmis, duzenleme bekliyor',                  FALSE),
    ('ONAYLANDI',               'Nihai onay verilmis',                                   TRUE),
    ('REDDEDILDI',              'Reddedilmis',                                           TRUE);

INSERT INTO workflow_actions (name, description) VALUES
    ('GONDER',                          'Taslagi ilk kez incelemeye gonderir'),
    ('TEKRAR_GONDER',                   'Duzenleme sonrasi yeniden gonderir'),
    ('BASKANA_ILET',                    'Baskan Yardimcisi kaydi Baskana iletir'),
    ('CALISANA_GERI_GONDER',            'Kaydi duzenleme icin Calisana geri gonderir'),
    ('ONAYLA',                          'Baskan kaydi nihai onaylar'),
    ('REDDET',                          'Baskan kaydi reddeder'),
    ('BASKAN_YARDIMCISINA_GERI_GONDER', 'Baskan kaydi Baskan Yardimcisina geri gonderir');

-- TransitionRules.java RULES listesindeki 8 kuralin birebir karsiligi.
-- requires_comment degerleri [TOPLANTIDA KARAR] - simdilik hepsi FALSE,
-- WorkflowTransitionValidator incelenip REDDET / CALISANA_GERI_GONDER
-- gibi aksiyonlarin gercekten yorum zorunlu tutup tutmadigi teyit
-- edilmeli.

INSERT INTO workflow_transitions (from_status_id, action_id, role_id, to_status_id, actor_requirement, requires_comment)
SELECT fs.id, a.id, r.id, ts.id, v.actor_requirement, FALSE
FROM (VALUES
    ('TASLAK',                'GONDER',                          'CALISAN',            'CREATOR',              'BSK_YRD_INCELEMESINDE'),
    ('DUZENLEME_BEKLIYOR',    'TEKRAR_GONDER',                   'CALISAN',            'CREATOR_AND_ASSIGNEE', 'BSK_YRD_INCELEMESINDE'),
    ('BSK_YRD_INCELEMESINDE', 'BASKANA_ILET',                    'BASKAN_YARDIMCISI',  'ASSIGNEE',              'BASKAN_INCELEMESINDE'),
    ('BSK_YRD_INCELEMESINDE', 'CALISANA_GERI_GONDER',            'BASKAN_YARDIMCISI',  'ASSIGNEE',              'DUZENLEME_BEKLIYOR'),
    ('BASKAN_INCELEMESINDE',  'ONAYLA',                          'BASKAN',             'ASSIGNEE',              'ONAYLANDI'),
    ('BASKAN_INCELEMESINDE',  'REDDET',                          'BASKAN',             'ASSIGNEE',              'REDDEDILDI'),
    ('BASKAN_INCELEMESINDE',  'CALISANA_GERI_GONDER',            'BASKAN',             'ASSIGNEE',              'DUZENLEME_BEKLIYOR'),
    ('BASKAN_INCELEMESINDE',  'BASKAN_YARDIMCISINA_GERI_GONDER', 'BASKAN',             'ASSIGNEE',              'BSK_YRD_INCELEMESINDE')
) AS v(from_name, action_name, role_name, actor_requirement, to_name)
JOIN workflow_statuses fs ON fs.name = v.from_name
JOIN workflow_actions  a  ON a.name  = v.action_name
JOIN roles              r  ON r.name  = v.role_name
JOIN workflow_statuses ts ON ts.name = v.to_name;