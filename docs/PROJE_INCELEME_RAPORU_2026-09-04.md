# Proje inceleme raporu — 4 Eylül 2026

**Sonuç:** Mevcut temel akış ve test altyapısı çalışıyor; fakat 10 Eylül Workflow V1 teslim tanımı henüz karşılanmıyor. Dinamik rol/departman kullanımını engelleyen istemci ve yönetim eksikleri yanında, ek regresyon senaryolarıyla doğrulanmış sekiz backend davranış problemi var. Standart testlerin yeşil olması bu boşlukları kapatmıyor.

## 1. İncelenen kapsam ve kanıt sınırı

- Çalışma dalı: `codex/ap-2-frontend-uyum`.
- Commit: `c9b029700e644dad2075896195b10b4c4059230c` — 4 Eylül 2026 15:47 TRT.
- Yerel geçmişte PR #69 (`543ec27`) ve PR #70 (`020bb9b`) birleşimleri mevcut. Bu rapor uzak sunucunun güncel durumunu veya TEST deploy'unu doğruladığını iddia etmez.
- Backend: 205 üretim Java dosyası; 80 test/destek Java dosyası. Web: 22 birim/bileşen test dosyası; mobil: 13 test dosyası. Envanter, ilgili uygulama yolları ve testler üzerinden incelendi; her dosyanın tüm satırlarının denetlendiği iddiası yoktur.
- Kök README, aktif mimari/veri/workflow/API/ortam belgeleri, ADR-0005/0006/0007, CI, Compose, migration zinciri ve iki dış plan belgesi karşılaştırıldı.
- Plan belgeleri hedef ve kabul ölçütü olarak okundu; içlerindeki görev atama, uygulama ve dağıtım ifadeleri bu inceleme için işlem talimatı sayılmadı.
- İnceleme öncesindeki `.claude/` içeriğine dokunulmadı. Uygulama koduna düzeltme yapılmadı. Ek testlerin kaynakları normal test ağacından çıkarılıp raporun kanıt klasörüne kondu.

**Öncelik:** P1 teslim öncesinde çözülmesi gereken güvenlik, iş akışı veya veri bütünlüğü sorunu; P2 belirli koşullarda yanlış davranış veya önemli sözleşme eksikliği. “Kod kanıtı” ile “çalıştırılarak doğrulanan bulgu” ayrı belirtilmiştir.

## 2. Bu incelemede çalıştırılan kontroller

| Kontrol | Sonuç | Açıklama |
|---|---|---|
| Backend `mvn -o ... verify` | **776 / 776 geçti**, JAR üretildi | Ek inceleme testleri eklenmeden önce, mevcut proje testleri |
| Frontend `npm test` | **126 / 126 geçti** | 22 test dosyası |
| Frontend lint / build / E2E typecheck | **Başarılı** | Üç komut ayrı çalıştırıldı |
| Mobil lint / typecheck | **Başarılı** | Mevcut kurulu bağımlılıklar |
| Mobil `npm test -- --runInBand` | **64 / 64 geçti** | Son tam tur; ilk turdaki tek 5 saniye zaman aşımı hedefli ve tam tekrarda geçmiştir |
| Gerçek backend ile Playwright | **15 / 15 geçti** | Ayrı PostgreSQL/Mailpit/Backend Compose projesi; Chromium |
| Ek backend regresyon probları | **8 beklenen davranış ihlali doğrulandı** | 5 + 2 + 1 testlik üç tur; assertion hatası, altyapı hatası değil |
| Sürümlenmiş OpenAPI / çalışan backend | **32 yol ve 37 DTO şemasının alan kümelerinde fark yok** | Tüm açıklama, required/enum ve yanıt semantiğinin birebir eşit olduğu iddiası değildir |

Veritabanı testlerinden önce üç Compose dosyası ve çalışan konteynerler incelendi. `workflow-db` 5433, `wf-scratch` 5434 üzerinde bulundu. Backend testleri için yalnız `wf-scratch` içinde oluşturulan `workflow_review_20260904` veritabanı kullanıldı. E2E ayrıca `workflow-review-e2e` adlı izole Compose projesinde çalıştı; geliştirme DB'si ve 8080'deki mevcut backend test hedefi yapılmadı.

İlk Playwright turu eksik Chromium ikili dosyası nedeniyle tarayıcı testlerini başlatamadı. Gerekli sürüm geçici proje klasörüne kuruldu; ardından 15 testin tamamı geçti. İlk turdaki 14 başlatma hatası uygulama hatası olarak sayılmadı.

Gerçek Android push, uzak CI son koşumu, TEST sunucusu, yük testi, bağımlılık CVE taraması ve yedekten geri yükleme bu raporda doğrulanmış değildir.

## 3. Doğrulanan backend problemleri

### B01 — P1 — Workflow e-postasının hızlı işlem tokenı commit sonrasında üretilemiyor

**Kanıt:** [WorkflowStatusChangedListener.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/notification/listener/WorkflowStatusChangedListener.java:91), [MailActionTokenService.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/notification/service/MailActionTokenService.java:124).

E-posta dinleyicisi `AFTER_COMMIT` aşamasında çalışıyor. Buradan çağrılan `issue()`, varsayılan `@Transactional` yayılımıyla yeni bir transaction açmıyor. İlk `consumeOpenTokens` yazımı “No active transaction for update or delete query” hatası veriyor. Dinleyici hatayı yakalayıp e-postayı düğmesiz gönderiyor.

**Tekrar üretim:** Gerçek transaction içinde workflow olayı yayınlandı; commit sonrası `mail_action_tokens` sayısı **beklenen 1, gerçekleşen 0** oldu. Mail gönderimi mock olduğu için dışarıya ileti gönderilmedi.

**Etki:** NT-7 mail üzerinden işlem kabulü, mevcut başarılı workflow ve mail birim testlerine rağmen sağlanamaz.

**Düzeltme:** Token üretimine bağımsız ve gerçekten commit edilen transaction sınırı verilmeli veya token asıl transaction içinde üretilip yalnız mail gönderimi commit sonrasına taşınmalı. Gerçek commit → mail içeriği → preview → consume → tekrar tüketimi reddetme entegrasyon testi eklenmeli. Sahip: **Bahadır**, transaction değerlendirmesi Burak.

### B02 — P1 — Dinamik departman rolünden Başkana iletilen kaydın önceki aktöre dönüşü kırılıyor

**Kanıt:** [WorkflowApplicationService.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/workflow/service/WorkflowApplicationService.java:151), [TargetUserResolver.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/workflow/service/TargetUserResolver.java:62), [WorkflowTransitionValidator.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/WorkflowTransitionValidator.java:93), [V15 geçiş seed'i](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/resources/db/migration/V15__workflow_transitions.sql:86).

Dinamik departman üyesi `BASKANA_ILET` yaptığında `last_deputy_id` bu üyenin UUID'si oluyor. Başkanın `BASKAN_YARDIMCISINA_GERI_GONDER` geçişi aynı kişiyi `PREVIOUS_ACTOR` ile çözüyor; ancak seed hâlâ hedef rolün yerleşik `BASKAN_YARDIMCISI` olmasını istiyor.

**Tekrar üretim:** Dinamik rol + permission + transition bağı + departman üyeliği/routing kuruldu. Başkana iletme başarılı oldu. Başkanın önceki aktöre dönüşü **`WORKFLOW_TARGET_ROLE_INVALID`** ile reddedildi.

**Etki:** Plandaki “Satın Alma uzmanı → Başkan” örneğinin geri dönüş kolu tamamlanmıyor. Mevcut departman testlerinin oluşturucuya geri dönüşü doğrulaması bu kolu kapsamıyor.

**Düzeltme:** Dinamik önceki aktöre geri dönüşün kişi/departman ve hedef rol anlamı ADR/API düzeyinde netleştirilmeli; mevcut yerleşik akışı koruyan ileri migration ve resolver doğrulaması birlikte yapılmalı. Hedef rol kontrolünü koşulsuz kaldırmak yeterli ve güvenli bir çözüm değildir. Sahip: **Burak + Alperen**.

### B03 — P1 — Görev devri optimistic locking korumasını atlıyor

**Kanıt:** [RecordRepository.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/record/repository/RecordRepository.java:37), [UserService.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/user/service/UserService.java:196).

`devretBekleyenIsleri` ve `updateLastDeputyId` toplu JPQL güncellemeleri `records.version` değerini artırmıyor. Daha önce kaydı yükleyen bir workflow transaction'ı, devir commit edildikten sonra eski `lastDeputyId` ile yazabiliyor.

**Tekrar üretim:** Bir transaction eski snapshot'ı aldı; ikinci transaction gerçek iki repository devir metodunu çalıştırıp commit etti. İlk transaction'ın eski snapshot ile yazımı **çatışma almadan başarılı oldu**.

**Etki:** Devir bilgisi eski kullanıcıya dönebilir; sonraki geri dönüş yanlış/eski kişiyi hedefler. Sıralı görev devri E2E testinin geçmesi yarış güvenliğini kanıtlamaz.

**Düzeltme:** Devir yazımları sürümü artırmalı; aynı transaction içindeki iki bulk yazımın sürüm ve persistence-context davranışı tasarlanmalı. Etkilenen kayıtlar ve workflow ile yarış senaryosu test edilmeli. Sahip: **Alperen + Burak**.

### B04 — P1 — Dosya yükleme, kayıt gönderildikten sonra commit edebiliyor

**Kanıt:** [RecordLockValidator.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/attachment/service/RecordLockValidator.java:21), [FileService.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/attachment/service/FileService.java:67).

`RecordLockValidator` yalnız normal bir okuma yapıyor; ismine rağmen kayıt kilidi almıyor. Dosya yüklemesi kaydın sürümünü de değiştirmiyor. Kontrol ile dosya kaydının yazılması arasında workflow durumu değişirse eski izinle devam ediliyor.

**Tekrar üretim:** Dosya isteği TASLAK kontrolünü geçtiği anda durduruldu. Başka transaction kaydı incelemeye taşıdı ve sürümü artırdı. Dosya isteği devam ettirildi: **beklenen 0, gerçekleşen 1 dosya satırı**.

**Etki:** İncelemeye gönderildikten sonra değişmemesi gereken ekler değiştirilebilir. Aynı kontrolü kullanan silme yolu da birlikte ele alınmalıdır.

**Düzeltme:** Dosya işlemi ve workflow geçişi ortak kayıt kilidi/sürüm protokolüne bağlanmalı. Depolama yazımlarının transaction başarısızlığında temizlenmesi ayrıca çözülmeli. Sahip: **Burak + attachment/veri katmanı sahibi**.

### B05 — P1 — Aynı refresh token eşzamanlı iki kez yenilenebiliyor

**Kanıt:** [AuthService.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/auth/service/AuthService.java:76), [TokenRepository.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/user/repository/TokenRepository.java:10), [Token.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/user/entity/Token.java:9).

Token normal sorguyla okunuyor; satır kilidi, sürüm alanı veya `revoked=false` şartlı atomik tüketim yok. İki istek aynı anda henüz iptal edilmemiş tokenı okuyup ayrı yeni tokenlar üretebiliyor.

**Tekrar üretim:** İki gerçek transaction, eski tokenı okuduktan sonra bariyerle eşitlendi. **Beklenen 1 başarılı yenileme, gerçekleşen 2**.

**Etki:** Token rotasyonunun tek tüketim garantisi bozuluyor; eşzamanlı tekrar kullanım iki bağımsız oturum zinciri oluşturabiliyor. Bu bulgu tokenı bilmeyen kişinin giriş yapabildiği anlamına gelmez.

**Düzeltme:** Pessimistic lock veya koşullu UPDATE ile tek tüketim sağlanmalı; refresh/logout/parola değiştirme yarışları aynı tasarım altında ele alınmalı. Sahip: **Burak + Alperen**.

### B06 — P2 — Dondurulmuş içeriğin yeni düzenlemeleri arama sonucundan anlaşılabiliyor

**Kanıt:** [RecordSpecifications.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/search/specification/RecordSpecifications.java:39), [RecordContentView.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/record/view/RecordContentView.java:60).

Başkan Yardımcısına yanıt hazırlanırken handoff snapshot'ı gösteriliyor; fakat `q` ve kategori filtreleri güncel kayıt kolonlarında çalışıyor. Gösterilmeyen düzenleme, sonuçta kaydın var/yok olmasını etkiliyor.

**Tekrar üretim:** Eski snapshot'ta bulunmayan benzersiz bir sözcük yalnız canlı açıklamaya eklendi. Snapshot ile sınırlı aktörün bu sözcükle sorgusu **kaydı döndürdü**.

**Etki:** Kullanıcı, görmemesi gereken canlı içerikte belirli bir sözcüğün varlığını ve kategori değişimini sorgulayabilir. ID görünürlük parity testi, içerik sürümü parity'sini kapsamıyor.

**Düzeltme:** Arama/filtre/sıralama görünür içerik sürümünü esas almalı; snapshot başlığı, açıklaması ve kategorisi SQL tarafına tutarlı aktarılmalı. Sahip: **Alperen + Burak**.

### B07 — P2 — Geçmiş görünümünde listelenen silinmiş ek indirilemiyor

**Kanıt:** [FileService.java: liste](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/attachment/service/FileService.java:143), [FileService.java: indirme](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/attachment/service/FileService.java:157).

Handoff anında mevcut olup sonradan silinen dosya, dondurulmuş görünümde doğru biçimde listeleniyor. Download/preview ise daha görünürlük kontrolüne gelmeden `findByIdAndDeletedAtIsNull` kullandığı için dosyayı bulamıyor.

**Tekrar üretim:** Aynı aktör ve dosya için liste başarılı; indirme **`ResourceNotFoundException`**.

**Düzeltme:** Dosyanın bulunması ile güncel/tarihsel erişim yetkisi ayrı değerlendirilerek liste ve indirme aynı zaman kesitine bağlanmalı. Güncel görünümde silinmiş dosyanın açılması engellenmeye devam etmeli. Sahip: **attachment sahibi + Burak**.

### B08 — P2 — Soft-delete edilmiş taslak güncellenebiliyor

**Kanıt:** [RecordServiceImpl.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/record/service/RecordServiceImpl.java:75), [updateRecord](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/record/service/RecordServiceImpl.java:148).

Detay okuma silinmiş kayıtları dışlıyor; update/delete ortak yükleyicisi dışlamıyor. Taslak sahibinin permission ve durum kontrolü geçince silinmiş içerik güncelleniyor ve audit yazılıyor.

**Tekrar üretim:** `deleted_at` dolu bir TASLAK için update çağrısı **hatasız tamamlandı**.

**Düzeltme:** Bütün değişiklik yollarında aktif kayıt yükleme davranışı ortaklaştırılmalı; tekrar DELETE'in idempotent mi 404 mü olduğu belgelenmeli. Sahip: **record sahibi + Burak**.

## 4. İstemci ve sözleşme problemleri

### B09 — P1 — Mobil rol şeması AP-2 ile uyumsuz

**Kod kanıtı:** [mobile users.ts](C:/Users/burak/Desktop/projects/WorkFlowProject/mobile/src/api/users.ts:5), [mobil aksiyon seçimi](C:/Users/burak/Desktop/projects/WorkFlowProject/mobile/src/components/records/RecordWorkflowActions.tsx:28), [mobil dashboard](<C:/Users/burak/Desktop/projects/WorkFlowProject/mobile/src/app/(app)/index.tsx:155>).

`roleName` yalnız dört sabit değeri kabul eden Zod enum'uyla ayrıştırılıyor; `roleId` ve `systemKey` kullanılmıyor. Admin'in oluşturduğu dinamik rol veya yeniden adlandırdığı yerleşik rol için geçerli `/api/users/me` cevabı reddediliyor. Login endpoint'i token verebilir; kırılma kullanıcı profilinin okunması ve buna bağlı ekranlarda oluşur.

Web AP-2 düzeltmesinin mobil karşılığı eksik. Profil şeması, etiketler, dashboard, oluşturma yetkisi ve workflow seçimleri birlikte dönüştürülmeli. Kabul: yeni dinamik rol ve yeniden adlandırılmış yerleşik rolle giriş sonrası liste, detay ve yetkili işlem. Sahip: **Bahadır**.

### B10 — P1 — Web dinamik rolü okuyabiliyor, workflow aksiyonunu sunamıyor

**Kod kanıtı:** [RecordActionPanel.tsx](C:/Users/burak/Desktop/projects/WorkFlowProject/frontend/src/components/records/RecordActionPanel.tsx:84), [AuthUser](C:/Users/burak/Desktop/projects/WorkFlowProject/frontend/src/types/auth.ts:22).

Bütün işlem düğmeleri `systemKey === CALISAN/BASKAN_YARDIMCISI/BASKAN` koşullarına bağlı. Dinamik rolün `systemKey=null` olması normal; bu kullanıcı için panel tamamen kapanıyor. Permission ve workflow bağı doğru olsa bile departman uzmanı kaydı işleyemiyor.

İstemciye backend'in hesapladığı kullanılabilir aksiyon/ilişki bilgisi sağlanmalı. Kullanıcı rolünü başka bir sistem rolü gibi göstermeye veya workflow kurallarını ikinci kez istemcide kurmaya dayalı geçici çözümden kaçınılmalı. Aynı model mobilde de kullanılmalı. Sahip: **Burak + Tamer/Bahadır**.

### B11 — P2 — Departman ataması yanıt DTO'larında kayboluyor

**Kod kanıtı:** [RecordResponse.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/record/dto/RecordResponse.java:9), [RecordSearchResponse.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/search/dto/RecordSearchResponse.java:30), [WorkflowActionResponse.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/workflow/dto/WorkflowActionResponse.java:10), [web detay adapteri](C:/Users/burak/Desktop/projects/WorkFlowProject/frontend/src/api/recordDetails.ts:111).

İstek `targetDepartmentId` kabul ediyor ve kayıt DB'de departmana atanıyor; liste/detay/aksiyon yanıtlarında `assignedDepartmentId` yok. Detay DTO'su doğrudan kişi atamasını da taşımıyor; web adapteri atama alanlarını sabit null dolduruyor.

İstemci, boş atama ile departman kuyruğunu ayıramıyor ve “hangi departmanda?” bilgisini güvenilir biçimde gösteremiyor. Ortak atama DTO'su, gerekli gösterim adı ve mümkünse kayıt sürümü belirlenip OpenAPI/web/mobil birlikte güncellenmeli. Sahip: **Burak + Alperen + istemci sahipleri**.

### B12 — P2 — Departman hedefi kalıcı workflow audit'ine yazılmıyor

**Kod kanıtı:** [WorkflowTransitionAudit.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/workflow/model/WorkflowTransitionAudit.java:12), [AuditLogService.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/audit/service/AuditLogService.java:38).

`WorkflowTransitionAudit` departman alanı taşımıyor. `AuditLogService`, modeldeki kişi atama alanını da kaydetmiyor. Departman event'i yayınlansa bile audit satırında yalnız aksiyon/durum/aktör/comment kalıyor. Departman gönderiminde comment isteğe bağlı.

Kayıt daha sonra başka yere geçtiğinde önceki gönderimin hangi departmana yapıldığı kalıcı geçmişten bulunamıyor. Önceki/yeni kişi/departman atamaları için yapılandırılmış audit sözleşmesi ve migration gerekli. Serbest metin açıklamasına güvenilmemeli. Sahip: **Burak + Alperen**.

## 5. Planla karşılaştırılan açık V1 kapsamı

| İş | Mevcut durum | Tamamlanması gereken |
|---|---|---|
| AP-2 rol CRUD | Backend ve web `RolesPage` mevcut; 126 web testi içinde ekran testleri var | Bu dalın merge/TEST kabulü; mobil uyum B09 |
| AP-3 permission matrisi | Katalog/repository mevcut | Yönetim servisi, HTTP/API, ekran; permission kaldırmanın açık işlere etkisi |
| AP-4 departman/üyelik | V18–V22 entity/repository mevcut | CRUD/üyelik API ve ekranları; parent döngüsü ve açık kuyruk koruması |
| AP-5 routing | Runtime okuyucusu mevcut | Yönetim API/UI, role/permission/transition doğrulaması ve kullanım koruması |
| AP-8 actor binding | `WorkflowActorBindingService.listTransitions/bind/unbind` mevcut | HTTP adapter ve yönetim ekranı; mevcut `/rules/reload` tek başına AP-8 değildir |
| Kullanıcı departman gönderimi | Backend `DEPARTMANA_GONDER` + request alanı ve üretilmiş web tipi mevcut | Web/mobil departman seçici, normal kullanıcıya uygun hedef keşif API'si, işlem düğmesi |
| NT-5 departman bildirimleri | Listener departman için açıkça boş alıcı kümesi dönüyor | Ortak eligibility ile uygun alıcıları bulma, dedupe ve kanal testleri |
| NT-2/3/4/10 realtime | Web bildirimleri 30 saniyelik polling kullanıyor | WebSocket/STOMP backend ve istemci, authentication, commit sonrası yayın, reconnect/fallback, TEST kabulü |
| NT-7 mail kabulü | Altyapı ve preview/consume uçları var | B01 düzeltmesi ve gerçek mail işlem zinciri E2E testi |
| NT-8/9 Android push | Token kayıt ve sıcak bildirim tıklaması mevcut | Config plugin, cold-start, token refresh listener, kalıcı paket/Firebase eşleşmesi ve fiziksel cihaz kabulü |
| OPS TEST/handoff | Compose/CI ve yerel doğrulama mevcut | Yeni teslimin ortam kanıtı; ürün web barındırma/FRONTEND_URL çözümü ve devir belgesi |

**Doğrudan kod kanıtları:** [departman için boş bildirim alıcısı](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/notification/listener/WorkflowStatusChangedListener.java:183), [30 saniye polling](C:/Users/burak/Desktop/projects/WorkFlowProject/frontend/src/hooks/useNotificationCenter.ts:18), [yalnız rule reload controller'ı](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/workflow/controller/WorkflowRuleAdminController.java:42), [push listener](C:/Users/burak/Desktop/projects/WorkFlowProject/mobile/src/services/notifications/pushNotificationManager.ts:118), [mobil plugin listesi](C:/Users/burak/Desktop/projects/WorkFlowProject/mobile/app.json:36).

**TEST topolojisi açığı:** [Caddyfile](C:/Users/burak/Desktop/projects/WorkFlowProject/deploy/Caddyfile:28) bütün ürün yollarını backend'e gönderiyor; [TEST notu](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/TEST_ORTAMI_NOTU.md:11) web arayüzünün barındırılmadığını açıkça söylüyor. Bu mevcut API-only topolojide `/hizli-islem` sayfası çalışmaz. V1 web/mail kabulü için erişilebilir frontend adresi ve deploy sorumluluğu karara bağlanmalı.

**V2 eksik sayılmadı:** graph designer, workflow definition/versioning, draft/publish, çoklu graph, parent üzerinden otomatik routing ve claim mekanizmasının bulunmaması V1 hatası değildir.

## 6. Ek riskler ve izlenecek teknik borç

Bu maddeler, yukarıdaki sekiz çalıştırılmış probla aynı kanıt seviyesinde değildir.

| Risk | Kanıt / koşul | Öneri |
|---|---|---|
| Rol adında büyük/küçük harf tekilliği yarışa açık | [RoleAdminService.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/rbac/service/RoleAdminService.java:197) kendisi de kabul ediyor; DB unique harf duyarlı | Türkçe normalizasyonla tutarlı DB tekillik modeli ve eşzamanlı create/update testi |
| Routing sınırlı yerleşik rolü runtime'da reddetmiyor | [DepartmentRoutingAdapter.java](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/workflow/adapter/DepartmentRoutingAdapter.java:57) aktiflik/aktör/Admin kontrol ediyor; `maxUsers` ve dinamik rol şartını aramıyor | ADR-0007 ile uyumlu AP-5 yazma doğrulaması ve runtime savunması; doğrudan yanlış SQL verisi için negatif test |
| Refresh TTL'nin iki ayrı kaynağı var | `JwtUtil` konfigürasyon kullanırken `AuthService` DB süresini iki yerde 7 gün yazıyor; refresh JWT exp kontrolü yapmıyor | DB expiresAt ile yapılandırılan ömrü tek kaynaktan üretme ve kısa TTL kabul testi |
| Parola sıfırlama sayaç/tüketim yarışı | `PasswordResetCode` üzerinde @Version, repository okumalarında satır kilidi yok | Aynı kod için paralel yanlış deneme, çift verify/reset ve kod yenileme yarışlarını test etme |
| Eski istemci sürümü bildirilemiyor | Record/Workflow request'lerinde expectedVersion, cevaplarında version yok | Gerçek eşzamanlı server transaction'ı ile eski ekrandan sonradan gelen isteği ayıran sözleşme; optimistic locking var diye ikincisinin korunduğunu iddia etmeme |
| Disk yazımı DB rollback'inde geri alınmıyor | `FileService` storage.store ardından save yapıyor; rollback temizliği yok | Çoklu yüklemede depolama/DB hatası sonrası orphan temizliği; geçici dosya/staging düzeni |
| Eligibility sorguları ölçeklenmeyebilir | `DepartmentVisibilityAdapter` departman × transition dolaşıyor; her resolve üyeleri/rolü/routing'i okuyor | Gerçekçi üyelik sayısında sorgu sayısı ölçümü ve toplu sorgu tasarımı; ölçülmüş performans sorunu olarak sunulmuyor |
| Rule snapshot bir instance'a ait | `ReloadableTransitionRuleSource` JVM içi volatile/synchronized | Mevcut tek instance sınırını koruma; çoğaltmadan önce dağıtık invalidation |
| Mail/push yeniden teslim garantisi yok | Commit sonrası gönderim, kalıcı outbox/retry kaydı bulunmuyor | Kanal hata kaydı ve yeniden deneme politikasını ürün ihtiyacına göre tanımlama |
| Operasyon kabulü eksik | TEST notunda backup/restore, reboot, izleme, saklama ve secret rotasyonu açık | DB + uploads tutarlı yedek ve gerçek restore turu; sorumlu ve kanıt tarihi |

## 7. Dokümantasyon güncelleme matrisi

Tarihsel test sayılarını silip yenisiyle değiştirmek yerine, her sayıyı kendi commit'i ve test türüyle saklayın. “Kod mevcut”, “test dalına birleşti”, “CI geçti”, “TEST deploy edildi”, “ürün kabulü geçti” ayrı durumlar olmalı.

| Belge / yer | Bulunan sapma | Gerekli güncelleme |
|---|---|---|
| [Kök README](C:/Users/burak/Desktop/projects/WorkFlowProject/README.md:27) | PR #66 / V22 / 712 test; departman runtime “uygulanmadı” deniyor | Güncel dal/commit, V23/WF-5/6 mevcut kod, AP-2 UI ve gerçek açık işler; bu raporun tarihli kanıtına bağlantı |
| [docs/README.md](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/README.md:26) | AP-2 UI açık gösteriliyor; 772/117 önceki teslim sayıları | AP-2 UI'nin bu dalda mevcut olduğu; 776/126/64/15 ayrı yerel kanıtları; ek probların bulguları |
| [architecture.md](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/architecture.md:42) | Department runtime bağlantısı ve görünürlüğü yok deniyor | DepartmentRoutingResolver/Adapter, DepartmentVisibilityAdapter ve yeni portlar; mevcut problemler ile hedef kapsam ayrımı |
| [DB-1](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/DB_1_VERI_MODELI_SOZLESMESI.md:5) | Şema V22, runtime açık; §15.3 hata kodu uygulanmamış deniyor | V23/10 geçiş ve çalışan hata kodları; §20 durum tablosu; B02/B11/B12 kararları |
| [database.md](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/database.md:356) | V23 listelenmiş; diğer kanonik belgelerle aynı taban yok | Tek şema envanteri, migration kabul tarihi, devir version davranışı ve audit genişletmesi iş kaydı |
| [workflow.md](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/workflow.md:5) | Feature dalı/merge bekleme kaydı eski; dönüş kolu eksik anlatılıyor | Güncel kod tabanı; dinamik aktör → Başkan → önceki aktör sınırı, version ve notification durumları |
| [Görünürlük sözleşmesi](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/WF2C2_DB8_GORUNURLUK_SOZLESMESI.md:66) | Departman teslimi doğru; parity ifadesi kapsamı geniş algılanabilir | ID erişim parity'si ile içerik sürümü/arama/dosya parity'sini ayırma; B06/B07 kabul senaryoları |
| [Web API sözleşmesi](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/FRONTEND_BACKEND_SOZLESMESI.md:324) | Departman request mevcut; istemci tamamlanması ve response/audit boşlukları yeterince görünür değil | Kullanılabilir aksiyon, uygun departman keşfi, assignment DTO ve UI kabul maddeleri |
| [Mobil API envanteri](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/MOBIL_API_ENVANTERI.md:115) | Logout'ta deviceToken “eklenecek” deniyor; aynı belgede aşağıda mevcut anlatılıyor | Gerçek LogoutRequest ile hizalama; dynamic-role şema kırılması ve departman işlem desteğini açık iş olarak yazma |
| [TEST ortam notu](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/TEST_ORTAMI_NOTU.md:43) | Repo V22, DEPARTMENT/runtime yok deniyor | Repo V23 bilgisini güncelleme; çalışan uzak TEST sürümünü ayrıca doğrulama; API-only/web/mail kabul çelişkisini çözme |
| [Record.java yorumları](C:/Users/burak/Desktop/projects/WorkFlowProject/backend/src/main/java/btk/staj/WorkFlowProject/record/entity/Record.java:55) | WF-5/WF-6 uygulanmadı yorumu | Entity ile runtime'ın güncel bağlantısını anlatma |
| Dış görev dağılımı belgesi | PR #68 ve feature dallarında merge bekleme; AP-2 UI eksik | PR #69/#70 yerel geçmiş kanıtı; AP-2 UI bu dalda; B01–B12 ve sahipleri; eksik iş/checklist/envanter bölümlerini birlikte hizalama |
| Dış V1/V2 planı | Hedef mimari olarak geçerli; başlangıç grafiği yalnız sekiz eski geçişi gösteriyor | V23'ün iki departman gönderim kenarını ayrıca gösterme; dinamik önceki aktöre geri dönüş ve tam demo kapsamı |
| CI / OpenAPI yönergesi | Bu turda yol/alan drift'i bulunmadı; otomatik karşılaştırma gate'i yok | Şema normalizasyonu + semantik diff, üretilmiş client doğrulaması; “drift var” ile “drift kontrolü yok” ayrımı |

Dış belgeler: [GÖREV DAĞILIMI](C:/Users/burak/Desktop/projects/plan/GOREV_DAGILIMI_VE_YOL_HARITASI_GUNCEL.md), [WORKFLOW V1/V2 PLANI](<C:/Users/burak/Desktop/projects/plan/WORKFLOW_V1_V2_PLANI (1).md>). Bu dosyalar inceleme kapsamında değiştirilmedi.

## 8. Önerilen tamamlama sırası ve kabul

1. **Burak + Alperen:** B02 dinamik geri dönüş kararını kapatın; B03 görev devri ve B04 dosya yarışını düzeltin. B06 içerik filtreleme, B07 dosya erişimi ve B08 soft-delete davranışını aynı bütünlük turunda kapatın.
2. **Bahadır + Burak:** B01 mail transaction ve B05 token rotasyonunu kapatın. Mevcut yeşil birim testlerine ek olarak gerçek commit ve eşzamanlılık testlerini kalıcılaştırın.
3. **Burak + istemci sahipleri:** B09/B10/B11 için ortak kullanıcı/atama/aksiyon sözleşmesini tamamlayın. Dinamik rol backend'de çalışırken istemcide düğme saklanması kabulü engelliyor.
4. **Tamer:** AP-3/AP-4/AP-5/AP-8 yönetim zincirini bitirin. Admin'in SQL veya manuel deploy gerektirmeden yeni birimi kullanılır hale getirebildiğini gösterin.
5. **Bahadır:** NT-5 uygun alıcı fan-out, realtime, mail işlem E2E ve Android push kabulünü tamamlayın.
6. **Burak / OPS:** Dokümanları aynı kod tabanına hizalayın; CI/TEST/web barındırma ve release kanıtını ekleyin.

**V1 kabulüne eklenecek tek uçtan uca organizasyon senaryosu:** Admin yeni dinamik rolü açar; permission verir; departman ve iki üye tanımlar; mevcut transition'a aktör bağlar; routing ekler. Çalışan departmana gönderir. İki uygun üye kaydı görür ve bildirim alır; uygunsuz üye göremez. Uygun üye Başkana iletir; Başkanın geri dönüşü tanımlanan önceki aktöre/kuyruğa ulaşır. İki üyenin yarışında tek işlem kalır. Routing/üyelik/permission kaldırmanın açık kayıt etkisi denetlenir. Aynı kayıt web, mobil ve mail üzerinden doğru açılır.

Bu senaryo, mevcut 15 E2E testinin yerine geçmez; onların kapsamadığı V1 organizasyon zincirini tamamlar.

## 9. Kanıt dosyaları

- [Tekrar üretim test kaynağı](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/reviews/2026-09-04/ReviewRegressionProbeTest.java)
- [Tekrar üretim ve koşum özeti](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/reviews/2026-09-04/TEKRAR_URETIM.md)
- Yerel ayrıntılı loglar `tmp/review-*.log` altında tutuldu; Git'e eklenmedi.
- Sürümlenmiş uygulama dosyalarında düzeltme yapılmadı; bu teslim inceleme raporu ve tekrar üretim kanıtıdır.
