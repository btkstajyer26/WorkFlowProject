-- =====================================================================
-- EBYS - records.status: sabit CHECK yerine katalog FK'si
--
-- Kaynak: DB_1_VERI_MODELI_SOZLESMESI.md SS11
-- Kolon tipi DEGISMEZ (VARCHAR(50) kalir, status_id'ye donusturulmez) -
-- Record.java'daki @Enumerated(EnumType.STRING) esleme ve API sozlesmesi
-- boylece bozulmadan DB referans butunlugu kazanilir.
--
-- Sozlesmedeki sira:
--   1. workflow_statuses zaten olusturuldu ve seed edildi (V14)
--   2. mevcut records.status degerlerinin katalogda oldugu dogrulanir
--   3. chk_records_status kaldirilir
--   4. FK eklenir
-- =====================================================================

-- Adim 2 - savunma amacli dogrulama. chk_records_status zaten V1'den beri
-- ayni 6 degeri zorunlu tuttugu icin bu blok normal sartlarda hicbir satir
-- bulmamali; bulursa migration burada durur, sessizce yanlis veri birakmaz.
DO $$
DECLARE
    orphan_count INT;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM records r
    WHERE NOT EXISTS (
        SELECT 1 FROM workflow_statuses ws WHERE ws.name = r.status
    );

    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'records.status icinde workflow_statuses kataloguna uymayan % satir var - once veri temizligi yapin', orphan_count;
    END IF;
END $$;

-- Adim 3
ALTER TABLE records DROP CONSTRAINT chk_records_status;

-- Adim 4 - ON UPDATE RESTRICT ON DELETE RESTRICT sozlesmede acikca istendi.
ALTER TABLE records
    ADD CONSTRAINT fk_records_status FOREIGN KEY (status)
        REFERENCES workflow_statuses (name)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT;