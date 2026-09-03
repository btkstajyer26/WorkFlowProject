# ADR-0007: Rol Kapasitesi ve Birim Tekilliği

- Durum: Kabul Edildi
- Tarih: 2026-09-04
- Karar sahipleri: Burak (`WF`) · Alperen (`DB`) · Tamer (`AP`)

## Bağlam

İki ayrı ADR bu soruyu açık bırakarak birbirine devretti.

[ADR-0003](0003-veri-tanimli-akis-motoru-ve-birim-bazli-roller.md) rolün **daire
başına tekil** olmasını önerdi (`roles.scope`, `users.org_unit_id`,
`ROLE_IN_UNIT`) ve tekilliğin veritabanında kısmi bir `UNIQUE` indeksle
zorlanmasını istedi. Bu öneri uygulanmadı; birim semantiği
[ADR-0005](0005-departman-atamasi-ve-akis-kurali.md) ile `departments` +
`department_members` üzerine taşındı. Ama ADR-0003'ün **tekillik** yarısı bir
karara bağlanmadı: `roles.max_users` bugün hâlâ "sistem genelinde tek aktif
kullanıcı" anlamına geliyor ve README'de "tekil rol invariant'ı yalnız uygulama
seviyesinde zorlanıyor" maddesi açık duruyor.

[ADR-0006](0006-departman-hedefli-target-strategy.md) ise ters yönden aynı yere
geldi: departman gönderiminin anlamlı olması için `BASKAN_YARDIMCISI.max_users = 1`
kısıtının kalkması gerektiğini varsaydı ve bu varsayıma dayanarak `GONDER`'in
emekliye ayrılması sonucuna vardı. O varsayım bu ADR ile inceleniyor.

### Bugünkü mekanizma

| Yer | Ne yapıyor |
| --- | --- |
| `roles.max_users` (`V12`) | `NULL` sınırsız; dolu değer aktif kullanıcı sayısını sınırlar. Seed: `ADMIN`, `BASKAN`, `BASKAN_YARDIMCISI` = `1` |
| `RoleCapacityService.validate` | Projeksiyonlu sayım; rol satırlarını ID sırasıyla `PESSIMISTIC_WRITE` kilitler, sayım ve yazım aynı transaction'da |
| `UserService.setActive` | Aktif Başkan Yardımcısının pasifleştirilmesini reddeder |
| `UserService.changeRole` | Koltuk boşalıyorsa `replacementBaskanYardimcisiId` zorunlu; yerine geçen aktif ve `CALISAN` olmalı |
| `kullaniciIsleriniDevret` | Devirde `assigned_to` **ve** `last_deputy_id` yeni kullanıcıya taşınır |
| `TargetUserResolver.resolveSingleActiveRole` | `activeUsers.size() != 1` ise `WORKFLOW_ROLE_NOT_CONFIGURED` |

Kritik gözlem: **kapasite tarafı gevşetmeye zaten hazır.** `RoleCapacityService`
`max_users` değerini veriden okur ve `> 1` değerleri genel olarak işler — hata
mesajında çoğul dalı bile vardır. `max_users`'ı gevşetmek kapasite servisinde
**kod değişikliği gerektirmez**, veri değişikliğidir.

Kıran yer başka: `TargetStrategy.ROLE`. Bugünkü sekiz geçişten üçü bu stratejiyi
kullanır — `GONDER` ve `TEKRAR_GONDER` (`→ BASKAN_YARDIMCISI`), `BASKANA_ILET`
(`→ BASKAN`). Hedef rolde ikinci bir aktif kullanıcı belirdiği anda bu üç geçiş
`409 WORKFLOW_ROLE_NOT_CONFIGURED` dönmeye başlar.

Bağlayıcı kısıtlar:

- Workflow V1 kabulü **`max_users` sınırının korunmasını** şart koşuyor ve mevcut
  workflow'un regresyonsuz çalışmasını istiyor.
- `DB-1` §16: mevcut sekiz geçişin iş davranışını değiştirmek kapsam dışı.
- `DB-1` §6.1: `max_users` genel bir `CHECK` ile doğrulanamaz; sayım yalnız aktif
  kullanıcıları kapsar ve transaction içinde yapılır.

## Değerlendirilen Seçenekler

### S1 — `max_users` Workflow V1'de gevşetilir mi?

1. **Hayır; V1 boyunca korunur.** Departman modeli gevşetmeye ihtiyaç duymuyor:
   ADR-0005 routing kuralı bir **rol** işaret eder (`target_role_id`) ve V1
   senaryolarının işaret ettiği roller panelden açılan dinamik rollerdir
   (`HUKUK_UZMANI`, `SATIN_ALMA_UZMANI` …), `max_users` taşımazlar. Yerleşik üç
   rol departman routing hedefi değildir.
2. **Evet, yalnız `BASKAN_YARDIMCISI` için.** ADR-0006'nın varsaydığı yol. İkinci
   yardımcı eklendiği anda `GONDER` ve `TEKRAR_GONDER` kırılır; koltuk devri
   makinesi ("önce devret", `replacementBaskanYardimcisiId`) anlamsızlaşır ve V1
   kabulünün iki maddesi birden ihlal edilir.
3. **Bütün yerleşik roller için `max_users = NULL`.** `BASKANA_ILET` de kırılır;
   devir kuralları tamamen çöker. Kazanç yok, yıkım en geniş.

### S2 — Tekillik nerede zorlanır?

1. **Uygulama katmanı** — `RoleCapacityService` + rol satırı `PESSIMISTIC_WRITE`
   kilidi. Bugünkü uygulama. Yarış koşulu kilitle kapatılır; sınır veriden okunur.
2. **Kısmi `UNIQUE` indeks** (ADR-0003'ün `uq_active_scoped_role` önerisi).
   `max_users` **veri** olduğu için indeks onu okuyamaz: indeks ancak "bu rolde en
   fazla bir aktif kullanıcı" varsayımını şemaya dondurabilir, `max_users = 3` olan
   bir rolü ifade edemez. Yani `V12` ile seçilen modelle uyumsuzdur — ADR-0003 bu
   indeksi `roles.scope` enum'u varken önermişti, o enum hiç açılmadı.
3. **Trigger.** Statik veriyi trigger'la korumak projede başka hiçbir yerde
   kullanılmıyor; ayrıca aynı "sınır veridir" problemi burada da geçerli.

### S3 — Gevşetme günü geldiğinde `TargetStrategy.ROLE` ne yapar?

1. **Anlamı değişmez; o adım departman yoluna taşınır.** `ROLE` "tam olarak bir
   aktif kullanıcı" demeye devam eder, çok kullanıcılı adımlar `DEPARTMENT`
   stratejisiyle çözülür. İki primitive'in anlamı ayrık kalır.
2. **`ROLE` herhangi bir aktif kullanıcıyı seçer.** Kaydın kime düştüğü
   belirsizleşir; "neden bana geldi" sorusunun audit'te cevabı kalmaz.
3. **`ROLE` bir rol kuyruğuna dönüşür.** `assigned_to` ve `assigned_department_id`
   dışında üçüncü bir atama kavramı gerektirir; departman kuyruğunun yaptığı işi
   ikinci kez tanımlar.

### S4 — Başkan Yardımcısı koltuk devri kuralları

1. **Korunur.** `max_users = 1` sürdüğü sürece gereklidir: koltuk boşalırsa
   `GONDER` hedefsiz kalır.
2. **Kaldırılır.** Gevşetme olmadan yapılırsa koltuk boşalabilir ve `GONDER`
   `WORKFLOW_ROLE_NOT_CONFIGURED` döner — bu kuralların var olma sebebi tam olarak
   budur.

## Karar

**S1 → 1.** `roles.max_users` Workflow V1 boyunca **korunur**.
`BASKAN_YARDIMCISI`, `BASKAN` ve `ADMIN` için değer `1` kalır. Departman modeli bu
kısıt altında eksiksiz çalışır; departman routing kuralları `max_users` taşımayan
dinamik rolleri hedef gösterir.

Bunun doğrudan sonucu: **ADR-0006 bu ADR'ye bağımlı değildir.** ADR-0006'nın yazma
yolu (`DEPARTMANA_GONDER` + `targetDepartmentId`) `max_users` hiç gevşetilmeden
teslim edilebilir ve `GONDER` emekliye ayrılmaz.

**S2 → 1.** Tekillik uygulama katmanında, `RoleCapacityService` ve rol satırı
kilidiyle zorlanmaya devam eder. **ADR-0003'ün kısmi `UNIQUE` indeks önerisi
reddedilir:** sınır artık bir enum değil bir sayı kolonudur ve indeks veriden
okunan bir sınırı ifade edemez. README'deki "invariant yalnız uygulama seviyesinde"
maddesi bu ADR ile **bilinçli bir karar** hâline gelir; açık bir eksik olarak
kalmaz.

Kabul edilen bedel: invariant, veritabanına doğrudan `UPDATE` ile dokunan bir yol
karşısında korunmaz. Uygulamanın tek yazma yolu olduğu ve `PESSIMISTIC_WRITE`
kilidinin yarış koşulunu kapattığı kabul edilir.

**S3 → 1 (yön), kesin tasarım ertelenir.** Gevşetme günü geldiğinde `ROLE`'ün
anlamı değiştirilmez; çok kullanıcılı adım `DEPARTMENT` stratejisine taşınır. Hangi
adımların taşınacağı ve `GONDER` satırlarının akıbeti V1 sonrasına bırakılır ve
kendi kararını gerektirir.

**S4 → 1.** Koltuk devri kuralları korunur.

### Bağlayıcı sıra

`max_users` bir gün gevşetilirse aşağıdaki sıra **değiştirilemez**; tersine
çevrilirse akış üretimde kırılır:

```text
1. ADR-0006 yazma yolu üretimde çalışır ve testlidir
2. İlgili adım departman routing'e taşınır ve doğrulanır
3. ROLE stratejili geçiş satırı yeni migration ile is_active = FALSE yapılır
4. Koltuk devri kuralları (setActive / changeRole) gevşetilir
5. roles.max_users gevşetilir
```

Adım 5 önce yapılırsa `GONDER`, `TEKRAR_GONDER` veya `BASKANA_ILET` ikinci aktif
kullanıcının eklendiği anda `409` dönmeye başlar.

## Sonuçlar

Olumlu:

- Workflow V1 kabulünün "`max_users` sınırı korunur" ve "mevcut workflow
  regresyonsuz çalışır" maddeleri korunur; sekiz geçişin hiçbiri değişmez.
- ADR-0006 bağımsızlaşır: departman yazma yolu bir kapasite kararını beklemez.
- ADR-0003'ün açık bıraktığı tekillik sorusu kapanır; README'deki madde karara
  dönüşür.
- Kapasite tarafında kod değişikliği yoktur — `RoleCapacityService` bugünkü hâliyle
  `max_users > 1` değerlerini zaten işler.

Maliyet ve riskler:

- Tekillik invariant'ı veritabanı seviyesinde zorlanmaz; doğrudan SQL ile bozulabilir.
- `AP-5` routing editörü, `max_users` sınırlı bir yerleşik rolü hedef gösteren kural
  kaydedilmesine izin verirse departman tek kişiye çözülür ve model anlamını yitirir.
  Editör bu durumu görünür kılmalı veya engellemelidir.
- Koltuk devri makinesi (`replacementBaskanYardimcisiId`) yaşamaya devam eder;
  departman yolu yaygınlaştıkça operasyonel olarak tuhaf görünecektir.
- "Birden fazla Daire Başkanı" ihtiyacı — ADR-0003'ün çıkış noktası — bu ADR ile
  **karşılanmaz**; departman + dinamik rol yoluyla karşılanır. Yerleşik üç rolün
  çoğullaşması hâlâ açık bir sorudur.

Takip işleri:

- `AP-5` — routing kuralı kaydedilirken hedef rolün `max_users` değerini göster/uyar
- `AP-2` · `AP-3` — panelden açılan dinamik roller `max_users = NULL` ile açılır
- `docs/architecture.md` · `docs/database.md` — tekillik invariant'ının bilinçli bir
  karar olduğunun not edilmesi
- V1 sonrası — çok kullanıcılı yerleşik rol ihtiyacı doğarsa yukarıdaki bağlayıcı
  sırayı uygulayan yeni bir ADR

Kapsam dışı: `roles.scope` / `users.org_unit_id` (ADR-0005 ile yerine geçildi);
`ROLE` stratejisinin çok kullanıcılı davranışının kesin tasarımı; `user_roles` çoklu
ana rol; departman hiyerarşisi üzerinden eskalasyon.

## Kabul kaydı

Karar 4 Eylül 2026'da kabul edildi. Bağlayıcı sonuç: `roles.max_users` Workflow V1
boyunca değiştirilmez ve tekillik invariant'ı uygulama katmanında kalır. Bu kabul
[ADR-0006](0006-departman-hedefli-target-strategy.md)'nın kapasiteye bağımlı
olmadığını da kesinleştirir; iki karar birlikte uygulanabilir.

## Kapatılan sorular

| Soru | Cevap |
| --- | --- |
| ADR-0006 departman gönderimi için `max_users` gevşetilmeli mi? | Hayır — `S1 → 1`; routing hedefleri `max_users` taşımayan dinamik rollerdir |
| `GONDER` Workflow V1'de emekliye ayrılacak mı? | Hayır — kapasite korunduğu için `ROLE` stratejisi çalışmaya devam eder |
| Tekillik veritabanında zorlanacak mı? (ADR-0003) | Hayır — `S2 → 1`; sınır veri olduğu için kısmi indeks onu ifade edemez |
| Başkan Yardımcısı koltuk devri kaldırılacak mı? | Hayır — `S4 → 1` |

## Bağlantılar

- [`0003-veri-tanimli-akis-motoru-ve-birim-bazli-roller.md`](0003-veri-tanimli-akis-motoru-ve-birim-bazli-roller.md)
  — kısmi `UNIQUE` indeks önerisi ve birim bazlı tekillik
- [`0005-departman-atamasi-ve-akis-kurali.md`](0005-departman-atamasi-ve-akis-kurali.md)
  — birim semantiğinin departmana taşınması, `S3` "kural bir rol işaret eder"
- [`0006-departman-hedefli-target-strategy.md`](0006-departman-hedefli-target-strategy.md)
  — "`GONDER` emekliye ayrılacak mı?" başlığı
- [`DB_1_VERI_MODELI_SOZLESMESI.md`](../DB_1_VERI_MODELI_SOZLESMESI.md) — §6.1
  `max_users` sözleşmesi, §7.2 `target_strategy`, §8 sekiz geçiş seed'i
- [`workflow.md`](../workflow.md) — rol kapasitesi ve koltuk devri kuralları
- `user/service/RoleCapacityService.java` — projeksiyonlu sayım ve rol kilidi
- `user/service/UserService.java` — `setActive` ve `changeRole` koltuk kuralları
- `workflow/service/TargetUserResolver.java` — `resolveSingleActiveRole`
