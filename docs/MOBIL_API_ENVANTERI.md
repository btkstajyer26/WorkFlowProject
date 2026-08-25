# Mobil API Envanteri

**Durum:** Uç envanteri tamam; TEST ortamı kuruldu ve gerçek cihazdan doğrulandı (M0 · M9 ✅)
**Son kod doğrulaması:** 20 Ağustos 2026, `test` dalı `0e4043a`
**TEST ortamı doğrulaması:** 21 Ağustos 2026, `test` dalı `4726d69` — [TEST ortamı](#test-ortamı)

Mobil istemcinin kullanacağı uçların tam listesi. Her satır koda bakılarak
doğrulandı; **tahminle bağlama yok.** Uç değişirse bu belge aynı PR'da güncellenir.

Kanonik kaynaklar: [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
(alan sözleşmesi) · [workflow.md](workflow.md) (durum geçişleri ve görünürlük) ·
Swagger `/swagger-ui.html`, ham şema `/v3/api-docs`.

---

## ✅ Sprint 0 boşlukları — kapandı

**1. ~~TEST ortamı yok.~~ ✅ Kuruldu — Burak Kaya (M9).** Ekipçe erişilebilen
HTTPS TEST ortamı 21 Ağustos 2026'da ayağa kaldırıldı ve gerçek fiziksel
Android cihazdan doğrulandı. Mobil istemci artık `localhost` yerine bu adrese
bağlanabilir; push bildiriminin (Sprint 4) önündeki ortam engeli kalktı.

| | Adres | Örnek hesaplar | Veri |
|---|---|---|---|
| `DEV` | `http://localhost:8080` | Yerel bootstrap admin | Boş şema + Flyway |
| `TEST` | `https://workflowproject-test.duckdns.org` | Çalışan ×2, Bşk. Yrd., Başkan, Admin — [aşağıda](#test-ortamı) | 7 kayıt, altı workflow durumu |
| `PROD` | _(kapsam dışı)_ | — | — |

Ayrıntı, hesaplar ve kabul kanıtı: [TEST ortamı](#test-ortamı).
Kurulum, topoloji ve bilinen sınırlamalar: [TEST_ORTAMI_NOTU.md](TEST_ORTAMI_NOTU.md).

**2. ~~Sürüm sabitlenmiş `openapi.json` yok.~~ ✅ Sabitlendi:
[docs/openapi.json](openapi.json)** — 20 Ağustos 2026, `0e4043a`.
27 uç, 31 şema. Mobil istemci kodu bundan üretilir.

> [!WARNING]
> **Dosya bayat.** Sabitlendiği tarihten sonra eklenen `/api/device-tokens`
> uçları (M2) dosyada yok. Cihaz token kaydı üretilmiş istemciyle yapılamaz;
> aşağıdaki komutla yeniden üretilmesi gerekiyor.

Yeniden üretmek için (backend ayaktayken):

```bash
curl -s http://localhost:8080/v3/api-docs   | python -c "import sys,json;print(json.dumps(json.load(sys.stdin),ensure_ascii=False,indent=2))"   > docs/openapi.json
```

Biçimlendirme bilerek yapılıyor: springdoc tek satır JSON üretiyor ve o hâlde
her değişiklik tek satırlık okunamaz bir diff'e dönüşüyor.

> Dosyadaki `servers[0].url` `http://localhost:8080` — üretildiği ortamın
> adresidir. Mobil base URL'i kendi yapılandırmasından alır, bu alanı kullanmaz.

---

## TEST ortamı

**Base URL:** `https://workflowproject-test.duckdns.org`
**Deploy SHA:** `4726d6974ae30f54120a7423d288acf18465da8c` (`4726d69`, `test` dalı)
**Doğrulama tarihi:** 21 Ağustos 2026

Mobil istemci base URL'i `EXPO_PUBLIC_API_BASE_URL` üzerinden alır; değişken
zorunludur ve tanımsızsa uygulama açılışta durur
([mobile/src/api/client.ts](../mobile/src/api/client.ts)). EAS build
environment'ına tam adıyla verilmelidir — `eas.json` bu değeri kendiliğinden
sağlamıyor.

### Hesaplar

Parolalar **bu belgeye yazılmaz**; güvenli ekip kanalından paylaşılır. Admin
parolası ekip geneline paylaşılmaz.

| E-posta | Rol | Görünür kayıt |
|---|---|---:|
| `calisan1@ebys-test.local` | `CALISAN` | 6 |
| `calisan2@ebys-test.local` | `CALISAN` | 1 |
| `bskyrd@ebys-test.local` | `BASKAN_YARDIMCISI` | 5 |
| `baskan@ebys-test.local` | `BASKAN` | 3 |
| `m9-admin@workflow.test` | `ADMIN` | — (mobil kapsam dışı) |

Görünür kayıt sütunu görünürlük kapsamının kanıtıdır: `calisan1` yalnızca kendi
altı kaydını görüyor, `calisan2`'nin taslağını görmüyor.

### Veri

Toplam 7 kayıt. `calisan1` altı workflow durumunun her birinden bir kayda
sahip — `TASLAK`, `BSK_YRD_INCELEMESINDE`, `BASKAN_INCELEMESINDE`,
`DUZENLEME_BEKLIYOR`, `ONAYLANDI`, `REDDEDILDI`. Yedinci kayıt `calisan2`'nin
taslağıdır ve görünürlük kapsamının negatif tarafını test eder.

Kayıtlar doğrudan SQL ile değil API üzerinden üretildi
([deploy/seed-test-data.sh](../deploy/seed-test-data.sh)); böylece bcrypt
parolalar, denetim satırları ve durum geçişleri tutarlı oluştu.

### Kabul kanıtı

| | |
|---|---|
| Cihaz | Samsung Galaxy A34 |
| İşletim sistemi | Android 16 |
| Ağ | Mobil veri |
| Tarih | 21 Ağustos 2026, ~23:00 TRT |
| Hesap | `calisan1@ebys-test.local` |
| Mobil build | EAS Android preview, build `cdeede67-8124-4cd9-81ac-11296e380c7c` |
| Backend SHA | `4726d69` |
| Sonuç | Giriş başarılı; kayıt listesinde 6/6 kayıt görüldü |

Cihazda görülen sayı, seed'in API üzerinden hesapladığı `calisan1` görünür
kayıt sayısıyla birebir eşleşti.

### Ortam yüzeyi

Dışarıya açık tek servis Caddy'dir (`80`/`443`). `5432` (PostgreSQL), `8080`
(backend), `8025`/`1025` (Mailpit) ve `5173` dış ağdan kapalıdır. `/mail`
arayüzü basic auth ile korunur. TLS sertifikası Let's Encrypt'ten otomatik
alınır ve yenilenir.

TEST'te **ürün web frontend'i yayınlanmaz.** Bu nedenle e-posta derin
bağlantıları bu aşamada çalışmaz; ayrıntı ve gerekçe için
[TEST_ORTAMI_NOTU.md](TEST_ORTAMI_NOTU.md).

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

> [!WARNING]
> İki bilinen açık: (1) `DELETE` ucu tokenın oturumdaki kullanıcıya ait olup
> olmadığını **doğrulamıyor**; (2) `openapi.json` bu uçları içermediği için
> üretilmiş istemcide karşılıkları yok. Ayrıntı:
> [MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md](MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md) M2.

> [!NOTE]
> Token kaydedilse bile **push şu an gönderilmiyor**: `PushNotificationService`
> yazılmış ama workflow listener'ına bağlanmamış (M3). MOB-12 bu bağlantı
> yapılmadan doğrulanamaz.

---

## Mobil kapsam dışı uçlar

`/api/admin/**` (kullanıcı ve rol yönetimi) ve `/api/user-audit-logs/**` yalnız
`ADMIN` içindir; mobil v1'de `ADMIN` rolü yok, bu uçlar bağlanmaz.
