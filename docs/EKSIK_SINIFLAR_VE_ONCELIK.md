# Eksik Sınıflar ve Öncelik Sırası

**Tarih:** 13 Ağustos 2026
**Kapsam:** Yalnızca backend — frontend hariç
**Kaynak:** `integration/tum-feature-branchleri` dalı (mevcut: 128 sınıf, 289 test)

Bir önceki sürüm 10 Ağustos'ta 50 eksik sınıf sayıyordu. Yeni sınıf listesi
uzun süredir bitmiş durumda. 13 Ağustos'ta CORS eklenerek **Faz 1'in son
işlevsel maddesi de kapandı**; geriye yalnızca iki Clean Code / test kalitesi
maddesi kaldı.

13 Ağustos'ta `test`, `feature/record` ve `feature/frontend-uygulamasi`
dalları entegre edildi: kayıt listeleme tek uca indirildi (görünürlük kuralı
artık tek yerde), çalışma notu modülü geri alındı (sözleşme zaten kapsam
dışı bırakmıştı), Başkan Yardımcısı pasifleştirme kuralı sözleşmeyle
hizalandı, frontend gerçek API adreslerine (`/api/v1` öneksiz) geçti.

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

> Yeni modül kalmadı, işlevsel eksik kalmadı. Buradan sonrası şartnamenin
> §6.2 kod kalitesi kriterleri.

| İş | Sorumlu | Açıklama |
|---|---|---|
| `templates/mail/*.html` 🟢 | *Melih* | `MailService` hâlâ ~170 satırlık HTML'i metin bloğu olarak içinde taşıyor. Şablon dosyaya çıkarılmalı (thymeleaf bağımlılığı eklenecek). İşlevsel bir eksik değil, Clean Code maddesi. |
| `common` testleri 🟢 | *ilgili sahipler* | 289 testin çoğu `workflow`, `rbac`, `audit`, `search`, `record` ve `auth`'ta. `common` paketinin (GlobalExceptionHandler, ApiError vb.) hiç testi yok. |
| `user` / `auth` test boşlukları 🟡 | *Nisan · Sümeyye* | Uçlar tamamlandı ama üç dar test eksik: aynı e-postayla ikinci kullanıcı denemesinin 409 döndüğü, pasif kullanıcının **elindeki geçerli token'ının** da reddedildiği (`JwtAuthenticationFilter`), `UserService.setActive`/`changeRole` için ayrı bir servis testi (şu an yalnız controller/integration seviyesinde dolaylı kapsanıyor). |

---

## 10 Ağustos'tan bu yana tamamlananlar

| Modül | Durum |
|---|---|
| `common` — hata yönetimi | ✅ `GlobalExceptionHandler`, `ApiError`, tipli hata sınıfları |
| `auth` / `user` | ✅ JWT filtresi, `AuthenticatedUser`, `CustomUserDetailsService`, entity alanları |
| `record` — CRUD | ✅ Repository, service, controller, DTO'lar, kategori yönetimi |
| `rbac` | ✅ `RecordAccessPolicy`, `MethodSecurityConfig`, `SecurityConfig`, yetki matrisi |
| `audit` | ✅ Tamamı; onay akışı portuna bağlandı, silinemezlik ve görünürlük kapsamı uygulandı |
| Loglama | ✅ `logback-spring.xml`, ECS JSON structured format |
| `workflow` | ✅ Dört adaptör, `WorkflowActionController`, transaction sınırı, bean yapılandırması, `WORKFLOW_VERSION_CONFLICT` hata sözleşmesi |
| Onay akışının uca bağlanması | ✅ Paralel `RecordWorkflowController` kaldırıldı; tüm geçişler durum makinesinden geçiyor |
| `notification` | ✅ Uygulama içi bildirim, `@Async` e-posta, derin bağlantı, zamanlanmış token temizliği (`TokenCleanupJob`) |
| `search` + `record` — birleşik listeleme | ✅ İki ayrı uç (`/api/records`, `/api/records/search`) tek uçta (`GET /api/records`) toplandı; görünürlük kuralı artık `RecordSearchService`'te tek yerde |
| `attachment` — oturum kimliği ve adres | ✅ `@AuthenticationPrincipal` kullanıyor; yükleme ucu sözleşmeye uygun `POST /api/records/{id}/files` |
| `record` — oturum kimliği | ✅ Sahte UUID kaldırıldı, `RecordAccessPolicy.assertCanView` ile korunuyor, tipli exception'lar kullanılıyor |
| `auth` — hata kodu | ✅ Hatalı giriş 401 (`INVALID_CREDENTIALS`), token rotation, `/api/auth/change-password` |
| `user` — tam kapsam | ✅ `GET /api/users/me`, admin kullanıcı listeleme/arama, `PATCH .../active`, `GET /api/admin/roles`, `GET /api/admin/audit-logs`; `UserAuditLogService` tüm yazma işlemlerine bağlı |
| `user` — Başkan Yrd. pasifleştirme | ✅ Rol devri şartı kaldırıldı; doğrudan pasifleştirilebilir (sözleşme ve frontend ile hizalı) |
| İlk Admin | ✅ `BootstrapAdminRunner`; varsayılan parola yok, yalnızca ortam değişkeni açıkça verildiğinde çalışır |
| CI | ✅ GitHub Actions: PostgreSQL'li backend `mvn verify` + frontend lint/test/build |
| `pom.xml` | ✅ Duplike bağımlılık ve plugin tanımları kaldırıldı |
| DTO doğrulama | ✅ `auth` ve `user` DTO'larına `@NotBlank`/`@Email`/`@Size` |
| `common/config/CorsConfig` | ✅ İzinli origin `CORS_ALLOWED_ORIGINS` ortam değişkeninden, `*` yok |
| `/api/v1` öneki | ✅ Kaldırıldı; `record`/`categories` artık `/api/...` altında (frontend generated client dahil) |
| Çalışma notu modülü | ✅ **Kaldırıldı** — sözleşme kapsam dışı bıraktı, açıklama artık workflow isteğinin `comment` alanında. `V6__drop_record_notes.sql` |

İki not:

- Önceki listedeki `rbac/RoleName` maddesi farklı çözüldü — ayrı bir rol tipi
  eklenmedi, `workflow/statemachine/RoleName` projenin tek rol tipi olarak
  benimsendi ve `rbac` de onu kullanıyor.
- Önceki listede "onay akışı sınıflarına `@Service` eklenmeli" yazıyordu; bu
  yanlıştı. `WorkflowApplicationService` ve durum makinesi bilerek anotasyonsuz
  tutuluyor ki Spring olmadan test edilebilsinler.

---

## Kişi başı kalan iş

| Sorumlu | Paket | Kalan iş | Öncelik |
|---|---|---|---|
| Melih Kocaman | `templates/mail` | Şablon dosyaları | Teslim öncesi |
| Nisan · Sümeyye | `user` / `auth` | Üç dar test (yukarıda) | Teslim öncesi |
| Herkes | `common` | Test kapsamı | Teslim öncesi |
| Hacer | `common/config` | — | ✅ tamamlandı (CORS) |
| Ecesu Başak | `attachment` | — | ✅ tamamlandı |
| Irmak Tanrıverdi | `search` | — | ✅ tamamlandı |
| Esra Öncü · Burak Kaya | `workflow` | — | ✅ tamamlandı |
| Alperen Kara · Fevzi B. Urganioğlu | `record` | — | ✅ tamamlandı |
| **Toplam yeni sınıf** | | **0** | |

Ayrıca [EKSIK_CONTROLLERLAR_VE_KARARLAR.md](EKSIK_CONTROLLERLAR_VE_KARARLAR.md)
dosyasına bakınız: bildirim geçmişi ucu ve frontend'deki bir ölü kod bulgusu
orada takip ediliyor.
