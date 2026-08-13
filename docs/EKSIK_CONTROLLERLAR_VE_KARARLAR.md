# Eksik Controller'lar ve Kararlar

**Tarih:** 13 Ağustos 2026
**Kaynak:** `integration/tum-feature-branchleri` × [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
**Kapsam:** `auth` ve `user` modülleri **hariç** — o iş listesi tamamlandığı
için ayrı dosya artık tutulmuyor; kalan test boşlukları
[EKSIK_SINIFLAR_VE_ONCELIK.md](EKSIK_SINIFLAR_VE_ONCELIK.md) altında.

13 Ağustos'un ikinci turunda karar bekleyen maddelerin tamamı kapandı
(listeleme birleştirmesi dahil). **Geriye tek yazılacak uç, bir de yeni
tespit edilen ölü kod kaldı.**

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
| Başkan Yrd. / Başkan rol kuralları | ✅ Rol ataması `UserService`'te; pasifleştirme kuralı 2.4'te ayrıca hizalandı |
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

### 2.4 Başkan Yardımcısı pasifleştirme — backend sözleşmeyle hizalandı

Sözleşme §8 eskiden "aktif yardımcı rol devredilmeden pasifleştirilemez;
`409 DEPUTY_TRANSFER_REQUIRED`" diyordu. Bu metin doğrudan pasifleştirmeye
izin verecek şekilde güncellendi ve frontend buna göre değişti
(`AdminContext.tsx`'teki engelleyici kontrol kaldırıldı).

Backend bunu yansıtmıyordu: `UserService.setActive` hâlâ eski kuralı
uyguluyor, aktif Başkan Yardımcısı'nı pasifleştirme denemesinde
`BusinessRuleException` fırlatıyordu. Kontrol kaldırıldı; pasifleştirme artık
rolü değiştirmiyor, yalnızca erişimi kapatıyor.

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
| 2 | Frontend'deki ölü `RecordSearchController.ts` kalıntısının temizlenmesi | Frontend ekibi |
| 3 | Sözleşme §6 örnek JSON'unun düzeltilmesi | Ebrar · frontend |
| 4 | §12 netleştirmeleri | Ekip |
