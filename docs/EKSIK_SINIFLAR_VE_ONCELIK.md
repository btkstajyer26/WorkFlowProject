# Eksik Sınıflar ve Öncelik Sırası

**Tarih:** 11 Ağustos 2026
**Kapsam:** Yalnızca backend — frontend hariç
**Kaynak:** `integration/tum-feature-branchleri` dalı (mevcut: 100 sınıf, 211 test)

Bir önceki sürüm 10 Ağustos'ta 50 eksik sınıf sayıyordu. O tarihten bu yana
`auth`, `user`, `record`, `common`, `rbac`, `audit` modülleri ve onay akışının
adaptörleri geldi. **Kalan 14 yeni sınıf** aşağıda; ayrıca sınıf sayısıyla
ölçülmeyen iki yapısal sorun var ve bunlar yeni sınıf yazmaktan daha
önceliklidir.

> Sınıf adları öneridir; paket yerleşimi projenin modül bazlı yapısına uyar.

---

## Önce bunlar: yeni sınıf değil, bağlantı sorunu

Bu iki madde yeni kod yazmayı değil, **var olan kodu birbirine bağlamayı**
gerektiriyor. İkisi de teslim kalitesini doğrudan etkiliyor.

### 1. Onay akışı iki kere yazılmış durumda 🔴

Şartnamedeki durum makinesi `workflow` modülünde tam ve doğrulanmış hâlde
duruyor (`TransitionRules`, `WorkflowTransitionValidator`, 90+ test). Ama
uçlara bağlı olan bu değil: [`RecordServiceImpl`](../backend/src/main/java/btk/staj/WorkFlowProject/record/service/RecordServiceImpl.java)
durumları doğrudan `record.setStatus(...)` ile değiştiriyor ve
[`RecordWorkflowController`](../backend/src/main/java/btk/staj/WorkFlowProject/record/controller/RecordWorkflowController.java)
bu yolu kullanıyor.

Sonuçları:

- Geçiş kuralları (kim, hangi durumda, hangi aksiyonu alabilir) çalışmıyor —
  yalnızca `@PreAuthorize` ile rol bakılıyor, **durum** bakılmıyor.
- Denetim izi yazılmıyor. Şartname §4.2 tüm durum değişikliklerinin Audit Log'a
  yazılmasını istiyor; bu yoldan geçen hiçbir işlem loglanmıyor.
- Zorunlu açıklama kuralı (§3 "geri gönderirken açıklama zorunludur")
  uygulanmıyor.
- Kilitli/terminal durumdaki kayıt korunmuyor.

**Yapılacak:** `RecordWorkflowController` + `RecordServiceImpl`'deki geçiş
metotları kaldırılıp uçlar `WorkflowApplicationService` üzerinden
`WorkflowActionApi` sözleşmesine bağlanmalı. — *Esra & Burak, Alperen & Fevzi
birlikte*

### 2. Onay akışı sınıfları Spring bean'i değil 🟡

`WorkflowApplicationService`, `TargetUserResolver` ve
`WorkflowTransitionValidator` üzerinde `@Service` / `@Component` yok, dolayısıyla
Spring bunları hiç oluşturmuyor. 1. madde bunlar olmadan çözülemez. Bağımlılığı
yok, bugün yapılabilir. — *Esra & Burak*

---

## İşaretler

| İşaret | Anlamı |
|---|---|
| 🟢 **Yeni** | Sıfırdan yazılacak sınıf |
| 🟡 **Değişiklik** | Mevcut sınıfa ekleme |
| 🔴 **Sorun** | Kaldırılması / düzeltilmesi gereken |

---

## Faz 1 — Onay akışını tamamlayan son parçalar

### Esra Öncü · Burak Kaya — `workflow`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `workflow/adapter/RecordPortAdapter` | 🟢 Yeni | `WorkflowRecordPort` implementasyonu. Diğer üç adaptör (`UserPortAdapter`, `SecurityCurrentActorProvider`, `SpringWorkflowEventPublisher`) hazır; eksik olan tek adaptör bu. `RecordRepository` hazır, bekleyen bir şey yok. |
| `workflow/controller/WorkflowActionController` | 🟢 Yeni | Mevcut `WorkflowActionApi` arayüzünü uygulayan somut controller. Yukarıdaki 1. maddenin uygulanacağı yer. |
| `WorkflowApplicationService`<br>`TargetUserResolver`<br>`WorkflowTransitionValidator` | 🟡 Değişiklik | `@Service` / `@Component` eklenmeli (bkz. 2. madde). |

### Alperen Kara · Fevzi Berke Urganioğlu — `record`

| Sınıf | Tür | Açıklama |
|---|---|---|
| `record/service/RecordServiceImpl` | 🔴 Sorun | Durum değiştiren metotlar (`submitToDeputy`, `forwardToChairman`, `approve`, `reject`, geri gönderme) durum makinesini atlıyor. Kaldırılıp workflow'a devredilmeli. |
| `record/controller/RecordWorkflowController` | 🔴 Sorun | Aynı sebeple kaldırılacak; yerini `WorkflowActionController` alacak. |

---

## Faz 2 — Bağımsız modüller

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

## Faz 3 — Teslim öncesi kalite

> Şartname değerlendirmeyi yalnızca işlevselliğe değil kod kalitesine de bağlıyor
> (§6.2). Her modül sahibi kendi alanında yapar.

| İş | Tür | Açıklama |
|---|---|---|
| `attachment/service/FileService`<br>`attachment/controller/FileController` | 🟡 Değişiklik | `uploadedBy` ve `deletedBy` hâlâ `@RequestParam` ile isteyenin kendi beyanı; oturumdan alınmalı. Şu hâliyle bir kullanıcı başkası adına dosya yükleyebilir/silebilir. — *Ecesu* |
| `*ServiceTest` · `*ControllerTest` | 🟢 Yeni | 211 testin çoğu `workflow`, `rbac` ve `audit`'te. `record`, `auth`, `user` modüllerinin iş kuralları test edilmemiş. |
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
| `workflow` adaptörleri | ✅ 4 adaptörün 3'ü (`RecordPortAdapter` kaldı) |

Not: Önceki listedeki `rbac/RoleName` maddesi farklı çözüldü — ayrı bir rol tipi
eklenmedi, `workflow/statemachine/RoleName` projenin tek rol tipi olarak
benimsendi ve `rbac` de onu kullanıyor.

---

## Kişi başı kalan iş

| Sorumlu | Paket | Kalan sınıf | Öncelik |
|---|---|---:|---|
| Melih Kocaman | `notification` | 7 | Hemen başlayabilir |
| Irmak Tanrıverdi | `search` | 5 | Hemen başlayabilir |
| Esra Öncü · Burak Kaya | `workflow` | 2 + 3 değişiklik | **Faz 1 — en kritik** |
| Alperen Kara · Fevzi B. Urganioğlu | `record` | 2 kaldırma | **Faz 1 — workflow ile birlikte** |
| Ecesu Başak | `attachment` | 2 değişiklik | Faz 3 |
| Herkes | kendi modülü | test + doğrulama | Faz 3 |
| **Toplam yeni sınıf** | | **14** | |
