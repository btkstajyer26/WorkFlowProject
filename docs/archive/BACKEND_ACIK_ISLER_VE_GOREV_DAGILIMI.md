# Backend Açık İşler ve Görev Dağılımı

> [!NOTE]
> **Bu belge 19 Ağustos 2026'da kapatılmıştır ve tarihsel kayıttır.**
> Aşağıdaki durum tabloları 17 Ağustos'taki entegrasyon durumunu yansıtır.
> Güncel durum için [README](../README.md) "Mevcut durum" ve "Bilinen eksikler"
> bölümlerine, workflow davranışı için [workflow.md](workflow.md)'ye bakınız.
>
> **Kapanış durumu:** §3 tablosundaki 12 işin **11'i tamamlandı** —
> **A1** (`JwtAuthenticationFilter` parola değişimini zorluyor),
> **A2** (dosya yetkilendirmesi), **A3** (`UserService.java:169` aday `CALISAN`
> kontrolü), **A4** (refresh token `jti`), **B1** (dosya listesi ucu),
> **B2** (`creator` filtresi), **C1a** (durum makinesi hedef bayrağı),
> **C1b** (`TargetUserResolver` tekil yardımcıyı backend'de çözüyor),
> **C2a** (`RecordServiceImpl`'de üç `recordLifecycleEvent` çağrısı),
> **C2b** (`recordLifecycleEvent`) ve C3 dışındaki her şey.
>
> **C3 — e-posta ve bildirim alıcı matrisi (Melih): 20 Ağustos 2026'da kapandı.**
> `feature/notification-service` `test`'e entegre edildi;
> `WorkflowStatusChangedListener.recipientsOf` artık atama yapılan geçişte
> atanan kullanıcıya, nihai onay/ret geçişinde hem kaydı oluşturana hem kaydı
> Başkana ileten yardımcıya (`last_deputy_id`) bildirim üretiyor.
> **§3 tablosundaki 12 işin 12'si tamamlandı; bu belgede açık iş kalmadı.**
>
> **Test durumu:** 352 test / 3 hata → **383 test / 0 hata** (temiz PostgreSQL 15).

**Tarih:** 17 Ağustos 2026
**Kaynak:** Frontend ekibinin *"Frontend–Backend Güncel Eksikler ve Karar
Noktaları"* belgesi (16 Ağustos)
**Doğrulama tabanı:** `integration/tum-feature-branchleri` @ `956aaf3`

Frontend'in bildirdiği dokuz backend maddesinin tamamı güncel kod üzerinde tek
tek açılıp doğrulandı; dokuzu da hâlâ geçerli. Aşağıda **önce karar verilmesi
gerekenler**, sonra **kişi kişi ne yazılacağı** var.

Herkes kendi bölümünü okuyup doğrudan uygulayabilir: hangi klasör, hangi dosya,
hangi satır, hangi test.

---

## 1. Kararlar

**Beş karar da verildi, hiçbir iş karar bekliyor değil.** Aşağıda ne
kararlaştırıldığı ve neden öyle olduğu var. Frontend'e sorulacak tek bir küçük
konu kaldı, en sonda.

| # | Konu | Karar |
|---|---|---|
| 1 | Kayda ait dosyalar nasıl dönecek? | **Ayrı uç:** `GET /api/records/{id}/files` |
| 2 | Dosya alan adları değişecek mi? | **Hayır**, mevcut adlar kalıyor |
| 3 | Oluşturana göre filtre neyle çalışacak? | **Ad/soyad metni:** `?creator=ahmet` |
| 4 | `targetUserId` yine gönderilirse? | **Reddedilecek** (`WORKFLOW_TARGET_NOT_ALLOWED`) |
| 5 | Genişletilmiş alıcı kümesi nerede geçerli? | **Hem e-postada hem uygulama içi bildirimde** |

### Karar 1 — Ayrı uç

`GET /api/records/{id}/files` yazılacak; `RecordResponse` içine `files` dizisi
**gömülmeyecek**. Gömme, `record` modülünü `attachment` modülüne bağlar ve iki
ekibi tek işe kilitlerdi. Detay ekranındaki bir ek istek buna değmez.

### Karar 2 — Dosya alan adları değişmiyor

Frontend `fileName`, `contentType`, `size`, `createdAt` istemişti; mevcut
`FileResponseDto` `originalName`, `mimeType`, `fileSize`, `uploadedAt` taşıyor.
**Mevcut adlar kalıyor**, çünkü bu adlar daha doğru:

- `mimeType` değeri istemcinin gönderdiği `Content-Type` başlığından değil,
  **dosya içeriğinden** tespit ediliyor (`FileContentValidator.detectAndValidate`).
  Alana `contentType` demek, tam da backend'in bilerek güvenmediği şeyi
  çağrıştırırdı.
- `originalName`, `storedName`'in karşıtı. Diskteki ad rastgele GUID; kullanıcıya
  gösterilen ad bu. `fileName` demek bu ayrımı siler.
- `uploadedAt`, `createdAt`'ten daha kesin: dosyanın yüklendiği an.

Değiştirmenin bedeli DTO + OpenAPI + frontend'in yeniden üretimi; kazancı sıfır.
Adlar sözleşmeye bu hâliyle yazılacak.

### Karar 3 — Oluşturan filtresi ad/soyad metniyle çalışacak

> *"Neden id ile erişemiyoruz? Kişi oturum açınca zaten kendi kayıtlarına
> erişmesi gerekmez mi?"* — Haklı soru; cevabı bu filtrenin **kimin için**
> olduğunda.

**Çalışan zaten yalnız kendi kayıtlarını görüyor** ve bu, filtreyle değil
sorgunun kendisiyle sağlanıyor. `RecordSpecifications` içinde görünürlük
kapsamı şöyle:

```java
case CALISAN -> cb.equal(root.get("createdBy"), currentUserId);
```

Yani Çalışan için "oluşturana göre filtre" **işlevsiz** — listesinde başkasının
kaydı hiç yok. Kendi kayıtlarına erişmesi için hiçbir şey yapılmasına gerek yok,
bugün de öyle çalışıyor.

Bu filtreye ihtiyacı olanlar **Başkan ve Başkan Yardımcısı**: onların listesinde
farklı Çalışanların gönderdiği evraklar var ve "Ahmet'in gönderdiklerini göster"
demek istiyorlar.

UUID neden işe yaramaz: Başkan Yardımcısı bir kişiyi **adıyla** tanıyor, UUID'siyle
değil. UUID ile filtrelemek için "kullanıcıları listele" ucuna ihtiyaç olurdu;
tekil Başkan Yardımcısı kararı gereği Admin dışı rollere böyle bir uç
**açılmayacak** (frontend belgesi F bölümü). Yani UUID'yi bulmanın güvenli bir
yolu yok.

**Karar:** `?creator=<metin>` — `users.first_name` / `users.last_name` üzerinde
büyük-küçük harf duyarsız arama. Mevcut görünürlük kapsamıyla **AND**'lenir,
kimseye yeni kayıt erişimi kazandırmaz.

### Karar 4 — `targetUserId` gönderilirse reddedilecek

C1'den sonra hedefi backend çözecek. Frontend yine de `targetUserId` gönderirse
istek `WORKFLOW_TARGET_NOT_ALLOWED` ile reddedilecek — sessizce yok sayılmayacak.
Sebep: kural zaten kodda var ve `BASKANA_ILET` dahil **diğer bütün aksiyonlar**
böyle davranıyor; `GONDER`/`TEKRAR_GONDER` istisnaydı. İstisna kalkınca davranış
tek tipleşiyor ve frontend'de kalmış bir hata sessizce gizlenmiyor.

### Karar 5 — Alıcı kümesi her iki kanalda da aynı

Nihai onayda genişleyen alıcı kümesi hem e-postaya hem uygulama içi bildirime
uygulanacak. Aksi hâlde kullanıcı e-posta alıp zil ikonunda hiçbir şey görmezdi.

---

### Frontend'e sorulacak tek şey 🟡

Karar 3'ün bir yan sonucu var: `RecordSearchResponse` bugün `createdBy`
alanında **yalnız UUID** taşıyor, oluşturanın adını taşımıyor. Başkan
Yardımcısı `?creator=ahmet` ile filtreleyebilecek ama dönen listede kimin
evrağı olduğunu göremeyecek.

**Soru:** Liste cevabına oluşturanın adı (`createdByName`) eklensin mi?

- Eklenirse: `RecordSearchResponse` + `RecordSearchServiceImpl`'de küçük bir
  değişiklik — Irmak'ın işine bir alan eklenir.
- Eklenmezse: filtre çalışır ama liste "kimin evrağı" bilgisini göstermez.

Bu backend'i bloke etmiyor; Irmak filtreyi yazarken cevap gelirse aynı PR'da
halleder. **Soracak:** Irmak Tanrıverdi → frontend.

---

## 2. Görevler — kim ne yazacak

Ortak kural: dal `test`'ten açılır (`feature/<konu>`), PR `test`'e gider.
Her iş **testiyle birlikte** gelir.

---

### 👤 Nisan Tat · Sümeyye Baykan — `auth`, `user`

#### İş A1 — Zorunlu şifre değişimini backend'de zorla 🔴

Bugün `mustChangePassword=true` olan kullanıcı token'ıyla her ucu çağırabiliyor;
kural yalnız arayüzde. `mustChangePassword` kodda sadece `User` entity'si ve
`LoginResponse` içinde geçiyor, hiçbir yerde okunmuyor.

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/auth/security/JwtAuthenticationFilter.java`

Mevcut `doFilterInternal` içinde, pasif kullanıcı kontrolünün (`!userDetails.isEnabled()`)
hemen **altına** şunu ekleyin:

1. `userDetails`'i `AuthenticatedUser`'a çevirin (`instanceof` ile) ve
   `getUser().isMustChangePassword()` değerini okuyun.
2. `true` ise ve istek yolu şu üçünden **biri değilse** zinciri kesin:
   - `POST /api/auth/change-password`
   - `POST /api/auth/logout`
   - `GET /api/users/me`
3. Kesme durumunda cevabı **filtre içinde** yazın: `403`, `Content-Type: application/json`,
   gövde `ApiError` (`code = "PASSWORD_CHANGE_REQUIRED"`).
   `filterChain.doFilter(...)` **çağrılmaz**.

> ⚠️ Exception fırlatmayın. Filtre `GlobalExceptionHandler`'dan önce çalışır,
> fırlatılan hata `ApiError` gövdesine dönüşmez. Aynı sorunu
> `rbac/config/SecurityErrorHandlers.java` çözmüş; oradaki `write(...)` metodunun
> aynısını kullanın. Hacer bu metodu ortak bir sınıfa çıkaracak (bkz. Hacer/A1
> desteği) — o gelene kadar aynı altı satırı filtreye yazıp sonra ortak sınıfa
> geçin.

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/auth/security/JwtAuthenticationFilterTest.java` (dosya zaten var)
- `mustChangePassword=true` + `/api/categories` → 403, gövdede `PASSWORD_CHANGE_REQUIRED`
- `mustChangePassword=true` + `/api/auth/change-password` → zincir devam ediyor
- `mustChangePassword=false` + herhangi bir uç → zincir devam ediyor

**Bitti sayılır:** Parola değişimi bekleyen kullanıcının access token'ıyla
`GET /api/categories` çağrısı 403 dönüyor.

---

#### İş A3 — Devralan adayın CALISAN olduğunu doğrula 🔴

Bugün aday sadece "aktif mi" diye kontrol ediliyor. API doğrudan çağrılırsa
Admin veya Başkan yanlışlıkla Başkan Yardımcısı yapılabilir.

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/user/service/UserService.java`
**Metot:** `assignBaskanYardimcisi(UUID replacementUserId, UUID previousHolderId)` (~153. satır)

Mevcut `if (!replacement.isActive())` kontrolünün hemen altına ekleyin:

```java
if (!"CALISAN".equals(replacement.getRole().getName())) {
    throw new BusinessRuleException(
            "Başkan Yardımcısı yalnızca Çalışan rolündeki bir kullanıcıya devredilebilir");
}
```

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/user/service/UserServiceTest.java`
- Aday `ADMIN` → `BusinessRuleException`
- Aday `BASKAN` → `BusinessRuleException`
- Aday aktif `CALISAN` → devir başarılı

**Bitti sayılır:** Admin/Başkan adayla devir denemesi `400 BUSINESS_RULE_VIOLATION`.

---

#### İş A4 (destek) — Rotasyon testi

Hacer `jti` ekledikten sonra:

**Dosya:** `backend/src/test/java/btk/staj/WorkFlowProject/auth/service/AuthServiceTest.java`
- Aynı kullanıcı için arka arkaya iki login → iki farklı refresh token
- Aynı kullanıcı için arka arkaya iki rotasyon → iki farklı refresh token

---

### 👤 Hacer Bengü Ünal — `rbac`, `common`

#### İş A4 — Refresh token'a `jti` ekle 🔴

Refresh token bugün yalnız `subject` + `issuedAt` + `expiration` içeriyor.
`issuedAt` saniye hassasiyetinde olduğu için aynı kullanıcı aynı saniyede iki
kez login olursa **birebir aynı token** üretiliyor ve `tokens_token_key` unique
kısıtı 409 veriyor.

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/rbac/config/JwtUtil.java`
**Metot:** `generateRefreshToken(UUID userId)` (40. satır)

Builder zincirine bir satır ekleyin:

```java
public String generateRefreshToken(UUID userId) {
    return Jwts.builder()
            .id(UUID.randomUUID().toString())   // jti — aynı saniyedeki token'ları ayırır
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(getSigningKey())
            .compact();
}
```

`.id(...)` jjwt'de `jti` claim'ini yazar. Doğrulama tarafında değişiklik
gerekmiyor — `jti` okunmuyor, yalnız token'ı benzersizleştiriyor.

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/rbac/config/JwtUtilTest.java` (yeni dosya)
- Aynı `userId` ile peş peşe iki `generateRefreshToken` → iki farklı string
- Üretilen token hâlâ `isTokenValid` geçiyor ve `subject` doğru çözülüyor

**Bitti sayılır:** Aynı saniyede iki login 409 üretmiyor.

---

#### İş A1 (destek) — `ApiError` yazıcısını ortak sınıfa çıkar 🟢

`SecurityErrorHandlers.write(...)` bugün private ve o sınıfa gömülü.
`JwtAuthenticationFilter` de aynı gövdeyi yazmak zorunda (A1).

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/common/exception/ApiErrorWriter.java` (yeni)

`write(HttpServletResponse, HttpStatus, String code, String message)` metodunu
buraya taşıyın, `@Component` yapın, `SecurityErrorHandlers` de bunu kullansın.
Böylece filtre ve güvenlik handler'ları aynı gövdeyi üretir.

**Bitti sayılır:** `SecurityErrorHandlers` içinde kopya `write` kalmadı, filtre
aynı bileşeni kullanıyor.

---

### 👤 Ecesu Başak — `attachment`

Bu iki iş aynı dosyalara dokunuyor, **birlikte yapılmalı**.

#### İş A2 — Dosya uçlarına yetki kontrolü 🔴

Bugün:
- `GET /api/files/{id}/download` ve `/preview` yalnız `id` alıyor; UUID'yi bilen
  **her oturumlu kullanıcı** indirebiliyor.
- `RecordLockValidator.assertUploadAllowed` yalnız kaydın silinmiş/terminal
  olup olmadığına bakıyor; **her Çalışan başkasının kaydına dosya ekleyebiliyor**.
- `deleteFile` sahiplik hiç kontrol etmiyor.

**Dosya 1:** `backend/src/main/java/btk/staj/WorkFlowProject/attachment/service/RecordLockValidator.java`

`assertUploadAllowed(UUID recordId)` metodunu
`assertModifyAllowed(UUID recordId, UUID currentUserId)` olarak genişletin:

1. Kayıt yoksa → `ResourceNotFoundException` (bugün `IllegalArgumentException`).
2. `record.getDeletedAt() != null` → `BusinessRuleException`.
3. **Yeni:** `!currentUserId.equals(record.getCreatedBy())` → `ForbiddenException`.
4. **Yeni:** durum `TASLAK` veya `DUZENLEME_BEKLIYOR` değilse →
   `BusinessRuleException`. (Bugünkü `isTerminal()` kontrolü yetersiz:
   `BSK_YRD_INCELEMESINDE` terminal değil ama dosya eklenmemeli.)

**Dosya 2:** `backend/src/main/java/btk/staj/WorkFlowProject/attachment/service/FileService.java`

1. Sınıfa iki bağımlılık ekleyin: `RecordRepository` ve `RecordAccessPolicy`
   (ikisi de mevcut bean, yeni sınıf yazılmayacak).
2. Şu private yardımcıyı ekleyin:

```java
private void assertCanViewRecord(UUID recordId, RoleName role, UUID currentUserId) {
    Record record = recordRepository.findById(recordId)
            .orElseThrow(() -> new ResourceNotFoundException("Kayıt bulunamadı: " + recordId));
    recordAccessPolicy.assertCanView(
            role, currentUserId, record.getCreatedBy(), record.getAssignedTo(), record.getStatus());
}
```

3. `downloadFile` ve `previewFile` imzalarına oturum bilgisini ekleyin:
   `downloadFile(UUID id, RoleName role, UUID currentUserId)`. Dosyayı
   bulduktan sonra `assertCanViewRecord(fileEntity.getRecordId(), role, currentUserId)`
   çağırın.
4. `uploadFile` → `recordLockValidator.assertModifyAllowed(recordId, uploadedBy)`.
5. `deleteFile` → dosyayı bulduktan sonra
   `recordLockValidator.assertModifyAllowed(fileEntity.getRecordId(), deletedBy)`.
6. `IllegalArgumentException` kullanan yerleri tipli exception'lara çevirin.

**Dosya 3:** `backend/src/main/java/btk/staj/WorkFlowProject/attachment/controller/FileController.java`

1. Bütün `try/catch` blokları kaldırılsın; hata gövdesini
   `GlobalExceptionHandler` üretsin (düz metin `body(e.getMessage())` kalmasın).
2. `download`/`preview` uçları `@AuthenticationPrincipal AuthenticatedUser currentUser`
   alsın ve rol + id'yi servise geçsin.

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/attachment/service/FileServiceAuthorizationTest.java` (yeni)
- Başkasının kaydındaki dosyayı indirme → `ForbiddenException`
- Başkasının kaydına upload → `ForbiddenException`
- Başkasının dosyasını silme → `ForbiddenException`
- `BASKAN_YARDIMCISI` kendisine atanan kaydın dosyasını indirebiliyor
- `BASKAN_ONAYINDA` durumundaki kayda upload → `BusinessRuleException`

**Bitti sayılır:** Yukarıdaki beş senaryo testte yeşil; `FileController`'da
`try/catch` kalmadı.

---

#### İş B1 — Kayda ait dosya listesi + tipli upload cevabı 🟢

Frontend bilmediği dosya UUID'siyle indirme isteği gönderemiyor.

> Karar 1 gereği ayrı uç yazılacak, `RecordResponse`'a gömülmeyecek.
> Karar 2 gereği alan adları **değişmeyecek**; `FileResponseDto` olduğu gibi kalıyor.

**İyi haber:** repository metodu **zaten var** —
`FileRepository.findAllByRecordIdAndDeletedAtIsNull(UUID recordId)`. Yeni sorgu
yazılmayacak.

**Dosya:** `attachment/controller/FileController.java` + `attachment/service/FileService.java`

1. `FileService`'e ekleyin:

```java
@Transactional(readOnly = true)
public List<FileResponseDto> listByRecord(UUID recordId, RoleName role, UUID currentUserId) {
    assertCanViewRecord(recordId, role, currentUserId);   // A2'deki yardımcı
    return fileRepository.findAllByRecordIdAndDeletedAtIsNull(recordId)
            .stream().map(FileController::toDto).toList();
}
```
(`toDto` bugün controller'da private; servise taşıyın.)

2. `FileController`'a ekleyin:

```java
@GetMapping("/api/records/{id}/files")
public List<FileResponseDto> listFiles(@PathVariable("id") UUID recordId,
                                       @AuthenticationPrincipal AuthenticatedUser currentUser) { ... }
```

3. `uploadFile`'ın dönüş tipini `ResponseEntity<?>` yerine
   **`ResponseEntity<FileResponseDto>`** yapın. OpenAPI'nin `object` üretmesinin
   sebebi bu `?`.

**Test:** `FileServiceAuthorizationTest`'e iki vaka
- Kaydı görebilen kullanıcı listeyi alıyor, silinmiş dosyalar listede yok
- Göremeyen kullanıcı → `ForbiddenException`

**Bitti sayılır:** `GET /api/records/{id}/files` çalışıyor ve OpenAPI'de dönüş
tipi `FileResponseDto[]` görünüyor.

---

### 👤 Irmak Tanrıverdi — `search`

#### İş B2 — Oluşturana göre sunucu taraflı filtre 🟢

Şartname kayıtların oluşturana göre filtrelenmesini istiyor. Bugün
`RecordSearchCriteria` yalnız `q`, `status`, `categoryId`, `from`, `to` taşıyor;
`q` sadece başlık ve açıklamada arıyor.

> Karar 3 gereği filtre **ad/soyad metniyle** çalışacak (`?creator=ahmet`),
> UUID'yle değil. Gerekçesi kararlar bölümünde: Çalışan zaten yalnız kendi
> kayıtlarını görüyor, filtreye ihtiyacı olan Başkan/Bşk.Yrd. kişiyi adıyla
> tanıyor ve onlara kullanıcı listeleme ucu açılmayacak.

**Dosya 1:** `backend/src/main/java/btk/staj/WorkFlowProject/search/dto/RecordSearchCriteria.java`

`private String creator;` alanını + getter/setter ekleyin.

**Dosya 2:** `backend/src/main/java/btk/staj/WorkFlowProject/search/specification/RecordSpecifications.java`

`creator` doluysa: `records.created_by` → `users` join'i üzerinden
`firstName`/`lastName` alanlarında `LIKE %creator%` (büyük/küçük harf duyarsız)
predicate'i ekleyin ve mevcut predicate listesine **AND** ile bağlayın.

> ⚠️ Görünürlük kapsamı ayrı bir predicate olarak zaten uygulanıyor; yeni filtre
> onu gevşetmemeli, yalnız daraltmalı. Çalışan bu filtreyle başkasının kaydını
> **göremiyor** olmalı.

**Dosya 3 (2 satır, `record` ekibiyle koordine):**
`backend/src/main/java/btk/staj/WorkFlowProject/record/controller/RecordController.java`
→ `getAllRecords`'a `@RequestParam(required = false) String creator` ekleyip
`criteria.setCreator(creator)` çağırın.

**Test:**
- `search/specification/RecordSpecificationsTest.java` → predicate üretiliyor mu
- `search/service/RecordSearchServiceImplTest.java` → **Çalışan `creator=<başkası>`
  gönderince boş sonuç alıyor** (kapsam genişlemiyor)

**Bitti sayılır:** `GET /api/records?creator=ahmet` çalışıyor, RBAC kapsamı
korunuyor, parametre adı sözleşmeye yazıldı.

---

### 👤 Esra Öncü — `workflow` (durum makinesi çekirdeği)

#### İş C1a — `GONDER`/`TEKRAR_GONDER` hedef zorunluluğunu kaldır 🟢

Çalışanın aktif Başkan Yardımcısının id'sini öğrenebileceği güvenli bir uç yok
ve olmamalı (tekil rol kararı). `BASKANA_ILET` hedefi zaten backend çözüyor;
bu iki aksiyon da aynı hâle gelecek.

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/WorkflowAction.java`

23. ve 26. satırlardaki ilk parametreyi `true` → `false` yapın:

```java
GONDER(false, false, RoleName.BASKAN_YARDIMCISI),
TEKRAR_GONDER(false, false, RoleName.BASKAN_YARDIMCISI),
```

İlk parametre `targetUserIdRequiredInRequest`. `false` olunca istemci hedef
göndermez, hedefi servis çözer — ama `expectedTargetRole` (`BASKAN_YARDIMCISI`)
korunduğu için çözülen kullanıcının rolü ve aktifliği doğrulanmaya devam eder.

Sınıf javadoc'undaki *"Yalnizca GONDER ve TEKRAR_GONDER icin true"* cümlesi de
güncellensin.

**Dosya 2:** `backend/src/main/java/btk/staj/WorkFlowProject/workflow/statemachine/WorkflowTransitionValidator.java`

`WORKFLOW_TARGET_REQUIRED` (56. satır) ve `WORKFLOW_TARGET_NOT_ALLOWED`
(59. satır) kuralları bayrağı okuyor; bayrak değişince davranış kendiliğinden
doğru olmalı. **Doğrulayın:** Karar 4 gereği istemci yine `targetUserId`
gönderirse buradan `WORKFLOW_TARGET_NOT_ALLOWED` dönmeli — sessizce yok
sayılmamalı.

**Test:**
- `workflow/statemachine/WorkflowTransitionValidatorTest.java` → `GONDER`
  hedefsiz geçiyor; hedefle gönderilirse `WORKFLOW_TARGET_NOT_ALLOWED`
- `workflow/statemachine/TransitionRulesTest.java` → mevcut vakalar yeşil kalıyor

**Bitti sayılır:** Hedefsiz `GONDER` doğrulamayı geçiyor.

---

### 👤 Burak Kaya — `workflow` (uygulama katmanı)

#### İş C1b — Tekil Başkan Yardımcısını backend çözsün 🟢

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/workflow/service/TargetUserResolver.java`

37. satırdaki `switch` kolunu değiştirin:

```java
// önce
case GONDER, TEKRAR_GONDER -> resolveRequestedTarget(requestedTargetUserId);
// sonra
case GONDER, TEKRAR_GONDER -> resolveSingleActiveRole(RoleName.BASKAN_YARDIMCISI);
```

`resolveSingleActiveRole` **zaten var** (38. satırda `BASKANA_ILET` kullanıyor)
ve aktif kullanıcı sayısı 1 değilse `TargetResolution.RoleNotConfigured` dönüyor
— yani "yardımcı yok" ve "birden fazla yardımcı" durumları için anlamlı 409
davranışı hazır. Yeni hata kodu yazmanız gerekmiyor.

`resolveRequestedTarget` başka hiçbir yerde kullanılmıyorsa silin.
`requestedTargetUserId` **parametresi kalmalı**: Karar 4 gereği istemci yine
hedef gönderirse istek reddedilecek ve bu kontrolü validator bu değer üzerinden
yapıyor.

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/workflow/service/TargetUserResolverTest.java`
- Tek aktif yardımcı varken `GONDER` → o kullanıcı çözülüyor
- Hiç aktif yardımcı yokken → `RoleNotConfigured`
- İki aktif yardımcı varken → `RoleNotConfigured`

**Bitti sayılır:** `targetUserId` göndermeden `GONDER` uçtan uca çalışıyor.

---

### 👤 Alperen Kara · Fevzi Berke Urganioğlu — `record`

#### İş C2a — Kayıt CRUD işlemlerini audit'e yaz 🟢

Şartnamedeki işlem geçmişi "Kayıt Oluşturuldu / Düzenlendi" hareketlerini
istiyor. Bugün `RecordServiceImpl` içinde tek bir audit çağrısı yok; yalnız
workflow geçişleri yazılıyor.

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/record/service/RecordServiceImpl.java`

Ebrar'ın ekleyeceği metodu (bkz. Ebrar/C2b) üç yerden çağırın — **kaydın
kendisiyle aynı `@Transactional` içinde**, ayrı bir servis çağrısı olarak değil:

| Metot | Action | Ne zaman |
|---|---|---|
| `createRecord` | `RECORD_CREATED` | `save` sonrası, dönüşten önce |
| `updateRecord` | `RECORD_UPDATED` | `saveAndFlush` sonrası |
| `deleteRecord` | `RECORD_DELETED` | soft delete sonrası |

Aktör bilgisi zaten elinizde (`PermissionService`/`AuthenticatedUser`).
`RECORD_UPDATED` için değişiklik özetini `comment` alanına kısa bir metin olarak
koyabilirsiniz (örn. `"başlık ve kategori güncellendi"`); zorunlu değil.

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/record/service/RecordServiceImplTest.java`
- `createRecord` çağrısı audit servisini `RECORD_CREATED` ile bir kez çağırıyor
- `updateRecord` → `RECORD_UPDATED`
- `deleteRecord` → `RECORD_DELETED`

#### İş B2 (destek) — `RecordController`'a `creator` parametresi

Irmak'ın işi için `getAllRecords`'a iki satır (bkz. Irmak/B2, Dosya 3).

**Bitti sayılır:** Kayıt oluşturup güncelleyen kullanıcı
`GET /api/audit-logs/record/{id}` cevabında iki yeni satır görüyor.

---

### 👤 Ebrar Şeyma Karakuş — `audit`

#### İş C2b — Yaşam döngüsü olayları için audit metodu 🟢

`AuditLogService.record(...)` bugün yalnız `WorkflowTransitionAudit` alıyor —
yani `record` modülünün audit yazabilmesi için workflow modelini kurması
gerekirdi. Onun yerine ikinci bir giriş noktası açın.

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/audit/service/AuditLogService.java`

```java
/** Kayit yasam dongusu olaylari (olusturma/guncelleme/silme); durum gecisi yoktur. */
public void recordLifecycleEvent(UUID recordId,
                                 UUID actorId,
                                 RoleName actorRole,
                                 String action,
                                 RecordStatus currentStatus,
                                 String comment) {
    AuditLog log = AuditLog.builder()
            .recordId(recordId)
            .userId(actorId)
            .roleId(resolveRoleId(actorRole))
            .action(action)
            .previousStatus(null)
            .newStatus(currentStatus.name())
            .comment(comment)
            .createdAt(LocalDateTime.now())
            .build();
    auditLogRepository.save(log);
}
```

> ✅ **Flyway migration'a gerek yok.** `audit_logs` tablosunda
> `previous_status` zaten nullable, `new_status` ise `NOT NULL` — bu yüzden
> `newStatus` alanına kaydın **o anki durumu** yazılır (`RECORD_CREATED` için
> `TASLAK`). Şema değişmiyor.

> ✅ **Geçmiş sorgusu değişmiyor.** `AuditLogRepository.findHistoryByRecordId`
> action'a göre filtrelemiyor, `ORDER BY createdAt ASC` ile hepsini dönüyor —
> yeni olaylar workflow geçişleriyle kronolojik karışacak.

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/audit/service/AuditLogServiceTest.java`
- `recordLifecycleEvent` doğru `AuditLog` satırını kuruyor (`previousStatus` null)
- Rol adı `roles` tablosunda yoksa `IllegalStateException`

**Bitti sayılır:** `record` ekibi tek metotla audit yazabiliyor, şema değişmedi.

---

### 👤 Melih Kocaman — `notification`

#### İş C3 — Nihai onayda alıcı kümesini genişlet 🟡

Bugün `ONAYLA`/`REDDET` sonrası `assignedTo` boş kaldığı için yalnız kaydı
oluşturan bilgilendiriliyor; süreçte görev alan Başkan Yardımcısı haber almıyor.

> Karar 5 gereği genişleyen alıcı kümesi **hem e-postaya hem uygulama içi
> bildirime** uygulanacak.

**Dosya:** `backend/src/main/java/btk/staj/WorkFlowProject/notification/listener/WorkflowStatusChangedListener.java`

1. `recipientOf(event)` metodunu **`recipientsOf(event)`** yapın; dönüş tipi
   `Set<UUID>` (sıra korunsun diye `LinkedHashSet`).
2. Kural:
   - `event.assignedTo() != null` → tek alıcı, o kişi.
   - `assignedTo == null` (nihai onay/ret) → **kaydı oluşturan** *ve* **kaydı
     Başkana ileten yardımcı**.
3. Yardımcının id'si `Record.lastDeputyId` alanında duruyor
   (`record/entity/Record.java`, `last_deputy_id` kolonu). Listener'da
   `RecordRepository` **zaten enjekte edilmiş** (başlık okumak için
   kullanılıyor), yeni bağımlılık gerekmiyor:
   `recordRepository.findById(recordId).map(Record::getLastDeputyId)`.
4. `Set` kullanıldığı için aynı kişi iki role denk gelirse **mükerrer e-posta
   gitmez** — ayrı bir kontrol yazmayın.
5. `createInAppNotification` ve `sendMail` metotlarını bu küme üzerinde
   döndürün.

> ✅ Transaction davranışını **değiştirmeyin**: uygulama içi bildirim
> `@EventListener` ile aynı transaction'da, e-posta `@TransactionalEventListener(AFTER_COMMIT)`
> + `@Async` ile commit sonrası gidiyor. Frontend'in "e-posta hatası
> transaction'ı geri almamalı" isteği bu yapıyla zaten karşılanıyor.

**Test:** `backend/src/test/java/btk/staj/WorkFlowProject/notification/listener/WorkflowStatusChangedListenerTest.java`

Alıcı matrisini tablo hâlinde yazın (`@ParameterizedTest` + `@CsvSource`):

| Aksiyon | `assignedTo` | Beklenen alıcılar |
|---|---|---|
| `GONDER` | yardımcı | yalnız yardımcı |
| `BASKANA_ILET` | başkan | yalnız başkan |
| `ONAYLA` | null | oluşturan + `lastDeputyId` |
| `REDDET` | null | oluşturan + `lastDeputyId` |
| `ONAYLA` (oluşturan = yardımcı) | null | tek kişi, tek bildirim |
| `ONAYLA` (`lastDeputyId` null) | null | yalnız oluşturan |

**Bitti sayılır:** Matris testte yeşil, mükerrer e-posta yok.

---

#### İş D2 — E-posta deep link yolu 🔵 *isteğe bağlı, acil değil*

Şablon `/records/{id}` üretiyor, frontend'in kanonik yolu `/kayitlar/{id}`.
Frontend geriye uyumlu yönlendirme ekledi. Yapılacaksa
`notification/service/MailService.java` içindeki `deepLink` satırı ve
`app.frontend-url` sözleşmesi birlikte güncellenir.

---

#### 🔴 Devam eden: sızan Mailtrap parolası

`feature/notification-service` dalındaki `8c42300` commit'inde Mailtrap API
parolası düz metin duruyor ve artık `integration` ile `test` dallarından da
erişilebilir. **Mailtrap panelinden token iptal edilip yenisi üretilmeli**,
yeni değer `MAIL_PASSWORD` ortam değişkeninden verilmeli. Geçmişten temizleme
(`git filter-repo` + force-push) ancak iptalden sonra ve ekip kararıyla.

---

## 3. Durum — 17 Ağustos akşamı

Ekip föye göre çalışmaya başladı. Entegrasyon dalındaki güncel durum:

| İş | Kim | Durum |
|---|---|---|
| **A2** Dosya yetkilendirmesi | Ecesu | ✅ `test`'e girdi (`6c14e1c`) — `assertCanViewRecord`, `assertModifyAllowed`, tipli exception'lar, `FileServiceAuthorizationTest` |
| **B1** Dosya listesi ucu | Ecesu | ✅ Aynı commit — `listByRecord`, `GET /api/records/{id}/files` |
| **A4** Refresh token `jti` | Hacer | ✅ `feature/jwt-jti-ve-apierror` (PR #20) — `JwtUtilTest` ile |
| **A1 desteği** `ApiErrorWriter` | Hacer | ✅ Aynı dal — ortak sınıfa çıkarıldı, `SecurityErrorHandlers` onu kullanıyor |
| **C2b** `recordLifecycleEvent` | Ebrar | ✅ `feature-ebrar` — çatışma çözülerek alındı (aşağıya bakın) |
| **B2** `creator` filtresi | Irmak | ✅ `feature/search-filtreleme-sayfalama` — çatışma çözülerek alındı (aşağıya bakın) |
| **C1a** Durum makinesi hedef bayrağı | Esra | 🟡 `feature/workflow-gonder-hedef-cozumleme` dalında yazıldı, **entegre edilmedi** — C1b bekliyor |
| **C1b** `TargetUserResolver` | Burak | 🔴 başlanmadı |
| **A1** Şifre değişimi zorlaması | Nisan · Sümeyye | 🔴 başlanmadı |
| **A3** Aday CALISAN kontrolü | Nisan · Sümeyye | 🔴 başlanmadı |
| **C2a** Kayıt CRUD audit çağrıları | Alperen · Fevzi | 🔴 başlanmadı — `feature/record` dalında yeni iş yok |
| **C3** E-posta alıcı matrisi | Melih | 🔴 başlanmadı |

### Entegrasyonda düzeltilen üç gerileme 🔴

İki dal da **eski bir kod sürümüne dayandığı** için, yeni işlerinin yanında
mevcut kodu geriye götürüyordu. Yeni işler alındı, gerilemeler alınmadı:

**`feature-ebrar` — rol çözümlemesi ham SQL'e dönüyordu.** `AuditLogService`
rolü `RoleRepository` ile çözüyor; dal bunu `JdbcTemplate` + elle yazılmış
`SELECT id FROM roles WHERE name = ?` hâline geri götürmüştü. Mevcut yapı
korundu, `recordLifecycleEvent` ona bağlandı; testleri de `RoleRepository`
fixture'ına taşındı.

**`feature/search-filtreleme-sayfalama` — Admin bütün evrakları görüyordu.**
Dalın `RecordSpecifications` sürümünde `case "ADMIN": break;` vardı, yani
Admin için hiçbir kapsam koşulu eklenmiyordu. Şartname ve `RecordAccessPolicy`
gereği **Admin evrak göremez**; `cb.disjunction()` korundu. Aynı dosyada iki
şey daha geriye gidiyordu: `withFilters` imzası `RoleName` yerine `String`
alıyordu (exhaustive switch kayboluyordu) ve Başkan kapsamı yalnızca
`status = BASKAN_INCELEMESINDE`'ye düşüyordu (kendisine atanan kayıtları
göremez hâle gelirdi). Üçü de korundu; yalnızca `creator` filtresi alınıp
mevcut dosyanın biçimine taşındı.

> **Tekrar eden desen:** bu, entegrasyonda üçüncü kez oluyor (daha önce
> `RecordServiceImpl` güvenlik kontrolleri ve `nisan-sumeyye`'nin sildiği
> test import'ları). Sebep hep aynı: dal `test`'ten güncellenmeden uzun süre
> ayrı kalıyor. **Öneri:** işe başlamadan önce `git pull --rebase origin test`.

### Test durumu

- Backend: **352 test, 3 hata** — üçü de yerel PostgreSQL isteyen entegrasyon
  testleri (`contextLoads`, `AuditLogRepositoryIntegrationTest` ve yeni gelen
  `RecordRepositorySortingTest`). Mantık hatası yok.
- Frontend: **96/96** (mock modunda). `AdminUsersPage.test.tsx` içindeki
  "rol seçmeden hesap oluşturma" testi ilk turda kırmızı, ikinci turda yeşil
  geldi — **kararsız (flaky)**. Tek başına çalıştırıldığında 5/5 geçiyor,
  yani dosyalar arası paylaşılan MSW durumundan şüpheleniliyor. Frontend
  ekibinin bakması gereken bir konu, entegrasyonu bloke etmiyor.

### `feature/record` — entegre edilecek bir şey yok

Dal, entegrasyon dalıyla **içerik olarak birebir aynı**; tek fark bu iki
doküman. Görünüşe göre `test` üstüne rebase edilmiş ama üzerine yeni iş
eklenmemiş. C2a (kayıt CRUD audit çağrıları) hâlâ yazılmadı.

---

## 3. Öncelik sırası

| Sıra | İş | Kim |
|---|---|---|
| 1 | A1, A2, A3, A4 — güvenlik, dördü de testli | Nisan · Sümeyye, Ecesu, Hacer |
| 2 | B1 — dosya listesi ucu (frontend'i açar) | Ecesu |
| 3 | C1 — workflow hedefinin backend'de çözülmesi | Esra · Burak |
| 4 | C2 — kayıt CRUD audit | Alperen · Fevzi, Ebrar |
| 5 | B2 — oluşturana göre filtre | Irmak |
| 6 | C3 — e-posta alıcı matrisi | Melih |
| 7 | Dört rolle uçtan uca doğrulama | Ekip |

İşler bittikçe `FRONTEND_BACKEND_SOZLESMESI.md`'nin §5 (kayıt detayı),
§9 (dosyalar), §10 (bildirimler) ve workflow bölümleri **aynı PR'da**
güncellenmeli — sözleşmenin koddan geride kalması bu projede iki kez yaşandı.
§9'a dosya alan adları (Karar 2) ve yeni listeleme ucu, §5'e `creator`
parametresi (Karar 3) yazılacak.

---

## 4. Doğrulama kanıtları

Belgedeki her iddia için koda bakıldı:

| Madde | Kanıt |
|---|---|
| A1 | `mustChangePassword` yalnız `User` entity'si ve `LoginResponse`'ta geçiyor; hiçbir filtre okumuyor |
| A2 | `FileController.downloadFile/previewFile` yalnız `id` alıyor; `FileService`'te hiçbir erişim politikası çağrısı yok |
| A3 | `UserService.assignBaskanYardimcisi` aktiflik kontrol ediyor, rol kontrol etmiyor |
| A4 | `JwtUtil.generateRefreshToken` yalnız `subject` + `issuedAt` + `expiration` kullanıyor |
| B1 | `RecordResponse`'ta `files` yok, `GET /api/records/{id}/files` ucu yok, upload cevabı `ResponseEntity<?>` |
| B2 | `RecordSearchCriteria` yalnız `q`, `status`, `categoryId`, `from`, `to` taşıyor |
| C1 | `TargetUserResolver` `GONDER` için istekten geleni alıyor, `BASKANA_ILET` için backend çözüyor |
| C2 | `RecordServiceImpl` içinde tek bir audit çağrısı yok |
| C3 | `WorkflowStatusChangedListener.recipientOf`, `assignedTo == null` iken yalnız `createdBy` döndürüyor |

Frontend belgesinin **D3, E ve F** bölümleri backend işi içermiyor.
