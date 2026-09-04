# Dokümantasyon ve teslim durumu

Bu dizin çalışan kodu, kabul edilmiş tasarım kararlarını ve tarihli test kanıtlarını
ayrı takip eder. **Kod tabanı: 4 Eylül 2026, `test` @ `3eb3691` (PR #66).**
Bir ADR'nin kabul edilmesi ilgili runtime'ın uygulandığı anlamına gelmez.

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
| Web ve mobil HTTP sözleşmeleri | [Web](FRONTEND_BACKEND_SOZLESMESI.md), [mobil](MOBIL_API_ENVANTERI.md), [OpenAPI](openapi.json) |
| TEST dağıtımı ve ortam sınırları | [TEST ortamı notu](TEST_ORTAMI_NOTU.md) |
| Tasarım gerekçeleri ve karar durumları | [ADR dizini](decisions/README.md) |

## Hazır olanlar ve kalan kapsam

| Alan | Hazır teslim | Kalan iş |
| --- | --- | --- |
| Rol/yetki ve workflow kimliği | RoleId, permission authority, kapasite kontrolü, DB kural kaynağı ve canlı reload; `AP-2` rol CRUD backend uçları ve kullanımdaki rolün korunması | `AP-2` yönetim ekranı ve permission matrisi (`AP-3`) |
| Ortak görünürlük (`WF-2C2/DB-8`) | Creator/direct assignee ve sistem istisnaları; SQL scope, dinamik rol liste/detay/geçmiş/dosya erişimi | Yetkili departman üyesi kolu ve policy–SQL/E2E kabulü |
| Aktör rolü bağlama (`WF-8`) | `WorkflowActorBindingService.listTransitions/bind/unbind`, permission/koruma, audit ve commit sonrası snapshot | `AP-8` HTTP adapter ve yönetim ekranı |
| Departman veri katmanı (`DB-11/12`) | V18–V22 tablo/entity/repository; ad 150, self-parent CHECK, RESTRICT FK, çoklu üyelik ve routing tekilliği | Yönetim servis/API/UI (`AP-4/5`) ve runtime port/query bağlantısı |
| Atama (`DB-13/WF-5`) | V21 `records.assigned_department_id`, karşılıklı dışlama ve Record eşlemesi; `actorHoldsAssignment` rename'i | Snapshot/update/event departman alanları, hedef çözümleme ve gönderim migration'ı |
| Departman runtime (`WF-6`) | ADR-0005/0006/0007 ve temel veri katmanı | Routing/eligibility resolver, ortak görünürlük ve event entegrasyonu |
| Bildirim ve istemci kabulü | Mevcut REST/polling, uygulama içi bildirim, mail-action altyapısı, FCM desteği ve token temizliği | WebSocket, departman fan-out (`NT-5`), mail E2E ve gerçek cihaz push kabulü |

## WF-5/WF-6 öncesi entegrasyon sınırı

Burak runtime ve ortak policy'yi, Alperen persistence/query ve ileri migration'ı
birlikte tamamlar. Tamer yönetim HTTP/UI'sini, Bahadır workflow olayının bildirim
kanallarını geliştirir. Temel departman şeması ve ADR kararı beklenmez.

1. **Gönderim aynı teslimde açılır.** [ADR-0006](decisions/0006-departman-hedefli-target-strategy.md)
   uyarınca `DEPARTMENT`, `DEPARTMANA_GONDER`, `targetDepartmentId`, geçiş
   constraint/seed'leri ve resolver desteği birlikte gerekir. Bunlar mevcut
   runtime/HTTP sözleşmesinde yoktur. V18–V22 kullanılmıştır; yeni migration
   numarası uygulanacağı gün güncel dal üzerinden seçilir.
2. **Atama kişi veya departmandır.** Her ikisi birden dolamaz; ikisi de boş
   olabilir. Geçişin gerektirdiği atama, uygulama transaction'ında doğrulanır.
   Snapshot, update ve event departman bilgisini taşımalıdır. Mevcut iki aşamalı
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
   eşzamanlı first-action-wins ile rollback testleri gerekir. Mevcut snapshot,
   geçmiş kesimi, dosya ve mail testleri korunur. WF-8 kullanım korumasının mevcut
   kişi ilişkileriyle sınırlı olması da departman entegrasyonunda değerlendirilir.

Grafik tasarımcısı, workflow versioning ve draft/publish Workflow V2 kapsamındadır.
WF-8 servisinin hazır olması AP-8 ekranlarını, şemanın hazır olması da WF-2C2
departman kabulünü kapatmaz.

## Doğrulama kanıtı

Son kayıtlı tam backend `verify`: **4 Eylül 2026, 12:37 TRT — 712 test,
0 failure, 0 error, 0 skipped; JAR üretildi.** 703 mevcut teste 9 departman
şema senaryosu eklendi. Test edilen teslim ve `test` @ `3eb3691` aynı Git dosya
ağacını taşır. PostgreSQL 15.18 ve `127.0.0.1:5433 → 5432` önceden doğrulandı;
izole test şemaları temizlendi, mevcut `public` şema V17'de korundu. Ayrıntı
[V22 kabul kaydındadır](database.md#v22-yükseltme-ve-geri-alma-davranışı).

Bu dokümantasyon turunda backend testleri yeniden çalıştırılmadı. 712 sonucu
güncel CI, TEST deploy, frontend/mobile veya departman ürün kabulünün kanıtı değildir.
WF-2C2'nin 667 ve WF-8'in 703 testlik kayıtları kendi teslim tarihlerine aittir.

## Tarihsel belgeler

[WF-2A envanteri](WF2A_ROLE_NAME_ENVANTERI.md) ve
[WF-2D2 rollout](WF2D2_ROLE_ID_ROLLOUT.md) aşamalı dönüşümün gerekçe ve kanıtlarını
saklar; eski gate/sınıf/rol red davranışları güncel talimat değildir.
`archive/` altındaki kapatılmış görev ve ortam kabul kayıtları tarihsel kalır.
ADR-0003'ün rol kapsamı/tekillik önerisinin yerine ADR-0005/0007 geçmiştir;
ADR-0005/0006/0007 kabul edilmiş kararlardır, runtime teslim durumları yukarıdadır.
