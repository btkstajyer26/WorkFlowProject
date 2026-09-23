# ADR-0010: Parent kayıt için dinamik subtask alt akışı

- Durum: Önerildi
- Tarih: 2026-09-21
- Karar sahipleri: Proje ekibi

## Bağlam

`BSK_YRD_INCELEMESINDE` durumundaki bir Parent kayıt, Başkan Yardımcısı
tarafından dinamik sayıda alt göreve bölünebilmelidir.

Her Subtask farklı bir kullanıcıya atanır ve Parent'ın genel workflow
motorundan bağımsız, hafif bir akış yürütür:

`DEGERLENDIRME → ISLEM → ONAY → TAMAMLANDI | REDDEDILDI`

`TAMAMLANDI` ve `REDDEDILDI` terminal durumlardır.

Parent bölünürken `UNANIMOUS` veya `MAJORITY` onay politikası seçilir.
`UNANIMOUS` için gerekli onay sayısı N, `MAJORITY` için
`floor(N / 2) + 1` olarak split anında hesaplanıp Parent üzerinde saklanır.

Politika sonucu daha erken belli olsa bile Parent bütün Subtask'ların terminal
duruma gelmesini bekler. Son Subtask sonuçlandığında Parent kullanıcı aksiyonu
olmadan `ALT_GOREV_BEKLIYOR` durumundan `KONTROL` durumuna ilerler.

Parent gerçek bir `Record` olduğu için bu iki Parent geçişi mevcut workflow
motorundan geçmelidir. Subtask'lar ise genel workflow motoruna dahil edilmez.

Otomatik Parent geçişinin mevcut workflow doğrulama ve audit yolunu atlamaması
için insan olmayan bir `SISTEM` workflow aktörüne ihtiyaç vardır.

ADR-0003 veri tanımlı ana workflow motorunu ele almıştır. Paralel alt görev
fan-out/join davranışı ise ayrı bir mimari karardır ve bu ADR kapsamında
tanımlanır.

## Değerlendirilen Seçenekler

### 1. Subtask'ları genel workflow motoruna dahil etmek

Her Subtask'ı ayrı bir `Record` gibi genel workflow motorundan geçirmek.

Bu yaklaşım mevcut transition altyapısını yeniden kullanır ancak Subtask'lara
gereksiz görünürlük, hedef çözümleme, audit ve genel workflow davranışlarını
taşır. Ayrıca Subtask'ın tekrar bölünmesi gibi anlamsız durumların ayrıca
engellenmesi gerekir.

Bu seçenek reddedildi.

### 2. Ayrı hafif Subtask modeli kullanmak

Parent mevcut workflow motorunda kalır. Subtask'lar ayrı `subtasks` tablosunda
ve kendilerine ait küçük durum makinesiyle ilerler.

Parent'a ait iki yeni workflow geçişi eklenir:

- `BSK_YRD_INCELEMESINDE + ALT_GOREVLERE_AYIR → ALT_GOREV_BEKLIYOR`
- `ALT_GOREV_BEKLIYOR + ALT_GOREVLER_SONUCLANDI → KONTROL`

İkinci geçiş sistem aktörü tarafından gerçekleştirilir.

Bu seçenek seçildi.

### 3. Subtask'lar için ikinci genel amaçlı workflow motoru kurmak

Gelecekte genişletilebilir olsa da v1 ihtiyacının çok ötesinde karmaşıklık,
versioning ve yeni workflow tanım altyapısı gerektirir.

Bu seçenek v1 kapsamında reddedildi.

## Karar

### Veri modeli

`records` tablosuna yalnız Parent kayıtlar için kullanılan:

- `subtask_approval_policy`
- `subtask_required_approvals`

alanları eklenir.

Subtask'lar ayrı `subtasks` tablosunda tutulur. Bir Subtask tekrar bölünemez.

Subtask durumları:

- `DEGERLENDIRME`
- `ISLEM`
- `ONAY`
- `TAMAMLANDI`
- `REDDEDILDI`

### Parent workflow

`RecordStatus` içine:

- `ALT_GOREV_BEKLIYOR`
- `KONTROL`

`WorkflowAction` içine:

- `ALT_GOREVLERE_AYIR`
- `ALT_GOREVLER_SONUCLANDI`

eklenir.

`ALT_GOREVLERE_AYIR` gerçek kullanıcıyla,
`ALT_GOREVLER_SONUCLANDI` ise `SISTEM` aktörüyle çalıştırılır.

### Sistem aktörü

Parent'ın otomatik ilerlemesinde `record.status` doğrudan değiştirilmez.

Workflow uygulama servisi açıkça verilen bir `CurrentActor` ile
çalıştırılabilen overload sunar. Normal HTTP akışı mevcut aktör sağlayıcısını
kullanmaya devam eder; yalnız otomatik join yolu sistem aktörünü geçirir.

`SYSTEM` actor requirement bir insan ilişkisi kontrolü değildir; creator veya
assignee ilişkisi aramaz. Yetkilendirme yine transition'ın `actor_role_id`
alanıyla yapılır ve bu alan `SISTEM` rolünü zorunlu tutar.

Yalnız `actor_requirement = SYSTEM` transition'larında
`required_permission_id = NULL` kullanılabilir. Bu değer “ek capability
permission gerekmiyor” anlamına gelir; rol ve workflow aktörü kontrollerini
atlamaz. `CREATOR`, `ASSIGNEE`, `CREATOR_AND_ASSIGNEE` ve diğer insan
transition'larında required permission zorunluluğu aynen korunur. Dolayısıyla
bu istisna global bir “permission gerekmiyor” davranışı değildir ve SISTEM
transition'ına `RECORD_FORWARD` gibi bir insan permission'ı verilmez.

### Split kuralları

Parent yalnız `BSK_YRD_INCELEMESINDE` durumundayken bölünebilir.

- En az iki Subtask gerekir.
- Aynı kullanıcı birden fazla Subtask'a atanamaz.
- Approval policy zorunludur.
- `UNANIMOUS`: required approvals = N.
- `MAJORITY`: required approvals = `floor(N / 2) + 1`.

### Subtask aksiyonları

Bir Subtask üzerinde yalnız `assignedTo` kullanıcısı işlem yapabilir.

Desteklenen aksiyonlar:

- `DEGERLENDIRMEYI_TAMAMLA`
- `ISLEMI_TAMAMLA`
- `ONAYLA`
- `REDDET`

`REDDET` için yorum zorunludur.

### Race-safe join

Terminal Subtask tamamlandığında Parent `PESSIMISTIC_WRITE` ile kilitlenir.

Terminal olmayan sibling varsa işlem yapılmaz.

Bütün Subtask'lar terminal durumdaysa approval policy sonucundan bağımsız olarak
sistem aktörüyle `ALT_GOREVLER_SONUCLANDI` çalıştırılır ve Parent `KONTROL`
durumuna geçer.

Eş zamanlı iki tamamlama durumunda Parent kilidi işlemleri serileştirir.
İkinci deneme artık geçerli transition bulamazsa mevcut
`WORKFLOW_INVALID_TRANSITION` davranışı idempotent no-op olarak ele alınır.

### REST response sınırı

`SubtaskView` yalnız tek bir Subtask'ın alanlarını taşır:

- `id`
- `parentRecordId`
- `title`
- `description`
- `assignedTo`
- `assignedToName`
- `status`
- `resolutionComment`
- `createdAt`
- `completedAt`

Parent'a ait `approvalPolicy` ve `requiredApprovals` alanları hiçbir zaman
`SubtaskView` içine konmaz.

Split response üst seviyede:

- `parentRecordId`
- `approvalPolicy`
- `requiredApprovals`
- `subtasks: SubtaskView[]`

taşır.

GET `/api/records/{recordId}/subtasks` response'u üst seviyede:

- `approvalPolicy`
- `requiredApprovals`
- `subtasks: SubtaskView[]`

taşır.

Henüz bölünmemiş Parent için GET:

- `approvalPolicy = null`
- `requiredApprovals = null`
- `subtasks = []`

döndürür.

## Sonuçlar

- Parent mevcut workflow motorunun bütünlüğünü korur.
- Subtask'lar genel workflow motorunu gereksiz yere genişletmez.
- Otomatik Parent geçişi mevcut transition doğrulama ve audit yolundan geçer.
- Paralel tamamlanmalarda Parent yalnız bir kez ilerler.
- Onay politikası split anında Parent üzerinde kalıcılaştırılır.
- Bütün Subtask'lar terminal duruma gelmeden Parent ilerlemez.
- Mobil istemci v1 kapsamı dışındadır.
- Subtask geri sarma v1 kapsamında desteklenmez.

## Bağlantılar

- [ADR-0003: Veri tanımlı akış motoru ve birim bazlı roller](0003-veri-tanimli-akis-motoru-ve-birim-bazli-roller.md)
- [ADR-0005: Departman ataması ve akış kuralı](0005-departman-atamasi-ve-akis-kurali.md)
- [Workflow dokümantasyonu](../workflow.md)
- [Veritabanı dokümantasyonu](../database.md)
