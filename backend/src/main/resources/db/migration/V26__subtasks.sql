-- =====================================================================
-- Parent/Subtask foundation (ADR-0010)
-- =====================================================================

ALTER TABLE records
    ADD COLUMN subtask_approval_policy     VARCHAR(20),
    ADD COLUMN subtask_required_approvals  INTEGER;

ALTER TABLE records
    ADD CONSTRAINT chk_records_subtask_approval_policy CHECK (
        subtask_approval_policy IS NULL
        OR subtask_approval_policy IN ('UNANIMOUS', 'MAJORITY')
    ),
    ADD CONSTRAINT chk_records_subtask_required_approvals CHECK (
        subtask_required_approvals IS NULL OR subtask_required_approvals > 0
    ),
    ADD CONSTRAINT chk_records_subtask_approval_fields CHECK (
        (subtask_approval_policy IS NULL AND subtask_required_approvals IS NULL)
        OR
        (subtask_approval_policy IS NOT NULL AND subtask_required_approvals IS NOT NULL)
    );

CREATE TABLE subtasks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_record_id    UUID NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    assigned_to         UUID NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DEGERLENDIRME',
    created_by          UUID NOT NULL,
    resolution_comment  TEXT,
    version             INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP,
    completed_at        TIMESTAMP,

    CONSTRAINT fk_subtasks_parent_record FOREIGN KEY (parent_record_id)
        REFERENCES records (id),
    CONSTRAINT fk_subtasks_assigned_to FOREIGN KEY (assigned_to)
        REFERENCES users (id),
    CONSTRAINT fk_subtasks_created_by FOREIGN KEY (created_by)
        REFERENCES users (id),
    CONSTRAINT chk_subtasks_status CHECK (status IN (
        'DEGERLENDIRME', 'ISLEM', 'ONAY', 'TAMAMLANDI', 'REDDEDILDI'
    ))
);

CREATE INDEX idx_subtasks_parent_record_id ON subtasks (parent_record_id);
CREATE INDEX idx_subtasks_assigned_to ON subtasks (assigned_to);

INSERT INTO workflow_statuses (
    name, display_name, is_terminal, is_editable_by_creator, display_order, is_active
) VALUES
    ('ALT_GOREV_BEKLIYOR', 'Alt Görevler Bekleniyor', FALSE, FALSE, 70, TRUE),
    ('KONTROL',             'Kontrol',                 FALSE, FALSE, 80, TRUE);

INSERT INTO workflow_actions (name, display_name, comment_required, is_active) VALUES
    ('ALT_GOREVLERE_AYIR',       'Alt Görevlere Ayır',       FALSE, TRUE),
    ('ALT_GOREVLER_SONUCLANDI',  'Alt Görevler Sonuçlandı',  FALSE, TRUE);

INSERT INTO roles (
    name, description, system_key, is_system, is_workflow_actor, max_users, is_active
) VALUES (
    'SISTEM', 'Uygulama içi otomatik workflow geçişlerini çalıştıran servis rolü',
    'SISTEM', TRUE, TRUE, 1, TRUE
);

-- Parola rastgele uretilip atilmistir; acik metni migration'da veya loglarda yoktur.
-- .invalid adresi gercek posta teslimini onler; seed hesabinin bilinen giris bilgisi yoktur.
INSERT INTO users (
    first_name, last_name, email, password_hash, role_id,
    is_active, must_change_password
)
SELECT
    'Workflow', 'Sistem', 'workflow-system@system.invalid',
    '$2y$12$J2xLUnyF9q/DJR67HX1keOjSh1JT06GVZUsG.OkDp6g9Gwx5INlnK',
    r.id, TRUE, FALSE
FROM roles r
WHERE r.system_key = 'SISTEM';

ALTER TABLE workflow_transitions
    DROP CONSTRAINT chk_transition_actor_requirement;

ALTER TABLE workflow_transitions
    ADD CONSTRAINT chk_transition_actor_requirement CHECK (actor_requirement IN (
        'CREATOR', 'ASSIGNEE', 'CREATOR_AND_ASSIGNEE', 'SYSTEM'
    ));

INSERT INTO workflow_transitions (
    from_status_id, action_id, actor_role_id, actor_requirement,
    to_status_id, expected_target_role_id, target_strategy,
    required_permission_id, is_active
)
SELECT
    fs.id, a.id, ar.id, 'ASSIGNEE',
    ts.id, NULL, 'NONE', p.id, TRUE
FROM workflow_statuses fs
JOIN workflow_actions a ON a.name = 'ALT_GOREVLERE_AYIR'
JOIN roles ar ON ar.system_key = 'BASKAN_YARDIMCISI'
JOIN workflow_statuses ts ON ts.name = 'ALT_GOREV_BEKLIYOR'
JOIN permissions p ON p.code = 'RECORD_FORWARD'
WHERE fs.name = 'BSK_YRD_INCELEMESINDE';

INSERT INTO workflow_transitions (
    from_status_id, action_id, actor_role_id, actor_requirement,
    to_status_id, expected_target_role_id, target_strategy,
    required_permission_id, is_active
)
SELECT
    fs.id, a.id, ar.id, 'SYSTEM',
    ts.id, NULL, 'NONE', NULL, TRUE
FROM workflow_statuses fs
JOIN workflow_actions a ON a.name = 'ALT_GOREVLER_SONUCLANDI'
JOIN roles ar ON ar.system_key = 'SISTEM'
JOIN workflow_statuses ts ON ts.name = 'KONTROL'
WHERE fs.name = 'ALT_GOREV_BEKLIYOR';
