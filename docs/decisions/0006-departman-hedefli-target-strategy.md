# ADR-0006: Departman Hedefli `target_strategy` ve Gönderim Sözleşmesi

- Durum: Kabul Edildi
- Tarih: 2026-09-03 (önerildi) · 2026-09-04 (kabul edildi)
- Karar sahipleri: Burak (`WF`) · Alperen (`DB`) · Tamer (`AP`)

## Bağlam

[ADR-0005](0005-departman-atamasi-ve-akis-kurali.md) departman atamasının **okuma ve
yetki** tarafını sabitledi: veri şekli (`DB-1` §15), üyelik, akış kuralı tablosu,
çözümleyici ve durum makinesine yansıması. Ama kaydı bir departmana **yazan** yolu
bilinçli olarak kapsam dışı bıraktı, çünkü bir geçişin departmanı hedef göstermesi
`target_strategy`'de departman değerlerini gerektiriyor ve `DB-1` §7.2 bunları
dondurmuş durumda.

Karar hazırlanırken durum şuydu: `records.assigned_department_id` ve `department_routing_rules`
**şekil olarak** kabul edildi (`DB-1` §15), `DepartmentRoutingResolver` tasarlandı —
fakat migration'lar `V17`'de duruyor, kodda departman kavramı yok ve bu kolonu yazan
hiçbir çalışma zamanı yolu tanımlı değil. Karar verilmezse `DB-11`/`DB-12`/`DB-13` ve
`WF-6` tamamlansa bile özellik üretimde hiç tetiklenmez.

**Uygulama durumu — 4 Eylül 2026:** V18–V21 departman, üyelik, routing ve kayıt
ataması şemasını/entity/repository katmanını ekledi; V22 DB-1 şema kısıtlarını
hizaladı. Bu ADR kabul edilmiştir, ancak `DEPARTMENT`, `DEPARTMANA_GONDER`,
geçiş seed'leri ve WF-5/WF-6 runtime'ı henüz uygulanmamıştır. Aşağıdaki gönderim
DDL'i için yeni bir ileri migration gerekir; V18 numarası artık kullanılmıştır.

> **Kapsam notu.** Bu ADR yalnız yazma yolunu açar. Departman gönderiminin anlamlı
> olması için routing kuralının işaret ettiği rolün **kapasite sınırı olmayan** bir rol
> olması gerekir (`roles.max_users IS NULL`). Workflow V1'in departman senaryoları
> zaten böyle kurgulanmıştır: yeni dinamik roller (`HUKUK_UZMANI`,
> `SATIN_ALMA_UZMANI` …) `max_users` taşımaz. Yerleşik `BASKAN_YARDIMCISI`
> (`max_users = 1`) departman routing hedefi olarak **kullanılmaz**; V1 kabulü
> `max_users` sınırının korunmasını şart koşuyor
> ([ADR-0007](0007-rol-kapasitesi-ve-birim-tekilligi.md) `S1 → 1`). Ayrıntı:
> aşağıdaki "`GONDER` emekliye ayrılacak mı?" başlığı.

Bağlayıcı kısıtlar:

- `WorkflowTransitionValidator`'ın **dokuz kontrolünün sırası ve hata kodları
  değişmez** (ADR-0005 ile devralınan kısıt; negatif testler tam kod assert ediyor).
- **Mevcut sekiz geçişin iş davranışı değişmez** (`DB-1` §16). Bugünkü `GONDER`
  akışı, tek aktif Başkan Yardımcısına çözümlenmeye devam eder.
- Uygulanmış `V15` düzenlenmez; değişiklik ileri yönlü migration'la gelir (`DB-1` §13.1).
- `DB-1` §7.2: "Desteklenmeyen bir primitive yalnız metin eklenerek etkinleştirilemez.
  DB constraint, resolver, validator ve testler **aynı değişiklikte** genişletilmelidir."

## Değerlendirilen Seçenekler

### S1 — Kaç yeni `target_strategy` değeri açılır?

1. **Yalnız `DEPARTMENT`.** Kayıt departmana atanır; o adımda kimin işlem yapacağı
   `department_routing_rules` üzerinden çözülür.
2. **`DEPARTMENT` + `DEPARTMENT_ROLE`.** İkincisi hedef rolü geçiş satırında sabitler.
   Ama ADR-0005 `S3` "kural bir rol işaret eder" dedi; aynı bilgi iki yerde tanımlanır
   ve ikisi ayrıştığında hangisinin kazandığı belirsiz kalır.
3. **Dördünü birden aç** (`PARENT_DEPARTMENT`, `EXPLICIT_USER` dahil). Karşılığı olan
   davranış yok; `DB-1` §7.2'nin dondurma gerekçesini boşa çıkarır.

### S2 — Hedef departmanın kimliği nereden gelir?

1. **İstekten** — `WorkflowActionRequest.targetDepartmentId`. Gönderen seçer.
2. **Kayıttan türetilir** (ör. oluşturanın departmanı). ADR-0005 `S6` çoklu üyeliğe
   izin verdiği için "oluşturanın departmanı" tekil bir soru değil; bu seçenek o
   kararla birlikte çözümsüz hâle geliyor.
3. **Geçiş satırında sabit** (`workflow_transitions.target_department_id`). N departman
   için tek grafik olamaz; her departman başına geçiş satırı çoğaltmak gerekirdi.

### S3 — Kişi mi departman mı: isteğin şekli

1. **`targetUserId` XOR `targetDepartmentId`.** İkisi birden dolu ise `400`. Bu,
   ADR-0005'in "kişi seçilmişse o kazanır" kuralıyla ve `records` üzerindeki
   karşılıklı dışlama `CHECK`'iyle aynı hizada durur.
2. **Tek alan + tip ayrımı** (`targetType` + `targetId`). İstemci sözleşmesini kırar,
   mevcut `targetUserId` alanını emekliye ayırmayı gerektirir.

### S4 — Departmana gönderim hangi geçişle yapılır?

Bu sorunun cevabını `uq_transition_from_action_role UNIQUE (from_status_id, action_id,
actor_role_id)` kısıtı daraltıyor: `TASLAK + GONDER + CALISAN` zaten **tek** satır ve
`ROLE` stratejili. Aynı aksiyonla ikinci bir departman satırı **tanımlanamaz**.

1. **Yeni aksiyon** (`DEPARTMANA_GONDER`). Mevcut sekiz satır hiç değişmez, `UNIQUE`
   kısıtı korunur, kullanıcıya "Gönder" ve "Departmana Gönder" ayrı görünür.
2. **Mevcut satırın stratejisini `DEPARTMENT`e çevirmek.** Role gönderim tamamen
   kaybolur; "mevcut davranış değişmez" kısıtı çiğnenir.
3. **Stratejiyi çalışma zamanında isteğe göre seçmek.** Grafik veri tanımlı olmaktan
   çıkar; hangi kuralın uygulandığı `workflow_transitions`'tan okunamaz hâle gelir.
4. **`UNIQUE` kısıtını `target_strategy` ile genişletmek.** `DB-1` §6.6'daki kabul
   edilmiş tekillik sözleşmesini değiştirir ve aynı adımda iki geçerli geçiş üretir.

### S5 — İstekte hedef beklentisi validator'a nasıl anlatılır?

Bugün `WorkflowAction.isTargetUserIdRequiredInRequest()` bütün aksiyonlarda `false`;
6. ve 7. kontroller "beklenen hedef alanı var mı / fazladan gönderilmiş mi" sorusunu
bu bayrakla cevaplıyor.

1. **Aksiyona ikinci bir istek bayrağı eklenir** (`targetDepartmentIdRequiredInRequest`).
   Aksiyon zaten yalnız *istekle ilgili* bilgiyi taşıyor (yorum zorunluluğu, istemci
   hedef alanı); bu bölünmeye uyar.
2. **Beklenti geçişe taşınır.** Hedef *çözümü* geçişin özelliği (`DB-1` §6.5), ama
   isteğin **şekli** aksiyonun özelliği; taşımak iki kavramı birleştirir ve bugünkü
   `WorkflowAction` javadoc'unun sözleşmesini bozar.

### S6 — Departman doğrulaması nerede yapılır?

1. **Servis katmanında**, `TargetUserResolver` deseniyle: istekteki departman var mı,
   aktif mi. Validator saf Java kalır, dokuz kontrol değişmez.
2. **Validator'ın içinde.** Departman varlığı repository sorgusu ister; `SM-5` ile
   kazanılan altyapısız test edilebilirlik kaybolur.

## Karar

**S1 → 1.** Yalnız `DEPARTMENT` açılır. `DEPARTMENT_ROLE`, `PARENT_DEPARTMENT` ve
`EXPLICIT_USER` dondurulmuş kalır; hedef rol bilgisi kuralın `target_role_id`'sindedir.

**S2 → 1.** Hedef departman istekte gelir: `WorkflowActionRequest.targetDepartmentId`.

**S3 → 1.** İstek düzeyinde karşılıklı dışlama: `targetUserId` ve `targetDepartmentId`
aynı anda dolu olamaz, aksi hâlde `VALIDATION_ERROR` (`400`). Bu kural DB'deki
`CHECK (assigned_to IS NULL OR assigned_department_id IS NULL)` ile aynı invariantın
istek tarafındaki karşılığıdır.

**S4 → 1.** Yeni aksiyon: **`DEPARTMANA_GONDER`**. Mevcut sekiz geçiş korunur; iki yeni
satır eklenir — aynı aksiyonun iki farklı geçişte kullanılması `CALISANA_GERI_GONDER`
deseninin aynısıdır:

| Kaynak durum | Aksiyon | Aktör | İlişki | Strateji | Hedef durum |
| --- | --- | --- | --- | --- | --- |
| `TASLAK` | `DEPARTMANA_GONDER` | `CALISAN` | `CREATOR` | `DEPARTMENT` | `BSK_YRD_INCELEMESINDE` |
| `DUZENLEME_BEKLIYOR` | `DEPARTMANA_GONDER` | `CALISAN` | `CREATOR_AND_ASSIGNEE` | `DEPARTMENT` | `BSK_YRD_INCELEMESINDE` |

Gerekli permission `RECORD_FORWARD`, `expected_target_role_id` **NULL**. Açıklama
zorunlu değildir (`comment_required = FALSE`), `GONDER` ile aynı.

Hedef durum bilerek mevcut `BSK_YRD_INCELEMESINDE`'dir. Workflow V1 yeni teknik
status açmaya izin vermez; ayrıca ADR-0005 routing kuralı da bu durumu departman
adımı olarak kullanır (`(departman, BSK_YRD_INCELEMESINDE, aksiyon) → rol`).
Durum adının departman bağlamında bire bir okunmaması kabul edilen bir bedeldir;
yeni bir durum düğümü Workflow V2 işidir.

**S5 → 1.** `WorkflowAction` ikinci bir istek bayrağı taşır. 6. ve 7. kontrollerin
**sırası ve hata kodları aynı kalır**; yalnız "beklenen hedef alanı" tanımı iki alanı
birden kapsar:

- Beklenen alan yoksa → `WORKFLOW_TARGET_REQUIRED` (bugünkü kod)
- Beklenmeyen alan gönderilmişse → `WORKFLOW_TARGET_NOT_ALLOWED` (bugünkü kod)

Validator'a **yeni hata kodu eklenmez.**

**S6 → 1.** Departmanın varlığı ve aktifliği servis katmanında, `TransitionContext`
doldurulmadan önce doğrulanır. İki servis katmanı kodu kullanılır:

- `WORKFLOW_DEPARTMENT_INVALID` (`400`) — istekteki departman yok veya pasif.
- `WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED` (`409`) — ADR-0005'ten; kural yok ya da
  işaret edilen rolde aktif üye yok.

### Başarılı geçiş ne yazar

`assigned_department_id = istekteki departman`, `assigned_to = NULL`. Karşılıklı
dışlama `WorkflowRecordUpdate`'in compact constructor'ında ve DB `CHECK`'inde
korunur (ADR-0005 `S8`).

### Şema karşılığı

DDL `DB-1`'e aittir, burada tekrarlanmaz. WF-5/WF-6 ile birlikte hazırlanacak
ayrı ileri migration'ın kapsamı:
`chk_transition_target_strategy` ve `chk_transition_target_strategy_role` kısıtları
`DEPARTMENT` değerini (ve onun için `expected_target_role_id IS NULL` koşulunu)
kapsayacak biçimde yeniden oluşturulur; `workflow_actions`'a `DEPARTMANA_GONDER`
satırı, `workflow_transitions`'a yukarıdaki iki satır seed edilir. Uygulanmış `V15`
düzenlenmez.

## Sonuçlar

Olumlu:

- Mevcut sekiz geçiş ve `GONDER` davranışı birebir korunur; departman gönderimi
  yanına eklenir, yerine geçmez.
- `uq_transition_from_action_role` ve `DB-1` §6.6 tekillik sözleşmesi değişmez.
- Validator'ın dokuz kontrolü, sırası ve hata kodları sabit kalır. 8. ve 9. kontroller
  `expectedTargetRoleId != null` koşuluyla korunduğu için departman geçişinde
  çalışmazlar. `WorkflowTransitionValidator`'ın 6–7. kontrolleri kullanıcı ve departman hedef alanlarını ayrı değerlendirir; dosya değişir, dokuz kontrolün sırası ve mevcut hata kodları korunur.
- ADR-0005'in `DepartmentRoutingResolver` tasarımı olduğu gibi kullanılır; bu ADR
  yalnız onu tetikleyen yolu açar.

Maliyet ve riskler:

- `WorkflowActionRequest` dördüncü bileşen kazanır ve kayıt **29 yerde** kuruluyor
  (çoğu test, biri `MailActionTokenService`). Üç argümanlı ikincil bir constructor
  eklenerek mevcut çağrı noktaları değiştirilmeden derlenir; yeni alan yalnız
  departman gönderiminde dolar.
- `TransitionRules` (test referansı) 8 → 10 satır, parity'nin `EXPECTED_RULE_COUNT`
  değeri ve 6×7×4 = 168 kombinasyonu 6×8×4 = 192 olur.
- `WorkflowRecordSnapshot` `assignedDepartmentId` taşımak zorunda (`WF-5`); aksi hâlde
  departmana atanmış kayıtta yetki çözümlenemez.
- `GONDER` ve `DEPARTMANA_GONDER` bir arada durduğu sürece "hangi kayıt niye kişiye,
  niye departmana gitti" sorusu operasyonel bir belirsizlik yaratır; panel bunu
  ekranda açıkça ayırmalı.
- Mobil istemci yeni aksiyonu tanımadan gönderim ekranını açmamalı; `WorkflowAction`
  enum'u istemci sözleşmesinde paylaşılıyor.
- **Çekirdek tipler `DEPARTMENT` için genişletilmek zorundadır.** Validator'ın
  değişmemesi, hiçbir şeyin değişmediği anlamına gelmiyor. Bugünkü kod "hedef =
  kullanıcı" varsayımını üç yerde taşıyor ve `DEPARTMENT` bu varsayımı kırar:

  | Yer | Bugünkü davranış | `DEPARTMENT` eklenince |
  | --- | --- | --- |
  | `TransitionRule` compact constructor | `targetStrategy != NONE` ⇒ `expectedTargetRoleId` zorunlu | `expected_target_role_id = NULL` taşıyan departman satırı **açılışı düşürür**; kural `DEPARTMENT`'ı da muaf tutmalı |
  | `WorkflowApplicationService.requiresTargetUser` | `targetStrategy != NONE` | `DEPARTMENT` hedef *kullanıcı* gerektirmez; aksi hâlde iki geçişli doğrulama `IllegalStateException` ile durur |
  | `TargetUserResolver.resolve` | `TargetStrategy` üzerinde tüketici `switch` | Yeni sabit derleme hatası üretir; departman dalı açıkça ele alınmalı |

  Üçü de `DB-1` §7.2'nin "DB constraint, resolver, validator ve testler **aynı
  değişiklikte** genişletilmelidir" kuralının kapsamındadır ve `WF-5`/`WF-6` ile
  birlikte yapılır.
- **Atama ve olay modeli departman alanı taşımalıdır.** `WorkflowRecordUpdate`
  bugün yalnız `assignedTo` yazar; `assignedDepartmentId` eklenmeli ve karşılıklı
  dışlama compact constructor'da doğrulanmalıdır (ADR-0005 `S8`).
  `WorkflowStatusChangedEvent` de departmanı taşımalıdır: bugün `assignedTo`
  boş kaldığında `recipientsOf` oluşturan + son yardımcıya düşüyor; departman
  gönderiminde bu **yanlış alıcı kümesidir** (`NT-5`).
- **Kapasite kısıtı.** Departman routing kuralı `max_users` sınırlı bir yerleşik role
  (`BASKAN`, `BASKAN_YARDIMCISI`, `ADMIN`) işaret ederse departman tek kişiye çözülür
  ve departman modeli anlamını yitirir. `AP-5` editörü kural kaydederken bunu
  görünür kılmalı; V1 senaryoları hedef olarak `max_users` taşımayan dinamik rolleri
  kullanır.

Takip işleri:

- `DB-13` · ayrı ileri migration — CHECK'lerin genişletilmesi, aksiyon ve iki geçiş satırının seed'i
- `WF-5` — `WorkflowRecordSnapshot` ve `WorkflowRecordUpdate`'in departman alanı;
  `WorkflowRecordUpdate` compact constructor'ında karşılıklı dışlama
- `WF-5` — `TransitionRule` invariant'ının `DEPARTMENT` muafiyeti,
  `requiresTargetUser` ayrımı ve `TargetUserResolver` departman dalı
- `WF-6` — `DepartmentRoutingResolver`'ın gönderim yoluna bağlanması
- `WF-6` — `WorkflowStatusChangedEvent`'in departmanı taşıması (`NT-5` alıcı çözümü)
- `AP-4` · `AP-5` — gönderim ekranında departman seçici; kişi/departman ayrımının görünür kılınması
- `NT-5` — departmana fan-out (ADR-0005: rol bazlı küme, `user_id` ile tekilleştirilir)
- `docs/FRONTEND_BACKEND_SOZLESMESI.md` ve `docs/MOBIL_API_ENVANTERI.md` — yeni aksiyon
  ve `targetDepartmentId` alanı

Kapsam dışı: `DEPARTMENT_ROLE`, `PARENT_DEPARTMENT`, `EXPLICIT_USER` stratejileri;
hiyerarşi üzerinden eskalasyon; departmandan kişiye devir; departmana **geri gönderme**
(`CALISANA_GERI_GONDER` kişiye gitmeye devam eder); workflow versioning;
`roles.max_users` kapasite sınırının gevşetilmesi ve `GONDER`'in akıbeti (V1 sonrası
açık soru — aşağıya bakınız).

## Kabul kaydı

Karar 4 Eylül 2026'da kabul edildi. Kabul, `S1`–`S6` seçimlerinin ve aşağıdaki
"Kapatılan sorular" başlığının bağlayıcı olduğu anlamına gelir; uygulama işleri
`DB-13` ileri migration'ı, `WF-5`, `WF-6`, `AP-4`/`AP-5` ve `NT-5` altında yürür ve
Workflow V1'in 10 Eylül teslim kapsamındadır.

Kabul sırasında iki düzeltme yapıldı:

1. "`GONDER` emekliye ayrılacak mı?" sorusunun cevabı **Evet**'ten **V1'de
   hayır**'a çevrildi; gerekçe bu ADR'nin kendi bağlayıcı kısıtı, `DB-1` §16 ve
   V1 kabulünün "`max_users` sınırı korunur" maddesidir
   ([ADR-0007](0007-rol-kapasitesi-ve-birim-tekilligi.md) `S1 → 1`).
2. "Kod değişikliği gerektirmez" iddiası düzeltildi: değişmeyen şey
   `WorkflowTransitionValidator`'dır; `TransitionRule`, `requiresTargetUser`,
   `TargetUserResolver`, `WorkflowRecordUpdate` ve `WorkflowStatusChangedEvent`
   genişletilmek zorundadır (bkz. "Maliyet ve riskler").

## Kapatılan sorular

**`GONDER` emekliye ayrılacak mı? → Workflow V1'de hayır.** `GONDER` ile
`DEPARTMANA_GONDER` V1'de birlikte durur. Emeklilik seçeneği bu ADR'de reddedildi,
çünkü üç bağlayıcı kayda birden aykırı:

- bu ADR'nin kendi bağlayıcı kısıtı — "mevcut sekiz geçişin iş davranışı değişmez";
- `DB-1` §16 — "mevcut sekiz geçişin iş davranışının değiştirilmesi" kapsam dışı;
- Workflow V1 kabulü — mevcut workflow regresyonsuz çalışır ve **`max_users` sınırı
  korunur**.

Altındaki teknik gözlem yine de geçerlidir ve **kısıt olarak kayda geçer**:
`TargetStrategy.ROLE`, `TargetUserResolver.resolveSingleActiveRole` içinde
`activeUsers.size() != 1` olduğunda `WORKFLOW_ROLE_NOT_CONFIGURED` döner. Yani
`BASKAN_YARDIMCISI` rolüne ikinci bir aktif kullanıcı eklendiği anda `GONDER` kırılır.

V1'deki karşılığı bir emeklilik değil, bir **kısıttır**: departman routing kuralları
kapasite sınırlı yerleşik rolleri hedef göstermez; departman hedefleri `max_users`
taşımayan dinamik rollerdir. `BASKAN_YARDIMCISI.max_users = 1` V1 boyunca korunur ve
`GONDER` bugünkü davranışını sürdürür.

Kapasite sınırının gevşetilmesi — ve o gevşetildiğinde `GONDER`'in `ROLE`
stratejisiyle yaşayamayacağı — [ADR-0007](0007-rol-kapasitesi-ve-birim-tekilligi.md)
ile ayrıca ele alındı. O ADR `max_users`'ın V1 boyunca korunmasına karar verir,
gevşetme gününe **bağlayıcı bir sıra** tanımlar (önce departman yolu çalışır, sonra
`ROLE` satırı pasifleşir, en son `max_users` gevşer) ve bu ADR'nin ona bağımlı
olmadığını tespit eder: V1 yazma yolu kapasite hiç gevşetilmeden teslim edilebilir.

`AP-4`/`AP-5` bu nedenle **tek seçici sunmaz**: gönderim ekranı kişi ve departman
yollarını ayrı ayrı gösterir (yukarıdaki "Maliyet ve riskler" maddesiyle aynı hizada).

**Yeniden gönderim aynı departmana mı gider? → Hayır, gönderen serbest seçer.**
Kayıt "departman hafızası" tutmaz; `DEPARTMANA_GONDER` her seferinde isteği okur.
Gerekçe grafiğin kendi deseninde: bugün de `TEKRAR_GONDER` hedefi yeniden çözer,
önceki atananı hatırlamaz. Departman için farklı davranmak `records`'a `last_deputy_id`
benzeri ikinci bir hafıza kolonu (`last_department_id`) eklemeyi gerektirirdi ve
"geri gönderilen kayıt neden başka departmana gidemiyor" sorusunu üretirdi. Geri
**gönderme** hedefleri (`CREATOR`, `PREVIOUS_ACTOR`) sabit kalmaya devam eder; sabit
olan geri dönüştür, yeniden gönderim değil.

## Bağlantılar

- [`0005-departman-atamasi-ve-akis-kurali.md`](0005-departman-atamasi-ve-akis-kurali.md)
  — okuma/yetki tarafı, `DepartmentRoutingResolution`, atama sözleşmesi
- [`DB_1_VERI_MODELI_SOZLESMESI.md`](../DB_1_VERI_MODELI_SOZLESMESI.md) — §6.5 aksiyon
  katalogu, §6.6 geçiş tekilliği ve CHECK'ler, §7.2 dondurulmuş stratejiler, §15
  departman veri şekli
- [`workflow.md`](../workflow.md) — validator kontrol sırası ve hata kodları
- `workflow/statemachine/TargetStrategy.java` — dondurulmuş değerlerin javadoc'u
- `workflow/statemachine/WorkflowAction.java` — istek bayrakları
- `workflow/service/TargetUserResolver.java` — servis katmanı çözümleme deseni
- `workflow/dto/WorkflowActionRequest.java` — istek sözleşmesi
