# Eksik Controller'lar ve Kararlar

**Tarih:** 13 Ağustos 2026
**Kaynak:** `integration/tum-feature-branchleri` × [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
**Kapsam:** `auth` ve `user` modülleri **hariç** — o iş listesi tamamlandığı
için ayrı dosya artık tutulmuyor; kalan test boşlukları
[EKSIK_SINIFLAR_VE_ONCELIK.md](EKSIK_SINIFLAR_VE_ONCELIK.md) altında.

13 Ağustos'un ikinci turunda karar bekleyen maddelerin tamamı kapandı
(listeleme birleştirmesi dahil). **Geriye tek yazılacak uç, bir de yeni
tespit edilen ölü kod kaldı.**

13 Ağustos'un üçüncü turunda `fix/workflow-flush` ve `feature/nisan-sumeyye`
entegre edildi: kayıt güncellemesi artık `saveAndFlush`, refresh ucu pasif
hesabı reddediyor, `/api/auth/change-password` artık kimlik doğrulaması
istiyor, geçersiz `sort` parametresi 500 yerine 400 dönüyor. Bu turda
Başkan Yardımcısı koltuk devri kararı **bir kez daha** değişti — bkz. 2.4.

---

## 1. Kalan işler

### 1.1 `GET /api/notifications` — geçmiş bildirimler 🟢

**Sorumlu:** Melih (`notification`)

Şu an yalnız `GET /api/notifications/unread` var ve sayfasız. Kullanıcı
okuduğu bildirimlere bir daha erişemiyor; arayüzdeki "Tümü" görünümü bu ucu
bekliyor.

| Metot | Adres | Durum |
|---|---|---|
| `GET` | `/api/notifications` | 🟢 **yazılacak** — okunmuş + okunmamış, sayfalı |
| `GET` | `/api/notifications/unread` | ✅ var |
| `GET` | `/api/notifications/unread/count` | ✅ var |
| `PUT` | `/api/notifications/{id}/read` | ✅ var |

Sözleşme §10 bu üç ucu **olduğu gibi kabul etti**. `PATCH .../read-all` ilk
sürüm kapsamı dışına alındı.

Cevap modeli mevcut `NotificationResponse` ile aynı olmalı: `id`, `recordId`,
`message`, `notificationType`, `read`, `createdAt`.

### 1.2 Frontend'de ölü kod: `RecordSearchController.ts` 🔴

**Sorumlu:** Frontend ekibi (backend'deki listeleme birleştirmesinin —
bkz. 2.2 — frontend karşılığı)

Backend'de `RecordSearchController` (`/api/records/search`) kaldırıldı,
listeleme `GET /api/records`'e taşındı (bkz. 2.2). Ama frontend hâlâ eski
uca göre üretilmiş kod taşıyor:

- `frontend/src/api/generated/RecordSearchController.ts`
- `frontend/src/api/recordSearch.ts` (facade)
- `frontend/src/mocks/api/handlers/recordSearchHandlers.ts` (MSW handler)

Şu an hiçbir sayfa veya context bu facade'ı çağırmıyor (yalnız kendi testi
ve `api/index.ts` barrel export'u referans veriyor) — yani **çalışan bir
akışı bozmuyor**, ama `VITE_API_MODE=backend` ile gerçek backend'e
bağlanıldığında bu dosyalar 404 üreten bir uç için hâlâ orada duruyor
olacak. OpenAPI yeniden üretildiğinde (`npm run api:generate`) otomatik
silinir; elle temizlenmesi de düşünülebilir.

`/api/v1` önekiyle aynı kökten geliyor: ikisi de backend'in listeleme
birleştirmesinden (§2) önce üretilmiş generated kod kalıntısı. `/api/v1`
tarafı düzeltildi (bkz. 2.5); bu, aynı kategoriden kalan tek parça.

---

## 2. Bu turlarda kapananlar

| İş | Nasıl kapandı |
|---|---|
| `/api/v1` öneki | ✅ Backend'de kaldırıldı; frontend generated client'ta bir kez düzeltilip yanlışlıkla geri alınmış, tekrar düzeltildi (bkz. 2.5) |
| CORS | ✅ `CorsConfig` — izinli origin `CORS_ALLOWED_ORIGINS`'ten, `*` yok |
| Dosya yükleme adresi | ✅ `POST /api/records/{id}/files` |
| Kayıt talebi akışı | ✅ Frontend'den kaldırıldı; kullanıcıyı Admin oluşturuyor |
| Başkan Yrd. / Başkan rol kuralları | ✅ Rol ataması `UserService`'te; koltuk devri 2.4'te — **karar iki kez değişti, sözleşme geride kaldı** |
| Çalışma notu modülü | ⛔ Kapsamdan çıktı, sonra kod da geri alındı — bkz. 2.3 |
| Kayıt geçmişi ucu | ⛔ Kapsamdan çıktı — backend'in mevcut `/api/audit-logs/record/{id}` adresi kabul edildi |
| Denetim izi cevap modeli | ✅ Karar fiilen verildi: düz alanlar (`userFullName`) korunuyor |
| **Kayıt listeleme birleştirmesi** | ✅ **Uygulandı** — bkz. 2.2 |
| **Başkan Yrd. pasifleştirme** | ✅ **Backend hizalandı** — bkz. 2.4 |

### 2.1 Çalışma notu modülü — koddan da temizlendi

Sözleşme §6 zaten kapsam dışı bırakmıştı: açıklama artık doğrudan workflow
isteğinin `comment` alanında gönderiliyor. Bu turda `record` ekibi
`RecordNote` modülünü (controller, DTO, entity, repository, service) kısa
süreliğine geri ekleyip hemen ardından tamamen kaldırdı — net etki sıfır,
ama artık kod da sözleşmeyle tutarlı. `record_notes` tablosu
`V6__drop_record_notes.sql` ile düşürüldü.

### 2.2 Kayıt listeleme birleştirmesi uygulandı

Önceki kararın ("uç `record`'da kalır, mantık `search`'ten gelir") uygulaması
geldi: `RecordSearchController` silindi, `RecordController.getAllRecords`
artık `RecordSearchService`'i çağırıyor.

**Ancak bu değişiklik ciddi bir güvenlik gerilemesi taşıyordu** ve merge
sırasında düzeltildi:

- `getRecordById`'deki `RecordAccessPolicy.assertCanView` çağrısı düşmüştü;
  herhangi bir rol herhangi bir kaydı görebilir hale gelmişti.
- Tipli exception'lar (`ForbiddenException`, `ResourceNotFoundException`,
  `BusinessRuleException`) düz `RuntimeException`'a geri dönmüştü.
- `createRecord`/`updateRecord`/`deleteRecord`'daki `PermissionService`
  kontrolleri düşmüştü.

`RecordServiceImpl` bu kontroller korunarak yeniden yazıldı; yalnızca
listeleme metodu kaldırıldı (artık `RecordSearchService`'te). Ayrıca
`RecordController`, `RecordSearchCriteria`'nın güncel alan adlarını
(`q`/`from`/`to`) değil eski adlarını (`setText`/`setStartDate`/
`setEndDate`) çağırıyordu — derleme hatası olurdu, düzeltildi.

Güvenlik kontrolünün geri geldiğini doğrulayan test zaten vardı
(`RecordServiceImplTest`), yani bu artık koruma altında.

### 2.3 Denetim izi cevap modeli — karar fiilen verildi

Frontend'in üretilmiş istemcisi `data-contracts.ts` içinde **`userFullName`**
kullanıyor — backend'in mevcut düz modeli benimsendi. `AuditLogResponse`
değişmeyecek.

> **Küçük tutarsızlık kalıcı:** Sözleşme §6'daki örnek JSON hâlâ `actor{}`
> nesnesi gösteriyor. Düzeltilmesi *Ebrar · frontend ekibi*'nde.

### 2.4 Başkan Yardımcısı koltuk devri — karar bir kez daha değişti 🔴 *sözleşme geride kaldı*

Bu madde iki kez ters yöne döndü, sırasıyla:

1. **İlk hâl (sözleşmenin eski metni):** aktif yardımcı rol devredilmeden
   pasifleştirilemez, `409 DEPUTY_TRANSFER_REQUIRED`.
2. **Birinci tur:** sözleşme §8 doğrudan pasifleştirmeye izin verecek
   şekilde güncellendi, frontend buna göre değişti
   (`AdminContext.tsx`'teki engelleyici kontrol kaldırıldı), backend'deki
   `UserService.setActive` kontrolü de kaldırıldı.
3. **İkinci tur (şu anki hâl):** ekip kararı tersine döndü. Devir artık
   `changeRole` üzerinden yapılıyor — Başkan Yardımcısı başka bir tekil role
   (Başkan veya Admin) geçerken **aynı istekte** zorunlu bir
   `replacementBaskanYardimcisiId` gönderilmeli; devir aynı transaction
   içinde uygulanıyor. `setActive`'deki devirsiz-pasifleştirme engeli geri
   eklendi.

> **Sözleşme artık geride kaldı:** [§8](FRONTEND_BACKEND_SOZLESMESI.md#L339)
> hâlâ *"Aktif Başkan Yardımcısı doğrudan pasifleştirilebilir"* diyor —
> bu, 2. adımın metni, artık geçerli değil. `PATCH .../role` satırı
> ("Rol değiştirme **veya Başkan Yardımcısı rolünü devretme**") kabaca
> tutarlı ama devir isteğinin gövdesini (`replacementBaskanYardimcisiId`)
> tanımlamıyor. Düzeltilmesi *Nisan · Sümeyye · frontend ekibi*'nde.

### 2.5 `/api/v1` öneki — bir kez düzeltildi, yanlışlıkla geri alındı, tekrar düzeltildi

Backend'deki önek kaldırma daha önce yapılmıştı. Frontend ekibinden biri
generated client'taki karşılığını da doğru şekilde düzeltmiş (adresler,
mock handler'lar, normalize script'ine kalıcı bir koruma) ama commit hemen
ardından bir başka commit'le **açıklamasız geri alınmış**. Entegrasyon
sırasında bu fark edildi; orijinal düzeltme commit'i tekrar uygulandı.

---

## 3. Karar bekleyenler 🔴

### 3.1 Sözleşme §12 netleştirmeleri

Sayfalama indeksi (backend `Pageable` kullanıyor → **0**), çoklu `status`
filtresi formatı, hata `code` listesinin belgelenmesi. Dosya türü/boyutu
zaten kodda hazır (`FileContentValidator`), yalnızca sözleşmeye yazılması
gerekiyor. Ertelendi.

### 3.2 Sözleşme §13 açık ürün kararları

- `records` tablosunda kullanıcıya gösterilecek benzersiz `record_no` alanı.
- `notifications.record_id` zorunlu olduğu için kayıttan bağımsız sistem
  duyuruları desteklenmiyor.

---

## 4. Öncelik

| Sıra | İş | Sorumlu |
|---|---|---|
| 1 | `GET /api/notifications` (geçmiş bildirimler) | Melih |
| 2 | Sözleşme §8'in koltuk devri metninin güncel koda göre düzeltilmesi (2.4) | Nisan · Sümeyye · frontend |
| 3 | Frontend'deki ölü `RecordSearchController.ts` kalıntısının temizlenmesi | Frontend ekibi |
| 4 | Sözleşme §6 örnek JSON'unun düzeltilmesi | Ebrar · frontend |
| 5 | §12 netleştirmeleri | Ekip |
