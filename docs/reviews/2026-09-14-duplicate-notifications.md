# Duplicate notification incelemesi — Paket 1

Durum (14 Eylül kaydı): **CODE COMPLETE / MANUAL ACCEPTANCE PENDING; reconnect ve polling
fallback kabulü bu inceleme tarihinde tamamlanmamıştı.**

> **16 Eylül 2026 güncellemesi:** Backend kesintisi sonrası gerçek browser
> reconnect kabulü PASS oldu. WebSocket engelli, REST açık ve hedef sekme sürekli
> odaktayken izole 30 saniyelik polling fallback kabulü hâlâ PENDING durumundadır.

> **16 Eylül handoff güncellemesi:** İki kullanıcıyla temel workflow → realtime
> notification → record refresh kabulü sonraki kabul kaydında PASS'tir. Bu
> incelemenin aşağıdaki 14 Eylül notları, o adım henüz sayılmamışken alınmış
> tarihsel gözlemdir. Backend kesintisi sonrası reconnect ve WebSocket engelliyken
> 30 saniyelik polling fallback gerçek browser turları hâlâ
> **ACCEPTANCE PENDING** durumundadır.

Manuel bildirilen sürekli popup'ın kesin kök nedeni henüz doğrulanmadı.
Aşağıdaki tekrar üretimler kontrollü transport testlerine aittir; gerçek
tarayıcıda tek workflow aksiyonu için uçtan uca ölçüm olarak okunmamalıdır.

## Düzeltmeden önce toplanan kanıt

Gerçek `@stomp/stompjs` Client ve frame parser kullanıldı; yalnız WebSocket
transportu kontrollü test nesnesiyle değiştirildi. Native close geciktirildi.
Yeni test dosyası eski uygulamada **1 başarılı / 3 başarısız** sonuç verdi:

- StrictMode başlangıcı ve normal reconnect: bir socket / bağlantı başına bir SUBSCRIBE.
- Eski mount kapanırken yeni mount: ilk socket hâlâ CLOSING iken ikinci socket açıldı.
- İki eşzamanlı hook sahibi: iki socket açıldı.
- Aynı ID iki kez teslim edildiğinde: 5 yerine 10 query invalidation çağrısı oluştu.

Kapanış sorununun kaynağı kurulu kitaplığın `augment-websocket.ts` dosyasında
doğrulandı: `deactivate({ force: true })`, native close tamamlanmadan sentetik
onclose çağırır. Dolayısıyla önceki `await disconnecting` gerçek kapanışı beklemiyordu.
Bu örtüşme tek başına sürekli popup üretildiğinin kanıtı değildir; disposed
kontrolü eski hook callback'inin cache değiştirmesini zaten engelliyordu.

## İstenen 11 ihtimal

| No | İnceleme ve sonuç |
| --- | --- |
| 1 | Her hook mount'u ayrı Client yaratıyordu. İki eşzamanlı sahip ile iki socket testte yeniden üretildi; artık sekme içindeki sahipler tek transport paylaşıyor. |
| 2 | Tek bağlantının normal onConnect akışında bir SUBSCRIBE var. İki bağımsız mount iki bağlantı/abonelik oluşturabiliyordu. Artık ortak transport ve saklanan subscription var. |
| 3 | Normal StrictMode ilk mount/unmount/mount döngüsü eski kodda da bir socket açtı. Bağlı client'ın kapanışı sırasında remount örtüşmesi ayrı testte başarısızdı. |
| 4 | Normal kopma/reconnect eski ve yeni kodda bağlantı başına bir abonelik kurdu. Zorunlu deactivate sonrası remount gerçek close beklemiyordu; düzeltildi. |
| 5 | Token listener cleanup vardı, fakat açık unsubscribe yoktu ve force deactivate erken tamamlanıyordu. Artık unsubscribe, deactivate ve native socket.close kullanılıyor; yeni bağlantı gerçek close'u bekliyor. |
| 6 | Üretim kaynaklarında tek çağrı yeri `ProtectedApplication`; App içinde ikinci realtime hook çağrısı bulunmadı. Eşzamanlı mount koruması ayrıca test edildi. |
| 7 | Gerçek oturumun MESSAGE/message-id sayımı elde edilmedi. Testte aynı ID tekrar teslim edildiğinde her teslim callback'e giriyordu. Artık ID başına yan etki bir kez uygulanıyor; callback'in ağ teslim sayısı ile işlenen event sayısı ayrı kavramlardır. |
| 8 | Workflow listener alıcıları Set ile tekilleştiriyor; alıcı başına bir create, create başına bir save/event, AFTER_COMMIT listener başına bir publish ve mevcut kullanıcı için bir convertAndSendToUser yolu var. Birim testler bu çağrıları doğruluyor; geçmiş canlı publish çağrılarının sayısı ölçülmedi. |
| 9 | Yerel DB'de 3 satır / 3 farklı ID var. Aşağıdaki audit eşleşmesi üç ayrı workflow aksiyonu gösteriyor. İncelenen veri için duplicate DB kaydı yok. |
| 10 | Realtime callback toast/banner açmıyor, yalnız cache invalidate ediyor. NotificationCenter API listesini gösteriyor. Ekran testinde invalidation/refetch tek satırı korudu, toast üretmedi. |
| 11 | 30 saniyelik polling listeleri/count'u yeniliyor. Ekran testinde üç polling çevrimi ve bir invalidation sonrasında aynı persisted ID tek satır kaldı, toast/status/alert oluşmadı. Gerçek bağlantı kesilerek fallback kabulü yapılmadı. |

## Yerel DB / audit eşleşmesi (salt okunur)

Kayıt: `c9f76cb1-1957-4d9e-b843-1d8f8ab8ff7d`.

| 10 Eylül 2026 aksiyonu | Bildirim ID | DB kaydı |
| --- | --- | --- |
| 14:36:41 GONDER | ef18c5fc-f127-4f23-b730-cef60f72306c | 1 |
| 14:37:53 CALISANA_GERI_GONDER | 30cc722e-b2c0-45e5-b4d4-4811794fa1a9 | 1 |
| 14:38:28 TEKRAR_GONDER | 04ce8293-0e43-4f31-9473-248cdfc7f5f8 | 1 |

İlk ve son bildirim aynı alıcıya ve aynı metinle gidiyor, fakat ID'leri ve
workflow aksiyonları farklı. Mesaj metnine göre dedup yapılmadı.

## Fix ve regresyon doğrulaması

- Sekme içinde referans sayımıyla ortak Client; son sahip ayrılınca kapatma.
- Subscription cleanup ve gerçek native close tamamlanmadan yeni bağlantıyı açmama.
- Notification ID geçmişini QueryClient ömrüne bağlama; reconnect/remount sırasında
  tekrar yan etkiyi engelleme, token temizlenince mevcut sahiplerin geçmişini silme.
  App hesap/rol değişiminde QueryClient'ı değiştirir. ID geçmişi yalnız bellektedir;
  tam sayfa yenilemesinden sonra korunmaz ve uzun oturumda farklı ID sayısıyla büyür.
- Bozuk/ID'siz payload mevcut cache yenileme davranışını korur.
- Polling toast üretimine bağlanmadı.

Frontend ilgili 5 dosyada **35 test başarılı**. Son transport assertion eklerinden
sonra 4 transport testi ayrıca başarılı. `npm run build`, `npm run lint` ve
`git diff --check` başarılı.

Frontend komutu (yerel Node Web Storage çakışması için):

```sh
NODE_OPTIONS=--no-experimental-webstorage npm test -- src/hooks/useRealtimeNotifications.transport.test.tsx src/hooks/useRealtimeNotifications.test.tsx src/hooks/useNotificationCenter.test.tsx src/pages/NotificationsPage.test.tsx src/App.test.tsx
```

Backend: NotificationServiceTest (9), RealtimeNotificationPublisherTest (2),
RealtimeNotificationEventListenerTest (2), WorkflowStatusChangedListenerTest (19),
StompJwtChannelInterceptorTest (8): **40 test başarılı**. İlk çalışmada sandbox
içinde Mockito self-attach başarısız oldu; kurulu Mockito jar'ı açık `-javaagent`
olarak verilince testler geçti. Backend üretim kodu değiştirilmedi. Kullanıcının
önceden değiştirdiği StompJwtChannelInterceptorTest dosyasına dokunulmadı.

## Manuel tekrar test sonucu ve açık kabul

Safari'de `http://localhost:5173/bildirimler` açık Başkan Yardımcısı oturumunda
incelendi. Başlangıç ve iki 30 saniyelik polling aralığından uzun süre sonraki
gözlemde aynı iki satır (14:36, 14:38) vardı; yeni satır/popup görülmedi. Bu iki
satır yukarıdaki ayrı GONDER ve TEKRAR_GONDER bildirimleriyle uyumlu.
Gözlemler arası kısa ömürlü popup veya gerçek polling HTTP sayısı ölçülmedi.

Bu 14 Eylül incelemesi sırasında **tek yeni aksiyon → save/publish/send → STOMP
MESSAGE → UI** zinciri henüz manuel olarak sayılmamıştı. Temel iki-kullanıcı turu
sonraki kabul kaydında tamamlandı. Bu inceleme tarihinde reconnect ve polling
fallback turları tamamlanmamıştı. 16 Eylül'de reconnect PASS oldu; izole polling
fallback kabulü hâlâ beklemektedir.

Geçici production debug log eklenmedi, commit oluşturulmadı, DB değiştirilmedi.
