# ADR-0009: Workflow Audit'inde Atama Sözleşmesi

- Durum: Kabul Edildi
- Tarih: 2026-09-08 · uygulandı 8 Eylül 2026 (`V25` + `B12`)
- Karar sahipleri: Burak (`WF`) · Alperen (`DB`)
- Kapsadığı bulgu: `B12` (P2)

## Bağlam

Bir workflow geçişi kaydın atamasını değiştirir: kişiye, departmana veya hiç
kimseye. Bu değişimin **kalıcı geçmişte** okunabilir olması gerekir; "bu evrak
daha önce hangi departmana gönderilmişti?" sorusunun cevabı kaydın **o anki**
durumundan değil, geçmişten gelmelidir.

Bugün gelmiyor. Üç ayrı katman aynı bilgiyi sırayla düşürüyor:

| Katman | Durum |
|---|---|
| `WorkflowApplicationService.performAction` | `assignedDepartmentId`'yi hesaplıyor, `WorkflowRecordUpdate`'e ve event'e veriyor — **audit record'una vermiyor** |
| `WorkflowTransitionAudit` | `assignedTo` alanını taşıyor; **departman alanı ve önceki atama yok** |
| `AuditLogService.record` | Dokuz alanın sekizini map'liyor; **`audit.assignedTo()`'yu hiç okumuyor** |
| `audit_logs` tablosu | Atama kolonu **yok** (`V1` + `V7` dışında tabloya dokunulmamış) |

Sonuç: kişi ataması modele girip persistence'ta sessizce kayboluyor, departman
ataması modele hiç girmiyor. `AuditLogServiceTest.mapsEveryTransitionFieldOntoTheRow`
adına rağmen `assignedTo` için assertion taşımadığı için kayıp testlerce de
görülmüyordu.

Serbest metne güvenilemez: departmana gönderimde `comment` isteğe bağlıdır.

## Değerlendirilen Seçenekler

### S1 — Alan şekli

**A) Dört kolon, tür türetilir.** `previous_assigned_to` / `previous_assigned_department_id`
ve `new_assigned_to` / `new_assigned_department_id`. Atama türü
(`USER`/`DEPARTMENT`/`NONE`) okuma anında `AssignmentView.of(UUID, Integer)` ile
türetilir.

**B) Altı kolon, tür açıkça yazılır.** A'nın üstüne `previous_assignment_kind` ve
`new_assignment_kind` (`VARCHAR`), artı tür↔kimlik tutarlılık CHECK'i. Append-only
satır kendi kendini anlatır ve yazıldığı andaki yorumu dondurur.

**C) Yalnız yeni atama (iki kolon).** En küçük değişiklik; ama ardışık geçişlerde
"önceki gönderim nereye yapılmıştı" sorusu yine cevapsız kalır — bulgunun asıl
şikâyeti karşılanmaz.

### S2 — Okuma tarafı açılsın mı?

**A) Yalnız yazma.** Kolonlar dolar, hiçbir uç döndürmez.
**B) Yazma + okuma.** `AuditLogResponse` atama bilgisini de taşır.

### S3 — Transaction sınırı

**A) Değişmez.** Audit yazımı `WorkflowActionService.performAction`'ın açtığı
transaction'a katılmaya devam eder.
**B) Audit kendi transaction'ına alınır** (`REQUIRES_NEW`), böylece audit hatası
geçişi düşürmez.

## Karar

**K1 — Alan şekli: S1-A (dört kolon, tür türetilir).**

`audit_logs` tablosuna dört nullable kolon eklenir ve her yan için ayrı bir
karşılıklı dışlama CHECK'i konur. **Ayrı bir `assignment_kind` kolonu açılmaz.**

Gerekçe iki tanedir:

1. **Türetimin tek doğruluk kaynağı zaten `AssignmentView.of(...)`'dur.** Türü
   şemada da saklamak ikinci bir kaynak yaratır; iki kaynak zamanla ayrışır ve
   ayrışmayı önlemek için konacak tutarlılık CHECK'i, saklamanın kazandırdığı
   her şeyi geri alır.
2. **`records` tablosu da aynı şekli kullanıyor** (`V21`: `assigned_to` +
   `assigned_department_id` + `chk_records_assignment_exclusive`). Audit'in kaydın
   kendisinden farklı bir şekil kullanması için bir sebep yok; aynı şekil,
   `AssignmentView.of`'un iki yerde de aynı biçimde çalışması demektir.

S1-B'nin lehine olan argüman — "append-only iz yazıldığı andaki yorumu
dondurmalı" — kabul edilebilir bir kaygıdır, fakat burada dondurulacak bir yorum
yoktur: `kind` türetimi üç değerli ve tam belirlenmiş bir fonksiyondur
(kişi dolu → `USER`, departman dolu → `DEPARTMENT`, ikisi de boş → `NONE`).
Bu fonksiyonun ileride değişmesi, `records` tablosunun okunuşunu da değiştirirdi;
yani risk audit'e özgü değil ve audit'te kolon açarak kapatılamaz.

**"İkisi de NULL" serbesttir ve `NONE` demektir.** CHECK yalnız "ikisi birden
dolu" hâlini yasaklar — `chk_records_assignment_exclusive` ile birebir aynı
semantik.

**K2 — Kolonlar yalnız geçiş satırlarında anlamlıdır.**

`audit_logs` üç tür satır taşır: workflow geçişleri, kayıt yaşam döngüsü olayları
(`recordLifecycleEvent`) ve `ADMIN` aktörünün HTTP erişim logları
(`recordAccess`). Yalnız birincisi atama değiştirir; diğer ikisinde dört kolon da
`NULL` kalır ve okuma tarafı bu satırlarda atamayı **yorumlamamalıdır**. Geçiş
satırı ayrımı zaten `previous_status IS NOT NULL` ile yapılıyor.

Yaşam döngüsü satırlarına "değişmedi" anlamında kaydın o anki ataması
yazılabilirdi; reddedildi — `previous == new` satırları geçiş satırlarıyla
karışır ve `recordLifecycleEvent`'in üç çağrı yeri atamayı parametre olarak
almadığı için `record` modülünü de değiştirmek gerekirdi.

**K3 — Geriye dönük veri üretilmez.**

`V25` öncesi yazılmış bütün satırlar dört kolonda da `NULL` kalır ve
**"atama bilgisi kaydedilmemiş"** olarak okunur. Geçmiş satırlara kaydın bugünkü
atamasından türetilmiş değer yazmak, kaydedilmemiş bir olguyu kaydedilmiş gibi
göstermek olurdu; append-only bir izde bu kabul edilemez. Migration'ın veri
onarım adımı **yoktur**.

**K4 — Okuma tarafı da açılır (S2-B).**

`AuditLogResponse` `previousAssignment` ve `newAssignment` alanlarını
`AssignmentView` olarak taşır. **Ham kimlikler açılmaz**: `AssignmentKind`
sözleşmesi (APP-9/APP-10/B11 §3.1) istemcinin türü nullable alanları
karşılaştırarak çıkarsamasını açıkça yasaklar.

Yalnız yazma tarafı (S2-A) reddedildi çünkü **üründe doğrulanamaz** kalırdı:
hiçbir uç döndürmediği için kolon bir daha sessizce düşerse — bulgunun kendisi
tam olarak budur — kimse fark etmez.

Ad çözümü `AssignmentViewResolver.resolveAll(...)` ile **toplu** yapılır, satır
başına `resolve(...)` çağrılmaz (N+1). Zenginleştirme, görünürlük **kırpmasından
sonra** uygulanır: gizlenen satırlardaki kişi ve departman adları yanıta hiç
girmez.

**K5 — Transaction sınırı değişmez (S3-A).**

`WorkflowActionService.performAction` üzerindeki `@Transactional` tek sınır
olmaya devam eder; `AuditLogService` `@Transactional` almaz ve çağıranın
transaction'ına katılır. `update → audit → publish` sırası korunur.

S3-B (audit'i `REQUIRES_NEW`'e almak) **reddedildi.** `WorkflowActionService`'in
kendi javadoc'undaki kural bağlayıcıdır: *"bir geçiş sırasında kaydı günceller ve
denetim izini yazar; bu ikisi ya birlikte olmalı ya da hiç olmamalı."* Audit'i
ayrı transaction'a almak, kaydın güncellendiği ama izinin yazılmadığı bir aralık
açardı. `MailActionTokenService.issue`'nun `REQUIRES_NEW` kullanması bu kuralın
istisnası değildir — o, `AFTER_COMMIT` fazından çağrıldığı için zaten commit
edilmiş bir transaction'ın dışındadır.

`WorkflowTransitionPersistenceIntegrationTest.aFailedAuditWriteRollsBackTheRecordUpdate`
bu invariant'ı çiviler ve `B12` teslimi sonrasında **değişmeden yeşil kalmalıdır**;
bu, sınırın korunduğunun kanıtıdır.

**K6 — Model alan adları kolon adlarıyla hizalanır.**

`WorkflowTransitionAudit.assignedTo` → `newAssignedTo` olarak yeniden adlandırılır
ve dört alan `previousStatus`/`newStatus` çiftinin paralelinde durur. Record'un
varlık nedeni "önceki/yeni" çiftleridir; ikinci çiftin farklı bir adlandırma
kuralı izlemesi okuyanı yanıltırdı.

Kısa (uyum) kurucu **eklenmez**. Dokuz argümanlı bir uyum kurucusu previous
yanını sessizce `null` bırakırdı — bu tam olarak `B12`'nin kendisidir. Her çağrı
yeri iki yanı da açıkça belirtmek zorundadır.

## Sonuçlar

**Şema.** `V25__audit_assignment_columns.sql`: dört nullable kolon, dört FK
(`users` ve `departments`, hepsi `ON DELETE RESTRICT` — `fk_audit_user`/
`fk_audit_role` ve `V22`'nin departman FK kuralıyla aynı), iki CHECK. **Index
açılmaz**: bu kolonlar hiçbir okuma yolunun `WHERE`'inde geçmez (`audit_logs`
sorguları `record_id`/`user_id`/`created_at` üzerinden gider) ve append-only,
sürekli büyüyen bir tabloda kullanılmayan index salt yazma maliyetidir.

**Uygulama sırası.** `V25` **tek başına uygulanabilir ve zararsızdır** — kolonlar
nullable, mevcut entity onları yazmaz. Tersi geçerli değildir: `AuditLog` entity'si
kolonları tanırken migration koşmamışsa her audit yazımı düşer. Bu yüzden
migration Java'dan **önce veya onunla birlikte** gider. `V24`'ün "tek başına
uygulanamaz" uyarısı buraya uymaz; oradaki bağımlılık iki yönlüydü.

**Sözleşme.** `records.assigned_department_id` üzerindeki "bu alan
`WorkflowTransitionAudit`'te taşınmaz" notu geçersizleşir. `docs/openapi.json`'da
`AuditLogResponse` iki yeni `$ref` alanı kazanır; `AssignmentView` şeması `B11`
ile zaten mevcuttur, yeni şema tanımı gerekmez.

**Kabul edilen sınır.** `WorkflowStatusChangedEvent` `previousAssignedDepartmentId`
taşımaz ve bu teslimde **eklenmez**; bildirim alıcı hesabı onu kullanmıyor,
`NT-5` fan-out'u yeni atamadan çalışıyor. Event'in genişletilmesi ayrı bir
ihtiyaç doğduğunda ele alınır.

## Kabul kaydı

Karar `B12`'nin uygulanmasıyla aynı teslimde kabul edildi ve uygulandı.
Bulgunun sahibi kulvar olarak Alperen'dedir; bu teslim tek seferlik bir sapmayla
Burak tarafından yapılmıştır ve kulvar sahipliğini devretmez.

## Bağlantılar

- [APP-9/APP-10/B11 istemci sözleşmesi](../APP9_APP10_B11_ISTEMCI_SOZLESMESI.md) — §3 ortak atama sözleşmesi, §6 "Veri (`B12`)" kabul maddesi
- [ADR-0005](0005-departman-atamasi-ve-akis-kurali.md) — departman ataması ve akış kuralı
- [ADR-0006](0006-departman-hedefli-target-strategy.md) — `DEPARTMENT` hedef stratejisi
- [DB-1 veri modeli sözleşmesi](../DB_1_VERI_MODELI_SOZLESMESI.md) — §13.1 ileri migration kuralı
- [database.md](../database.md) · [architecture.md](../architecture.md) · [workflow.md](../workflow.md)
