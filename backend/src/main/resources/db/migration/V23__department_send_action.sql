-- =====================================================================
-- EBYS - Departman hedefli gonderim: DEPARTMENT stratejisi + DEPARTMANA_GONDER
--
-- Kaynak: docs/decisions/0006-departman-hedefli-target-strategy.md (Kabul
-- Edildi, 4 Eylul 2026), karar bolumu S1/S4 + "Kabul kaydi" tablosu.
--
-- Mevcut 8 gecis ve GONDER davranisi DEGISMEZ (ADR'nin baglayici kisiti,
-- DB-1 SS16). Bu migration yalniz EKLEME yapar:
--   1. chk_transition_target_strategy / _role kisitlarina DEPARTMENT eklenir
--   2. workflow_actions'a DEPARTMANA_GONDER satiri eklenir
--   3. workflow_transitions'a 2 yeni satir eklenir (TASLAK ve
--      DUZENLEME_BEKLIYOR'dan, ikisi de BSK_YRD_INCELEMESINDE'ye)
--
-- V15 duzenlenmiyor (Flyway kurali) - bu SS13.1'e uygun ileri migration.
-- =====================================================================

-- Adim 1: CHECK kisitlarini DEPARTMENT'i kapsayacak sekilde yeniden olustur.
-- DEPARTMENT, NONE ile ayni kuralı tasir: expected_target_role_id NULL olmali
-- (hedef rol degil, department_routing_rules uzerinden departman ici cozulur).

ALTER TABLE workflow_transitions
    DROP CONSTRAINT chk_transition_target_strategy;

ALTER TABLE workflow_transitions
    ADD CONSTRAINT chk_transition_target_strategy CHECK (target_strategy IN (
        'NONE', 'ROLE', 'CREATOR', 'CURRENT_ASSIGNEE', 'PREVIOUS_ACTOR', 'DEPARTMENT'
    ));

ALTER TABLE workflow_transitions
    DROP CONSTRAINT chk_transition_target_strategy_role;

ALTER TABLE workflow_transitions
    ADD CONSTRAINT chk_transition_target_strategy_role CHECK (
        (target_strategy = 'NONE'       AND expected_target_role_id IS NULL) OR
        (target_strategy = 'DEPARTMENT' AND expected_target_role_id IS NULL) OR
        (target_strategy = 'ROLE'       AND expected_target_role_id IS NOT NULL) OR
        (target_strategy IN ('CREATOR', 'CURRENT_ASSIGNEE', 'PREVIOUS_ACTOR'))
    );

-- Adim 2: Yeni aksiyon. ADR karari: comment_required = FALSE (GONDER ile ayni).

INSERT INTO workflow_actions (name, display_name, comment_required) VALUES
    ('DEPARTMANA_GONDER', 'Departmana Gönder', FALSE);

-- Adim 3: ADR-0006 "Karar S4 -> 1" tablosundaki 2 satirin birebir karsiligi.
-- Gerekli permission RECORD_FORWARD (GONDER ile ayni), expected_target_role_id
-- NULL - hedef rol department_routing_rules'tan (WF-6) calisma zamaninda cozulur.

INSERT INTO workflow_transitions (
    from_status_id, action_id, actor_role_id, actor_requirement,
    to_status_id, expected_target_role_id, target_strategy, required_permission_id
)
SELECT
    fs.id, a.id, ar.id, v.actor_requirement,
    ts.id, NULL, 'DEPARTMENT', p.id
FROM (VALUES
    ('TASLAK',             'DEPARTMANA_GONDER', 'CALISAN', 'CREATOR',              'BSK_YRD_INCELEMESINDE'),
    ('DUZENLEME_BEKLIYOR', 'DEPARTMANA_GONDER', 'CALISAN', 'CREATOR_AND_ASSIGNEE', 'BSK_YRD_INCELEMESINDE')
) AS v(from_name, action_name, actor_key, actor_requirement, to_name)
JOIN workflow_statuses fs ON fs.name        = v.from_name
JOIN workflow_actions  a  ON a.name         = v.action_name
JOIN roles              ar ON ar.system_key = v.actor_key
JOIN workflow_statuses ts ON ts.name        = v.to_name
JOIN permissions        p  ON p.code        = 'RECORD_FORWARD';