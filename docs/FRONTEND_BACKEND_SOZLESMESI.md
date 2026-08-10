# Frontend - Backend Entegrasyon Sözleşmesi

Bu belge EBYS frontendinin ihtiyaç duyduğu API kabiliyetlerini tanımlar. Endpoint adları backend ekibinin Swagger/OpenAPI dokümanıyla kesinleştirilecektir. Buradaki örnek adresler öneridir; rol, durum ve aksiyon değerleri ise mevcut backend diyagramındaki adlarla uyumludur.

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

| API değeri | Arayüz etiketi | Temel kapsam |
|---|---|---|
| `CALISAN` | Çalışan | Yalnız kendi kayıtları |
| `BASKAN_YARDIMCISI` | Başkan Yardımcısı | Kendisine atanan ve kendi işlem yaptığı kayıtlar |
| `BASKAN` | Başkan | Onay aşamasında kendisine gelen ve kendi işlem yaptığı kayıtlar |
| `ADMIN` | Sistem Yöneticisi | Kullanıcı/rol yönetimi ve sistem genelindeki audit kayıtlarını görüntüleme |

`ADMIN` workflow aktörü veya hedefi olamaz. Yetkili bir Admin başka bir aktif kullanıcıya `ADMIN` rolü atayabilir.

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

Backend tarafından desteklenmesi beklenen aksiyon değerleri:

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
| `TASLAK` | Çalışan | `GONDER` | `BSK_YRD_INCELEMESINDE` | Backend tek aktif Başkan Yardımcısını otomatik bulur |
| `DUZENLEME_BEKLIYOR` | Çalışan | `TEKRAR_GONDER` | `BSK_YRD_INCELEMESINDE` | Backend tek aktif Başkan Yardımcısını otomatik bulur |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısı | `BASKANA_ILET` | `BASKAN_INCELEMESINDE` | Başkan hedefini backend organizasyon kuralıyla belirleyebilir |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısı | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | `comment` zorunlu |
| `BASKAN_INCELEMESINDE` | Başkan | `CALISANA_GERI_GONDER` | `DUZENLEME_BEKLIYOR` | `comment` zorunlu |
| `BASKAN_INCELEMESINDE` | Başkan | `BASKAN_YARDIMCISINA_GERI_GONDER` | `BSK_YRD_INCELEMESINDE` | `comment` zorunlu |
| `BASKAN_INCELEMESINDE` | Başkan | `ONAYLA` | `ONAYLANDI` | Not isteğe bağlı |
| `BASKAN_INCELEMESINDE` | Başkan | `REDDET` | `REDDEDILDI` | `comment` zorunlu |

Her geçiş tek transaction içinde kaydı güncellemeli, audit log eklemeli ve hedef kullanıcı için bildirim oluşturmalıdır.

## 4. Kimlik doğrulama

Önerilen endpointler:

| Metot | Adres | Koruma | Amaç |
|---|---|---|---|
| `POST` | `/api/auth/login` | Açık | Access/refresh token ve kullanıcı özeti üretir |
| `POST` | `/api/auth/refresh` | Refresh token | Access token yeniler |
| `POST` | `/api/auth/logout` | Bearer | Aktif refresh tokenı iptal eder |
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
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "user-uuid",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@kurum.gov.tr",
    "role": "CALISAN"
  }
}
```

Backend ekibinin ayrıca netleştirmesi gerekenler:

- Refresh tokenın request body mi yoksa ayrı bir header ile mi gönderileceği
- Refresh token rotation ve iptal davranışı
- Access ve refresh süreleri
- Süresi dolmuş, geçersiz ve iptal edilmiş tokenlar için hata `code` değerleri

Frontend, 401 cevabında bir kez token yenilemeyi deneyip başarısız olursa `/giris?reason=expired` adresine yönlendirecektir. Bearer tokenı tarayıcı depolamasında tutmak XSS etkisini artırır; mobil uyumluluk kararı korunurken frontend ve backend tarafında CSP, kısa access süresi ve refresh rotation uygulanmalıdır.

## 5. Kayıt API'si

### Listeleme

```http
GET /api/records?page=0&size=10&status=TASLAK&categoryId=4&q=sunucu&from=2026-08-01&to=2026-08-31&sort=updatedAt,desc
Authorization: Bearer <accessToken>
```

Desteklenmesi beklenen parametreler:

| Parametre | Tip | Açıklama |
|---|---|---|
| `page` | integer | Öneri: Spring ile uyumlu, 0 tabanlı |
| `size` | integer | İlk sürümde `5`, `10`, `20` |
| `q` | string | Kayıt numarası, başlık ve açıklamada arama |
| `status` | enum veya tekrar eden parametre | Bir veya birden fazla durum |
| `categoryId` | integer/uuid | Kategori filtresi |
| `from` | `YYYY-MM-DD` | Güncellenme tarihi başlangıcı |
| `to` | `YYYY-MM-DD` | Güncellenme tarihi bitişi |
| `sort` | string | Öneri: `updatedAt,desc` |

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

| Metot | Önerilen adres | Amaç |
|---|---|---|
| `GET` | `/api/records/{id}` | Yetki kapsamındaki kayıt detayı |
| `POST` | `/api/records` | Çalışanın yeni taslağını oluşturur |
| `PUT` | `/api/records/{id}` | Sahibi olan çalışanın taslak/düzeltme kaydını günceller |
| `DELETE` | `/api/records/{id}` | Yalnız `TASLAK` kaydını siler veya soft-delete yapar |

Oluşturma/güncelleme gövdesi:

```json
{
  "title": "Sunucu Donanım Alım Talebi",
  "description": "Talebin ayrıntılı açıklaması",
  "categoryId": 4
}
```

Backend tarafından üretilmesi gereken alanlar: `id`, benzersiz ve değişmez `recordNo`, `status`, `createdBy`, `assignedTo`, `createdAt`, `updatedAt`.

### İş akışı aksiyonu

Tek bir aksiyon endpointi mevcut backend enum yapısına daha doğrudan uyar:

```http
POST /api/records/{id}/actions
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "action": "CALISANA_GERI_GONDER",
  "comment": "Bütçe kalemi ve teklif dosyası eksik."
}
```

Backend ayrı `/submit`, `/forward`, `/approve`, `/reject`, `/return` endpointleri kullanırsa frontend servis adaptörü buna uyarlanabilir. Ancak request/response ve hata davranışı Swagger'da tek anlamlı biçimde belgelenmelidir.

### Kayıt cevap modeli

```json
{
  "id": "record-uuid",
  "recordNo": "EBYS-2026-000023",
  "title": "Sunucu Donanım Alım Talebi",
  "description": "Talebin ayrıntılı açıklaması",
  "category": { "id": 4, "name": "Bilgi İşlem" },
  "status": "BASKAN_INCELEMESINDE",
  "createdBy": {
    "id": "employee-uuid",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@kurum.gov.tr"
  },
  "assignedTo": {
    "id": "chair-uuid",
    "firstName": "Mehmet",
    "lastName": "Demir",
    "role": "BASKAN"
  },
  "attachments": [],
  "lastAction": {
    "action": "BASKANA_ILET",
    "comment": "Teknik şartname uygun bulundu.",
    "actor": { "id": "deputy-uuid", "firstName": "Ayşe", "lastName": "Kaya" },
    "createdAt": "2026-08-04T10:30:00Z"
  },
  "createdAt": "2026-08-01T09:15:00Z",
  "updatedAt": "2026-08-04T10:30:00Z"
}
```

## 6. İşlem geçmişi ve notlar

| Metot | Önerilen adres | Amaç |
|---|---|---|
| `GET` | `/api/records/{id}/history` | Kullanıcının görmeye yetkili olduğu kaydın geçmişi |
| `GET` | `/api/records/{id}/notes` | Kullanıcının görmeye yetkili olduğu kaydın notları |
| `PUT` | `/api/records/{id}/notes/me` | JWT kullanıcısının kayıttaki tek notunu oluşturma veya güncelleme |

Geçmiş cevabı en az şu alanları taşımalıdır:

```json
[
  {
    "id": "audit-uuid",
    "action": "BASKANA_ILET",
    "actor": {
      "id": "user-uuid",
      "firstName": "Ayşe",
      "lastName": "Kaya",
      "role": "BASKAN_YARDIMCISI"
    },
    "comment": "Uygun bulunmuştur.",
    "createdAt": "2026-08-04T10:30:00Z"
  }
]
```

Audit kayıtlarını güncelleyen veya silen endpoint olmamalıdır. Kullanıcı yalnız görmeye yetkili olduğu kaydın ilgili geçmişini görebilir; sistem genelindeki audit logları ayrı bir idari yetkidir.

Bağımsız kayıt notları audit geçmişinden ayrı tutulur. Çalışan, Başkan Yardımcısı ve Başkan, görmeye yetkili oldukları her kayıt için en fazla bir not tutabilir. `PUT /notes/me` aynı kullanıcı ve kayıt için yeni satır üretmek yerine mevcut notu günceller. `ONAYLANDI` veya `REDDEDILDI` durumundaki kayıtların notları salt okunurdur.

Not kaydetme isteği:

```json
{
  "body": "Teknik plan ve bütçe kalemleri kontrol edildi.",
  "version": 1
}
```

Yeni not oluşturulurken `version` gönderilmez. Güncellemede istemci son okuduğu `version` değerini gönderir; eşleşmiyorsa backend `409 NOTE_VERSION_CONFLICT` döndürür. Başarılı cevap şu alanları taşır:

```json
{
  "id": "note-uuid",
  "recordId": "record-uuid",
  "author": {
    "id": "user-uuid",
    "firstName": "Ayşe",
    "lastName": "Kaya",
    "role": "BASKAN_YARDIMCISI"
  },
  "body": "Teknik plan ve bütçe kalemleri kontrol edildi.",
  "createdAt": "2026-08-04T10:15:00Z",
  "updatedAt": "2026-08-04T10:30:00Z",
  "version": 2
}
```

Not gövdesi boş olamaz ve en fazla 1000 karakterdir. Veritabanındaki `(record_id, author_id)` benzersiz kısıtı bu kuralın yarış durumlarında da korunmasını sağlar. Not oluşturma/güncelleme `records.updated_at`, `records.last_action` veya append-only `audit_logs` satırlarını değiştirmez; süreç aksiyonuna yazılan `comment` ise yalnız işlem geçmişinde kalır ve güncellenemez.

## 7. Kategoriler

| Metot | Önerilen adres | Amaç |
|---|---|---|
| `GET` | `/api/categories?active=true` | Form ve filtrelerde kullanılacak tek kategori kaynağı |

Kategori adı frontend içinde kalıcı enum olarak kabul edilmemelidir. Şu anki mock değerler: İdari, Mali, İnsan Kaynakları, Bilgi İşlem ve Teknik.

Başkan Yardımcısı ve Başkan frontend tarafından seçilmez. Backend beklenen tek aktif kullanıcıyı bulamazsa `409`, birden fazla aktif kullanıcı bulursa yine `409` dönmelidir.

## 8. Admin kullanıcı, rol ve log API'si

| Metot | Önerilen adres | Amaç |
|---|---|---|
| `GET` | `/api/admin/users?page=0&size=10&q=&role=&active=` | Kullanıcı listesi, arama ve filtreleme |
| `POST` | `/api/admin/users` | Varsayılan Çalışan rolüyle hesap açma; istek rol alanı içermez |
| `PATCH` | `/api/admin/users/{id}/role` | Rol değiştirme veya Başkan Yardımcısı rolünü devretme |
| `PATCH` | `/api/admin/users/{id}/active` | Hesabı etkinleştirme/pasifleştirme |
| `GET` | `/api/admin/roles` | Atanabilir roller; `ADMIN` dahil |
| `GET` | `/api/admin/audit-logs?type=USER|RECORD&page=0&size=20&q=` | Evrak ve kullanıcı/rol loglarını listeleme |

Admin kuralları:

- Kullanıcı silinmez veya rolsüz bırakılmaz; erişim `active=false` ile kapatılır.
- Admin başka bir aktif kullanıcıya `ADMIN` rolü atayabilir; mevcut Admin hesabının rolü ve aktifliği bu arayüzden değiştirilemez.
- `BASKAN_YARDIMCISI` rolü verildiğinde mevcut aktif yardımcı `CALISAN`, hedef kullanıcı `BASKAN_YARDIMCISI` yapılır. İki güncelleme tek transaction içinde olmalıdır.
- Aktif yardımcı rol devredilmeden pasifleştirilemez; `409 DEPUTY_TRANSFER_REQUIRED` dönmelidir.
- Pasifleştirilen kullanıcının aktif tokenları iptal edilmelidir.
- Hesap açma, rol değişikliği/devri ve aktiflik değişikliği append-only `user_audit_logs` kaydı üretmelidir.
- `audit_logs` ve `user_audit_logs` tek sayfalı API modeliyle sunulur; update/delete audit endpointi olmaz.

Hesap açma isteği `firstName`, `lastName`, `email`, `role` alanlarını taşır. Cevap ilk girişte değiştirilecek geçici parolayı yalnız bir kez dönebilir; hesap `mustChangePassword=true` başlamalıdır.

## 9. Dosyalar

| Metot | Önerilen adres | Amaç |
|---|---|---|
| `POST` | `/api/records/{id}/files` | `multipart/form-data` ile dosya yükleme |
| `GET` | `/api/files/{id}/preview` | Yetki kontrolünden sonra önizleme |
| `GET` | `/api/files/{id}/download` | Yetki kontrolünden sonra indirme |
| `DELETE` | `/api/files/{id}` | Yalnız düzenlenebilir kayıttaki eki kaldırma |

İlk sürümde frontend şu uzantıları kabul eder: PDF, DOC, DOCX, XLS, XLSX, PNG, JPG ve JPEG. Kesin dosya boyutu, dosya adedi ve MIME listesi backend ekibiyle netleştirilmelidir. Backend uzantıya güvenmemeli; içeriği/MIME değerini doğrulamalı ve saklama adını kullanıcıya göstermemelidir.

## 10. Bildirimler

| Metot | Önerilen adres | Amaç |
|---|---|---|
| `GET` | `/api/notifications?page=0&size=20&unread=true` | JWT kullanıcısına ait bildirimler |
| `PATCH` | `/api/notifications/{id}/read` | Tek bildirimi okundu yapar |
| `PATCH` | `/api/notifications/read-all` | Kullanıcının tüm bildirimlerini okundu yapar |

Bildirim modeli mevcut diyagramla uyumlu olarak `id`, `userId`, `recordId`, `message`, `isRead`, `createdAt` alanlarını taşımalıdır. İlk sürüm REST/polling ile çalışabilir; SSE veya WebSocket sonraki sürüme bırakılabilir.

## 11. Standart hata cevabı

Spring Boot `@ControllerAdvice` ile ortak cevap önerisi:

```json
{
  "timestamp": "2026-08-04T12:15:00Z",
  "status": 409,
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Bu kayıt mevcut durumunda yeniden gönderilemez.",
  "fieldErrors": [
    { "field": "title", "message": "Başlık zorunludur." }
  ],
  "traceId": "7f9c8a4b1a6e"
}
```

Beklenen HTTP durumları:

| HTTP | Frontend davranışı |
|---|---|
| `400` | Alan hatalarını ilgili form alanlarında gösterir |
| `401` | Bir kez refresh; başarısızsa girişe yönlendirir |
| `403` | Yetki ekranını gösterir |
| `404` | Kayıt bulunamadı ekranını gösterir |
| `409` | Güncel olmayan durum/geçersiz geçiş mesajını gösterip kaydı yeniden çeker |
| `413` / `415` | Dosya boyutu/türü mesajını dosya alanında gösterir |
| `500` | Genel hata mesajı ve `traceId` gösterir |

## 12. Backend ekibinden beklenen teslimler

1. Lokal API base URL ve varsa test ortamı URL'si
2. Güncel Swagger/OpenAPI bağlantısı
3. JWT login, refresh, logout ve hata örnekleri
4. Sayfalama indeksinin 0 mı 1 mi başladığı
5. Liste filtrelerinin ve çoklu `status` formatının kesin hali
6. Kayıt aksiyon endpointinin kesin adresi ve request modeli
7. Tek aktif Başkan Yardımcısı/Başkan çözümleme kuralı
8. Kategori ve Admin kullanıcı/rol/audit endpointleri
9. Dosya türü, boyutu ve adet sınırları
10. Standart hata response'u ve tüm `code` değerleri
11. CORS için `http://localhost:5173` izni

Frontend ekibinin veritabanı bağlantı bilgisine veya şifresine ihtiyacı yoktur.

## 13. Açık ürün kararları

- `records` tablosunda kullanıcıya gösterilecek benzersiz `record_no` alanı kesinleşmeli.
- `notifications.record_id` zorunluysa kayıttan bağımsız sistem duyuruları desteklenmeyecek; gerekiyorsa şema değişmeli.
- Profil güncelleme ve şifre değiştirme kapsamı şartnamede olmadığı için bu işlemler için endpoint talep edilmemiştir.
- Self-service kayıt/signup ekranı kapsam dışıdır; kullanıcı hesaplarını yetkili sistem yöneticisi oluşturur.
