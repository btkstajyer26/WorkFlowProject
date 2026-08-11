ALTER TABLE files
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID,
    ADD CONSTRAINT fk_file_deleted_by FOREIGN KEY (deleted_by) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_files_not_deleted ON files (record_id) WHERE deleted_at IS NULL;
