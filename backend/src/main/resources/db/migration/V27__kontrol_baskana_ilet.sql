-- =====================================================================
-- KONTROL'den Baskana iletme (ADR-0010 tamamlanmasi)
-- =====================================================================
--
-- V26, Parent'i tum alt gorevler sonuclandiginda KONTROL'e otomatik
-- ilerletiyordu ama oradan cikis hic tanimlanmamisti - KONTROL bir
-- olu uctu. Bu gecis mevcut BASKANA_ILET aksiyonunu yeniden kullanir;
-- bolme sonrasi assigned_to bos kaldigi icin (target_strategy=NONE)
-- ASSIGNEE/CREATOR kurallari hic kimseyi eslestiremez, bu yuzden yeni
-- ROLE_ONLY aktor gereksinimi eklendi: rolu tutan herhangi bir Baskan
-- Yardimcisi iletebilir, DUZENLEME_BEKLIYOR kuyrugundaki rol-geneli
-- gorunurlukle ayni mantik.

ALTER TABLE workflow_transitions
    DROP CONSTRAINT chk_transition_actor_requirement;

ALTER TABLE workflow_transitions
    ADD CONSTRAINT chk_transition_actor_requirement CHECK (actor_requirement IN (
        'CREATOR', 'ASSIGNEE', 'CREATOR_AND_ASSIGNEE', 'SYSTEM', 'ROLE_ONLY'
    ));

INSERT INTO workflow_transitions (
    from_status_id, action_id, actor_role_id, actor_requirement,
    to_status_id, expected_target_role_id, target_strategy,
    required_permission_id, is_active
)
SELECT
    fs.id, a.id, ar.id, 'ROLE_ONLY',
    ts.id, tr.id, 'ROLE', p.id, TRUE
FROM workflow_statuses fs
JOIN workflow_actions a ON a.name = 'BASKANA_ILET'
JOIN roles ar ON ar.system_key = 'BASKAN_YARDIMCISI'
JOIN workflow_statuses ts ON ts.name = 'BASKAN_INCELEMESINDE'
JOIN roles tr ON tr.system_key = 'BASKAN'
JOIN permissions p ON p.code = 'RECORD_FORWARD'
WHERE fs.name = 'KONTROL';
