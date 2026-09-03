# ADR-0005: Departman Ataması ve Akış Kuralı

- Durum: Önerildi
- Tarih: 2026-09-03
- Karar sahipleri: Tamer (öneren) · Burak (`WF`) · Alperen (`DB`)

## Bağlam

Bugün bir kayıt tek bir **kişiye** atanıyor (`records.assigned_to`). Mentör isteği,
kaydın bir **departmana** gönderilebilmesi ve o departmandaki uygun kişinin işlem
yapabilmesi. Departman, projede hiç tasarlanmamış yeni bir domain kavramı.

Semantik yön ekipçe kararlaştırıldı: kayıt gönderilirken departmandan belirli bir
kişi seçilmişse işlemi o kişi yapar; seçilmemişse departmanın önceden tanımlı
akışı kimi işaret ediyorsa o yapar. Bu ADR o yönün **veri şeklini** ve **durum
makinesine yansımasını** sabitler.

Kararı üç kulvar birden bekliyor: Alperen `department_members` ve akış kuralı
tablosunu (`DB-12`), Tamer akış kuralı editörünü (`AP-5`) ve çözümleyiciyi
(`WF-6`), Bahadır departmana fan-out'u (`NT-5`) buna göre yazacak. Karar tek yerde
yazılı olmazsa üçü ayrı ayrı doğru, birbirine göre yanlış kod yazar.

Bağlayıcı kısıtlar:

- `WorkflowTransitionValidator` saf Java kalır; Spring, JPA ve repository
  validator'a giremez (`SM-5` ile kazanılan altyapısız test edilebilirlik).
- Validator'ın dokuz kontrolünün **sırası ve hata kodları** değişmez; negatif
  testler tam hata kodu assert ediyor.
- Mevcut kişiye-atama davranışı birebir korunur; backend test eşiği (bugün 639)
  düşmez.

## Değerlendirilen Seçenekler

### S1 — Aktör ilişkisi: `ActorRequirement` genişletilsin mi?

**Seçenek A — Enum'a yeni değer eklenir** (ör. `DEPARTMENT_MEMBER`).

Artı: "aktörün kayıtla ilişkisi" kavramı veride açıkça görünür; geçiş bazında
departman şartı tanımlanabilir.

Eksi: `DB-1` §6.6 `actor_requirement` için bağlayıcı bir CHECK tanımlıyor
(`CREATOR` · `ASSIGNEE` · `CREATOR_AND_ASSIGNEE`). Yeni değer, kabul edilmiş bir
sözleşmeyi, uygulanmış `V15` seed'ini ve parite testinin referansını birlikte
değiştirmeyi gerektirir. Sekiz geçişin davranışı değişmediği hâlde üç dosya
birden oynar.

**Seçenek B — `TransitionContext.actorIsAssignee` hesaplaması genişletilir.**

Artı: Enum, CHECK, seed ve parite testi hiç değişmez. Kişiye atanmış kayıtta
boolean bugünküyle aynı şekilde hesaplanır, bu da mevcut davranışın korunduğunu
kanıtlamayı kolaylaştırır. Validator'ın public sözleşmesi ve hata kodu sırası
aynı kalır; `WORKFLOW_FORBIDDEN` yine dördüncü adımdan döner.

Eksi: Boolean iki anlamı birden taşır — doğrudan atanan **veya** departman
üzerinden yetkili. Ayrım veride görünmez hâle gelir.

### S2 — Akış kuralının taneciği

1. `(departman, kaynak durum, aksiyon)` — `workflow_transitions` ile aynı tanecik.
2. `(departman, kaynak durum)` — daha az satır, ama aynı durumdan çıkan iki farklı
   aksiyonu ayıramaz.
3. `(departman, adım sırası)` — geçiş grafiği dallandığı anda anlamsızlaşır.

### S3 — Kural kimi işaret eder?

1. Departman içi bir **rol**.
2. Belirli **kişiler** — personel değiştiğinde kural sessizce bozulur ve kişi
   seçimi zaten gönderim anında yapılabildiği için aynı şeyi iki yerde tanımlar.

### S4 — Üyelikteki rol

1. Global `users.role_id` kullanılır.
2. Üyelik kapsamlı ayrı bir rol tutulur — `DB-1` §10'daki "bir kullanıcı = bir ana
   rol" kararıyla ve `user_roles` yasağıyla çelişir.

## Karar

**S1 → Seçenek B.** `ActorRequirement` bu iterasyonda genişlemez. Departman desteği
dördüncü kontrolün *içine* girer, yeni bir adım olarak araya girmez.
`actorIsAssignee` şu soruya cevap verir hâle gelir:

```text
aktör kaydın assigned_to'su mu
  VEYA
kayıt bir departmana atanmışsa, aktör o departmanın üyesi ve akış kuralının
bu adımda işaret ettiği role sahip mi?
```

**S2 → Seçenek 1.** Kural `(departman, kaynak durum, aksiyon)` taneciğinde tutulur.
Bugünkü grafikte aktör rolü kaynak duruma göre zaten tekil, yani aksiyon boyutu şu
an ayırt edici değil; ancak bu grafiğin bir tesadüfü. Aksiyon boyutunu şimdi
eklemenin maliyeti seed'de birkaç satır, sonradan eklemenin maliyeti şema
değişikliği.

**S3 → Seçenek 1.** Kural bir rol işaret eder (`target_role_id`).

**S4 → Seçenek 1.** `department_members` yalnız `(department_id, user_id)` tutar;
"bu kişi bu adımda yetkili mi" sorusu `users.role_id` ile kuralın rolü
karşılaştırılarak cevaplanır.

Bunlara ek olarak:

- **Kişi seçilmişse o kazanır.** Departmandan belirli bir kişi seçilmişse kayıt o
  kişiye atanır, akış kuralına hiç bakılmaz.
- **Uygun herkes görür, ilk işlem yapan kilitler.** Ayrı bir üstlenme (claim)
  mekanizması yoktur; eşzamanlılık mevcut optimistic locking ile çözülür.
- **Uygun kimse yoksa sessizce geçilmez.** Kural bulunamazsa veya işaret edilen
  rolde aktif üye yoksa servis katmanı `WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED`
  ile durdurur. Mevcut `WORKFLOW_ROLE_NOT_CONFIGURED` deseniyle aynı yerde çalışır.
- **Departmandan kişiye devir yoktur** — bu iterasyonun kapsamı dışında.

### Veri şekli

`department_routing_rules`:

| Kolon | Tip | Null | Sözleşme |
| --- | --- | --- | --- |
| `id` | `INTEGER` | hayır | PK |
| `department_id` | `INTEGER` | hayır | FK → `departments.id` |
| `from_status_id` | `INTEGER` | hayır | FK → `workflow_statuses.id` |
| `action_id` | `INTEGER` | hayır | FK → `workflow_actions.id` |
| `target_role_id` | `INTEGER` | hayır | FK → `roles.id` |
| `is_active` | `BOOLEAN` | hayır | varsayılan `TRUE` |

```text
PRIMARY KEY (id)
UNIQUE (department_id, from_status_id, action_id)
FOREIGN KEY (...) ... ON DELETE RESTRICT
```

`UNIQUE` kısıtı `workflow_transitions`'daki
`UNIQUE (from_status_id, action_id, actor_role_id)` ile aynı mantıkta: bir departman
için bir adımda tek kural.

### `records` atama sözleşmesi

Atama hedefi mevcut `assigned_to` kolonu (kişi) **veya** yeni
`assigned_department_id` (departman) olur. Kural üç kademelidir:

1. **İkisi birden dolu olamaz.** Bu, satır içi `CHECK` ile DB seviyesinde
   zorlanır:

   ```sql
   CHECK (assigned_to IS NULL OR assigned_department_id IS NULL)
   ```

2. **Atama gerektiren durumda biri dolu olmalıdır.** Bugün bu küme
   `BSK_YRD_INCELEMESINDE`, `BASKAN_INCELEMESINDE` ve `DUZENLEME_BEKLIYOR`.
3. **Atama gerektirmeyen durumda ikisi de `NULL` olabilir.** Bugün bu küme
   `TASLAK` (kayıt oluşturulduğu an) ile terminal `ONAYLANDI` ve `REDDEDILDI`.

> ⚠️ **2. madde `CHECK` ile zorlanamaz.** Satır içi `CHECK` başka tabloya
> (`workflow_statuses`) bakamaz ve "atama gerektiren durum" bilgisi bugün veride
> yok: `workflow_statuses` yalnız `is_terminal`, `is_editable_by_creator`,
> `display_order` ve `is_active` taşıyor (`V14`). `is_terminal` da bu bilgiyi
> türetmeye yetmez — `TASLAK` terminal değildir ama atamasızdır.
>
> Bu nedenle 2. madde **uygulama servisinde**, atamayı yazan transaction içinde
> doğrulanır. DB-1 §6.1'in `max_users` için verdiği kararla aynı kalıptır:
> genel bir `CHECK` ile doğrulanamayan invariant, yazma yolunda kilitli biçimde
> korunur. Alternatifi — `workflow_statuses.requires_assignment` kolonu ve
> trigger — bu iterasyonda **seçilmedi**; gerekirse ayrı karar konusudur.

Yalnız 1. madde DDL'e girer; `DB-11`/`DB-12`/`DB-13` bu ayrımı esas alır.

**Neden "tam olarak biri dolu" değil.** Önceki taslak global bir XOR öneriyordu.
Bu kısıt bugünkü davranışla çelişiyordu: yeni kayıt `TASLAK` durumunda ve
`assigned_to = NULL` ile oluşuyor
(`DynamicWorkflowRoleIntegrationTest`), `ONAYLA` ve `REDDET` ise
`target_strategy = NONE` ile atamayı `NULL`'a çekiyor (`V15`). Global XOR konsaydı
hiçbir kayıt oluşturulamazdı ve yukarıdaki "mevcut kişiye-atama davranışı birebir
korunur" kısıtı çiğnenirdi.

### Çözümün yeri

`DepartmentRoutingResolver` servis katmanında, `TransitionContext` **doldurulurken**
çalışır — validator'ın içinde değil. Validator'a giren şey yine sadece boolean olur.

## Sonuçlar

Olumlu:

- `ActorRequirement`, `workflow_transitions` seed'i ve validator'ın kontrol sırası
  değişmez; mevcut regresyon testleri korunur.
- Akış kuralı tablosu geçiş tablosuyla aynı tanecikte durduğu için panel editörü
  (`AP-5`) `(durum, aksiyon)` çiftlerini doğrudan `workflow_transitions`'tan
  okuyup listeleyebilir.
- Departman yetkisi rol üzerinden çözüldüğü için personel değişimi kuralı bozmaz.

Maliyet ve riskler:

- `actorIsAssignee` iki anlamı birden taşır. Ayrımın `TransitionContext`'te ayrı
  bir alanla görünür kılınıp kılınmayacağı `WF-5` sırasında yeniden
  değerlendirilmelidir.
- `recipientsOf` (`NT-5`) departmana atanmış kayıtta tek kişi değil, kuralın
  işaret ettiği roldeki aktif departman üyeleri kümesini döndürmek zorunda. Önceki
  plandaki "dokunmana gerek yok" notu departmanlarla geçersiz.
- Kural satırı olmayan departman kullanılamaz hâle gelir. `AP-5` editörü,
  departman oluşturulurken kural tanımlanmasını teşvik etmeli.

Takip işleri:

- `DB-11` · `DB-12` · `DB-13` — tablolar ve karşılıklı dışlama `CHECK`'i
  (atama sözleşmesi 1. madde). 2. maddenin uygulama servisinde nereye
  yerleşeceği `WF-5` ile birlikte kararlaştırılır
- `WF-5` — `TransitionContext` genişlemesi
- `WF-6` — `DepartmentRoutingResolver`
- `AP-4` · `AP-5` — departman ekranları ve kural editörü
- `NT-5` — departmana fan-out

Kapsam dışı: departmanlar arası devir ve vekâlet, üyelik kapsamlı roller,
departman hedefli `target_strategy` değerleri (`DEPARTMENT`, `DEPARTMENT_ROLE`,
`PARENT_DEPARTMENT`, `EXPLICIT_USER` — `DB-1` §7.2 bunları bilerek dondurmuş),
workflow versioning.

## Açık sorular

- `TransitionContext`'e "departman üzerinden yetkili" ayrımını görünür kılan ayrı
  bir alan eklenecek mi? (`WF-5`)
- Bir kullanıcı birden fazla departmana üye olabilir mi? (`DB-12`)
- Departmanın üst departman referansı bu iterasyonda kullanılacak mı, yoksa yalnız
  kolon olarak mı açılacak? (`DB-11`)

## Bağlantılar

- [`DB_1_VERI_MODELI_SOZLESMESI.md`](../DB_1_VERI_MODELI_SOZLESMESI.md) — §6.6
  `actor_requirement` CHECK'i, §7.2 target strategy, §10 tek rol kararı, §15
  ertelenen departman kararları
- [`workflow.md`](../workflow.md) — mevcut workflow davranışı
- `workflow/statemachine/WorkflowTransitionValidator.java` — dördüncü kontrol
- `workflow/statemachine/TransitionContext.java` — `actorIsAssignee`
