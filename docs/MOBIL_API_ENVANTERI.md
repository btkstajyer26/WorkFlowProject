# Mobil API Envanteri

**Durum:** Uç envanteri tamam; TEST ortamı bekleniyor (M0 · M9)
**Son kod doğrulaması:** 20 Ağustos 2026, `test` dalı `0e4043a`

Mobil istemcinin kullanacağı uçların tam listesi. Her satır koda bakılarak
doğrulandı; **tahminle bağlama yok.** Uç değişirse bu belge aynı PR'da güncellenir.

Kanonik kaynaklar: [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
(alan sözleşmesi) · [workflow.md](workflow.md) (durum geçişleri ve görünürlük) ·
Swagger `/swagger-ui.html`, ham şema `/v3/api-docs`.

---

## ⚠️ Sprint 0'da kapatılması gereken boşluk

**1. TEST ortamı yok — sorumlu: Burak Kaya (M9).** Bugün yalnız
`localhost:8080` var. Gerçek cihaz `localhost`'a bağlanamaz, dolayısıyla **push
bildirimi hiç test edilemez** (Sprint 4 buna bağlı). Aşağıdaki tabloyu ortam
ayağa kalkınca Burak doldurur:

| | Adres | Örnek hesaplar | Veri |
|---|---|---|---|
| `DEV` | `http://localhost:8080` | Yerel bootstrap admin | Boş şema + Flyway |
| `TEST` | _(belirlenmedi)_ | _(belirlenmedi)_ | _(belirlenmedi)_ |
| `PROD` | _(kapsam dışı)_ | — | — |

**2. ~~Sürüm sabitlenmiş `openapi.json` yok.~~ ✅ Sabitlendi:
[docs/openapi.json](openapi.json)** — 20 Ağustos 2026, `0e4043a`.
27 uç, 31 şema. Mobil istemci kodu bundan üretilir.

Yeniden üretmek için (backend ayaktayken):

```bash
curl -s http://localhost:8080/v3/api-docs   | python -c "import sys,json;print(json.dumps(json.load(sys.stdin),ensure_ascii=False,indent=2))"   > docs/openapi.json
```

Biçimlendirme bilerek yapılıyor: springdoc tek satır JSON üretiyor ve o hâlde
her değişiklik tek satırlık okunamaz bir diff'e dönüşüyor.

> Dosyadaki `servers[0].url` `http://localhost:8080` — üretildiği ortamın
> adresidir. Mobil base URL'i kendi yapılandırmasından alır, bu alanı kullanmaz.

---

## Genel kurallar

**Kimlik.** Aşağıdaki uçlar dışında **her istek** `Authorization: Bearer <accessToken>`
ister. Açık uçlar: `POST /api/auth/login`, `/refresh`, `/logout`,
`/forgot-password`, `/verify-reset-code`, `/reset-password`.

**Kullanıcı kimliği gövdeden alınmaz.** Hiçbir uç `userId` kabul etmez; oturum
JWT'den okunur. Gövdeye kullanıcı kimliği koymak sessizce yok sayılır.

**Roller (mobil v1).** `CALISAN`, `BASKAN_YARDIMCISI`, `BASKAN`.
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

> M4 kapsamında bu gövdeye opsiyonel `deviceToken` eklenecek. Bugün DTO yalnız
> `refreshToken` taşıyor ve alan zorunlu.

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
| `POST` | `` | `CALISAN` | `TASLAK` oluşturur |
| `PUT` | `/{id}` | `CALISAN` | Yalnız kendi `TASLAK`/`DUZENLEME_BEKLIYOR` kaydı |
| `DELETE` | `/{id}` | `CALISAN` | Yalnız `TASLAK` |

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
| `CALISAN` | Yalnız kendi oluşturduğu kayıtlar |
| `BASKAN_YARDIMCISI` | Kendisine atanan + `DUZENLEME_BEKLIYOR` + bir kez kendi elinden geçmiş (`last_deputy_id`) |
| `BASKAN` | Onayına gelen + sonuçlanan (`ONAYLANDI`/`REDDEDILDI`) |

Kapsam dışı kayıt listede **hiç dönmez**, sayfa sayısına da girmez. Kimliğiyle
doğrudan istenirse `403 FORBIDDEN`.

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

**`targetUserId` gönderilmez.** Alan DTO'da duruyor ama backend bilerek yok
sayıyor; hedefi her aksiyon için sunucu çözer.

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
| `TASLAK` | `CALISAN` (sahibi) | `GONDER` | `BSK_YRD_INCELEMESINDE` | opsiyonel |
| `DUZENLEME_BEKLIYOR` | `CALISAN` (sahibi) | `TEKRAR_GONDER` | `BSK_YRD_INCELEMESINDE` | opsiyonel |
| `BSK_YRD_INCELEMESINDE` | `BASKAN_YARDIMCISI` (atanan) | `BASKANA_ILET` | `BASKAN_INCELEMESINDE` | opsiyonel |
| `BSK_YRD_INCELEMESINDE` | `BASKAN_YARDIMCISI` (atanan) | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | **zorunlu** |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `ONAYLA` | `ONAYLANDI` | opsiyonel |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `REDDET` | `REDDEDILDI` | **zorunlu** |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | **zorunlu** |
| `BASKAN_INCELEMESINDE` | `BASKAN` (atanan) | `BASKAN_YARDIMCISINA_GERI_GONDER` | `BSK_YRD_INCELEMESINDE` | **zorunlu** |

`comment` en fazla 2000 karakter. Zorunlu olduğu yerde boş gönderilirse
`400`. Tablodaki dışında her kombinasyon reddedilir — mobil buton gizlese bile
backend ayrıca doğrular.

### ⚠️ Bu ucun kendi hata kodları var

Workflow ucu genel kod ailesini **kullanmaz**; `ApiError.code` alanında
`WORKFLOW_*` kodları döner. Mobil bu ucu ayrı ele almalı:

| `code` | HTTP | Anlamı |
|---|---|---|
| `WORKFLOW_INVALID_TRANSITION` | 400 | Bu durumda bu işlem yapılamaz |
| `WORKFLOW_COMMENT_REQUIRED` | 400 | Açıklama zorunlu (boşluk kabul edilmez) |
| `WORKFLOW_TARGET_REQUIRED` | 400 | Hedef kullanıcı gerekli |
| `WORKFLOW_TARGET_NOT_ALLOWED` | 400 | Hedef bu işlem için uygun değil |
| `WORKFLOW_TARGET_ROLE_INVALID` | 400 | Hedefin rolü uygun değil |
| `WORKFLOW_TARGET_INACTIVE` | 400 | Hedef kullanıcı pasif |
| `WORKFLOW_FORBIDDEN` | 403 | Bu kayıt üzerinde işlem yetkisi yok |
| `WORKFLOW_ROLE_NOT_ALLOWED` | 403 | Rolünüz bu işlemi yapamaz |
| `WORKFLOW_RECORD_LOCKED` | 409 | Kayıt kilitli |
| `WORKFLOW_ROLE_NOT_CONFIGURED` | 409 | İşlemi devralacak yetkili belirlenemedi |
| `WORKFLOW_VERSION_CONFLICT` | 409 | Kayıt siz işlem yaparken değişti |
| `WORKFLOW_STATUS_NOT_CONFIGURED` | 500 | Sunucu yapılandırma hatası |

`WORKFLOW_TARGET_*` kodları bugün pratikte oluşmaz (hedefi backend çözüyor),
ama sözleşmede duruyorlar.

**409'lar kural ihlali değil, geçici çatışmadır.** `WORKFLOW_VERSION_CONFLICT`
ve `WORKFLOW_RECORD_LOCKED` alındığında mobil kaydı yeniden yükleyip kullanıcıya
güncel durumu göstermeli; isteği sessizce tekrarlamamalı. `message` alanı
kullanıcıya gösterilebilecek Türkçe metin taşır.

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

Aynı kaydın geçmişi herkese aynı gelmez. Kural: **kullanıcı evrağı yalnız kendi
masasında olduğu dönem boyunca görür.**

| Rol | Ne görür |
|---|---|
| Çalışan (sahibi) | Tamamı |
| Bşk. Yrd. | Kayıt `DUZENLEME_BEKLIYOR` iken **devir anına kadar** kırpılmış |
| Başkan | Evrak kendisine **ilk iletildiği andan itibaren** |

Kırpma sunucuda yapılır; gizlenen satırlar cevaba hiç konmaz. **Mobil bunu eksik
veri sanıp yeniden istemesin.** Başkan ekranında listenin `RECORD_CREATED` ile
başlamaması normaldir.


### Sayfalama kararı — gerekçeli

Projede önceden tanımlanmış sayısal bir cevap boyutu sınırı bulunamadı
(sözleşmedeki boyut sınırları yalnızca dosya yüklemeleri içindir).

Tek satır ortalama ~535 byte. Gerçekçi bir kullanım senaryosunda (birkaç
geri-gönderme döngüsü, ~20-50 satır) cevap ~10-26 KB — mobil için önemsiz.

**Ancak durum makinesinde `BSK_YRD_INCELEMESINDE ↔ DUZENLEME_BEKLIYOR` ve
`BASKAN_INCELEMESINDE ↔ DUZENLEME_BEKLIYOR` arasındaki geçiş sayısını
sınırlayan hiçbir iş kuralı yok.** Teorik olarak 1000 satır ~522 KB'a,
5000 satır ~2.5 MB'a ulaşabilir — bu, mobil için ciddi bir sorun olurdu.

**Karar: Şimdilik sayfalama eklenmeyecek** (web istemcisi sayfalamasız
kullanıyor, gerçek kullanımda yüzlerce geri-gönderme beklenmiyor), **ama bu
bir risk alanı olarak işaretleniyor.** Öneri: ya bir evrağın geri-gönderme
sayısına iş kuralı düzeyinde bir üst sınır getirilmeli, ya da bu uca
ileride sayfalama eklenmeli. Karar `record`/`workflow` ekipleriyle
görüşülmeli.

*(Not: Bu ölçüm, çalışan bir ortamda gerçek verilerle değil, örnek bir
JSON satırının hesaplanmasıyla yapıldı — TEST ortamı ayağa kalkınca
gerçek verilerle doğrulanmalı, bkz. M0/M9.)*
---

## 6. Dosyalar

| Metot | Adres | Yetki | Not |
|---|---|---|---|
| `POST` | `/api/records/{id}/files` | `CALISAN` | multipart, **tek dosya** |
| `GET` | `/api/records/{id}/files` | Kaydı görebilen | |
| `GET` | `/api/files/{id}/download` | Kaydı görebilen | |
| `GET` | `/api/files/{id}/preview` | Kaydı görebilen | Inline |
| `DELETE` | `/api/files/{id}` | `CALISAN` | Soft delete |

**Yükleme `multipart/form-data`, alan adı `file`.** Uç **tek dosya** alır;
çoklu seçimde mobil sırayla yükler (bkz. görev M6 — çoklu upload açık iş).

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

## Mobil kapsam dışı uçlar

`/api/admin/**` (kullanıcı ve rol yönetimi) ve `/api/user-audit-logs/**` yalnız
`ADMIN` içindir; mobil v1'de `ADMIN` rolü yok, bu uçlar bağlanmaz.

`/api/device-tokens` **henüz yok** — M2 ile gelecek, geldiğinde bu belgeye eklenir.
