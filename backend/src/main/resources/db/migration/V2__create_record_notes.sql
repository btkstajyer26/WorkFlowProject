-- Kayit notlari audit loglarindan ayridir: her kullanici bir kayitta tek not
-- tutar ve bu notu kayit sonuclanana kadar guncelleyebilir.
CREATE TABLE record_notes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id      UUID NOT NULL,
    author_id      UUID NOT NULL,
    -- Not yazildigi/guncellendigi andaki rol, ekranda baglam gostermek icin
    -- ayrica saklanir.
    author_role_id INT NOT NULL,
    body           TEXT NOT NULL,
    -- Ayni notun es zamanli guncellemelerinde optimistic locking icindir.
    version        INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_record_notes_record_author UNIQUE (record_id, author_id),
    CONSTRAINT chk_record_notes_body_not_blank CHECK (btrim(body) <> ''),
    CONSTRAINT fk_record_note_record FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE,
    CONSTRAINT fk_record_note_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_record_note_role   FOREIGN KEY (author_role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_record_notes_record_id ON record_notes (record_id);
CREATE INDEX idx_record_notes_author_id ON record_notes (author_id);
