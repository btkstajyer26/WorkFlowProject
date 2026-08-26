-- E-posta üzerinden tek tıkla workflow aksiyonu için tek kullanımlık anahtar.
--
-- Neden ayrı tablo: password_reset_codes iki aşamalı (kod -> anahtar) bir akışı
-- ve deneme sayacını modelliyor; burada tek aşama var ama anahtarın hangi
-- evrağa ve hangi aksiyona bağlı olduğu tutulmalı. İki model birbirine
-- sığmadığı için ayrıldılar. Anahtar üretimi ve özet alma yöntemi aynıdır:
-- 256 bit rastgele değerin SHA-256 özeti saklanır (tek yönlü ama sorgulanabilir).
--
-- Anahtarın kendisi hiçbir yerde saklanmaz; yalnız e-posta gövdesinde gider.
--
-- Üç sınır birlikte çalışır:
--   1) consumed_at  -> tek kullanım
--   2) expires_at   -> süre
--   3) user_id + record_id + action -> anahtar yalnız bir kişinin, bir evrakta,
--      bir aksiyonu için geçerlidir; başka evrağa veya aksiyona taşınamaz
--
-- Bu üçü yetmez: tüketim anında gerçek durum makinesi yeniden çalıştırılır.
-- Evrak arada el değiştirdiyse geçiş oradan reddedilir. Tablo yetkiyi değil,
-- yalnız "bu bağlantıyı bu kişi aldı" bilgisini taşır.

CREATE TABLE mail_action_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 256 bit rastgele anahtarın SHA-256 özeti, hex (64 karakter).
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    record_id   UUID NOT NULL,
    -- Aksiyonu yürütecek kişi. Tüketimde aktör BURADAN çözülür; evrağın o anki
    -- assigned_to alanından türetilmez, yoksa anahtar devredilen koltuğa da
    -- yarardı.
    user_id     UUID NOT NULL,
    -- WorkflowAction enum adı.
    action      VARCHAR(50) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    -- Dolu ise anahtar tüketilmiştir; ikinci kez kullanılamaz.
    consumed_at TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mail_action_record FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE,
    CONSTRAINT fk_mail_action_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE
);

-- Tüketim sorgusu anahtarın özetiyle gelir; UNIQUE kısıtı zaten indeks kurar.
-- Süresi geçmiş satırların toplu temizliği için ayrı indeks.
CREATE INDEX idx_mail_action_tokens_expires_at ON mail_action_tokens (expires_at);

-- Aynı evrak + kişi için açık anahtarları bulup kapatmak (yeni bildirim
-- gönderilirken eskisi geçersizleşsin diye).
CREATE INDEX idx_mail_action_tokens_open ON mail_action_tokens (record_id, user_id)
    WHERE consumed_at IS NULL;
