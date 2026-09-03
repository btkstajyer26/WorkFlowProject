# WF-2A — RoleName Kaldırma Hazırlık Envanteri

> Güncel uygulama durumu ve kabul kararları [§18.8](#188-wf-2b-ve-wf-2c1-uygulama-kapanışı--23-eylül-2026) içindedir. Önceki envanter/gate tabloları tarihsel snapshot'tır.

## 1. Amaç

Bu belge, `RoleName` kaldırıldığında hangi kodun neden etkileneceğini ve her
bağımlılığın hangi hedef modele taşınması gerektiğini kaydeder. Bu çalışma bir
refactor veya rollout değildir; sonraki küçük WF-2 PR'ları için karar ve bağımlılık
haritasıdır.

Bağlayıcı kaynaklar:

- `docs/DB_1_VERI_MODELI_SOZLESMESI.md`
- `docs/workflow.md`
- bu belgenin repo snapshot'ında bulunan backend, frontend ve mobile kodu

DB-1'e göre `roles.id` ilişkisel kimlik, `roles.name` yönetilebilir/gösterilen
addır. Yerleşik rol semantiği gerekiyorsa `system_key`, capability gerekiyorsa
permission authority, workflow kuralında rol kimliği gerekiyorsa `RoleId` veya
eşdeğer hafif domain kimliği kullanılmalıdır. Bu üç kavram birbirinin yerine
geçmez.

Envanter kategorileri:

| Kod | Anlam | Hedef yön |
| --- | --- | --- |
| A | Permission / authority | `Permission.code` ve `hasAuthority(...)` |
| B | Workflow rol kimliği | `RoleId` / `roles.id` referansı |
| C | Yerleşik sistem rolü semantiği | Dar kapsamlı `SystemRoleKey` veya `roles.max_users` |
| D | Görünürlük / policy | Capability + kayıt ilişkisi + durum tabanlı policy |
| E | Compatibility / DTO / API / UI | Geçici rol adı kontratı veya presentation |
| F | Dead / redundant / test-only | Silme, test fixture dönüşümü veya arşivleme |

Bir dosya birden çok semantik taşıyorsa aşağıdaki tablolarda kullanım sembol
seviyesinde ayrılmıştır; her satır yalnız bir kategoriye atanmıştır.

## 2. Repo snapshot

Snapshot tarihi: **2026-09-01**. Bu bölümdeki sayılar ve davranış tabloları aynı
tarihte repo üzerinde ikinci kez, bağımsız olarak yeniden doğrulanmıştır; sapma
bulunmamıştır (bkz. §3.4).

| Kontrol | Sonuç |
| --- | --- |
| Çalışılan dal | `feature/dynamic-workflow` |
| HEAD | `098f79e3ba9628bed4ad2955daa81dda201078d9` |
| HEAD özeti | `docs(workflow): RoleName migration envanteri eklendi` |
| Önceki commit | `7652431406059cbcf012156557ac222e9412625a` &mdash; `feat(workflow): DB geçiş kuralı kaynağı iskeleti eklendi` (SM-7A) |
| Yerel `test` | `ff570563de2348c2cab684b676b7a06ce7bf49a2` |
| `origin/test` | `ff570563de2348c2cab684b676b7a06ce7bf49a2` |
| Uzak `refs/heads/test` | `ff570563de2348c2cab684b676b7a06ce7bf49a2` (`git ls-remote` ile doğrulandı) |
| Başlangıç working tree | Temiz |
| Dal ilişkisi | Mevcut HEAD, güncel `test` üzerine iki commit'tir: SM-7A iskeleti ve bu envanter commit'i |
| `RoleName.java` | Mevcut: `backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/RoleName.java` |
| `StaticTransitionRuleSource` | Mevcut ve production bean olarak aktif |
| `DbTransitionRuleSource` | Mevcut SM-7A iskeleti; anotasyonsuz, bean değil |
| DB-1 sözleşmesi | Mevcut dalda var; saf `test` ref'inde yok |

`test...HEAD` farkı yalnız SM-7A kapsamındaki beş Java/test dosyası, DB-1
sözleşmesi ve bu envanter belgesidir. `DbTransitionRuleSource` ve testi saf `test`
ref'inde yoktur.

Envanter bilerek `test` yerine bu dal üzerinden çıkarılmıştır: bağlayıcı kaynak olan
`docs/DB_1_VERI_MODELI_SOZLESMESI.md` saf `test` ref'inde bulunmaz. Production Java
kaynağı açısından iki ref arasındaki tek fark SM-7A dosyalarıdır; §3.1 bu farkı ayrıca
sayısallaştırır.

Güncel `TransitionRuleSource` imzası:

```java
Optional<TransitionRule> find(
        RecordStatus from,
        WorkflowAction action,
        RoleName actorRole);

List<TransitionRule> all();
```

`WorkflowConfiguration#transitionRuleSource()` halen
`new StaticTransitionRuleSource()` döndürür. Dolayısıyla SM-7A production
kaynağını DB'ye geçirmemiştir.

## 3. Sayısal özet

### 3.1. Backend ve client sayıları

| Ölçüm | Production | Test | Toplam / not |
| --- | ---: | ---: | --- |
| `RoleName` import eden Java dosyası | 18 | 23 | 41 |
| Tam `RoleName` token'ı içeren Java dosyası | 26 | 25 | 51 |
| `RoleName.X` doğrudan referansı | 14 | 314 | 328 |
| `RoleName.valueOf` | 6 | 0 | 6 |
| `hasRole(` annotation noktası | 7 | — | 12 endpoint'i koruyor |
| `hasAnyRole(` | 0 | — | Yok |
| Rol üzerinde exhaustive switch bulunan dosya | 2 | — | `RecordAccessPolicy`, `RecordSpecifications` |
| `.getExpectedTargetRole()` çağrısı | 1 | 2 | 3; declaration sayılmadı |
| `.requiresTargetUser()` çağrısı | 3 | 0 | Üçü `WorkflowApplicationService` içinde |
| Teknik rol string'i içeren frontend dosyası | — | — | 41 |
| Teknik rol string'i içeren mobile dosyası | — | — | 7 |
| Frontend + mobile | — | — | 48 dosya: 23 runtime, 25 test/mock/e2e |

Saf `test` ref'inde tam `RoleName` token'ı production/test için `25/24`, import
sayıları `17/22`'dir. Mevcut daldaki `+1/+1` farkı SM-7A'nın
`DbTransitionRuleSource` ve `DbTransitionRuleSourceTest` dosyalarından gelir.

Tam token sayımı `\bRoleName\b` ile yapılmıştır. Düz `RoleName` substring araması
`getRoleName` gibi compatibility metotlarını da yakalar ve production için 32
dosya verir; bu değer enum/type bağımlılığı sayısı değildir.

### 3.2. Backend Java çift tırnaklı teknik rol sabitleri

| Değer | Production | Test | Toplam |
| --- | ---: | ---: | ---: |
| `"CALISAN"` | 2 | 43 | 45 |
| `"BASKAN_YARDIMCISI"` | 5 | 15 | 20 |
| `"BASKAN"` | 1 | 48 | 49 |
| `"ADMIN"` | 5 | 7 | 12 |

Bu sayım sabit-string eşleşmesidir; `BASKAN_INCELEMESINDE` gibi daha uzun
status/action adları dahil değildir. Tek tırnaklı Spring Security ifadeleri
`hasRole` tablosunda ayrıca sayılmıştır.

### 3.3. Production dışı kaynaklar

- Tam `RoleName` token'ı içeren dört mevcut doküman vardır:
  `docs/database.md`, `docs/DB_1_VERI_MODELI_SOZLESMESI.md`,
  `docs/archive/EKSIK_SINIFLAR_VE_ONCELIK.md` ve
  `docs/archive/BACKEND_ACIK_ISLER_VE_GOREV_DAGILIMI.md`.
- `backend/src/main/resources/db/migration/V1__init_database_schema.sql`, dört
  yerleşik rol adını seed eder. Mevcut V1 değiştirilmemeli; DB-6 ileri yönlü
  migration ile `system_key` ve diğer alanları backfill etmelidir.
- `deploy/seed-test-data.sh` teknik rol adlarını test ortamı kullanıcı terfileri
  ve raporlama için kullanır; production Java sayısına dahil değildir.
- `docs/openapi.json` içinde dört teknik rolün tam string enum'u yoktur. Status
  ve action adlarında geçen `BASKAN` substring'leri rol kontratı değildir.

### 3.4. Tekrar üretilebilir arama sınırları

Ana sayımlar aşağıdaki arama biçimleriyle doğrulanmıştır:

```text
rg -l --glob '*.java' '^import .*RoleName;' backend/src/main/java
rg -l --glob '*.java' '\bRoleName\b' backend/src/main/java
rg -o --glob '*.java' 'RoleName\.[A-Z][A-Z_]+' backend/src
rg -o --glob '*.java' 'RoleName\.valueOf' backend/src
rg -o --glob '*.java' 'hasRole\(' backend/src/main/java
rg -l --glob '*.{ts,tsx,js,jsx}' '["''](CALISAN|BASKAN_YARDIMCISI|BASKAN|ADMIN)["'']' frontend mobile
```

Generated dosyalar ve docs production Java sayımlarına katılmamıştır.

`rg` varsayılan olarak `.gitignore` kurallarına uyar. Client sayımı bu nedenle
git-tracked kaynağı ölçer. Ignore kurallarını uygulamayan düz bir `grep -r` çağrısı
aynı pattern için mobile'da 7 yerine 8, toplamda 48 yerine 49 dosya döndürür; fark
yalnız git-ignored Expo web bundle'ı
`mobile/dist/_expo/static/js/web/entry-<hash>.js` dosyasıdır. Bu dosya derleme
çıktısıdır ve rol bağımlılığı envanterine ait değildir; sayıyı tekrar üretirken
ignore kuralları uygulanmalı veya bu yol açıkça dışlanmalıdır.

## 4. Mevcut RoleName dependency graph

```text
roles.id
   │
   └─ AuthenticatedUser.getRoleId()          ← ilişkisel kimlik bugün de mevcut
        └─ RequestAuditFilter ── audit_logs.role_id

roles.name
   │
   ├─ AuthenticatedUser.getRoleName()
   │    ├─ ROLE_<name> authorities ── hasRole(...) annotations
   │    ├─ SecurityCurrentActorProvider ── RoleName.valueOf
   │    ├─ RecordService/FileController ── RoleName.valueOf
   │    ├─ /api/users/me ── frontend/mobile role conditionals
   │    └─ audit request roleName ── ADMIN tablo routing
   │
   ├─ UserService / BootstrapAdminRunner
   │    ├─ default CALISAN
   │    ├─ singleton ADMIN/BASKAN/BASKAN_YARDIMCISI
   │    └─ BASKAN_YARDIMCISI görev/koltuk devri
   │
   └─ RoleName enum
        ├─ TransitionRuleSource / TransitionRule / TransitionRules
        ├─ TransitionContext / CurrentActor / WorkflowUserSnapshot
        ├─ WorkflowAction.expectedTargetRole
        ├─ TargetUserResolver / WorkflowUserPort
        ├─ WorkflowTransitionValidator
        ├─ audit/event payloads
        └─ RecordAccessPolicy / RecordSpecifications
```

Kırılma nedeni tek değildir. `RoleName` aynı anda authorization etiketi,
workflow FK yerine geçen kimlik, built-in semantik, visibility strategy seçici
ve API presentation değeri olarak kullanılıyor. Toplu bir enum-to-string veya
enum-to-SystemRoleKey dönüşümü bu anlamları tekrar birbirine bağlar.

Grafikteki iki kol aynı principal'dan çıkar: `AuthenticatedUser` hem `getRoleName()`
hem `getRoleId()` sunar, fakat workflow ve rbac zinciri yalnız ad kolunu kullanır.
Ayrıntı §10.2'dedir.

## 5. Production kullanım envanteri

Aşağıdaki yolların ortak kökü
`backend/src/main/java/btk/staj/WorkFlowProject/` dizinidir.

| Dosya / sembol | Bugünkü bağımlılık ve nedeni | Kategori | Hedef / bloke ettiği iş |
| --- | --- | --- | --- |
| `workflow/statemachine/RoleName` enum sabitleri | Genel rol identity'si dört sabit ada kapalı | B | Workflow kimlikleri `RoleId`; enum ancak tüm tüketiciler ayrışınca silinir |
| `RoleName#isWorkflowActor` | `ADMIN=false`, diğer üç rol `true` | D | DB-6 `roles.is_workflow_actor`; saf validator'a eligibility bilgisinin nasıl taşınacağı açık tasarım sorusu |
| `workflow/statemachine/TransitionRule#actorRole` | `(from, action, actorRole)` anahtarının rol bileşeni | B | `actorRoleId`; SM-7B ve DB parity |
| `workflow/statemachine/TransitionRuleSource#find` | Lookup parametresi `RoleName` | B | `RoleId`; tüm source implementasyonları atomik olarak uyarlanmalı |
| `workflow/statemachine/TransitionRules` | Sekiz statik kural RoleName ile indeksli | B | DB parity oracle; SM-9 yeşil olmadan silinmez |
| `workflow/statemachine/StaticTransitionRuleSource` | RoleName imzalı statik port adapter'ı | B | Production DB source devreye alınana kadar rollback kaynağı |
| `workflow/adapter/DbTransitionRuleSource` | SM-7A teknik `actorRole` string'ini `RoleName.valueOf` eşdeğeriyle map eder | B | SM-7B reader `actor_role_id`/rol referansı sağladıktan sonra RoleId mapping |
| `workflow/model/TransitionRuleRecord#actorRole` | DB adapter öncesi teknik rol adı string'i | B | SM-7B'de ilişkisel rol kimliği; environment-specific ID statik koda gömülmez |
| `workflow/statemachine/TransitionContext#actorRole` | Aktör transition kimliği | B | `RoleId` |
| `TransitionContext#targetRole` | Çözülen hedefin beklenen rolle eşleşmesi | B | Hedef `RoleId` karşılaştırması |
| `workflow/statemachine/WorkflowAction#expectedTargetRole` | Hedef rol action enum'unda gömülü | B | `workflow_transitions.expected_target_role_id`; WF-2D1 |
| `WorkflowTransitionValidator` rule lookup ve hedef karşılaştırma | Context/Rule/Action RoleName değerlerini karşılaştırır | B | Transition-owned RoleId metadata; validator saf Java kalır |
| `WorkflowTransitionValidator` workflow-actor erken kontrolü | `actorRole().isWorkflowActor()` hata sırasını belirler | D | Eligibility flag/policy input; `WORKFLOW_ROLE_NOT_ALLOWED` sırası korunmalı |
| `workflow/model/CurrentActor#role` | Security'den workflow ve visibility katmanına rol taşır | B | `RoleId` + gerekirse ayrı permission/eligibility snapshot'ı |
| `workflow/adapter/SecurityCurrentActorProvider` | `AuthenticatedUser.getRoleName()` → `RoleName.valueOf` | B | Principal'dan ilişkisel role ID alma; geçersiz-ad converter'ı kalkar |
| `workflow/model/WorkflowUserSnapshot#role` | Hedef kullanıcı rol kimliği | B | `RoleId` |
| `workflow/port/WorkflowUserPort#findActiveByRole` | Aktif hedefi `RoleName` ile arar | B | `findActiveByRoleId(RoleId)` veya transition hedef referansı |
| `workflow/adapter/UserPortAdapter` | `role.name()` query ve entity adından `RoleName.valueOf` | B | Role ID query/projection; display name business identity olmaktan çıkar |
| `workflow/model/TargetResolution.RoleNotConfigured` | Hatalı/eksik hedef rolünü `RoleName` ile taşır | B | `RoleId` veya transition target descriptor |
| `workflow/service/TargetUserResolver` | Action switch'i RoleName ile tek aktif rol arar | B | `target_strategy` + `expectedTargetRoleId`; action switch'i kaldırılır |
| `workflow/service/WorkflowApplicationService` | Actor/target rolünü context, audit ve event'e taşır | B | RoleId tabanlı orchestration; `requiresTargetUser` transition metadata'dan gelir |
| `workflow/model/WorkflowTransitionAudit#actorRole` | Audit FK'sini sonradan ad üzerinden çözmek için taşınır | B | Bilinen actor RoleId doğrudan audit portuna yazılır |
| `audit/service/AuditLogService#resolveRoleId` | `RoleName.name()` ile `roles.name` lookup | B | Actor RoleId doğrudan kullanılır; lookup kaldırılır |
| `workflow/model/WorkflowStatusChangedEvent#actorRole` | Event'te taşınıyor, hiçbir production listener okumuyor | F | Constructor/testler doğrulandıktan sonra redundant alan silinebilir; RoleId'ye körlemesine taşınmamalı |
| `rbac/service/PermissionService` transition convenience metotları | Rule source'u RoleName ile sorgular; production çağrısı yok | F | UI erken-kontrol ihtiyacı yoksa metotları ve yalnız testlerini temizle; statik kuralların kendisi dead değildir |
| `PermissionService#canCreateRecord` / `canEditOrDeleteDraft` | Production `RecordServiceImpl` role göre capability ön kontrolü yapar | A | `RECORD_CREATE` / `RECORD_EDIT`; owner/status koşulları ayrıca korunur |
| `record/service/RecordServiceImpl#getCurrentUserRole` | Principal rol adını enum'a çeviren bridge | B | CurrentActor/authority/policy girdisi; altı `valueOf` noktasından biri |
| `RecordServiceImpl` create/update/delete kontrolleri | Rol capability olarak kullanılıyor | A | Permission authority + mevcut creator/status kuralları |
| `rbac/service/RecordAccessPolicy` | RoleName exhaustive switch; detay ve tarihçe görünürlüğü | D | DB-8 policy strategy; `RECORD_VIEW` tek başına yeterli değil |
| `search/specification/RecordSpecifications#visibilityScope` | Aynı switch'in JPA predicate karşılığı | D | Query üretilebilen aynı policy; boolean/JPA parity zorunlu |
| `record/view/RecordContentView` | RoleName ile handoff snapshot seçer | D | Policy karar sonucu; dosya/başlık snapshot davranışı korunur |
| `attachment/controller/FileController` list/download/preview | Principal roleName → RoleName; visibility policy'ye iletir | D | CurrentActor/policy context; üç `valueOf` noktası |
| `attachment/service/FileService` | RoleName'i record/file visibility ve frozen content için taşır | D | Policy context/decision |
| `record/service/RecordServiceImpl`, `audit/controller/AuditLogController`, `search/service/RecordSearchServiceImpl` | Access policy tüketicileri | D | Ortak policy API; detay, liste, dosya ve tarihçe aynı sonucu üretmeli |
| `attachment/controller/FileController` upload/delete annotations | `hasRole('CALISAN')` capability gibi kullanılıyor | A | `RECORD_EDIT`, ayrıca owner/lock service kuralları |

### 5.1. Test bağımlılıkları

Tam `RoleName` token'ı içeren 25 test dosyası hedef PR'a göre birlikte
taşınmalıdır:

| Alan | Test dosyaları | Kategori / hedef |
| --- | --- | --- |
| Workflow rule/identity | `DbTransitionRuleSourceTest`, `SecurityCurrentActorProviderTest`, `SpringWorkflowEventPublisherTest`, `UserPortAdapterTest`, `WorkflowActionControllerTest`, `WorkflowTransitionPersistenceIntegrationTest`, `TargetUserResolverTest`, `WorkflowApplicationServiceTest`, `TransitionRulesTest`, `WorkflowTransitionValidatorTest` | F; WF-2D1/D2 fixture dönüşümü |
| Notification/event | `WorkflowStatusChangedListenerTest`, `NotificationControllerTest` | F; redundant event role alanı ve CurrentActor fixture'ları |
| RBAC/visibility/record/file/audit/user | `AuthorizationMatrixTest`, `PermissionServiceTest`, `PermissionServiceDelegationTest`, `RecordSpecificationsTest`, `RecordRepositorySortingTest`, `RecordSearchServiceImplTest`, `RecordServiceImplTest`, `RecordContentViewTest`, `FileServiceTest`, `FileServiceAuthorizationTest`, `AuditLogServiceTest`, `AuditLogControllerTest`, `UserServiceTest` | F; WF-2B/WF-2C ile davranış-pariteli dönüşüm |

23 dosya explicit import taşır. `TransitionRulesTest` ve
`WorkflowTransitionValidatorTest` aynı package içinde olduğu için import
etmeden RoleName kullanır; import sayısı ile token dosyası sayısının farkı budur.

`PermissionService` içinde production tarafından çağrılan RoleName bağımlı
metotlar yalnız `canCreateRecord` ve `canEditOrDeleteDraft`'tır. Transition
convenience metotları testlerden çağrılır; `canReturnToBaskanYrd` hiç
çağrılmaz. `isRecordLocked`, `isCommentRequired` ve
`isCommentRequiredForReturn` da yalnız test tüketimine sahiptir. Bunlar
RoleName kaldırma blocker'ı olmamakla birlikte WF-2E öncesi redundant API
cleanup adayıdır.

## 6. Spring Security envanteri

Yedi annotation noktası toplam 12 endpoint'i korur. `hasAnyRole` yoktur.

| Dosya / endpoint | Mevcut kontrol | Gerçekte korunan şey | Önerilen authority | DB-1 kataloğu | Risk ve gerekli test |
| --- | --- | --- | --- | --- | --- |
| `FileController#uploadFiles` `POST /api/records/{id}/files` | `CALISAN` | Kaydı düzenleyebilme / dosya ekleme | `RECORD_EDIT` | Var | Record lock ve sahiplik service'te kalmalı; çalışan dışı/owner olmayan/locked testleri |
| `FileController#deleteFile` `DELETE /api/files/{id}` | `CALISAN` | Kaydı düzenleyebilme / dosya silme | `RECORD_EDIT` | Var | Dosyanın bağlı kaydı ve lock doğrulaması korunmalı |
| `RecordController#createRecord` `POST /api/records` | `CALISAN` | Kayıt oluşturma | `RECORD_CREATE` | Var | Annotation ve `PermissionService` ikili kontrolü aynı PR'da uyarlanmalı |
| `RecordController#updateRecord` `PUT /api/records/{id}` | `CALISAN` | Kayıt düzenleme | `RECORD_EDIT` | Var | Creator ve editable-status koşulları permission'dan bağımsız kalmalı |
| `RecordController#deleteRecord` `DELETE /api/records/{id}` | `CALISAN` | Taslağı silme | `RECORD_EDIT` | Var | Creator + yalnız `TASLAK` koşulu korunmalı |
| `AdminController#createUser` | Class-level `ADMIN` | Kullanıcı yönetme | `USER_MANAGE` | Var | Method security testi ve negatif `USER_VIEW` senaryosu |
| `AdminController#changeRole` | Class-level `ADMIN` | Kullanıcı rolünü değiştirme | **OPEN DESIGN QUESTION:** `USER_MANAGE`, `ROLE_MANAGE` veya ikisi | İkisi de var | Tek capability seçimi uydurulmamalı; koltuk devri testleri de çalışmalı |
| `AdminController#listUsers` | Class-level `ADMIN` | Kullanıcıları görüntüleme | `USER_VIEW` | Var | Yalnız view authority pozitif; manage olmayan kullanıcı testi |
| `AdminController#setActive` | Class-level `ADMIN` | Kullanıcı etkinliğini yönetme | `USER_MANAGE` | Var | Protected ADMIN ve deputy devir kuralları korunmalı |
| `AdminController#listRoles` | Class-level `ADMIN` | Rol kataloğunu görüntüleme | `ROLE_VIEW` | Var | `ROLE_MANAGE` gerektirmeden read-only erişim kararı doğrulanmalı |
| `AdminController#listAuditLogs` | Class-level `ADMIN` | Denetim kayıtlarını görüntüleme | **OPEN DESIGN QUESTION:** audit-view capability | Yok | `ADMIN_PANEL_ACCESS` kör karşılık değildir; katalog/seed ve iki audit tipi test edilmeli |
| `UserAuditLogController#getGecmis` | `ADMIN` | Bir kullanıcının audit geçmişini görüntüleme | **OPEN DESIGN QUESTION:** audit-view capability | Yok | Yetkisiz bilgi ifşası negatif testi zorunlu |

`AdminController` class-level `hasRole('ADMIN')` altı methodu topluca korur.
`ADMIN_PANEL_ACCESS` admin arayüzü kabuğunu açmak için kullanılabilir; tek başına
`USER_MANAGE`, `ROLE_MANAGE` veya audit okuma yetkisi anlamına gelmez. Karara
göre class-level `ADMIN_PANEL_ACCESS` ile method-level authority birlikte
istenebilir; bu karar WF-2B2 başlamadan verilmelidir.

`WorkflowActionController` üzerinde bilerek `@PreAuthorize` yoktur. Nihai
workflow kararı transition, actor role identity, required permission ve kayıt
ilişkisini birlikte doğrulamalıdır; controller'a kaba bir role annotation
eklemek doğru migration değildir.

## 7. Workflow domain envanteri

### 7.1. Rol identity zinciri

Bugünkü zincir:

```text
AuthenticatedUser.role.name
  → SecurityCurrentActorProvider
  → CurrentActor(RoleName)
  → TransitionContext.actorRole
  → TransitionRuleSource.find(..., RoleName)
  → TransitionRule.actorRole
```

Hedef zincir:

```text
Authenticated principal role_id
  → CurrentActor(RoleId, permissions/eligibility snapshot as needed)
  → TransitionContext.actorRoleId
  → TransitionRuleSource.find(..., RoleId)
  → TransitionRule.actorRoleId
```

Permission tek başına transition'a izin vermez. DB-1 gereği aktör RoleId'si,
transition `actor_role_id`, `required_permission_id` ve `actor_requirement`
birlikte doğrulanır.

`WorkflowStatusChangedListener` alıcıları `assignedTo`, record creator ve
`lastDeputyId` üzerinden çözer; event'teki `actorRole` değerini okumaz. Bu alan
notification routing için RoleId'ye dönüştürülmesi gereken bir identity değil,
önce kaldırılması değerlendirilecek redundant payload'dır.

Environment'lar arasında sayısal rol ID'leri sabit kabul edilemez. Bu nedenle
`StaticTransitionRuleSource` içine `RoleId(1)` benzeri sabitler koymak güvenli
değildir. WF-2D2 ancak DB transition reader production'a bağlandıktan ve SM-9
static/DB parity testi yeşil olduktan sonra tamamlanabilir.

### 7.2. `WorkflowAction.getExpectedTargetRole()` özel analizi

Bugünkü action metadata:

| Action | Expected target role | Çözüm davranışı |
| --- | --- | --- |
| `GONDER` | `BASKAN_YARDIMCISI` | Tek aktif rol sahibi |
| `TEKRAR_GONDER` | `BASKAN_YARDIMCISI` | Tek aktif rol sahibi |
| `BASKANA_ILET` | `BASKAN` | Tek aktif rol sahibi |
| `CALISANA_GERI_GONDER` | `CALISAN` | `records.created_by` |
| `BASKAN_YARDIMCISINA_GERI_GONDER` | `BASKAN_YARDIMCISI` | `records.last_deputy_id` |
| `ONAYLA` | `null` | Hedef yok |
| `REDDET` | `null` | Hedef yok |

Çağrı noktaları:

- Production: `WorkflowTransitionValidator#validate`, çözülen
  `TransitionContext.targetRole` ile action'ın expected role değerini karşılaştırır.
- Test: `WorkflowTransitionValidatorTest` içinde iki helper çağrısı.
- `WorkflowAction#requiresTargetUser()` doğrudan `expectedTargetRole != null`
  sonucudur.
- `WorkflowApplicationService` bu metodu üç kez kullanır: preliminary validation
  sentinel akışı, unexpected resolved target ve unexpected `NotProvided` kontrolü.
- `TargetUserResolver` `getExpectedTargetRole()` çağırmaz; aynı bilgiyi action
  switch'iyle ikinci kez kodlar.

DB-1'e uygun migration:

```text
WorkflowAction.expectedTargetRole
          │
          ▼
TransitionRule.expectedTargetRoleId + targetStrategy
          │
          ├─ TargetUserResolver transition routing'i uygular
          └─ TransitionContext çözülen targetRoleId'yi taşır
                         │
                         ▼
                saf Java validator karşılaştırır
```

WF-2D1 önce `TransitionRule`/reader'ı `expected_target_role_id`,
`target_strategy` ve DB-1'deki `required_permission_id` ile genişletmeli; resolver
transition metadata'dan sürülmelidir. `WorkflowAction.expectedTargetRole` ve
`requiresTargetUser()` ancak static/DB parity ve aynı hata sırası kanıtlandıktan
sonra kaldırılmalıdır.

### 7.3. Validator saflığı ve hata sırası

Validator repository veya Spring bağımlılığı alamaz. Bugünkü ilk kontrol
`RoleName.isWorkflowActor()` olduğu için ADMIN, terminal kayıt kontrolünden önce
`WORKFLOW_ROLE_NOT_ALLOWED` alır. DB-6 sonrasında `is_workflow_actor` verisinin
validator'a hangi saf input ile taşınacağı **OPEN DESIGN QUESTION**'dır. Olası
çözüm bir actor eligibility snapshot'ıdır; validator içinde repository sorgusu
veya rol adı karşılaştırması kabul edilmez.

## 8. System-role hardcode envanteri

| Dosya / davranış | Bugünkü hardcode | Gerçek hedef | Kategori | Not |
| --- | --- | --- | --- | --- |
| `UserService#createUser` | `findByName("CALISAN")` | `SystemRoleKey.CALISAN` lookup | C | Varsayılan hesap rolü gerçek built-in semantiktir |
| `UserService#SINGLETON_ROLES` | `ADMIN`, `BASKAN`, `BASKAN_YARDIMCISI` set'i | `roles.max_users` | C | Tekillik için üç system key switch'i yazılmamalı; sayım aktif kullanıcılarla transaction/lock içinde yapılmalı |
| `UserService#changeRole` generic hedef | Request `roleName`, `findByName(newRoleName)` | API'den RoleId | E | Yönetilebilir display name business identity değildir |
| Deputy koltuğundan ayrılma | `BASKAN_YARDIMCISI` name karşılaştırmaları | `SystemRoleKey.BASKAN_YARDIMCISI` | C | `max_users=1` tek başına replacement ve iş devri semantiğini anlatmaz |
| Replacement eligibility | Replacement rolü `CALISAN` olmalı | `SystemRoleKey.CALISAN` | C | Özel domain kuralıdır; permission değildir |
| Deputy rol lookup ve görev devri | `findByName("BASKAN_YARDIMCISI")`, `assigned_to` ve `last_deputy_id` devri | System key lookup + aynı transaction | C | `last_deputy_id` güncellemesi korunmalı |
| `UserService#setActive` ADMIN koruması | `"ADMIN".equals(role.name)` | `SystemRoleKey.ADMIN` veya açık protected-system-role policy | C | ADMIN hesabı bu uçtan pasifleştirilemiyor |
| Deputy pasifleştirme koruması | `BASKAN_YARDIMCISI` | `SystemRoleKey.BASKAN_YARDIMCISI` | C | Önce devir zorunluluğu korunmalı |
| `BootstrapAdminRunner` | Aktif ADMIN name query ve `findByName("ADMIN")` | `SystemRoleKey.ADMIN` | C | Bootstrap gerçekten built-in ADMIN semantiğidir |
| `RequestAccessEvent#adminActor` | `"ADMIN".equals(roleName)` ile audit tablosu seçimi | **OPEN DESIGN QUESTION** | C | SystemRoleKey.ADMIN ile devam mı, event türü/tablo politikası mı kullanılacağı kararlaştırılmalı |
| `RoleName#isWorkflowActor` | Sabit enum boolean | `roles.is_workflow_actor` | D | SystemRoleKey'e çevrilmemeli; rol metadata'sıdır |
| `TargetUserResolver` BASKAN/BY hedefleri | Built-in role enum'ları | Transition `expected_target_role_id` | B | Workflow target identity olduğu için SystemRoleKey değildir |

`BASKAN` için tek başına görülen singleton davranışı `max_users=1` ile
çözülür. `BASKAN` workflow hedefi ise transition RoleId'dir. Bu iki kullanımın
hiçbiri sırf ad sabit olduğu için genel bir `SystemRoleKey.BASKAN` switch'ine
dönüştürülmemelidir.

## 9. Visibility / policy envanteri

### 9.1. Mevcut görünürlük davranışı

`RecordAccessPolicy` boolean karar, `RecordSpecifications` aynı kararın JPA
predicate karşılığıdır.

| Rol | Gerçek mevcut davranış | Gelecek model girdileri |
| --- | --- | --- |
| `CALISAN` | Yalnız `currentUserId == createdBy` kayıtları | `RECORD_VIEW` + creator relationship |
| `BASKAN_YARDIMCISI` | `assignedTo == user` **veya** status `DUZENLEME_BEKLIYOR` **veya** `lastDeputyId == user` | `RECORD_VIEW` + assignee/status/previous-actor ilişkileri |
| `BASKAN` | Status `BASKAN_INCELEMESINDE`, `ONAYLANDI` veya `REDDEDILDI` **veya** `assignedTo == user` | `RECORD_VIEW` + status/assignee policy |
| `ADMIN` | Daima false; evrak göremez | ADMIN seed'inde record permission olmaması + deny policy |

Ek content/history kuralları:

- Başkan Yardımcısı için status `DUZENLEME_BEKLIYOR` ve kayıt kendisine atanmış
  değilse `seesRecordAsOfHandoff=true`: başlık/açıklama/kategori snapshot'tan,
  ekler devir anına göre, audit geçmişi son `DUZENLEME_BEKLIYOR` geçişine kadar
  gösterilir.
- Başkan için `seesHistoryFromPresidentHandover=true`: geçmiş ilk
  `BASKAN_INCELEMESINDE` geçişinden başlar.
- Bu kararları `RecordServiceImpl`, `RecordContentView`, `FileService`,
  `RecordSearchServiceImpl` ve `AuditLogController` tüketir.

### 9.2. Migration riski

`RECORD_VIEW` kaba capability kapısıdır; hangi kayıtların görülebileceğini tek
başına tanımlamaz. DB-8, rol/policy strategy ile actor relationship ve status
kısıtlarını hem in-memory boolean hem JPA predicate üretebilecek şekilde
tasarlamalıdır. Bu belgede yeni visibility scope adları uydurulmamıştır.

Kabul edilmeyen çözümler:

- `switch(roleName)` yerine `switch(roleString)` yazmak.
- Sadece `hasAuthority('RECORD_VIEW')` ekleyip record scope'u kaldırmak.
- Boolean policy'yi değiştirip `RecordSpecifications` predicate'ini eski
  bırakmak veya tersini yapmak.
- Handoff snapshot ve history clipping'i sıradan record-view ile birleştirip
  kaybetmek.

Bu alan WF-2C2 başlamadan önce **DB-8 tasarımı gerektirir**.

## 10. JWT / authentication etkisi

### 10.1. Mevcut akış

```text
JWT subject=email
  → JwtAuthenticationFilter
  → CustomUserDetailsService.loadUserByUsername(email)
  → UserRepository.findByEmail
  → AuthenticatedUser(User)
  → getAuthorities() = [ROLE_<role.name>]
```

- `CustomUserDetailsService` yalnız `User` yükler.
- `Role` entity'sinde yalnız `id`, `name`, `description` vardır; permission
  ilişkisi yoktur.
- `User.role` `@ManyToOne` default EAGER'dır, fakat permission koleksiyonu
  bulunmadığı için authority yüklenmez.
- `spring.jpa.open-in-view=false` olduğu için gelecekte lazy permission
  koleksiyonunu controller/security filter sırasında okumak güvenli değildir.
  WF-2B1 query-specific fetch join, entity graph veya projection ile aktif
  permission kodlarını authentication yüklemesi sırasında hazır etmelidir;
  global EAGER koleksiyon önerilmez.
- `JwtAuthenticationFilter` geçerli her access token isteğinde email'i çıkarır
  ve kullanıcıyı DB'den yeniden yükler. Doğru fetch uygulandığında rol/permission
  değişiklikleri eski token ömrü boyunca yetki taşımaya devam etmez.
- `AuthenticatedUser#getAuthorities()` production'da
  `JwtAuthenticationFilter` ve `MailActionTokenService` tarafından çağrılır.
  E-posta aksiyonu da aynı permission authority snapshot'ını üretmelidir.

### 10.2. Principal'da hazır RoleId

İlişkisel rol kimliği bugün de principal üzerinde mevcuttur ve production'da zaten
akmaktadır; workflow ve rbac zinciri onu kullanmayıp ad üzerinden yeniden türetir.

| Nokta | Bugünkü davranış |
| --- | --- |
| `auth/security/AuthenticatedUser.java:31` | `getRoleId()` &rarr; `user.getRole().getId()`; `getRoleName()` ile yan yana durur |
| `audit/RequestAuditFilter.java:97` | `getRoleId()` production'da **zaten** tüketilir; `audit_logs.role_id` buradan yazılır |
| `workflow/adapter/SecurityCurrentActorProvider.java` | `readRole()` elinin altındaki ID'yi kullanmaz; `getRoleName()` okuyup `RoleName.valueOf(...)` yapar |
| `audit/service/AuditLogService.java:226` | `resolveRoleId(RoleName)` her workflow audit yazımında `roleRepository.findByName(role.name())` ile DB'ye geri gider |

Sonuç, `id → name → enum → name → id` biçiminde kapanan gereksiz bir sapaktır. Aynı
değer request başına `RequestAuditFilter` yolunda ID olarak taşınırken, workflow
yolunda ada çevrilip audit yazımında repository sorgusuyla tekrar ID'ye döndürülür.

Bunun envanter açısından üç sonucu vardır:

1. **WF-2D2 sanıldığından ucuzdur.** `CurrentActor`'ı RoleId'ye taşımak yeni bir auth
   plumbing'i, yeni projection veya yeni principal alanı gerektirmez; accessor hazırdır.
   İş, `SecurityCurrentActorProvider#readRole` içindeki `valueOf` sapağını kaldırmak ve
   `CurrentActor` imzasını taşımaktır (WF2-014/WF2-015).
2. **Ölçülebilir bir kazanç vardır.** `AuditLogService#resolveRoleId` her audit yazımında
   bir `roles` sorgusu üretir. Actor RoleId doğrudan taşındığında bu lookup ve onun
   `IllegalStateException` yolu tümüyle kalkar (WF2-022).
3. **Bu, WF-2B1'in yerine geçmez.** Hazır RoleId yalnız §1'deki B kategorisini, yani
   workflow rol kimliğini çözer. Permission authority loading ayrı bir eksendir ve DB-7'ye
   bağlıdır; `getRoleId()`'nin mevcut olması `hasRole` &rarr; `hasAuthority` dönüşümünü
   kolaylaştırmaz. İki kavram bu belgede ayrı tutulmuştur.

`getRoleId()`'nin bugün var olması RoleName'i silinebilir yapmaz: `CurrentActor`,
`TransitionContext`, `TransitionRule` ve `TransitionRuleSource` halen enum taşıdığı için
WF-2D2 bloker listesi (§14.1) değişmez.

### 10.3. JWT role claim

`JwtUtil` access token'a `claim("role", roleName)` yazar. Backend'de
`extractRole` yoktur ve `JwtAuthenticationFilter` bu claim'i okumaz; server
authorization DB'den yeniden yüklenen principal'a dayanır. Frontend ve mobile
da JWT'yi decode ederek rol okumaz; `/api/users/me` içindeki `UserResponse.roleName`
değerini kullanır.

Bu nedenle claim mevcut authorization'ın doğruluk kaynağı değildir. Yine de
harici tüketici olup olmadığı doğrulanmadan aynı WF-2B PR'ında silinmemeli;
category E compatibility olarak deprecate edilmelidir.

### 10.4. Authority migration test etkisi

Özellikle aşağıdakiler güncellenmeli/eklenmelidir:

- `JwtAuthenticationFilterTest`: permission authority'lerinin authentication'a
  taşınması, pasif permission'ın dışlanması ve canlı DB reload.
- `MailActionTokenServiceTest`: mail principal'ının aynı authorities ile gerçek
  workflow validator'a ulaşması.
- `AuthorizationMatrixTest`: `@WithMockUser(roles=...)` yerine authority
  fixture'ları ve method-level capability matrisi.
- `AdminControllerTest` ile record/file/audit controller testleri: view/manage
  ayrımı ve negatif çapraz-capability senaryoları.
- Yeni auth repository testi: OSIV kapalıyken aktif permission kodları erişilebilir
  ve N+1/lazy initialization hatası yok.

WF-2B1 geçici olarak permission authority'leri ile legacy `ROLE_*` authority'leri
birlikte yayınlamalıdır. Bütün `hasRole` noktaları kalktıktan sonra WF-2B2 sonunda
legacy authority üretimi kaldırılabilir.

## 11. Frontend / mobile follow-up

Client kontrolleri güvenlik sınırı değildir; backend her işlemi yine enforce
etmelidir. Bununla birlikte role-name API kontratı değişirse bu dosyalar kırılır.

### 11.1. Runtime dosyaları

| İstemci dosyası | Bağımlılık | Alt etiket | Follow-up |
| --- | --- | --- | --- |
| `frontend/src/types/auth.ts` | Dört role kapalı union ve label map | API contract / presentation | `roleName` presentation olarak kalacaksa açık string/model; capability listesi ayrı |
| `frontend/src/schemas/admin.ts` | Assignable role enum | API contract | `/api/admin/roles` ID/name verisinden doğrulama |
| `frontend/src/api/admin.ts` | Response rolünü sabit union'a cast eder | API contract | Role ID + display name kontratı |
| `frontend/src/auth/authSession.ts` | `/me.roleName` dört sabitle doğrulanır | API contract | `/me` capability/role descriptor sözleşmesi |
| `frontend/src/App.tsx` | ADMIN route ayrımı | Authorization-benzeri navigation | `ADMIN_PANEL_ACCESS`; server enforcement zorunlu |
| `frontend/src/pages/LoginPage.tsx` | Login sonrası ADMIN redirect | Navigation | Capability-driven landing route |
| `frontend/src/pages/ForgotPasswordPage.tsx` | Oturumlu kullanıcı ADMIN redirect | Navigation | Aynı landing-route kararı |
| `frontend/src/components/layout/Sidebar.tsx` | Admin/record menüsü ve create item role göre | Authorization-benzeri UI | Permission listesiyle menü görünürlüğü |
| `frontend/src/components/layout/AppShell.tsx` | CALISAN composer, ADMIN notification görünürlüğü | Authorization-benzeri UI | `RECORD_CREATE` ve notification capability/policy |
| `frontend/src/components/records/RecordActionPanel.tsx` | Role + status ile action/edit/return hedef UI'si | Authorization-benzeri UI | Backend allowed-actions/permission modeli; yalnız sunum olarak hedef label kalabilir |
| `frontend/src/pages/RecordsPage.tsx` | CALISAN edit ve sayfa başlığı | Mixed policy/presentation | Edit capability + backend record state; label ayrı |
| `frontend/src/pages/BackendRecordEditPage.tsx` | CALISAN + status edit gate | Authorization-benzeri UI | `RECORD_EDIT` + creator/status response |
| `frontend/src/pages/RecordDetailPage.tsx` | CALISAN olmayan düzeltme ekranında polling kararı | Client behavior | Role yerine record/view state sinyali |
| `frontend/src/context/AdminContext.tsx` | CALISAN default ve deputy tekilliği | API/domain compatibility | Backend SystemRole descriptor + max-users/handoff contract |
| `frontend/src/components/admin/ChangeRoleDialog.tsx` | Dört rol option'ı, ADMIN/deputy/CALISAN kuralları | API/domain compatibility | `/roles` descriptor; backend invariant tek doğruluk kaynağı |
| `frontend/src/pages/admin/AdminUsersPage.tsx` | Role filtreleri ve ADMIN koruması | Presentation / client guard | Dynamic roles + capability; server koruması kalır |
| `frontend/src/pages/admin/AdminDashboardPage.tsx` | Dört role göre sayaç/kart | Presentation | Dynamic role listesi; system role badge gerekirse `system_key` sunumu |
| `mobile/src/api/users.ts` | `roleName` dört sabitli Zod enum | API contract | `/me` role descriptor/capabilities |
| `mobile/src/app/(app)/_layout.tsx` | CALISAN create tab'i | Authorization-benzeri UI | `RECORD_CREATE` |
| `mobile/src/app/(app)/index.tsx` | ADMIN record kapısı ve CALISAN create CTA | Authorization-benzeri UI | Record capabilities/admin panel erişimi |
| `mobile/src/app/(app)/olustur.tsx` | CALISAN create screen guard | Authorization-benzeri UI | `RECORD_CREATE`; backend enforcement |
| `mobile/src/app/(app)/kayitlar/[id].tsx` | CALISAN owner edit/action kontrolü | Authorization-benzeri UI | Allowed actions + creator/status |
| `mobile/src/components/records/RecordWorkflowActions.tsx` | Üç workflow rolü + status/owner ile action listesi | Authorization-benzeri UI | Backend transition/allowed-actions modeli |

### 11.2. Test/mock/e2e dosyaları

Frontend'te 24 test/mock/e2e dosyası, mobile'da
`mobile/src/components/records/RecordWorkflowActions.test.tsx` teknik rol
string'i taşır. Bunlar category F fixture bağımlılığıdır ve ilgili client
follow-up ile güncellenmelidir:

```text
frontend/e2e/admin-users.spec.ts
frontend/e2e/admin-role-invariants.spec.ts
frontend/e2e/global-setup.ts
frontend/src/App.test.tsx
frontend/src/api/api.test.ts
frontend/src/components/records/RecordActionPanel.test.tsx
frontend/src/components/users/UserAvatar.test.tsx
frontend/src/hooks/useRecordWorkflowAction.test.tsx
frontend/src/mocks/admin.ts
frontend/src/mocks/users.ts
frontend/src/mocks/api/auth.ts
frontend/src/mocks/api/db.ts
frontend/src/mocks/api/recordAccess.ts
frontend/src/mocks/api/handlers/adminHandlers.ts
frontend/src/mocks/api/handlers/fileHandlers.ts
frontend/src/mocks/api/handlers/recordHandlers.ts
frontend/src/mocks/api/handlers/workflowHandlers.ts
frontend/src/pages/LoginPage.test.tsx
frontend/src/pages/PasswordChangePage.test.tsx
frontend/src/pages/ProfilePage.test.tsx
frontend/src/pages/RecordDetailPage.test.tsx
frontend/src/pages/RecordFormsEdgeCases.test.tsx
frontend/src/pages/RecordsPage.test.tsx
frontend/src/pages/admin/AdminUsersPage.test.tsx
mobile/src/components/records/RecordWorkflowActions.test.tsx
```

Sayısal toplam, `rg -l` ile bulunan 24 frontend + 1 mobile dosyadır.

## 12. Migration matrix

| ID | Dosya / sembol | Mevcut bağımlılık | Kategori | Hedef model | Bloker | Önerilen PR |
| --- | --- | --- | --- | --- | --- | --- |
| WF2-001 | `RoleName` enum constants | Genel business identity rol adı | B | `RoleId` | Bütün B tüketicileri | WF-2E |
| WF2-002 | `RoleName#isWorkflowActor` | Enum boolean | D | DB `is_workflow_actor` + saf eligibility input | DB-6, validator tasarımı | WF-2D2 |
| WF2-003 | `TransitionRule#actorRole` | RoleName | B | `actorRoleId` | SM-7B | WF-2D2 |
| WF2-004 | `TransitionRuleSource#find` | RoleName key | B | RoleId key | Production DB source | WF-2D2 |
| WF2-005 | `TransitionRules` | Statik RoleName kuralları | B | DB parity oracle, sonra retirement | SM-9 green | WF-2D2 |
| WF2-006 | `StaticTransitionRuleSource` | RoleName imzalı adapter | B | DB source'a rollback sınırı | SM-9 green | WF-2D2 |
| WF2-007 | `DbTransitionRuleSource` | String → RoleName mapping | B | Reader'dan RoleId/reference | SM-7B | WF-2D2 |
| WF2-008 | `TransitionRuleRecord#actorRole` | Teknik role-name string | B | Actor role FK projection | SM-7B schema/reader | WF-2D2 |
| WF2-009 | `TransitionContext#actorRole/#targetRole` | RoleName | B | RoleId | Rule/target migration | WF-2D2 |
| WF2-010 | `WorkflowAction#expectedTargetRole` | Action-owned RoleName | B | Transition expectedTargetRoleId | SM-7B + parity | WF-2D1 |
| WF2-011 | `WorkflowAction#requiresTargetUser` | Expected role null kontrolü | B | `target_strategy != NONE` | WF2-010 | WF-2D1 |
| WF2-012 | `WorkflowTransitionValidator` lookup/target | Enum identity comparison | B | RoleId comparison | WF2-003/009/010 | WF-2D2 |
| WF2-013 | Validator actor eligibility | `isWorkflowActor()` | D | Saf eligibility snapshot | DB-6, open design | WF-2D2 |
| WF2-014 | `CurrentActor#role` | RoleName | B | RoleId | Principal role ID | WF-2D2 |
| WF2-015 | `SecurityCurrentActorProvider` | `getRoleName/valueOf`; principal'daki `getRoleId()` kullanılmıyor | B | Principal RoleId; yeni auth plumbing gerekmez (§10.2) | WF2-014 | WF-2D2 |
| WF2-016 | `WorkflowUserSnapshot#role` | RoleName | B | RoleId | User projection | WF-2D2 |
| WF2-017 | `WorkflowUserPort#findActiveByRole` | RoleName query | B | RoleId query | DB transition target | WF-2D2 |
| WF2-018 | `UserPortAdapter` | role name query/converter | B | RoleId projection/query | WF2-017 | WF-2D2 |
| WF2-019 | `TargetResolution.RoleNotConfigured` | RoleName payload | B | RoleId/target descriptor | Resolver migration | WF-2D1 |
| WF2-020 | `TargetUserResolver` | Action switch + built-in roles | B | Transition `target_strategy` | SM-7B | WF-2D1 |
| WF2-021 | `WorkflowApplicationService` target flow | Action `requiresTargetUser` | B | Transition routing metadata | WF2-010/020 | WF-2D1 |
| WF2-022 | `WorkflowTransitionAudit/AuditLogService` | RoleName → name lookup → role ID; audit yazımı başına bir `roles` sorgusu | B | Actor RoleId doğrudan; `resolveRoleId` lookup'ı tümüyle kalkar (§10.2) | WF2-014 | WF-2D2 |
| WF2-023 | `WorkflowStatusChangedEvent#actorRole` | Tüketilmeyen RoleName | F | Alanı kaldır | Listener/test doğrulaması | WF-2D2 |
| WF2-024 | `PermissionService` transition helpers | Test-only RoleName convenience API | F | Kullanılmıyorsa kaldır | UI consumer teyidi | WF-2E öncesi cleanup |
| WF2-025 | `PermissionService` create/edit | RoleName capability | A | `RECORD_CREATE/RECORD_EDIT` | DB-7 | WF-2B2 |
| WF2-026 | `RecordServiceImpl#getCurrentUserRole` | roleName converter | B | CurrentActor/role ID | WF2-014 | WF-2D2 |
| WF2-027 | `RecordServiceImpl` create/edit/delete | RoleName gate | A | Authorities + creator/status policy | WF-2B1 | WF-2B2 |
| WF2-028 | `RecordAccessPolicy` | Exhaustive RoleName switch | D | Visibility policy | DB-8 open design | WF-2C2 |
| WF2-029 | `RecordSpecifications` | Aynı switch'in JPA biçimi | D | Queryable visibility policy | WF2-028 | WF-2C2 |
| WF2-030 | `RecordContentView` | Role-specific snapshot | D | Policy decision | DB-8 | WF-2C2 |
| WF2-031 | `FileService/FileController` view paths | RoleName visibility input | D | Policy context | WF2-028 | WF-2C2 |
| WF2-032 | Record/search/audit visibility consumers | RoleName policy input | D | Ortak policy API | WF2-028/029 | WF-2C2 |
| WF2-033 | Yedi `hasRole` annotation | Role authority | A | Method-level permissions | DB-7, capability kararları | WF-2B2 |
| WF2-034 | `AuthenticatedUser#getAuthorities` | `ROLE_<role.name>` | A | Aktif `Permission.code` authorities | DB-7 | WF-2B1 |
| WF2-035 | `CustomUserDetailsService` / `Role` entity | Permission yüklemiyor | A | Query-specific active permission fetch | DB-7 | WF-2B1 |
| WF2-036 | `JwtAuthenticationFilter` | Legacy authorities tüketiyor | A | Permission authorities, per-request DB reload | WF2-034/035 | WF-2B1 |
| WF2-037 | `MailActionTokenService` | Legacy principal authorities | A | Aynı permission principal | WF2-034 | WF-2B1 |
| WF2-038 | `JwtUtil` role claim | Kullanılmayan role-name claim | E | Deprecate/compatibility kararı | External consumer kontrolü | WF-2E |
| WF2-039 | `UserService#changeRole` request | `roleName` business lookup | E | RoleId API | Client kontratı | WF-2C1 veya ayrı API PR |
| WF2-040 | `UserService#createUser` | Default CALISAN name | C | `SystemRoleKey.CALISAN` | DB-6 | WF-2C1 |
| WF2-041 | `SINGLETON_ROLES` | Üç adlık set | C | `roles.max_users` invariant | DB-6 + locking | WF-2C1 |
| WF2-042 | Deputy handoff | BY/CALISAN name checks | C | Dar SystemRoleKey semantiği | DB-6 | WF-2C1 |
| WF2-043 | ADMIN/deputy deactivation | Role-name protection | C | SystemRoleKey/policy | DB-6 | WF-2C1 |
| WF2-044 | `BootstrapAdminRunner` | ADMIN name lookup | C | `SystemRoleKey.ADMIN` | DB-6 | WF-2C1 |
| WF2-045 | `RequestAccessEvent#adminActor` | ADMIN string routing | C | System key veya event routing policy | Open design | WF-2C1 |
| WF2-046 | `UserResponse/RoleResponse/ChangeRoleRequest` | roleName API contract | E | Role descriptor/RoleId; display name kalabilir | Client rollout | Client follow-up |
| WF2-047 | Admin role filter/specification | Role-name query contract | E | RoleId filter | API compatibility | Client follow-up |
| WF2-048 | Frontend/mobile runtime | 23 dosyada teknik role conditionals | E | Capabilities + role descriptor | `/me` contract | Client follow-up |
| WF2-049 | Frontend/mobile test/mock/e2e | 25 dosyada fixture | F | Yeni contract fixture'ları | WF2-048 | Client follow-up |
| WF2-050 | `V1` role seed | Dört role-name satırı | E | Forward DB-6 backfill; V1 değişmez | DB-6 | DB-6 |
| WF2-051 | Arşiv docs/test seed script | Eski anlatım ve teknik roller | F | Arşivle/güncelle; runtime sayımından ayrı | İlgili rollout | WF-2E/docs follow-up |
| WF2-052 | `WorkflowConfiguration` source bean | Static source aktif | B | DB source production wiring | SM-7B/SM-9 | WF-2D2 |
| WF2-053 | `AuditLogResponse` ve audit roleName snapshot'ları | Tarihsel/gösterim rol adı | E | Display snapshot kalabilir; authorization identity olamaz | Audit API compatibility | WF-2E/client follow-up |
| WF2-054 | `AuthenticatedUser#getRoleName` | Security, audit ve enum converter bridge'i; `getRoleId()` ile yan yana durur | E | Ayrışma yeni accessor eklemeyi değil, çağrı yerlerini hazır `getRoleId()`'ye taşımayı gerektirir; display name presentation'da kalır | WF2-014/034 ve API compatibility | WF-2D2/WF-2E |

## 13. Önerilen WF-2 PR sırası

Tek devasa WF-2 PR yerine aşağıdaki review edilebilir sıra önerilir.

| Sıra / PR | Amaç ve production alanları | Ön koşul | Özellikle çalışacak testler | Rollback sınırı | Çakışma riski |
| --- | --- | --- | --- | --- | --- |
| 1 — WF-2B1 Authority loading | `Role`, auth repository/query, `CustomUserDetailsService`, `AuthenticatedUser`, JWT/mail principal; permission + geçici `ROLE_*` dual publish | DB-7 tabloları ve seed | Auth repository integration, `JwtAuthenticationFilterTest`, `MailActionTokenServiceTest` | Uygulama eski role authority'ye dönebilir; additive DB tabloları kalır | Auth/JPA mapping ve security fixture'ları |
| 2 — WF-2B2 Endpoint authorization | 12 endpoint için method-level `hasAuthority`, RecordService capability kontrolleri, legacy `ROLE_*` kaldırma | WF-2B1; role-change ve audit capability kararları | `AuthorizationMatrixTest`, `AdminControllerTest`, record/file/audit controller ve service testleri | Dual publish korunurken annotation'lar role modeline geri döndürülebilir | Controller'lar ve güvenlik testleri |
| 3 — WF-2C1 System role / max-users | `Role` metadata, repository, `UserService`, `BootstrapAdminRunner`, request-audit routing | DB-6; locking stratejisi; audit-routing kararı | `UserServiceTest`, bootstrap testleri, eşzamanlı max-users integration, request audit testleri | Kod eski name lookup'a dönebilir; additive kolon/backfill korunur | Role entity/repository ve admin API |
| 4 — WF-2C2 Visibility policy | `RecordAccessPolicy`, `RecordSpecifications`, content/file/history/search consumers | DB-8 policy tasarımı | `RecordSpecificationsTest`, `RecordContentViewTest`, `FileServiceAuthorizationTest`, `RecordSearchServiceImplTest`, `AuditLogControllerTest` | Eski policy adapter'ı tek parça geri alınır | Record/search/audit katmanları; client görünürlük varsayımları |
| 5 — WF-2D1 Transition-owned targeting | Rule/reader'a expected target, target strategy, required permission; resolver/application flow | SM-7B ve ilgili DB transition kolonları | `DbTransitionRuleSourceTest`, `TargetUserResolverTest`, `WorkflowTransitionValidatorTest`, `WorkflowApplicationServiceTest` | Action metadata dual-read/parity süresince fallback kalır | WorkflowAction/Rule/Resolver/Validator |
| 6 — WF-2D2 Workflow RoleId rollout | CurrentActor, context, source, snapshots, ports, audit; DB bean aktivasyonu ve static retirement | SM-9 parity green; WF-2D1; DB source hazır | Tüm workflow unit testleri, persistence integration, audit/event adapter testleri | `WorkflowConfiguration` bean'i static source'a döndürülür; DB verisi kalır | Workflow modelleri ve çok sayıda fixture |
| 7 — WF-2E RoleName deletion | Kalan converter/enum/test/docs temizliği; compatibility kararları | Sıfır production enum tüketicisi ve deletion checklist | Backend full suite + client contract testleri + zero-reference searches | PR revert enum'u geri getirir; ayrı davranış rollout'u içermemeli | Geniş test fixture ve client contracts |

WF-2B1 ile WF-2C1 DB görevleri hazırsa kısmen paralel geliştirilebilir; önerilen
merge sırası yine authority rollout'unu önce görünür kılar. WF-2E bütün önceki
kapıların ardındadır.

## 14. Blokerler

### 14.1. Dependency graph

```text
DB-7 permissions + role_permissions
        │
        ▼
WF-2B1 permission authority loading
        │
        ▼
WF-2B2 hasAuthority migration
        │
        └──── legacy ROLE_* kaldırılabilir

DB-6 roles.system_key / is_workflow_actor / max_users / is_active
        │
        ├──── WF-2C1 system-role ve max-users cleanup
        │
        └──── WF-2D2 validator eligibility girdisi

DB-8 visibility policy tasarımı
        │
        ▼
WF-2C2 RecordAccessPolicy + RecordSpecifications

SM-7B DB reader/entity + transition metadata
        │
        ▼
WF-2D1 transition-owned target routing
        │
SM-9 static/DB parity green
        │
        ▼
WF-2D2 production DB source + workflow RoleId
        │
        ▼
WF-2E RoleName enum deletion
```

### 14.2. Açık tasarım soruları

1. **Role change authority:** `AdminController#changeRole` için
   `USER_MANAGE`, `ROLE_MANAGE` veya ikisinin birlikte gerekip gerekmediği.
2. **Audit read capability:** DB-1 kataloğunda audit okuma permission'ı yok.
   `ADMIN_PANEL_ACCESS` ile eşitlemek yerine capability/seed kararı alınmalı.
3. **Validator eligibility carrier:** `roles.is_workflow_actor` saf Java
   validator'a repository bağımlılığı olmadan nasıl taşınacak?
4. **Visibility policy:** DB-8'in scope/strategy modeli boolean ve JPA predicate
   parity'sini, status ve actor relationship kurallarını nasıl üretecek?
5. **Request audit routing:** ADMIN system key ile iki tablo ayrımı kalacak mı,
   yoksa routing actor rolünden bağımsız event category'ye mi taşınacak?
6. **Client capability contract:** `/api/users/me` permission codes veya ayrı
   capability descriptor yayınlayacak mı? `roleName` yalnız presentation için
   ne kadar süre korunacak?
7. **RoleId Java biçimi:** İlişkisel integer ID'nin çıplak `Integer` yerine
   hafif `RoleId` value object ile taşınıp taşınmayacağı WF-2D API tasarımında
   kesinleştirilmeli; sayısal sabitler kullanılmamalı.

## 15. RoleName ne zaman silinebilir?

`RoleName` ancak aşağıdaki maddelerin tamamı sağlandığında silinebilir:

- [ ] DB-6 role metadata kolonları backfill edilmiş ve `name` business identity
      olarak kullanılmıyor.
- [ ] DB-7 permission ve role-permission seed'leri production'da mevcut.
- [ ] `AuthenticatedUser` aktif permission code authority'leri üretiyor.
- [ ] Production'da `hasRole`/`hasAnyRole` ve gerekli olmayan legacy `ROLE_*`
      authority kalmamış.
- [ ] Role-change ve audit-read capability açık soruları çözülmüş.
- [ ] `TransitionRuleSource#find` RoleName istemiyor.
- [ ] `TransitionRule`, `TransitionContext`, `CurrentActor`,
      `WorkflowUserSnapshot`, `TargetResolution` ve workflow portları RoleName
      taşımıyor.
- [ ] `WorkflowAction.expectedTargetRole` ve ona bağlı `requiresTargetUser`
      transition metadata ile değiştirilmiş.
- [ ] `TargetUserResolver` action-to-role switch'i yerine `target_strategy` ve
      expected target RoleId kullanıyor.
- [ ] Validator saf Java kalıyor, RoleId/permission/actor requirement birlikte
      doğrulanıyor ve mevcut hata sırası korunuyor.
- [ ] `is_workflow_actor` eligibility kontrolü rol adı veya enum olmadan yapılıyor.
- [ ] Production bean gerçek DB source'u kullanıyor ve SM-9 static/DB parity
      testi yeşil.
- [ ] `TransitionRules`/`StaticTransitionRuleSource` parity oracle/rollback
      görevini tamamladıktan sonra kaldırılmış.
- [ ] Audit yazımı actor rolünü ad üzerinden tekrar sorgulamadan RoleId ile yapılıyor.
- [ ] Redundant `WorkflowStatusChangedEvent.actorRole` kaldırılmış veya gerçek
      ihtiyaç varsa RoleId ile açıkça modellenmiş.
- [ ] `UserService` default/deputy/ADMIN davranışları SystemRoleKey, generic
      kapasite sınırı `max_users` ile çalışıyor.
- [ ] `BootstrapAdminRunner` ve request-audit routing rol adına bağlı değil.
- [ ] `RecordAccessPolicy` ve `RecordSpecifications` içinde exhaustive rol switch'i
      yok; aynı visibility davranışı iki teknik biçimde parity testli.
- [ ] Handoff snapshot, file filtering ve iki yönlü history clipping korunuyor.
- [ ] `RoleName.valueOf`, enum import'u ve business identity amaçlı dört teknik
      role-name hardcode'u production Java'da kalmamış.
- [ ] DTO/API'de kalan `roleName` alanları yalnız bilinçli presentation/compatibility
      değerleri; backend enum'una bağlı değil.
- [ ] Frontend/mobile authorization-benzeri kontroller capability/allowed-actions
      modeline geçirilmiş veya ayrı takip işiyle açıkça kabul edilmiş.
- [ ] Bütün backend test fixture'ları RoleId/permission/policy modeline taşınmış.
- [ ] Client schema/mock/e2e fixture'ları yeni kontratla uyumlu.
- [ ] Generated OpenAPI/client çıktıları yalnız API kontratı değiştiyse yeniden
      üretilmiş.
- [ ] Repo aramaları production source'ta `\bRoleName\b`, `RoleName.valueOf`,
      `hasRole(` ve business-identity rol hardcode'ları için sıfır sonuç veriyor.

## 16. Kapsam dışı

WF-2A kapsamında yapılmayanlar:

- `RoleName` silme veya yeni `RoleId`/`SystemRoleKey` sınıfı ekleme
- `hasAuthority` dönüşümü veya permission authority loading
- `TransitionRule`, `TransitionRuleSource`, `TransitionContext`, `CurrentActor`
  ya da `WorkflowAction` imza değişikliği
- DB migration, entity/repository, seed veya production bean değişikliği
- `RecordAccessPolicy`, `RecordSpecifications`, `UserService` refactor'ı
- frontend/mobile kaynak veya test değişikliği
- full Maven suite; yalnız doküman değiştiği için gerekli değildir

Arşiv dokümanlarındaki eski sayı ve öneriler kaynak doğruluk kabul edilmemiş,
repo aramaları yeniden yapılmıştır.

## 17. Sonuç

`RoleName` tek bir enum silme işi değildir. Güvenli sıra permission authority,
system-role/max-users, visibility policy, transition-owned target metadata ve
workflow RoleId rollout'unu birbirinden ayırır. En güçlü silme kapıları DB-7
authority migration'ı, DB-8 visibility tasarımı ve SM-9 static/DB parity'dir.

**WF-2A yalnız mevcut RoleName bağımlılıklarını envanterleyip sonraki migration
PR'larını tanımlar; bu commit RoleName'i veya mevcut authorization/workflow
davranışını değiştirmez.**

## 18. WF-2 rollout preflight sonucu

Bu bölüm, WF-2B → WF-2C → WF-2D → WF-2E aşamalarını tek turda uygulama talebinin
preflight sonucunu kaydeder. **Sonuç: dört aşama da bloke; hiçbir production
kodu değiştirilmemiştir.**

> ⚠️ Aşağıdaki §18.1–§18.5 tabloları **1 Eylül 2026 durumudur.** DB-1
> migration'ları ve SM-7B/SM-9 o tarihten sonra tamamlandı; güncel gate durumu
> için [§18.7](#187-gate-güncellemesi--2-eylül-2026)'ye bakınız.

| Alan | Değer |
| --- | --- |
| Preflight tarihi | 2026-09-01 |
| Dal | `feature/dynamic-workflow` |
| Preflight HEAD | `098f79e3ba9628bed4ad2955daa81dda201078d9` |
| Dal ilişkisi | Güncel `test` (`ff570563de2348c2cab684b676b7a06ce7bf49a2`) üzerine SM-7A ve WF-2A envanteri |
| Değiştirilen production dosyası | 0 |

### 18.1. Prerequisite gate tablosu

| Prerequisite | Durum | Kanıt |
| --- | --- | --- |
| DB-1 migration'ları merge edilmiş | **YOK** | `backend/src/main/resources/db/migration/` `V11`'de biter. `V12`+ hiçbir yerel veya uzak dalda bulunmaz (`main`, `test`, `v2`, `feature/notification-service`, `feature/m9-*` kontrol edildi) |
| `roles.system_key`, `is_system`, `is_workflow_actor`, `max_users`, `is_active` | **YOK** | `rbac/Role.java` yalnız `id`, `name`, `description` alanlarını taşır |
| `permissions` tablosu | **YOK** | `backend/src` içinde sıfır referans |
| `role_permissions` tablosu | **YOK** | `backend/src` içinde sıfır referans |
| Permission seed | **YOK** | Katalog yalnız `DB_1_VERI_MODELI_SOZLESMESI.md` §6.2'de metin olarak tanımlıdır |
| `workflow_statuses`, `workflow_actions`, `workflow_transitions` | **YOK** | `backend/src` içinde sıfır referans |
| `TransitionRuleRecordReader` implementasyonu | **YOK** | Port'un kendi javadoc'u SM-7A kapsamında gerçek adapter olmadığını belirtir |
| `DbTransitionRuleSource` production runtime'a bağlı | **HAYIR** | `WorkflowConfiguration#transitionRuleSource()` halen `new StaticTransitionRuleSource()` döndürür |
| SM-9 static/DB parity testi | **YOK** | `backend/src/test` altında parity testi bulunmaz |
| DB-8 visibility modeli | **TASARIM OLARAK DA YOK** | DB-1 sözleşmesinde visibility/scope bölümü yoktur; `role_visibility_scopes` repoda hiç geçmez |
| WF-2A envanteri | **VAR ve güncel** | Bu belge; §3.1 sayıları preflight sırasında birebir yeniden üretildi |

Preflight sırasında yeniden doğrulanan sayılar (§3.4 arama biçimleriyle):

```text
production \bRoleName\b dosyası      = 26
production hasRole(                  = 7
production hasAnyRole(               = 0
getExpectedTargetRole                = 3
V12+ migration (tüm dallar)          = 0
permissions|role_permissions|system_key|max_users|is_workflow_actor
  (backend/src içinde)               = 0
```

### 18.2. Aşama bazlı gate sonucu

| Aşama | Sonuç | Blocker | Neden bypass edilemez |
| --- | --- | --- | --- |
| WF-2B — permission authority | **BLOKE** | DB-7 (permission persistence + seed) | Authority'lerin okunacağı bir kaynak yoktur. Hard-code role→permission map yazmak yasaktır; `Role` entity'sinde permission ilişkisi yoktur |
| WF-2C — system role / max_users | **BLOKE** | DB-6 (`system_key`, `max_users`, `is_active`, `is_workflow_actor` kolonları) | `SystemRoleKey` ile resolve edilecek `system_key` kolonu yoktur; `max_users` invariant'ı için veri alanı yoktur. Ayrıca WF-2B verify'ı geçmemiştir |
| WF-2C — visibility policy | **BLOKE** | DB-8 (**tasarım dahil yok**) | Mevcut dört rol davranışı ancak role switch ile korunabilir; String switch'e çevirmek gizli enum üretir ve açıkça yasaktır |
| WF-2D — workflow RoleId | **BLOKE** | SM-7B (DB transition tabloları + reader adapter) ve SM-9 (parity) | `actor_role_id` / `expected_target_role_id` verisi yoktur; production kaynağı static'tir. Environment'a özel sayısal rol ID'si koda gömülemez |
| WF-2E — RoleName silme | **BLOKE** | Yukarıdakilerin tamamı | 26 production dosyası halen `RoleName`'e bağımlıdır |

DB-1 §13.2'deki güvenli rollout sırası bu sonucu doğrular: adım 1–8 tümüyle
şema, seed, entity ve parity işidir ve **hiçbiri uygulanmamıştır**; WF-2B…WF-2E
o listedeki adım 9–10'dur.

### 18.3. Migration matrisi durum işaretlemesi

§12 matrisindeki 54 satırın tamamı bu turda **`remaining`** durumundadır.
`migrated` sayısı **0**, `intentionally retained` sayısı **0**'dır.

| Bloke eden predecessor | Bekleyen WF2 ID'leri | Adet |
| --- | --- | ---: |
| DB-7 (permission persistence + seed) | WF2-025, 027, 033, 034, 035, 036, 037 | 7 |
| DB-6 (role metadata kolonları) | WF2-002, 040, 041, 042, 043, 044, 045 | 7 |
| DB-8 (visibility policy — tasarım yok) | WF2-028, 029, 030, 031, 032 | 5 |
| SM-7B (DB transition reader/metadata) | WF2-003, 004, 007, 008, 010, 011, 019, 020, 021 | 9 |
| SM-9 parity + production bean | WF2-005, 006, 052 | 3 |
| Yukarıdakilere bağlı workflow RoleId zinciri | WF2-009, 012, 013, 014, 015, 016, 017, 018, 022, 026 | 10 |
| Tümüne bağlı enum silme | WF2-001 | 1 |
| Redundant/cleanup (predecessor'a bağlı değil, WF-2E öncesi) | WF2-023, 024 | 2 |
| Client / API compatibility follow-up | WF2-038, 039, 046, 047, 048, 049, 050, 051, 053, 054 | 10 |
| **Toplam** | | **54** |

§15 silme checklist'indeki kutuların hiçbiri bu turda işaretlenmemiştir.

### 18.4. Baseline doğrulama

Kod değişmediği için bu bir regresyon kapısı değil, referans ölçümdür:

```text
cd backend && ./mvnw --batch-mode --no-transfer-progress verify
Tests run: 506, Failures: 0, Errors: 14, Skipped: 0  → BUILD FAILURE
```

14 hatanın tamamı **yerel ortam kaynaklıdır**, kod kaynaklı değildir. Kök neden:

```text
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "postgres"
```

`application.properties` integration testleri için gerçek bir PostgreSQL bekler
(`jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:workflowdb}`).
Etkilenen sınıflar yalnız DB'ye giden testlerdir:
`AuditLogRepositoryIntegrationTest`, `RecordRepositorySortingTest` ve
`WorkflowTransitionPersistenceIntegrationTest`. Kalan 492 test yeşildir.

Bu, WF-2 için **ek bir preflight bulgusudur**: DB-6/DB-7/DB-8 migration'ları
yazıldığında doğrulanabilmeleri için çalışan bir yerel PostgreSQL ve
`ddl-auto=validate` geçişi zorunludur (DB-1 §13.1). Ortam kurulumu için
`docs/TEST_ORTAMI_NOTU.md` ve `docker-compose.yml` kullanılır.

### 18.5. Blocker'ı açacak iş sırası

Aşağıdaki sıra DB-1 §13.2'den türetilmiştir; yeni iş icat edilmemiştir.

| # | İş | Kabul kriteri kaynağı |
| --- | --- | --- |
| 1 | DB-6: `roles` kolonlarını genişlet ve dört yerleşik rolü backfill et | DB-1 §6.1 tablosu ve §17 üçüncü madde |
| 2 | DB-7: `permissions` + `role_permissions` oluştur ve seed et | DB-1 §6.2/§6.3 ve §17 dördüncü madde |
| 3 | SM-7B: `workflow_statuses`, `workflow_actions`, `workflow_transitions` + entity/repository + reader adapter | DB-1 §17 beşinci ve altıncı madde |
| 4 | `records.status` sabit `CHECK` yerine katalog FK'si | DB-1 §11 zorunlu migration sırası |
| 5 | SM-9: static/DB parity testi; sekiz kural birebir eşleşmeli | DB-1 §17 sekizinci ve dokuzuncu madde |
| 6 | DB-8: visibility policy **sözleşmesi** (bu belge yazılmadan WF-2C2 planlanamaz) | Mevcut sözleşme yok; §9 davranış tablosu girdi olarak kullanılmalı |

1–2 tamamlandığında WF-2B ve WF-2C'nin system-role/max-users yarısı açılır.
3–5 tamamlandığında WF-2D açılır. 6 tamamlanmadan WF-2C'nin visibility yarısı
ve dolayısıyla WF-2E açılmaz.

### 18.6. Açık kalan tasarım soruları

§14.2'deki yedi sorunun hiçbiri bu turda kapatılmamıştır. Bunlardan üçü
DB-6/DB-7 migration'ı yazılmadan **önce** karara bağlanmalıdır: role-change
authority seçimi, audit-read capability'sinin katalogda bulunmaması ve
request-audit routing'in ADMIN system key'e bağlı kalıp kalmayacağı.

### 18.7. Gate güncellemesi — 2 Eylül 2026

§18.1 ve §18.2 tabloları **1 Eylül 2026 durumunu** kaydeder ve artık kısmen
geçersizdir. Aradan geçen iki iş turu blocker'ın büyük bölümünü kaldırdı.

| Prerequisite | 1 Eylül | 2 Eylül | Kanıt |
| --- | --- | --- | --- |
| DB-1 migration'ları | YOK | **VAR** | `V12`–`V16`; DB-1 §17'nin on beş kabul kriteri de doğrulandı |
| `roles` metadata kolonları (DB-6) | YOK | **VAR** | `V12`; dört yerleşik rol `system_key`/`max_users`/`is_workflow_actor` ile backfill edildi |
| `permissions` + `role_permissions` (DB-7) | YOK | **VAR** | `V13`; 16 permission, 20 rol-permission eşlemesi |
| Workflow katalog tabloları (SM-7B) | YOK | **VAR** | `V14`/`V15`; 6 status, 7 action, 8 geçiş |
| `TransitionRuleRecordReader` implementasyonu | YOK | **VAR** | `JpaTransitionRuleRecordReader` |
| `DbTransitionRuleSource` production'a bağlı | HAYIR | **EVET** | `WorkflowConfiguration#transitionRuleSource` |
| SM-9 static/DB parity testi | YOK | **VAR** | `TransitionRuleSourceParityTest`; 168 kombinasyon + katalog bayrakları |
| DB-8 visibility modeli | TASARIM OLARAK DA YOK | **HÂLÂ YOK** | DB-1'de visibility bölümü yok; §9.2 bu tasarımı şart koşuyor |

Aşama bazlı sonuç:

| Aşama | Yeni durum |
| --- | --- |
| WF-2B — permission authority | **AÇIK.** DB-7 tabloları ve seed'i mevcut |
| WF-2C — system role / max_users | **AÇIK.** DB-6 kolonları mevcut; WF-2B verify'ından sonra |
| WF-2C — visibility policy | **BLOKE.** Tek kalan blocker DB-8 ve tasarımı hâlâ yazılmadı |
| WF-2D1 — geçişin hedef metadata'sı | **BİTTİ.** `WorkflowAction.getExpectedTargetRole()` kaldırıldı; hedef `target_strategy`'den çözülüyor |
| WF-2D2 — workflow RoleId | **AÇIK.** Aktör ve hedef rol hâlâ `RoleName`; `system_key` üzerinden compatibility seam ile taşınıyor |
| WF-2E — RoleName silme | **BLOKE.** WF-2C visibility yarısına bağlı |

`WF-2D1` ile birlikte `RoleName` döndüren production noktası üçten ikiye indi: `WorkflowAction` artık `RoleName`'e hiç dokunmuyor. Kalanlar `TransitionRule.actorRole` / `expectedTargetRole` (WF-2D2) ve visibility policy (WF-2C2).

§18.5'teki iş sırasının 1–5. maddeleri kapandı; açık kalan tek madde 6'dır
(DB-8 visibility sözleşmesi). Bu, WF-2E'nin önündeki **tek** engeldir ve
kod değil, tasarım işidir.

Baseline ölçümü de değişti: §18.4'teki `506 test / 14 error` ölçümü, yerel
PostgreSQL'in kapalı olmasından kaynaklanıyordu. Veritabanı ayaktayken güncel
ölçüm **522 test / 0 failure / 0 error**'dur. Ölçüm alınırken `DB_PORT`
değerinin `.env` ile aynı olması gerekir; `application.properties` varsayılanı
`5432`, projenin compose dosyası ise `5433` yayınlar.

### 18.8. WF-2B ve WF-2C1 uygulama kapanışı — 2–3 Eylül 2026

İş sırası korundu: önce `docs/workflow.md` içindeki eski SM-7 anlatımı düzeltildi;
ardından WF-2B uygulandı ve tam backend `verify` **542 test / 0 failure / 0 error**
ile geçti (2 Eylül 23:31). WF-2C1 bu kapı geçildikten sonra başladı. Son tam
`verify` **592 test / 0 failure / 0 error**, paketleme dahil `BUILD SUCCESS`
ile tamamlandı (2 Eylül 23:59, Europe/Istanbul). Başlangıçtaki 532 testlik kapsam
korunarak toplam 60 senaryo eklendi. Bunlar yerel doğrulama sonuçlarıdır.

| Aşama | Güncel durum |
| --- | --- |
| SM-7 dokümantasyon düzeltmesi | **TAMAM.** Üretim DB okuma zinciri, açılış snapshot'ı, fail-fast ve statik parity referansı belgelendi |
| WF-2B — permission authorities | **TAMAM.** `V17`, ortak principal factory, 12 endpoint yetkisi, workflow permission metadata'sı ve audit aktör ID'leri uygulandı |
| WF-2C1 — system_key / max_users | **TAMAM.** `SystemRoleKey`, `roleId` uyumluluğu, rol ID'sine göre aktif kullanıcı kapasitesi ve transaction kilitleri uygulandı |
| WF-2D1 — hedef metadata'sı | **KORUNDU.** Hedefler geçişin `target_strategy`/`expected_target_role_id` metadata'sından çözülür |
| WF-2D2 — workflow RoleId | **KAPSAM DIŞI.** Workflow aktör/hedef rolleri hâlâ `RoleName`; dönüşümler `system_key` okur |
| WF-2C2 / DB-8 — görünürlük modeli | **BLOKE.** Ayrı tasarım kararı gerekir |
| WF-2E — RoleName tamamen silme | **BLOKE.** WF-2D2 ve görünürlük çalışması tamamlanmalıdır |

Kabul edilen kararlar:

- `V17`: `FILE_MANAGE` ve `RECORD_DELETE` → `CALISAN`; `AUDIT_VIEW` → `ADMIN`.
  Seed eşlemeleri gösterilen rol adına değil `system_key` değerine dayanır.
- Rol atama dahil kullanıcı yönetimi `USER_MANAGE` ister. Ek `ROLE_MANAGE` veya
  `ADMIN_PANEL_ACCESS` koşulu yoktur. Diğer endpoint eşlemeleri `workflow.md`'dedir.
- Principal yalnız aktif rolün aktif permission kodlarını değişmez kümede taşır.
  `ROLE_<rol adı>` yayını yoktur. JWT her istekte DB'den okunur; e-posta aksiyonları
  aynı factory'yi kullanır. Pasif rol erişim sağlayamaz; global EAGER koleksiyon eklenmedi.
- Workflow validator'ının dördüncü kontrolü kayıt ilişkisiyle birlikte geçişin
  permission'ını doğrular; eksiklik `WORKFLOW_FORBIDDEN` döndürür. Önceki hata
  öncelikleri korunur ve ret halinde hedef sorgusu/yazım yapılmaz. Eksik aktif
  geçiş permission metadata'sı açılışı durdurur; statik/DB parity metadata'yı kapsar.
- `roleId` pozitif olmalıdır; eski `roleName` API sınırında ID'ye çözülür. İkisinden
  tam biri gerekir; ikisi birlikte veya ikisi de eksikse 400 döner. Pasif rol
  atanamaz. Web/mobilin mevcut `roleName` gönderimleri ve yanıt alanları korunur.
  OpenAPI snapshot'ı gerçek Springdoc çıktısından, ilgili web tipi aynı snapshot'tan güncellendi.
- `max_users=NULL` sınırsızdır. Sayım yalnız aktif kullanıcıları rol ID'sine göre
  kapsar. Mevcut kullanıcılar UUID, ardından roller ID sırasıyla kilitlenir.
  Oluşturma, bootstrap, rol değişimi, etkinleştirme ve devir ortak kapasite kontrolündedir.
  Devirde net kapasite hesaplanır; rol/kayıt/audit yazımları birlikte commit veya rollback olur.
  Limit aşımında `409 ADMIN_LIMIT_EXCEEDED` korunur.
- Request audit dağılımı `ADMIN` **sistem anahtarı** için `audit_logs`, diğerleri
  için `user_audit_logs` olarak kalır. Kullanıcı yönetimi ve kayıt yaşam döngüsü
  audit'i dinamik rollerde enum dönüşümü yapmaz. Dinamik workflow/görünürlük ayrı kapsamdır.

Yeni doğrulamalar; 12 endpoint'in doğru/boş/farklı authority matrisi, eski JWT'den
yetki kaldırma, pasif rol/permission, kapalı OSIV, gerçek e-posta validator akışı,
dinamik aktörün servis ve audit işlemleri, yeniden adlandırılmış sistem rolleri,
1/2/sınırsız kapasite, pasif kullanıcı sayımı, eşzamanlı atama/etkinleştirme ve
kayıt devrinden sonraki audit hatasında tüm işlemin geri alınmasını kapsar.
DB testlerinden önce Compose ve çalışan Docker PostgreSQL incelendi;
`workflow-db` sağlıklı ve yerel port `.env` ile uyumlu **5433** olarak doğrulandı.
Web istemcisinde güncellenen rol atama tipiyle TypeScript kontrolü ve Vite
üretim derlemesi (`npm run build`) başarılıdır.

Uygulama referansları: [Spring method security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html),
[Spring Data JPA kilitleme](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).
