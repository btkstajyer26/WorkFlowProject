# Eksik Sınıflar ve Öncelik Sırası

**Tarih:** 11 Ağustos 2026
**Kapsam:** Yalnızca backend — frontend hariç
**Kaynak:** `integration/tum-feature-branchleri` dalı (mevcut: 116 sınıf, 246 test)

Bir önceki sürüm 10 Ağustos'ta 50 eksik sınıf sayıyordu. O tarihten bu yana
`auth`, `user`, `record`, `common`, `rbac`, `audit` modülleri, onay akışının
adaptörleri, onay akışının uca bağlanması, bildirim ve arama modülleri
tamamlandı. Yeni sınıf listesi bitti; **kalan iş yalnızca teslim öncesi
kalite maddeleri**.

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
| `common/config/CorsConfig` 🟢 | *Hacer* | **Projede hiç CORS yapılandırması yok.** Frontend `5173`, backend `8080` — ayrı origin. Frontend şu an mock veriyle çalıştığı için sorun görünmüyor; ilk gerçek istekte her uç tarayıcıda bloklanır. İzinli origin ortam değişkeninden okunmalı, `*` verilmemeli (kimlik doğrulamalı istekler için zaten geçersiz). |
| `attachment/service/FileService`<br>`attachment/controller/FileController` 🟡 | *Ecesu* | `uploadedBy` ve `deletedBy` hâlâ `@RequestParam` ile isteyenin kendi beyanı; oturumdan alınmalı. Şu hâliyle bir kullanıcı başkası adına dosya yükleyebilir/silebilir. |
| `record/service/RecordServiceImpl` 🟡 | *Alperen · Fevzi* | `getCurrentUserId()` sabit bir UUID döndürüyor (`11111111-…`). CRUD'un tamamı bu sahte kullanıcı üzerinden çalışıyor: sahiplik kontrolleri anlamsız. Arama modülünde aynı sorun düzeltildi, örnek orada. |
| `templates/mail/*.html` 🟢 | *Melih* | `MailService` ~130 satırlık HTML'i metin bloğu olarak içinde taşıyor. Şablon dosyaya çıkarılmalı (thymeleaf bağımlılığı eklenecek). İşlevsel bir eksik değil, Clean Code maddesi. |
| `record` · `auth` · `common` testleri 🟢 | *ilgili sahipler* | 246 testin çoğu `workflow`, `rbac`, `audit` ve `search`'te. Bu üç modülün hiç testi yok. |
| `*Request` DTO'ları 🟡 | *ilgili sahipler* | `@Valid`/`@NotBlank` yalnızca `RecordCreateRequest`, `RecordUpdateRequest` ve `WorkflowActionRequest`'te var. `auth` ve `user` DTO'larında doğrulama yok. |

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
| Ecesu Başak | `attachment` | 2 değişiklik | **Sıradaki iş** |
| Herkes | kendi modülü | test + doğrulama | Teslim öncesi |
| Melih Kocaman | `notification` | — | ✅ tamamlandı |
| Irmak Tanrıverdi | `search` | — | ✅ tamamlandı |
| Esra Öncü · Burak Kaya | `workflow` | — | ✅ tamamlandı |
| Alperen Kara · Fevzi B. Urganioğlu | `record` | — | ✅ tamamlandı |
| **Toplam yeni sınıf** | | **0** | |
