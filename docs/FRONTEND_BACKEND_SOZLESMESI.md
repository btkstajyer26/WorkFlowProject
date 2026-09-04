# Frontend - Backend Entegrasyon Sözleşmesi

> **WF-2C2 (4 Eylül 2026):** Kayıt, liste, dosya ve geçmiş okumaları ortak visibility scope kullanır. Dinamik roller `RECORD_VIEW` ile oluşturdukları veya kendilerine atanmış kayıtları okuyabilir. ADMIN deny ve sistem rollerinin içerik/geçmiş kesimleri korunur. WF-5/WF-6 ile uygun departman/durum kapsamı da uygulanmıştır; `targetDepartmentId` ve `DEPARTMANA_GONDER` HTTP sözleşmesine eklenmiştir. [Sözleşme ve hata davranışları](WF2C2_DB8_GORUNURLUK_SOZLESMESI.md).

Bu belge EBYS frontendinin kullandığı API sözleşmesini ve henüz tamamlanmamış entegrasyon ihtiyaçlarını tanımlar. Mevcut endpoint ve cevap modellerinde backend kodu ile çalışan uygulamanın `/v3/api-docs` çıktısı esas alınır; `docs/openapi.json` bunun sürümlenmiş inceleme anlık görüntüsüdür. Gelecekte eklenmesi beklenen işlemler ayrıca "backend bekleniyor" olarak işaretlenir.

> Son dokümantasyon karşılaştırması: 4 Eylül 2026, `codex/ap-2-frontend-uyum` @ `c9b0297` (AP-2 hizalaması + V23 + WF-5/WF-6).
> WF-8 ve V18–V22 yeni HTTP uçları/alanları eklemedi. AP-2 ile rol yönetimi uçları
> ve `UserResponse`'un rol alanları değişti (aşağıda). Departman gönderimi
> bu dalda uygulanmıştır. Bu turda sürümlenmiş `openapi.json` ile çalışan
> backend arasında **32 yol ve 37 DTO şemasının alan kümelerinde fark bulunmadı**;
> bu, açıklama/required/enum ve yanıt semantiğinin birebir eşit olduğu iddiası
> değildir ve otomatik bir drift gate'i hâlâ yoktur. Açık istemci maddeleri
> aşağıdaki §"Tamamlanmamış istemci sözleşmesi" bölümündedir.
> [Teslim sınırları](README.md).

## 1. Temel kararlar

- Frontend React + Vite uygulamasıdır; yerel adresi varsayılan olarak `http://localhost:5173` olur.
- Client yalnız backend API ile konuşur. Veritabanına doğrudan erişmez.
- JWT, her korumalı istekte `Authorization: Bearer <accessToken>` başlığıyla gönderilir.
- Frontend arayüzde butonları role göre gizlese de gerçek yetkilendirme her endpointte backend tarafından yapılır.
- Client liste isteklerine başka bir kullanıcının `userId` değerini eklemez. Backend kapsamı JWT içindeki kullanıcı ve rol üzerinden belirler.
- Tarihler ISO 8601 ve saat dilimi içeren biçimde dönmelidir. Frontend bunları kullanıcının yerel saat diliminde gösterir.
- Kayıtların kalıcı kaynağı backend/veritabanıdır. Mock state yalnız geliştirme içindir.

## 2. Roller ve durumlar

### Roller

| Yerleşik `system_key` | Arayüz etiketi | Kayıt okuma kapsamı |
|---|---|---|
| `CALISAN` | Çalışan | Oluşturduğu veya doğrudan kendisine atanmış kayıtlar |
| `BASKAN_YARDIMCISI` | Başkan Yardımcısı | Oluşturduğu/atandığı kayıtlar, bütün `DUZENLEME_BEKLIYOR` ve `last_deputy_id` ile ilişkili kayıtlar |
| `BASKAN` | Başkan | Oluşturduğu/atandığı kayıtlar, bütün `BASKAN_INCELEMESINDE`, `ONAYLANDI` ve `REDDEDILDI` durumları |
| `ADMIN` | Sistem Yöneticisi | Evrak okuyamaz; kullanıcı/rol ve audit yönetimi ilgili permission'lara bağlıdır |

`ADMIN` workflow aktörü veya hedefi olamaz. Yetkili bir Admin başka bir aktif kullanıcıya `ADMIN` rolü atayabilir.

> **Bu tablo kapalı bir liste değildir.** Dinamik rol de `RECORD_VIEW` ile oluşturduğu
> veya doğrudan atandığı kaydı okuyabilir. Kapsamlar aktif kullanıcı/rol ve
> `RECORD_VIEW` gerektirir; ADMIN deny korunur. Rol kataloğu `roles` tablosundan
> gelir; liste `GET /api/admin/roles` ile okunur ve panelden rol oluşturma/düzenleme
> `AP-2` backend uçları ve rol yönetimi ekranıyla açıktır. `roles.name`
> gösterim adıdır ve **değiştirilebilir** — istemci rolü ada göre sabit bir listeye
> karşı doğrulamamalıdır. Backend sistem istisnalarını `system_key`, workflow
> kimliğini `RoleId` ile belirler.

### Kayıt durumları

```text
TASLAK
BSK_YRD_INCELEMESINDE
BASKAN_INCELEMESINDE
DUZENLEME_BEKLIYOR
ONAYLANDI
REDDEDILDI
```

`ONAYLANDI` ve `REDDEDILDI` terminal durumlardır. Bu kayıtların içeriği, dosyaları ve durumu artık değiştirilemez.

## 3. Yetki ve durum geçişleri

Backend tarafından desteklenen aksiyon değerleri:

```text
GONDER
TEKRAR_GONDER
BASKANA_ILET
CALISANA_GERI_GONDER
BASKAN_YARDIMCISINA_GERI_GONDER
ONAYLA
REDDET
```

| Mevcut durum | Rol | Aksiyon | Hedef durum | Ek kural |
|---|---|---|---|---|
| `TASLAK` | Çalışan | `GONDER` | `BSK_YRD_INCELEMESINDE` | Backend sistemdeki tek aktif Başkan Yardımcısını bulur; `targetUserId` gönderilmez |
| `DUZENLEME_BEKLIYOR` | Çalışan | `TEKRAR_GONDER` | `BSK_YRD_INCELEMESINDE` | Backend sistemdeki tek aktif Başkan Yardımcısını bulur; `targetUserId` gönderilmez |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısı | `BASKANA_ILET` | `BASKAN_INCELEMESINDE` | Backend sistemdeki tek aktif Başkanı bulur; `targetUserId` gönderilmez |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısı | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | Backend hedefi `createdBy` alanından bulur; `comment` zorunlu |
| `BASKAN_INCELEMESINDE` | Başkan | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | Backend hedefi `createdBy` alanından bulur; `comment` zorunlu |
| `BASKAN_INCELEMESINDE` | Başkan | `BASKAN_YARDIMCISINA_GERI_GONDER` | `BSK_YRD_INCELEMESINDE` | Backend hedefi `lastDeputyId` alanından bulur; `comment` zorunlu |
| `BASKAN_INCELEMESINDE` | Başkan | `ONAYLA` | `ONAYLANDI` | Not isteğe bağlı |
| `BASKAN_INCELEMESINDE` | Başkan | `REDDET` | `REDDEDILDI` | `comment` zorunlu |

Her geçiş tek transaction içinde kaydı güncellemeli, audit log eklemeli ve hedef kullanıcı için bildirim oluşturmalıdır.

## 4. Kimlik doğrulama

Mevcut endpointler:

| Metot | Adres | Koruma | Amaç |
|---|---|---|---|
| `POST` | `/api/auth/login` | Açık | Access/refresh token ve zorunlu parola değişikliği bilgisini üretir |
| `POST` | `/api/auth/refresh` | Açık | Gövdedeki refresh tokenı döndürerek access/refresh token çiftini yeniler |
| `POST` | `/api/auth/logout` | Açık | Gövdedeki aktif refresh tokenı iptal eder |
| `POST` | `/api/auth/change-password` | Bearer | Mevcut parolayı doğrulayıp parolayı değiştirir |
| `POST` | `/api/auth/forgot-password` | Açık | E-posta adresine 6 haneli doğrulama kodu yollar |
| `POST` | `/api/auth/verify-reset-code` | Açık | Kodu doğrular, tek kullanımlık sıfırlama anahtarı üretir |
| `POST` | `/api/auth/reset-password` | Açık | Sıfırlama anahtarıyla yeni parola belirler |
| `GET` | `/api/users/me` | Bearer | Aktif kullanıcının kimlik ve rol bilgisini döner |

Giriş isteği:

```json
{
  "email": "john.doe@kurum.gov.tr",
  "password": "kullanici-sifresi"
}
```

Giriş cevabı:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "mustChangePassword": true
}
```

`mustChangePassword=true` olduğunda frontend kullanıcının diğer korumalı sayfalara erişimini durdurur ve `/sifre-degistir` adresine yönlendirir. Şifre değiştirme isteği:

```json
{
  "currentPassword": "gecici-parola",
  "newPassword": "YeniParola123"
}
```

Yeni parola en az 8 karakter olmalı, en az bir harf ve bir rakam içermelidir. Ayrıca mevcut paroladan farklı olmalıdır; aynıysa backend `400 PASSWORD_REUSED` döner ve frontend mesajı yeni parola alanının altında gösterir. Başarılı değişiklik `mustChangePassword=false` yapar ve kullanıcının aktif refresh tokenlarını iptal eder. Frontend yerel oturumu temizleyip kullanıcıdan yeni parolasıyla tekrar giriş yapmasını ister.

### Unutulan parola akışı

Akış üç adımdır ve oturum gerektirmez: kullanıcı giriş ekranındaki **Şifremi unuttum** bağlantısıyla `/sifre-sifirla` adresine gider, e-postasına gelen 6 haneli kodu aynı ekranda girer, kod doğrulanınca `/sifre-degistir?token=...` parola belirleme ekranına yönlendirilir.

**1. Kod talebi** — `POST /api/auth/forgot-password`

```json
{
  "email": "john.doe@kurum.gov.tr"
}
```

E-posta sistemde bulunsa da bulunmasa da cevap aynıdır: `202 Accepted`, gövdesiz. Farklı bir cevap ucu kayıtlı e-postaları keşfetmek için kullanılabilir hâle getirirdi. Hesap varsa ve aktifse backend rastgele bir 6 haneli kod üretip e-postayla yollar; kodun yalnızca BCrypt özeti saklanır. Aynı hesap için önceki açık kodlar geçersiz kılınır ve bekleme süresi (varsayılan 60 sn) dolmadan ikinci kod üretilmez.

**2. Kod doğrulama** — `POST /api/auth/verify-reset-code`

```json
{
  "email": "john.doe@kurum.gov.tr",
  "code": "135790"
}
```

Cevap `200 OK`:

```json
{
  "resetToken": "tek-kullanimlik-anahtar",
  "expiresInSeconds": 900
}
```

Kod varsayılan olarak 10 dakika geçerlidir ve 5 yanlış denemeden sonra ölür; her başarısız durum ortak hata sözleşmesiyle `400 INVALID_OR_EXPIRED_RESET_CODE` döner (hangi sebep olduğu bilgisi sızdırılmaz). Dönen anahtar 256 bit rastgeledir, veritabanında SHA-256 özetiyle tutulur ve varsayılan 15 dakika geçerlidir.

**3. Parola belirleme** — `POST /api/auth/reset-password`

```json
{
  "token": "tek-kullanimlik-anahtar",
  "newPassword": "YeniParola123"
}
```

Başarılı istek `204 No Content` döner; anahtar yeniden kullanılamaz, parola BCrypt ile özetlenerek saklanır, `mustChangePassword=false` olur ve kullanıcının mevcut refresh tokenları iptal edilir. Geçersiz, kullanılmış veya süresi dolmuş anahtar `INVALID_OR_EXPIRED_RESET_TOKEN` kodunu döner. Yeni parola mevcut paroladan farklı olmalıdır: aynıysa `PASSWORD_REUSED` döner ve anahtar tüketilmez, kullanıcı başka bir parolayla tekrar deneyebilir. Frontend başarılı sıfırlamadan sonra `/giris?reason=password-reset` adresine gider.

Süreler ve bekleme aralığı `app.password-reset.*` ayarlarıyla (ortam değişkeni karşılıkları `PASSWORD_RESET_CODE_TTL_MINUTES`, `PASSWORD_RESET_TOKEN_TTL_MINUTES`, `PASSWORD_RESET_RESEND_COOLDOWN_SECONDS`) değiştirilebilir.

Refresh ve logout istekleri `{ "refreshToken": "..." }` gövdesini kullanır. Başarılı refresh işleminde eski token iptal edilip yeni access/refresh çifti üretilir. Varsayılan access süresi 1 saat, refresh süresi 7 gündür ve ortam değişkenleriyle değiştirilebilir. Geçersiz veya süresi dolmuş giriş/refresh bilgisi `401 INVALID_CREDENTIALS` döner.

Frontend, 401 cevabında bir kez token yenilemeyi deneyip başarısız olursa `/giris?reason=expired` adresine yönlendirecektir. Bearer tokenı tarayıcı depolamasında tutmak XSS etkisini artırır; mobil uyumluluk kararı korunurken frontend ve backend tarafında CSP, kısa access süresi ve refresh rotation uygulanmalıdır.

## 5. Kayıt API'si

### Listeleme

```http
GET /api/records?page=0&size=10&status=TASLAK&categoryId=4&q=sunucu&from=2026-08-01T00:00:00&to=2026-08-31T23:59:59&sort=updatedAt,desc
Authorization: Bearer <accessToken>
```

Desteklenen parametreler:

| Parametre | Tip | Açıklama |
|---|---|---|
| `page` | integer | Spring ile uyumlu, 0 tabanlı |
| `size` | integer | İlk sürümde `5`, `10`, `20` |
| `q` | string | Başlık ve açıklamada arama |
| `status` | enum | Tek durum filtresi |
| `categoryId` | integer/uuid | Kategori filtresi |
| `from` | ISO 8601 date-time | Oluşturulma tarihi başlangıcı |
| `to` | ISO 8601 date-time | Oluşturulma tarihi bitişi |
| `creator` | string | Oluşturan kişinin ad/soyad bilgisinde arama |
| `sort` | string | Spring Data sıralaması; ör. `updatedAt,desc` |

Sayfalı cevap:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 128,
  "totalPages": 13
}
```

Frontend URL'leri Türkçe ve kullanıcı odaklıdır; API parametrelerine servis katmanında çevrilir:

| Frontend query | API karşılığı |
|---|---|
| `q` | `q` |
| `kategori` | `categoryId` |
| `durum` | `status` |
| `baslangic` | `from` |
| `bitis` | `to` |
| `sayfa` | `page` |
| `boyut` | `size` |
| `gorunum` | API'ye aynen gönderilmez; aşağıdaki durum gruplarına çevrilir |

Rol bazlı `gorunum` eşlemeleri:

| Rol | Görünüm | Durumlar |
|---|---|---|
| Çalışan | `taslaklar` | `TASLAK` |
| Çalışan | `duzeltme-bekleyenler` | `DUZENLEME_BEKLIYOR` |
| Çalışan | `onay-asamasindakiler` | `BSK_YRD_INCELEMESINDE`, `BASKAN_INCELEMESINDE` |
| Çalışan | `sonuclananlar` | `ONAYLANDI`, `REDDEDILDI` |
| Başkan Yardımcısı | `incelenecekler` | `BSK_YRD_INCELEMESINDE` |
| Başkan Yardımcısı | `baskan-incelemesindekiler` | `BASKAN_INCELEMESINDE` |
| Başkan Yardımcısı | `duzeltmede-olanlar` | `DUZENLEME_BEKLIYOR` |
| Başkan Yardımcısı | `sonuclananlar` | `ONAYLANDI`, `REDDEDILDI` |
| Başkan | `onay-bekleyenler` | `BASKAN_INCELEMESINDE` |
| Başkan | `onaylananlar` | `ONAYLANDI` |
| Başkan | `reddedilenler` | `REDDEDILDI` |

### Detay, oluşturma ve düzenleme

| Metot | Adres | Amaç |
|---|---|---|
| `GET` | `/api/records/{id}` | Yetki kapsamındaki kayıt detayı |
| `POST` | `/api/records` | Çalışanın yeni taslağını oluşturur |
| `PUT` | `/api/records/{id}` | Sahibi olan çalışanın taslak/düzeltme kaydını günceller |

#### Düzeltmedeki kaydın içeriği dondurulur

Kayıt `CALISANA_GERI_GONDER` ile düzeltmeye düştüğünde içeriğinin o anki hali
(`title`, `description`, `categoryId`) kaydın üzerinde saklanır. Başkan
Yardımcısı `duzeltmede-olanlar` sekmesinden bu kaydı izlemeye devam eder, ancak
evrak o sırada Çalışanın elindedir: yardımcıya **devir anındaki kopya**
gösterilir. Çalışanın düzeltme sırasında kaydettiği değişiklikler ona yansımaz;
`TEKRAR_GONDER` ile kayıt yeniden yardımcıya atandığında güncel içerik açılır.

Kural detay ucunda, liste/arama ucunda ve ek dosya listesinde birlikte
uygulanır — yalnızca biri dondurulsaydı liste başlığı ya da yeni yüklenen bir ek
sızdırmaya devam ederdi. Ek dosyalar ayrıca kopyalanmaz; `uploaded_at` ve
`deleted_at` üzerinden devir anına göre süzülür, dolayısıyla devirde duran ama
sonradan silinen bir ek yardımcıda hâlâ görünür, sonradan eklenen görünmez.
Listede gizlenen bir eke kimliğiyle doğrudan erişim de `404` döner.

Kaydın sahibi Çalışan ve Başkan bu dondurmadan etkilenmez; ikisi de her zaman
güncel içeriği görür. İşlem geçmişinde ise Başkanın da kendi kırpması vardır:
geçmiş, evrak kendisine ilk iletildiği anda başlar (bkz. §6).
| `DELETE` | `/api/records/{id}` | Yalnız `TASLAK` kaydını siler veya soft-delete yapar |

Oluşturma/güncelleme gövdesi:

```json
{
  "title": "Sunucu Donanım Alım Talebi",
  "description": "Talebin ayrıntılı açıklaması",
  "categoryId": 4
}
```

Tekil kayıt cevabı düz bir modeldir ve `id`, `title`, `description`, `categoryId`, `status`, `createdAt`, `createdBy`, `createdByFullName` alanlarını taşır. Liste cevabındaki öğeler bunlara ek olarak `assignedTo` ve `updatedAt` alanlarını içerir. Kategori adı `/api/categories`, ek dosyalar dosya endpointleri ve işlem geçmişi audit endpointi üzerinden alınır.

`createdBy` kullanıcı UUID'sidir; `createdByFullName` ise onun gösterim adıdır ve **hem liste hem detay cevabında** gelir. Normal kullanıcıların başka kullanıcıları çözümleyebileceği genel bir kullanıcı listeleme endpointi yok, bu yüzden ad kayıtla birlikte gönderilir. İstemci adı işlem geçmişindeki `RECORD_CREATED` satırından türetmemelidir: geçmişi kırpılan roller (Başkan) o satırı hiç görmez ve geri düşülen ilk satır başka birini gösterir (bkz. §6). Kullanıcı silinmişse alan boş gelir; istemci kimliğe geri düşer. Oluşturan kişiye göre sunucu taraflı filtreleme `GET /api/records?creator=` parametresiyle desteklenir; bu parametre oluşturucunun ad ve soyadında arama yapar. Serbest metin `q` araması başlık ve açıklamayla sınırlı kalır.

### İş akışı aksiyonu

Backend workflow uygulama katmanının mevcut HTTP sözleşmesi tek bir aksiyon endpointi tanımlar:

```http
POST /api/records/{recordId}/workflow/actions
Authorization: Bearer <accessToken>
Content-Type: application/json
```

İstek modeli:

```json
{
  "action": "CALISANA_GERI_GONDER",
  "comment": "Bütçe kalemi ve teklif dosyası eksik."
}
```

| Alan | Zorunluluk | Kural |
|---|---|---|
| `action` | Her zaman zorunlu | `WorkflowAction` enum değerlerinden biri |
| `targetUserId` | **Hiçbir aksiyonda gönderilmez** | Hedefi her zaman backend çözer. Alan yine de gönderilirse istek `400 WORKFLOW_TARGET_NOT_ALLOWED` ile reddedilir — sessizce yok sayılmaz |
| `targetDepartmentId` | Yalnız `DEPARTMANA_GONDER` için zorunlu | Integer departman ID; `targetUserId` ile birlikte gönderilmez |
| `comment` | Geri gönderme ve `REDDET` için zorunlu | En fazla 2000 karakter; diğer aksiyonlarda isteğe bağlı |

`GONDER` ve `TEKRAR_GONDER` isteği şu biçimdedir:

```json
{
  "action": "GONDER",
  "comment": "İncelemeye gönderildi."
}
```

> **Karar — Başkan Yardımcısı hedefleme (kapandı):** `GONDER`/`TEKRAR_GONDER` hedefini backend, `BASKANA_ILET` ile aynı yoldan sistemdeki tek aktif Başkan Yardımcısından çözer. Gerekçe: Çalışana açık tek kullanıcı ucu `GET /api/users/me`'dir ve tekil rol kararı gereği kullanıcı listeleme ucu ona açılmayacaktır — yani hedefin UUID'sini güvenle keşfetmesinin bir yolu yok. Frontend'de **kişi** hedefi seçim arayüzü **yapılmayacak**.
>
> Bu karar kişi hedefi içindir. Departmana gönderim ayrı bir yoldur: [ADR-0006](decisions/0006-departman-hedefli-target-strategy.md) (**Kabul Edildi**, 4 Eylül 2026) yeni bir `DEPARTMANA_GONDER` aksiyonu ve istekte `targetDepartmentId` alanı getirir. `targetUserId` ile `targetDepartmentId` aynı anda dolu olamaz — aksi hâlde `400 VALIDATION_ERROR`. Departman seçici kişi seçici değildir ve `GONDER` yolunu değiştirmez; ikisi bir arada durduğu için gönderim ekranı kişi ve departman yollarını açıkça ayırmalıdır. V23 + WF-5/WF-6 bu aksiyonu ve HTTP alanını uygular. `TASLAK` için oluşturucu, `DUZENLEME_BEKLIYOR` için oluşturucu ve atama sahibi gönderebilir; gerekli permission `RECORD_FORWARD`, hedef durum `BSK_YRD_INCELEMESINDE` olur.
>
> Sistemde tam olarak bir aktif Başkan Yardımcısı yoksa (devir sırasında sıfır, hatalı yapılandırmada birden fazla) istek `409 WORKFLOW_ROLE_NOT_CONFIGURED` döner. Bu geçici bir durumdur; kullanıcıya "İşlemi devralacak yetkili şu anda belirlenemedi, yöneticinize başvurun" mesajı gösterilmeli, istek daha sonra tekrarlanabilir.

```json
{ "action": "DEPARTMANA_GONDER", "targetDepartmentId": 12, "comment": "Satın alma incelemesi" }
```

Departmana gönderim `assigned_department_id` alanını doldurur ve `assigned_to` alanını temizler. Hedef aktif olmalı; iniş durumu için aktif routing/transition, aktif workflow rolü, uygun aktif üye, `RECORD_VIEW` ve geçiş permission'ı bulunmalıdır. Eksik/pasif departman `400 WORKFLOW_DEPARTMENT_INVALID`, kullanılabilir iniş routing'i yoksa `409 WORKFLOW_DEPARTMENT_ROUTING_NOT_CONFIGURED` döner. Kayıt zaten departmandayken eksik routing veya yetkisiz üyelik `403 WORKFLOW_FORBIDDEN` üretir. Üyelik tek başına yetki vermez.

Yetkili departman üyeleri liste/detay/geçmiş/dosya uçlarında ortak kapsamla görünürlük kazanır. Dinamik workflow aksiyon arayüzü ve departman yönetim ekranları ayrı frontend teslimidir.

### Tamamlanmamış istemci sözleşmesi

Aşağıdaki maddeler 4 Eylül 2026 incelemesinde kod üzerinden doğrulanmıştır ve
Workflow V1 kabulünü bugün engellemektedir. Ayrıntı:
[inceleme raporu](PROJE_INCELEME_RAPORU_2026-09-04.md).

| No | Boşluk | Kabul maddesi |
| --- | --- | --- |
| **B10** | Web aksiyon paneli (`RecordActionPanel`) bütün düğmeleri `systemKey === CALISAN/BASKAN_YARDIMCISI/BASKAN` koşullarına bağlar. Dinamik rolün `systemKey` değeri `null` olduğu için, permission ve workflow bağı doğru olsa bile panel tamamen kapanır | Backend, kayıt ve aktör için **hesaplanmış kullanılabilir aksiyon ve ilişki** bilgisini istemciye sunmalıdır. Workflow kurallarının istemcide ikinci kez kurulması veya dinamik rolün başka bir sistem rolü gibi gösterilmesi kabul değildir. Aynı model mobilde de kullanılmalıdır |
| **B11** | İstek `targetDepartmentId` kabul eder ve kayıt DB'de departmana atanır; `RecordResponse`, `RecordSearchResponse` ve `WorkflowActionResponse` `assignedDepartmentId` taşımaz. Detay DTO'su doğrudan kişi atamasını da taşımaz ve web adapteri atama alanlarını sabit `null` doldurur | Ortak atama DTO'su (kişi/departman ayrımı, gösterim adı ve mümkünse kayıt sürümü) belirlenip OpenAPI, web ve mobil birlikte güncellenmelidir. İstemci boş atama ile departman kuyruğunu ayırabilmelidir |
| — | Normal kullanıcı için **uygun departman keşif ucu** yoktur; departman seçici bu olmadan yapılamaz | `DEPARTMANA_GONDER` hedefi olabilecek aktif departmanları döndüren, yetkiye saygılı bir uç tanımlanmalıdır |
| — | Departman/üyelik/routing ve permission matrisi için yönetim ucu yoktur (`AP-3`/`AP-4`/`AP-5`); `AP-8` için de yalnız `POST /api/workflow/rules/reload` vardır | Yönetim ekranları bu uçlar tanımlanmadan planlanamaz |

Başarılı aksiyon cevabı tam kayıt modeli değil, backend tarafından hesaplanan geçiş özetidir:

```json
{
  "recordId": "record-uuid",
  "action": "CALISANA_GERI_GONDER",
  "previousStatus": "BASKAN_INCELEMESINDE",
  "newStatus": "DUZENLEME_BEKLIYOR",
  "assignedTo": "calisan-uuid",
  "performedBy": "baskan-uuid",
  "performedAt": "2026-08-10T12:30:00Z"
}
```

Frontend başarılı cevaptan sonra ilgili kayıt, liste ve geçmiş sorgularını geçersiz kılarak güncel veriyi yeniden ister. Bu davranış API/query katmanında tutulur; component katmanına doğrudan `fetch` çağrısı eklenmez.

Endpoint somut controller ve uygulama servisiyle çalışır; durum/atama güncellemesi, audit kaydı ve bildirim aynı transaction içinde yürütülür. Yetkili aktör JWT'den belirlenir ve hedef kullanıcı backend tarafından çözülür.

#### Frontend yeniden gönderme davranışı

`DUZENLEME_BEKLIYOR` durumundaki kaydı formda kaydetmek yalnız `PUT /api/records/{id}` isteğiyle içeriği günceller; kayıt durumunu değiştirmez. Kullanıcı daha sonra `TEKRAR_GONDER` workflow aksiyonunu ayrıca çalıştırmalıdır. Frontend, geri dönen kaydın detayında **Yeniden Gönder** aksiyonunu göstermeli; düzenleme ekranında yalnız kaydetme sunuluyorsa kaydetme sonrasında kullanıcıyı bu aksiyona açıkça yönlendirmelidir. Bu davranış backend teslimi değil, frontend takip işidir.

### Kayıt detay cevap modeli

```json
{
  "id": "record-uuid",
  "title": "Sunucu Donanım Alım Talebi",
  "description": "Talebin ayrıntılı açıklaması",
  "categoryId": 4,
  "status": "BASKAN_INCELEMESINDE",
  "createdBy": "creator-uuid",
  "createdByFullName": "Ahmet Yılmaz",
  "createdAt": "2026-08-01T09:15:00"
}
```

Bu cevap kategori, dosya veya geçmiş nesnelerini içine gömmez. Frontend gerekli ek verileri ilgili endpointlerden alır ve sorgu önbelleğinde birleştirir.

## 6. İşlem geçmişi ve açıklamalar

| Metot | Adres | Amaç |
|---|---|---|
| `GET` | `/api/audit-logs/record/{recordId}` | Kullanıcının görmeye yetkili olduğu kaydın işlem geçmişi ve kesinleşmiş açıklamaları |

Geçmiş erişimi ortak kayıt görünürlüğüne bağlıdır; ek `AUDIT_VIEW` gerekmez.
Dinamik roller ve Çalışan görünür kaydın tam geçmişini okur. Yardımcı ve Başkan
sistem istisnalarında aşağıdaki kesimler sunucuda uygulanır; gizlenen satırlar
cevaba hiç konmaz. Sistem istisnası rol adından değil `system_key`'den seçilir.

**Geriye doğru kırpma (Başkan Yardımcısı).** `duzeltmede-olanlar` sekmesi
sayesinde geri gönderdiği kaydı `DUZENLEME_BEKLIYOR` durumunda izlemeye devam
eder, ancak evrak o sırada Çalışanın elindedir: bu aralıkta geçmiş **devir
anına kadar kırpılmış** döner. Çalışanın düzeltme sırasında ürettiği satırlar
(`RECORD_UPDATED`) listeye girmez. Çalışan `TEKRAR_GONDER` ile kaydı geri
yolladığında kayıt yeniden yardımcıya atanır ve geçmiş bütünüyle açılır.

**İleriye doğru kırpma (Başkan).** Geçmiş, evrağın Başkana **ilk iletildiği
andan itibaren** başlar; öncesindeki Çalışan–Başkan Yardımcısı trafiği
(oluşturma, düzeltme turları, geri gönderme gerekçeleri) ona kapalıdır. Kesme
noktası ilk iletimdir, sonuncusu değil: Başkan evrağı yardımcıya geri gönderip
tekrar aldığında son iletime göre kırpmak, kendi yazdığı gerekçeyi de gizlerdi.
İletimi açıklayan geçiş satırı bulunamazsa (veri tutarsızlığı) cevap boş döner.

Kaydın sahibi Çalışan her iki kırpmadan da etkilenmez; geçmişini eksiksiz görür.

Geçmişi kırpılan roller oluşturma satırını görmediği için, **kaydı oluşturanın
adı denetim izinden türetilmemelidir**; `createdByFullName` alanı hem liste hem
detay cevabında bu yüzden vardır (bkz. §5).

Cevap `AuditLogResponse` listesidir. İşlemi yapan kişi **iç içe `actor` nesnesi
değil, düz alanlar** olarak döner (`userId`, `userFullName`, `roleId`,
`roleName`) — frontend'in üretilmiş istemcisi de bu modeli kullanıyor:

```json
[
  {
    "id": "audit-uuid",
    "recordId": "record-uuid",
    "userId": "user-uuid",
    "userFullName": "Ayşe Kaya",
    "roleId": 2,
    "roleName": "BASKAN_YARDIMCISI",
    "action": "BASKANA_ILET",
    "previousStatus": "BSK_YRD_INCELEMESINDE",
    "newStatus": "BASKAN_INCELEMESINDE",
    "comment": "Uygun bulunmuştur.",
    "createdAt": "2026-08-04T10:30:00Z"
  }
]
```

Audit kayıtlarını güncelleyen veya silen endpoint olmamalıdır. Kullanıcı yalnız görmeye yetkili olduğu kaydın ilgili geçmişini görebilir; sistem genelindeki audit logları ayrı bir idari yetkidir.

Özel veya ayrı kaydedilen bir çalışma notu modeli kullanılmaz. Kullanıcı açıklamasını doğrudan workflow işlem penceresinde yazar ve aynı `POST /api/records/{recordId}/workflow/actions` isteğinin `comment` alanında gönderir. Frontend audit endpoint'ine ikinci bir yazma isteği göndermez.

Başkana iletme, gönderme ve onay açıklaması isteğe bağlı; ret ve tüm geri gönderme açıklamaları zorunludur. Açıklama en fazla 2000 karakterdir. Başarılı işlem tek backend transaction'ında durum/atama güncellemesini, append-only audit kaydını ve bildirimi tamamlar. Kesinleşen `comment` yalnız ilgili audit olayında kalır, güncellenemez ve sonraki yetkili kullanıcı tarafından İşlem Geçmişi'nde okunur.

## 7. Kategoriler

| Metot | Adres | Amaç |
|---|---|---|
| `GET` | `/api/categories` | Form ve filtrelerde kullanılacak tek kategori kaynağı |

Kategori adı frontend içinde kalıcı enum olarak kabul edilmemelidir. Şu anki mock değerler: İdari, Mali, İnsan Kaynakları, Bilgi İşlem ve Teknik.

Başkan Yardımcısı ve Başkan frontend tarafından seçilmez. Backend beklenen tek aktif kullanıcıyı bulamazsa `409`, birden fazla aktif kullanıcı bulursa yine `409` dönmelidir.

## 8. Admin kullanıcı, rol ve log API'si

| Metot | Adres | Amaç |
|---|---|---|
| `GET` | `/api/admin/users?page=0&size=10&q=&role=&active=` | Kullanıcı listesi, arama ve filtreleme |
| `POST` | `/api/admin/users` | Varsayılan Çalışan rolüyle hesap açma; istek rol alanı içermez |
| `PATCH` | `/api/admin/users/{id}/role` | Rol değiştirme; Başkan Yardımcısı koltuğunun devri de aynı istekte yapılır |
| `PATCH` | `/api/admin/users/{id}/active` | Hesabı etkinleştirme/pasifleştirme |
| `GET` | `/api/admin/roles?includeInactive=false` | Rol kataloğu; `ROLE_VIEW` ister. Cevap sayfalanmamış düz dizidir ve rol adı sabit rol listesine çevrilmeden gösterilir. Varsayılan çağrı yalnız **atanabilir (aktif)** rolleri döner; yönetim ekranı pasifleri de görmek için `includeInactive=true` gönderir. AP-2 rol yönetimi ekranının (`/admin/roller`) kaynağıdır. Cevap `id`, `name`, `description`, `systemKey`, `system`, `workflowActor`, `maxUsers` ve `active` taşır |
| `POST` | `/api/admin/roles` | Panelden dinamik rol açma; `ROLE_MANAGE` ister. Gövde `name` (zorunlu, ≤100), `description` (≤255) ve `workflowActor` taşır. Yeni rol daima dinamik (`systemKey = null`) ve sınırsız kapasiteli açılır |
| `PATCH` | `/api/admin/roles/{id}` | Rol güncelleme; `ROLE_MANAGE` ister. Kısmi gövde: yalnız gönderilen `name` / `description` / `workflowActor` / `active` alanları uygulanır. Sistem rolü yeniden adlandırılabilir ama pasifleştirilemez ve workflow aktörlüğü değiştirilemez; `systemKey` ve `system` istemciden hiçbir koşulda değiştirilemez. Rol **silinmez** |
| `GET` | `/api/admin/audit-logs?type=USER\|RECORD&page=0&size=20&q=` | Evrak ve kullanıcı/rol loglarını listeleme |
| `POST` | `/api/workflow/rules/reload` | Geçiş kuralı snapshot'ını veritabanından yeniden okur; grafiği **yazmaz**. `WORKFLOW_MANAGE` ister, cevap `{"ruleCount": n}`. Geçersiz kural kümesi yüklenmez ve çalışan snapshot korunur |

Admin kuralları:

WF-8 Java servisi hazırdır; AP-8'in bağ listeleme/ekleme/kaldırma HTTP uçları henüz
eklenmedi. Tamer'in adapter'ı `WorkflowActorBindingService` kullanmalı; mutasyon
çevresinde transaction açmamalı ve ek reload çağırmamalıdır. Yetkiler, DTO ve hata
kodları: [WF-8 / AP-8 sözleşmesi](WF8_AP8_AKTOR_ROL_BAGLAMA_SOZLESMESI.md).

- Kullanıcı silinmez veya rolsüz bırakılmaz; erişim `active=false` ile kapatılır.
- Admin başka bir aktif kullanıcıya `ADMIN` rolü atayabilir; mevcut Admin hesabının rolü ve aktifliği bu arayüzden değiştirilemez.
- `ADMIN`, `BASKAN` ve `BASKAN_YARDIMCISI` **tekil** rollerdir: aynı anda yalnız bir aktif kullanıcı tutabilir. Rol zaten başka bir aktif kullanıcıdaysa istek `409 ADMIN_LIMIT_EXCEEDED` ile reddedilir — backend mevcut sahibi kendiliğinden `CALISAN`'a düşürmez.
- Pasifleştirilen kullanıcının aktif tokenları iptal edilmelidir.
- Hesap açma, rol değişikliği/devri ve aktiflik değişikliği append-only `user_audit_logs` kaydı üretmelidir.
- `audit_logs` ve `user_audit_logs` tek sayfalı API modeliyle sunulur; update/delete audit endpointi olmaz.

Rol yönetimi kuralları (`AP-2`):

- Rol **silinmez**; DELETE ucu yoktur. Erişim `active=false` ile kapatılır.
- `systemKey` ve `isSystem` istemciden hiçbir koşulda değiştirilemez. Yeni rol daima `systemKey=null`, `isSystem=false` ve `maxUsers=null` (sınırsız) olarak açılır.
- Sistem rolü **yeniden adlandırılabilir** — `roles.name` görünen addır — ama pasifleştirilemez ve workflow aktörlüğü değiştirilemez. Panel bu iki işlemi kilitli gösterir; asıl kararı backend verir.
- Aktif kullanıcısı olan rol pasifleştirilemez; istek `400 BUSINESS_RULE_VIOLATION` ile reddedilir. Böylece hiçbir kullanıcı pasif rolde kalmaz.
- Açık workflow kaydı bulunan rolün pasifleştirilmesi veya workflow aktörlüğünün kapatılması `409 ROLE_IN_USE` ile reddedilir; kontrol departman kuyruklarını da kapsar (WF-8 ile aynı kullanım koruması).
- Rol adı benzersizdir ve **büyük/küçük harf ayrımı yapmaz**: "Muhasebe" varken "muhasebe" açılamaz. Karşılaştırma Türkçe kurallarıyla yapılır — "İdari" ile "idari" aynı sayılır, "Isıtma" ile "İsıtma" farklıdır. Pasif rollerin adı da yeniden kullanılamaz. Ön kontrol `400 BUSINESS_RULE_VIOLATION` döner ve mesaj çakışılan kaydın kendi yazımını gösterir.
- Bu kural uygulama katmanındadır; `roles.name` veritabanı kısıtı harf duyarlıdır. Eşzamanlı iki isteğin aynı adı farklı harflerle yazması teorik olarak geçebilir. Kalıcı çözüm `upper(name)` üzerinde bir unique index'tir ve ayrı bir migration ister (DB kulvarı).
- `workflowActor`, rolün mevcut geçişlere aktör olarak bağlanabilmesi için gerekir (`WF-8` şartı: `is_workflow_actor=true`, `is_system=false`, `system_key=NULL`).
- Rol oluşturma ve güncelleme append-only `user_audit_logs` kaydı üretir: `ROLE_CREATED` ve `ROLE_UPDATED`. Bu kayıtlarda hedef kullanıcı yoktur; etkilenen rol `previous_role_id` / `new_role_id` alanlarında taşınır.
- İstemci rolü ada göre sabit bir listeye karşı doğrulamaz. Panelin `AdminRole` tipi `UserRole` union'ından bağımsızdır ve rol adı sunucudan geldiği gibi gösterilir.

### 8.1 `UserResponse`'ta rol kimliği ve gösterim adı

`GET /api/users/me` ve bütün `/api/admin/users` cevapları rolü **üç ayrı alanla**
taşır. Karıştırılmamalıdır:

| Alan | Anlam | İstemci nasıl kullanır |
| --- | --- | --- |
| `roleId` | İlişkisel kimlik (`roles.id`) | Rol değiştirme isteğinde `roleId` olarak geri gönderilir |
| `systemKey` | Yerleşik rolün **değişmez** teknik anahtarı; dinamik rolde `null` | **Davranış ve arayüz kararları yalnız buna bakar** |
| `roleName` | Gösterim adı (`roles.name`) | Yalnız ekranda gösterilir |

```json
{
  "id": "user-uuid",
  "firstName": "Ayşe",
  "lastName": "Kaya",
  "email": "ayse.kaya@kurum.gov.tr",
  "roleId": 2,
  "systemKey": "BASKAN_YARDIMCISI",
  "roleName": "Başkan Yardımcısı",
  "active": true,
  "createdAt": "2026-08-01T09:00:00"
}
```

> **İstemci rol adını sabit bir listeye karşı doğrulamamalıdır.** AP-2 ile panelden
> dinamik rol açılabiliyor ve yerleşik rol yeniden adlandırılabiliyor; adı kapalı bir
> listeye karşı denetleyen bir istemci, dinamik role atanmış kullanıcının oturum
> açmasını tamamen engeller. `systemKey` bilinen bir anahtar değilse rol dinamiktir
> ve o kullanıcı hiçbir sistem rolüne özel arayüz almaz — bu bir hata durumu değildir.

`roleName` yerleşik roller için başlangıçta teknik adla aynıdır (`"CALISAN"`);
arayüz bu durumda kendi yerelleştirilmiş etiketini gösterir, Admin rolü yeniden
adlandırdığında ise sunucudan gelen ad kazanır.

### 8.2 Başkan Yardımcısı koltuğunun devri

Bu kural iki kez değişti; aşağıdaki metin **çalışan kodun** karşılığıdır
(`UserService.changeRole` / `UserService.setActive`).

Koltuk devri ayrı bir uç değildir, `PATCH /api/admin/users/{id}/role` isteğinin
gövdesinde yapılır:

```json
{
  "roleName": "BASKAN",
  "replacementBaskanYardimcisiId": "user-uuid"
}
```

- `roleName` zorunludur. `replacementBaskanYardimcisiId` normalde gönderilmez.
- Bir kullanıcı **`BASKAN_YARDIMCISI` rolünden çıkıyorsa** koltuk boşalacağı için `replacementBaskanYardimcisiId` **aynı istekte zorunludur**; gönderilmezse `400 BUSINESS_RULE_VIOLATION`. Backend rastgele/otomatik atama yapmaz, devredilecek kişiyi Admin açıkça seçer.
- Belirtilen kullanıcı aynı transaction içinde `BASKAN_YARDIMCISI` yapılır, eski Başkan Yardımcısına atanmış kayıtlar yeni kullanıcıya devredilir ve rol/görev devri audit kayıtları üretilir.
- Yerine atanacak kullanıcı koltuğu boşaltan kişinin kendisi olamaz ve pasif bir hesap olamaz → `400 BUSINESS_RULE_VIOLATION`. Kullanıcı bulunamazsa `404 RESOURCE_NOT_FOUND`.
- **Aktif Başkan Yardımcısı doğrudan pasifleştirilemez.** `PATCH /api/admin/users/{id}/active` isteği `400 BUSINESS_RULE_VIOLATION` döner ("Önce Başkan Yardımcısı rolünü başka bir aktif kullanıcıya devredin"). Arayüz önce yukarıdaki devir isteğini yaptırmalı, pasifleştirmeyi ondan sonra denemelidir.
- Admin hesabı da bu ekrandan pasifleştirilemez → `400 BUSINESS_RULE_VIOLATION`.

Hesap açma isteği `firstName`, `lastName`, `email` ve `password` alanlarını taşır; **rol alanı içermez**, hesap her zaman `CALISAN` rolüyle açılır ve `mustChangePassword=true` başlar. Diğer roller yalnız yukarıdaki rol değiştirme ucuyla verilir.

## 9. Dosyalar

| Metot | Adres | Amaç |
|---|---|---|
| `POST` | `/api/records/{id}/files` | `multipart/form-data`; aynı `file` alanıyla bir veya daha çok dosya yükleme |
| `GET` | `/api/records/{id}/files` | Yetki kapsamındaki kaydın eklerini listeleme |
| `GET` | `/api/files/{id}/preview` | Yetki kontrolünden sonra önizleme |
| `GET` | `/api/files/{id}/download` | Yetki kontrolünden sonra indirme |
| `DELETE` | `/api/files/{id}` | Yalnız düzenlenebilir kayıttaki eki kaldırma |

Frontend ve backend PDF, DOC, DOCX, XLS, XLSX, PNG, JPG ve JPEG dosyalarını kabul eder. Backend dosya başına ve istek başına 10 MB sınırı uygular; içeriği Apache Tika ile doğrular, uzantı/MIME uyumunu denetler ve dosyayı rastgele saklama adıyla kaydeder. Kayıt başına azami dosya adedi henüz kesinleşmemiştir.

## 10. Bildirimler

Güncel OpenAPI sözleşmesinde kesinleşen ve MSW ile modellenen işlemler:

| Metot | Adres | Amaç |
|---|---|---|
| `GET` | `/api/notifications` | JWT kullanıcısının okunmuş ve okunmamış bildirimlerini sayfalı getirir |
| `GET` | `/api/notifications/unread` | JWT kullanıcısının okunmamış bildirimlerini alır |
| `GET` | `/api/notifications/unread/count` | Menü rozeti için okunmamış bildirim sayısını alır |
| `PUT` | `/api/notifications/{id}/read` | Kullanıcıya ait tek bildirimi okundu yapar |

`GET /api/notifications` backend'de yazıldı. Cevap gövdesi diğer listelerdeki
`PagedResponse` ile aynıdır (`content`, `page`, `size`, `totalElements`,
`totalPages`); `content` öğeleri `NotificationResponse`'tur. Sayfa numarası
0'dan başlar, `size` en fazla 100'e kadar dikkate alınır. **Sıralama
isteğe bağlı değildir:** liste her zaman en yeniden eskiye döner ve gönderilen
`sort` parametresi yok sayılır.

`PUT /api/notifications/read-all` ilk sürüm kapsamı dışındadır. Frontend “Tümü” ve “Okunmamış” görünümlerini sunar ancak bildirimleri yalnızca tek tek okundu yapar.

Mevcut `NotificationResponse`; `id`, `recordId`, `message`, `notificationType`, `read` ve `createdAt` alanlarını taşır. Kullanıcı kimliği JWT'den belirlenir ve cevapta ayrıca gönderilmez. Mevcut sürüm REST/polling kullanır. WebSocket planlanan gelecek kanaldır; uygulanmış değildir.

Push kanalı mobil istemciye özeldir. Backend aynı alıcı matrisi için FCM HTTP v1
gönderimi yapar; bu kanal web REST bildirim sözleşmesini değiştirmez.

### E-posta hızlı işlem

E-posta bağlantısı `${FRONTEND_URL}/hizli-islem#token=...` sayfasını açar.
`QuickActionPage` tokenı URL fragment'ından okur ve `history.replaceState` ile
adres çubuğundan temizler; token query string'e veya backend erişim loglarına taşınmaz.

| Metot | Adres | Gövde | Davranış |
|---|---|---|---|
| `POST` | `/api/public/mail-actions/preview` | `{ "token": "..." }` | Tokenı doğrular ve `recordId`, `recordTitle`, `recordStatus`, `action`, `recipientName`, `expiresAt` döner; durum değiştirmez |
| `POST` | `/api/public/mail-actions/consume` | `{ "token": "..." }` | Kullanıcı açıkça onayladıktan sonra tek kullanımlık tokenı tüketir ve `{ "recordId": "uuid" }` döner |

İki uç da JWT istemez; güvenlik, 256 bit süreli tokenın SHA-256 özeti ve
kayıt/aksiyon/alıcı bağı üzerinden sağlanır. Preview hiçbir zaman otomatik
consume çağrısına dönüşmez; posta güvenlik tarayıcılarının linki önceden açması
workflow mutasyonu üretmez.

## 11. Standart hata cevabı

Spring Boot `@ControllerAdvice` ile kullanılan ortak cevap biçimi:

```json
{
  "timestamp": "2026-08-04T12:15:00Z",
  "status": 409,
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Bu kayıt mevcut durumunda yeniden gönderilemez.",
  "fieldErrors": [
    { "field": "title", "message": "Başlık zorunludur." }
  ]
}
```

Beklenen HTTP durumları:

| HTTP | Frontend davranışı |
|---|---|
| `400` | Alan hatalarını ilgili form alanlarında gösterir |
| `401` | Bir kez refresh; başarısızsa girişe yönlendirir |
| `403` | Yetki ekranını gösterir |
| `404` | Kayıt bulunamadı ekranını gösterir |
| `409` | Güncel olmayan durum/geçersiz geçiş mesajını gösterip kaydı yeniden çeker. `WORKFLOW_VERSION_CONFLICT` ve `VERSION_CONFLICT` kodları kayıt siz işlem yaparken değiştiği anlamına gelir: detay yeniden çekilmeli, güncel durum gösterilmeli, aksiyon **otomatik tekrarlanmamalıdır** |
| `413` / `415` | Dosya boyutu/türü mesajını dosya alanında gösterir |
| `500` | Hassas iç ayrıntı göstermeden genel hata mesajı sunar |

## 12. Kalan backend entegrasyon ihtiyaçları

1. Kayıt başına azami ek dosya adedi ve buna karşılık gelecek hata kodu
2. README'deki ortak hata sözleşmesini tamamlamak için `ApiError` cevabına istek yolu (`path`) eklenmesi; dağıtık izleme kullanılacaksa `traceId` alanının ayrıca kararlaştırılması

## 13. Açık ürün kararları

- `notifications.record_id` zorunluysa kayıttan bağımsız sistem duyuruları desteklenmeyecek; gerekiyorsa şema değişmeli.
- `ADMIN`, `BASKAN` ve `BASKAN_YARDIMCISI` tekil kalır. Başkan Yardımcısı için atomik devir modeli uygulanmıştır ve devredilecek kişi açıkça seçilir; rastgele kullanıcı atanmaz. Başkan için eşdeğer bir devir alanı/işlemi yoktur: mevcut Başkan varken başka bir kullanıcıyı Başkan yapma isteği `409 ADMIN_LIMIT_EXCEEDED` döner. Ürün kararı olarak bu davranışın korunacağı veya yeni Başkanı açıkça seçen atomik bir Başkanlık devri sözleşmesi ekleneceği netleştirilmelidir.
- Profil güncelleme endpointi henüz kapsam dışıdır. Zorunlu ilk giriş parola değişikliği `POST /api/auth/change-password` ile desteklenir.
- Self-service kayıt/signup ekranı kapsam dışıdır; kullanıcı hesaplarını yetkili sistem yöneticisi oluşturur.
