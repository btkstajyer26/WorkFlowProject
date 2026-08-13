# Eksik Sınıflar ve Öncelik Sırası

**Tarih:** 13 Ağustos 2026
**Kapsam:** Yalnızca backend — frontend hariç
**Kaynak:** `integration/tum-feature-branchleri` dalı (mevcut: 130 sınıf, 301 test)

Bir önceki sürüm 10 Ağustos'ta 50 eksik sınıf sayıyordu. Yeni sınıf listesi
uzun süredir bitmiş durumda. 13 Ağustos'ta CORS eklenerek **Faz 1'in son
işlevsel maddesi de kapandı**; geriye yalnızca iki Clean Code / test kalitesi
maddesi kaldı.

13 Ağustos'ta iki turda entegrasyon yapıldı:

1. `test`, `feature/record` ve `feature/frontend-uygulamasi` dalları:
   kayıt listeleme tek uca indirildi (görünürlük kuralı artık tek yerde),
   çalışma notu modülü geri alındı (sözleşme zaten kapsam dışı bırakmıştı),
   frontend gerçek API adreslerine (`/api/v1` öneksiz) geçti.
2. `fix/workflow-flush`, `feature/nisan-sumeyye` ve
   `feature/frontend-uygulamasi` (zorunlu parola değişikliği): kayıt
   güncellemesi artık `saveAndFlush` ile transaction içinde görünür
   kılınıyor, refresh ucu pasif hesabı reddediyor, geçersiz `sort`
   parametresi 500 yerine 400 dönüyor.

> **Politika değişikliği:** Başkan Yardımcısı pasifleştirme kuralı ilk
> turda "doğrudan pasifleştirilebilir" olarak hizalanmıştı. İkinci turda
> ekip bunu tersine çevirdi: rol devri artık `changeRole` üzerinden,
> zorunlu bir yerine atanacak kullanıcı parametresiyle yapılıyor; devirsiz
> doğrudan pasifleştirme yine engellendi. Ayrıntı için
> [EKSIK_CONTROLLERLAR_VE_KARARLAR.md §2.4](EKSIK_CONTROLLERLAR_VE_KARARLAR.md).

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
| `common` testleri 🟢 | *ilgili sahipler* | 301 testin çoğu `workflow`, `rbac`, `audit`, `search`, `record`, `auth` ve `user`'da. `common` paketinin (`GlobalExceptionHandler`, `ApiError` vb.) hâlâ hiç testi yok — yalnız dolaylı olarak `AdminControllerTest` üzerinden bir hata kodu (`INVALID_SORT_FIELD`) kapsanıyor. |
| `user` / `auth` — kalan iki dar test 🟡 | *Nisan · Sümeyye* | `UserService.setActive`/`changeRole` testi ve refresh token'da pasif hesap reddi artık kapsamda (`UserServiceTest`, `AuthServiceTest`). Kalan iki boşluk: aynı e-postayla ikinci kullanıcı denemesinin 409 döndüğü test edilmiyor; pasif kullanıcının **elindeki geçerli access token'ının** da reddedildiği `JwtAuthenticationFilter` seviyesinde test edilmiyor (kontrol kodda var, testi yok). |

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
| `user` — Başkan Yrd. koltuk devri | ✅ Devir `changeRole`'a taşındı (zorunlu `replacementBaskanYardimcisiId`, aynı transaction); devirsiz doğrudan pasifleştirme yine engelli — bkz. yukarıdaki politika değişikliği notu |
| `auth` — refresh pasif kontrolü | ✅ Token geçerli olsa bile hesap pasifse `refresh()` reddediyor; önceden yalnız login ve erişim token'ı (filtre) kontrol ediyordu |
| `auth` — parola değiştirme akışı | ✅ Frontend'de zorunlu ilk giriş parola değişikliği eklendi (`PasswordChangePage`); `/api/auth/change-password` artık kimlik doğrulaması istiyor (önceden `/api/auth/**` içinde açıktı, `@AuthenticationPrincipal` null geldiğinde 500 verirdi) |
| `workflow` — kayıt güncellemesi flush | ✅ `RecordPortAdapter.update` artık `saveAndFlush`; manuel sürüm kontrolünün ürettiği optimistic-lock hatası audit/event çağrılarından önce, aynı metot içinde kesinleşiyor |
| `common` — geçersiz sıralama alanı | ✅ `?sort=` var olmayan bir alana göre isterse 500 yerine 400 (`INVALID_SORT_FIELD`) dönüyor; tüm sayfalı uçları kapsıyor |
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
| Nisan · Sümeyye | `user` / `auth` | İki dar test (yukarıda) | Teslim öncesi |
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
