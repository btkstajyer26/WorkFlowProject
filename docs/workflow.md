# İş Akışı ve Durum Geçişleri

Bu belge, İş Akışı ve Onay Yönetim Sistemi'nin çalışan backend kodundaki workflow davranışını tanımlar. Ürün hedefinden çok **mevcut uygulamayı** esas alır; planlanan ancak henüz uygulanmayan davranışlar “Bilinen boşluklar” bölümünde ayrıca belirtilir.

> Son kod doğrulaması 22 Ağustos 2026 tarihinde `feature/m9-envanter` dalının `183517e` commit'i üzerinde yapılmıştır. Durum makinesi, API veya hata eşlemesi değiştirildiğinde bu belge aynı değişiklik kapsamında güncellenmelidir.

## İçindekiler

- [Kapsam ve kaynaklar](#kapsam-ve-kaynaklar)
- [Mimari](#mimari)
- [Roller ve organizasyon kuralları](#roller-ve-organizasyon-kuralları)
- [Kayıt görünürlüğü ve aksiyon yetkisi](#kayıt-görünürlüğü-ve-aksiyon-yetkisi)
- [Kayıt durumları](#kayıt-durumları)
- [Geçiş matrisi](#geçiş-matrisi)
- [HTTP API sözleşmesi](#http-api-sözleşmesi)
- [Doğrulama sırası](#doğrulama-sırası)
- [Hedef çözümleme ve atama](#hedef-çözümleme-ve-atama)
- [Transaction, audit ve bildirimler](#transaction-audit-ve-bildirimler)
- [Hata sözleşmesi](#hata-sözleşmesi)
- [Eşzamanlılık ve kayıt kilidi](#eşzamanlılık-ve-kayıt-kilidi)
- [Test kapsamı](#test-kapsamı)
- [Bilinen boşluklar ve kararlar](#bilinen-boşluklar-ve-kararlar)
- [Değişiklik kontrol listesi](#değişiklik-kontrol-listesi)

## Kapsam ve kaynaklar

Workflow'un tek yazma ucu şudur:

```http
POST /api/records/{recordId}/workflow/actions
```

Kanonik uygulama kaynakları:

| Konu | Kaynak |
| --- | --- |
| Durumlar | [`RecordStatus`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/RecordStatus.java) |
| Aksiyon özellikleri | [`WorkflowAction`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/WorkflowAction.java) |
| İzinli geçişler | [`TransitionRules`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/TransitionRules.java) |
| Doğrulama sırası | [`WorkflowTransitionValidator`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/WorkflowTransitionValidator.java) |
| Hedef çözümleme | [`TargetUserResolver`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/service/TargetUserResolver.java) |
| Uygulama akışı | [`WorkflowApplicationService`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/service/WorkflowApplicationService.java) |
| Transaction sınırı | [`WorkflowActionService`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/service/WorkflowActionService.java) |
| HTTP ucu | [`WorkflowActionApi`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/controller/WorkflowActionApi.java) ve [`WorkflowActionController`](../backend/src/main/java/btk/staj/WorkFlowProject/workflow/controller/WorkflowActionController.java) |
| HTTP hata eşlemesi | [`GlobalExceptionHandler`](../backend/src/main/java/btk/staj/WorkFlowProject/common/exception/GlobalExceptionHandler.java) |
| Workflow yan etkileri | [`WorkflowStatusChangedListener`](../backend/src/main/java/btk/staj/WorkFlowProject/notification/listener/WorkflowStatusChangedListener.java) |

İstemci hedef durumu göndermez. Yalnız aksiyonu ve aksiyona bağlı alanları gönderir; hedef durum her zaman backend'deki merkezi geçiş tablosundan hesaplanır.

## Mimari

```mermaid
flowchart LR
    CLIENT["Frontend / API istemcisi"] --> CONTROLLER["WorkflowActionController"]
    CONTROLLER --> TX["WorkflowActionService (@Transactional)"]
    TX --> APP["WorkflowApplicationService"]
    APP --> ACTOR["CurrentActorProvider"]
    APP --> RECORD["WorkflowRecordPort"]
    APP --> TARGET["TargetUserResolver"]
    APP --> VALIDATOR["WorkflowTransitionValidator"]
    APP --> AUDIT["AuditService"]
    APP --> EVENT["WorkflowEventPublisher"]
    RECORD --> DB[(PostgreSQL)]
    AUDIT --> DB
    EVENT --> INAPP["Uygulama içi bildirim"]
    EVENT --> MAIL["Commit sonrası e-posta"]
```

Durum makinesi ve uygulama servisi doğrudan Spring, JPA veya HTTP'ye bağlı değildir. Spring bean bağlantıları `WorkflowConfiguration` içinde yapılır. Controller, saf uygulama servisini doğrudan değil, transaction açan `WorkflowActionService` üzerinden çağırır.

## Roller ve organizasyon kuralları

| Rol | Workflow kapsamı |
| --- | --- |
| `CALISAN` | Kendi kaydını ilk kez gönderir veya düzeltme sonrasında yeniden gönderir. |
| `BASKAN_YARDIMCISI` | Yalnız kendisine atanmış kaydı Başkana iletir ya da Çalışana geri gönderir. |
| `BASKAN` | Yalnız kendisine atanmış kaydı onaylar, reddeder veya geri gönderir. |
| `ADMIN` | Workflow aktörü ve hedefi değildir. Her aksiyon denemesi `WORKFLOW_ROLE_NOT_ALLOWED` ile reddedilir. |

Kullanıcı yönetiminde `ADMIN`, `BASKAN` ve `BASKAN_YARDIMCISI` tekil rol olarak tasarlanmıştır. `changeRole` ikinci aktif kullanıcıya tekil rol verilmesini engeller; ancak `setActive(..., true)` aynı singleton kontrolünü yapmadığından bu invariant bütün yazma yollarında henüz garanti edilmez.

Başkan Yardımcısı koltuğu için ek kurallar:

- Aktif Başkan Yardımcısı doğrudan pasifleştirilemez.
- Bu kullanıcı başka bir role geçirilirken `PATCH /api/admin/users/{id}/role` isteğinde `replacementBaskanYardimcisiId` verilmelidir.
- Yerine seçilen kullanıcı aktif olmalı ve koltuğu boşaltan kullanıcıyla aynı olmamalıdır.
- Devir aynı kullanıcı yönetimi transaction'ında uygulanır ve iki rol değişikliği de audit kaydı üretir.

> Rol tekil olduğu için `GONDER` ve `TEKRAR_GONDER` hedefini de backend çözer; istemci `targetUserId` göndermez. Çalışanın erişebildiği kullanıcı ucu yalnız `GET /api/users/me` olduğundan Başkan Yardımcısı UUID'sini keşfedemez ve tekil rol kararı gereği ona kullanıcı listeleme ucu açılmayacaktır. Sistemde tam olarak bir aktif Başkan Yardımcısı yoksa (devir anında sıfır, yanlış yapılandırmada birden fazla) istek `409 WORKFLOW_ROLE_NOT_CONFIGURED` ile durur.

## Kayıt görünürlüğü ve aksiyon yetkisi

Kayıt listeleme/detay görünürlüğü ile workflow aksiyonu yapma yetkisi aynı kontrol değildir:

| Rol | Kayıt okuma kapsamı |
| --- | --- |
| `CALISAN` | Yaşam döngüsü boyunca kendisinin oluşturduğu kayıtlar |
| `BASKAN_YARDIMCISI` | Kendisine atanmış kayıtlar, `DUZENLEME_BEKLIYOR` durumundakiler ve bir kez kendi elinden geçmiş kayıtlar (`last_deputy_id`) |
| `BASKAN` | `BASKAN_INCELEMESINDE` durumundaki, sonuçlanmış (`ONAYLANDI`/`REDDEDILDI`) veya kendisine atanmış kayıtlar |
| `ADMIN` | Hiçbir workflow kaydı |

Kapsamın iki kolu, `assigned_to`'nun geçişte boşalması yüzünden gerekli:

- **Başkan Yardımcısı**, `BASKANA_ILET` ile `assigned_to`'yu Başkana devreder ama `last_deputy_id` kendisinde kalır. Bu kol olmasaydı ilettiği evrağı anında kaybeder; "Sonuçlananlar" ve panodaki "Son Kayıtlar" listeleri kalıcı olarak boş görünürdü.
- **Başkan**, `ONAYLA`/`REDDET` ile `assigned_to`'yu boşaltır. Sonuçlanan iki durum kapsama açıkça yazılmasaydı kendi verdiği karardan sonra kaydı kaybeder; "Onaylananlar" ve "Reddedilenler" sekmeleri boş kalırdı. Bu iki duruma yalnız Başkanın kararıyla gelinebildiği için kapsam genişlemez.

Liste sorguları soft-delete edilmiş kayıtları dışlar. Kayıt audit geçmişi ucu da okumadan önce aynı `RecordAccessPolicy` kuralını uygular.

Aynı kural iki biçimde durur: tek kayıt için `RecordAccessPolicy`, sorgu koşulu olarak `RecordSpecifications.visibilityScope`. **Biri değişirse diğeri de değişmelidir** — ikisi ayrıştığında detay ucu kaydı açarken liste ucu onu hiç döndürmez.

Workflow controller'ı ayrıca `RecordAccessPolicy` çağırmaz. Aksiyon yetkisi; rol, durum ve `createdBy`/`assignedTo` ilişkisi üzerinden durum makinesinde belirlenir. Okuma kapsamı bir kaydı görünür kılması, o kayıt üzerinde aksiyon yapılabileceği anlamına gelmez: ilettiği evrağı izleyen Başkan Yardımcısı onu salt okunur görür.

## Kayıt durumları

| Durum | Anlamı | Kaydı oluşturan düzenleyebilir mi? | Terminal mi? |
| --- | --- | --- | --- |
| `TASLAK` | Oluşturulmuş, henüz incelemeye gönderilmemiş kayıt | Evet | Hayır |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısının işlemini bekleyen kayıt | Hayır | Hayır |
| `BASKAN_INCELEMESINDE` | Başkanın nihai kararını bekleyen kayıt | Hayır | Hayır |
| `DUZENLEME_BEKLIYOR` | Düzenleme için kaydı oluşturan Çalışana dönmüş kayıt | Evet | Hayır |
| `ONAYLANDI` | Başkan tarafından onaylanmış kayıt | Hayır | Evet |
| `REDDEDILDI` | Başkan tarafından nihai olarak reddedilmiş kayıt | Hayır | Evet |

`ONAYLANDI` ve `REDDEDILDI` durumlarında yeni workflow aksiyonu uygulanamaz. Kayıt içeriği yalnız `TASLAK` ve `DUZENLEME_BEKLIYOR` durumlarında sahibi tarafından düzenlenebilir. Dosya yükleme terminal durumda engellenir; dosya silme ise mevcut kodda kayıt durumunu kontrol etmez. Bu asimetri [Bilinen boşluklar ve kararlar](#bilinen-boşluklar-ve-kararlar) bölümünde açık risk olarak tutulur.

Yeni kayıt, record modülü tarafından doğrudan `TASLAK` durumuyla oluşturulur; bu işlem bir workflow geçişi değildir.

Kanonik durum adı `BASKAN_INCELEMESINDE` değeridir. Eski sözleşme veya örneklerde görülebilen `BASKAN_ONAYINDA` kodda tanımlı değildir ve API'de kullanılmamalıdır.

```mermaid
stateDiagram-v2
    [*] --> TASLAK
    TASLAK --> BSK_YRD_INCELEMESINDE: GONDER
    DUZENLEME_BEKLIYOR --> BSK_YRD_INCELEMESINDE: TEKRAR_GONDER
    BSK_YRD_INCELEMESINDE --> BASKAN_INCELEMESINDE: BASKANA_ILET
    BSK_YRD_INCELEMESINDE --> DUZENLEME_BEKLIYOR: CALISANA_GERI_GONDER
    BASKAN_INCELEMESINDE --> DUZENLEME_BEKLIYOR: CALISANA_GERI_GONDER
    BASKAN_INCELEMESINDE --> BSK_YRD_INCELEMESINDE: BASKAN_YARDIMCISINA_GERI_GONDER
    BASKAN_INCELEMESINDE --> ONAYLANDI: ONAYLA
    BASKAN_INCELEMESINDE --> REDDEDILDI: REDDET
    ONAYLANDI --> [*]
    REDDEDILDI --> [*]
```

## Geçiş matrisi

Tabloda bulunmayan her durum–aksiyon–rol birleşimi geçersizdir.

| Mevcut durum | Aksiyon | Aktör rolü | Gerekli kayıt ilişkisi | Hedef çözümü | Açıklama | Hedef durum |
| --- | --- | --- | --- | --- | --- | --- |
| `TASLAK` | `GONDER` | `CALISAN` | Kaydı oluşturan | İstekteki aktif Başkan Yardımcısı | İsteğe bağlı | `BSK_YRD_INCELEMESINDE` |
| `DUZENLEME_BEKLIYOR` | `TEKRAR_GONDER` | `CALISAN` | Hem oluşturan hem atanan | İstekteki aktif Başkan Yardımcısı | İsteğe bağlı | `BSK_YRD_INCELEMESINDE` |
| `BSK_YRD_INCELEMESINDE` | `BASKANA_ILET` | `BASKAN_YARDIMCISI` | Atanan kullanıcı | Backend'deki tek aktif Başkan | İsteğe bağlı | `BASKAN_INCELEMESINDE` |
| `BSK_YRD_INCELEMESINDE` | `CALISANA_GERI_GONDER` | `BASKAN_YARDIMCISI` | Atanan kullanıcı | Kaydın `createdBy` kullanıcısı | Zorunlu | `DUZENLEME_BEKLIYOR` |
| `BASKAN_INCELEMESINDE` | `ONAYLA` | `BASKAN` | Atanan kullanıcı | Yok | İsteğe bağlı | `ONAYLANDI` |
| `BASKAN_INCELEMESINDE` | `REDDET` | `BASKAN` | Atanan kullanıcı | Yok | Zorunlu | `REDDEDILDI` |
| `BASKAN_INCELEMESINDE` | `CALISANA_GERI_GONDER` | `BASKAN` | Atanan kullanıcı | Kaydın `createdBy` kullanıcısı | Zorunlu | `DUZENLEME_BEKLIYOR` |
| `BASKAN_INCELEMESINDE` | `BASKAN_YARDIMCISINA_GERI_GONDER` | `BASKAN` | Atanan kullanıcı | Kaydın `lastDeputyId` kullanıcısı | Zorunlu | `BSK_YRD_INCELEMESINDE` |

Kayıt ilişkileri:

- **Kaydı oluşturan:** oturum kullanıcısının kimliği `record.createdBy` ile aynıdır.
- **Atanan kullanıcı:** oturum kullanıcısının kimliği `record.assignedTo` ile aynıdır.
- **Hem oluşturan hem atanan:** iki koşul aynı anda sağlanmalıdır. `TEKRAR_GONDER` yalnız düzenleme için kendisine dönmüş kaydın sahibi tarafından yapılabilir.

## HTTP API sözleşmesi

### Kimlik doğrulama

Workflow ucu herkese açık değildir. Geçerli JWT ile kimliği doğrulanmış, aktif bir kullanıcı gerekir. Controller üzerinde ayrı `@PreAuthorize` bulunmaz; rol, durum ve kayıt ilişkisi kontrolleri merkezi durum makinesinde uygulanır.

### İstek

```http
POST /api/records/{recordId}/workflow/actions
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "action": "GONDER",
  "comment": "İncelemeye sunulmuştur."
}
```

| Alan | Tip | Genel kural |
| --- | --- | --- |
| `action` | `WorkflowAction` | Her zaman zorunlu. Bilinmeyen enum değeri `400 BAD_REQUEST` üretir. |
| `targetUserId` | UUID | **Hiçbir aksiyonda gönderilmez.** Hedefi her zaman backend çözer; alan yine de gönderilirse istek `400 WORKFLOW_TARGET_NOT_ALLOWED` ile reddedilir. |
| `comment` | string | En fazla 2000 karakter. Geri gönderme aksiyonları ve `REDDET` için boş olmayan değer zorunludur. |

Zorunlu açıklamada yalnız boşluk, sekme veya satır sonundan oluşan değer kabul edilmez. Açıklaması isteğe bağlı aksiyonlarda boş değer teknik olarak kabul edilir; istemcinin alanı göndermemesi tercih edilir.

Çalışana geri gönderme örneği:

```json
{
  "action": "CALISANA_GERI_GONDER",
  "comment": "Bütçe kalemi ve teklif dosyası eksik."
}
```

Onay örneği:

```json
{
  "action": "ONAYLA"
}
```

### Başarılı yanıt

```json
{
  "recordId": "720f7295-68d1-4f61-b4e8-bb82f2bd9d0a",
  "action": "BASKANA_ILET",
  "previousStatus": "BSK_YRD_INCELEMESINDE",
  "newStatus": "BASKAN_INCELEMESINDE",
  "assignedTo": "5bedacf4-f132-4e30-9d38-e5ac3ef63d08",
  "performedBy": "ee9069af-b7b4-4afc-a170-4fd0d65bed35",
  "performedAt": "2026-08-13T12:30:00Z"
}
```

`assignedTo`, onay ve nihai ret sonrasında `null` olur. `performedAt`, backend'in UTC saatinden üretilir. Başarılı cevap tam kayıt detayı değil, geçiş özetidir; istemci gerekirse kayıt detayını yeniden çekmelidir.

## Doğrulama sırası

Hangi hata kodunun döneceği doğrulama sırasına bağlıdır. Uygulanan sıra şöyledir:

1. Aktör rolü workflow'a katılabilir mi? `ADMIN` burada elenir.
2. Kayıt terminal durumda mı?
3. Durum–aksiyon–rol birleşimi geçiş tablosunda var mı?
4. Aktör, kuralın istediği kayıt ilişkisini sağlıyor mu?
5. Zorunlu açıklama dolu mu?
6. İstekte hedef bekleniyorsa `targetUserId` var mı? (Şu an hiçbir aksiyon beklemiyor.)
7. İstekte yanlışlıkla `targetUserId` gönderilmiş mi?
8. Çözülen hedef beklenen role sahip mi?
9. Çözülen hedef aktif mi?

Uygulama servisi hedef gerektiren aksiyonlarda iki aşamalı doğrulama yapar: önce aktör/durum/istek alanlarını kontrol eder, sonra hedef kullanıcıyı çözüp rol ve aktiflik doğrulamasını tamamlar. Bu sayede yetkisiz bir istek için gereksiz kullanıcı sorgusu yapılmaz.

## Hedef çözümleme ve atama

| Aksiyon | Hedefin kaynağı | Başarısızlık davranışı |
| --- | --- | --- |
| `GONDER` | Aktif `BASKAN_YARDIMCISI` rolündeki kullanıcılar | Tam olarak bir aktif Başkan Yardımcısı yoksa `WORKFLOW_ROLE_NOT_CONFIGURED` |
| `TEKRAR_GONDER` | Aktif `BASKAN_YARDIMCISI` rolündeki kullanıcılar | Tam olarak bir aktif Başkan Yardımcısı yoksa `WORKFLOW_ROLE_NOT_CONFIGURED` |
| `BASKANA_ILET` | Aktif `BASKAN` rolündeki kullanıcılar | Tam olarak bir aktif Başkan yoksa `WORKFLOW_ROLE_NOT_CONFIGURED` |
| `CALISANA_GERI_GONDER` | `record.createdBy` | Referans kullanıcı yoksa veri bütünlüğü hatası; rolü/aktifliği yanlışsa hedef doğrulama hatası |
| `BASKAN_YARDIMCISINA_GERI_GONDER` | `record.lastDeputyId` | Alan boşsa veya kullanıcı yoksa veri bütünlüğü hatası; rolü/aktifliği yanlışsa hedef doğrulama hatası |
| `ONAYLA` | Hedef yok | `assignedTo=null` |
| `REDDET` | Hedef yok | `assignedTo=null` |

Başkan Yardımcısı kaydı Başkana ilettiğinde aktör kimliği `lastDeputyId` alanına yazılır. Başkanın `BASKAN_YARDIMCISINA_GERI_GONDER` aksiyonu bu alanı kullanır; rastgele veya o anki başka bir kullanıcıya yönlendirme yapmaz.

## Transaction, audit ve bildirimler

### İşlem sırası

```mermaid
sequenceDiagram
    participant C as İstemci
    participant W as WorkflowActionService
    participant A as WorkflowApplicationService
    participant R as RecordPortAdapter
    participant D as AuditService
    participant E as Spring Event
    participant N as Uygulama içi bildirim
    participant M as E-posta

    C->>W: POST workflow action
    W->>W: Transaction başlat
    W->>A: performAction
    A->>A: Aktör + kayıt + hedef + geçiş doğrulama
    A->>R: update(expectedVersion)
    R->>R: saveAndFlush
    A->>D: audit kaydı ekle
    A->>E: WorkflowStatusChangedEvent yayınla
    E->>N: Aynı transaction'da bildirimi yaz
    W->>W: Commit
    E-->>M: AFTER_COMMIT
    M-->>M: @Async e-posta gönder
    W-->>C: WorkflowActionResponse
```

Kayıt güncellemesi, workflow audit kaydı ve uygulama içi bildirim tek transaction içindedir. Bu işlemlerden biri başarısız olursa tamamı geri alınır. E-posta dış sistem yan etkisi olduğu için yalnız başarılı commit sonrasında ve asenkron gönderilir.

### Audit

Her başarılı geçiş aşağıdaki bilgileri append-only `audit_logs` kaydına dönüştürür:

- kayıt kimliği;
- aksiyon;
- önceki ve yeni durum;
- aktör kimliği ve işlem anındaki rolü;
- açıklama;
- işlem zamanı.

Kayıt geçmişi şu uçtan okunur:

```http
GET /api/audit-logs/record/{recordId}
```

Okuma öncesinde `RecordAccessPolicy.assertCanView` çalışır. `AuditLogResponse` aktörü iç içe nesne yerine düz `userId`, `userFullName`, `roleId` ve `roleName` alanlarıyla döndürür. Audit kaydını güncelleyen veya silen bir HTTP ucu yoktur; veritabanı seviyesinde değiştirilemezlik ayrıca güçlendirilmelidir.

### Bildirim alıcısı

| Geçiş sonucu | Uygulama içi bildirim ve e-posta alıcısı |
| --- | --- |
| Bir kullanıcıya atanan kayıt | Yeni `assignedTo` kullanıcısı |
| `ONAYLANDI` veya `REDDEDILDI` | Kaydı oluşturan kullanıcı |

Uygulama içi mesaj 500 karaktere sığacak şekilde kısaltılır. Bildirim türü aksiyondan `RECORD_SUBMITTED`, `RECORD_FORWARDED`, `RECORD_RETURNED`, `RECORD_APPROVED` veya `RECORD_REJECTED` olarak türetilir.

Bildirim okuma uçları:

| Metot ve adres | Davranış |
| --- | --- |
| `GET /api/notifications?page=0&size=20` | Oturum kullanıcısının okunmuş ve okunmamış geçmişi; en yeniden eskiye, `size` en fazla 100 |
| `GET /api/notifications/unread` | Okunmamış bildirimler |
| `GET /api/notifications/unread/count` | Okunmamış bildirim sayısı |
| `PUT /api/notifications/{id}/read` | Yalnız bildirimin sahibi için okundu işareti |

Geçmiş listesinde istemcinin `sort` parametresi kullanılmaz; sıra daima backend tarafından en yeniden eskiye sabitlenir.

### E-posta

E-posta gövdesi `templates/mail/workflow-status.html` Thymeleaf şablonundan üretilir. Kullanıcıdan gelen başlık ve açıklama `th:text` ile HTML kaçışlı yazılır. Mesaj, kayıt detayına `${FRONTEND_URL}/records/{recordId}` biçiminde deep link içerir; gönderen adresi `MAIL_FROM` ile yapılandırılır.

SMTP veya şablon hatası loglanır, workflow transaction'ı geri alınmaz. Mevcut uygulamada retry, outbox veya dead-letter queue yoktur.

## Hata sözleşmesi

Standart hata gövdesi:

```json
{
  "code": "WORKFLOW_COMMENT_REQUIRED",
  "message": "Bu işlem için açıklama zorunludur",
  "status": 400,
  "timestamp": "2026-08-13T15:30:00"
}
```

Bean Validation hatalarında ayrıca `fieldErrors` bulunur. Mevcut `ApiError` modelinde `path` veya `traceId` alanı yoktur.

| Kod/durum | Mevcut HTTP | Ne zaman oluşur? |
| --- | --- | --- |
| `RESOURCE_NOT_FOUND` | `404` | Kayıt yoksa veya workflow için soft-delete edilmişse |
| `WORKFLOW_ROLE_NOT_ALLOWED` | `403` | `ADMIN` gibi workflow dışı rol aksiyon denerse |
| `WORKFLOW_FORBIDDEN` | `403` | Aktör kaydın gerekli sahibi/atananı değilse |
| `WORKFLOW_RECORD_LOCKED` | `409` | Terminal kayıtta aksiyon denenirse |
| `WORKFLOW_INVALID_TRANSITION` | `400` | Durum–aksiyon–rol birleşimi tanımlı değilse |
| `WORKFLOW_COMMENT_REQUIRED` | `400` | Zorunlu açıklama yoksa veya boşsa |
| `WORKFLOW_TARGET_REQUIRED` | `400` | Rezerve kod; hedefi her aksiyonda backend çözdüğü için bunu üreten bir aksiyon kalmadı |
| `WORKFLOW_TARGET_NOT_ALLOWED` | `400` | İstek `targetUserId` taşırsa (artık bütün aksiyonlar için) |
| `WORKFLOW_TARGET_ROLE_INVALID` | `400` | Hedef bulunamazsa veya beklenen rolde değilse |
| `WORKFLOW_TARGET_INACTIVE` | `400` | Hedef kullanıcı pasifse |
| `WORKFLOW_STATUS_NOT_CONFIGURED` | `500` | Rezerve kod; mevcut enum-tabanlı akışta bunu üreten bir yol yoktur |
| `WORKFLOW_VERSION_CONFLICT` | `409` | Kayıt, istek hazırlanırken başka bir işlem tarafından değiştirilmişse. Durum makinesi üretmez; `RecordPortAdapter` flush anındaki `@Version` çatışmasını bu koda çevirir |
| `VERSION_CONFLICT` | `409` | Aynı çatışmanın workflow dışı yazmalarda (ör. kayıt güncelleme) oluşan hâli; `GlobalExceptionHandler` emniyet ağı üretir |
| `WORKFLOW_ROLE_NOT_CONFIGURED` | `409` | Tekil rol hedefi çözülemezse: `BASKANA_ILET` için aktif Başkan, `GONDER`/`TEKRAR_GONDER` için aktif Başkan Yardımcısı sayısı 1 değilse. Kalıcı kural ihlali değil geçici çatışma olduğu için `4xx`; sunucu tarafında `WARN` olarak loglanır |
| `VALIDATION_ERROR` | `400` | `action` yoksa veya `comment` 2000 karakteri aşarsa |
| `UNAUTHORIZED` | `401` | Geçerli kimlik doğrulama yoksa |

`WorkflowDataIntegrityException` gibi beklenmeyen referans bütünlüğü sorunları şu anda genel handler'a düşer ve `500 INTERNAL_ERROR` olur; ayrıntı istemciye sızdırılmaz.

## Eşzamanlılık ve kayıt kilidi

Kayıt entity'sindeki sürüm alanı optimistic locking için kullanılır. Uygulama servisi kaydı okuduğu sürümle `WorkflowRecordUpdate` üretir. `RecordPortAdapter`:

1. Kaydı yeniden okur.
2. Mevcut sürümü beklenen sürümle karşılaştırır.
3. Durum, atama ve `lastDeputyId` alanlarını değiştirir.
4. `saveAndFlush` ile veritabanı hatasını transaction bitmeden görünür hale getirir.

> 2. adımdaki karşılaştırma **ikincil** bir savunmadır: çağrıyla aynı transaction içinde 1. adımdaki okuma persistence context'ten aynı managed entity'yi döndürdüğü için sürümler genellikle eşit çıkar. Asıl koruma 4. adımdaki flush anında Hibernate'in `@Version` kontrolüdür — ürettiği `UPDATE ... WHERE id = ? AND version = ?` ifadesi sıfır satır güncellediğinde çatışma doğar.

Soft-delete edilmiş kayıt, workflow uygulama servisi tarafından bulunamamış gibi değerlendirilir ve `404 RESOURCE_NOT_FOUND` döner.

Sürüm çatışması `409 WORKFLOW_VERSION_CONFLICT` olarak döner. `RecordPortAdapter`, altyapıya özgü `OptimisticLockingFailureException`'ı port sınırını geçmeden bu koda çevirir (özgün istisna `cause` olarak korunur), böylece workflow çekirdeği persistence teknolojisini tanımamaya devam eder. Workflow dışındaki yazmalarda (örneğin `RecordServiceImpl`) oluşan çatışmalar ise `GlobalExceptionHandler`'daki emniyet ağına düşer ve `409 VERSION_CONFLICT` döner; ikisi de aynı kullanıcı mesajını taşır ve `WARN` olarak loglanır.

İstemci `409` aldığında kayıt detayını yeniden çekmeli, kullanıcıya güncel durumu göstermeli ve aksiyonu otomatik olarak tekrarlamamalıdır.

## Test kapsamı

Mevcut otomatik testler şu katmanları kapsar:

- merkezi geçiş tablosunun sekiz izinli geçişi;
- validator'ın rol, terminal durum, ilişki, açıklama ve hedef kontrolleri;
- hedef kullanıcı çözümleme senaryoları;
- uygulama servisinin başarılı ve reddedilen akışları;
- istek DTO'su Bean Validation kuralları;
- controller'ın başarılı işlem ve temel hata cevapları;
- güvenlik aktörünün kimlik/aktiflik/rol çözümlemesi;
- JPA record adaptörünün sürüm kontrolü ve `saveAndFlush` davranışı;
- event publisher ve bildirim listener davranışı;
- bildirim geçmişi, sahiplik ve sayfalama servisi;
- Thymeleaf e-posta şablonu ve HTML escaping.

Doğrulama tabanı olan commit üzerinde `workflow` paketindeki **164 test** ve backend `verify`'ın tamamı (**448 test**) temiz bir PostgreSQL 15 örneğinde hatasız geçmiştir.

Bu testlerin 11'i (`WorkflowTransitionPersistenceIntegrationTest`) gerçek bir PostgreSQL bağlantısı ister; veritabanı ayakta değilse `ApplicationContext` hatasıyla düşerler. Yerelde `docker compose up -d db` gerekir.

Önemli eksik testler:

- gerçek PostgreSQL üzerinde iki eşzamanlı workflow isteğinin yarış testi (uçtan uca `409` sözleşmesi artık `WorkflowTransitionPersistenceIntegrationTest` ile kapsanıyor, ancak gerçek paralel istek yarışı hâlâ test edilmiyor);
- record update + audit + uygulama içi bildirimin birlikte rollback entegrasyon testi;
- backend ve gerçek frontend arasında workflow uçtan uca testi;
- bildirim geçmişinin gerçek PostgreSQL'de kullanıcı kapsamı, sırası ve sayfalama testi.

## Bilinen boşluklar ve kararlar

1. ~~**Optimistic-lock hata eşlemesi**~~ — **çözüldü.** `RecordPortAdapter` çatışmayı `WORKFLOW_VERSION_CONFLICT`'e çeviriyor, handler bu kodu `409`'a eşliyor ve workflow dışı yazmalar için `OptimisticLockingFailureException` → `409 VERSION_CONFLICT` emniyet ağı var. Uçtan uca doğrulama `WorkflowTransitionPersistenceIntegrationTest` içinde.
2. ~~**Tekil Başkan Yardımcısı ve istek hedefi**~~ — **çözüldü (C1).** `GONDER`/`TEKRAR_GONDER` hedefini artık backend, `BASKANA_ILET` ile aynı yoldan tek aktif kullanıcıdan çözer; istemci hedef göndermez, gönderirse istek reddedilir. Geriye kalan tek risk aşağıdaki 10. maddededir: tekil rol invariant'ı veritabanı kısıtıyla değil okuma anında zorlanır.
3. ~~**Frontend entegrasyonu**~~ — **çözüldü.** `WorkflowContext` ve `transitionRecord` mock geçiş kolu frontend'den kaldırıldı; kayıt detayındaki aksiyon paneli yalnız gerçek API'yi (`useRecordWorkflowAction`) kullanıyor. Geçiş kuralı artık tek yerde, backend'de duruyor.
4. ~~**İlk parola değişimi**~~ — **çözüldü.** `JwtAuthenticationFilter` parola değişimi bekleyen kullanıcıyı `403 PASSWORD_CHANGE_REQUIRED` ile durduruyor; workflow dahil bütün korumalı uçlar kapalı. Açık bırakılanlar yalnızca parola değiştirme, çıkış ve `GET /api/users/me`.
5. **E-posta teslim garantisi:** Gönderim asenkron ve best-effort'tur; retry/outbox/DLQ yoktur.
6. **Audit değiştirilemezliği:** Uygulama yazma/silme ucu sunmaz, fakat veritabanı rolü veya trigger ile append-only kuralı zorlanmaz.
7. **Bildirim geçmişi indeksi:** Büyüyen veri için `(user_id, created_at DESC)` birleşik indeksi değerlendirilmelidir.
8. ~~**Sözleşme drift'i (`BASKAN_ONAYINDA`)**~~ — **çözüldü.** İfade entegrasyon sözleşmesinde artık geçmiyor.
9. ~~**Terminal ek silme ve dosya IDOR'u**~~ — **çözüldü.** `deleteFile` artık `RecordLockValidator.assertModifyAllowed` çağırıyor: soft-delete kontrolü, `created_by` sahiplik kontrolü ve yalnız `TASLAK`/`DUZENLEME_BEKLIYOR` durum kilidi. `downloadFile`, `previewFile` ve `listByRecord` ise `RecordAccessPolicy.assertCanView` üzerinden kayıt görünürlüğüyle sınırlı.
10. **Tekil rolün yeniden etkinleştirilmesi:** `setActive(..., true)` aynı rolde başka aktif kullanıcı olup olmadığını kontrol etmez. İki aktif Başkan oluşursa `BASKANA_ILET`, iki aktif Başkan Yardımcısı oluşursa `GONDER`/`TEKRAR_GONDER` hedefi tekilleştiremediği için `409 WORKFLOW_ROLE_NOT_CONFIGURED` ile durur. C1 sonrası bu, Çalışanın en sık kullandığı aksiyonu da etkilediği için invariant'ın yazma tarafında (rol atama/aktifleştirme) zorlanması daha önemli hâle geldi.
11. ~~**Koltuk devrinde `last_deputy_id` bayat kalıyor**~~ — **çözüldü (M5, 20 Ağustos 2026).** `RecordRepository.updateLastDeputyId` eklendi ve `UserService.kullaniciIsleriniDevret` içinde `devretBekleyenIsleri` ile **aynı transaction'da** çağrılıyor. Koltuk devrinden sonra `BASKAN_YARDIMCISINA_GERI_GONDER` yeni yardımcıyı çözüyor; devredilen kayıtlar yeni yardımcının görünürlük kapsamına da giriyor. `UserServiceTest` kapsıyor.

Başlangıç şartnamesiyle bilinçli veya fiilî uygulama farkları da korunmalıdır:

- Başkan geri gönderme hedefini serbestçe seçmez; Çalışana dönüş `createdBy`, Başkan Yardımcısına dönüş `lastDeputyId` ile sabittir.
- Şartnamedeki “tüm ilgililer” ifadesine karşılık mevcut uygulama her olay için tek alıcı seçer: yeni atanan kullanıcı, terminal geçişte ise kaydı oluşturan kullanıcı.

## Değişiklik kontrol listesi

Yeni bir workflow durumu veya aksiyonu eklenirken en az şu işler aynı değişiklikte yapılmalıdır:

1. `RecordStatus` veya `WorkflowAction` enum'unu güncelleyin.
2. İzinli birleşimi yalnız `TransitionRules` içine ekleyin; controller/service içinde paralel kural yazmayın.
3. Hedef, açıklama ve aktör ilişkisini `WorkflowAction`/validator modelinde tanımlayın.
4. Hedef çözümleme gerekiyorsa `TargetUserResolver` ve port testlerini güncelleyin.
5. Audit ve bildirim türü/alıcı davranışını belirleyin.
6. `WorkflowErrorCode` ve gerçek HTTP eşlemesini birlikte ekleyin.
7. Durum makinesi, uygulama servisi, controller ve entegrasyon testlerini güncelleyin.
8. OpenAPI istemcisini yeniden üretin ve frontend mock/adapter katmanını eşleyin.
9. Bu belgeyi, `README.md` özetini ve frontend–backend sözleşmesini aynı PR'da güncelleyin.
