# ADR-0004: Bildirim Teslimi, Realtime Web ve Mobil Push Mimarisi

- Durum: Kabul Edildi
- Tarih: 2026-09-16
- Karar sahipleri: Proje ekibi · Bahadır (notification/mobile)

## Bağlam

Workflow durum değişikliği tek bir iş olayıdır; uygulama içi bildirim, web
istemcisinin anlık yenilenmesi, e-posta ve mobil push bu olayın farklı teslim
kanallarıdır. Kanallar aynı alıcı ve iş kuralını istemcilerde yeniden kurmamalı,
başarısız bir transaction dış sisteme kalıcı bir bildirim çıkarmamalı ve realtime
kesintisi kullanıcıyı bildirimlerden tamamen koparmamalıdır.

Web istemcisinde REST bildirim merkezi ve 30 saniyelik polling zaten bulunuyordu.
Realtime kanal eklenirken bu güvenli geri dönüş yolunu kaldırmamak; JWT kimliğini
STOMP bağlantısına taşırken yeni bir access-token kopyası saklamamak; kullanıcıların
ortak broker kuyruğunu dinlemesini engellemek gerekiyordu. React StrictMode,
remount ve reconnect ise aynı mantıksal bildirimin birden çok kez işlenmesi riskini
görünür kıldı.

Mobilde backend FCM HTTP v1 ile push gönderebilir. İstemcinin native token kaydı,
token yenilenmesi ve foreground/background/cold-start yaşam döngüsünü tek yerde
yönetmesi; bildirime dokunulduğunda yalnız payload'daki `recordId` ile kayıt
detayına gitmesi gerekir. Expo Go native push kabul ortamı değildir.

## Değerlendirilen Seçenekler

1. **Yalnız REST polling.** Basittir, ancak bildirim ve kayıt durumu en fazla
   polling aralığı kadar gecikir.
2. **Bütün istemcilerin doğrudan ortak `/queue/notifications` kuyruğuna abone
   olması.** Kurulumu kolaydır, fakat kullanıcı izolasyonunu broker adresine
   bırakır ve başka kullanıcıların payload'larını sızdırabilir.
3. **JWT ile kimliklendirilmiş STOMP user destination + kalıcı REST polling
   fallback'i.** Düşük gecikme, kullanıcı izolasyonu ve kesinti toleransı sağlar;
   bağlantı yaşam döngüsü ile duplicate koruması ek sorumluluk getirir.

## Karar

**Seçenek 3 uygulanır.** REST ve realtime birbirinin alternatifi değil, aynı
sunucu gerçeğine ulaşan tamamlayıcı kanallardır.

### WebSocket, kimlik ve destination

- WebSocket/STOMP endpoint'i `/ws`'dir.
- İstemci her STOMP `CONNECT` frame'inde o andaki access tokenı
  `Authorization: Bearer <access token>` native header'ıyla gönderir.
- Backend doğrulanmış principal olarak kullanıcının e-posta adresini kullanır.
- İstemci yalnız `/user/queue/notifications` adresine abone olur.
- Sunucu kullanıcıya özel teslimi `convertAndSendToUser(email,
  "/queue/notifications", payload)` ile yapar. Sunucu destination'ı
  `/queue/notifications`'dır.
- İstemcinin doğrudan paylaşılan `/queue/notifications` adresine abone olması
  yasaktır. Private destination aboneliği de authenticated principal gerektirir.

### Transaction ve payload

Realtime teslim zinciri şöyledir:

```text
Notification DB save
  -> application event
  -> başarılı transaction commit
  -> AFTER_COMMIT listener
  -> realtime publisher
  -> /user/queue/notifications
```

Rollback edilen işlem realtime mesaj üretmez. Realtime payload'ı REST ile aynı
`NotificationResponse` modelidir: `id`, `recordId`, `message`,
`notificationType`, `read` ve `createdAt`.

### Web istemcisi ve fallback

- Web istemcisi `@stomp/stompjs` kullanır ve kopma sonrası yeniden bağlanır.
- Token ilk render'da kopyalanıp dondurulmaz; her bağlantı/reconnect öncesi güncel
  access token okunur.
- Mesaj iş kuralı taşımaz. İstemci notification list/count sorgularını ve varsa
  `recordId` ile kayıt detay/liste sorgularını invalid eder; otorite yine REST
  API'dir.
- 30 saniyelik HTTP polling kaldırılmaz. WebSocket kapalı veya erişilemezken
  sürekli fallback olarak çalışır.

### Duplicate ve yaşam döngüsü koruması

- Bir sekmedeki hook sahipleri tek STOMP client ve tek aktif subscription paylaşır.
- Son sahip ayrıldığında subscription ve bağlantı kapatılır; yeni bağlantı önceki
  native socket gerçekten kapanmadan açılmaz.
- Aynı `NotificationResponse.id` aynı `QueryClient` yaşam döngüsünde bir kez
  işlenir. Reconnect/remount tekrar teslimi yeni bir UI yan etkisi üretmez.
- Oturum/token temizlendiğinde bağlantı kapatılır ve teslim geçmişi temizlenir;
  hesap/rol değişiminde yeni `QueryClient` kullanılır.
- ID'siz veya bozuk payload yalnız cache yenileme sinyali olabilir; istemci mesaj
  metnine bakarak sahte bir logical-notification kimliği üretmez.

### Mobil push yaşam döngüsü

- Fiziksel cihazın native FCM/APNs tokenı authenticated
  `POST /api/device-tokens` ile kaydedilir.
- Native token yenilendiğinde listener yeni tokenı backend'e yeniden kaydeder.
- Foreground bildirimi uygulama handler'ıyla gösterilir; background bildirimi
  platform tarafından teslim edilir.
- Warm/background tap ve cold-start response aynı doğrulanmış `recordId` yolunu
  kullanarak `/(app)/kayitlar/{recordId}` ekranına gider.
- Aynı response identifier bir istemci yaşam döngüsünde iki kez yönlendirme
  üretmez. Geçersiz veya UUID olmayan `recordId` yok sayılır.

### Güvenlik sınırları

- Realtime için access tokenın storage'a yeni bir kopyası yazılmaz; mevcut oturum
  belleğindeki güncel token okunur.
- Workflow/business kuralları web veya mobile kopyalanmaz. İstemciler backend'in
  `available-actions`, `target-departments`, `assignment` ve `version`
  sözleşmelerini tüketir; backend her mutasyonu yeniden doğrular.
- Firebase servis hesabı, private key ve gerçek `google-services.json` repository'ye
  gömülmez. Backend FCM bilgilerini ortam değişkenlerinden alır; mobil dosya yerel
  ve Git tarafından izlenmeyen yapılandırmadır.

## Sonuçlar

- WebSocket gecikmeyi düşürürken REST endpoint'leri ve polling kesinti toleransını
  korur.
- User destination ve e-posta principal'ı kullanıcı izolasyonunu açık hâle getirir;
  doğrudan shared queue aboneliği güvenlik ihlalidir.
- Bildirim satırı commit edilmeden realtime yayın yapılamaz; buna karşılık commit
  sonrası broker teslimi best-effort'tur. Kalıcı outbox veya çok-instance broker
  bu kararın kapsamında değildir.
- Sekme içi ortak client ve ID tabanlı işleme, reconnect/remount duplicate etkisini
  sınırlar. ID geçmişi bellektedir; tam sayfa yenilemesinden sonra korunmaz.
- Mobil push için native build, Firebase ve fiziksel cihaz zorunluluğu operasyonel
  kabulün parçasıdır; unit test tek başına gerçek FCM teslimini kanıtlamaz.

## Acceptance / Operasyon Notları

- Backend STOMP/realtime/recipient/push testleri ve frontend transport/cache
  testleri kod yolunu kapsar.
- İki kullanıcıyla temel realtime teslim ve kaydın refresh olmadan yenilenmesi
  mevcut kabul kaydında geçmiştir.
- Gerçek tarayıcıda backend kesintisi sonrası reconnect kabulü:
  **PASS** — backend yeniden başlatıldıktan sonra STOMP otomatik yeniden bağlandı; yeni bildirim refresh olmadan geldi ve duplicate oluşmadı.
- WebSocket engelliyken yaklaşık 30 saniyede REST polling fallback kabulü:
  **ACCEPTANCE PENDING**.
- Android registration, renewal, foreground, background, cold-start ve tap kodu
  testlidir; Firebase yapılandırılmış fiziksel cihaz uçtan uca turu:
  **MANUAL DEVICE ACCEPTANCE PENDING**.
- Tekrarlanabilir çalıştırma, Mailpit, browser ve cihaz kontrol listesi
  [D04 kabul rehberindedir](../D04_NOTIFICATION_MOBILE_REALTIME_KABUL_REHBERI.md).

## Bağlantılar

- [D04 notification/mobile/realtime kabul rehberi](../D04_NOTIFICATION_MOBILE_REALTIME_KABUL_REHBERI.md)
- [Bahadır final handoff](../reviews/2026-09-16-bahadir-final-handoff.md)
- [Frontend–backend sözleşmesi](../FRONTEND_BACKEND_SOZLESMESI.md#10-bildirimler)
- [Mobil API envanteri](../MOBIL_API_ENVANTERI.md#9-cihaz-tokenları--apidevice-tokens)
- [Duplicate notification incelemesi](../reviews/2026-09-14-duplicate-notifications.md)
