# WF-2D2 — Workflow RoleId geçişi

> **Tarihsel rollout kaydı.** PR #61/#60 `test`'e birleşmiştir. Aşağıdaki PR 1/2
> anlatımı ve 614/626/639 test sayıları o aşamaların kanıtıdır. WF-2C2 ile
> görünürlük daha sonra RoleId/ortak scope'a taşındı; dinamik roller artık
> RECORD_VIEW ve creator/direct assignee ilişkisiyle okuyabilir.
> [Güncel görünürlük sözleşmesi](WF2C2_DB8_GORUNURLUK_SOZLESMESI.md) ve
> [teslim durumu](README.md) esas alınmalıdır.

## PR 1: okuma yolu

`TransitionRuleRow` ve `TransitionRuleRecord`, `actor_role_id` ve
`expected_target_role_id` değerlerini doğrudan taşır. `DbTransitionRuleSource`
bunları pozitif değer zorunluluğu olan saf Java `RoleId` tipine çevirir.
Value object, rol ID'sinin başka bir sayısal kimlikle karışmasını önler.
Geçersiz kimlik hatası alanı ve satır numarasını bildirir.

Aktör tarafının davranışı bu aşamada korunur. `TransitionRule`, ID'lerin
yanında geçici `RoleName` alanları taşır. `LegacyWorkflowRoleMapping`, her
snapshot yüklemesinde yerleşik rollerin gerçek ID'lerini `system_key` ile
eşler. Dinamik aktör ve hedefler ikinci PR'a kadar desteklenmez. Reload
başarısız olursa çalışan snapshot korunur.

`TransitionRules.all(roleIds)` ve `StaticTransitionRuleSource(roleIds)`
eşlemeyi çağırandan alır; production kodunda sayısal rol sabiti yoktur.
Parity testi eşlemeyi PostgreSQL'den `system_key → id` olarak okur.
Veritabanısız testler `WorkflowRoleFixtures` içindeki ortak sentetik
eşlemeyi kullanır. Statik tablo TZ-1 ile test ağacına taşındı ve parity referansı olarak orada
kalır; production artifact'ında yer almaz ve sentetik ID'lerle production
fallback olarak kullanılamaz.

## PR 2: aktör zinciri

Aktör, context, hedef, permission, audit ve event modelleri RoleId taşır.
Geçici çift taşıma ve `LegacyWorkflowRoleMapping` kaldırılmıştır. Güvenlik
adaptörü principal'ın hazır rol ID'sini okur; `system_key = NULL` artık
workflow reddi üretmez. Dinamik roller tanımlı geçiş, gerekli permission,
aktörlük bayrağı ve kayıt ilişkisi sağlandığında aksiyon alabilir.

Hedef sorgusu rol ID'si, aktif kullanıcı ve aktif rol üzerinden çalışır.
`ROLE` stratejisi tam bir aktif kullanıcı ister; hedefin workflow aktörü
olması gerekmez. Audit yazımı aktörün rol ID'sini doğrudan kullanır ve rol
repository'sine yeniden gitmez. Olay payload'ı da aynı ID'yi korur.

Validator'a zaten taşınan DB kaynaklı `workflowActor` boolean korunur.
Validator saf Java kalır; hata sırası ve hedef stratejileri değişmez.
Audit yazımı ID'yi doğrudan kaydeder; görünürlük ve geçmiş filtreleri bu
işin dışındadır. HTTP sözleşmesi ve Flyway migration'ları değişmez.

### Görünürlük sınırı — WF-2D2 teslim anı

`AuditLogController` ve `RecordSearchServiceImpl` de eski `CurrentActor`
modelini tüketiyordu. Bu iki okuyucu `CurrentVisibilityActorProvider` ve
`VisibilityActor` üzerinden aynı sistem rolü kimliğini alır. Güvenlik
adaptöründeki eski görünürlük rol çözümü bu sınıra taşınmıştır; dinamik rol
bu okuyucularda önceki `WORKFLOW_ROLE_NOT_ALLOWED` hatasıyla reddedilir.
Workflow aktörü ise yalnız RoleId taşır. Bu ayrım görünürlük kurallarını
ID'ye dönüştürmeden derleme bağımlılığını çözer.

`RecordAccessPolicy`, `RecordSpecifications`, `RecordServiceImpl`,
`RecordContentView`, dosya görünürlüğü ve audit geçmiş filtreleri değişmedi.
Ayrımın HTTP regresyon testi de dinamik workflow yetkisinin arama/geçmiş
kapsamını genişletmediğini doğrular.

## Doğrulama

3 Eylül 2026, Git Bash ve backend Maven wrapper ile:

| Aşama | Test | Failure | Error | Skipped |
|---|---:|---:|---:|---:|
| Başlangıç | 614 | 0 | 0 | 0 |
| PR 1 | 626 | 0 | 0 | 0 |
| PR 2 | 639 | 0 | 0 | 0 |

API değişikliklerinden sonra `./mvnw --batch-mode clean test-compile` geçti.
Her iki aşamada `DB_PORT=5433 ./mvnw --batch-mode clean verify` çalıştırıldı;
test dışlanmadı veya atlanmadı. Öncesinde Compose ve çalışan konteynerler
kontrol edildi; PostgreSQL `127.0.0.1:5433` üzerinden erişildi.

- `DynamicWorkflowRoleIntegrationTest`: 11 gerçek PostgreSQL/HTTP testi.
  İki `system_key = NULL` rol, gerçek JWT filtresi ve permission okumasıyla
  geçişi tamamlar. Durum, atama, sürüm, audit aktör rol ID'si ve event ID'si
  doğrulanır. Eksik permission/aktörlük/kayıt ilişkisi/geçiş, pasif hedef
  kullanıcı/rol, birden fazla hedef ve yanlış hedef rolü reddedilir.
- Yeni testler transaction sonunda rollback olur; `@AfterTransaction`
  kural snapshot'ını yeniden yükleyip başlangıçtaki içerikle karşılaştırır.
- `WorkflowTransitionPersistenceIntegrationTest`: mevcut sekiz geçiş,
  audit hatasında rollback ve sürüm çatışmasında 409 kanıtları geçti.
- Aynı sayısal ID'li farklı nesnelerin aktör ve hedef eşleşmesi ile farklı
  ortam eşlemeleri test edildi. Parity bütün kural alanlarını, tüm
  durum–aksiyon–rol kombinasyonlarını ve reload sonuçlarını karşılaştırır.
- Geçersiz `actor_role_id`, gerçek DB'de `fk_transition_actor_role`
  tarafından reddedildi.
- Yeni backend imajı oluşturulup başlatıldı; `/actuator/health` → `UP`.
- Ayrı, yeni bir geçici DB önce migration'larla açıldı ve `UP` oldu.
  Ardından yalnız bu DB'de `CREATOR` satırının hedef rolü boşaltıldı.
  Uygulama çıkış kodu 1 ile açılmayı reddetti:
  `Inconsistent transition configuration at row 5: targetStrategy CREATOR requires expectedTargetRoleId`.
  Geçici konteyner ve veritabanı kaldırıldı; normal backend sağlıklı kaldı.

## Tarihsel teslim sırası

İlk dal `feature/wf-2d2-role-id` (PR #61), ikinci dal
`feature/wf-2d2-role-id-actors` (PR #60) idi. İkinci dal ilk aşamaya bağlı
hazırlandı; her iki PR `test`'e birleşti. Bu sıra yeniden açılacak iş listesi değildir.

Eski sürüme geri dönüşten önce dinamik rol kullanan aktif geçişler eski
sürümle uyumlu hale getirilmelidir; eski sürüm bu satırlarla açılmaz.
