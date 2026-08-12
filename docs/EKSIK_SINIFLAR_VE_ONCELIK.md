# Eksik Sınıflar ve Öncelik Sırası

**Tarih:** 12 Ağustos 2026
**Kapsam:** Yalnızca backend — frontend hariç
**Kaynak:** `integration/tum-feature-branchleri` dalı (mevcut: 116 sınıf, 274 test)

Bir önceki sürüm 10 Ağustos'ta 50 eksik sınıf sayıyordu. O tarihten bu yana
`auth`, `user`, `record`, `common`, `rbac`, `audit` modülleri, onay akışının
adaptörleri, onay akışının uca bağlanması, bildirim ve arama modülleri
tamamlandı. Yeni sınıf listesi bitti; **kalan iş yalnızca teslim öncesi
kalite maddeleri**.

12 Ağustos'ta `feature/github-ci`, `test`, `feature/record`,
`feature/proje-altyapisi`, `feature-ebrar` ve `feature/nisan-sumeyye`
dalları entegre edildi: `attachment` ve `record` oturum kimliği sorunları
kapandı, GitHub Actions CI eklendi, `auth` modülü test kapsamına girdi,
DTO doğrulaması ve kullanıcı/rol yönetimi tamamlandı.

> `AuditLogRepositoryIntegrationTest` ve `WorkFlowProjectApplicationTests`
> ayakta bir PostgreSQL ister; ikisi de yalnızca CI'da (veya
> `docker compose up db` ile) çalışır.

> Sınıf adları öneridir; paket yerleşimi projenin modül bazlı yapısına uyar.

---

## İşaretler

| İşaret | Anlamı |
|---|---|
| 🟢 **Yeni** | Sıfırdan yazılacak sınıf |
| 🟡 **Değişiklik** | Mevcut sınıfa ekleme |
| 🔴 **Sorun** | Kaldırılması / düzeltilmesi gereken |

---

## Faz 1 — Teslim öncesi kalite

> Yeni modül kalmadı. Buradan sonrası şartnamenin §6.2 kod kalitesi
> kriterleri; her modül sahibi kendi alanında yapar.

| İş | Sorumlu | Açıklama |
|---|---|---|
| `common/config/CorsConfig` 🟢 | *Hacer* | **Projede hiç CORS yapılandırması yok.** Frontend `5173`, backend `8080` — ayrı origin. Frontend şu an mock veriyle çalıştığı için sorun görünmüyor; ilk gerçek istekte her uç tarayıcıda bloklanır. İzinli origin ortam değişkeninden okunmalı, `*` verilmemeli (kimlik doğrulamalı istekler için zaten geçersiz). **Entegre edilen altı dalın hiçbirinde yok; Faz 1'in tek açık işlevsel maddesi.** |
| `templates/mail/*.html` 🟢 | *Melih* | `MailService` ~130 satırlık HTML'i metin bloğu olarak içinde taşıyor. Şablon dosyaya çıkarılmalı (thymeleaf bağımlılığı eklenecek). İşlevsel bir eksik değil, Clean Code maddesi. |
| `record` · `common` testleri 🟢 | *ilgili sahipler* | 274 testin çoğu `workflow`, `rbac`, `audit`, `search` ve `auth`'ta. Bu iki modülün hiç testi yok. |

---

## 10 Ağustos'tan bu yana tamamlananlar

| Modül | Durum |
|---|---|
| `common` — hata yönetimi | ✅ `GlobalExceptionHandler`, `ApiError`, üç hata tipi |
| `auth` / `user` | ✅ JWT filtresi, `AuthenticatedUser`, `CustomUserDetailsService`, entity alanları |
| `record` — CRUD | ✅ Repository, service, controller, DTO'lar, kategori yönetimi |
| `rbac` | ✅ `RecordAccessPolicy`, `MethodSecurityConfig`, `SecurityConfig`, yetki matrisi |
| `audit` | ✅ Tamamı; onay akışı portuna bağlandı, silinemezlik ve görünürlük kapsamı uygulandı |
| Loglama | ✅ `logback-spring.xml`, ECS JSON structured format |
| `workflow` | ✅ Dört adaptör, `WorkflowActionController`, transaction sınırı, bean yapılandırması |
| Onay akışının uca bağlanması | ✅ Paralel `RecordWorkflowController` kaldırıldı; tüm geçişler durum makinesinden geçiyor |
| `notification` | ✅ Uygulama içi bildirim, `@Async` e-posta, derin bağlantı; dinleyici gerçek workflow event'ine bağlandı |
| `search` | ✅ Dinamik filtreleme, sayfalama; kapsam gerçek oturumdan ve `RecordAccessPolicy` ile hizalı |
| `attachment` — oturum kimliği | ✅ `FileController` `uploadedBy`/`deletedBy` yerine `@AuthenticationPrincipal` kullanıyor; kullanıcı başkası adına yükleyip silemiyor |
| `record` — oturum kimliği | ✅ Sahte UUID kaldırıldı. Oturum `SecurityContextHolder`'dan okunuyor, `getRecordById` `RecordAccessPolicy.assertCanView` ile korunuyor, listeleme sorgusuna rol bazlı kapsam predicate'i eklendi, `RuntimeException`'lar tipli exception'larla değiştirildi |
| `auth` — hata kodu | ✅ Hatalı giriş 500 yerine 401 dönüyor (`InvalidCredentialsException` → `INVALID_CREDENTIALS`) |
| `auth` — testler | ✅ 26 test: `AuthControllerTest`, `AuthServiceTest`, `JwtAuthenticationFilterTest`, `CustomUserDetailsServiceTest`. Testler gerçek `GlobalExceptionHandler` üzerinden `ApiError` sözleşmesini doğruluyor |
| CI | ✅ GitHub Actions: PostgreSQL'li backend `mvn verify` + frontend lint/test/build; Maven Wrapper sabitlendi |
| `pom.xml` | ✅ Duplike `spring-boot-starter-web` ve ikinci `maven-compiler-plugin` tanımı kaldırıldı |
| DTO doğrulama | ✅ `auth` ve `user` DTO'larına `@NotBlank`/`@Email`/`@Size`; `spring-boot-starter-validation` eklendi |
| `user` — rol yönetimi | ✅ Hesaplar daima Çalışan rolüyle açılıyor; rol değişimi ayrı uçta (`PATCH /api/admin/users/{id}/role`), tek aktif Admin kuralıyla |
| `auth` — pasif hesap | ✅ Pasif hesap doğru parolayla giremiyor; geçerli token da kimlik doğrulamış saymıyor |
| İlk Admin | ✅ `BootstrapAdminRunner`; yalnızca `BOOTSTRAP_ADMIN_EMAIL` ve `BOOTSTRAP_ADMIN_PASSWORD` açıkça verildiğinde ve aktif Admin yokken çalışır, varsayılan parola yok |

İki not:

- Önceki listedeki `rbac/RoleName` maddesi farklı çözüldü — ayrı bir rol tipi
  eklenmedi, `workflow/statemachine/RoleName` projenin tek rol tipi olarak
  benimsendi ve `rbac` de onu kullanıyor.
- Önceki listede "onay akışı sınıflarına `@Service` eklenmeli" yazıyordu; bu
  yanlıştı. `WorkflowApplicationService` ve durum makinesi bilerek anotasyonsuz
  tutuluyor ki Spring olmadan test edilebilsinler. Bean tanımları dışarıdan
  `WorkflowConfiguration` içinde yapıldı, transaction sınırı ise ayrı bir
  `WorkflowActionService` ile sağlandı.

---

## Kişi başı kalan iş

| Sorumlu | Paket | Kalan sınıf | Öncelik |
|---|---|---:|---|
| Hacer | `common/config` | `CorsConfig` | **Sıradaki iş** |
| Melih Kocaman | `templates/mail` | şablon dosyaları | Teslim öncesi |
| Herkes | kendi modülü | `record` ve `common` testleri | Teslim öncesi |
| Nisan · Sümeyye | `user` / `auth` | — | ✅ tamamlandı |
| Ecesu Başak | `attachment` | — | ✅ tamamlandı |
| Irmak Tanrıverdi | `search` | — | ✅ tamamlandı |
| Esra Öncü · Burak Kaya | `workflow` | — | ✅ tamamlandı |
| Alperen Kara · Fevzi B. Urganioğlu | `record` | — | ✅ tamamlandı |
| **Toplam yeni sınıf** | | **1** | |
