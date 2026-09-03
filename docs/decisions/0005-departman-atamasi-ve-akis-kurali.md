# ADR-0005: Departman Ataması ve Akış Kuralı

- Durum: Kabul Edildi
- Tarih: 2026-09-03
- Karar sahipleri: Tamer (öneren) · Burak (`WF`) · Alperen (`DB`)

## Bağlam

Bugün bir kayıt tek bir **kişiye** atanıyor (`records.assigned_to`). Mentör isteği,
kaydın bir **departmana** gönderilebilmesi ve o departmandaki uygun kişinin işlem
yapabilmesi. Departman, projede hiç tasarlanmamış yeni bir domain kavramı.

Semantik yön ekipçe kararlaştırıldı: kayıt gönderilirken departmandan belirli bir
kişi seçilmişse işlemi o kişi yapar; seçilmemişse departmanın önceden tanımlı
akışı kimi işaret ediyorsa o yapar. Bu ADR o yönün **hangi seçeneklerle** hayata
geçtiğini sabitler. Kararların veri şekli `DB-1` §15'tedir; burada tekrarlanmaz.

Karar dört kulvarı birden ilgilendiriyor: Alperen `department_members` ve akış
kuralı tablosunu (`DB-12`), Burak çözümleyiciyi (`WF-6`), Tamer akış kuralı
editörünü (`AP-5`), Bahadır departmana fan-out'u (`NT-5`) buna göre yazacak. `DB-1` §15 de
bu kararlar verilmeden departman DDL'inin bağlayıcı olmayacağını söylüyordu.

Bağlayıcı kısıtlar:

- `WorkflowTransitionValidator` saf Java kalır; Spring, JPA ve repository
  validator'a giremez (`SM-5` ile kazanılan altyapısız test edilebilirlik).
- Validator'ın dokuz kontrolünün **sırası ve hata kodları** değişmez; negatif
  testler tam hata kodu assert ediyor.
- Mevcut kişiye-atama davranışı birebir korunur; backend test eşiği düşmez.
  ADR yazılırken eşik 639'du; `TZ-1` (PR #64) sonrası **646**'dır — bağlayıcı olan
  o günün sayısı değil, kapsamın azaltılmamasıdır.

## Değerlendirilen Seçenekler

### S1 — Aktör ilişkisi: `ActorRequirement` genişletilsin mi?

**A — Enum'a yeni değer eklenir** (ör. `DEPARTMENT_MEMBER`). Kavram veride açıkça
görünür, geçiş bazında departman şartı tanımlanabilir. Ama `DB-1` §6.6
`actor_requirement` için bağlayıcı bir CHECK tanımlıyor; yeni değer kabul edilmiş
bir sözleşmeyi, uygulanmış `V15` seed'ini ve parite testinin referansını birlikte
değiştirmeyi gerektirir. Sekiz geçişin davranışı değişmediği hâlde üç dosya oynar.

**B — `TransitionContext.actorIsAssignee` hesaplaması genişletilir.** Enum, CHECK,
seed ve parite testi hiç değişmez; kişiye atanmış kayıtta boolean bugünküyle aynı
hesaplanır. Bedeli: boolean iki anlamı birden taşır — doğrudan atanan **veya**
departman üzerinden yetkili.

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

### S5 — Departman üzerinden yetki `TransitionContext`'te görünür kılınsın mı?

**A — Ayrı bir alan eklenir** (ör. `actorAuthorizedViaDepartment`). Yetkinin
kaynağı tipin kendisinde okunur. Ama validator bu alanı **okumaz**: `S1` gereği
dördüncü kontrolün davranışı sabit. Tipin javadoc'u onu "doğrulama için gereken
bütün girdiler" diye tanımlıyor; okunmayan bir bileşen bu sözleşmeyi bozar ve
kayıt pozisyonel kurulduğu için yedi kurulum noktası (beşi test) boşuna değişir.

**B — Alan eklenmez; ayrım servis katmanındaki resolver dönüşünde durur.** Tip
karar girdisi olarak kalır; yetkinin kaynağına ihtiyaç duyan iki tüketici de
(audit yazımı, `NT-5` fan-out'u) zaten resolver sonucunun elde olduğu yerde
çalışıyor. Bedeli: iki anlam `TransitionContext` imzasında hâlâ görünmez.

### S6 — Bir kullanıcı kaç departmana üye olabilir?

1. **Birden fazla** — `user_id` üzerinde ek `UNIQUE` yok; tablonun `S4` ile
   kabul edilen şekli zaten N:N.
2. **Tek** — `UNIQUE (user_id)`. Şema 1:N olur ama tablo N:N gibi görünmeye devam
   eder; kısıt sonradan gevşetilebilir, sıkılaştırılamaz.

Çözüm yönü tek yönlü: kayıt → `assigned_department_id` → kural → rol → o roldeki
aktif üyeler. Kullanıcıdan departmana doğru bir arama yok, yani "bu kullanıcının
departmanı" diye tekil bir soru hiçbir yolda sorulmuyor; `users.role_id` global
olduğu için çoklu üyelik iki farklı rol de üretmiyor.

### S7 — `departments.parent_department_id` bu iterasyonda

1. **Kolon açılır, davranış bağlanmaz.**
2. **Kolon açılmaz** — `DB-1` §15 çekirdek şekli kabul etmişti; kolonu sonraya
   bırakmak departman kurulurken girilebilecek veriyi ikinci turda toplamayı
   gerektirir.
3. **Kolon açılır ve çözümlemede yukarı yürünür** (eskalasyon) — `PARENT_DEPARTMENT`
   `DB-1` §7.2 ile bilerek dondurulduğu için o dondurmayı çözmek gerekir.

### S8 — "Atama gerektiren durumda hedef bulunmalı" kuralı uygulamada nereye yerleşir?

1. **`RecordStatus` enum'ına `requiresAssignment` bayrağı.** Enum bugün de
   `is_terminal` ve `is_editable_by_creator`'ı taşıyor, ama o iki bayrağın `V14`'te
   karşılığı var; karşılığı olmayan üçüncüsü yalnız Java'da duran yeni bir
   doğruluk kaynağı olurdu.
2. **`workflow_statuses.requires_assignment` kolonu + trigger.** Statik veriyi
   trigger'la korumak bu projede başka hiçbir yerde kullanılmıyor.
3. **Geçişin `target_strategy` değerinden türetilir.**
   `WorkflowApplicationService.requiresTargetUser(rule)` bunu bugün zaten
   hesaplıyor (`target_strategy != NONE`) ve `resolvedTarget(...)` hedef
   çözülemediğinde `WORKFLOW_ROLE_NOT_CONFIGURED` ile durduruyor.

## Karar

**S1 → B.** `ActorRequirement` bu iterasyonda genişlemez. Departman desteği
dördüncü kontrolün *içine* girer, yeni bir adım olarak araya girmez.
`actorHoldsAssignment` (aşağıda `S5` ile yeniden adlandırılan alan) şu soruya
cevap verir hâle gelir:

```text
aktör kaydın assigned_to'su mu
  VEYA
kayıt bir departmana atanmışsa, aktör o departmanın üyesi ve akış kuralının
bu adımda işaret ettiği role sahip mi?
```

**S2 → 1.** Kural `(departman, kaynak durum, aksiyon)` taneciğinde tutulur.
Bugünkü grafikte aktör rolü kaynak duruma göre zaten tekil, yani aksiyon boyutu şu
an ayırt edici değil; ancak bu grafiğin bir tesadüfü. Aksiyon boyutunu şimdi
eklemenin maliyeti seed'de birkaç satır, sonradan eklemenin maliyeti şema
değişikliği.

**S3 → 1.** Kural bir rol işaret eder (`target_role_id`).

**S4 → 1.** Üyelik kendi rolünü taşımaz; "bu kişi bu adımda yetkili mi" sorusu
`users.role_id` ile kuralın rolü karşılaştırılarak cevaplanır.

**S5 → B.** `TransitionContext`'e yeni alan eklenmez. Ayrım
`DepartmentRoutingResolver`'ın dönüş tipinde görünür kılınır; tip mevcut
`TargetResolution` desenini izler:

```text
DepartmentRoutingResolution
  NotDepartmentAssigned                      -> kayıt kişiye atanmış; bugünkü yol
  Resolved(targetRoleId, eligibleUserIds)    -> kural bulundu, roldeki aktif üyeler
  RuleNotConfigured(departmentId, fromStatusId, actionId)
  NoEligibleMember(departmentId, targetRoleId)
```

Boolean'ın iki anlamı en azından dürüst bir isme çekilir:
`TransitionContext.actorIsAssignee` → **`actorHoldsAssignment`**. Bileşen her yerde
pozisyonel kurulduğu için beş test kurulum noktası değişmez; rename yalnız
`TransitionContext`, `WorkflowTransitionValidator` ve `ActorRequirement`'ın
parametre adını etkiler. Audit ve `NT-5` yetkinin kaynağını
`DepartmentRoutingResolution`'dan okur.

**S6 → 1.** Çoklu üyelik açıktır. Karşılığında iki yer çoğulu varsaymak zorundadır:
`NT-5` alıcı kümesini `user_id` ile tekilleştirir, `DB-8` görünürlük sorguları
`assigned_department_id = ?` değil `IN (:aktörün departmanları)` kurar.

**S7 → 1.** `parent_department_id` yalnız kolon olarak açılır; `WF-6` hiçbir
koşulda yukarı yürümez. Kendine referans satır içi `CHECK` ile, daha uzun döngüler
parent'ı yazan uygulama servisinde ata zinciri yürünerek engellenir (`DB-1` §6.1
`max_users` kalıbı). `AP-4` ağacı gösterip düzenleyebilir.

**S8 → 3.** Kural yeni bir mekanizma gerektirmiyor: beklenti statüden değil
geçişin `target_strategy` değerinden türetilir ve bugünkü
`requiresTargetUser`/`resolvedTarget` yoluna departman dalı eklenir. Karşılıklı
dışlama ayrıca `WorkflowRecordUpdate`'in compact constructor'ında doğrulanır —
komut saf Java ve atamayı yazan tek yol olduğu için DB `CHECK`'iyle aynı kural
altyapısız test edilebilir hâle gelir. `RecordStatus`'a bayrak,
`workflow_statuses`'a kolon eklenmez.

Statü seviyesindeki garanti — "atama isteyen bir statüye yalnız `NONE` olmayan
geçişlerle girilebilir" — geçiş grafiğinin özelliğidir. **Workflow V1'de grafik
topolojisi sabittir ve Admin onu değiştiremez**, dolayısıyla garantiyi seed'in
kendisi ve parite/invariant testleri taşır. Publish-time doğrulayıcıya (`DB-1` §14
workflow designer sınırı) devri Workflow V2 işidir.

Bunlara ek olarak:

- **Kişi seçilmişse o kazanır.** Departmandan belirli bir kişi seçilmişse kayıt o
  kişiye atanır, akış kuralına hiç bakılmaz.
- **Uygun herkes görür, ilk işlem yapan kilitler.** Ayrı bir üstlenme (claim)
  mekanizması yoktur; eşzamanlılık mevcut optimistic locking ile çözülür.
- **Uygun kimse yoksa sessizce geçilmez.** Kural bulunamazsa veya işaret edilen
  rolde aktif üye yoksa servis katmanı `WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED`
  ile durdurur. Mevcut `WORKFLOW_ROLE_NOT_CONFIGURED` deseniyle aynı yerde çalışır.
- **Atama kısıtı global XOR değildir.** `assigned_to` ve `assigned_department_id`
  aynı anda dolu olamaz, ama ikisi birden `NULL` olabilir: yeni kayıt `TASLAK`
  durumunda atamasız oluşur ve `ONAYLA`/`REDDET` atamayı `NULL`'a çeker. Global XOR
  konsaydı hiçbir kayıt oluşturulamazdı.
- **Departmandan kişiye devir yoktur** — bu iterasyonun kapsamı dışında.

Bu kararların tablo, kolon ve kısıt karşılığı **`DB-1` §15**'tedir; burada
tekrarlanmaz, böylece şemanın tek doğruluk kaynağı DB-1 olarak kalır.

### Çözümün yeri

`DepartmentRoutingResolver` servis katmanında, `TransitionContext` **doldurulurken**
çalışır — validator'ın içinde değil. Validator'a giren şey yine sadece boolean olur.

### Departmana atamayı kim yazar

Bu ADR departman atamasının **okuma ve yetki** tarafını sabitliyor. Kaydı bir
departmana **yazan** gönderim yolu kapsam dışında: bir geçişin departmanı hedef
göstermesi `target_strategy`'de `DEPARTMENT` / `DEPARTMENT_ROLE` değerlerini
gerektirir; `DB-1` §7.2 ve `TargetStrategy` javadoc'u bunları bilerek dondurdu ve
dondurmayı çözmek kabul edilmiş bir sözleşmeyi, `chk_transition_target_strategy`
kısıtını, `V15` seed'ini ve parite testini birlikte değiştirmek demek — `S1`'de
aynı gerekçeyle reddedilen hamlenin daha büyüğü.

Sonuç: `assigned_department_id`'yi bu ADR'nin kendi kapsamında hiçbir çalışma
zamanı yolu yazmaz. `WF-6` çözümleyicisi, `DB-13` seed'i veya repository ile
yazılmış departman atamalarına karşı test edilebilir; uçtan uca gönderim akışı
**ADR-0006** (departman hedefli `target_strategy` ve gönderim sözleşmesi) ile
karara bağlanır. Bu ayrım bilinçlidir: `DB-11`/`DB-12`/`DB-13`, `WF-5`, `WF-6`,
`AP-4`/`AP-5` ve `NT-5` bu ADR ile paralel yürüyebilir; yalnız son gönderim adımı
ADR-0006'yı bekler.

> **Zamanlama güncellemesi (4 Eylül 2026).** Bu ADR yazıldığında gönderim yolu
> "sonraki iterasyon" idi. Plan güncellemesiyle departman uygulaması **10 Eylül
> Workflow V1 teslimine alındı**; ADR-0006 de aynı teslimin kritik yolundadır.
> Yani gönderim yolu ayrı bir iterasyona değil, bu ADR ile **paralel yürüyen bir
> V1 iş kalemine** ertelenmiştir. Sıra korunur (yazma yolu ADR-0006 kararını
> bekler), takvim ayrışmaz.

## Sonuçlar

Olumlu:

- `ActorRequirement`, `workflow_transitions` seed'i ve validator'ın kontrol sırası
  değişmez; mevcut regresyon testleri korunur.
- Akış kuralı geçiş tablosuyla aynı tanecikte durduğu için `AP-5` editörü
  `(durum, aksiyon)` çiftlerini doğrudan `workflow_transitions`'tan okuyabilir.
- Departman yetkisi rol üzerinden çözüldüğü için personel değişimi kuralı bozmaz.
- `S8` yeni şema, kolon veya enum bayrağı getirmiyor; invariant bugünkü yolda ve
  tek yazma komutunda toplanıyor.
- `S5` rename'i pozisyonel kayıt kurulumunu bozmadığı için test dosyalarına
  dokunmaz.

Maliyet ve riskler:

- `actorHoldsAssignment` iki anlamı birden taşımaya devam eder; validator'ın reddi
  (`WORKFLOW_FORBIDDEN`) hangi anlamın sağlanmadığını söylemez, sebep audit
  kaydından okunur.
- `recipientsOf` (`NT-5`) departmana atanmış kayıtta tek kişi değil, kuralın
  işaret ettiği roldeki aktif üyeler kümesini döndürmek zorunda; çoklu üyelik
  nedeniyle küme `user_id` ile tekilleştirilmelidir. Önceki plandaki "dokunmana
  gerek yok" notu departmanlarla geçersiz.
- `DB-8` görünürlük sorguları tekil departman varsayamaz.
- Kural satırı olmayan departman kullanılamaz hâle gelir; `AP-5` editörü departman
  oluşturulurken kural tanımlanmasını teşvik etmeli.
- `parent_department_id` davranışsız açıldığı için veri girilir ama hiçbir
  çözümleme yolu onu doğrulamaz; döngü kontrolü yazma yolunda unutulursa `AP-4`
  ağacı sonsuz dallanabilir.
- Gönderim yolu ADR-0006'ya kaldığı için `WF-6` bu ADR tek başına merge edildiğinde
  üretimde tetiklenen bir yol değildir; ölü kalmaması testlerle ve `AP-5` editörüyle
  güvenceye alınır. ADR-0006 aynı V1 teslimine alındığından bu boşluk 10 Eylül
  kabulünde kapanmalıdır — `WF-6`'nın üretimde tetiklenmemesi V1 çıkışında kabul
  edilebilir bir son durum değildir.

Takip işleri:

- `ADR-0006` — departman hedefli `target_strategy` ve gönderim sözleşmesi
- `DB-11` · `DB-12` · `DB-13` — `DB-1` §15'teki şeklin migration'ları
- `WF-5` — `actorIsAssignee` → `actorHoldsAssignment` rename'i ve javadoc
- `WF-6` — `DepartmentRoutingResolver` ve `DepartmentRoutingResolution`;
  `WorkflowRecordUpdate`'te karşılıklı dışlama doğrulaması
- `AP-4` · `AP-5` — departman ekranları (parent döngü kontrolü) ve kural editörü
- `NT-5` — departmana fan-out, `user_id` ile tekilleştirme

Kapsam dışı: departmanlar arası devir ve vekâlet, üyelik kapsamlı roller,
departman hedefli `target_strategy` değerleri (ADR-0006), hiyerarşi üzerinden
eskalasyon, workflow versioning.

## Kapatılan sorular

| Soru | Cevap |
| --- | --- |
| `TransitionContext`'e departman ayrımını görünür kılan ayrı bir alan eklenecek mi? (`WF-5`) | Hayır — `S5 → B` |
| Bir kullanıcı birden fazla departmana üye olabilir mi? (`DB-12`) | Evet — `S6 → 1` |
| Üst departman referansı kullanılacak mı, yoksa yalnız kolon olarak mı açılacak? (`DB-11`) | Yalnız kolon — `S7 → 1` |
| Atama gerektiren durumun doğrulaması uygulamada nereye yerleşir? (`WF-5`) | Geçişin `target_strategy` değerinden türetilir — `S8 → 3` |

## Bağlantılar

- [`DB_1_VERI_MODELI_SOZLESMESI.md`](../DB_1_VERI_MODELI_SOZLESMESI.md) — §6.6
  `actor_requirement` CHECK'i, §7.2 target strategy, §10 tek rol kararı, §14
  workflow designer sınırı, **§15 bu kararların veri şekli**
- [`workflow.md`](../workflow.md) — mevcut workflow davranışı
- `workflow/statemachine/WorkflowTransitionValidator.java` — dördüncü kontrol
- `workflow/statemachine/TransitionContext.java` — `actorHoldsAssignment`
  (`WF-5` ile `actorIsAssignee`'den yeniden adlandırıldı)
- `workflow/statemachine/TargetStrategy.java` — dondurulmuş departman değerleri
- `workflow/service/WorkflowApplicationService.java` — `requiresTargetUser`,
  `resolvedTarget` (`S8`)
- `workflow/model/TargetResolution.java` — `DepartmentRoutingResolution`'ın deseni
- `workflow/model/WorkflowRecordUpdate.java` — atamayı yazan tek komut
