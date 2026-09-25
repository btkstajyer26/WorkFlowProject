# Mobil Entegrasyon — Görev Dağılımı

> **Arşiv belgesi.** Aşağıdaki görev dağılımı 31 Ağustos 2026 durumunu kaydeder; aktif iş listesi değildir. Kod, dal, commit ve sayı referansları (ör. `openapi.json` için "30 yol") o tarihe aittir. Güncel API sözleşmesi [Mobil API envanterinde](../MOBIL_API_ENVANTERI.md), teslim sınırları [dokümantasyon dizinindedir](../README.md). Maddelerin bir kısmı hâlâ açık olabilir; doğrulamadan iş listesi olarak kullanmayın.

**Tarih:** 31 Ağustos 2026 (kod doğrulaması aynı gün)
**Dal:** `test`'ten `feature/<konu>` açılır, PR `test`'e gider.

Mobil, mevcut Spring Boot REST API'sini kullanır. İş kuralları mobilde yazılmaz.
Sözleşme: [FRONTEND_BACKEND_SOZLESMESI.md](../FRONTEND_BACKEND_SOZLESMESI.md) ·
Uç envanteri: [MOBIL_API_ENVANTERI.md](../MOBIL_API_ENVANTERI.md)

Bu belge **yalnız açık işleri** taşır. Kapanan işler en altta tek satırlık kayıt
olarak durur; tasarım gerekçeleri envanter ve mimari belgelerindedir.

---

## Açık işler

| # | Sahip | Modül | İş | Durum |
|---|---|---|---|---|
| M0-kalan | Entegrasyon | — | `docs/openapi.json` Springdoc çıktısından yeniden üretildi (30 yol, 35 şema) | ✅ |
| MOB-12a | Mobil | `mobile/` | Token yenileme dinleyicisi (`addPushTokenListener`) | 🔴 |
| MOB-12b | Mobil | `mobile/` | Soğuk açılış bildirim yönlendirmesi | 🔴 |
| MOB-12c | Mobil | `mobile/` | Gerçek Android cihaz push kanıtı | 🔴 |
| MOB-12d | Mobil | `mobile/` | `google-services.json` ve development build kurulumunun tamamlanması | 🟡 |
| MOB-16 | Mobil | `mobile/` | iOS release / imza | 🟡 |

> **En kritik madde MOB-12'nin cihaz kanıtıdır.** Backend tarafı hazır; mobil
> istemci `expo-notifications` ile native tokenı alıyor, `/api/device-tokens`
> üzerinden kaydediyor ve açık uygulamada bildirime dokunulduğunda doğru kayıt
> ekranına yönlendiriyor. Token yenileme, soğuk açılış ve gerçek cihaz turu eksik.

> ✅ **PR #43 merge edilmeden kapatıldı.** `feature/notification-service` dalının 24 Ağustos'taki
> dört yeni commit'i e-posta üzerinden tek tıkla onay özelliği getiriyor, ancak
> uç kimlik doğrulaması olmadan açılıyor ve kaydın atandığı kişi adına workflow
> aksiyonu yürütüyor. Ayrıntı ve düzeltme koşulları aşağıda "Engellenen iş"
> bölümünde.

---

## Backend açık işleri

### 👤 Entegrasyon — M0 kalanı: `openapi.json` güncellemesi ✅

[docs/openapi.json](../openapi.json), 31 Ağustos'ta PostgreSQL gerektirmeyen Spring
test bağlamındaki gerçek `/v3/api-docs` yanıtından yeniden üretildi. Dosya **30
yol, 35 şema** içerir; `/api/device-tokens` ve `/api/public/mail-actions/*`
uçları ile çoklu `file` şeması doğrulandı.

Mobil istemci API katmanı el yazımıdır; frontend üretimi ise çalışan backend'in
`/v3/api-docs` çıktısını kullanır. Depodaki dosya istemci üretim kaynağı değil,
sürümlenmiş sözleşme anlık görüntüsüdür ve güncel Springdoc çıktısından alınmalıdır.

Geçici üretim testi işlem sonunda kaldırıldı; çalışan test kodunda yalnız mevcut
erişim doğrulaması kaldı.

**Bitti sayılır:** `docs/openapi.json` içinde `/api/device-tokens` ve
`/api/public/mail-actions/*` uçları var.

---

## Mobil açık işleri

`mobile/` paketi Expo + React Native + TypeScript. 31 Ağustos son tekil turunda
`typecheck` geçti ve 13 dosyadaki 64 Jest testinin tamamı yeşil oldu; önceki
paralel turda bir ekran testi zaman aşımına uğradı. Lint resolver'ı, kurulu ve manifestte bulunan bazı
Expo/Testing Library bağımlılıklarını çözemedi. CI'da `Mobile / quality` işi aynı kontrolleri çalıştırır.

### MOB-12 — Push istemci temeli tamamlandı; cihaz kabulü açık 🔄

**Bağımlılık:** M2 ve M3 — ikisi de kapandı, backend hazır.

`pushNotificationManager.ts`, `expo-notifications` üzerinden izin ister,
`getDevicePushTokenAsync()` ile native FCM/APNs tokenını alır ve oturum açan
kullanıcı için `POST /api/device-tokens` çağrısı yapar. Bildirime dokunma
dinleyicisi `recordId` içeren bildirimi `/(app)/kayitlar/{id}` rotasına taşır.
Bu temel birim testleriyle kapsanmıştır; gerçek cihazda uçtan uca kanıt yoktur.

Paket: `expo-notifications` (Expo SDK 57 development build; Expo Go yeterli değildir)

1. ✅ Giriş sonrası `POST /api/device-tokens` — `platform` ve `deviceName` ile.
2. ✅ Foreground/background bildirim dokunuşunda kayıt detayına yönlendirme.
3. **`addPushTokenListener` eklenmeli.** Native token kendiliğinden yenilenebilir
   (uygulama verisi silinmesi, yeniden kurulum, uzun süre kullanılmama). Yeni
   token anında `POST /api/device-tokens`'a yazılır; yazılmazsa cihaz sessizce
   bildirim almayı bırakır ve bu **hata olarak görünmez**.
4. `getLastNotificationResponseAsync()` ile kapalı uygulamadan açılış işlenmeli.
5. `google-services.json` örnekten kurulmalı; temiz checkout development build'i belgelenmeli.
6. Çıkışta token'ı ayrıca silme; `POST /api/auth/logout` gövdesine `deviceToken`
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

## E-posta hızlı işlem — güvenli sürüm yazıldı

`origin/feature/notification-service` dalındaki dört commit
(`2b602f4`, `f5173cd`, `b91d897`, `dbb8523`) **alınmadı**; özellik güvenli
deseniyle yeniden yazıldı. Fikir aynı: durum bildirimi e-postasına "Hızlı
İşlem" düğmesi koyup akışı hızlandırmak.

### Reddedilen sürümdeki sorunlar

| # | Sorun |
|---|---|
| 1 | `GET /api/public/notification/quick-action?recordId=&action=` kimlik doğrulaması olmadan açıktı ve kaydın `assignedTo` kişisini bulup **onun adına** aksiyon yürütüyordu. Kayıt UUID'sini bilen herkes evrağı onaylayabilirdi. |
| 2 | Durum değiştiren bir `GET`. Posta ağ geçitleri linkleri önceden getirir; alıcı dokunmadan evrak onaylanabilirdi. |
| 3 | `catch (Exception e)` hatayı yutup her koşulda `302` dönüyordu; başarısız işlem başarılı görünüyordu. |
| 4 | `String.valueOf(actor.getRole())` bir entity üzerinde çalışıyordu; üretilen authority geçersizdi. |
| 5 | `spring.flyway.validate-on-migrate=false` — "uygulanmış migration'ı değiştirmeyin" kuralını zorlayan checksum kontrolü kapatılmıştı. |
| 6 | `sendPasswordResetCode` Thymeleaf şablonundan string birleştirmeye düşürülmüştü. |

> Ayrıca özellik **zaten çalışmıyordu.** `SecurityCurrentActorProvider`
> principal'in `AuthenticatedUser` olmasını şart koşar
> (`principal.getClass() != AuthenticatedUser.class` → `AuthenticationServiceException`).
> Reddedilen kod principal olarak `User` entity'si koyuyordu, yani her çağrı
> istisna atıp yutulan `catch` bloğuna düşer ve kullanıcı `302` görürdü.

### Yazılan sürüm

**Migration:** `V11__mail_action_tokens.sql` — `mail_action_tokens` tablosu.
Anahtarın kendisi saklanmaz, yalnız SHA-256 özeti (parola sıfırlamadaki desen).

Anahtar üç sınırla birlikte çalışır:

1. `consumed_at` → tek kullanım
2. `expires_at` → süre (`MAIL_ACTION_TOKEN_TTL_HOURS`, varsayılan 72 saat)
3. `user_id` + `record_id` + `action` → başka evrağa, aksiyona veya kişiye taşınamaz

Bu üçü yetmez: tüketimde **gerçek durum makinesi yeniden çalışır**. Evrak arada
el değiştirdiyse geçiş oradan reddedilir. Tablo yetki kaynağı değildir.

**Uçlar** — ikisi de `POST`, ikisi de oturumsuz:

| Uç | Ne yapar |
|---|---|
| `POST /api/public/mail-actions/preview` | Anahtarı doğrular, onay ekranı bilgisini döner. **Durum değiştirmez**, güvenle önceden getirilebilir. |
| `POST /api/public/mail-actions/consume` | Anahtarı tüketir ve aksiyonu yürütür. Yalnız kullanıcının açık onayıyla çağrılır. |

Önizleme de `POST`'tur çünkü anahtar **gövdede** taşınır; URL'ye yazılsaydı
erişim log'larına, `Referer` başlığına ve tarayıcı geçmişine düşerdi. Proje bu
kararı `afbf4b5` ile şifre sıfırlama akışında zaten vermişti.

`SecurityConfig`'de joker yol (`/api/public/**`) **bilerek kullanılmadı**; iki
uç adıyla açıldı. Joker, o önekle eklenen her yeni ucu sessizce herkese açardı;
`AuthorizationMatrixTest` bunu sabitleyen bir test taşıyor.

**Aktör anahtardan çözülür**, evrağın `assignedTo` alanından türetilmez —
türetilseydi anahtar, koltuk devredildikten sonra yeni kişinin adına iş yapardı.
`consume` içinde `SecurityContext` anahtarın sahibiyle kurulur; anahtar
doğrulandığı için bu, JWT filtresinin yaptığı işin aynı yetkiye dayanan
eşdeğeridir. Önceki bağlam `finally` ile aynen geri konur.

**Tüketim aksiyondan önce yazılır.** Aksiyon durum makinesinden dönerse
transaction geri alınır ve anahtar tüketilmemiş kalır. "Tüketildi ama iş
yapılmadı" ya da "iş yapıldı ama anahtar açık kaldı" aralığı oluşmaz.

**E-posta bağlantısı:** `{frontendUrl}/hizli-islem#token=...` — anahtar adres
**parçasında**. Parça sunucuya hiç gönderilmez. Arayüz değeri okur okumaz
`history.replaceState` ile adresten siler.

**Hangi aksiyon önerilir:** rolden değil evrağın yeni durumundan türetilir
(`MailActionTokenService.primaryActionFor`). Bildirim zaten yalnız evrağın
atandığı kişiye gider, dolayısıyla durum alıcıyı tek bir aksiyona bağlar.

| Durum | Önerilen aksiyon |
|---|---|
| `BSK_YRD_INCELEMESINDE` | `BASKANA_ILET` |
| `BASKAN_INCELEMESINDE` | `ONAYLA` |
| `DUZENLEME_BEKLIYOR` | `TEKRAR_GONDER` |
| `TASLAK`, `ONAYLANDI`, `REDDEDILDI` | yok — düğme çıkmaz |

Geri gönderme ve ret bilerek dışarıdadır: ikisinde de açıklama zorunludur, tek
tıkla yapılamaz.

**Testler:** `MailActionTokenServiceTest` (14), `AuthorizationMatrixTest`
içinde 4 uç testi, `WorkflowStatusChangedListenerTest`'te 3 yeni senaryo,
frontend `QuickActionPage.test.tsx` (6).

### Kalan

- Gerçek SMTP/Mailpit üzerinden uçtan uca deneme yapılmadı (Docker gerekiyordu).
- Süresi dolmuş satırların toplu temizliği için zamanlanmış iş yok; indeks
  (`idx_mail_action_tokens_expires_at`) hazır, iş tanımlanmadı.

---

## Açık işler için gereken sözleşmeler

### Push payload

Backend'in gönderdiği payload. Mevcut MOB-12 yönlendirmesi `recordId` alanını kullanır.

```json
{
  "notification": { "title": "...", "body": "..." },
  "data": {
    "recordId": "uuid",
    "type": "RECORD_SUBMITTED"
  }
}
```

`type` değerleri `NotificationType` enum'undan gelir; yeni sözlük uydurulmaz:
`RECORD_SUBMITTED` · `RECORD_FORWARDED` · `RECORD_APPROVED` ·
`RECORD_REJECTED` · `RECORD_RETURNED`

`data` alanları **string** olmalı (FCM `data` yalnız string kabul eder).

Geçersiz token temizliği backend'de yapılır — mobil tarafın ayrıca token
silmesi gerekmez:

| FCM cevabı | Backend ne yapar |
|---|---|
| `UNREGISTERED` (uygulama silinmiş / token iptal) | `is_active = false` |
| `INVALID_ARGUMENT` (token bozuk) | `is_active = false` |
| `UNAVAILABLE` / `INTERNAL` | Pasifleştirme **yok**, geçici hata |

### Deep-link

Push'a dokunulduğunda açılacak route. Yukarıdaki payload'un `data.recordId`
alanı ile mevcut MOB-12 yönlendirmesi bu sözleşmeyi uygular.

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

1. M0 kalanı (`openapi.json`) → sürümlenmiş API sözleşmesi güncel kalır; mobil istemci el yazımıdır
2. MOB-12 iOS ayağı → MOB-16 iOS release
3. MOB-12d development build kurulumu → MOB-12c gerçek cihaz kanıtı
4. Backend PR'ları web akışını bozmaz

M2 → M8 ve M2 + M3 → MOB-12 zincirlerindeki engeller 24 Ağustos'ta kalktı; M8 de
aynı gün kapandı.

---

## Kapanan işler

Aşağıdakiler koda karşı doğrulanmıştır; **yeniden açılmaz, yeniden yazılmaz.**

### Backend

| # | Sahip | İş | Kapanış |
|---|---|---|---|
| M0 | Entegrasyon | Uç envanteri ([MOBIL_API_ENVANTERI.md](../MOBIL_API_ENVANTERI.md)) — hata kodları, sayfalama zarfı, `sort` tuzağı, tarih biçimi, görünürlük, dosya kuralları | 20 Ağu 2026 (`openapi.json` maddesi hariç) |
| M2 | Melih Kocaman | `device_tokens` tablosu, `POST`/`DELETE` uçları, upsert kuralı: `token` UNIQUE üzerinden eşleşir, `user_id` dahil **tüm** alanlar güncellenir (aynı telefonda başka kullanıcı giriş yaparsa push yanlış kişiye gitmesin diye) | 20 Ağu 2026 |
| M1 | Ebrar Şeyma Karakuş | İşlem geçmişi ucunun mobil doğrulaması: kırpma kuralları ve her zaman `null` dönen HTTP alanları envanter §5'te yazılı; cevap boyutu ölçülüp **sayfalama eklenmeme kararı** gerekçesiyle kayıtlı (gerçekçi en kötü durum 20 geri gönderme turu = 106 satır = 44,5 KB) | 24 Ağu 2026 |
| M8 | Hacer Bengü Ünal | `/api/device-tokens` yetki matrisi testi. `AuthorizationMatrixTest.DeviceTokenYonetimi`: auth'suz `POST` ve `DELETE` 401 döner; girişli kullanıcı kendi tokenını kaydedip silebilir. Kayıt testi principal olarak `AuthenticatedUser` kullanıyor — gerçek filtre davranışını yansıtıyor | 24 Ağu 2026 |
| M2-kalan | Melih Kocaman | `DELETE /api/device-tokens` artık `(token, user_id)` çifti üzerinden çalışıyor (`deactivateByTokenAndUserId`); kullanıcıya ait olmayan token sessizce yok sayılır. Token değerleri log'da `maskToken` ile maskeleniyor. `DeviceTokenServiceTest` sahiplik senaryolarını kapsıyor | 24 Ağu 2026 |
| M3 | Melih Kocaman | `PushNotificationService` `WorkflowStatusChangedListener`'a bağlandı; alıcılar mevcut `recipientsOf` matrisinden geliyor, servis `@Nullable` (FCM yapılandırılmamış ortamda akış pushsuz çalışır). `PushNotificationServiceTest` `UNREGISTERED`/`INVALID_ARGUMENT` pasifleştirmesini ve `UNAVAILABLE`'ın pasifleştirmediğini doğruluyor. **Gerçek cihazda doğrulanmadı — MOB-12 açık** | 24 Ağu 2026 |
| M4 | Nisan Tat · Sümeyye Baykan | Çıkışta cihaz token'ı pasifleştirme. `LogoutRequest.deviceToken` opsiyonel; `AuthServiceTest` üç senaryoyu da kapsıyor: token yokken web akışı bozulmuyor, varken pasifleşiyor, **başkasınınki pasifleşmiyor** | 21 Ağu 2026 |
| M5 | Alperen Kara · Fevzi Berke Urganioğlu · Nisan Tat · Sümeyye Baykan | Koltuk devrinde `last_deputy_id` güncelleme; `assigned_to` devriyle aynı transaction'da | 20 Ağu 2026 |
| M6 | Ecesu Başak | Çoklu dosya upload — `@RequestPart("file") MultipartFile[]`, `List<FileResponseDto>` döner, tek dosyalı web çağrısı bozulmadı | 20 Ağu 2026 (`2c31c3a`) |
| M7 | Ecesu Başak | Multipart mobil senaryo testleri — `FileServiceTest`'e 130 satır eklendi | 20 Ağu 2026 (`2c31c3a`) |
| M9 | Burak Kaya | HTTPS TEST ortamı (`workflowproject-test.duckdns.org`), seed betiği, gerçek cihaz kabulü. Ayrıntı: [M9 TEST kabul kanıtı](M9_TEST_KABUL_KANITI.md) | 21 Ağu 2026 |

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
| MOB-12 temel | `expo-notifications` ile izin, native token kaydı ve sıcak bildirim dokunuşunda kayıt yönlendirmesi | `pushNotificationManager.ts`, `deviceTokens.ts` ve testleri |
| MOB-13 | Bildirim merkezi | `(app)/bildirimler.tsx` |
| MOB-14 | Profil ve çıkış | `(app)/profil.tsx` |
| MOB-15 | Hata / boş / offline UX | `components/states/`, `components/feedback/OfflineBanner.tsx` |

**Not:** Jest paketi API/auth dışında kayıt aksiyonları, durum rozeti, dosya listesi
ve offline banner gibi ekran bileşenlerini de kapsar. Gerçek cihaz ve tam rota E2E testi yoktur.

---

## Kararlar (değişmedi)

| Konu | Karar |
|---|---|
| Stack | **React Native + Expo + TypeScript** (`mobile/`) — şartnameden sapma. Gerekçe: [ADR 0002](../decisions/0002-mobil-istemci-teknolojisi.md) |
| Neden RN | Ekip zaten React/TS/TanStack Query/RHF/Zod biliyor; Flutter = sıfırdan Dart |
| Web bileşenleri | Kopyalanmaz. `View` / `Text` / `Pressable` / `FlatList` kullanılır |
| Roller (v1) | `CALISAN`, `BASKAN_YARDIMCISI`, `BASKAN` — **ADMIN yok** |
| Push | Backend **FCM HTTP v1**. Mobil: Expo SDK 57 `expo-notifications`; native FCM/APNs tokenı `getDevicePushTokenAsync()` ile alınır |
| Push test | **Expo Go ile olmaz.** Development build + gerçek cihaz |
