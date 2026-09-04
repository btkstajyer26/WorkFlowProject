# ADR-0003: Veri tanımlı akış motoru ve birim bazlı roller

- Durum: Yerine Geçildi — [ADR-0005](0005-departman-atamasi-ve-akis-kurali.md) (birim kapsamı) · [ADR-0007](0007-rol-kapasitesi-ve-birim-tekilligi.md) (rol tekilliği) · motor tarafı uygulandı
- Tarih: 2026-08-25
- Karar sahipleri: Proje ekibi (mühendislik tarafının talebi üzerine)

> **Uygulanma durumu (3 Eylül 2026).** Aşağıdaki §Bağlam bölümü 25 Ağustos'un
> fotoğrafıdır ve bugünkü kodu tarif etmez; ADR biçimi gereği düzenlenmemiştir.
> O tarihten bu yana kararın **motor tarafı uygulandı**: geçiş kuralları
> `workflow_transitions` tablosundan okunuyor (`ReloadableTransitionRuleSource` →
> `DbTransitionRuleSource`), statik tablo `TZ-1` ile test ağacına taşındı,
> `hasRole` anotasyonu kalmadı (yetkilendirme `hasAuthority` permission
> kodlarıyla), workflow rol kimliği `RoleId` oldu ve
> `WorkflowAction.getExpectedTargetRole()` kaldırıldı. **Uygulanmayan kısım
> birim bazlı rol kapsamıdır** (`roles.scope`, `users.org_unit_id`,
> `ROLE_IN_UNIT`); yerine sistem geneli tekillik `roles.max_users` ile taşınıyor.
> Birim bazlı rol kapsamı önerisinin yerine
> [ADR-0005](0005-departman-atamasi-ve-akis-kurali.md) geçti: birim semantiği
> `roles.scope` / `users.org_unit_id` ile değil `departments` + `department_members`
> ile taşınır; üyelik kendi rolünü taşımaz, yetki global `users.role_id` ile çözülür
> (ADR-0005 `S4 → 1`). `ROLE_IN_UNIT` yerine `(departman, durum, aksiyon) → rol`
> akış kuralı gelir (ADR-0005 `S2`/`S3`).
>
> §Karar'ın **"Tekilliğin veritabanında zorlanması"** alt başlığı
> [ADR-0007](0007-rol-kapasitesi-ve-birim-tekilligi.md) `S2 → 1` ile **reddedildi**:
> tekillik `roles.scope` enum'u yerine `roles.max_users` sayı kolonuyla taşındığı
> için kısmi `UNIQUE` indeks veriden okunan sınırı ifade edemez. Invariant uygulama
> katmanında, `RoleCapacityService` ve rol satırı kilidiyle zorlanır.
>
> Aşağıdaki §Karar bölümünün **veri modeli önerisi de uygulanmadı**: `flows`,
> `flow_steps`, `flow_transitions`, `flow_step_visibility` ve `record_step_holders`
> yerine `workflow_statuses` / `workflow_actions` / `workflow_transitions` üçlüsü
> uygulandı; tek adım geri dönüş işaretçisi `records.last_deputy_id` olarak korundu
> (`TargetStrategy.PREVIOUS_ACTOR`). §Karar içindeki **akış sürümleme** maddesi
> ("pazarlık konusu değildir") Workflow V1 kapsamında **değildir**; versioning,
> draft/publish ve grafik düzenleme Workflow V2'ye bırakıldı — sınır
> `DB-1` §14 ve Workflow V1/V2 planındadır.

## Bağlam

Onay akışı bugün Java'da sabit yazılı. Mühendislik tarafı akışın **veri**
olmasını istiyor: sisteme yeni bir rol — örneğin **Daire Başkanı** — eklendiğinde
kod değiştirilmemeli, veritabanına rol ve akış satırları eklenmeli.

Ek kısıt, ilk tasarım turunda açık bırakılan sorunun cevabıyla geldi:
**birden fazla Daire Başkanı olacak.** Yani rol sistemde tekil değil, **daire
başına tekil.** Bu, mevcut modelin en temel varsayımını doğrudan hedef alıyor.

### Bugünkü durum

Geçiş tablosunun kendisi zaten tablo: `TransitionRules` sekiz satırlık bir liste
ve `WorkflowTransitionValidator` onu indeksleyip sorguluyor. Kural eklemek kod
akışını değiştirmiyor. Sorun tabloda değil, **tablonun etrafındaki sözlükte**:

| Yer | Ne varsayıyor |
| --- | --- |
| `RecordStatus` | 6 durum, `terminal` / `editableByCreator` bayrakları |
| `WorkflowAction` | 7 aksiyon, `commentRequired` + `expectedTargetRole` |
| `RoleName` | 4 rol, `workflowActor` bayrağı |
| `TargetUserResolver` | Aksiyon başına `switch`; hedef = "o roldeki **tek** aktif kullanıcı" |
| `RecordAccessPolicy` | Rol başına `switch`, durum adları gömülü |
| `records.last_deputy_id` | **Tek adım** geri dönüş işaretçisi |
| Audit kırpması | İki özel sorgu (`…DevreKadar`, `…IletimdenItibaren`) |
| `NotificationType.of` | Aksiyon → bildirim türü `switch` |
| `@PreAuthorize("hasRole('CALISAN')")` | 7 anotasyon, rol adına bağlı |
| `RecordActionPanel.tsx` | Rol + durum → aksiyon; arayüzde **ikinci bir durum makinesi** |

Ölçülen yayılım (25 Ağustos 2026, `test` dalı):

| Alan | Dosya | Satır |
| --- | ---: | ---: |
| Backend main | 36 | 98 |
| Backend test | 25 / 48 | 465 |
| Frontend (`src` + `e2e`) | 29 | 186 |
| Mobil | 7 | 33 |
| **Toplam** | **97** | **782** |

### Tekil rol varsayımı

`TargetUserResolver` her hedefi "o roldeki aktif kullanıcıları getir, sayı 1
değilse `WORKFLOW_ROLE_NOT_CONFIGURED` ile dur" diye çözüyor.
`UserService.ensureSingletonRoleAvailable` bu tekilliği yazma anında zorluyor.

Birden fazla Daire Başkanı bu varsayımı geçersiz kılıyor: aynı rolde N aktif
kullanıcı olacak ve "hangisi?" sorusunun cevabı **evrağa** bağlı olacak.

## Değerlendirilen Seçenekler

### 1. Her daire için ayrı rol

`DAIRE_BASKANI_PERSONEL`, `DAIRE_BASKANI_BILGI_ISLEM`, … Tekil rol varsayımı
korunur, mevcut çözümleyici hiç değişmez.

- **Artı:** Kod tarafında en küçük değişiklik; `TargetUserResolver` aynen kalır.
- **Eksi:** Rol patlaması. Her yeni daire yalnız bir rol değil, o rolün geçtiği
  **bütün geçiş satırlarının kopyasını** gerektirir; N daire için akış tablosu
  N katına çıkar. Görünürlük ve yetki satırları da çoğalır. Kaçınılmak istenen
  "yeni birim = elle çoğaltma" problemi, koddan veriye taşınmış hâliyle aynen
  devam eder. **Reddedildi.**

### 2. Rolü birim kapsamıyla nitelemek

Tek bir `DAIRE_BASKANI` rolü; hangi kullanıcının hedef olduğu **rol + birim**
çiftinden çözülür. Birim, evraktan gelir.

- **Artı:** Akış tablosu daire sayısından bağımsız — bir satır bütün daireler
  için geçerli. Yeni daire açmak yalnız `org_units` ve `users` satırı.
- **Eksi:** `records` tablosuna birim alanı, kullanıcılara birim bağı,
  görünürlüğe birim boyutu ekleniyor. Mevcut kayıtlar için geriye dönük doldurma
  kararı gerekiyor.

### 3. Tam organizasyon hiyerarşisi

İç içe birimler (Genel Müdürlük → Daire → Şube), vekâlet, yetki devri.

- **Artı:** Gerçek kurumsal yapıya en yakın.
- **Eksi:** Bugün istenen şey için fazlasıyla geniş. Hiyerarşik çözümleme
  ("en yakın üst birimin başkanı") ayrı bir tasarım ve doğrulama yükü getiriyor.
  **Şimdilik reddedildi**; aşağıda genişleme yolu bırakıldı.

## Karar

**Seçenek 2.** Akış tanımı veritabanına taşınır; roller `GLOBAL` veya `UNIT`
kapsamlı olur; hedef çözümlemesi evrağın birimini kullanır.

### Rol kapsamı

`roles` tablosuna `scope` eklenir:

| Kapsam | Anlamı | Örnek |
| --- | --- | --- |
| `GLOBAL` | Sistemde **tek** aktif kullanıcı | `ADMIN`, `BASKAN`, `BASKAN_YARDIMCISI` |
| `UNIT` | **Birim içinde** tek aktif kullanıcı | `DAIRE_BASKANI` |
| `MULTI` | Tekillik aranmaz | `CALISAN` |

`CALISAN` bugün de tekil değil; üçüncü kapsam bu gerçeği modele yazıyor —
şu an `SINGLETON_ROLES` kümesinin dışında tutularak örtük bırakılmış durumda.

### Tekilliğin veritabanında zorlanması

Bugün tekillik yalnız uygulama katmanında korunuyor. Birim boyutu eklenince
uygulama kontrolü tek başına yetmez (yarış koşulu iki daire başkanı üretebilir).
PostgreSQL 15 — projenin kullandığı sürüm — `NULLS NOT DISTINCT` desteklediği
için tek bir kısmi indeks her iki kapsamı da kapatır:

```sql
-- GLOBAL rollerde org_unit_id NULL'dur; NULLS NOT DISTINCT olmadan iki NULL
-- satır birbirinden farklı sayılır ve indeks tekilliği koruyamazdı.
CREATE UNIQUE INDEX uq_active_scoped_role
    ON users (role_id, org_unit_id) NULLS NOT DISTINCT
 WHERE is_active AND role_scope <> 'MULTI';
```

Bu, README'de açık madde olarak duran "tekil rol invariant'ı yalnız uygulama
seviyesinde zorlanıyor" sorununu da kapatır.

### Yönlendirme anahtarı: evrağın birimi, aktörün birimi değil

`records.org_unit_id` kayıt oluşturulurken **oluşturanın biriminden** yazılır ve
**değişmez**.

Bu ayrım kritik: hedef çözümlemesi evrağın birimini kullanır, işlemi yapan
kişinin o anki birimini değil. Bir personel akış ortasında başka daireye
geçerse, yoldaki evraklar özgün dairelerine yönlenmeye devam eder. Aksi hâlde
personel nakli, yolda kalmış evrakları sessizce başka bir daire başkanının
önüne düşürürdü.

### Veri modeli

```text
org_units       (id, code, name, is_active)
roles           (id, name, scope, is_workflow_actor)          ← scope eklendi
role_permissions(role_id, permission)                          ← yeni
users           (…, org_unit_id)                               ← eklendi, UNIT rollerde zorunlu

flows              (id, code, version, is_active)
flow_steps         (id, flow_id, code, name, ordinal,
                    is_initial, is_terminal, editable_by_owner,
                    assignee_rule, assignee_role_id)
flow_transitions   (id, flow_id, from_step_id, to_step_id,
                    action_code, actor_role_id, actor_requirement,
                    comment_required, notification_type, label, ordinal)
flow_step_visibility (step_id, role_id, scope, unit_scope, history_scope)

records            (…, flow_id, flow_version, current_step_id, org_unit_id)
record_step_holders(record_id, step_id, user_id)               ← last_deputy_id yerine
```

### Adımın kime düştüğü

`flow_steps.assignee_rule`:

| Kural | Anlamı | Bugünkü karşılığı |
| --- | --- | --- |
| `OWNER` | Kaydı oluşturan | `CALISANA_GERI_GONDER` hedefi |
| `ROLE` | O roldeki tek aktif kullanıcı (`GLOBAL`) | `BASKANA_ILET` |
| `ROLE_IN_UNIT` | O roldeki, **evrağın birimindeki** tek aktif kullanıcı | *(yeni)* |
| `PREVIOUS_HOLDER` | Bu adımı en son elinde tutan kişi | `last_deputy_id` |
| `NONE` | Kimseye düşmez | Terminal durumlar |

`ROLE_IN_UNIT` bu kararın tek yeni çözümleme kuralıdır. Diğer dördü bugünkü
davranışı birebir karşılıyor.

### Örnek akış — veri, kod değil

Daire Başkanı zincire eklenmiş hâliyle `flow v2`:

| # | Adım | Kime düşer |
| --: | --- | --- |
| 1 | `TASLAK` | `OWNER` |
| 2 | `DAIRE_BASKANI_INCELEMESINDE` | `ROLE_IN_UNIT` (`DAIRE_BASKANI`) |
| 3 | `BSK_YRD_INCELEMESINDE` | `ROLE` (`BASKAN_YARDIMCISI`) |
| 4 | `BASKAN_INCELEMESINDE` | `ROLE` (`BASKAN`) |
| 5 | `ONAYLANDI` / `REDDEDILDI` | `NONE` |
| — | `DUZENLEME_BEKLIYOR` | `OWNER` |

Yeni bir daire açmak bu tabloya **hiç dokunmaz**: `org_units` satırı, o dairenin
başkanı için `users` satırı, bitti. Akış tanımı daire sayısından bağımsızdır.

### Görünürlük

`flow_step_visibility` üç boyut taşır:

- `scope` — `ASSIGNEE` / `ROLE_WIDE` / `OWNER` / `PREVIOUS_HOLDER`
- `unit_scope` — `ANY` / `SAME_UNIT`
- `history_scope` — `FULL` / `FROM_FIRST_ENTRY` / `UNTIL_HANDOFF`

Daire Başkanı `ROLE_WIDE` + `SAME_UNIT` alır: kendi dairesindeki, o adımdaki her
evrağı görür; başka dairenin evrağını görmez.

`history_scope` bugünkü iki özel audit sorgusunu tek kurala indirger:
**kullanıcı, evrağın kendi kapsamına ilk girdiği andan itibarini görür.**

### Yetki anotasyonları

`hasRole('CALISAN')` → `hasAuthority('RECORD_CREATE')`. Yetki *kim olduğuna*
değil *ne yapabildiğine* bağlanır; `role_permissions` ikisini birleştirir.

**Roller veri, izin listesi koddur.** İzinler kapalı bir enum olarak kalır;
aksi hâlde `role_permissions` herkese her şeyi verebilir hâle gelir ve RBAC
anlamını yitirir.

### Akış sürümleme

`records` hem `flow_id` hem `flow_version` tutar. Akış tanımı değiştiğinde
yolda olan kayıtlar başladıkları sürümde kalır. Bu **pazarlık konusu değildir**:
sürümleme olmadan tanım değişikliği yarı yoldaki evrakları tanımsız duruma
düşürür.

### Bozuk akış tanımının engellenmesi

Kod tablosunda derleyici koruyordu; veritabanı tablosu istediği kadar bozuk
olabilir. Akış `is_active = true` yapılırken bir doğrulayıcı çalışmalı:

- tam olarak bir başlangıç adımı,
- en az bir **ulaşılabilir** terminal adım,
- terminal olmayan her adımdan en az bir çıkış,
- `(adım, aksiyon, rol)` üçlüsünde mükerrer satır yok,
- `ROLE` / `ROLE_IN_UNIT` kurallarının işaret ettiği rolün kapsamı tutarlı.

Bozuk akış **hiç etkinleşmemeli**. Çalışma zamanına bırakılırsa hata, aylar
sonra yolda kalmış bir evrakta ortaya çıkar.

## Sonuçlar

### Olumlu

- Yeni daire açmak veya daire başkanı atamak **kod değişikliği gerektirmez**.
- Akış tablosu daire sayısından bağımsız kalır; N daire için N kopya yok.
- Tekil rol invariant'ı veritabanı seviyesine iner (README'de açık madde).
- Arayüz ikinci bir durum makinesi taşımayı bırakır; aksiyonları uçtan öğrenir.
- Bugünkü davranışın tamamı yeni modelde ifade edilebiliyor — geçiş, davranış
  değişikliği olmadan yapılabilir.

### Maliyet

Fazlara bölünmüş tahmin (kodu bilen biri için, adam-gün):

| Faz | İçerik | Tahmin |
| --- | --- | ---: |
| 0 | Şema + mevcut akışın `flow v1` olarak tohumlanması + eşdeğerlik testi | 2–3 |
| 0b | `org_units`, `users.org_unit_id`, `records.org_unit_id` + geriye dönük doldurma | 2–3 |
| 1 | Motor kuralları veritabanından okur; Java tablosu gölge oracle kalır | 4–6 |
| 2 | Enum'lar sözlük olmaktan çıkar (97 dosya, 465 test satırı) | 12–18 |
| 3 | Yetki, görünürlük ve geçmiş kırpması veriye | 5–7 |
| 4 | `available-actions` ucu; frontend ve mobil oradan beslenir | 4–6 |

**Toplam ≈ 29–43 adam-gün.** İş paylaşılan tiplerin içinden geçtiği için
paralelleşmiyor: 97 dosyanın tamamı aynı iki enum'a bağlı, iki kişi aynı anda
çalışırsa sürekli çakışır. Adam eklemek takvimi kısaltmaz.

Bu, mevcut projenin üretildiği 3 haftalık takvimden farklı bir iş şeklidir:
o dönemde 12 kişi **ayrılabilir** modüller üzerinde paralel çalışıyordu; bu
refactor tek bir kesişen değişikliktir.

### Riskler

- **Yarıda bırakmak, hiç başlamamaktan kötüdür.** Faz 2'nin ortasında durulursa
  geriye iki doğruluk kaynağı, yarısı göç etmiş testler ve sahibi olmayan bir
  şema kalır. Faz 2 bitirilemeyecekse başlanmamalıdır.
- **Enum → metin dönüşümü sessiz bozar.** `audit_logs.action`,
  `previous_status`, `new_status` serbest metne döner; yazım hatası derleme
  zamanında değil çalışma zamanında ortaya çıkar. Java tablosunun bir sürüm
  boyunca gölge olarak tutulması bu yüzden zorunlu.
- **Başkansız daire akışı kilitler.** Bir dairede aktif Daire Başkanı yoksa o
  dairenin evrakları ilerleyemez. Hata mesajı birimi içermeli
  (`WORKFLOW_ROLE_NOT_CONFIGURED` + birim adı), yoksa Admin nerede boşluk
  olduğunu göremez.
- **Mevcut kayıtların birimi.** `records.org_unit_id` geriye dönük doldurulmalı.
  Varsayılan bir "Genel" birimi açmak en ucuz yol, ama bu birimin başkanı
  atanana kadar eski kayıtlar ilerleyemez. Karar Faz 0b'den önce verilmeli.
- **Faz 4 isteğe bağlı değil.** Yapılmazsa arayüz kendi kopyasını taşımaya devam
  eder ve akış eklendiğinde backend ile ayrışır.

### Kapsam dışı

Model **doğrusal olmayan** akışları kapsamıyor: paralel onay (iki kişi aynı
anda), koşullu dallanma (tutar > X ise Başkana), zaman aşımıyla otomatik
ilerleme. Bunlar geçiş satırına koşul ifadesi ve adım tipine "birleşme" kavramı
eklemeyi gerektirir; ayrı bir ADR konusudur.

İç içe birim hiyerarşisi de kapsam dışıdır. Genişleme yolu ucuz bırakıldı:
hiyerarşi gerektiğinde `assignee_rule`'a `ROLE_IN_ANCESTOR_UNIT` gibi **yeni bir
değer** eklenir; mevcut satırlar değişmez. Model yeniden şekillendirilmez.

### Takip işleri

1. Mevcut kayıtların hangi birime atanacağına karar verilmesi (Faz 0b öncesi).
2. `BASKAN_YARDIMCISI` ve `BASKAN` rollerinin `GLOBAL` kalıp kalmayacağının
   teyidi — bu ADR ikisini de `GLOBAL` varsayıyor.
3. Daire Başkanının kayıt oluşturup oluşturamayacağı (`RECORD_CREATE` izni).
4. Akış doğrulayıcısının kabul kriterlerinin yazılması.

## Bağlantılar

- [ADR-0001 — Modül bazlı paketleme](0001-modul-bazli-paketleme.md)
- [İş akışı ve durum geçişleri](../workflow.md)
- [Veritabanı tasarımı](../database.md)
- [Sistem mimarisi](../architecture.md)
