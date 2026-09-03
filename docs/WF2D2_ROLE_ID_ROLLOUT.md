# WF-2D2 — Workflow RoleId geçişi

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
eşlemeyi kullanır. Statik tablo TZ-1'e kadar referans olarak kalır;
sentetik ID'lerle production fallback olarak kullanılamaz.

## PR 2: aktör zinciri

İlk PR `test` dalına merge edildikten sonra aktör, context, hedef, permission,
audit ve event modelleri RoleId'ye geçirilecektir. Geçici çift taşıma ve
eşleme kaldırılacak; dinamik roller gerekli permission, aktörlük bayrağı
ve kayıt ilişkisi sağlandığında aksiyon alabilecektir.

Validator'a zaten taşınan DB kaynaklı `workflowActor` boolean korunur.
Validator saf Java kalır; hata sırası ve hedef stratejileri değişmez.
Audit yazımı ID'yi doğrudan kaydeder; görünürlük ve geçmiş filtreleri bu
işin dışındadır. HTTP sözleşmesi ve Flyway migration'ları değişmez.

## Doğrulama

Başlangıç: 614 test, 0 failure, 0 error, 0 skipped.
API değişikliklerinde `clean test-compile`, her PR kabulünde
`DB_PORT=5433 ./mvnw --batch-mode clean verify` çalıştırılır.
Öncesinde Docker Compose ve çalışan PostgreSQL konteyneri kontrol edilir.

İkinci PR'ın kabul kanıtı, gerçek HTTP güvenlik zinciri üzerinden dinamik
aktör ve hedefle başarılı geçiş; doğru atama ve audit rol ID'sidir.
Mevcut sekiz geçiş, transaction rollback ve sürüm çatışması testleri korunur.
Geçersiz FK'nin DB tarafından reddi ve geçici DB'deki eksik hedef metadata'sının
uygulama açılışını durdurması ayrı ayrı doğrulanır.

Eski sürüme geri dönüşten önce dinamik rol kullanan aktif geçişler eski
sürümle uyumlu hale getirilmelidir; eski sürüm bu satırlarla açılmaz.
