# ADR-0001: Backend'i modül (feature) bazlı paketleme

- Durum: Kabul Edildi
- Tarih: 2026-08-20 (karar Ağustos 2026 başında alındı, kayıt geriye dönük yazıldı)
- Karar sahipleri: Proje ekibi

## Bağlam

Backend'i 12 kişilik bir ekip aynı anda geliştiriyor. Spring Boot projelerinde
yaygın olan katman bazlı paketleme (`controller/`, `service/`, `repository/`,
`dto/`) bu ekip büyüklüğünde iki somut soruna yol açıyordu:

- Her yeni uç, herkesin aynı üç-dört klasöre dokunması demek. `controller/`
  klasörü sürekli çakışma üretiyordu.
- Bir modülün sınırının nerede bittiği kodda görünmüyordu. "Kayıt durumunu kim
  değiştirebilir?" sorusunun cevabı klasör yapısından okunamıyordu.

Karar geriye dönük yazıldı: gerekçe bugüne kadar yalnız ekip içi görev dağılımı
dosyasında duruyordu, kod tabanında hiçbir yerde yazılı değildi.
[architecture.md](../architecture.md) modül **sınırlarını** anlatıyor ama bu
seçimin **nedenini** anlatmıyor.

## Değerlendirilen Seçenekler

1. **Katman bazlı paketleme** (`controller/`, `service/`, `repository/`)
   - Artı: Spring dünyasında en tanıdık düzen; yeni gelen kişi nereye bakacağını bilir.
   - Eksi: Paralel çalışan 12 kişide sürekli merge çakışması. Modül sınırı
     görünmez; bir modülün başka bir modülün servisini çağırması fark edilmez.

2. **Modül (feature) bazlı paketleme** (`auth/`, `record/`, `workflow/`, …)
   - Artı: Her kişi kendi klasöründe çalışır, çakışma yüzeyi küçülür. Sahiplik
     klasörle örtüşür. Modüller arası bağımlılık import satırında görünür hale gelir.
   - Eksi: Katman kuralları (controller → service → repository) artık klasörle
     zorlanmaz, ayrıca yazılı kural gerekir.

## Karar

Modül bazlı paketleme seçildi. `btk.staj.WorkFlowProject` altında on modül var
ve her modül kendi `controller/`, `service/`, `repository/`, `dto/`, `entity/`
alt paketlerini taşıyor.

Katman kurallarının klasörle zorlanamaması, [architecture.md "Katmanlama
kuralları"](../architecture.md) bölümünde yazılı kuralla telafi edildi.

## Sonuçlar

**Olumlu:** Modül sahipliği net; bir PR genelde tek klasöre dokunuyor. Modüller
arası sınır ihlali code review'da görünür oluyor.

**Maliyet:** Aynı iş kuralının iki modülde birden yaşaması riski var ve bu risk
gerçekleşti: kayıt görünürlük kapsamı hem `rbac/RecordAccessPolicy` (tek kayıt
için boolean) hem `search/RecordSpecifications` (sorgu koşulu) içinde duruyor.
İkisi 20 Ağustos 2026'da ayrıştı; detay ucu kaydı açarken liste ucu aynı kaydı
hiç döndürmedi. Bu tür ikizlerin her iki tarafında da "biri değişirse diğeri de
değişmeli" uyarısı bulunmalı.

**Takip:** Modüller arası iş (örneğin `user` + `record`'a birden dokunan koltuk
devri) tek kişiye atanmamalı; iki modül sahibi de review etmeli.

## Bağlantılar

- [architecture.md](../architecture.md) — modül sınırları ve katmanlama kuralları
- [workflow.md](../workflow.md) — görünürlük kapsamının iki biçimi
