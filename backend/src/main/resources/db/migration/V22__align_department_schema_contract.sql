-- Align the published department schema with DB-1 sections 15.1-15.3.
-- V18-V21 are preserved, including their historical comments/checksums.
-- PostgreSQL/Flyway runs this migration transactionally. If existing rows
-- reference themselves as parent, the CHECK validation fails and all changes
-- below roll back; no department, membership, routing or assignment is repaired
-- or deleted implicitly.

ALTER TABLE departments
    ALTER COLUMN name TYPE VARCHAR(150);

ALTER TABLE departments
    ADD CONSTRAINT chk_department_parent_not_self
        CHECK (parent_department_id IS NULL OR parent_department_id <> id);

ALTER TABLE department_members
    DROP CONSTRAINT fk_department_member_department,
    DROP CONSTRAINT fk_department_member_user,
    ADD CONSTRAINT fk_department_member_department
        FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_department_member_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE department_routing_rules
    DROP CONSTRAINT fk_routing_department,
    ADD CONSTRAINT fk_routing_department
        FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT;
