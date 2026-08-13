# Eksik Controller'lar ve Kararlar

**Tarih:** 13 Ağustos 2026
**Kaynak:** `integration/tum-feature-branchleri` × [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
**Kapsam:** `auth` ve `user` modülleri **hariç** — onların uç listesi
[AUTH_USER_YAPILACAKLAR.md](AUTH_USER_YAPILACAKLAR.md) dosyasındadır.

13 Ağustos entegrasyonundan sonra listenin büyük kısmı kapandı. **Geriye tek
bir uç kaldı.**

Bu turda iki şey aynı anda oldu: ekip maddeleri yazdı, frontend ekibi de
sözleşmeyi gerçek backend'e göre güncelledi. İkincisi bazı maddeleri
*yazılarak* değil *kapsamdan çıkarılarak* kapattı.

---

## 1. Kalan tek iş

### `GET /api/notifications` — geçmiş bildirimler 🟢

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

Sözleşme §10 bu üç ucu **olduğu gibi kabul etti** — adres veya metot
değişikliği artık istenmiyor. `PATCH .../read-all` ilk sürüm kapsamı dışına
alındı; arayüz bildirimleri tek tek okundu yapıyor.

Cevap modeli mevcut `NotificationResponse` ile aynı olmalı: `id`, `recordId`,
`message`, `notificationType`, `read`, `createdAt`. Kullanıcı kimliği JWT'den
belirlendiği için gövdede dönmez.

---

## 2. Bu turda kapananlar

| İş | Nasıl kapandı |
|---|---|
| `/api/v1` öneki | ✅ Kaldırıldı; `record` ve `categories` artık `/api/...` altında |
| CORS | ✅ `CorsConfig` eklendi — izinli origin `CORS_ALLOWED_ORIGINS` ortam değişkeninden, `*` yok, `allowCredentials` ile uyumlu |
| Dosya yükleme adresi | ✅ `POST /api/records/{id}/files` (Ecesu). Kayıt kimliği artık yoldan geliyor |
| Kayıt talebi akışı | ✅ Frontend'den kaldırıldı; kullanıcıyı Admin oluşturuyor |
| Başkan Yrd. / Başkan rol kuralları | ✅ `UserService` içinde (Nisan · Sümeyye) |
| Çalışma notu modülü | ⛔ **Kapsamdan çıktı** — aşağıya bakınız |
| Kayıt geçmişi ucu | ⛔ **Kapsamdan çıktı** — aşağıya bakınız |
| Denetim izi cevap modeli | ✅ Karar fiilen verildi — aşağıya bakınız |

### Çalışma notu modülü kapsamdan çıktı

Önceki sürümde "en büyük tek parça" olarak işaretlenmişti. Sözleşme §6 artık
şöyle diyor:

> Özel veya ayrı kaydedilen bir çalışma notu modeli kullanılmaz. Kullanıcı
> açıklamasını doğrudan workflow işlem penceresinde yazar ve aynı
> `POST /api/records/{recordId}/workflow/actions` isteğinin `comment` alanında
> gönderir.

`GET/PUT /api/records/{id}/notes/me` uçları, `NOTE_VERSION_CONFLICT` kuralı ve
`WorkflowActionService`'e eklenmesi planlanan not temizleme adımı **artık
gerekmiyor**. Frontend'deki `RecordNotesPanel` de bu modele göre yeniden
kuruldu.

> `record_notes` tablosu `V2` migration'ında duruyor ama kullanılmıyor.
> Kaldırılıp kaldırılmayacağı ayrı bir karar; şimdilik dokunulmadı.

### Kayıt geçmişi ucu kapsamdan çıktı

Sözleşme `GET /api/records/{id}/history` yerine backend'in mevcut adresini
kabul etti: **`GET /api/audit-logs/record/{recordId}`**. Taşıma işi düştü.

### Denetim izi cevap modeli — karar fiilen verildi

Önceki sürümde açık bırakılmıştı: aktör iç içe nesne mi (`actor{}`), düz alan
mı (`userFullName`, `roleName`)?

Frontend OpenAPI şemasından istemcisini üretti ve `data-contracts.ts` içinde
**`userFullName`** kullanıyor — yani backend'in mevcut düz modeli benimsendi.
`AuditLogResponse` değişmeyecek.

> **Küçük tutarsızlık:** Sözleşme §6'daki örnek JSON hâlâ `actor{}` nesnesi
> gösteriyor, oysa üretilen istemci düz alanları kullanıyor. Örneğin
> güncellenmesi gerekiyor — *Ebrar · frontend ekibi*.

---

## 3. Karar bekleyenler 🔴

### 3.1 Kayıt listeleme iki uçtan veriliyor

**Sorumlu:** Alperen · Fevzi (`record`) + Irmak (`search`)

Karar önceki sürümde verildi ve **hâlâ geçerli**: listeleme ucu
`GET /api/records` altında kalacak, filtre ve kapsam mantığı `search`
modülünden gelecek, `RecordSearchController` kaldırılacak.

**Henüz uygulanmadı.** Görünürlük kapsamı hâlâ iki yerde ayrı ayrı yazılı:
`RecordServiceImpl.getFilteredRecords` kendi predicate'lerini kuruyor,
`RecordSpecifications.visibilityScope` aynı kuralı ikinci kez tanımlıyor. Aynı
güvenlik kuralının iki kopyası; biri değişince diğeri unutulur.

Bu turda `RecordSearchCriteria` alan adları sözleşmeye uyduruldu
(`q`, `from`, `to`), yani birleştirme için zemin hazır.

### 3.2 Sözleşme §12 netleştirmeleri

Sayfalama indeksi (backend `Pageable` kullanıyor → **0**), çoklu `status`
filtresi formatı, kayıt başına dosya adedi sınırı ve hata `code` listesinin
belgelenmesi. Ertelendi.

### 3.3 Sözleşme §13 açık ürün kararları

- `records` tablosunda kullanıcıya gösterilecek benzersiz `record_no` alanı.
- `notifications.record_id` zorunlu olduğu için kayıttan bağımsız sistem
  duyuruları desteklenmiyor.

---

## 4. Öncelik

| Sıra | İş | Sorumlu |
|---|---|---|
| 1 | `GET /api/notifications` (geçmiş bildirimler) | Melih |
| 2 | Kayıt listelemenin tek uca indirilmesi (3.1) | Alperen · Fevzi · Irmak |
| 3 | Sözleşme §6 örnek JSON'unun düzeltilmesi | Ebrar · frontend |
| 4 | §12 netleştirmeleri | Ekip |
