# Mobil API Envanteri

Mobil istemcinin kullandığı REST uçlarını, istek/yanıt biçimlerini ve hata
davranışlarını tanımlar. Uç değiştiğinde bu belge aynı değişiklik kapsamında
güncellenir.

4 Eylül 2026, `codex/ap-2-frontend-uyum` @ `c9b0297` tabanı ile hizalanmıştır.
`DEPARTMANA_GONDER` aksiyonu ve `targetDepartmentId` alanı backend'de mevcuttur;
mobil istemci bunları **kullanmaz**. AP-3/AP-4/AP-5/AP-8 yönetim uçları hâlâ
yoktur. Mobilin tüketeceği yeni uçlar ve ortak `assignment` nesnesi
[APP-9 / APP-10 / B11 sözleşmesinde](APP9_APP10_B11_ISTEMCI_SOZLESMESI.md)
tanımlıdır. [Güncel teslim sınırları](README.md) ayrı izlenir.

> **Açık kırılma — B09 (P1).** `mobile/src/api/users.ts` içindeki `roleName`
> yalnız `CALISAN`, `BASKAN_YARDIMCISI`, `BASKAN`, `ADMIN` değerlerini kabul eden
> bir Zod enum'uyla ayrıştırılır; `roleId` ve `systemKey` hiç kullanılmaz. Admin'in
> oluşturduğu **dinamik rol** veya **yeniden adlandırılmış yerleşik rol** için
> geçerli bir `GET /api/users/me` cevabı istemcide reddedilir. Login token
> verebilir; kırılma profil okunurken ve ona bağlı ekranlarda oluşur.
> `RecordWorkflowActions` ve dashboard da aynı sabit rol adlarına bağlıdır.
> Web AP-2 düzeltmesinin mobil karşılığı eksiktir: profil şeması, etiketler,
> dashboard, oluşturma yetkisi ve workflow aksiyon seçimi **birlikte**
> dönüştürülmelidir. Kabul: yeni dinamik rol ve yeniden adlandırılmış yerleşik
> rolle giriş sonrası liste, detay ve yetkili işlem çalışmalıdır.
> Ayrıntı: [inceleme raporu](PROJE_INCELEME_RAPORU_2026-09-04.md).
>
> **Hedef model karara bağlanmıştır** (4 Eylül 2026,
> [APP-9 / APP-10 / B11 sözleşmesi](APP9_APP10_B11_ISTEMCI_SOZLESMESI.md)):
> profil `roleId` + nullable `systemKey` + gösterim adı taşır; workflow düğmeleri
> istemcide hesaplanmaz, `GET /api/records/{id}/workflow/available-actions`
> yanıtından üretilir. `RecordWorkflowActions` içindeki istemci tarafı
> `getAvailableActions()` kaldırılır. Sözleşme **Önerildi** durumundadır; uçlar
> henüz uygulanmamıştır.

Kanonik kaynaklar: [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
(alan sözleşmesi) · [workflow.md](workflow.md) (durum geçişleri ve görünürlük) ·
Swagger `/swagger-ui.html`, ham şema `/v3/api-docs` ve inceleme amaçlı
[OpenAPI anlık görüntüsü](openapi.json). Mobil base URL'i
`EXPO_PUBLIC_API_BASE_URL` üzerinden alır; şemadaki `servers` alanını kullanmaz.

## Genel kurallar

**Kimlik.** Aşağıdaki uçlar dışında **her istek** `Authorization: Bearer <accessToken>`
ister. Açık uçlar: `POST /api/auth/login`, `/refresh`, `/logout`,
`/forgot-password`, `/verify-reset-code`, `/reset-password` ile tam yolları
`POST /api/public/mail-actions/preview` ve `/consume` olan hızlı işlem uçlarıdır.

**Kullanıcı kimliği gövdeden alınmaz.** Hiçbir uç `userId` kabul etmez; oturum
JWT'den okunur. Gövdeye kullanıcı kimliği koymak sessizce yok sayılır.

**Mobilin başlangıç rol senaryoları:** `CALISAN`, `BASKAN_YARDIMCISI`, `BASKAN`.
Backend kataloğu bunlarla sınırlı değildir; dinamik rol gerekli permission/ilişkiyle
okuyabilir ve tanımlı geçişi uygulayabilir. Bu backend desteği mobilin dinamik rol
ekran kabulünün tamamlandığı anlamına gelmez.
`ADMIN` mobil kapsamında değil — evrak göremez, `/api/admin/**` uçları mobile
dahil edilmedi.

**Hata gövdesi.** Tüm hatalar aynı `ApiError` biçimini döner:

```json
{
  "code": "FORBIDDEN",
  "message": "Bu kaydı görüntüleme yetkiniz yok",
  "status": 403,
  "timestamp": "2026-08-20T14:05:00",
  "fieldErrors": [ { "field": "title", "message": "Başlık boş bırakılamaz" } ]
}
```

`fieldErrors` yalnız `VALIDATION_ERROR` durumunda dolu gelir. Mobil kullanıcıya
`message` alanını gösterir, dallanma için `code` kullanır — HTTP status'a değil.

**Hata kodları:**

| `code` | HTTP | Ne zaman |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Gövde doğrulaması; `fieldErrors` dolu |
| `BUSINESS_RULE_VIOLATION` | 400 | İş kuralı (ör. taslak olmayan kaydı silme) |
| `INVALID_SORT_FIELD` | 400 | `?sort=` var olmayan alan |
| `BAD_REQUEST` | 400 | Bozuk/okunamayan istek |
| `INVALID_OR_EXPIRED_RESET_CODE` | 400 | Parola sıfırlama kodu yanlış/süresi dolmuş |
| `INVALID_OR_EXPIRED_RESET_TOKEN` | 400 | Sıfırlama anahtarı geçersiz |
| `PASSWORD_REUSED` | 400 | Yeni parola eskisiyle aynı |
| `UNAUTHORIZED` | 401 | Token yok/geçersiz → refresh dene |
| `INVALID_CREDENTIALS` | 401 | Giriş bilgileri hatalı |
| `PASSWORD_CHANGE_REQUIRED` | 403 | Zorunlu parola değişimi bekliyor (aşağıya bak) |
| `FORBIDDEN` | 403 | Yetki yok |
| `RESOURCE_NOT_FOUND` | 404 | Kayıt/dosya yok |
| `NOT_FOUND` | 404 | Böyle bir uç yok |
| `METHOD_NOT_ALLOWED` | 405 | Yanlış HTTP metodu |
| `CONFLICT` | 409 | Kayıt zaten mevcut |
| `VERSION_CONFLICT` | 409 | Eşzamanlı düzenleme (`PUT /api/records/{id}`) — kaydı yeniden yükle |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Desteklenmeyen içerik tipi |
| `INTERNAL_ERROR` | 500 | Beklenmeyen hata |

**Sayfalama.** Sayfalı uçlar Spring `Pageable` alır: `?page=0&size=20&sort=alan,asc`.
`page` **0'dan başlar**. Cevap zarfı:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
```

`sort` alanı entity özelliği olmalı (ör. `createdAt`, `title`, `status`); geçersiz
alan `400 INVALID_SORT_FIELD` döner. **Boş string göndermeyin** — Swagger'ın
doldurulmamış varsayılanı bu hatayı üretir.

**Tarih biçimi.** İstek ve cevapta ISO-8601 (`2026-08-20T14:05:00`). Zaman dilimi
taşınmaz; sunucu yerel saatidir.

---

## 1. Auth — `/api/auth`

| Metot | Adres | Yetki | Not |
|---|---|---|---|
| `POST` | `/login` | Açık | |
| `POST` | `/refresh` | Açık | |
| `POST` | `/logout` | Açık | Gövdedeki refresh token'ı iptal eder |
| `POST` | `/change-password` | Oturum | Zorunlu değişimde de çağrılabilir |
| `POST` | `/forgot-password` | Açık | Adres kayıtlı olmasa da `202` |
| `POST` | `/verify-reset-code` | Açık | |
| `POST` | `/reset-password` | Açık | |

**`POST /login`** → `{ "email", "password" }`

```json
{ "accessToken": "...", "refreshToken": "...", "mustChangePassword": false }
```

Cevapta **kullanıcı bilgisi yok.** Ad/rol için girişten hemen sonra
`GET /api/users/me` çağrılır.

**`POST /refresh`** → `{ "refreshToken": "..." }` → aynı `LoginResponse`.
Yenilemede yeni bir refresh token da döner; eskisi geçersizleşir (rotasyon).

**`POST /logout`** → `{ "refreshToken": "..." }` → `200`, gövde düz metin
`"Çıkış yapıldı"`. **JSON değil** — parse etmeyin.

> Opsiyonel `deviceToken` alanı **eklenmiştir** (`LogoutRequest.deviceToken`).
> Web bu alanı göndermez ve göndermemesi hata değildir. Gönderildiğinde cihaz
> token'ı aynı işlemde pasifleştirilir; çıkış yapan cihaza bildirim gitmez.
> `refreshToken` zorunlu olmaya devam eder.

**`POST /change-password`** → `{ "currentPassword", "newPassword" }` → düz metin.
`newPassword`: en az 8 karakter, en az bir harf ve bir rakam.

**Parola sıfırlama üç adımdır:**

1. `POST /forgot-password` `{ "email" }` → `202` (hesabın varlığı sızdırılmaz)
2. `POST /verify-reset-code` `{ "email", "code" }` — `code` 6 hane →
   `{ "resetToken": "...", "expiresInSeconds": 900 }`
3. `POST /reset-password` `{ "token", "newPassword" }` → `204 No Content`

### Zorunlu parola değişimi

`mustChangePassword=true` iken kullanıcı **yalnız üç ucu** çağırabilir:
`POST /api/auth/change-password`, `POST /api/auth/logout`, `GET /api/users/me`.
Diğer her istek `403 PASSWORD_CHANGE_REQUIRED` ile kesilir — kural arayüzde
değil `JwtAuthenticationFilter`'da zorlanır, mobil UI gizlemesine güvenilmez.

---

## 2. Kullanıcı — `/api/users`

| Metot | Adres | Yetki |
|---|---|---|
| `GET` | `/me` | Oturum |

```json
{
  "id": "uuid", "firstName": "Ahmet", "lastName": "Yılmaz",
  "email": "a@ornek.local", "roleName": "CALISAN",
  "active": true, "createdAt": "2026-08-01T09:00:00"
}
```

`roleName` mobilin rol bazlı ekran seçimini besler. **Nihai yetki yine
backend'de** — rol bilgisi sadece görünüm içindir.

---

## 3. Kayıtlar — `/api/records`

| Metot | Adres | Yetki | Not |
|---|---|---|---|
| `GET` | `` | Oturum | Sayfalı liste, görünürlük kapsamı uygulanır |
| `GET` | `/{id}` | Oturum | |
| `POST` | `` | `RECORD_CREATE` | `TASLAK` oluşturur |
| `PUT` | `/{id}` | `RECORD_EDIT` | Yalnız kendi `TASLAK`/`DUZENLEME_BEKLIYOR` kaydı |
| `DELETE` | `/{id}` | `RECORD_DELETE` | Yalnız kendi `TASLAK` kaydı |

**`GET /api/records`** parametreleri:

| Param | Tip | Not |
|---|---|---|
| `page`, `size`, `sort` | sayfalama | `page` 0'dan başlar |
| `status` | `RecordStatus` | Tek değer |
| `categoryId` | int | |
| `q` | string | **Yalnız başlık ve açıklamada** arar |
| `creator` | string | Oluşturanın ad/soyadında arar |
| `from`, `to` | ISO tarih | `createdAt` aralığı |

Liste öğesi:

```json
{
  "id": "uuid", "title": "...", "description": "...", "categoryId": 4,
  "status": "BSK_YRD_INCELEMESINDE",
  "createdBy": "uuid", "createdByFullName": "Ahmet Yılmaz",
  "assignedTo": "uuid", "createdAt": "...", "updatedAt": "..."
}
```

Detay cevabı: `id`, `title`, `description`, `categoryId`, `status`, `createdAt`,
`createdBy`, `createdByFullName`. **`assignedTo` ve kayıt numarası detayda yok** —
listeden taşımak gerekir.

> **`createdByFullName` kullanın.** Adı işlem geçmişindeki `RECORD_CREATED`
> satırından türetmeyin: Başkan o satırı hiç görmez (§5), geri düşülen ilk satır
> ona başka birini gösterir. Kullanıcı silinmişse alan boş gelir, kimliğe düşün.

**Oluşturma / güncelleme gövdesi** (ikisi de aynı):

```json
{ "title": "...", "description": "...", "categoryId": 4 }
```

Üçü de zorunlu. `status` gövdeden **alınmaz**; durum yalnız workflow ucuyla değişir.
Oluşturma `201 Created` döner, güncelleme `200`.

**Durumlar:** `TASLAK` · `BSK_YRD_INCELEMESINDE` · `BASKAN_INCELEMESINDE` ·
`DUZENLEME_BEKLIYOR` · `ONAYLANDI` · `REDDEDILDI`.
Son ikisi terminaldir; içerik, dosya ve durum artık değişmez.

### Görünürlük kapsamı

Liste ve detay aynı kuralı uygular — mobil ayrıca filtreleme yapmaz:

| Rol | Görür |
|---|---|
| Dinamik rol / `CALISAN` | Kendi oluşturduğu, doğrudan kendisine atanan veya yetkili departman/durum kapsamındaki kayıtlar |
| `BASKAN_YARDIMCISI` | Kendi oluşturduğu + kendisine atanan + `DUZENLEME_BEKLIYOR` + bir kez kendi elinden geçmiş (`last_deputy_id`) |
| `BASKAN` | Kendi oluşturduğu + kendisine atanan + onayına gelen + sonuçlanan (`ONAYLANDI`/`REDDEDILDI`) |
| `ADMIN` | Hiçbir kayıt |

Kapsam dışı kayıt listede **hiç dönmez**, sayfa sayısına da girmez. Kimliğiyle
doğrudan istenirse `403 FORBIDDEN`.

> **Tablo kapalı bir liste değildir.** Dört yerleşik rolün bugünkü kapsamını
> anlatır; rol kataloğu `roles` tablosundan gelir. Panelden rol oluşturma AP-2'de açıktır.
> Rol adı (`roles.name`) değiştirilebilir — istemci rolü ada göre sabit bir listeye
> karşı doğrulamamalıdır. Bütün kapsamlar aktif hesap/rol ve `RECORD_VIEW` gerektirir; ADMIN deny korunur. Dinamik rol erişimi uygulanmıştır; uygun routing/rol/permission ile departman görünürlüğü de uygulanmıştır. [WF-2C2 sözleşmesi](WF2C2_DB8_GORUNURLUK_SOZLESMESI.md). Silinmiş kaydın detay/dosya/geçmiş okumaları `404` döner.

**İçerik dondurma:** Kayıt `DUZENLEME_BEKLIYOR` iken onu geri gönderen Bşk. Yrd.
**devir anındaki kopyayı** görür — başlık, açıklama, kategori ve ek dosyalar
dahil. Çalışanın o sırada yaptığı düzenlemeler ona yansımaz. Kayıt
`TEKRAR_GONDER` ile döndüğünde güncel içerik açılır.

---

## 4. İş akışı — `POST /api/records/{recordId}/workflow/actions`

Yetki `@PreAuthorize` ile değil, durum makinesiyle belirlenir.

```json
{ "action": "BASKANA_ILET", "comment": "Uygun bulunmuştur." }
```

**`targetUserId` gönderilmez.** Gönderilirse `400 WORKFLOW_TARGET_NOT_ALLOWED` döner; kişi hedefini sunucu çözer.

**V23 + WF-5/WF-6:** `DEPARTMANA_GONDER` ve Integer `targetDepartmentId` desteklenir. İki hedef alanı birlikte `400 VALIDATION_ERROR`; eksik hedef `400 WORKFLOW_TARGET_REQUIRED`, yanlış alan `400 WORKFLOW_TARGET_NOT_ALLOWED` üretir.

```json
{ "action": "DEPARTMANA_GONDER", "targetDepartmentId": 12, "comment": "Satın alma incelemesi" }
```

Departmana gönderim `assigned_department_id` alanını doldurur ve `assigned_to` alanını temizler. Hedef aktif olmalı; iniş durumu için aktif routing/transition, aktif workflow rolü, uygun aktif üye, `RECORD_VIEW` ve geçiş permission'ı bulunmalıdır. Eksik/pasif departman `400 WORKFLOW_DEPARTMENT_INVALID`, kullanılabilir iniş routing'i yoksa `409 WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED` döner. Kayıt zaten departmandayken eksik routing veya yetkisiz üyelik `403 WORKFLOW_FORBIDDEN` üretir. Üyelik tek başına yetki vermez.

Cevap:

```json
{
  "recordId": "uuid", "action": "BASKANA_ILET",
  "previousStatus": "BSK_YRD_INCELEMESINDE", "newStatus": "BASKAN_INCELEMESINDE",
  "assignedTo": "uuid", "performedBy": "uuid",
  "performedAt": "2026-08-20T14:05:00Z"
}
```

`performedAt` burada **`Instant`** (UTC, `Z` ekli) — diğer uçlardaki yerel
`LocalDateTime`'dan farklı. Parse ederken dikkat.

### İzinli geçişler

| Durum | Rol | Aksiyon | Yeni durum | `comment` |
|---|---|---|---|---|
| `TASLAK` | `CALISAN` (sahibi) | `DEPARTMANA_GONDER` | `BSK_YRD_INCELEMESINDE` | opsiyonel |
| `DUZENLEME_BEKLIYOR` | `CALISAN` (sahibi ve atama sahibi) | `DEPARTMANA_GONDER` | `BSK_YRD_INCELEMESINDE` | opsiyonel |
| `TASLAK` | `CALISAN` (sahibi) | `GONDER` | `BSK_YRD_INCELEMESINDE` | opsiyonel |
| `DUZENLEME_BEKLIYOR` | `CALISAN` (sahibi) | `TEKRAR_GONDER` | `BSK_YRD_INCELEMESINDE` | opsiyonel |
| `BSK_YRD_INCELEMESINDE` | `BASKAN_YARDIMCISI` (atanan) | `BASKANA_ILET` | `BASKAN_INCELEMESINDE` | opsiyonel |
| `BSK_YRD_INCELEMESINDE` | `BASKAN_YARDIMCISI` (atanan) | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | **zorunlu** |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `ONAYLA` | `ONAYLANDI` | opsiyonel |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `REDDET` | `REDDEDILDI` | **zorunlu** |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | **zorunlu** |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `BASKAN_YARDIMCISINA_GERI_GONDER` | `BSK_YRD_INCELEMESINDE` | **zorunlu** |

`comment` en fazla 2000 karakter. Zorunlu olduğu yerde boş gönderilirse
`400`. Tablo başlangıç seed'ini gösterir; WF-8 aynı geçişe dinamik aktör rolü
bağlayabilir. Geçerli durum–aksiyon–rol birleşimini aktif DB transition'ları,
permission ve kayıt ilişkisi belirler; mobil buton gizlese bile backend doğrular.

### ⚠️ Bu ucun kendi hata kodları var

Workflow ucu genel kod ailesini **kullanmaz**; `ApiError.code` alanında
`WORKFLOW_*` kodları döner. Mobil bu ucu ayrı ele almalı:

| `code` | HTTP | Anlamı |
|---|---|---|
| `WORKFLOW_INVALID_TRANSITION` | 400 | Bu durumda bu işlem yapılamaz |
| `WORKFLOW_COMMENT_REQUIRED` | 400 | Açıklama zorunlu (boşluk kabul edilmez) |
| `WORKFLOW_TARGET_REQUIRED` | 400 | Hedef departman gerekli |
| `WORKFLOW_DEPARTMENT_INVALID` | 400 | Hedef departman yok/pasif |
| `WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED` | 409 | İniş durumunda kullanılabilir routing yok |
| `WORKFLOW_TARGET_NOT_ALLOWED` | 400 | Hedef bu işlem için uygun değil |
| `WORKFLOW_TARGET_ROLE_INVALID` | 400 | Hedefin rolü uygun değil |
| `WORKFLOW_TARGET_INACTIVE` | 400 | Hedef kullanıcı pasif |
| `WORKFLOW_FORBIDDEN` | 403 | Bu kayıt üzerinde işlem yetkisi yok |
| `WORKFLOW_ROLE_NOT_ALLOWED` | 403 | Rolünüz bu işlemi yapamaz |
| `WORKFLOW_RECORD_LOCKED` | 409 | Kayıt kilitli |
| `WORKFLOW_ROLE_NOT_CONFIGURED` | 409 | İşlemi devralacak yetkili belirlenemedi |
| `WORKFLOW_VERSION_CONFLICT` | 409 | Kayıt siz işlem yaparken değişti |
| `WORKFLOW_STATUS_NOT_CONFIGURED` | 500 | Sunucu yapılandırma hatası |

Hedefi backend çözse de eksik/pasif/uygunsuz hedef veya geçiş metadata'sı
`WORKFLOW_TARGET_*` hatalarını üretebilir. İstemci bunları yok saymamalıdır.

`WORKFLOW_VERSION_CONFLICT` eşzamanlı değişikliği, `WORKFLOW_RECORD_LOCKED`
terminal/kilitli kaydı, `WORKFLOW_ROLE_NOT_CONFIGURED` hedef rol yapılandırmasını
gösterir; her 409 geçici yarış değildir. Mobil kaydı yeniden yükleyip güncel
durumu ve hata mesajını göstermeli, isteği sessizce tekrarlamamalıdır.

---

## 5. İşlem geçmişi — `GET /api/audit-logs/record/{recordId}`

Yetki: kaydı görebilen herkes. Sayfalama **yok**, tüm satırlar tek listede
`createdAt` artan sırada döner.

```json
[{
  "id": "uuid", "recordId": "uuid",
  "userId": "uuid", "userFullName": "Ayşe Kaya",
  "roleId": 2, "roleName": "BASKAN_YARDIMCISI",
  "action": "BASKANA_ILET",
  "previousStatus": "BSK_YRD_INCELEMESINDE", "newStatus": "BASKAN_INCELEMESINDE",
  "comment": "Uygun bulunmuştur.",
  "httpMethod": null, "requestPath": null, "httpStatus": null, "errorCode": null,
  "createdAt": "2026-08-20T14:05:00"
}]
```

`httpMethod` / `requestPath` / `httpStatus` / `errorCode` kayıt geçmişinde
**her zaman `null`** — o alanlar Admin HTTP denetim satırları içindir. Mobil
modelinde opsiyonel işaretlensin.

`action` değerleri: geçiş aksiyonları (`GONDER`, `TEKRAR_GONDER`, `BASKANA_ILET`,
`CALISANA_GERI_GONDER`, `BASKAN_YARDIMCISINA_GERI_GONDER`, `ONAYLA`, `REDDET`) ve
yaşam döngüsü olayları (`RECORD_CREATED`, `RECORD_UPDATED`, `RECORD_DELETED`).
Yaşam döngüsü satırlarında `previousStatus` `null`'dur — geçiş değildirler.

### ⚠️ Geçmiş role göre kırpılır

Aynı kaydın geçmişi herkese aynı gelmez. Önce ortak kayıt görünürlüğü doğrulanır;
ardından sistem rolüne özgü geçmiş kesimi uygulanır. Ek `AUDIT_VIEW` gerekmez.

| Rol | Ne görür |
|---|---|
| Dinamik rol / Çalışan | Görünür kaydın tam geçmişi |
| Bşk. Yrd. | Kayıt `DUZENLEME_BEKLIYOR` iken **devir anına kadar** kırpılmış |
| Başkan | Evrak kendisine **ilk iletildiği andan itibaren** |

Kırpma sunucuda yapılır; gizlenen satırlar cevaba hiç konmaz. **Mobil bunu eksik
veri sanıp yeniden istemesin.** Başkan ekranında listenin `RECORD_CREATED` ile
başlamaması normaldir.


### Sayfalama kararı — gerekçeli

Projede önceden tanımlanmış sayısal bir cevap boyutu sınırı yoktur
(sözleşmedeki boyut sınırları yalnızca dosya yüklemeleri içindir).

**Karar: sayfalama eklenmeyecek.** Ölçüm aşağıda; gerçekçi en kötü durumda
bile cevap mobil için önemsiz kalıyor.

#### Satır başına boyut (ölçülen, UTF-8 bayt)

Jackson varsayılan ayarlarıyla (null alanlar dahil, boşluksuz), gerçek enum
değerleri ve gerçek yaşam döngüsü yorumlarıyla:

| Satır türü | Bayt |
|---|---|
| Yorumsuz geçiş (`GONDER`, `BASKANA_ILET`, …) | 403 |
| Yaşam döngüsü (`RECORD_CREATED` / `UPDATED` / `DELETED`) | 408 |
| Tipik yorumlu geri gönderme (~50 karakter açıklama) | 476 |
| **En kötü:** 2000 karakterlik yorum + en uzun enum'lar | 2474 |

Yorum uzunluğu üst sınırı `WorkflowActionRequest`'teki `@Size(max = 2000)`
ile zorlanır. Yaşam döngüsü satırlarının yorumu sabit metindir
("Kayıt oluşturuldu.", "Başlık ve kategori güncellendi.",
"Kayıt soft delete işlemiyle silindi."), kullanıcı girdisi değildir.

#### Gerçekçi akış (oluşturma + 2 düzenleme + gönder/ilet + N geri gönderme turu + onay)

| Geri gönderme turu | Satır | Cevap |
|---|---|---|
| 0 | 6 | 2,4 KB |
| 1 | 11 | 4,5 KB |
| 3 | 21 | 8,7 KB |
| 5 | 31 | 12,9 KB |
| 10 | 56 | 23,4 KB |
| 20 | 106 | **44,5 KB** |

Yirmi tur geri gönderme zaten uçuk bir senaryo ve cevap hâlâ 45 KB. Mobil
tarafta tek istekte inmesi sorun değil.

1 MB'a ulaşmak için gereken satır sayısı: tipik satırla **~2200**, her satırda
azami uzunlukta yorum varsa **~423**.

#### Sınırın davranışsal olduğu — kodda zorlanmıyor

Boyut iki bağımsız kaynaktan sınırsız büyüyebilir; ikisini de kısıtlayan bir iş
kuralı **yok**:

1. `BSK_YRD_INCELEMESINDE ↔ DUZENLEME_BEKLIYOR` ve
   `BASKAN_INCELEMESINDE ↔ DUZENLEME_BEKLIYOR` döngüleri sınırsız tekrarlanabilir.
2. **Her `PUT /api/records/{id}` çağrısı ayrı bir `RECORD_UPDATED` satırı yazar.**
   Taslağını çok kez kaydeden bir Çalışan, hiç geri gönderme olmadan da satır
   biriktirir. Bu kaynak döngü sayısından bağımsızdır.

Kararı değiştirecek eşik: tek bir kaydın geçmişi **birkaç yüz satırı** düzenli
olarak aşmaya başlarsa. O noktada tercih sırası — önce geri gönderme sayısına iş
kuralı düzeyinde üst sınır, o kabul edilmezse bu uca `limit` parametresi.
Sayfalama son çare: web istemcisi ucu sayfalamasız kullanıyor ve kırpma kuralı
(yukarıda) sayfalı bir uçta doğru uygulanmak zorunda kalır, bu da kuralı iki
yerde tutmayı gerektirir.

> Ölçüm çalışan bir ortamda gerçek verilerle değil, gerçek DTO alanları, enum
> değerleri ve doğrulama sınırları kullanılarak JSON serileştirmesiyle yapıldı.
> Satır sayısı ile boyut arasındaki ilişki doğrusal olduğu için gerçek veriyle
> tekrarlandığında sonucun değişmesi beklenmez.

---

## 6. Dosyalar

| Metot | Adres | Yetki | Not |
|---|---|---|---|
| `POST` | `/api/records/{id}/files` | `FILE_MANAGE` + sahiplik/kilit kontrolü | multipart, aynı `file` alanında bir veya daha çok dosya |
| `GET` | `/api/records/{id}/files` | Kaydı görebilen | |
| `GET` | `/api/files/{id}/download` | Kaydı görebilen | |
| `GET` | `/api/files/{id}/preview` | Kaydı görebilen | Inline |
| `DELETE` | `/api/files/{id}` | `FILE_MANAGE` + sahiplik/kilit kontrolü | Soft delete |

**Yükleme `multipart/form-data`, alan adı `file`.** Backend `MultipartFile[]`
kabul eder. Mobil, dosya bazında ilerleme, hata ve yeniden deneme gösterebilmek
için seçilen dosyaları bilinçli olarak sırayla ve her istekte tek dosya gönderir.

```json
{
  "id": "uuid", "recordId": "uuid",
  "originalName": "teklif.pdf", "mimeType": "application/pdf",
  "fileSize": 182342, "uploadedBy": "uuid", "uploadedAt": "..."
}
```

**İzinli türler** (içerik doğrulaması dosya adına değil **magic byte'lara**
bakar — uzantıyı değiştirerek geçilemez):

`application/pdf` · `application/msword` (.doc) · `.docx` · `application/vnd.ms-excel` (.xls) ·
`.xlsx` · `image/png` · `image/jpeg`

**Boyut sınırı:** dosya başına 10 MB, istek başına 10 MB
(`spring.servlet.multipart.*`). Aşılırsa Spring `MaxUploadSizeExceededException`
üretir.

Kayıt `DUZENLEME_BEKLIYOR` iken geri gönderen Bşk. Yrd. **devir anındaki dosya
listesini** görür: sonradan yüklenen görünmez, sonradan silinen hâlâ görünür.
Gizlenen bir dosyaya kimliğiyle doğrudan erişim `404` döner.

---

## 7. Bildirimler — `/api/notifications`

| Metot | Adres | Cevap |
|---|---|---|
| `GET` | `` | `PagedResponse<NotificationResponse>` — okunmuş + okunmamış, en yeniden eskiye |
| `GET` | `/unread` | `List<NotificationResponse>` — sayfalama yok |
| `GET` | `/unread/count` | `long` (düz sayı) |
| `PUT` | `/{id}/read` | `204 No Content` |

```json
{
  "id": "uuid", "recordId": "uuid",
  "message": "...", "notificationType": "RECORD_FORWARDED",
  "read": false, "createdAt": "..."
}
```

Alan adı **`read`**, `isRead` değil. `notificationType` değerleri:
`RECORD_SUBMITTED` · `RECORD_FORWARDED` · `RECORD_APPROVED` · `RECORD_REJECTED` ·
`RECORD_RETURNED`.

`GET /api/notifications`'ta **`sort` gönderilmez** — sıra sunucuda sabittir.
Toplu "hepsini okundu yap" ucu **yoktur**; okuma tekildir.

**Alıcı kuralı:** atama yapılan geçişte atanan kullanıcıya; nihai onay/ret
geçişinde hem kaydı oluşturana hem kaydı Başkana ileten yardımcıya gider.

---

## 8. Kategoriler — `GET /api/categories`

Oturum ister. Form ve filtrelerdeki tek kategori kaynağı.
Kategori adları **mobilde sabit enum olarak tutulmaz**; `categoryId` → ad eşlemesi
bu uçtan gelir.

---

## 9. Cihaz tokenları — `/api/device-tokens`

Mobil push bildirimi için FCM token kaydı. Oturum ister; kullanıcı **JWT'den**
okunur, gövdede `userId` kabul edilmez.

| Metot | Adres | Gövde | Yanıt |
|---|---|---|---|
| `POST` | `/api/device-tokens` | `{ "token", "platform": "ANDROID"\|"IOS", "deviceName"? }` | `200`, gövdesiz |
| `DELETE` | `/api/device-tokens` | `{ "token" }` | `204`, gövdesiz |

**Upsert:** `token` kolonu UNIQUE'tir. Aynı token yeniden gönderilirse satır
güncellenir — `user_id`, `platform`, `device_name`, `is_active` ve `updated_at`
birlikte. FCM token'ı cihaz + uygulama başına tekildir, kullanıcı başına değil.

**`DELETE` normal çıkış akışı değildir.** Yalnız token yenilenmesi ve cihazı
elle kaldırma içindir. Normal çıkışta token `POST /api/auth/logout` gövdesindeki
opsiyonel `deviceToken` alanı ile pasifleşir. Mobil, çıkışta ayrıca
`DELETE /api/device-tokens` çağırmaz.

`DELETE`, `(token, user_id)` sahipliğini doğrular; başka kullanıcıya ait tokenı
sessizce değiştirmez. `PushNotificationService`, workflow listener'ına bağlıdır
ve geçiş sonrasında mevcut alıcı matrisi için push göndermeyi dener. FCM
yapılandırılmamış ortamda workflow push olmadan çalışmaya devam eder.

Mobil istemci `expo-notifications` ile native cihaz tokenını alıp bu uca kaydeder.
Eksikler token yenileme dinleyicisi, soğuk açılış yönlendirmesi ve gerçek cihaz
uçtan uca push kanıtıdır.

---

## 10. E-posta hızlı işlem — `/api/public/mail-actions`

Bu uçlar mobil v1 tarafından çağrılmaz; aynı backend sözleşmesinin oturumsuz web
akışıdır. E-postadaki `/hizli-islem#token=...` sayfası anahtarı URL fragment'ından
okur, adres çubuğundan temizler ve yalnız JSON gövdesinde taşır.

| Metot | Adres | Gövde | Davranış |
|---|---|---|---|
| `POST` | `/api/public/mail-actions/preview` | `{ "token": "..." }` | Anahtarı doğrular; kayıt, aksiyon, alıcı ve son kullanma bilgisini döner; durum değiştirmez |
| `POST` | `/api/public/mail-actions/consume` | `{ "token": "..." }` | Kullanıcı onayından sonra anahtarı tek kez tüketir ve `{ "recordId": "uuid" }` döner |

Anahtar süreli, tek kullanımlık ve kayıt/aksiyon/alıcıya bağlıdır. Preview çağrısı
mutasyon yapmaz; consume aynı anahtarla ikinci kez çalışmaz.

---

## Mobil kapsam dışı uçlar

`/api/admin/**` (kullanıcı ve rol yönetimi) ve `/api/user-audit-logs/**` yalnız
`ADMIN` içindir; mobil v1'de `ADMIN` rolü yok, bu uçlar bağlanmaz.
