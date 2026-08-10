# Eksik Sınıflar ve Öncelik Sırası

**Tarih:** 10 Ağustos 2026
**Kapsam:** Yalnızca backend — frontend hariç
**Kaynak:** `integration/tum-feature-branchleri` dalı (mevcut: 55 sınıf, 112 test)

Backend'de yazılması gereken **50 yeni sınıf**, sahipleriyle birlikte bağımlılık sırasına
dizilmiş hâlde. Sıra keyfî değil: her faz bir sonrakinin önünü açıyor.

> Sınıf adları öneridir; paket yerleşimi projenin modül bazlı yapısına uyar.

---

## Sıra neden böyle

**İki sınıf bütün projeyi bekletiyor.** Biri JWT doğrulama filtresi: o gelmeden hiçbir
istekte "bu işlemi kim yapıyor" sorusu cevaplanamıyor. Diğeri kayıt repository'si: o
olmadan onay akışı kayda erişemiyor, arama modülü de sorgulayacak bir şey bulamıyor.

**İki modül ise bugün beklemeden başlayabilir.** Denetim izi ve bildirim modüllerinin
bağlanacağı arayüzler (`AuditService`, `WorkflowEventPublisher`) zaten yazılmış durumda.
Ebrar ve Melih kimseyi beklemeden kendi sınıflarını yazabilir.

**Veritabanı hiçbir fazı bekletmiyor.** Dokuz tablo, indeksleri ve kısıtlarıyla hazır;
yazılacak entity'lerin tamamının karşılığı şemada mevcut.

### İşaretler

| İşaret | Anlamı |
|---|---|
| 🟢 **Yeni** | Sıfırdan yazılacak sınıf |
| 🟡 **Değişiklik** | Mevcut sınıfa ekleme |
| 🔴 **Silinecek** | Kaldırılması gereken |

---

## Faz 1 — Kilidi açanlar

> Diğer her şey bunları bekliyor. Bu fazda üç ekip paralel çalışabilir.

### Nisan Tat · Sümeyye Baykan — `auth` / `user`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `auth/security/JwtAuthenticationFilter` | 🟢 Yeni | Her istekte Bearer token'ı okur, doğrular ve SecurityContext'i doldurur. **Projedeki en kritik eksik.** |
| `auth/security/AuthenticatedUser` | 🟢 Yeni | `UserDetails` implementasyonu; kullanıcı ID'sini ve rolünü taşır. Onay akışının aktörü buradan okuyacak. |
| `auth/service/CustomUserDetailsService` | 🟢 Yeni | E-postadan kullanıcıyı DB'den yükler. Şu an bu sınıf olmadığı için Spring'in bozuk varsayılan kullanıcısı devrede. |
| `auth/dto/RefreshTokenRequest` | 🟢 Yeni | Ham `String` gövde yerine tipli istek. `LogoutRequest` ile birlikte. |
| `user/entity/User` | 🟡 Değişiklik | `is_active`, `must_change_password`, `updated_at` alanları şemada var ama entity'de yok. **`is_active` olmadan onay akışı pasif kullanıcıya kayıt atanmasını engelleyemez.** |
| `user/repository/UserRepository` | 🟡 Değişiklik | Role ve aktiflik durumuna göre kullanıcı sorgusu — onay akışının hedef çözümlemesi için gerekli. |

### Alperen Kara · Fevzi Berke Urganioğlu — `record`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `record/repository/RecordRepository` | 🟢 Yeni | `JpaRepository` + `JpaSpecificationExecutor` — ikincisi arama modülü için şart. |
| `record/service/RecordService` | 🟢 Yeni | Oluşturma, düzenleme, taslak silme (soft delete), "kayıtlarım" listesi. |
| `record/controller/RecordController` | 🟢 Yeni | CRUD uçları. Onay akışı uçları ayrı controller'da kalacak. |
| `record/dto/CreateRecordRequest`<br>`record/dto/UpdateRecordRequest`<br>`record/dto/RecordResponse`<br>`record/dto/RecordSummaryResponse` | 🟢 Yeni | Entity dışa açılmamalı; liste ve detay için ayrı yanıt tipleri. |
| `record/entity/Category`<br>`record/repository/CategoryRepository`<br>`record/controller/CategoryController`<br>`record/dto/CategoryResponse` | 🟢 Yeni | Kategori yönetimi. `categories` tablosu hazır, tarafta hiç kod yok. |
| `record/entity/Record` | 🟡 Değişiklik | `status` alanı `String` yerine `@Enumerated(EnumType.STRING)` ile durum makinesi enum'una bağlanmalı. |
| `record/entity/RecordStatus` | 🔴 Silinecek | Durum makinesindeki enum'un kopyası; kilitleme kurallarını taşımıyor ve hiçbir yerde kullanılmıyor. |

### Hacer Bengü Ünal — `common`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `common/exception/GlobalExceptionHandler` | 🟢 Yeni | `@RestControllerAdvice`. **Erken yazılmalı:** herkes controller yazmadan önce hata formatı belli olsun. |
| `common/exception/ApiError` | 🟢 Yeni | Standart hata gövdesi — kod, mesaj, alan hataları, zaman damgası. |
| `common/exception/ResourceNotFoundException`<br>`common/exception/BusinessRuleException`<br>`common/exception/ForbiddenException` | 🟢 Yeni | Ortak hata tipleri. Şu an her modül `RuntimeException` fırlatıyor, hepsi 500 dönüyor. |

---

## Faz 2 — Onay akışını sisteme bağlama

> Faz 1'den sonra. Onay akışı çekirdeği hazır ve 93 testle doğrulanmış; eksik olan
> yalnızca dış dünyaya bakan beş adaptör.

### Esra Öncü · Burak Kaya — `workflow`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `workflow/adapter/RecordPortAdapter` | 🟢 Yeni | `WorkflowRecordPort` implementasyonu — `RecordRepository`'yi bekliyor. |
| `workflow/adapter/UserPortAdapter` | 🟢 Yeni | `WorkflowUserPort` implementasyonu — kullanıcıyı ve role göre aktif kullanıcıları çözer. |
| `workflow/adapter/SecurityCurrentActorProvider` | 🟢 Yeni | `CurrentActorProvider` implementasyonu — `JwtAuthenticationFilter`'ı bekliyor. |
| `workflow/adapter/SpringWorkflowEventPublisher` | 🟢 Yeni | `WorkflowEventPublisher` implementasyonu — durum değişikliğini Spring event'i olarak yayınlar, bildirim modülü dinler. **Başka modüle bağımlı değil, hemen yazılabilir.** |
| `workflow/controller/WorkflowActionController` | 🟢 Yeni | Mevcut `WorkflowActionApi` arayüzünü uygulayan somut controller. |
| `WorkflowApplicationService`<br>`TargetUserResolver`<br>`WorkflowTransitionValidator` | 🟡 Değişiklik | Spring bean'i değiller; `@Service` / `@Component` eklenmeli. **Bu da beklemiyor.** |

### Hacer Bengü Ünal — `rbac`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `rbac/RoleName` | 🟢 Yeni | Ortak rol tipi. Şu an onay akışı kendi geçici tanımını taşıyor ve Javadoc'unda bu devrin planlandığı yazılı. |
| `rbac/service/RecordAccessPolicy` | 🟢 Yeni | Şartnamedeki kayıt görünürlük kapsamı: Çalışan yalnızca kendi kayıtları, Bşk. Yrd. kendisine gelenler, Başkan onay aşamasındakiler. |
| `rbac/config/MethodSecurityConfig` | 🟢 Yeni | `@EnableMethodSecurity` — yetki matrisinin `@PreAuthorize` ile uygulanabilmesi için. |
| `rbac/config/SecurityConfig` | 🟡 Değişiklik | Şu an her istek `permitAll` — geçici. Gerçek kurallar ve JWT filtresi zincire eklenmeli. |

### Ebrar Şeyma Karakuş — `audit` · **bugün başlayabilir**

| Sınıf | Tür | Açıklama |
|---|---|---|
| `audit/service/AuditLogService` | 🟢 Yeni | Onay akışının `AuditService` portunu doldurur. Arayüz hazır, beklemeye gerek yok. |
| `audit/entity/AuditLog`<br>`audit/repository/AuditLogRepository` | 🟢 Yeni | Silinemez işlem kaydı. `audit_logs` tablosu ve üç indeksi hazır. |
| `audit/controller/AuditLogController`<br>`audit/dto/AuditLogResponse` | 🟢 Yeni | Kayıt detayındaki işlem geçmişi tablosunu besleyen uç. |
| `audit/entity/UserAuditLog`<br>`audit/repository/UserAuditLogRepository`<br>`audit/service/UserAuditLogService` | 🟢 Yeni | Kullanıcı ve rol değişikliklerinin ayrı izi. `user_audit_logs` tablosu hazır. |

---

## Faz 3 — Bağımsız modüller

> Bildirim modülü bugün başlayabilir; arama modülü `RecordRepository`'yi beklemek zorunda.

### Melih Kocaman — `notification` · **bugün başlayabilir**

| Sınıf | Tür | Açıklama |
|---|---|---|
| `notification/listener/WorkflowStatusChangedListener` | 🟢 Yeni | Durum değişikliği event'ini dinler, bildirim ve e-postayı tetikler. |
| `notification/service/MailService` | 🟢 Yeni | `JavaMailSender` ile Outlook/SMTP gönderimi, `@Async`. Docker'daki Mailpit ile denenebilir. |
| `notification/entity/Notification`<br>`notification/repository/NotificationRepository`<br>`notification/service/NotificationService` | 🟢 Yeni | Uygulama içi bildirimler. `notifications` tablosu ve okunmamış indeksi hazır. |
| `notification/controller/NotificationController`<br>`notification/dto/NotificationResponse` | 🟢 Yeni | Bildirim listeleme ve okundu işaretleme. |
| `templates/mail/*.html` | 🟢 Yeni | Kayıt özeti, güncel durum, son açıklama ve kayda giden derin bağlantı butonu. Klasör açılmış, boş. |

### Irmak Tanrıverdi — `search`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `search/specification/RecordSpecifications` | 🟢 Yeni | Duruma, kategoriye, kullanıcıya, tarihe ve metne göre dinamik kriterler. |
| `search/dto/RecordSearchCriteria` | 🟢 Yeni | Filtre parametrelerini taşıyan tip. |
| `search/service/RecordSearchService` | 🟢 Yeni | Sayfalama ile arama. **Yetki kapsamı burada uygulanmalı** — kimse yetkisi olmayan kaydı sonuçta görmemeli. |
| `search/controller/RecordSearchController`<br>`common/dto/PagedResponse` | 🟢 Yeni | Arama ucu ve ortak sayfalama yanıtı. |

### Ecesu Başak — `attachment`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `attachment/service/FileContentValidator` | 🟢 Yeni | Gerçek içerik doğrulaması. Şu an yalnızca istemcinin gönderdiği `Content-Type`'a güveniliyor. |
| `attachment/service/FileService` | 🟡 Değişiklik | Silme işlemi; kilitli veya terminal durumdaki kayda ek eklenmesini engelleme; `uploadedBy`'ı istek parametresi yerine oturumdan alma. |
| `application.properties` | 🟡 Değişiklik | `spring.servlet.multipart.max-file-size` — şu an dosya boyutu sınırı yok. |

---

## Faz 4 — Teslim öncesi kalite

> Modüller bittikçe. Şartname değerlendirmeyi yalnızca işlevselliğe değil kod kalitesine
> de bağlıyor. Bu maddeler tek kişinin değil, her modül sahibinin kendi alanında yapacağı iş.

| İş | Tür | Açıklama |
|---|---|---|
| `*ServiceTest` · `*ControllerTest` | 🟢 Yeni | Şu an 112 testin 93'ü onay akışında. Diğer yedi modülün kritik iş kuralları test edilmemiş. |
| `logback-spring.xml` | 🟢 Yeni | Structured log formatı. Projede tek bir `Logger` tanımı yok — *Ebrar* |
| Tüm `*Request` DTO'ları | 🟡 Değişiklik | `@Valid`, `@NotBlank`, `@Email` doğrulamaları. Onay akışı dışında hiçbir istekte doğrulama yok. |
| Entity dönen uçlar | 🟡 Değişiklik | Katmanlı mimari kriteri: hiçbir controller entity dönmemeli. Yanıt DTO'ları tamamlanmalı. |

---

## Kişi başı dağılım

| Sorumlu | Paket | Yeni sınıf | İlk faz |
|---|---|---:|---|
| Nisan Tat · Sümeyye Baykan | `auth`, `user` | 5 | Faz 1 |
| Alperen Kara · Fevzi B. Urganioğlu | `record` | 11 | Faz 1 |
| Hacer Bengü Ünal | `common`, `rbac` | 8 | Faz 1 |
| Esra Öncü · Burak Kaya | `workflow` | 5 | Faz 2 |
| Ebrar Şeyma Karakuş | `audit` | 8 | Hemen |
| Melih Kocaman | `notification` | 7 | Hemen |
| Irmak Tanrıverdi | `search` | 5 | Faz 1 sonrası |
| Ecesu Başak | `attachment` | 1 | Faz 3 |
| **Toplam** | | **50** | |
