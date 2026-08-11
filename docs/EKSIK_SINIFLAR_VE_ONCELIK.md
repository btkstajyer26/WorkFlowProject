# Eksik Sınıflar ve Öncelik Sırası

**Tarih:** 11 Ağustos 2026
**Kapsam:** Yalnızca backend — frontend hariç
**Kaynak:** `integration/tum-feature-branchleri` dalı (mevcut: 101 sınıf, 225 test)

Bir önceki sürüm 10 Ağustos'ta 50 eksik sınıf sayıyordu. O tarihten bu yana
`auth`, `user`, `record`, `common`, `rbac`, `audit` modülleri, onay akışının
adaptörleri ve onay akışının uca bağlanması tamamlandı. **Kalan 12 yeni sınıf**
aşağıda ve hepsi iki kişide toplanmış durumda.

> Sınıf adları öneridir; paket yerleşimi projenin modül bazlı yapısına uyar.

---

## İşaretler

| İşaret | Anlamı |
|---|---|
| 🟢 **Yeni** | Sıfırdan yazılacak sınıf |
| 🟡 **Değişiklik** | Mevcut sınıfa ekleme |
| 🔴 **Sorun** | Kaldırılması / düzeltilmesi gereken |

---

## Faz 1 — Bağımsız modüller

> İkisi de kimseyi beklemiyor.

### Melih Kocaman — `notification` · **hiç başlanmadı**

| Sınıf | Tür | Açıklama |
|---|---|---|
| `notification/listener/WorkflowStatusChangedListener` | 🟢 Yeni | Durum değişikliği event'ini dinler. Yayıncı taraf (`SpringWorkflowEventPublisher`) ve event tipi (`WorkflowStatusChangedEvent`) **hazır** — dinleyici yazılır yazılmaz çalışır. |
| `notification/service/MailService` | 🟢 Yeni | `JavaMailSender` ile Outlook/SMTP gönderimi, `@Async` (§6.2). Docker'daki Mailpit ile denenebilir. |
| `notification/entity/Notification`<br>`notification/repository/NotificationRepository`<br>`notification/service/NotificationService` | 🟢 Yeni | Uygulama içi bildirimler. `notifications` tablosu ve okunmamış indeksi hazır. |
| `notification/controller/NotificationController`<br>`notification/dto/NotificationResponse` | 🟢 Yeni | Bildirim listeleme ve okundu işaretleme. |
| `templates/mail/*.html` | 🟢 Yeni | §4.5: kayıt özeti, güncel durum, son açıklama ve kayda giden derin bağlantı butonu. Klasör açılmış, boş. |

### Irmak Tanrıverdi — `search` · **hiç başlanmadı**

| Sınıf | Tür | Açıklama |
|---|---|---|
| `search/specification/RecordSpecifications` | 🟢 Yeni | Duruma, kategoriye, kullanıcıya, tarihe ve metne göre dinamik kriterler (§4.4). `RecordRepository` zaten `JpaSpecificationExecutor` genişletiyor. |
| `search/dto/RecordSearchCriteria` | 🟢 Yeni | Filtre parametrelerini taşıyan tip. |
| `search/service/RecordSearchService` | 🟢 Yeni | Sayfalama ile arama. **Yetki kapsamı burada uygulanmalı** — `RecordAccessPolicy` hazır, kimse yetkisi olmayan kaydı sonuçta görmemeli. |
| `search/controller/RecordSearchController`<br>`common/dto/PagedResponse` | 🟢 Yeni | Arama ucu ve ortak sayfalama yanıtı. |

---

## Faz 2 — Teslim öncesi kalite

> Şartname değerlendirmeyi yalnızca işlevselliğe değil kod kalitesine de bağlıyor
> (§6.2). Her modül sahibi kendi alanında yapar.

| İş | Tür | Açıklama |
|---|---|---|
| `attachment/service/FileService`<br>`attachment/controller/FileController` | 🟡 Değişiklik | `uploadedBy` ve `deletedBy` hâlâ `@RequestParam` ile isteyenin kendi beyanı; oturumdan alınmalı. Şu hâliyle bir kullanıcı başkası adına dosya yükleyebilir/silebilir. — *Ecesu* |
| `*ServiceTest` · `*ControllerTest` | 🟢 Yeni | 225 testin çoğu `workflow`, `rbac` ve `audit`'te. `record`, `auth`, `user` modüllerinin iş kuralları test edilmemiş. |
| `*Request` DTO'ları | 🟡 Değişiklik | `@Valid`/`@NotBlank` yalnızca `RecordCreateRequest`, `RecordUpdateRequest` ve `WorkflowActionRequest`'te var. `auth` ve `user` DTO'larında doğrulama yok. |

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
| Melih Kocaman | `notification` | 7 | **Faz 1 — hemen başlayabilir** |
| Irmak Tanrıverdi | `search` | 5 | **Faz 1 — hemen başlayabilir** |
| Ecesu Başak | `attachment` | 2 değişiklik | Faz 2 |
| Herkes | kendi modülü | test + doğrulama | Faz 2 |
| Esra Öncü · Burak Kaya | `workflow` | — | ✅ tamamlandı |
| Alperen Kara · Fevzi B. Urganioğlu | `record` | — | ✅ tamamlandı |
| **Toplam yeni sınıf** | | **12** | |
