# D04 — Notification, Mobile ve Realtime Operasyon/Kabul Rehberi

Bu belge Bahadır kapsamındaki notification, web realtime, Mailpit quick action,
MOB-1 ve Android push teslimlerini yerelde tekrar üretmek için kanonik kontrol
listesidir. Secret, parola, gerçek mail-action tokenı, FCM private key veya gerçek
`google-services.json` içeriği bu belgeye ve test kanıtlarına yazılmaz.

16 Eylül 2026 durum ayrımı:

| Kabul | Durum |
| --- | --- |
| İki kullanıcıyla temel realtime bildirim ve kayıt yenileme | **PASS** — önceki gerçek browser kabul kaydı; Paket 5'te tekrar koşulmadı |
| Backend kesintisi sonrası browser reconnect | **PASS** — backend durdurulup yeniden başlatıldı; STOMP yeniden bağlandı, yeni bildirim refresh olmadan geldi ve duplicate oluşmadı |
| WebSocket yokken yaklaşık 30 saniyelik REST polling fallback | **ACCEPTANCE PENDING** |
| Mailpit → quick action → audit → ikinci kullanım reddi | **PASS** — gerçek Mailpit kabulü |
| MOB-1 dynamic role, available actions, department target, assignment/version | **PASS** — kod ve teknik test |
| Android push registration/renewal/foreground/background/cold-start/tap | **CODE COMPLETE / MANUAL DEVICE ACCEPTANCE PENDING** |

## A. Yerel Projeyi Çalıştırma

### 1. Veritabanı ve Mailpit

Maven backend çalıştırılırken Docker `backend` servisi kapalı tutulur; aksi hâlde
iki süreç de host `8080` portunu kullanmak ister. Yalnız `workflow-db` ve
`workflow-mailpit` başlatılır:

```sh
docker compose up -d db mailpit
docker compose stop backend
docker compose ps db mailpit backend
docker compose port db 5432
```

`db` sağlıklı, `mailpit` çalışır, `backend` ise stopped/not running görünmelidir.
DB host portunu her makinede `docker compose port db 5432` çıktısından doğrulayın;
belgelerdeki eski `5433` örneklerini sabit varsaymayın.

### 2. Maven backend

DB/JWT gibi hassas değerleri terminal oturumuna veya commit edilmeyen yerel ortam
dosyasına verin. Komut satırına, shell history'ye veya belgeye gerçek değer
yazmayın. Maven root `.env` dosyasını kendiliğinden okumaz.

Gerekli ortam değişkenleri yüklendikten sonra:

```sh
cd backend
DB_HOST=127.0.0.1 MAIL_HOST=127.0.0.1 MAIL_PORT=1025 \
  FRONTEND_URL=http://localhost:5173 \
  CORS_ALLOWED_ORIGINS=http://localhost:5173 \
  ./mvnw spring-boot:run
```

Kontrol noktaları:

- REST health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Mailpit: `http://localhost:8025`
- WebSocket/STOMP endpoint: `ws://localhost:8080/ws`

### 3. Vite frontend

İkinci terminalde:

```sh
cd frontend
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

Browser'ı `http://localhost:5173` ile açın. `localhost` ve `127.0.0.1` farklı
origin'lerdir. Frontend `http://127.0.0.1:5173` ile açılacaksa aynı origin
`CORS_ALLOWED_ORIGINS` listesine açıkça eklenmeli ve backend yeniden başlatılmalıdır.
DB/SMTP bağlantısında `127.0.0.1` kullanılması browser origin'ini değiştirmez.

Web istemcisi STOMP `CONNECT` frame'inde güncel access tokenı gönderir ve yalnız
`/user/queue/notifications` adresine abone olur. `/queue/notifications` adresine
doğrudan istemci aboneliği kabul hatasıdır.

## B. Realtime Browser Kabulü

### Önkoşullar

- İki ayrı kullanıcı ve iki ayrı browser profili/incognito context kullanın;
  tek profil içindeki storage paylaşımına güvenmeyin.
- Kullanıcı A'nın yapabileceği bir workflow aksiyonu ve Kullanıcı B'nin yeni
  atanan/alıcı olacağı bir kayıt hazırlayın.
- Kullanıcı B'de bildirim merkezi ve kayıt detayı açık olsun.
- DevTools Network panelinde `/ws`, notification REST istekleri ve workflow POST
  izlenebilsin. Token veya STOMP Authorization değerini ekran görüntüsüne/loga
  almayın.

### B1. Temel teslim — PASS

1. Kullanıcı B ile oturum açın; `/ws` bağlantısının açıldığını ve tek
   `/user/queue/notifications` aboneliği kurulduğunu doğrulayın.
2. Kullanıcı A ile workflow aksiyonunu tamamlayın.
3. Kullanıcı B'de sayfayı yenilemeden yeni bildirimin geldiğini doğrulayın.
4. Açık kayıt detayında durumun ve kullanılabilir aksiyonların sayfa yenilemeden
   güncellendiğini doğrulayın.
5. Tek logical notification için tek bildirim satırı bulunduğunu; reconnect veya
   polling refetch'inin ikinci satır/toast üretmediğini doğrulayın.

Bu temel davranış önceki gerçek browser kabul kaydında **PASS** durumundadır.
Paket 5 kod değiştirmedi ve kabulü tekrar koşmadı.

### B2. Reconnect — PASS

16 Eylül 2026 gerçek browser kabul turunda backend durdurulup yeniden
başlatıldı. Health tekrar `UP` olduktan sonra STOMP bağlantısı otomatik kuruldu;
yeni bildirim manuel refresh olmadan geldi, badge ve açık kayıt durumu yenilendi.
Her açık oturumda tek CONNECT / tek SUBSCRIBE gözlendi; 401/403 döngüsü veya
duplicate bildirim oluşmadı.

1. Kullanıcı B browser'ını ve sayfasını açık bırakın; refresh yapmayın.
2. Maven backend'i durdurun. Browser'da socket'in kapandığını görün.
3. Aynı ortam değişkenleriyle backend'i tekrar başlatın.
4. `@stomp/stompjs` reconnect sonrasında tek aktif socket ve tek private
   subscription oluştuğunu doğrulayın.
5. Kullanıcı A ile yeni bir workflow aksiyonu yapın.
6. Kullanıcı B'de yeni bildirimin ve kayıt durumunun refresh olmadan geldiğini;
   aynı notification ID'nin bir kez işlendiğini doğrulayın.

Bu senaryo kontrollü transport testinde kapsanır, fakat gerçek browser/backend
kesintisi turu tamamlanmadı. Sonuç yazılana kadar **PASS değildir**.

### B3. Polling fallback — ACCEPTANCE PENDING

1. Browser DevTools request blocking veya yerel proxy ile yalnız
   `ws://localhost:8080/ws` erişimini engelleyin. Browser'ı tamamen offline
   yapmayın ve backend'i durdurmayın; REST çalışmaya devam etmelidir.
2. Notification REST uçlarının `200` döndüğünü doğrulayın.
3. Başka kullanıcıyla yeni workflow aksiyonu oluşturun.
4. Realtime mesaj beklemeden en az bir tam polling aralığı geçirin.
5. Bildirimin yaklaşık 30 saniye içinde REST ile geldiğini ve tek satır olarak
   kaldığını doğrulayın. Kayıt detayının gerekirse REST ile yenilendiğini ayrıca
   kontrol edin.
6. WebSocket engelini kaldırın ve tek active client/subscription oluştuğunu
   doğrulayın.

Kodda 30 saniyelik polling sürekli açıktır; gerçek WebSocket-blocked browser turu
tamamlanmadı. Sonuç yazılana kadar **PASS değildir**.

## C. NT-7 Mailpit Quick Action Kabulü — PASS

1. `workflow-mailpit`, Maven backend ve Vite frontend'i A bölümündeki gibi açın.
2. Workflow aksiyonuyla hızlı işleme uygun alıcıya e-posta üretin.
3. `http://localhost:8025` içindeki workflow mailini açın. Gerçek tokenı belgeye,
   terminal çıktısına veya ekran görüntüsüne kopyalamayın.
4. Maildeki `/hizli-islem#token=...` bağlantısını açın. Sayfa tokenı fragment'tan
   alıp adres çubuğundan temizlemelidir.
5. `preview` ekranındaki kayıt/alıcı/aksiyon bilgisini kontrol edin; preview
   kaydı değiştirmemelidir.
6. Açık onayla `consume` çağrısını tamamlayın ve kayıt durumunun değiştiğini
   doğrulayın.
7. `audit_logs` veya yetkili İşlem Geçmişi ekranında tek workflow audit kaydı
   oluştuğunu doğrulayın.
8. Aynı bağlantıyla ikinci `consume` deneyin; `400
   INVALID_OR_EXPIRED_MAIL_ACTION_TOKEN` reddi bekleyin. İkinci workflow/audit
   kaydı oluşmamalıdır.

Gerçek Mailpit → `/hizli-islem` → preview → consume → audit → ikinci kullanım
reddi zinciri **PASS** durumundadır. Backend integration testi aynı transaction
ve tek-kullanım invariant'ını; frontend StrictMode regresyon testi fragment
temizlendikten sonra aynı component ömründeki tokenın kaybolmamasını kapsar.

## D. MOB-1 Mobil Sözleşme Kabulü — PASS

- Profil rolü kapalı bir ad enum'u değildir; `roleId`, nullable `systemKey`,
  gösterim adı ve `permissionCodes` tüketilir. Dinamik/yeniden adlandırılmış rol
  geçerli yanıttır.
- Workflow düğmeleri
  `GET /api/records/{id}/workflow/available-actions` yanıtından üretilir.
- `targetDepartmentRequired=true` ise hedefler `target-departments` ucundan gelir;
  mobil routing/rol eligibility kuralı kopyalamaz.
- `RecordResponse`, liste satırı ve `WorkflowActionResponse` içindeki `assignment`
  korunur. `assignment.kind` yalnız `USER`, `DEPARTMENT` veya `NONE`'dır.
- Kayıt detayında kullanıcı/departman ataması backend'in gösterim adıyla görünür.
- `version` detay, liste, available-actions ve workflow action yanıtında integer
  olarak tüketilir; mobil yeni bir workflow business rule üretmez.

Teknik kabul ve mobil test/typecheck/lint kaydı **PASS** durumundadır.

## E. NT-8/9 Android Push Kabulü

### Kurulum

1. Expo Go değil, `expo-notifications` içeren development veya preview native
   build ve fiziksel Android cihaz kullanın.
2. Cihaz ile geliştirme makinesini aynı LAN'a alın. Backend'in host firewall'ında
   gerekli yerel erişime izin verin; `EXPO_PUBLIC_API_BASE_URL` için makinenin LAN
   adresini kullanın. Fiziksel cihazdan `localhost`/`127.0.0.1` backend değildir.
3. Firebase Android uygulamasının package değeri `mobile/app.json` içindeki
   `expo.android.package` ile birebir aynı olmalıdır. Kalıcı package adı
   kesinleşmeden release kabulü vermeyin.
4. Gerçek `google-services.json` dosyasını `mobile/google-services.json` altında
   yerel tutun. Git tarafından izlenmediğini doğrulayın; içeriğini belgeye veya
   issue/PR'a yapıştırmayın.
5. Backend'e `FCM_PROJECT_ID`, `FCM_CLIENT_EMAIL` ve `FCM_PRIVATE_KEY` değerlerini
   secret/environment yönetimiyle verin. Değerleri repository'ye yazmayın.

### Lifecycle kontrol listesi

1. **Registration:** login sonrası native tokenın `POST /api/device-tokens` ile
   kaydedildiğini ve aynı tokenın idempotent upsert edildiğini doğrulayın.
2. **Renewal:** token yenilemesini tetikleyin veya test ortamındaki rotation
   yöntemini kullanın; listener'ın yeni tokenı tekrar kaydettiğini doğrulayın.
3. **Foreground:** uygulama açıkken push'ın handler ile gösterildiğini doğrulayın.
4. **Background:** uygulama arka plandayken push'ın sistem tepsisinde göründüğünü;
   dokununca doğru kayıt detayına gidildiğini doğrulayın.
5. **Cold-start:** uygulamayı kapatın, push'a dokunarak açın; last notification
   response'ın bir kez tüketildiğini ve doğru kayda yönlendirdiğini doğrulayın.
6. **Payload guard:** eksik/geçersiz `recordId` ile navigasyon yapılmadığını;
   geçerli UUID'nin `/(app)/kayitlar/{recordId}` rotasına gittiğini doğrulayın.

Kod ve otomatik testler tamamdır. Firebase bağlı fiziksel Android cihaz turu
yapılmadığından nihai durum açıkça:

**MANUAL DEVICE ACCEPTANCE PENDING**

## Otomatik Kanıt Komutları

Backend notification/realtime unit paketi (PostgreSQL istemez):

```sh
cd backend
./mvnw -o --batch-mode --no-transfer-progress \
  -Dtest=NotificationServiceTest,RealtimeNotificationPublisherTest,RealtimeNotificationEventListenerTest,WorkflowStatusChangedListenerTest,StompJwtChannelInterceptorTest,MailActionTokenServiceTest,PushNotificationServiceTest \
  test
```

B01 integration testi PostgreSQL ve izole şema kullanır:

```sh
cd backend
./mvnw -Dtest=MailActionTokenIntegrationTest test
```

Frontend ilgili regresyonlar:

```sh
cd frontend
NODE_OPTIONS=--no-experimental-webstorage npm test -- \
  src/hooks/useRealtimeNotifications.transport.test.tsx \
  src/hooks/useRealtimeNotifications.test.tsx \
  src/hooks/useNotificationCenter.test.tsx \
  src/pages/NotificationsPage.test.tsx \
  src/pages/QuickActionPage.test.tsx \
  src/App.test.tsx
npm run lint
npm run build
```

Mobil tam teknik kontrol:

```sh
cd mobile
npm test -- --runInBand
npm run typecheck
npm run lint
```

Otomatik testler reconnect/polling gerçek browser kabulünün veya FCM fiziksel
cihaz kabulünün yerine geçmez.
