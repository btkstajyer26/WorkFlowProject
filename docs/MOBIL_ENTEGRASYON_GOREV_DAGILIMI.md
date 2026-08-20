# Mobil Entegrasyon — Görev Dağılımı

**Tarih:** 20 Ağustos 2026 (kod doğrulaması aynı gün)  
**Dal:** `test`'ten `feature/<konu>` açılır, PR `test`'e gider.

Mobil, mevcut Spring Boot REST API'yi kullanır. İş kuralları mobilde yazılmaz.
Sözleşme: [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)

---

## Kararlar (ekibin bilmesi gerekenler)

| Konu | Karar |
|---|---|
| Stack | **React Native + Expo + TypeScript** (`mobile/`) — ⚠️ **şartnameden sapma, onay bekliyor** |
| Neden RN | Ekip zaten React/TS/TanStack Query/RHF/Zod biliyor; Flutter = sıfırdan Dart |
| Sapmanın kapsamı | Mobil şartname Flutter ve `mobile/lib/main.dart` diyor. Sapma yalnız istemciyi etkiler; M0–M8 backend işleri stack'ten bağımsızdır ve her iki halde de aynıdır. Danışman onayı alınmadan MOB-1 başlatılmamalı. |
| Web bileşenleri | Kopyalanmaz. `View` / `Text` / `Pressable` / `FlatList` kullanılır |
| Taşınabilir | Tipler, OpenAPI DTO, Zod, Axios hata modeli, Query keys, RHF mantığı, tema token, tarih/rol helper |
| Roller (v1) | `CALISAN`, `BASKAN_YARDIMCISI`, `BASKAN` — **ADMIN yok** |
| Push | Backend **FCM HTTP v1**. Mobil: `@react-native-firebase/app` + `messaging` |
| Push test | **Expo Go ile olmaz.** Development build + gerçek cihaz. Sprint 0'da Firebase Android/iOS + iOS APNs |

---

## Durum (bugün kodda ne var / yok)

| İş | Durum | Not |
|---|---|---|
| Alıcı matrisi (`recipientsOf`) | ✅ Bitti | Melih — açık iş değil |
| `createdByFullName` (liste + detay) | ✅ Bitti | Alan adı `createdByName` değil |
| `GET /api/records/{id}/files` | ✅ Bitti | Mobil doğrudan kullanır |
| Auth / workflow / audit / notifications API | ✅ Bitti | Yeni yazılmaz |
| `last_deputy_id` koltuk devri | ✅ Bitti | Alperen — `feature/record` entegre edildi (M5 kapandı) |
| `device_tokens` + API | 🔴 Yok | |
| FCM push gönderimi | 🔴 Yok | |
| Logout'ta device token pasif | 🔴 Yok | |
| `docs/MOBIL_API_ENVANTERI.md` | 🟡 Taslak | Yazıldı; TEST adresi ve `openapi.json` bekliyor |
| Çoklu dosya upload | 🔴 Açık | Uç tek dosya alıyor (`@RequestPart("file")`); şartname BE-06 çoklu istiyor |
| `LogoutRequest.deviceToken` | 🔴 Yok | DTO şu an yalnız `refreshToken` taşıyor (M4) |
| Geçersiz FCM token temizliği | 🔴 Yok | M3 |
| Deep-link route sözleşmesi | 🟡 Karar | Sprint 0'da sabitlenir, aşağıda |
| TEST API ortamı | 🟡 Sahipsiz | Push testi buna bağlı (M0) |
| Başkan işlem geçmişi kırpması | ✅ Bitti | Bugün indi — mobil detay ekranını etkiler, aşağıya bak |
| `notifications` tablosu | ✅ Var | V1'de; şartname §5'teki öneri yeniden yazılmayacak |
| `tokens` tablosu (refresh) | ✅ Var | V1'de; şartname §5'teki `refresh_tokens` yeniden yazılmayacak |

---

## Deep-link sözleşmesi

Push'a dokunulduğunda açılacak route. **Sprint 0'da sabitlenir**; M3 payload'u
ile MOB-12 yönlendirmesi bu sözleşmeye göre yazılır.

| | |
|---|---|
| Şema | `ebys://kayitlar/{recordId}` |
| Expo Router dosyası | `mobile/app/(app)/kayitlar/[id].tsx` |
| Kaynak alan | Push `data.recordId` |

Web'deki kanonik route `/kayitlar/:recordId` ile bilerek aynı; iki istemcide
farklı yol tutmak, bildirim metinlerini ve testleri ikiye böler.

**Üç uygulama durumu da aynı route'a gitmeli:**

| Durum | Mobil tarafta |
|---|---|
| Açık (foreground) | `onMessage` → in-app banner; dokunulursa yönlendir |
| Arka planda | `onNotificationOpenedApp` → yönlendir |
| Tamamen kapalı | `getInitialNotification` → açılışta yönlendir |

Kullanıcının o kaydı görme yetkisi yoksa uç `403` döner; mobil bunu boş ekranla
değil, "Bu kaydı görüntüleme yetkiniz yok" mesajıyla karşılar. Kayıt silinmişse
`404`. **Bu iki durum deep-link testine dahildir** — bildirim geldikten sonra
kaydın el değiştirmesi normal bir senaryo.

> E-posta bildirimindeki mevcut link `/records/{id}` üretiyor ve web'de
> `/kayitlar/{id}`'ye yönleniyor. Mobil şeması bu redirect'e bağlanmaz,
> doğrudan kanonik yolu kullanır.

---

## Mobilin bilmesi gereken iki backend kuralı

**1. İşlem geçmişi role göre kırpılır.** `GET /api/audit-logs/record/{recordId}`
herkese aynı listeyi döndürmez:

| Rol | Ne görür |
|---|---|
| Çalışan (sahibi) | Tamamı |
| Bşk. Yrd. | Kayıt `DUZENLEME_BEKLIYOR` iken **devir anına kadar** kırpılmış |
| Başkan | Evrak kendisine **ilk iletildiği andan itibaren** |

Kırpma sunucuda yapılır; gizlenen satırlar cevaba hiç konmaz. Mobil bunu
"eksik veri" sanıp yeniden istememeli.

**2. Oluşturanın adı geçmişten türetilmez.** `createdByFullName` alanını kullan.
Geçmişteki `RECORD_CREATED` satırından okumak Başkan'da yanlış kişiyi gösterir —
o satırı hiç görmez (kural 1). Web'de bu hata yaşandı ve düzeltildi.

---

## Sprint sırası

| Sprint | Ne |
|---|---|
| 0 | Stack onayı, **deep-link sözleşmesi**, **TEST ortamı sahibi**, M0 envanter + M1, Expo proje, Firebase + APNs, development build |
| 1 | Kişi1 auth ekranları |
| 2 | Kişi2 kayıt/liste/detay/onay |
| 3 | Kişi3 dosya + bildirim merkezi; Melih M2; Ecesu M6 kararı + M7 |
| 4 | Melih M3 push; Nisan·Sümeyye M4; Hacer M8; Kişi3 MOB-12 |
| 5 | Gerçek cihaz, web regresyon, release |



---

## Backend — yalnız açık işler

### 👤 Entegrasyon — M0 🟡 *Sprint 0 · taslak hazır, sahibi aranıyor*

**Çıktı:** [MOBIL_API_ENVANTERI.md](MOBIL_API_ENVANTERI.md) — **yazıldı.**

Uç envanteri koda karşı çıkarıldı: auth, users/me, records, workflow/actions,
audit-logs, files, notifications, categories. Her uç için metot, adres, yetki,
request/response ve hata kodu var. Kapsanan konular:

- Genel `ApiError` kod tablosu **ve** workflow ucunun ayrı `WORKFLOW_*` ailesi
- Sayfalama zarfı, `sort` tuzağı, tarih biçimi (workflow ucu UTC `Instant`
  döner, diğerleri yerel `LocalDateTime` — karışması kolay)
- Görünürlük kapsamı ve **role göre kırpılan işlem geçmişi**
- `createdByFullName` kullanımı ve geçmişten ad türetme yasağı
- Dosya izin listesi, boyut sınırı, devir anındaki dosya görünürlüğü

**Kalan iş — bu ikisi sahipsiz olduğu için M0 kapanmadı:**

1. **TEST ortamını kim ayağa kaldıracak?** Envanterdeki ortam tablosu boş.
   Bu olmadan gerçek cihazdan istek atılamaz, **push testi (Sprint 4) yapılamaz.**
2. **`openapi.json` sabitlemesi.** Komut envanterde yazılı, çalıştırılıp
   çıktının repoya konması gerekiyor.

**Bitti sayılır:** Kişi1/2/3 envantere bakarak bağlayabiliyor **ve** TEST
adresine gerçek cihazdan istek atılabiliyor. Birincisi sağlandı, ikincisi bekliyor.

##### Ayrıca sağlanacak

1. **Güncel OpenAPI çıktısı.** springdoc zaten ayakta
   (`/v3/api-docs`, `/swagger-ui.html`). Sürüm sabitlenmiş bir `openapi.json`
   repoya veya paylaşılan bir yere konur; mobil istemci kodu bundan üretilir.
2. **Ekipçe erişilebilen TEST API adresi.** MOB-1 `DEV`/`TEST`/`PROD` base URL
   ayrımı yapıyor ama ayakta bir TEST ortamı olmadan mobil yalnız `localhost`'a
   bağlanabilir — gerçek cihazdan bu çalışmaz ve **push testi imkânsızdır**
   (Sprint 4 buna bağlı). Adres, örnek hesaplar ve hangi veriyle yüklü olduğu
   envantere yazılır.

> Sahibi netleşmeli: TEST ortamını kim ayağa kaldırıyor? Kimseye atanmadıysa
> Sprint 0'ın çıkmayan işi budur.

**Bitti sayılır:** Kişi1/2/3 envantere bakarak bağlayabiliyor; TEST adresine
gerçek cihazdan istek atılabiliyor.

---

### 👤 Ebrar Şeyma Karakuş — `audit`

#### M1 — İşlem geçmişi ucunun mobil doğrulaması 🟡

**Bağımlılık:** M0 ile birlikte

`GET /api/audit-logs/record/{recordId}` sayfalama yapmaz, tüm satırları tek
listede döndürür. Mobilde uzun geçmişi olan bir kayıt tek istekte iner.

1. Kırpma kurallarının (yukarıdaki tablo) M0 envanterine doğru yazıldığını teyit et.
2. Gerçekçi en büyük geçmiş için cevap boyutunu ölç. Sınır aşılıyorsa sayfalama
   veya `limit` öner — **kendiliğinden ekleme**, web istemcisi bu ucu sayfalamasız
   kullanıyor.
3. `AuditLogResponse`'taki HTTP alanları (`httpMethod`, `requestPath`,
   `httpStatus`, `errorCode`) kayıt geçmişinde hep `null`; mobil sözleşmede
   opsiyonel işaretlensin.

**Bitti sayılır:** Envanterde geçmiş ucunun davranışı ve boyutu yazılı; sayfalama
kararı (gerekli / gereksiz) gerekçesiyle kayıtlı.

---

### 👤 Melih Kocaman — `notification`

#### M2 — Cihaz token API 🔴

**Migration:** `V10__device_tokens.sql` (V9 sondaki sürüm)

FK adlandırması ve index projedeki konvansiyona uyar (bkz. V1):

```sql
CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    token       TEXT NOT NULL UNIQUE,
    platform    VARCHAR(20) NOT NULL,
    device_name VARCHAR(120),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Push gönderimi "bu kullanıcının aktif cihazları" diye sorar.
CREATE INDEX idx_device_tokens_user_active ON device_tokens (user_id, is_active);
```

| Metot | Adres | Gövde |
|---|---|---|
| `POST` | `/api/device-tokens` | `{ "token", "platform": "ANDROID"\|"IOS", "deviceName"? }` |
| `DELETE` | `/api/device-tokens` | `{ "token" }` |

Kullanıcı JWT'den okunur; gövdede `userId` **kabul edilmez**.

##### Upsert kuralı (tabloda `token` UNIQUE)

Aynı `token` tekrar gelirse satır güncellenir — yalnız `is_active` değil,
**hepsi**:

| Alan | Neden |
|---|---|
| `user_id` | **Kritik.** Aynı telefonda başka kullanıcı giriş yapabilir. Güncellenmezse cihaz yeni kullanıcıdayken token eski kullanıcıya bağlı kalır ve push **yanlış kişinin** evrağını bildirir. |
| `platform` | Cihaz yeniden kurulduğunda değişebilir |
| `device_name` | Kullanıcı cihaz adını değiştirebilir |
| `is_active` | Daha önce pasifleştirilmiş token yeniden aktif olur |
| `updated_at` | Ölü token ayıklaması için son görülme bilgisi |

FCM token'ı cihaz+uygulama başına tekildir, kullanıcı başına değil — kural bu
yüzden `token` üzerinde çalışır, `(user_id, token)` üzerinde değil.

##### Bu uç normal çıkış akışı değildir

`DELETE /api/device-tokens` yalnız **token yenilenmesi** ve **cihazı elle
kaldırma** içindir. Normal çıkışta cihaz token'ı `POST /api/auth/logout`
üzerinden pasifleşir (bkz. M4). İki yol aynı işi yapmaz; sözleşmede ikisi de
kendi amacıyla durur.

Gövdeli `DELETE` bazı HTTP istemcilerinde/proxy'lerde düşürülür. Mobil tarafla
birlikte teyit edilsin; sorun çıkarsa `DELETE /api/device-tokens/{token}` veya
`POST /api/device-tokens/deactivate` tercih edilir.

**Test:** controller + service. Upsert testi **şart**: aynı token farklı
kullanıcıyla gelince `user_id` değişmeli.

**Bitti sayılır:** Kayıt / silme yeşil; aynı token ikinci kullanıcıyla
gönderildiğinde satır yeni kullanıcıya geçiyor.

---

#### M3 — FCM push 🔴

**Bağımlılık:** M2 (alıcı matrisi zaten ✅)

1. `PushNotificationService` — FCM HTTP v1.
2. Durum değişiminde aktif `device_tokens`'a push. Alıcılar mevcut
   `WorkflowStatusChangedListener.recipientsOf` matrisinden gelir — yeni bir
   alıcı mantığı yazılmaz.
3. Env: `FCM_PROJECT_ID`, `FCM_CLIENT_EMAIL`, `FCM_PRIVATE_KEY` (repoya yazılmaz).

##### Payload sözleşmesi

```json
{
  "notification": { "title": "...", "body": "..." },
  "data": {
    "recordId": "uuid",
    "type": "RECORD_SUBMITTED"
  }
}
```

`type` değerleri mevcut `NotificationType` enum'undan gelir; yeni bir sözlük
uydurulmaz:

`RECORD_SUBMITTED` · `RECORD_FORWARDED` · `RECORD_APPROVED` ·
`RECORD_REJECTED` · `RECORD_RETURNED`

`data` alanları **string** olmalı (FCM `data` yalnız string kabul eder).

##### Geçersiz token temizliği

FCM gönderim cevabındaki hataya göre token pasifleştirilir — aksi halde ölü
token'lar birikir ve her bildirimde boşa istek atılır:

| FCM cevabı | Yapılacak |
|---|---|
| `UNREGISTERED` (uygulama silinmiş / token iptal) | `is_active = false` |
| `INVALID_ARGUMENT` (token bozuk) | `is_active = false` |
| `UNAVAILABLE` / `INTERNAL` | Pasifleştirme **yok**, geçici hata — tekrar denenebilir |

Push gönderimi onay akışını bloklamaz: mail gönderimindeki gibi `@Async` çalışır
ve hata yalnız loglanır.

**Test:** `UNREGISTERED` dönen sahte FCM cevabında token pasifleşiyor;
`UNAVAILABLE` dönen cevapta pasifleşmiyor.

**Bitti sayılır:** Gerçek cihazda bildirim geliyor, `data.recordId` ve
`data.type` okunuyor, geçersiz token otomatik pasifleşiyor.

---

### 👤 Nisan Tat · Sümeyye Baykan — `auth`

#### M4 — Logout'ta device token pasif 🔴

**Bağımlılık:** M2

**Resmi çıkış akışı budur.** Cihaz token'ı normal çıkışta buradan pasifleşir;
`DELETE /api/device-tokens` çıkış için kullanılmaz (bkz. M2).

##### DTO değişikliği — `LogoutRequest`

Şu an DTO yalnız `refreshToken` taşıyor ve alan `@NotBlank`. Opsiyonel
`deviceToken` eklenecek:

```json
POST /api/auth/logout
{
  "refreshToken": "...",
  "deviceToken": "..."
}
```

- `deviceToken` **opsiyonel** — alana doğrulama konmaz. Web mevcut gövdesini
  değiştirmeden çalışmaya devam eder.
- Mobil gönderirse: refresh token iptal edilirken aynı transaction içinde
  `device_tokens.is_active = false`.
- Token oturumdaki kullanıcıya ait değilse **sessizce yok sayılır**; hata
  dönmek, başkasının token'ının varlığını sızdırırdı.

**Dosya:** `LogoutRequest`, `AuthService.logout`

**Test:** `AuthServiceTest` — (a) `deviceToken` yokken web akışı bozulmuyor,
(b) varken token pasifleşiyor, (c) başkasının token'ı pasifleşmiyor.

**Bitti sayılır:** Çıkış yapan cihaza push gitmiyor; web logout'u değişmedi.

---

### 👤 Alperen Kara · Fevzi Berke Urganioğlu (`record`) + Nisan Tat · Sümeyye Baykan (`user`)

#### M5 — Koltuk devrinde `last_deputy_id` güncelle ✅ *20 Ağustos 2026'da kapandı*

`RecordRepository.updateLastDeputyId` eklendi ve
`UserService.kullaniciIsleriniDevret` içinde `assigned_to` devriyle **aynı
transaction'da** çağrılıyor. Devir sonrası `BASKAN_YARDIMCISINA_GERI_GONDER`
doğru yardımcıyı buluyor ve devredilen kayıtlar yeni yardımcının görünürlük
kapsamına giriyor. `UserServiceTest` kapsıyor.

---

### 👤 Hacer Bengü Ünal — `rbac`

#### M8 — `/api/device-tokens` yetki testi 🔵

**Bağımlılık:** M2

`AuthorizationMatrixTest`'e satır ekle (auth zorunlu).

**Bitti sayılır:** Matrix yeşil.

---

### 👤 Ecesu Başak — `attachment`

#### M6 — Çoklu dosya upload 🔴

**Sorun:** `POST /api/records/{id}/files` tek dosya alıyor
(`@RequestPart("file") MultipartFile`). Şartname BE-06 çoklu upload istiyor;
mobilde galeriden çoklu seçim standart davranış.

**Seçenek A (önerilen):** Uç `MultipartFile[]` kabul etsin, `List<FileResponseDto>`
dönsün. Tek dosyalı mevcut web çağrısı bozulmamalı — alan adı `file` kalsın.
**Seçenek B:** Mobil tarafta sıralı tek tek yükleme; backend değişmez, ilerleme
göstergesi dosya başına olur.

Karar M0 envanteri sırasında Ecesu + Kişi3 arasında verilir.

**Bitti sayılır:** Karar yazılı; A seçildiyse çoklu upload yeşil ve web akışı bozulmamış.

---

#### M7 — Multipart mobil senaryo testi 🟡

Büyük dosya, yanlış MIME, preview/download `Content-Type` kontrolü.
Liste ucu için yeni endpoint yazılmaz (`GET /api/records/{id}/files` zaten var).

**Bitti sayılır:** Mobil upload/indir senaryosu testte yeşil.

---

### Açık işi olmayan modüller

Bu modüller mobil için hazır; **yeniden yazılmayacak.** Sahipleri web
regresyonundan ve kendi alanlarına gelen sorulardan sorumlu:

| Sahip | Modül | Neden açık iş yok |
|---|---|---|
| Esra Öncü · Burak Kaya | `workflow` | Durum makinesi, geçiş kuralları ve `POST /api/records/{recordId}/workflow/actions` mobilin ihtiyacını karşılıyor. Hedef kullanıcı backend'de çözülüyor, mobil `targetUserId` göndermiyor. |
| Irmak Tanrıverdi | `search` | `GET /api/records` sayfalama, filtre ve `sort` destekliyor; geçersiz `sort` 400 dönüyor. `createdByFullName` eklendi. |

Hacer'in `common` paketindeki hata formatı mobilde olduğu gibi kullanılır;
yeni bir hata modeli yazılmaz. `ApiError` alanları: `code`, `message`, `status`,
`timestamp`, `fieldErrors[] { field, message }` (form doğrulama için).

---

## Mobil — React Native + Expo

### 👤 Kişi1 — Altyapı & giriş

**Klasör:** `mobile/src/core/`, `mobile/src/features/auth/`

#### MOB-1 — Proje kurulumu

1. Expo + TypeScript projesi (`mobile/`).
2. `DEV` / `TEST` / `PROD` API base URL.
3. Feature klasörleme.
4. Sprint 0: Firebase app kaydı + **development build** (push için Expo Go yetmez).

Web'den taşınabilir: tipler, OpenAPI client, Zod, Axios hata modeli, Query keys.

**Bitti sayılır:** Development build açılıyor, backend'e istek atılıyor.

---

#### MOB-2 — HTTP katmanı

1. Axios + interceptor (`Authorization: Bearer`).
2. `401` → refresh → fail ise login.
3. `ApiError` parse: `code`, `message`, `status`, `timestamp`, `fieldErrors[]`.

**Bitti sayılır:** Token yenileme sessiz çalışıyor.

---

#### MOB-3 — Giriş & güvenli token saklama

**Uçlar:** `POST /api/auth/login|refresh|logout`, `GET /api/users/me`

Secure store (`expo-secure-store` / Keychain / Keystore) — `AsyncStorage` değil.

Logout: `POST /api/auth/logout` gövdesinde `refreshToken` + (M4 sonrası)
opsiyonel `deviceToken`; ardından local temizlik. Ayrı bir
`DELETE /api/device-tokens` çağrısı **yapılmaz**.

**Bitti sayılır:** Giriş → ana ekran → çıkış.

---

#### MOB-4 — Zorunlu parola

**Uç:** `POST /api/auth/change-password`

`mustChangePassword=true` → yalnız change-password / logout / me.

**Bitti sayılır:** Geçici parola ile korumalı uç engelleniyor.

---

#### MOB-5 — Şifremi unuttum

**Uçlar:** `forgot-password` → `verify-reset-code` → `reset-password`

**Bitti sayılır:** 3 adım uçtan uca.

---

### 👤 Kişi2 — Kayıt & onay

**Bağımlılık:** MOB-2, MOB-3, M0

#### MOB-6 — Ana özet

Rol bazlı özet. `GET /api/records` (status) + `GET /api/notifications/unread/count`.

---

#### MOB-7 — Kayıt listesi

**Uç:** `GET /api/records?page&size&status&q&creator&from&to&sort`

Liste satırında **`createdByFullName`** göster (alan adı bu).

---

#### MOB-8 — Detay & geçmiş & dosya listesi

- `GET /api/records/{id}`
- `GET /api/audit-logs/record/{recordId}`
- `GET /api/records/{id}/files` ← zaten var; indirme Kişi3

---

#### MOB-9 — Oluştur / düzenle / gönder

`POST/PUT /api/records`, `GET /api/categories`,  
`POST /api/records/{id}/workflow/actions` (`GONDER` / `TEKRAR_GONDER`)

`targetUserId` **gönderilmez**.

---

#### MOB-10 — Onay / red / geri gönder

Aynı workflow ucu. Red / geri gönderde `comment` zorunlu.

| Rol | Durum | Aksiyon |
|---|---|---|
| Bşk. Yrd. | `BSK_YRD_INCELEMESINDE` | `BASKANA_ILET`, `CALISANA_GERI_GONDER` |
| Başkan | `BASKAN_INCELEMESINDE` | `ONAYLA`, `REDDET`, `CALISANA_GERI_GONDER`, `BASKAN_YARDIMCISINA_GERI_GONDER` |

**Bitti sayılır (Kişi2):** Oluştur → gönder → ilet → onay/red mutlu yolu.

---

### 👤 Kişi3 — Dosya, push, bildirim, profil

**Bağımlılık:** MOB-3; push için M2 + M3

#### MOB-11 — Dosya yükle / indir / önizle

`POST /api/records/{id}/files`, `GET /api/files/{id}/download|preview`, `DELETE /api/files/{id}`

Progress, izinler, desteklenen formatlar.

---

#### MOB-12 — Push (FCM)

**Bağımlılık:** M2, M3 + deep-link sözleşmesi (Sprint 0'da sabitlenmiş olmalı)

Paketler: `@react-native-firebase/app`, `@react-native-firebase/messaging`

1. Development build (Expo Go değil).
2. Giriş sonrası `POST /api/device-tokens` — `platform` ve `deviceName` ile.
3. **`onTokenRefresh` dinlenir.** FCM token'ı kendiliğinden yenilenebilir
   (uygulama verisi silinmesi, yeniden kurulum, uzun süre kullanılmama). Yeni
   token anında `POST /api/device-tokens`'a yazılır; yazılmazsa cihaz sessizce
   bildirim almayı bırakır ve bu **hata olarak görünmez**.
4. Foreground / background / kapalı — üçünde de deep-link sözleşmesindeki
   route'a yönlendir.
5. Çıkışta token'ı ayrıca silme; `POST /api/auth/logout` gövdesine `deviceToken`
   koymak yeterli (M4).

**Bitti sayılır:** Gerçek cihazda push → doğru kayıt; uygulama verisi
temizlendikten sonra tekrar giriş yapılınca bildirimler yine geliyor.

---

#### MOB-13 — Bildirim merkezi

`GET /api/notifications`, `/unread`, `PUT /{id}/read`

---

#### MOB-14 — Profil & çıkış

`GET /api/users/me` + MOB-3 logout.

---

#### MOB-15 — Hata / boş / offline UX

Ağ yok, `ApiError.message`, boş liste, loading — tutarlı.

---

#### MOB-16 — Release

Gerçek cihaz (Android + iOS), ekran boyutları, imza / provisioning.

---

## Özet

| Kişi | Modül | Açık görevler |
|---|---|---|
| **Entegrasyon** | — | M0 |
| **Ebrar** | `audit` | M1 |
| **Melih** | `notification` | M2, M3 |
| **Nisan · Sümeyye** | `auth`, `user` | M4 |
| **Alperen · Fevzi** | `record` | — (M5 kapandı) |
| **Ecesu** | `attachment` | M6, M7 |
| **Hacer** | `rbac`, `common` | M8 |
| **Esra · Burak** | `workflow` | — (hazır) |
| **Irmak** | `search` | — (hazır) |
| **Kişi1** | mobil | MOB-1 … MOB-5 |
| **Kişi2** | mobil | MOB-6 … MOB-10 |
| **Kişi3** | mobil | MOB-11 … MOB-16 |

> **Kişi1/2/3 henüz atanmadı.** Frontend ekibi (Zeynep Sena Şaltu, Yiğithan Ayhan,
> Tamer Erhan, Bartın Emre Sayar) React/TS bildiği için doğal aday; kabul
> kriterlerinden biri "web akışları bozulmamalı" olduğundan en az bir kişinin
> web bakımında kalması önerilir.

**Yapılmayacak / tekrar yazılmayacak:** alıcı matrisi, `createdByFullName`,
dosya listesi ucu, `notifications` ve `tokens` tabloları, Flutter.

---

## Bağımlılıklar

1. Stack onayı → MOB-1 (backend M0–M8 beklemez)
2. M0 → mobil API bağlama
3. **TEST API ortamı → gerçek cihaz testi → push testi.** Zincirin en kırılgan
   halkası; ortam yoksa Sprint 4 hiç doğrulanamaz.
4. Deep-link sözleşmesi (Sprint 0) → M3 payload'u **ve** MOB-12 yönlendirmesi
5. MOB-3 → Kişi2/3 korumalı ekranlar
6. M2 → M3, M4, M8, MOB-12
8. M6 kararı → MOB-11'in yükleme akışı
9. Backend PR web akışını bozmaz
