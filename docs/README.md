# Dokümantasyon

Bu dizin **sözleşmeleri ve tasarım kararlarını** tutar: neyin nasıl çalıştığı,
hangi kuralın neden konduğu, hangi invariant'ın korunması gerektiği.

**Durum takibi burada değildir.** Neyin bittiği, neyin açık olduğu, açık bulgular,
sahiplik ve teslim koşulu tek bir yerde izlenir: **görev dağılımı ve yol haritası
belgesi** (repo dışında, `plan/` dizininde). Bu ayrım bilinçlidir — ilerleme
yüzdesi, test sayısı ve commit kaydı yazıldığı gün eskir; sözleşme eskimez.

Bir ADR'nin kabul edilmesi ilgili runtime'ın uygulandığı anlamına gelmez. Aynı
biçimde "kod mevcut", "dala birleşti" ve "ürün kabulü geçti" ayrı durumlardır.

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

## Departman kuralları — kalıcı sınırlar

Ayrıntı ve gerekçe ADR'lerdedir; aşağıdakiler değişmemesi gereken sınırlardır.

1. **Atama kişi veya departmandır.** İkisi birden dolamaz, ikisi de boş olabilir
   (`chk_records_assignment_exclusive`). Geçişin gerektirdiği atama uygulama
   transaction'ında doğrulanır; snapshot, update, event ve audit aynı bilgiyi
   taşır. [ADR-0005](decisions/0005-departman-atamasi-ve-akis-kurali.md) ·
   [ADR-0009](decisions/0009-audit-atama-sozlesmesi.md)
2. **Eligibility tek ortak kuraldan çözülür.** Güncel üyelik, aktif
   kullanıcı/departman/rol, uygun aktif transition/routing, permission ve
   aktör–kayıt ilişkisi **birlikte** aranır. Liste, detay, dosya, geçmiş ve
   bildirim alıcısı bağımsız yetki kuralı üretmemelidir.
   **Uyarı:** `findActiveUsersByDepartmentId` yalnız kullanıcı aktifliğini
   filtreler; tek başına eligibility çözümü **değildir**.
   [WF-2C2 / DB-8](WF2C2_DB8_GORUNURLUK_SOZLESMESI.md)
3. **Kapasite ve hiyerarşi sınırı.** Yerleşik rollerin kapasitesi gevşetilmez;
   departman routing hedefleri sınırsız kapasiteli uygun workflow rolleridir.
   Çoklu üyelik vardır; parent departmandan yetki devralma, otomatik eskalasyon
   ve claim mekanizması **yoktur**.
   [ADR-0007](decisions/0007-rol-kapasitesi-ve-birim-tekilligi.md)
4. **Migration ile kod aynı teslimde gider.** `DEPARTMENT` stratejisi,
   `DEPARTMANA_GONDER` ve geçiş seed'leri `V23` ile birlikte uygulanmıştır; bu
   tür bir migration tek başına eski backend üzerine dağıtılmaz.
   [ADR-0006](decisions/0006-departman-hedefli-target-strategy.md)

Grafik tasarımcısı, workflow versioning ve draft/publish **Workflow V2**
kapsamındadır. WF-8 servisinin hazır olması AP-8 ekranlarını kapatmaz.

## Doğrulama

Kurulum ve kalite komutları [kök README'dedir](../README.md#kalite-komutları).
Backend suite'i çalışan bir PostgreSQL ister ve **geliştirme veritabanına karşı
koşturulmaz** — ayrı bir test veritabanı açın.

İki tuzak ölçülerek doğrulanmıştır:

- **DB adresini ortam değişkeniyle verin.** `MailActionTokenIntegrationTest`
  adresi `System.getenv()` ile okur; `-DDB_NAME=...` o sınıfta çalışmaz ve
  geliştirme veritabanına şema açar.
- **Bağlantı havuzunu sınırlayın.** Her Spring test context'i kendi havuzunu
  açar; çok sayıda `@SpringBootTest` sınıfı PostgreSQL'in `max_connections`
  sınırını aşıp suite'i *"too many clients"* ile düşürür.
  `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` ile sınırlayın.

Ayrıca `PreviousActorReturnIntegrationTest` yalnız **temiz** bir veritabanında
geçer: `ROLE` stratejisi tam bir aktif kullanıcı arar, seed'li bir DB'de aktif
`BASKAN` sayısı ikiye çıkar ve testler düşer. Migration'lar kullanıcı seed'i
yapmaz; boş DB doğru başlangıçtır.

## Tarihsel belgeler

`archive/` altındaki belgeler **güncel talimat değildir**; kapatılmış görev
dağılımlarını, aşamalı dönüşüm kayıtlarını ve ortam kabul kanıtlarını saklar —
aralarında [WF-2A RoleName envanteri](archive/WF2A_ROLE_NAME_ENVANTERI.md) ve
[WF-2D2 RoleId rollout'u](archive/WF2D2_ROLE_ID_ROLLOUT.md) da vardır. WF-2A,
`RoleName`'in bütünüyle kaldırılması (`WF-2E`, V1 sonrası) gündeme geldiğinde
bağımlılık haritası olarak işe yarar.

ADR-0003'ün rol kapsamı/tekillik önerisinin yerine ADR-0005 ve ADR-0007
geçmiştir.
