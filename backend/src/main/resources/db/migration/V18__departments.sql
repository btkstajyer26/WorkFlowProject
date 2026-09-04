-- =====================================================================
-- EBYS - departments
--
-- Kaynak: ADR-0005 (Kabul Edildi) + WORKFLOW_V1_V2_PLANI.md SS10, SS14
-- Amac: Kullanicilarin uye olabilecegi organizasyon gruplari / is
-- kuyruklari. V1'de hiyerarsi yalniz YAPISAL bilgidir - uygun kullanici
-- yoksa ust departmana otomatik eskalasyon YAPILMAZ (plan SS14).
-- =====================================================================

CREATE TABLE departments (
    id                     SERIAL PRIMARY KEY,
    name                   VARCHAR(100) NOT NULL UNIQUE,
    -- NULL = kok departman. Kendine referans, cok seviyeli hiyerarsi
    -- kurabilir. V1'de yalniz goruntuleme/yapi amacli - routing bu
    -- alani atlayarak calismaz.
    parent_department_id   INT,
    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_department_id)
        REFERENCES departments (id) ON DELETE RESTRICT
);

-- Bir departman silinmeden/pasiflestirilmeden alt departmanlarin
-- durumu kontrol edilebilsin diye RESTRICT (CASCADE ile alt agac
-- sessizce silinmez).

CREATE INDEX idx_departments_parent    ON departments (parent_department_id);
CREATE INDEX idx_departments_is_active ON departments (is_active);