-- Kayıt Çalışana geri gönderildiğinde içeriğinin o anki hali dondurulur.
--
-- Gerekçe: Başkan Yardımcısı, geri gönderdiği evrağı `duzeltmede-olanlar`
-- sekmesinden izlemeye devam eder (RecordAccessPolicy.canView). Ama evrak o
-- sırada Çalışanın elindedir; Çalışanın kaydettiği her değişiklik yardımcının
-- ekranına anında yansıyordu. Yardımcı, düzeltmeleri ancak `TEKRAR_GONDER`
-- ile geri geldiğinde görmelidir.
--
-- Çözüm, geçmiş kırpmasıyla aynı mantık: yardımcının gördüğü her şey devir
-- anında donar. Ayrı bir tablo yerine kolon tercih edildi; anlık görüntü
-- kaydın kendisine birebir bağlı ve tek satır yetiyor.
--
-- Kolonlar NULL kalabilir: hiç geri gönderilmemiş kayıtların anlık görüntüsü
-- yoktur. Değerler yalnızca kayıt DUZENLEME_BEKLIYOR durumundayken okunur,
-- bu yüzden yeniden gönderimde temizlenmelerine gerek yoktur; bir sonraki
-- geri gönderme üzerlerine yazar.

ALTER TABLE records
    ADD COLUMN snapshot_title       VARCHAR(255),
    ADD COLUMN snapshot_description TEXT,
    ADD COLUMN snapshot_category_id INT,
    -- Ek dosyalar ayrıca kopyalanmaz: files tablosunda uploaded_at ve
    -- deleted_at zaten var, devir anına göre süzmek yeterli.
    ADD COLUMN snapshot_at          TIMESTAMP,
    ADD CONSTRAINT fk_record_snapshot_category FOREIGN KEY (snapshot_category_id)
        REFERENCES categories (id) ON DELETE RESTRICT;

-- Şu anda düzeltmede olan kayıtların anlık görüntüsü yok; mevcut içerikleri
-- taban kabul edilir. Aksi halde bu kayıtlarda okunacak dondurulmuş değer
-- bulunmaz ve yardımcının ekranı boş kalırdı.
UPDATE records
SET snapshot_title       = title,
    snapshot_description = description,
    snapshot_category_id = category_id,
    snapshot_at          = COALESCE(updated_at, created_at)
WHERE status = 'DUZENLEME_BEKLIYOR'
  AND snapshot_at IS NULL;
