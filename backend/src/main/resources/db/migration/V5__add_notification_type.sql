-- Bildirimin hangi onay akisi olayindan dogdugu. Arayuz bildirimi bu bilgiye
-- gore ikonlar ve gruplar; mesaj metnini ayristirmak zorunda kalmaz.
--
-- Mevcut satirlar icin guvenli bir varsayilan yok, ama tablo bu kolon
-- eklenmeden once kullanilmadigi icin gecmis veri sorunu olusmaz.
ALTER TABLE notifications
    ADD COLUMN notification_type VARCHAR(50) NOT NULL;

-- Okunmamis bildirimler kullanici bazinda ve tur filtresiyle sorgulanabilir.
CREATE INDEX idx_notifications_type ON notifications (notification_type);
