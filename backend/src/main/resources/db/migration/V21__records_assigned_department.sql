-- =====================================================================
-- EBYS - records.assigned_department_id (TASLAK)
--
-- Kaynak: WORKFLOW_V1_V2_PLANI.md SS10
-- Amac: Bir kaydin kisiye DEGIL, bir departmana atanabilmesi. Ikisi
-- ayni anda dolu olamaz - tam olarak biri (ya da terminal/TASLAK
-- durumda ikisi de) dolu olabilir.
--
-- ONEMLI: Bu TASLAKTIR. ADR-0006 (departman hedefli gonderim karari)
-- Burak'in V1 kritik isi ve henuz kapanmadi - roadmap acikca "DB-13
-- assigned_department | Alperen | ADR-0006 final write semantics"
-- olarak isaretliyor. Kolon/FK/CHECK burada hazirlaniyor, ama final
-- yazma semantigi (hangi transition bu kolonu ne zaman yazar/temizler)
-- ADR-0006 kapanana kadar KESIN degil.
-- =====================================================================

ALTER TABLE records
    ADD COLUMN assigned_department_id INT;

ALTER TABLE records
    ADD CONSTRAINT fk_records_assigned_department
        FOREIGN KEY (assigned_department_id)
        REFERENCES departments (id)
        ON DELETE RESTRICT;

-- Plan SS10'daki invariant birebir: kisiye VE departmana ayni anda
-- atanmis bir kayit olamaz. TASLAK, ONAYLANDI, REDDEDILDI gibi hicbir
-- atamanin olmadigi durumlarda ikisi de NULL kalabilir - CHECK bunu
-- yasaklamiyor, yalniz "ikisi birden dolu" durumunu yasakliyor.
ALTER TABLE records
    ADD CONSTRAINT chk_records_assignment_exclusive
        CHECK (assigned_to IS NULL OR assigned_department_id IS NULL);

CREATE INDEX idx_records_assigned_department_id ON records (assigned_department_id);