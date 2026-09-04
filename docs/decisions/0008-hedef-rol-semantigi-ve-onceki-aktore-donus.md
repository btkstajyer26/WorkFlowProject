# ADR-0008: Hedef Rol Semantiği ve Önceki Aktöre Dönüş

- Durum: Önerildi
- Tarih: 2026-09-04
- Karar sahipleri: Burak (`WF`) · Alperen (`DB`)
- Kapsadığı bulgu: `B02` (P1)

## Bağlam

Dinamik bir departman rolü (`SATIN_ALMA_UZMANI` gibi) `BASKANA_ILET` yaptığında
kayıt Başkana ulaşır. Fakat Başkanın `BASKAN_YARDIMCISINA_GERI_GONDER` geçişi
`WORKFLOW_TARGET_ROLE_INVALID` ile reddedilir. Plandaki "Satın Alma uzmanı →
Başkan" örneğinin geri dönüş kolu tamamlanmaz.

Yüzeydeki sebep, `V15` seed'inin bu satırda `expected_target_role_id` olarak
yerleşik `BASKAN_YARDIMCISI` rolünü taşıması ve validator'ın 8–9. adımda çözülen
hedefin rolünü bu değere eşitlemesidir. Ancak asıl sorun bu tek satır değil,
kolonun **üç ayrı anlamı birden taşımasıdır**:

| # | Anlam | Nerede kullanılır | Hangi stratejiler için doğru |
|---|---|---|---|
| 1 | **Arama anahtarı** — hedef kullanıcı bu rolden bulunur | `TargetUserResolver.resolveSingleActiveRole` | Yalnız `ROLE` |
| 2 | **Çözüm sonrası doğrulama** — bulunan hedefin rolü bu olmalı | `WorkflowTransitionValidator` adım 8–9 | Yalnız `ROLE` için gereklidir |
| 3 | **"Bu geçiş hedef ister" işareti** — iki aşamalı doğrulamanın nöbetçisi | `WorkflowApplicationService.validatePreliminaryDecision` | Hiçbiri; `target_strategy` değerinden türetilebilir |

Üçüncü anlam kritiktir ve kolonu kaldırmayı doğrudan imkânsız kılar.
`WorkflowApplicationService` hedefi çözmeden **önce** bir ön doğrulama yapar ve bu
ön doğrulamanın `WORKFLOW_TARGET_ROLE_INVALID` ile reddedilmesini bekler; red
gelmezse `IllegalStateException` atar. Ön doğrulamada `targetRoleId` henüz
`null` olduğu için red **yalnız** `expected_target_role_id` doluysa oluşur.
`TransitionRule` bu yüzden kolonu `CREATOR` / `CURRENT_ASSIGNEE` /
`PREVIOUS_ACTOR` için **veritabanı CHECK'inden daha katı biçimde** zorunlu kılar:

```java
boolean targetRoleExpected = targetStrategy != NONE && targetStrategy != DEPARTMENT;
if (targetRoleExpected && expectedTargetRoleId == null) throw ...
```

Sonuç: kimliği çalışma zamanında belirlenen (`CREATOR`, `CURRENT_ASSIGNEE`,
`PREVIOUS_ACTOR`) stratejilerde, iş akışının kendisinin meşru biçimde belirlediği
bir kişiye **statik ve yerleşik** bir rol dayatılır. Dinamik rol tanımı bu
dayatmayı hiçbir zaman geçemez.

Bağlayıcı kısıtlar:

- `WorkflowTransitionValidator` DB/repository bağımlılığı almaz (V1 kırmızı çizgi 1).
- Rol adı workflow kimliği değildir; `RoleId` korunur (kırmızı çizgi 2).
- Mevcut sekiz yerleşik geçişin davranışı regresyonsuz kalır.
- `V15` düzenlenmez; değişiklik ileri migration ile gelir (DB-1 §13.1).
- Hedef rol kontrolünü koşulsuz kaldırmak **kabul edilebilir bir çözüm değildir**;
  kaldırılan güvenlik özelliğinin yerine ne konduğu gösterilmelidir.

## Değerlendirilen Seçenekler

### S1 — `last_deputy_id` kişi mi, kuyruk mu?

| Seçenek | Değerlendirme |
|---|---|
| **A. Kişi kalır** — geri dönüş, ileten kişiye gider | İleten kişi bağlamı taşır ve işi fiilen üstlenmiştir; first-action-wins zaten onu seçmiştir. `PREVIOUS_ACTOR` bugün de kişi anlamındadır. Ek şema gerekmez |
| B. Kuyruğa dönülür — geri dönüş, gönderen departmana gider | Kaydın hangi departmandan geldiğini hatırlamak için yeni kolon gerekir (`assigned_department_id` kişiye atandığında temizlenir). first-action-wins yarışını yeniden açar, hesap verebilirliği kaybeder |
| C. Yeni bir `PREVIOUS_QUEUE` stratejisi | B'nin maliyetine ek olarak yeni bir strateji primitive'i; V1 kapsamını genişletir, V2'de versioning ile birlikte ele alınması daha doğru |

**Seçilen: A.** Kolon kişi anlamını korur. Adının yanıltıcı olması ayrı bir
konudur (aşağıda K1).

### S2 — Kolonun üç anlamı nasıl ayrılır?

| Seçenek | Değerlendirme |
|---|---|
| A. Kolonu `NULL` yap, başka bir şey yapma | İki aşamalı doğrulamanın nöbetçisini kırar; `IllegalStateException` üretir. Tek başına uygulanamaz |
| B. `PREVIOUS_ACTOR` için rol kontrolünü `if` ile atla | Aynı hatayı `CREATOR` ve `CURRENT_ASSIGNEE` kollarında bırakır; kolonun üç anlamı yerinde durur ve bir sonraki dinamik rol senaryosunda tekrar kırılır. Özel durum ekleyerek kural kaynağını kirletir |
| **C. Üç anlamı üç mekanizmaya ayır** | (3) `target_strategy` değerinden türetilir; (1) yalnız `ROLE` kolunda kalır; (2) yerini role bağlı olmayan bir yetenek kontrolüne bırakır. Kolon tek anlamlı hâle gelir |

**Seçilen: C.**

### S3 — Kaldırılan statik rol kontrolünün yerine ne konur?

Statik kontrol aslında şu güvenlik özelliğini yaklaşık olarak sağlıyordu:
*kayıt, onunla hiçbir şey yapamayacak birine atanmasın.* Doğrudan kaldırılırsa bu
özellik kaybolur — raporun uyarısı tam olarak budur.

| Seçenek | Değerlendirme |
|---|---|
| A. Yalnız `is_active` kontrolü | Aktif ama iniş durumunda hiçbir aksiyonu olmayan kullanıcıya atama yapılabilir; kayıt ölü uçta kalır |
| **B. "Hedef iniş durumunda işlem yapabiliyor mu?" kontrolü** | Statik rol yerine yeteneği doğrular: rol-agnostiktir, dinamik rolleri geçirir, ölü uç atamasını engeller. Snapshot üzerinden saf hesaplanır, validator saf kalır |
| C. Yalnız hedefin permission listesine bak, geçişe bakma | Permission tek başına yetmez; aktör rol bağı ve `actor_requirement` de gerekir. Departman kolundaki `hasUsableRoutingInto` ile tutarsız olur |

**Seçilen: B.** Departman gönderiminde zaten uygulanan
`DepartmentRoutingResolver.hasUsableRoutingInto` ile **aynı şekildedir**; kişi kolu
ile departman kolu böylece tek bir "hedef gerçekten işleyebilir mi?" kuralında
buluşur.

### S4 — Önceki aktör artık işlem yapamıyorsa ne olur?

| Seçenek | Değerlendirme |
|---|---|
| A. Sessizce departman kuyruğuna düş | V1 §19.4.11 "uygun kullanıcı yoksa sessiz fallback yapılmaz" ile çelişir; kaydın nereye gittiği kullanıcıya görünmez |
| B. Sessizce yerleşik `BASKAN_YARDIMCISI` rolüne düş | Dinamik rolü yok sayar; B02'nin sebebini farklı biçimde geri getirir |
| **C. Ayrı hata koduyla reddet** | Davranış açık ve denetlenebilir. Kayıt mahsur kalmaz: Başkanın `CALISANA_GERI_GONDER` (`CREATOR`) kolu her zaman açıktır |

**Seçilen: C.**

## Karar

**K1 — `last_deputy_id` kişidir ve anlamı "son ileten aktör"dür.**
Departman kuyruğu değildir. Rolü ne olursa olsun `BASKANA_ILET` aksiyonunu
gerçekleştiren kullanıcıyı taşır; bugünkü yazma davranışı
(`action == BASKANA_ILET → actor.id()`) doğrudur ve değişmez. Kolonun adı
yerleşik role atıfta bulunduğu için yanıltıcıdır; **yeniden adlandırma Workflow
V2'ye bırakılır**, V1'de yalnız belgelenen anlam düzeltilir.

**K2 — `expected_target_role_id` yalnız `ROLE` stratejisinin arama anahtarıdır.**
Diğer stratejilerde `NULL` olur ve hiçbir doğrulamada okunmaz.

**K3 — "Bu geçiş hedef ister" bilgisi `target_strategy` değerinden türetilir.**
`TransitionContext` yeni bir `targetResolutionPending` alanı taşır. Ön doğrulama
bunu `true`, hedef çözüldükten sonraki nihai doğrulama `false` verir. Validator,
alan `true` iken hedefe bağlı 8–9. adımları çalıştırmaz ve iç nöbetçi reddini
üretir. `WORKFLOW_TARGET_ROLE_INVALID` artık nöbetçi olarak kullanılmaz; yalnız
gerçek rol uyuşmazlığında (`ROLE` stratejisi) döner.

**K4 — Kimliği çalışma zamanında belirlenen hedefler için yetenek kontrolü.**
`CREATOR`, `CURRENT_ASSIGNEE` ve `PREVIOUS_ACTOR` stratejilerinde çözülen hedef şu
üç koşulu birlikte sağlamalıdır:

1. `is_active = TRUE` (mevcut `WORKFLOW_TARGET_INACTIVE` korunur),
2. rolü aktif ve `is_workflow_actor = TRUE`,
3. **iniş durumunda en az bir işlem yapabiliyor:** aktif kural snapshot'ında
   `from = rule.to()` olan öyle bir geçiş vardır ki `actorRoleId` hedefin rolüne
   eşittir, hedef o geçişin `required_permission_code` değerine ve `RECORD_VIEW`
   iznine sahiptir, ve geçişin `actor_requirement` koşulu bu geçişin yaratacağı
   atama altında hedef için sağlanabilir
   (`isSatisfiedBy(hedef == createdBy, true)`).

Üçüncü koşul `hasUsableRoutingInto` ile aynı şekildedir. Hesap yalnız snapshot ve
hedefin permission kümesi üzerinden yapılır; **validator DB bağımlılığı almaz.**
`TransitionContext` bunun için `targetPermissionCodes` ve `targetWorkflowActor`
alanlarını da taşır; bu değerleri servis katmanı `WorkflowUserPort` üzerinden
doldurur ve `WorkflowUserSnapshot` genişletilir.

**K5 — Uygunsuz önceki aktör sessizce yönlendirilmez.**
K4 sağlanmazsa geçiş yeni `WORKFLOW_PREVIOUS_ACTOR_UNAVAILABLE` kodu ile
reddedilir (HTTP `409`). Departman kuyruğuna veya yerleşik role otomatik dönüş
yapılmaz. Kayıt mahsur kalmaz: Başkanın `CALISANA_GERI_GONDER` kolu `CREATOR`
stratejisiyle açık kalır.

**K6 — İleri migration `V24`.**
`V15` düzenlenmez. `V24`:

1. Üç satırda `expected_target_role_id` değerini `NULL` yapar — iki `CREATOR`
   (`CALISANA_GERI_GONDER` × 2) ve bir `PREVIOUS_ACTOR`
   (`BASKAN_YARDIMCISINA_GERI_GONDER`).
2. `chk_transition_target_strategy_role` kısıtını daraltır: `expected_target_role_id`
   **yalnız** `target_strategy = 'ROLE'` iken dolu olabilir.

`TransitionRule` içindeki Java invariant'ı bu kısıtın aynadaki karşılığına
çevrilir: dolu olması `ROLE` için zorunlu, diğerleri için yasak.

### Şema karşılığı

```sql
-- V24 (özet)
UPDATE workflow_transitions SET expected_target_role_id = NULL
 WHERE target_strategy IN ('CREATOR', 'CURRENT_ASSIGNEE', 'PREVIOUS_ACTOR');

ALTER TABLE workflow_transitions DROP CONSTRAINT chk_transition_target_strategy_role;
ALTER TABLE workflow_transitions ADD CONSTRAINT chk_transition_target_strategy_role CHECK (
    (target_strategy = 'ROLE' AND expected_target_role_id IS NOT NULL) OR
    (target_strategy <> 'ROLE' AND expected_target_role_id IS NULL)
);
```

### Yerleşik akışta ne değişir

Hiçbir şey. Yerleşik `BASKAN_YARDIMCISI` K4'ün üç koşulunu da sağlar: aktiftir,
workflow aktörüdür ve `BSK_YRD_INCELEMESINDE` durumundan `BASKANA_ILET` /
`CALISANA_GERI_GONDER` geçişlerinde aktördür. Regresyon testi bunu ayrıca
sabitler.

## Sonuçlar

**Olumlu**

- B02 kapanır; plandaki dinamik departman senaryosunun geri dönüş kolu tamamlanır.
- Aynı hata `CREATOR` ve `CURRENT_ASSIGNEE` kollarında da önlenir; düzeltme tek
  satırlık bir istisna değildir.
- Kişi ve departman kolları tek bir "hedef gerçekten işleyebilir mi?" kuralında
  buluşur; iki ayrı yetki mantığı kalmaz.
- `expected_target_role_id` tek anlamlı hâle gelir; kolonun ne olduğu şemadan
  okunabilir.
- Validator saflığı ve `RoleId` kimliği korunur.

**Olumsuz / maliyet**

- `TransitionContext`, `WorkflowUserSnapshot` ve `WorkflowUserPort` genişler; bu
  tiplere dokunan mevcut testler güncellenir.
- Yeni bir hata kodu istemci sözleşmesine girer.
- K4'ün üçüncü koşulu snapshot üzerinde bir tarama daha yapar. Kural sayısı küçük
  olduğu için ölçülen bir maliyet beklenmiyor; ölçüm yapılmadan "performans sorunu
  yok" denmez.
- `V24` uygulanmadan `TransitionRule` içindeki yeni invariant devreye alınırsa
  uygulama açılışta düşer. Migration ve kod aynı teslimde gider.

## Kabul kaydı

| Konu | Değer |
|---|---|
| Bulgu | `B02` (P1) |
| Sahip | Burak (`WF`) · Alperen (`DB`, `V24`) |
| Yeni migration | `V24` |
| Yeni hata kodu | `WORKFLOW_PREVIOUS_ACTOR_UNAVAILABLE` (`409`) |
| Değişen tipler | `TransitionContext`, `TransitionRule`, `WorkflowUserSnapshot`, `WorkflowUserPort`, `WorkflowTransitionValidator`, `WorkflowApplicationService` |
| Değişmeyen | Sekiz yerleşik geçişin davranışı, `RoleId` kimliği, validator saflığı, `GONDER` yolu |

**Kabul testleri**

1. Dinamik rol + permission + aktör bağı + departman üyeliği/routing kurulur; üye
   `BASKANA_ILET` yapar; Başkanın `BASKAN_YARDIMCISINA_GERI_GONDER` geçişi
   **başarılı olur** ve kayıt o kişiye atanır.
2. Aynı senaryoda önceki aktör pasifleştirilir → geçiş
   `WORKFLOW_PREVIOUS_ACTOR_UNAVAILABLE` ile reddedilir; kayıt durumu değişmez.
3. Aynı senaryoda önceki aktörün aktör rol bağı kaldırılır (K4.3 düşer) → aynı
   hata kodu.
4. Yerleşik `BASKAN_YARDIMCISI` akışının sekiz geçişi regresyonsuz çalışır.
5. `ROLE` stratejisinde yanlış roldeki hedef hâlâ `WORKFLOW_TARGET_ROLE_INVALID`
   alır; bu kod nöbetçi olarak hiçbir yolda üretilmez.
6. `V24` öncesi veriyle açılan uygulama, kısıt ve invariant uyumsuzluğunu açılışta
   görünür biçimde bildirir.

## Kapatılan sorular

- **Geri dönüş departmana mı yapılır?** Hayır; kişiye yapılır (S1-A).
- **Rol kontrolü tamamen kalkıyor mu?** Hayır; `ROLE` stratejisinde aynen kalır,
  diğerlerinde yerini yetenek kontrolüne bırakır (S3-B).
- **`last_deputy_id` yeniden adlandırılacak mı?** V1'de hayır; V2 adayıdır.
- **Uygun olmayan önceki aktörde otomatik yönlendirme var mı?** Yoktur (S4-C).

## Bağlantılar

- [ADR-0005](0005-departman-atamasi-ve-akis-kurali.md) — departman ataması ve akış kuralı
- [ADR-0006](0006-departman-hedefli-target-strategy.md) — `DEPARTMENT` stratejisi ve gönderim
- [ADR-0007](0007-rol-kapasitesi-ve-birim-tekilligi.md) — rol kapasitesi
- [DB-1 §7.2 / §8](../DB_1_VERI_MODELI_SOZLESMESI.md) — hedef çözüm primitive'leri ve geçiş tablosu
- [APP-9 / APP-10 / B11 sözleşmesi](../APP9_APP10_B11_ISTEMCI_SOZLESMESI.md) — kullanılabilir aksiyon, atama DTO'su ve alıcı çözümü
- [workflow.md](../workflow.md) — doğrulama sırası ve hata sözleşmesi
- [Tekrar üretim kanıtı](../reviews/2026-09-04/TEKRAR_URETIM.md) — B02'yi gösteren `dynamicDepartmentForwardMustSupportReturnToPreviousActor` probu ve gözlenen `WORKFLOW_TARGET_ROLE_INVALID` sonucu
