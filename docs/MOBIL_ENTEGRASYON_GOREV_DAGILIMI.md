# Mobil Entegrasyon — Görev Dağılımı

**Tarih:** 23 Ağustos 2026 (kod doğrulaması aynı gün)
**Dal:** `test`'ten `feature/<konu>` açılır, PR `test`'e gider.

Mobil, mevcut Spring Boot REST API'sini kullanır. İş kuralları mobilde yazılmaz.
Sözleşme: [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md) ·
Uç envanteri: [MOBIL_API_ENVANTERI.md](MOBIL_API_ENVANTERI.md)

Bu belge **yalnız açık işleri** taşır. Kapanan işler en altta tek satırlık kayıt
olarak durur; tasarım gerekçeleri envanter ve mimari belgelerindedir.

---

## Açık işler

| # | Sahip | Modül | İş | Durum |
|---|---|---|---|---|
| M0-kalan | Entegrasyon | — | `docs/openapi.json` yeniden üretilmeli | 🔴 |
| M1 | Ebrar Şeyma Karakuş | `audit` | İşlem geçmişi cevap boyutu ölçülüp sayfalama kararı yazılmalı | 🟡 |
| M2-kalan | Melih Kocaman | `notification` | `DELETE /api/device-tokens` sahiplik doğrulaması + token log maskeleme | 🔴 |
| M3 | Melih Kocaman | `notification` | `PushNotificationService` listener'a bağlanmalı | 🔴 |
| M8 | Hacer Bengü Ünal | `rbac` | `/api/device-tokens` yetki matrisi testi | 🔵 |
| MOB-12 | Mobil | `mobile/` | Push (FCM) istemci tarafı | 🔴 |
| MOB-16 | Mobil | `mobile/` | iOS release / imza | 🟡 |

> **En kritik madde M3'tür.** M2 ve M4 bittiği için mobil taraf token kaydedip
> çıkışta pasifleştirebilir, ama `PushNotificationService` çağrılmadığı için
> hiçbir push gönderilmez. MOB-12 bu bağlantı yapılmadan doğrulanamaz.

---

## Backend açık işleri

### 👤 Entegrasyon — M0 kalanı: `openapi.json` bayat 🔴

[docs/openapi.json](openapi.json) 20 Ağustos'ta sabitlendi ve hâlâ **27 yol, 31
şema** içeriyor. O tarihten sonra eklenen `/api/device-tokens` uçları dosyada
**yok** (`grep device-tokens docs/openapi.json` → sıfır eşleşme).

Mobil istemci kodu bu dosyadan üretildiği için cihaz token kaydı üretilmiş
istemciyle yapılamaz. Backend ayaktayken `/v3/api-docs` çıktısı yeniden
alınmalı; komut envanterde.

**Bitti sayılır:** `docs/openapi.json` içinde `/api/device-tokens` `POST` ve
`DELETE` uçları var.

---

### 👤 Ebrar Şeyma Karakuş — `audit`

#### M1 — İşlem geçmişi ucunun mobil doğrulaması 🟡

Üç maddenin **ikisi kapandı**, biri açık:

| Madde | Durum |
|---|---|
| Kırpma kurallarının envantere doğru yazılması | ✅ Envanter §5'te yazılı |
| `httpMethod` / `requestPath` / `httpStatus` / `errorCode` alanlarının opsiyonel işaretlenmesi | ✅ Envanter §5'te "her zaman `null`" notuyla yazılı |
| **Cevap boyutu ölçümü ve sayfalama kararı** | 🟡 **Açık** — envanterde ne ölçüm ne gerekçeli karar var |

`GET /api/audit-logs/record/{recordId}` sayfalama yapmaz, tüm satırları tek
listede döndürür. Mobilde uzun geçmişi olan bir kayıt tek istekte iner.

Gerçekçi en büyük geçmiş için cevap boyutu ölçülmeli. Sınır aşılıyorsa sayfalama
veya `limit` **önerilmeli** — kendiliğinden eklenmemeli; web istemcisi bu ucu
sayfalamasız kullanıyor.

**Bitti sayılır:** Envanterde ölçülen boyut ve sayfalama kararı (gerekli /
gereksiz) gerekçesiyle kayıtlı.

---

### 👤 Melih Kocaman — `notification`

#### M2 kalanı — iki güvenlik açığı 🔴

Cihaz token API'si kodda tam: `V10__device_tokens.sql`, `DeviceTokenController`,
`DeviceTokenService`, `DeviceTokenRepository`, `DeviceToken` entity ve
`DeviceTokenServiceTest`. Upsert kuralı uygulanmış durumda. İki madde açık:

**1. `DELETE /api/device-tokens` sahiplik doğrulaması yapmıyor.**
`DeviceTokenController.removeToken` `Authentication` parametresi almıyor;
`deactivateByToken` yalnız token değerine göre güncelliyor. Kimliği doğrulanmış
herhangi bir kullanıcı, başkasının token değerini gönderirse o kişinin push
bildirimlerini kapatabilir. Uç `(token, user_id)` çifti üzerinden çalışmalı,
kullanıcıya ait olmayan token M4'teki gibi **sessizce yok sayılmalı** — hata
dönmek başkasının token'ının varlığını sızdırır.

**2. Cihaz tokenları log'a açık yazılıyor.** `DeviceTokenService.deactivateToken`
(`log.info`) ile `PushNotificationService` (`log.warn` / `log.info`) token
değerini tam olarak yazıyor. Token bir kimlik bilgisidir ve log'lar 30 gün
saklanıyor; maskelenmeli.

> `DELETE /api/device-tokens` **normal çıkış akışı değildir.** Yalnız token
> yenilenmesi ve cihazı elle kaldırma içindir; normal çıkışta token
> `POST /api/auth/logout` üzerinden pasifleşir (M4, kapandı).

**Bitti sayılır:** Başkasının token'ıyla `DELETE` çağrısı hiçbir satırı
değiştirmiyor ve testte doğrulanıyor; log'larda tam token değeri geçmiyor.

---

#### M3 — FCM push servisi bağlanmadı 🔴

`PushNotificationService` depoda ve sözleşmenin tamamı yazılmış durumda: FCM
başlatma, `data.recordId` / `data.type` payload'u, `handleFcmError` ile geçersiz
token temizliği. Env anahtarları (`FCM_PROJECT_ID`, `FCM_CLIENT_EMAIL`,
`FCM_PRIVATE_KEY`) `.env.example` içinde tanımlı.

**Kalan iş — servis hiçbir yerden çağrılmıyor.** Tüm repoda, sınıfın kendi
dosyası dışında `PushNotificationService` geçen tek satır yok.
`WorkflowStatusChangedListener` yalnız uygulama içi bildirim ve e-posta
üretiyor. Servis listener'a bağlanana kadar push **hiç gitmez** ve bu eksiklik
sessizdir: hata üretmez, log'a düşmez.

Bağlarken alıcılar mevcut `recipientsOf` matrisinden alınmalı; yeni alıcı
mantığı yazılmamalı. Gönderim `@Async`'tir, onay akışını bloklamaz.

##### Payload sözleşmesi (MOB-12 buna göre yazılır)

```json
{
  "notification": { "title": "...", "body": "..." },
  "data": {
    "recordId": "uuid",
    "type": "RECORD_SUBMITTED"
  }
}
```

`type` değerleri mevcut `NotificationType` enum'undan gelir; yeni sözlük
uydurulmaz: `RECORD_SUBMITTED` · `RECORD_FORWARDED` · `RECORD_APPROVED` ·
`RECORD_REJECTED` · `RECORD_RETURNED`

`data` alanları **string** olmalı (FCM `data` yalnız string kabul eder).

##### Geçersiz token temizliği (yazıldı, sözleşme olarak duruyor)

| FCM cevabı | Yapılacak |
|---|---|
| `UNREGISTERED` (uygulama silinmiş / token iptal) | `is_active = false` |
| `INVALID_ARGUMENT` (token bozuk) | `is_active = false` |
| `UNAVAILABLE` / `INTERNAL` | Pasifleştirme **yok**, geçici hata |

**Bitti sayılır:** Gerçek cihazda bildirim geliyor, `data.recordId` ve
`data.type` okunuyor, geçersiz token otomatik pasifleşiyor.

---

### 👤 Hacer Bengü Ünal — `rbac`

#### M8 — `/api/device-tokens` yetki testi 🔵

**Bağımlılık:** M2 kalanı — sahiplik doğrulaması eklendikten sonra yazılmalı,
yoksa test bugünkü yanlış davranışı sabitler.

`AuthorizationMatrixTest`'e satır ekle (auth zorunlu). Şu an test dizininde
`device-tokens` geçen tek satır yok.

**Bitti sayılır:** Matrix yeşil ve `/api/device-tokens` uçlarını kapsıyor.

---

## Mobil açık işleri

`mobile/` paketi Expo + React Native + TypeScript. `lint`, `typecheck` ve 10
Jest testi temiz; CI'da `Mobile / quality` işi bunları her PR'da çalıştırır.

### MOB-12 — Push (FCM) 🔴

**Bağımlılık:** M2 kalanı, M3

Bugün mobilde `firebase`, `messaging` veya `device-tokens` geçen **tek satır
yok**; istemci tarafı hiç başlamadı.

Paketler: `@react-native-firebase/app`, `@react-native-firebase/messaging`

1. Development build (Expo Go push için yetmez). `eas.json` içinde
   `development` / `preview` / `production` profilleri hazır.
2. Giriş sonrası `POST /api/device-tokens` — `platform` ve `deviceName` ile.
3. **`onTokenRefresh` dinlenir.** FCM token'ı kendiliğinden yenilenebilir
   (uygulama verisi silinmesi, yeniden kurulum, uzun süre kullanılmama). Yeni
   token anında `POST /api/device-tokens`'a yazılır; yazılmazsa cihaz sessizce
   bildirim almayı bırakır ve bu **hata olarak görünmez**.
4. Foreground / background / kapalı — üçünde de aşağıdaki deep-link
   sözleşmesindeki route'a yönlendir.
5. Çıkışta token'ı ayrıca silme; `POST /api/auth/logout` gövdesine `deviceToken`
   koymak yeterli (M4, kapandı).

**Bitti sayılır:** Gerçek cihazda push → doğru kayıt; uygulama verisi
temizlendikten sonra tekrar giriş yapılınca bildirimler yine geliyor.

---

### MOB-16 — Release 🟡

Android tarafı yapıldı: 21 Ağustos 2026'da EAS preview APK ile Samsung Galaxy
A34 / Android 16 cihazda TEST ortamına bağlanıldı (M9 kabulü).

**Kalan:** iOS build, imza / provisioning ve iOS cihaz doğrulaması. Push'un iOS
ayağı ayrıca APNs anahtarı ister (MOB-12 ile birlikte).

---

## Açık işler için gereken sözleşmeler

### Deep-link

Push'a dokunulduğunda açılacak route. M3 payload'u ile MOB-12 yönlendirmesi bu
sözleşmeye göre yazılır.

| | |
|---|---|
| Şema | `ebys://kayitlar/{recordId}` |
| Expo Router dosyası | `mobile/src/app/(app)/kayitlar/[id].tsx` (**mevcut**) |
| Kaynak alan | Push `data.recordId` |

Web'deki kanonik route `/kayitlar/:recordId` ile bilerek aynı; iki istemcide
farklı yol tutmak, bildirim metinlerini ve testleri ikiye böler.

| Uygulama durumu | Mobil tarafta |
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

### Mobilin bilmesi gereken iki backend kuralı

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

## Bağımlılıklar

1. M2 kalanı → M8 (test, düzeltilmiş davranışı sabitlemeli)
2. M2 kalanı + M3 → MOB-12
3. M0 kalanı (`openapi.json`) → MOB-12'nin üretilmiş istemciyle token kaydı
4. MOB-12 iOS ayağı → MOB-16 iOS release
5. Backend PR'ları web akışını bozmaz

---

## Kapanan işler

Aşağıdakiler koda karşı doğrulanmıştır; **yeniden açılmaz, yeniden yazılmaz.**

### Backend

| # | Sahip | İş | Kapanış |
|---|---|---|---|
| M0 | Entegrasyon | Uç envanteri ([MOBIL_API_ENVANTERI.md](MOBIL_API_ENVANTERI.md)) — hata kodları, sayfalama zarfı, `sort` tuzağı, tarih biçimi, görünürlük, dosya kuralları | 20 Ağu 2026 (`openapi.json` maddesi hariç) |
| M2 | Melih Kocaman | `device_tokens` tablosu, `POST`/`DELETE` uçları, upsert kuralı: `token` UNIQUE üzerinden eşleşir, `user_id` dahil **tüm** alanlar güncellenir (aynı telefonda başka kullanıcı giriş yaparsa push yanlış kişiye gitmesin diye) | Kodda — iki güvenlik maddesi hariç |
| M4 | Nisan Tat · Sümeyye Baykan | Çıkışta cihaz token'ı pasifleştirme. `LogoutRequest.deviceToken` opsiyonel; `AuthServiceTest` üç senaryoyu da kapsıyor: token yokken web akışı bozulmuyor, varken pasifleşiyor, **başkasınınki pasifleşmiyor** | 21 Ağu 2026 |
| M5 | Alperen Kara · Fevzi Berke Urganioğlu · Nisan Tat · Sümeyye Baykan | Koltuk devrinde `last_deputy_id` güncelleme; `assigned_to` devriyle aynı transaction'da | 20 Ağu 2026 |
| M6 | Ecesu Başak | Çoklu dosya upload — `@RequestPart("file") MultipartFile[]`, `List<FileResponseDto>` döner, tek dosyalı web çağrısı bozulmadı | 20 Ağu 2026 (`2c31c3a`) |
| M7 | Ecesu Başak | Multipart mobil senaryo testleri — `FileServiceTest`'e 130 satır eklendi | 20 Ağu 2026 (`2c31c3a`) |
| M9 | Burak Kaya | HTTPS TEST ortamı (`workflowproject-test.duckdns.org`), seed betiği, gerçek cihaz kabulü. Ayrıntı: [TEST_ORTAMI_NOTU.md](TEST_ORTAMI_NOTU.md) | 21 Ağu 2026 |

Açık işi hiç olmayan modüller: `workflow` (Esra Öncü · Burak Kaya), `search`
(Irmak Tanrıverdi), `common` hata formatı (Hacer Bengü Ünal). Alıcı matrisi
(`recipientsOf`), `createdByFullName`, dosya listesi ucu, `notifications` ve
`tokens` tabloları hazır — yeniden yazılmayacak.

### Mobil

Mobil istemciyi Yiğithan Ayhan, Zeynep Sena Şaltu ve Barkın Emre Sayar yazdı —
belgede eskiden "Kişi1 / Kişi2 / Kişi3 — henüz atanmadı" diye duran roller.

| # | İş | Kanıt |
|---|---|---|
| MOB-1 | Expo + TS projesi, ortam bazlı API adresi, EAS profilleri | `mobile/app.json`, `mobile/eas.json` |
| MOB-2 | HTTP katmanı; `401` → refresh → başarısızsa çıkış; `ApiError` parse | `src/api/client.ts` (+ `client.test.ts`) |
| MOB-3 | Giriş ve **güvenli** token saklama — `expo-secure-store`, `WHEN_UNLOCKED_THIS_DEVICE_ONLY` | `src/auth/tokenStore.ts`, `sessionManager.test.ts` |
| MOB-4 | Zorunlu parola değişimi | `src/app/(password)/sifre-degistir.tsx` |
| MOB-5 | Şifremi unuttum — üç adım | `(auth)/sifre-sifirla.tsx`, `(auth)/yeni-sifre.tsx` |
| MOB-6 | Rol bazlı ana özet | `(app)/index.tsx`, `DashboardSummaryCard.tsx` |
| MOB-7 | Kayıt listesi, filtre paneli, `createdByFullName` | `(app)/kayitlar/index.tsx`, `RecordFiltersPanel.tsx` |
| MOB-8 | Detay + işlem geçmişi + dosya listesi | `(app)/kayitlar/[id].tsx`, `RecordHistory.tsx`, `FileList.tsx` |
| MOB-9 | Oluştur / düzenle / gönder (`targetUserId` gönderilmez) | `(app)/olustur.tsx`, `RecordForm.tsx` |
| MOB-10 | Onay / red / geri gönder | `RecordWorkflowActions.tsx` |
| MOB-11 | Dosya seç / yükleme kuyruğu / indir | `FilePickerButton.tsx`, `src/services/files/uploadQueue.ts` |
| MOB-13 | Bildirim merkezi | `(app)/bildirimler.tsx` |
| MOB-14 | Profil ve çıkış | `(app)/profil.tsx` |
| MOB-15 | Hata / boş / offline UX | `components/states/`, `components/feedback/OfflineBanner.tsx` |

**Not:** Ekran ve cihaz entegrasyon testleri yoktur; Jest kapsamı auth ve ortak
API katmanıyla sınırlıdır.

---

## Kararlar (değişmedi)

| Konu | Karar |
|---|---|
| Stack | **React Native + Expo + TypeScript** (`mobile/`) — şartnameden sapma. Gerekçe: [ADR 0002](decisions/0002-mobil-istemci-teknolojisi.md) |
| Neden RN | Ekip zaten React/TS/TanStack Query/RHF/Zod biliyor; Flutter = sıfırdan Dart |
| Web bileşenleri | Kopyalanmaz. `View` / `Text` / `Pressable` / `FlatList` kullanılır |
| Roller (v1) | `CALISAN`, `BASKAN_YARDIMCISI`, `BASKAN` — **ADMIN yok** |
| Push | Backend **FCM HTTP v1**. Mobil: `@react-native-firebase/app` + `messaging` |
| Push test | **Expo Go ile olmaz.** Development build + gerçek cihaz |
