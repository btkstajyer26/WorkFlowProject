# Sistem Mimarisi

Bu belge İş Akışı ve Onay Yönetim Sistemi'nin **çalışan mimarisini** tanımlar. Hedef durumu değil, koda bakılarak doğrulanmış mevcut yapıyı anlatır.

> Son kod doğrulaması 31 Ağustos 2026 tarihinde `test` dalının `4491a80` commit'i üzerinde yapılmıştır. Modül sınırları, katmanlama veya bağımlılık yönü değiştiğinde bu belge aynı değişiklik kapsamında güncellenmelidir.

## İçindekiler

- [Sistem bağlamı](#sistem-bağlamı)
- [Backend modül sınırları](#backend-modül-sınırları)
- [Katmanlama kuralları](#katmanlama-kuralları)
- [Workflow çekirdeği: port ve adapter](#workflow-çekirdeği-port-ve-adapter)
- [Kesişen konular](#kesişen-konular)
- [Veri ve dosya yönetimi](#veri-ve-dosya-yönetimi)
- [Yerel çalışma topolojisi](#yerel-çalışma-topolojisi)
- [Güvenlik sınırları](#güvenlik-sınırları)
- [Bilinen mimari boşluklar](#bilinen-mimari-boşluklar)

## Sistem bağlamı

```mermaid
flowchart TB
    U[Kullanıcı] --> UI[React 19 + TypeScript web istemcisi]
    U --> MOBILE[Expo SDK 57 mobil istemcisi]
    UI -->|REST/JSON + JWT| API[Spring Boot 4.1 REST API]
    MOBILE -->|REST/JSON + JWT| API
    API --> DB[(PostgreSQL 15)]
    API --> FS[(Dosya sistemi - uploads)]
    API --> SMTP[Mailpit yerel / Outlook SMTP hedef]
    API --> FCM[FCM HTTP v1]
```

Web istemcisi ile sunucu ayrı origin'lerde çalışır; erişim `CORS_ALLOWED_ORIGINS`
ile açıkça listelenen origin'lere sınırlıdır (`common/config/CorsConfig`). Mobil
istemci native HTTP kullanır. Kimlik doğrulama JWT taşıyıcı token ile yapılır,
oturum durumu sunucuda tutulmaz; yalnız refresh token'ları `tokens` tablosunda
izlenir ve iptal edilebilir.

## Backend modül sınırları

Ana paket `btk.staj.WorkFlowProject`. On modülün tamamı işlevseldir.

| Modül | Sınır | Dışa açtığı |
| --- | --- | --- |
| `auth` | Giriş, token yenileme, çıkış, parola değiştirme | `/api/auth/**`, `JwtAuthenticationFilter` |
| `user` | Kullanıcı oluşturma, rol atama, aktiflik, koltuk devri | `/api/admin/**`, `/api/users/me` |
| `rbac` | Rol tanımı, işlem yetkisi, kayıt görünürlük politikası, güvenlik yapılandırması | `RecordAccessPolicy`, `PermissionService`, `SecurityConfig` |
| `record` | Kayıt ve kategori yaşam döngüsü, taslak, soft delete | `/api/records`, `/api/categories` |
| `workflow` | İzinli durum geçişleri, hedef çözümleme, atama | `/api/records/{id}/workflow/actions`, `RecordStatus` |
| `attachment` | Dosya içerik doğrulama, saklama, erişim | `/api/records/{id}/files`, `/api/files/**` |
| `audit` | Değiştirilemez işlem geçmişi (kayıt ve kullanıcı) | `/api/audit-logs/**`, `/api/user-audit-logs/**` |
| `search` | Kriter tabanlı filtreleme, sayfalama, görünürlük kapsamı | `RecordSearchService`, `RecordSpecifications` |
| `notification` | Uygulama içi bildirim, cihaz tokenı, FCM push ve güvenli e-posta hızlı işlem | `/api/notifications/**`, `/api/device-tokens`, `/api/public/mail-actions/**` |
| `common` | Ortak hata sözleşmesi, sayfalama DTO'su, CORS | `ApiError`, `GlobalExceptionHandler`, `PagedResponse` |

İki sınır kararı ayrıca not edilmelidir:

- **Kayıt durumunu yalnız `workflow` değiştirir.** `record` modülü `status` alanına yazmaz; CRUD yalnız içerik alanlarını günceller.
- **Listeleme ve görünürlük kapsamı tek yerdedir.** `RecordController.getAllRecords` kendi filtre mantığını tutmaz; `RecordSearchService`'e devreder. Aynı erişim kuralının iki yerde yazılıp birinin unutulmasını önlemek için bu bilinçli bir tercihtir.

## Katmanlama kuralları

```text
controller -> service/domain -> repository
                    │
                    ├── audit
                    └── notification
```

- Controller katmanı yalnız HTTP sözleşmesi, doğrulama ve yanıt eşlemesiyle ilgilenir.
- İş kuralları ilgili servis veya domain katmanında tutulur.
- Yetki ve kayıt görünürlüğü backend'de uygulanır; istemci kontrolleri güvenlik sınırı değildir.
- Dış modüller başka bir modülün repository katmanına doğrudan bağlanmak yerine o modülün servis sınırını kullanır.
- Ortak hata yanıtları ve çapraz kesen yapılandırmalar `common` altında tutulur.

`workflow` bu şemanın tek istisnasıdır ve aşağıda ayrıca anlatılır.

## Workflow çekirdeği: port ve adapter

Onay akışı, projenin iş kuralı yoğunluğu en yüksek parçasıdır. Bu yüzden çekirdeği altyapıdan ayrılmıştır: **durum makinesi ve uygulama servisi Spring'i, JPA'yı ve HTTP'yi bilmez.**

```mermaid
flowchart TB
    subgraph HTTP
        C[WorkflowActionController]
        TX[WorkflowActionService - transaction sınırı]
    end
    subgraph Cekirdek["Çekirdek — saf Java, anotasyonsuz"]
        APP[WorkflowApplicationService]
        RES[TargetUserResolver]
        VAL[WorkflowTransitionValidator]
        RULES[TransitionRules - merkezî geçiş tablosu]
    end
    subgraph Portlar["port/ — çekirdeğin tanımladığı arayüzler"]
        P1[WorkflowRecordPort]
        P2[WorkflowUserPort]
        P3[AuditService]
        P4[WorkflowEventPublisher]
        P5[CurrentActorProvider]
    end
    subgraph Adapterler["adapter/ — altyapı uygulamaları"]
        A1[RecordPortAdapter - JPA]
        A2[UserPortAdapter - JPA]
        A3[AuditLogService - audit modülü]
        A4[SpringWorkflowEventPublisher]
        A5[SecurityCurrentActorProvider]
    end

    C --> TX --> APP
    APP --> RES
    APP --> VAL --> RULES
    APP --> P1 & P2 & P3 & P4 & P5
    P1 -.-> A1
    P2 -.-> A2
    P3 -.-> A3
    P4 -.-> A4
    P5 -.-> A5
```

Bağımlılık yönü kritik: **oklar çekirdekten dışarı, kesikli oklar dışarıdan içeri bakar.** Portları çekirdek tanımlar, altyapı uygular. Bu, bağımlılığı tersine çevirir ve çekirdeğin veritabanı, Spring context'i veya HTTP olmadan test edilmesini sağlar.

Yapının üç somut sonucu:

| Karar | Sonuç |
| --- | --- |
| Çekirdek sınıfları `@Service` taşımaz | `new` ile örneklenip test edilir; bean tanımları `WorkflowConfiguration`'da dışarıdan yapılır |
| Bütün geçişler `TransitionRules.RULES` listesinde | Yeni geçiş eklemek tek satır eklemektir; `if/else` zinciri yoktur |
| Transaction sınırı ayrı bir sınıfta (`WorkflowActionService`) | Çekirdek Spring bilmediği için transaction'ı kendisi açamaz; kayıt güncellemesi ve audit yazımı ya birlikte olur ya hiç olmaz |

Bir geçişin sırası: aktörü oku → kaydı bul → hedefi çöz → **validator'a sor** → kaydı güncelle → audit yaz → olay yayınla. Bütün kural kararları tek noktada, validator'da verilir; servis katmanında hiçbir geçiş kuralı tekrarlanmaz.

Ayrıntı için [workflow.md](workflow.md).

## Kesişen konular

| Konu | Uygulama | Not |
| --- | --- | --- |
| Hata yönetimi | `common/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) | Tüm hatalar tek `ApiError` biçiminde döner; workflow hata kodları burada HTTP durumlarına eşlenir |
| Kimlik doğrulama | `auth/security/JwtAuthenticationFilter` | Pasif hesabı ve parola değişimi bekleyen kullanıcıyı zincirin başında durdurur |
| Yetkilendirme | `@PreAuthorize` + `RecordAccessPolicy` | Workflow ucunda rol kontrolü bilinçli olarak controller'da değil durum makinesindedir |
| Denetim izi | `AuditLogService`, workflow transaction'ı **içinde** | Geçiş geri alınırsa audit satırı da geri alınır |
| Uygulama içi bildirim | `@EventListener`, transaction **içinde** | Geçişle birlikte yazılır veya hiç yazılmaz |
| E-posta | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | Geri alınabilir bir işlem için dışarıya e-posta çıkmasın diye commit sonrası; gönderim best-effort |
| Push | Aynı `AFTER_COMMIT` listener içinde `PushNotificationService` | `recipientsOf` alıcı matrisi kullanılır; FCM yapılandırılmamışsa workflow push olmadan devam eder |
| E-posta hızlı işlem | `mail_action_tokens` + `/api/public/mail-actions/preview` ve `/consume` | Anahtar süreli, tek kullanımlık ve alıcı/kayıt/aksiyona bağlıdır; preview mutasyon yapmaz |
| Loglama | `logback-spring.xml` | Konsola düz metin, dosyaya ECS şemasında JSON (`StructuredLogEncoder`); `dev` profilinde DEBUG, diğerlerinde INFO |

## Veri ve dosya yönetimi

- PostgreSQL şemasının tek otoritesi Flyway'dir; şema Hibernate tarafından üretilmez.
- Hibernate `ddl-auto=validate` ile yalnız entity–şema uyumunu doğrular. Uyumsuzluk uygulamayı açılışta durdurur.
- Open Session in View kapalıdır; gerekli ilişkiler servis işlemi içinde yüklenmelidir.
- Yüklenen dosyalar veritabanında ikili veri olarak tutulmaz. Disk üzerinde GUID tabanlı `stored_name` ile saklanır, kullanıcıya gösterilen `original_name` ve diğer metadata veritabanındadır.
- Dosya türü istemcinin gönderdiği `Content-Type` başlığına değil, Apache Tika ile **içerikten** tespit edilen türe göre doğrulanır.
- Kayıt silme soft delete'tir (`records.deleted_at`); workflow ve okuma uçları silinmiş kaydı `404` sayar.

Şema ayrıntıları için [database.md](database.md).

## Yerel çalışma topolojisi

Docker Compose varsayılan olarak üç servis başlatır:

| Servis | Bağımlılık / veri | Port |
| --- | --- | --- |
| `db` | `db-data-pg15` volume | `5432` |
| `mailpit` | Yerel SMTP yakalayıcı | `1025`, Web UI `8025` |
| `backend` | Sağlıklı `db`, `uploads` volume | Temel dosyada `0.0.0.0:8080` (LAN + localhost); TEST overlay'inde host portu kaldırılır |

Frontend servisi `frontend` profili arkasındadır ve `docker compose --profile frontend up` ile başlatılır. Uygulama `VITE_API_BASE_URL` üzerinden gerçek backend'e bağlanır; MSW yalnızca Vitest testlerinde kullanılır.

## Güvenlik sınırları

Aşağıdakiler uygulanmış davranışlardır:

- Yeni kullanıcıları yalnız Admin oluşturur ve **her hesap daima Çalışan rolüyle başlar**; başlangıç rolü dışarıdan seçilemez (`UserService.createUser`).
- Başkan Yardımcısı, Başkan ve Admin rolleri yalnız ayrı ve audit'lenen bir Admin işlemiyle (`changeRole`) atanır.
- Bu üç rol **tekildir**: aynı anda yalnız bir aktif kullanıcı tutabilir.
- Pasif tekil rol sahibi yeniden etkinleştirilirken de `ensureSingletonRoleAvailable` çalışır; aynı rolde başka aktif kullanıcı varsa yazma işlemi reddedilir.
- Admin rolü tek başına iş akışı kayıtlarına erişim vermez; `RecordAccessPolicy` Admin için boş kapsam üretir.
- Nihai onay ve ret yalnız Başkan tarafından, yalnız kendisine atanmış kayıtta yapılabilir.
- Admin hesabı aktiflik ucundan pasifleştirilemez (`UserService.setActive`).
- Parolalar yalnız tek yönlü hash ile saklanır; sırlar ortam değişkenlerinden okunur, repository'ye yazılmaz.
- İlk Admin yalnız `BOOTSTRAP_ADMIN_EMAIL` ve `BOOTSTRAP_ADMIN_PASSWORD` birlikte verildiğinde **ve sistemde aktif Admin yokken** oluşturulur; hesap parola değiştirme zorunluluğuyla açılır.

## Bilinen mimari boşluklar

- **Son Admin'in rolü korunmuyor.** `setActive` Admin hesabının pasifleştirilmesini engelliyor, ancak `changeRole` sistemdeki tek Admin'in rolünü başka bir role çevirmeyi engellemiyor. Tekil rol kontrolü yalnız bir role *girerken* çalışıyor, *çıkarken* değil. Sistem yönetimsiz kalabilir.
- **Audit append-only kuralı veritabanında zorlanmıyor.** Uygulama güncelleme veya silme ucu sunmuyor, fakat DB trigger'ı ya da rol kısıtı yok.
- **E-posta teslim garantisi yok.** Gönderim asenkron ve best-effort; retry, outbox veya DLQ bulunmuyor.
- **Bu belgedeki kararların çoğu ADR olarak kaydedilmedi.** `decisions/` altında iki ADR var (modül bazlı paketleme, mobil istemci teknolojisi); ancak port/adapter sınırı, tekil rol modeli ve enum tabanlı durum kolonu kararları yalnız bu belgede anlatılıyor, ayrı birer ADR'leri yok.

Dinamik workflow/rol kaynakları ve WebSocket bildirim kanalı bu çalışan mimarinin
parçası değildir; gelecek çalışma olarak planlanmaktadır.
