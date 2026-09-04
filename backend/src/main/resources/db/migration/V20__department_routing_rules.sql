-- =====================================================================
-- EBYS - department_routing_rules (TASLAK)
--
-- Kaynak: WORKFLOW_V1_V2_PLANI.md SS11, SS18 (Satin Alma ornegi)
-- Amac: Bir kayit departmana atandiginda, belirli bir (durum, aksiyon)
-- kombinasyonunda o departmanin hangi roldeki uyesinin islem
-- yapabilecegini tanimlar.
--
-- "Hukuk + BSK_YRD_INCELEMESINDE + BASKANA_ILET -> HUKUK_UZMANI" ornegi
-- (plan SS11) bu tabloya birebir karsilik gelir.
--
-- ONEMLI: Bu TASLAKTIR. Burak'in (WF-6, DepartmentRoutingResolver)
-- final routing semantigini onaylamasi bekleniyor - roadmap'te acikca
-- "DB-12 routing | Alperen | Burak'in final routing semantics kontrolu"
-- olarak isaretli. Sema burada uygulanip test edilir, ama Burak'in
-- onayindan sonra kolon/kisit degisebilir.
-- =====================================================================

CREATE TABLE department_routing_rules (
    id               SERIAL PRIMARY KEY,
    department_id    INT NOT NULL,
    from_status_id   INT NOT NULL,
    action_id        INT NOT NULL,
    -- Bu departmanda, bu durum+aksiyon icin islem yapmaya yetkili rol.
    -- workflow_transitions.actor_role_id ile AYNI semantik: rol
    -- yetkili olsa da kullanicinin departmana uye VE aktif olmasi ayrica
    -- aranir (uyelik kontrolu DepartmentMemberRepository'de).
    target_role_id   INT NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_routing_department    FOREIGN KEY (department_id)  REFERENCES departments (id)         ON DELETE CASCADE,
    CONSTRAINT fk_routing_from_status   FOREIGN KEY (from_status_id) REFERENCES workflow_statuses (id)    ON DELETE RESTRICT,
    CONSTRAINT fk_routing_action        FOREIGN KEY (action_id)      REFERENCES workflow_actions (id)     ON DELETE RESTRICT,
    CONSTRAINT fk_routing_target_role   FOREIGN KEY (target_role_id) REFERENCES roles (id)                ON DELETE RESTRICT,

    -- Ayni departman+durum+aksiyon icin birden fazla hedef rol tanimlanamaz.
    CONSTRAINT uq_routing_dept_status_action UNIQUE (department_id, from_status_id, action_id)
);

CREATE INDEX idx_routing_from_status ON department_routing_rules (from_status_id);
CREATE INDEX idx_routing_action      ON department_routing_rules (action_id);
CREATE INDEX idx_routing_target_role ON department_routing_rules (target_role_id);

-- department_id zaten CASCADE - departman silinirse (kullanimda degilse)
-- routing kurallari da gider. from_status/action/role RESTRICT - workflow
-- kataloğunun bir parcasi silinmeye calisilirsa routing kurali onu engeller.