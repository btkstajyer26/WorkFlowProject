# WF-8 / AP-8 — Sabit geçişlere dinamik aktör rolü bağlama

## Teslim sınırı

WF-8 backend servisi, mevcut bir geçişin aktör rollerini genişletir ve kullanılmayan
dinamik rol bağlarını pasifleştirir. `workflow_transitions` tablosu ve mevcut unique
constraint kullanılır; migration yoktur. Grafik topolojisi, hedef çözümleme,
permission ve aktör ilişkisi düzenlenmez. Departman tabloları sorgulanmaz.

Tamer'in AP-8 işi bu servise HTTP adapter'ı ve yönetim ekranı eklemektir. Bu teslim
yeni endpoint/UI içermez; mevcut `POST /api/workflow/rules/reload` ve yanıtı
`{"ruleCount": n}` korunur. AP-2/AP-3 rol ve permission yönetimi ile WF-2C2/DB-8
departman kabulü ayrı işlerdir.

## Java servis sözleşmesi

Spring bean: `workflow.service.WorkflowActorBindingService`.

| Metot | Girdi | Sonuç | Gerekli authority |
| --- | --- | --- | --- |
| `listTransitions()` | Yok | `List<WorkflowActorBindingView>` | `WORKFLOW_VIEW` |
| `bind(templateTransitionId, actorRoleId)` | İki pozitif `Integer` | Etkinleştirilen `WorkflowActorBindingView` | `WORKFLOW_MANAGE` |
| `unbind(bindingId)` | Pozitif `Integer` | Pasifleştirilen `WorkflowActorBindingView` | `WORKFLOW_MANAGE` |

Girdiler aktör kimliği, hedef rol, durum, aksiyon, permission veya aktör ilişkisi
taşımaz. Yönetici kimliği güvenilir `AuthenticatedUser` oturumundan alınır;
aktif hesap ve rol şartı vardır. Kayıt görünürlüğündeki ADMIN yasağı burada
uygulanmaz. Sadece frontend kontrolü yeterli değildir; servis yetkiyi kendisi denetler.

Liste aktif ve pasif bağları `bindingId` sırasıyla döndürür; boş liste geçerlidir.
DTO; bağ kimliği, kaynak/hedef durum ve aksiyonun kimlik/kod/görünen adları,
aktör rolünün kimliği/adı, `actorRequirement`, `targetStrategy`,
`expectedTargetRoleId`, `requiredPermissionId`, `requiredPermissionCode`,
`active` ve `protectedBinding` alanlarını içerir. Her satır bir bağdır; AP-8
sabit geçiş alanları üzerinden gruplayabilir. `protectedBinding`, rolün
`is_system=true` veya `system_key` dolu olmasıyla hesaplanır; rol adı kullanılmaz.
DTO doğrudan entity döndürmez ve Java record olarak değişmezdir.

### Bağlama

- Şablon aktif olmalı. Kaynak/hedef durum ve aksiyon aktif, runtime enum'larıyla
  uyumlu olmalı; kaynak terminal olamaz. Hedef stratejisi ve permission mevcut
  `TransitionRule` yapısal doğrulamasından geçer. Gerekli permission aktif olmalı.
- Hedef aktör rolü aktif, `is_workflow_actor=true`, `is_system=false` ve
  `system_key=NULL` olmalı. Aktif `RECORD_VIEW` ile şablonun gerekli permission'ı
  role atanmış olmalı. Sistem aktörlerine yeni bağ eklenmez.
- Sabit alanlar aynen kopyalanır: `from_status_id`, `action_id`,
  `actor_requirement`, `to_status_id`, `expected_target_role_id`,
  `target_strategy`, `required_permission_id`. Yalnız satır kimliği, aktör rolü
  ve aktiflik değişir. Kullanıcı veya kayıt ataması yapılmaz.
- Aynı `(from_status_id, action_id, actor_role_id)` için aktif bağ çakışmadır.
  Pasif bağ yalnız sabit alanları şablonla eşleşiyorsa aynı kimlikle etkinleşir;
  farklı alanlar üzerine yazılmaz. Yeni şablon referansı kolonu tutulmaz.

Bu işlem dinamik rolün bütün sistem rolü yaşam döngüsünü devraldığı anlamına
gelmez. Örneğin geri dönüş geçişinin beklediği hedef rol, kaydı oluşturanın yeni
rolünden farklıysa mevcut hedef rol doğrulaması yine reddeder. Bağ ekleme bunu
değiştirmez. Sonradan permission kaldırıldığında workflow'un mevcut permission
kontrolü erişimi keser; satırın otomatik silinmesi gerekmez.

### Kaldırma ve kullanım kontrolü

Fiziksel silme yoktur. Sistem bağları korunur. Zaten pasif bir bağ tekrar
kaldırıldığında ek audit yazılmaz.

SQL sorgusu silinmemiş, terminal olmayan ve bağın kaynak durumundaki kayıtları
rolün kullanıcılarıyla eşleştirir:

| Aktör ilişkisi | Kullanım sayılan durum |
| --- | --- |
| `CREATOR` | Kaydı oluşturan kullanıcı bağlanan rolde |
| `ASSIGNEE` | Doğrudan atanan kullanıcı bağlanan rolde |
| `CREATOR_AND_ASSIGNEE` | Aynı kullanıcı hem oluşturucu hem atanan ve bağlanan rolde |

En az bir eşleşme varsa kaldırma reddedilir; başka bir aksiyonun bulunması
sonucu değiştirmez. Hesap/rol pasifliği ve permission eksikliği sorguyu daraltmaz;
geçici yetki kaldırma ile açık kayıt koruması aşılamaz. Departman üyeliği veya
geçmişte işlem yapma bir eşleşme nedeni değildir.

Kontrol kaldırma transaction'ındaki veriyi değerlendirir. Başlamış işlemlerin
eski snapshot ile tamamlanması kabul edilmiştir; bu işlemler sonradan kaynak
duruma kayıt getirebilir. Kaldırma bütün uçuş halindeki işlemleri durdurmaz ve
hiç yeni bekleyen kayıt oluşmayacağı garantisini vermez.

## Transaction ve snapshot sınırı

Mutasyonlar açık transaction dışından, Spring bean üzerinden çağrılmalıdır.
`@Transactional(propagation=NEVER)` yanlış entegrasyonu reddeder. AP-8 controller'ı
mutasyonu kapsayan transaction açmamalı ve repository'ye doğrudan yazmamalıdır.
Servis kendi `TransactionTemplate` sınırını yönetir.

`ReloadableTransitionRuleSource` aynı monitor altında manuel reload ile bağ
yazmalarını sıraya alır. Rol satırı önce, ilgili geçiş satırları kimlik sırasıyla
kilitlenir. AP-2/AP-3 rol/permission yazıcıları da aynı rol satırını kilitlemelidir;
doğrudan SQL bu uygulama protokolünün dışındadır. Kullanıcı kapasitesi kuralları
değişmez.

Bağ ve audit aynı transaction'dadır. Yeni aktif kural kümesi commit öncesi
`DbTransitionRuleSource` ile tamamen doğrulanır. Transaction başarıyla döndükten
sonra hazırlanmış snapshot atomik olarak yayınlanır; commit sonrasında yeni DB
okuması yoktur. Doğrulama/commit hatası DB değişikliğini geri alır ve çalışan
snapshot'ı korur. Manuel reload daha eski bir snapshot ile başarılı yazmayı ezemez.
Yapısal reload, mevcut bağların permission'larını tekrar atamaz veya permission
iptalini kural yapılandırma hatasına dönüştürmez.

`TransitionRuleSource.snapshot()` salt okuma portunun parçasıdır. Değişmez
kaynak kendisini, reloadable kaynak mevcut delegate'i döndürür.
`WorkflowApplicationService` başlangıçta bir snapshot alır; kural seçimi,
iki validator geçişi ve hedef çözümleme aynı kurala dayanır. Çalışan workflow
transaction'larına kilit veya yeni transaction propagation eklenmez; mail token
tüketimiyle aksiyonun mevcut atomikliği korunur.

Bu garanti tek backend instance içindir. Başarılı commit ile bellek yayını
arasında süreç kapanırsa sonraki açılış DB'den güncel snapshot'ı yükler.
Çoklu instance yayını/invalidation bu teslimin kapsamı değildir.

Audit aksiyonları `WORKFLOW_BINDING_ENABLED` ve `WORKFLOW_BINDING_DISABLED`.
ADMIN işlemi `audit_logs`, diğer aktörler `user_audit_logs` tablosuna gider.
Yorumda şablon kimliği (etkinleştirmede), bağ kimliği, işlem yapanın rolü,
bağlanan rol, kaynak durum/aksiyon kimlikleri ve önceki/yeni aktiflik tutulur.
Kayıt kimliği ve HTTP alanları boş kalır; evrak geçmişine karışmaz.

## Hata sözleşmesi ve AP-8 entegrasyonu

`WorkflowBindingException.reason()` ayrı iş nedenini, `code()` ise
`WORKFLOW_BINDING_` önekli API kodunu döndürür. Global exception handler eşlemesi:

| Reason | HTTP |
| --- | --- |
| `INVALID_ID`, `INVALID_TEMPLATE`, `INVALID_ROLE`, `MISSING_ROLE_PERMISSION` | 400 |
| `TEMPLATE_NOT_FOUND`, `BINDING_NOT_FOUND`, `ROLE_NOT_FOUND` | 404 |
| `DUPLICATE_BINDING`, `METADATA_MISMATCH`, `PROTECTED_BINDING`, `BINDING_IN_USE` | 409 |

Oturumsuz/pasif hesap mevcut authentication davranışına tabidir; eksik yönetim
authority'si `403 FORBIDDEN` döner. Transaction sınırının ihlali programlama
hatasıdır; frontend iş hatası değildir. Bozuk toplam kural kümesi veya DB hatası
başarıya çevrilmez; rollback ve mevcut snapshot korunur.

AP-8 başarı yanıtından sonra kataloğu yenileyebilir. Ek reload çağrısı gerekmez.
Kaldırma/bağlama hatalarını yukarıdaki kodlarla göstermeli; toplu iş için her
servis çağrısı ayrı commit olduğundan toplu atomiklik vaat etmemelidir.

## Doğrulama

`WorkflowActorBindingIntegrationTest` gerçek PostgreSQL üzerinde commit,
kopyalama/yeniden etkinleştirme, yetki, kullanım ilişkileri, dinamik workflow
aksiyonu, rollback ve eşzamanlı reload/bağlama davranışlarını doğrular. Fixture'lar
yalnız kendi oluşturdukları kimliklerle temizlenir; sistem geçişlerinin değişmediği
ve snapshot'ın başlangıca döndüğü her test sonunda kontrol edilir.

`WorkflowSnapshotConsistencyTest` hedef çözümlemede işlemi durdurup arada reload
yapar: eski işlem tamamlanır, sonraki işlem kaldırılmış bağı kullanamaz.

Veritabanlı test öncesi Compose ve çalışan PostgreSQL/host portu kontrol edilmelidir.
Kabul: ilgili testler ve tam backend `mvn verify` başarılı; yeni migration veya
HTTP endpoint'i yok. Çalıştırma sonuçları teslimde ayrıca raporlanır.

### Yerel kabul kaydı — 4 Eylül 2026

- Docker Compose kontrolü: `workflow-db` sağlıklı, PostgreSQL 15.18,
  `127.0.0.1:5433 -> 5432`.
- `DB_PORT=5433` ile tam backend Maven `verify`: **703 test, 0 failure,
  0 error, 0 skipped; BUILD SUCCESS**. Backend JAR üretildi.
- WF-8 için 35 PostgreSQL servis senaryosu ve 1 işlem/snapshot tutarlılığı
  testi eklendi. Mevcut JPA'sız HTTP/güvenlik testleri yeni yönetim servisini
  mock olarak tanır; WF-8 yazma kabulünde gerçek servis ve DB kullanılır.
- Yerel koşu çıktısı: `backend/target/wf8-verify.log` (build çıktısı, Git'e alınmaz).
  Bu kayıt yerel test kabulüdür; dağıtım veya AP-8 HTTP/UI teslimi değildir.
