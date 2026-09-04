-- =====================================================================
-- EBYS - department_members
--
-- Kaynak: WORKFLOW_V1_V2_PLANI.md SS5.3
-- Bir kullanici birden fazla departmana uye olabilir. Uyelik rolu
-- yoktur - kullanicinin rolu users.role_id uzerinden GLOBAL cozulur
-- (plan SS5.3: "Uyeligin kendi rolu yoktur").
-- =====================================================================

CREATE TABLE department_members (
    department_id INT  NOT NULL,
    user_id       UUID NOT NULL,
    PRIMARY KEY (department_id, user_id),
    CONSTRAINT fk_department_member_department FOREIGN KEY (department_id)
        REFERENCES departments (id) ON DELETE CASCADE,
    CONSTRAINT fk_department_member_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- Birlesik PK yalniz department_id ile baslayan sorguyu (bir departmanin
-- uyeleri) verimli karsilar; ters yon (bir kullanicinin uye oldugu
-- departmanlar - "Kayitlarim" gibi kisisel görünüm sorguları için) ayri
-- indeks gerektirir.
CREATE INDEX idx_department_members_user_id ON department_members (user_id);