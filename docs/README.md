# Dokümantasyon ve teslim durumu

Bu dizin çalışan kodu, kabul edilmiş tasarım kararlarını ve tarihli test kanıtlarını
ayrı takip eder. **Kod tabanı: 4 Eylül 2026, `codex/ap-2-frontend-uyum` @ `c9b0297`;
PR #69 ve #70 yerel geçmişte birleşmiştir.**
Bir ADR'nin kabul edilmesi ilgili runtime'ın uygulandığı anlamına gelmez.
Aynı biçimde “kod mevcut”, “dala birleşti”, “CI geçti”, “TEST'e dağıtıldı” ve
“ürün kabulü geçti” ayrı durumlardır; bu belgede karıştırılmaz.

> **Karar paketi — 4 Eylül 2026.** `B02` ve istemci/bildirim sözleşmeleri karara
> bağlanmıştır: [ADR-0008](decisions/0008-hedef-rol-semantigi-ve-onceki-aktore-donus.md)
> ve [APP-9/APP-10/B11 sözleşmesi](APP9_APP10_B11_ISTEMCI_SOZLESMESI.md). İkisi de
> **Önerildi** durumundadır — karar metni hazırdır, kod ve `V24` migration'ı henüz
> uygulanmamıştır. Tamer (`B10`/`WEB-1`), Bahadır (`B09`/`MOB-1`/`NT-5`) ve
> Alperen (`B12`) bu sözleşmelere göre çalışmaya başlayabilir.

> **Açık davranış problemleri:** 4 Eylül 2026 tarihli inceleme, çalıştırılmış
> regresyon problarıyla sekiz backend davranış ihlali (B01–B08) ve dört istemci/sözleşme
> boşluğu (B09–B12) doğrulamıştır; beşi P1'dir. Aşağıdaki “hazır teslim”
> sütunu bu problemleri kapsamaz.

## Hangi belge okunmalı?

| İhtiyaç | Kaynak |
| --- | --- |
| Kurulum ve kalite komutları | [Kök README](../README.md) |
| Modül sınırları ve transaction mimarisi | [architecture.md](architecture.md) |
| Çalışan aksiyon, hedef, yetki ve görünürlük davranışı | [workflow.md](workflow.md) |
| Güncel şema ve migration işletimi | [database.md](database.md) |
| Bağlayıcı veri modeli ve atama kısıtları | [DB-1](DB_1_VERI_MODELI_SOZLESMESI.md) |
| Ortak kayıt görünürlüğü ve departman sorgu koşulları | [WF-2C2 / DB-8](WF2C2_DB8_GORUNURLUK_SOZLESMESI.md) |
| Mevcut geçişe rol bağlama ve Admin entegrasyonu | [WF-8 / AP-8](WF8_AP8_AKTOR_ROL_BAGLAMA_SOZLESMESI.md) |
| Kullanılabilir aksiyon, hedef departman keşfi, atama DTO'su ve bildirim alıcısı | [APP-9 / APP-10 / B11](APP9_APP10_B11_ISTEMCI_SOZLESMESI.md) |
| Web ve mobil HTTP sözleşmeleri | [Web](FRONTEND_BACKEND_SOZLESMESI.md), [mobil](MOBIL_API_ENVANTERI.md), [OpenAPI](openapi.json) |
| TEST dağıtımı ve ortam sınırları | [TEST ortamı notu](TEST_ORTAMI_NOTU.md) |
| Tasarım gerekçeleri ve karar durumları | [ADR dizini](decisions/README.md) |

## Hazır olanlar ve kalan kapsam

| Alan | Hazır teslim | Kalan iş |
| --- | --- | --- |
| Rol/yetki ve workflow kimliği | RoleId, permission authority, kapasite kontrolü, DB kural kaynağı ve canlı reload; `AP-2` rol CRUD backend uçları, kullanımdaki rolün korunması ve `RolesPage` yönetim ekranı | Permission matrisi (`AP-3`): yönetim servisi, HTTP ucu ve ekran yoktur; permission kaldırmanın açık işlere etkisi kararlaştırılmalıdır |
| Ortak görünürlük (`WF-2C2/DB-8`) | Creator/direct/system ve departman/durum scope; dinamik rol liste/detay/geçmiş/dosya, JWT ve policy–SQL parity | TEST/ürün kabulü |
| Aktör rolü bağlama (`WF-8`) | `WorkflowActorBindingService.listTransitions/bind/unbind`, permission/koruma, audit ve commit sonrası snapshot | `AP-8` HTTP adapter ve yönetim ekranı. `workflow` altındaki tek yönetim ucu `POST /api/workflow/rules/reload`'dur; bu tek başına AP-8 değildir |
| Departman veri katmanı (`DB-11/12`) | V18–V22 tablo/entity/repository; ad 150, self-parent CHECK, RESTRICT FK, çoklu üyelik ve routing tekilliği | Yönetim servis/API/UI (`AP-4/5`): departman ve routing için controller yoktur; parent döngüsü ve açık kuyruk koruması karara bağlanmalıdır |
| Atama (`DB-13/WF-5`) | V23, snapshot/update/event departman alanları, karşılıklı dışlama ve gönderim | Yönetim ve gönderim ekranı kabulü |
| Departman runtime (`WF-6`) | Routing/eligibility resolver, gönderim, ortak görünürlük, event ve yarış testleri | AP-4/AP-5 ekranları; NT-5 fan-out (listener departman için bilinçli olarak boş alıcı döner); dinamik aktörden Başkana iletilen kaydın önceki aktöre dönüşü (B02) |
| İstemci workflow kabulü | Web dinamik rolü okuyabilir ve departman isteği taşıyabilir | Web aksiyon paneli `systemKey` sabitlerine bağlıdır; dinamik rol düğme göremez (B10). Mobil `roleName` Zod enum'u dinamik rolü reddeder (B09). Atama alanları yanıt DTO'larında yoktur (B11) |
| Bildirim ve istemci kabulü | Mevcut REST/polling, uygulama içi bildirim, mail-action altyapısı, FCM desteği ve token temizliği | WebSocket, departman fan-out (`NT-5`), mail E2E (B01 düzeltmesine bağlı) ve gerçek cihaz push kabulü |

## WF-5/WF-6 entegrasyon sınırı

Burak runtime ve ortak policy'yi, Alperen persistence/query ve ileri migration'ı
birlikte tamamlar. Tamer yönetim HTTP/UI'sini, Bahadır workflow olayının bildirim
kanallarını geliştirir. Temel departman şeması ve ADR kararı beklenmez.

1. **Gönderim aynı teslimde açılır.** [ADR-0006](decisions/0006-departman-hedefli-target-strategy.md)
   uyarınca `DEPARTMENT`, `DEPARTMANA_GONDER`, `targetDepartmentId`, geçiş
   constraint/seed'leri ve resolver desteği V23 ile birlikte uygulanmıştır. V23 tek başına eski backend üzerine dağıtılmaz.
2. **Atama kişi veya departmandır.** Her ikisi birden dolamaz; ikisi de boş
   olabilir. Geçişin gerektirdiği atama, uygulama transaction'ında doğrulanır.
   Snapshot, update ve event departman bilgisini taşır. Mevcut iki aşamalı
   doğrulama, işlem başına tek snapshot, audit ve mail transaction bütünlüğü korunur.
3. **Eligibility ortak kurala dayanır.** Güncel üyelik, aktif kullanıcı/departman/rol,
   uygun aktif transition/routing, permission ve aktör-kayıt ilişkisi birlikte
   aranır. Mevcut `findActiveUsersByDepartmentId` yalnız kullanıcı aktifliğini
   filtreler; tam eligibility çözümü değildir. Liste/detail/file/history ve
   notification alıcı çözümü bağımsız yetki kuralları üretmemelidir.
4. **V1 kapasite ve hiyerarşi sınırı korunur.** [ADR-0007](decisions/0007-rol-kapasitesi-ve-birim-tekilligi.md)
   gereği yerleşik rollerin kapasitesi gevşetilmez; departman routing hedefleri
   sınırsız kapasiteli uygun workflow rolleridir. Çoklu üyelik vardır; parent
   departmandan yetki devralma, otomatik eskalasyon ve claim mekanizması yoktur.
5. **Kabul departman davranışını kanıtlar.** Gönderim/geri dönüş, yetkili-yetkisiz
   üye, üyelik/permission/rol/routing kaybı, policy–SQL ID eşitliği ve sayfalama,
   eşzamanlı first-action-wins ile rollback testleri eklendi. Mevcut snapshot,
   geçmiş kesimi, dosya ve mail testleri korunur. WF-8 ve AP-2 kullanım koruması departman kuyruklarını da kapsar; uygunluk/routing pasifleştirilerek aşılamaz.

Grafik tasarımcısı, workflow versioning ve draft/publish Workflow V2 kapsamındadır.
WF-8 servisinin hazır olması AP-8 ekranlarını, şemanın hazır olması da WF-2C2
departman kabulünü kapatmaz.

## Doğrulama kanıtı

**Güncel tur — 4 Eylül 2026, `codex/ap-2-frontend-uyum` @ `c9b0297`:** Backend
`mvn -o ... verify` **776 test / 0 failure / 0 error / 0 skipped**, JAR üretildi.
Frontend `npm test` **126 test / 22 dosya**, lint, build ve E2E typecheck başarılı.
Mobil lint/typecheck başarılı, `npm test -- --runInBand` **64 / 64**. Gerçek
backend ile Playwright **15 / 15** (ayrı `workflow-review-e2e` Compose projesi,
Chromium). Backend testleri yalnız `wf-scratch` içinde oluşturulan
`workflow_review_20260904` veritabanında çalıştı; geliştirme DB'si (`5433`) ve
`8080`'deki backend hedef yapılmadı.

Bu koşumların kaynağı 4 Eylül 2026 inceleme turudur; bu dokümantasyon turunda
yalnız frontend `npm test` yeniden çalıştırılarak **126/126** doğrulanmıştır.
Backend, mobil ve Playwright sayıları raporun aynı commit üzerindeki tarihli
kanıtından alınmıştır, bu turda tekrar edilmemiştir.

Bu sayılar **proje testlerinin** sonucudur. İnceleme turunda ayrıca çalıştırılan
sekiz regresyon probu bu suite'in dışındadır ve hepsi başarısız olmuştur; yeşil
suite B01–B12'yi kapatmaz. Koşum ayrıntısı, prob başına gözlenen sonuç ve tekrar
üretim adımları [kanıt klasöründedir](reviews/2026-09-04/TEKRAR_URETIM.md). Bu yerel
doğrulamadır; CI, TEST deploy ve AP-3/AP-4/AP-5/AP-8/NT-5 ürün kabulü değildir.

**Önceki tur — WF-5/WF-6 birleşik teslim — 4 Eylül 2026, 15:21 TRT:** Alperen V23 + AP-2
hizalaması + departman runtime/görünürlük üzerinde `mvn -o test`: **772 test,
0 failure, 0 error, 0 skipped**. Frontend `npm run lint`, `npm test`
(**117 test / 0 failure**) ve `npm run build` başarılıdır. Dinamik/yeniden
adlandırılmış rolle login ve departman isteğinin istemciden taşınması ayrıca testlidir.
Docker Compose ve konteynerler önceden incelendi; testler yalnız
`wf-scratch` / `127.0.0.1:5434` / `workflowdb` üzerinde çalıştı. Geliştirme
veritabanı `5433` ve çalışan backend `8080` korundu. İstemci, aynı üretim
seçenekleriyle geçici `8099/v3/api-docs` şemasından yenilendi; geçici backend
kapatıldı, ortama bağlı baseURL farkı alınmadı. `docs/openapi.json` yalnız ilgili
alanlarda güncellendi ve dört değişen DTO'nun alan kümeleri canlı şemayla karşılaştırıldı.
Bu yerel doğrulamadır; CI/TEST deploy ve AP-4/AP-5/NT-5 ürün kabulü değildir.

Son kayıtlı tam backend `verify`: **4 Eylül 2026, 12:37 TRT — 712 test,
0 failure, 0 error, 0 skipped; JAR üretildi.** 703 mevcut teste 9 departman
şema senaryosu eklendi. Test edilen teslim ve `test` @ `3eb3691` aynı Git dosya
ağacını taşır. PostgreSQL 15.18 ve `127.0.0.1:5433 → 5432` önceden doğrulandı;
izole test şemaları temizlendi, mevcut `public` şema V17'de korundu. Ayrıntı
[V22 kabul kaydındadır](database.md#v22-yükseltme-ve-geri-alma-davranışı).

Bu kayıt önceki V22 teslimine aittir; güncel WF-5/WF-6 doğrulaması aşağıya ayrıca kaydedilir. 712 sonucu
güncel CI, TEST deploy, frontend/mobile veya departman ürün kabulünün kanıtı değildir.
WF-2C2'nin 667 ve WF-8'in 703 testlik kayıtları kendi teslim tarihlerine aittir.

## Tarihsel belgeler

[WF-2A envanteri](WF2A_ROLE_NAME_ENVANTERI.md) ve
[WF-2D2 rollout](WF2D2_ROLE_ID_ROLLOUT.md) aşamalı dönüşümün gerekçe ve kanıtlarını
saklar; eski gate/sınıf/rol red davranışları güncel talimat değildir.
`archive/` altındaki kapatılmış görev ve ortam kabul kayıtları tarihsel kalır.
ADR-0003'ün rol kapsamı/tekillik önerisinin yerine ADR-0005/0007 geçmiştir;
ADR-0005/0006/0007 kabul edilmiş kararlardır, runtime teslim durumları yukarıdadır.
