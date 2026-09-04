# İnceleme kanıtı ve tekrar üretim — 4 Eylül 2026

İncelenen commit: `c9b029700e644dad2075896195b10b4c4059230c`, dal: `codex/ap-2-frontend-uyum`.

[Ana rapor](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/PROJE_INCELEME_RAPORU_2026-09-04.md), [test kaynağı](C:/Users/burak/Desktop/projects/WorkFlowProject/docs/reviews/2026-09-04/ReviewRegressionProbeTest.java).

Bu kaynak, doğru olması beklenen davranışı kontrol eden inceleme problarıdır. İncelenen commit'te aşağıdaki sekiz kontrol başarısız oldu; bunlar düzeltilmiş uygulama veya hazır bir kalıcı regresyon paketi değildir. Kalıcı teste dönüştürülürken fixture temizliği, hata sözleşmesi ve düzeltmeden sonra beklenen transaction sonuçları ayrıca düzenlenmelidir. Kaynak normal Maven test ağacının dışında tutulur.

## Doğrulanmış sonuçlar

| Bulgu | Test metodu | Gözlenen sonuç | Koşum |
|---|---|---|---|
| B01 | `quickActionTokenMustPersistAfterWorkflowCommit` | Commit sonrası token sayısı 0; beklenen 1. Listener logunda aktif transaction yok hatası | 1 |
| B08 | `deletedDraftMustRejectUpdate` | Silinmiş taslak güncellemesi hata vermedi | 1 |
| B07 | `frozenListedAttachmentMustRemainDownloadable` | Listede bulunan tarihsel ek download'da ResourceNotFoundException verdi | 1 |
| B04 | `fileUploadMustNotCommitAfterRecordLeavesEditableStatus` | Kayıt incelemeye geçtikten sonra yükleme 1 dosya satırı commit etti | 1 |
| B05 | `concurrentRefreshMustOnlyConsumeOldTokenOnce` | Aynı eski token için 2 başarılı yenileme; beklenen 1 | 1 |
| B06 | `frozenRecordSearchMustNotMatchHiddenLiveContent` | Yalnız güncel açıklamadaki sözcük, snapshot ile sınırlı aktörün aramasında eşleşti | 2 |
| B03 | `reassignmentMustInvalidateInFlightWorkflowSnapshot` | Devir commit'inden sonra eski workflow snapshot'ı çatışmasız yazıldı | 2 |
| B02 | `dynamicDepartmentForwardMustSupportReturnToPreviousActor` | Dinamik üyeden Başkana iletme başarılı; önceki aktöre dönüş WORKFLOW_TARGET_ROLE_INVALID | 3 |

Üç koşumun Maven sonuçları sırasıyla `Tests run: 5, Failures: 5, Errors: 0`, `Tests run: 2, Failures: 2, Errors: 0` ve `Tests run: 1, Failures: 1, Errors: 0`. Testler tek bir sekizli koşum olarak raporlanmamıştır. Eşzamanlılık kontrolleri rastgele bekleme yerine latch/barrier kullanır. Dosya yarışında diğer transaction'ın durum değişikliği SQL ile; görev devri yarışında üretimdeki iki repository metodu ile uygulanır. Mail, push ve dosya depolama servisleri mock'tur; bu problar dışarıya ileti/dosya göndermez.

## Tekrar çalıştırma

Java 21, Maven ve PostgreSQL 15 kullanıldı. Veritabanı testlerinden önce Docker Compose dosyaları ve çalışan konteynerler tekrar incelenmelidir. Bu oturumda `wf-scratch` konteynerinin PostgreSQL servisi `127.0.0.1:5434` üzerindeydi. İsim/port eşleşmesini doğrulamadan aşağıdaki ortam değerleri başka bir ortama taşınmamalıdır.

Yalnız inceleme için **yeni ve boş** `workflow_review_20260904` veritabanı oluşturulur. Problar fixture verisini kendi başına silmez; kullanılacak veritabanında korunacak veri bulunmamalıdır. Testte DB_NAME kontrolü bulunur; bu kontrol tek başına veritabanının boş veya doğru sunucuda olduğunu kanıtlamaz. Spring başlangıcındaki migration'lar bu kontrolden önce çalışabilir.

Örneğin bu oturumda doğrulanan konteyner için:

```powershell
$reviewDocker = 'C:\Users\burak\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe'
& $reviewDocker ps --format '{{.Names}} {{.Ports}}'
& $reviewDocker exec wf-scratch createdb -U postgres workflow_review_20260904
```

`createdb` zaten mevcut veritabanı hatası verirse onu silerek veya üstüne yazarak devam etmeyin; önce içeriğin/sahibin incelemeye ait olduğunu doğrulayın. Bu incelemenin sonunda oluşturulan veritabanı kaldırılmıştır.

Repo kökünde, normal test ağacında aynı isimli bir dosyanın bulunmadığını doğrulayarak kaynağı geçici olarak kopyalayın:

```powershell
$reviewRoot = 'C:\Users\burak\Desktop\projects\WorkFlowProject'
$reviewSource = Join-Path $reviewRoot 'docs\reviews\2026-09-04\ReviewRegressionProbeTest.java'
$reviewTarget = Join-Path $reviewRoot 'backend\src\test\java\btk\staj\WorkFlowProject\ReviewRegressionProbeTest.java'
if (Test-Path -LiteralPath $reviewTarget) { throw 'Hedef test zaten mevcut; üzerine yazılmadı.' }
Copy-Item -LiteralPath $reviewSource -Destination $reviewTarget
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '5434'
$env:DB_NAME = 'workflow_review_20260904'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'postgres'
$env:JWT_SECRET = 'review-only-test-jwt-secret-not-for-production-20260904'
Set-Location -LiteralPath (Join-Path $reviewRoot 'backend')
```

İlk koşumdaki beş test:

```powershell
mvn -o '-Dmaven.repo.local=C:/Users/burak/.m2/repository' --batch-mode --no-transfer-progress '-Dtest=ReviewRegressionProbeTest#quickActionTokenMustPersistAfterWorkflowCommit+deletedDraftMustRejectUpdate+frozenListedAttachmentMustRemainDownloadable+fileUploadMustNotCommitAfterRecordLeavesEditableStatus+concurrentRefreshMustOnlyConsumeOldTokenOnce' test
```

İkinci ve üçüncü koşum:

```powershell
mvn -o '-Dmaven.repo.local=C:/Users/burak/.m2/repository' --batch-mode --no-transfer-progress '-Dtest=ReviewRegressionProbeTest#frozenRecordSearchMustNotMatchHiddenLiveContent+reassignmentMustInvalidateInFlightWorkflowSnapshot' test
mvn -o '-Dmaven.repo.local=C:/Users/burak/.m2/repository' --batch-mode --no-transfer-progress '-Dtest=ReviewRegressionProbeTest#dynamicDepartmentForwardMustSupportReturnToPreviousActor' test
```

`-o` bu makinedeki mevcut Maven önbelleğini kullanır; farklı makinede bağımlılıklar önce sağlanmalıdır. Hata çıktılarında altyapı/başlangıç hatası ile tabloda belirtilen assertion sonuçları ayrılmalıdır. İş bittiğinde yalnız kopyalanan test kaynağını ve onun derlenmiş sınıfını kaldırın; aksi halde sonraki standart test koşumuna katılabilir. Yalnız bu çalışma için oluşturulduğu doğrulanan veritabanını kaldırın. Ana rapor ve kanıt kaynağı korunmalıdır.

## Standart doğrulama

Ek test kaynağı eklenmeden önce aynı ayrı DB üzerinde backend `mvn -o ... verify` koşumu **776/776** geçti ve JAR üretildi. Frontend `npm test`, `npm run lint`, `npm run build`, `npm run typecheck:e2e` başarılı; test sayısı **126**. Mobil lint/typecheck başarılı, son `npm test -- --runInBand` turu **64/64**. İlk mobil turdaki tek 5 saniye zaman aşımı hedefli ve tam tekrarda geçmiştir; kalıcı uygulama hatası olarak sınıflanmadı.

Playwright için repo kökünde `docker compose -p workflow-review-e2e -f docker-compose.e2e.yml up -d --build --wait` ile ayrı servisler başlatıldı. Frontend klasöründe `E2E_PROVISION_USER=true` ile `npm run test:e2e` çalıştırıldı: **15/15** geçti. Backend portu 18080, Mailpit 18025, web test sunucusu 5174. Eksik Chromium headless shell, `PLAYWRIGHT_BROWSERS_PATH` repo içindeki `tmp/review-playwright` olacak şekilde `npx playwright install chromium --only-shell` ile sağlandı. İlk tarayıcı başlatma hataları bu başarılı koşumdan ayrı tutuldu. E2E'nin tamamlanması dinamik organizasyon zincirini veya gerçek mail hızlı işlem kabulünü kanıtlamaz; ana rapordaki kapsam boşlukları geçerlidir.

Canlı OpenAPI'nin 32 yol ve 37 DTO alan kümesi sürümlenmiş dosyayla karşılaştırıldı. Bu kontrol tüm OpenAPI semantiği için tam bir diff değildir.

## Yerel log envanteri

Loglar repo kökündeki Git tarafından yok sayılan `tmp/` altında tutulur; repo kopyalanınca kendiliğinden taşınmaz. Sürümlenebilir kanıt test kaynağı ve bu özettir.

| Kanıt | Dosya |
|---|---|
| Backend temel paket | `tmp/review-backend-verify.log` |
| Beş ilk prob | `tmp/review-regression-probes.log` |
| İki ek prob | `tmp/review-additional-probes.log` |
| Dinamik geri dönüş probu | `tmp/review-department-return-probe.log` |
| Web test/lint/build/typecheck | `tmp/review-frontend-{test,lint,build,e2e-types}.log` |
| Mobil ilk/tekrar/son test | `tmp/review-mobile-{test,retry,final}.log` |
| Mobil lint/typecheck | `tmp/review-mobile-{lint,types}.log` |
| Başarılı E2E | `tmp/review-e2e-final.log` |
| E2E kurulum/ilk tarayıcı hatası | `tmp/review-e2e-start.log`, `tmp/review-e2e.log`, `tmp/review-browser-install.log` |
| Canlı OpenAPI | `tmp/review-live-openapi.json` |

## Ortamın bırakıldığı durum

Geçici test kaynağı normal test ağacından, ilgili derlenmiş sınıf da test çıktısından çıkarıldı. İncelemeye ait E2E Compose projesi ve inceleme veritabanı temizlendi. Önceden çalışan `workflow-db`, `wf-scratch`, `workflow-backend` ve `workflow-mailpit` konteynerleri korunmuştur. Uygulama koduna düzeltme yapılmadı; dış plan belgeleri değiştirilmedi.
