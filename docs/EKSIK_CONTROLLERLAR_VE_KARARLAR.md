# Eksik Controller'lar ve Kararlar

**Tarih:** 12 Ağustos 2026
**Kaynak:** `integration/tum-feature-branchleri` × [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
**Kapsam:** `auth` ve `user` modülleri **hariç** — onların uç listesi
[AUTH_USER_YAPILACAKLAR.md](AUTH_USER_YAPILACAKLAR.md) dosyasındadır ve burada
tekrarlanmaz.

12 Ağustos'ta karar bekleyen yedi maddenin altısı sonuçlandı. Bu sürüm
kararları ve kimin ne yazacağını içerir.

---

## İşaretler

| İşaret | Anlamı |
|---|---|
| 🟢 **Yeni** | Sıfırdan yazılacak |
| 🟡 **Değişiklik** | Mevcut sınıfa ekleme / adres düzeltmesi |
| ✅ **Bitti** | Bu turda uygulandı |
| 🔴 **Karar** | Hâlâ karar bekliyor |

---

## 1. Yazılacak uçlar

| # | İş | Sorumlu | İşaret |
|---|---|---|---|
| 1.1 | Çalışma notu modülü | **Alperen · Fevzi** (`record`) + **Esra · Burak** (`workflow` transaction) | 🟢 |
| 1.2 | Kayıt geçmişi ucu — adres ve model | **Ebrar** (`audit`) | 🟡 |
| 1.3 | Bildirim uçları (geçmiş bildirimler dahil) | **Melih** (`notification`) | 🟢🟡 |
| 1.4 | Dosya yükleme adresi | **Ecesu** (`attachment`) | 🟡 |
| 1.5 | Kayıt listelemenin tek uca indirilmesi | **Alperen · Fevzi** + **Irmak** | 🟡 |
| 1.6 | Başkan Yardımcısı / Başkan rol kuralları | **Nisan · Sümeyye** (`user`) | 🟢 |

---

### 1.1 Çalışma notu modülü 🟢 — *en büyük tek parça*

**Sorumlu:** Alperen · Fevzi (`record` paketi) — son maddede Esra · Burak
(`workflow`) ile ortak.

Sözleşme [§6](FRONTEND_BACKEND_SOZLESMESI.md#L288):

| Metot | Adres |
|---|---|
| `GET` | `/api/records/{id}/notes/me` |
| `PUT` | `/api/records/{id}/notes/me` |

`record_notes` tablosu `V2` migration'ında **hazır** — `(record_id, author_id)`
benzersiz kısıtı dahil. Entity, repository, service ve controller'ın **hiçbiri
yazılmamış**. Frontend'de `RecordNotesPanel` ve testleri hazır bekliyor.

Atlanmaması gereken kurallar:

- Notu yalnız kaydın **mevcut inceleme aşamasındaki atanmış kullanıcısı**
  yazabilir: `BSK_YRD_INCELEMESINDE` → atanmış Başkan Yardımcısı,
  `BASKAN_INCELEMESINDE` → atanmış Başkan. Çalışan, Admin ve geçmiş
  aşamalardaki aktörler yazamaz.
- `ONAYLANDI`, `REDDEDILDI`, `TASLAK`, `DUZENLEME_BEKLIYOR` durumlarında not
  yönetilemez.
- `PUT` yeni satır üretmez, mevcut notu günceller.
- İstemci son okuduğu `version` değerini gönderir; eşleşmezse
  **`409 NOTE_VERSION_CONFLICT`**.
- Başkasının çalışma notunu okuyan uç **bulunmaz**.
- Gövde boş olamaz, en fazla 1000 karakter.
- **Workflow işlemi başarılı olduğunda not aynı transaction içinde temizlenir;
  işlem başarısızsa not korunur.** → `WorkflowActionService`'in transaction
  sınırına adım eklenecek. Bu madde **Esra · Burak** ile ortak.

### 1.2 Kayıt geçmişi ucu 🟡

**Sorumlu:** Ebrar (`audit`)

Sözleşme `GET /api/records/{id}/history` istiyor; mevcut `AuditLogController`
`GET /api/audit-logs/record/{recordId}` sunuyor. Cevap modeli de farklı —
sözleşme aktörü iç içe nesne olarak istiyor, mevcut DTO düz alan taşıyor
(ayrıntı ve karar için bkz. §2.1).

### 1.3 Bildirim uçları 🟢🟡

**Sorumlu:** Melih (`notification`)

| Uç | Amaç | Durum |
|---|---|---|
| `GET /api/notifications?page=0&size=20` | **Geçmiş bildirimler** — okunmuş dahil tüm bildirimler, sayfalı | 🟢 **yeni gereksinim** |
| `GET /api/notifications?unread=true` | Yalnız okunmamışlar (aynı uç, parametreyle) | 🟡 |
| `PATCH /api/notifications/read-all` | Tümünü okundu işaretle | 🟢 |
| `PATCH /api/notifications/{id}/read` | Tek bildirimi okundu işaretle | 🟡 şu an `PUT` |
| `GET /api/notifications/unread/count` | Rozet sayacı | ✅ var, sözleşmeye eklenecek |

**Geçmiş bildirimler arayüzde gösterilecek.** Şu an yalnız
`GET /api/notifications/unread` var ve sayfasız; okunmuş bildirimlere hiç
erişilemiyor. Sözleşmedeki tek uç (`GET /api/notifications`) `unread`
parametresiyle her iki durumu da karşılamalı, sayfalama zorunlu — bildirimler
kullanıcı ömrü boyunca birikir.

Cevap modeli sözleşme §10'a göre: `id`, `userId`, `recordId`, `message`,
`isRead`, `createdAt`.

### 1.4 Dosya yükleme adresi 🟡

**Sorumlu:** Ecesu (`attachment`)

`POST /api/files/upload?recordId=` → **`POST /api/records/{id}/files`**.
İndirme, önizleme ve silme adresleri sözleşmeyle uyumlu, dokunulmayacak.

### 1.5 Kayıt listeleme tek uca inecek 🟡

**Sorumlu:** Alperen · Fevzi (`record`) + Irmak (`search`) — ortak

Karar ve gerekçe §2.2'de.

### 1.6 Başkan Yardımcısı / Başkan rol kuralları 🟢

**Sorumlu:** Nisan · Sümeyye (`user`)

Karar ve gerekçe §2.3'te.

---

## 2. Verilen kararlar

### 2.1 Denetim izi cevap modeli 🔴 *(tek açık madde)*

Sözleşme aktörü iç içe nesne olarak istiyor, mevcut DTO düz alan taşıyor:

```jsonc
// Sözleşmenin beklediği
{ "id": "...", "action": "BASKANA_ILET",
  "actor": { "id": "...", "firstName": "Ayşe", "lastName": "Kaya",
             "role": "BASKAN_YARDIMCISI" },
  "comment": "...", "createdAt": "..." }

// Mevcut AuditLogResponse
{ "id": "...", "userId": "...", "userFullName": "Ayşe Kaya",
  "roleId": 2, "roleName": "BASKAN_YARDIMCISI", ... }
```

İki seçenek:

1. **Backend uyum sağlar** — `AuditLogResponse` `actor` nesnesine sarılır.
   Repository'deki JPQL projeksiyonu (`SELECT new ...`) ve `AuditLogServiceTest`
   etkilenir. Ad/soyad şu an `CONCAT` ile birleşik geliyor, ayrılması gerekir.
2. **Sözleşme uyum sağlar** — frontend düz alanları okur. `AdminLogsPage` ve
   kayıt geçmişi bileşeni güncellenir.

İkisi de savunulabilir; frontend ekibinin hangi biçimi bekleyerek ekranı
kurduğuna bakılmalı. **Ebrar ve frontend ekibi birlikte karar verecek.**

### 2.2 Kayıt listeleme — uç `record`, mantık `search` ✅ *karar verildi*

**Karar:** Listeleme ucu `RecordController` altında `GET /api/records` olarak
kalacak; filtreleme ve kapsam mantığı `search` modülünden gelecek.
`RecordSearchController` kaldırılacak.

**Gerekçe:** Proje akışında listeleme CRUD'un doğal parçası — kullanıcı
"kayıtlarım" ekranını açtığında ayrı bir arama ucuna gitmiyor, aynı kaynağı
filtreleyerek okuyor. Sözleşme §5 de tek uç tanımlıyor
(`GET /api/records?page&size&status&categoryId&q&from&to&sort`) ve ayrı bir
arama adresi öngörmüyor.

Asıl belirleyici olan şu: görünürlük kapsamı şu anda **iki yerde ayrı ayrı**
uygulanıyor — `RecordServiceImpl` listeleme sorgusuna rol bazlı predicate
ekliyor, `RecordSearchService` de kendi kapsamını `RecordAccessPolicy` ile
hizalıyor. Aynı güvenlik kuralının iki kopyası demek, biri değişince diğerinin
unutulması demek. Tek uçta birleşince kural tek yerde kalır.

`search` tarafı daha zengin (`from`, `to`, dinamik filtre) ve kapsamı
`RecordAccessPolicy` ile zaten hizalı; bu yüzden **mantık `search`'ten,
adres `record`'dan** gelecek. `RecordController.getFilteredRecords`
`RecordSearchService`'i çağıracak, kendi predicate'lerini bırakacak.

Sözleşmedeki `from`, `to` ve `sort` parametreleri hiçbir tarafta tam
karşılanmıyor; birleştirme sırasında tamamlanacak.

### 2.3 Başkan Yardımcısı ve Başkan rol kuralları ✅ *karar verildi*

**Karar — iki kural:**

1. Sistemde aktif bir Başkan Yardımcısı varken **ikincisi atanamaz**. Aynısı
   Başkan için de geçerli.
2. Başkan Yardımcısı veya Başkan rolündeki kullanıcı **başka bir role
   çevrilemez** — Çalışan'a geri dönemez. Görevden alma, rolü değiştirerek
   değil **hesabı pasife çekerek** yapılır.

Görev devri iki adım olur: önce mevcut kişi pasifleştirilir, sonra yeni kişiye
rol atanır.

**Mantık nerede duracak:** `UserService.changeRole` içinde. Ayrı bir
`DeputyTransferService`'e gerek yok — sözleşmedeki "eski yardımcıyı otomatik
Çalışan yap" devir mantığı bu kararla **iptal oldu**; geriye iki basit kısıt
kaldı ve ikisi de rol değişiminin kendi kuralı. `PATCH /users/{id}/active`
ucu ise pasifleştirmeyi yapacak (henüz yazılmadı, AUTH_USER listesinde).

> **Sözleşme güncellenmeli:** [§8](FRONTEND_BACKEND_SOZLESMESI.md#L370) şu an
> *"`BASKAN_YARDIMCISI` rolü verildiğinde mevcut aktif yardımcı `CALISAN`,
> hedef kullanıcı `BASKAN_YARDIMCISI` yapılır"* ve
> *"`409 DEPUTY_TRANSFER_REQUIRED`"* diyor. Bu davranış artık geçerli değil.

**Neden önemli — düzeltme:** Bu belgenin ilk sürümünde "ikinci aktif Başkan
Yardımcısı onay akışını kilitler" yazıyordu; bu **yanlıştı**.
`TargetUserResolver`'a bakıldığında:

```java
case GONDER, TEKRAR_GONDER -> resolveRequestedTarget(requestedTargetUserId);
case BASKANA_ILET          -> resolveSingleActiveRole(RoleName.BASKAN);
```

Çalışan → Başkan Yardımcısı adımında hedef **istekten** geliyor, yani birden
fazla yardımcı akışı teknik olarak kilitlemez. Kilitleyen rol **Başkan**:
`resolveSingleActiveRole` `activeUsers.size() != 1` olduğunda
`RoleNotConfigured` döner, bu da `WORKFLOW_ROLE_NOT_CONFIGURED` → **500**
demektir.

Yani 1. kural Başkan Yardımcısı için bir **iş kuralı**, Başkan için
**teknik zorunluluk**. Sıfır aktif Başkan da aynı hatayı verir — bu yüzden
görevden alma ile yeni atama arasındaki boşlukta "Başkana ilet" işlemi
çalışmaz. İşletme kabulü gerekiyor; devir aynı oturumda tamamlanmalı.

### 2.4 Kayıt talebi akışı ✅ *karar verildi*

**Karar:** Frontend'deki öz-kayıt/başvuru akışı kaldırılacak; kullanıcıyı
yalnızca Admin oluşturacak. Backend'e başvuru modülü eklenmeyecek.

Frontend ekibine verilecek ayrıntılı doküman:
[FRONTEND_KULLANICI_OLUSTURMA_AKISI.md](FRONTEND_KULLANICI_OLUSTURMA_AKISI.md)
— kaldırılacak dosyalar, yerine gelen akış, uç sözleşmeleri ve hata kodları.

### 2.5 `/api/v1` öneki ✅ *uygulandı*

Önek kaldırıldı: `RecordController` ve `CategoryController` artık
`/api/records` ve `/api/categories` altında. `AuthorizationMatrixTest`
güncellendi, 274 test geçiyor.

Önek zaten backend'in kendi içinde tutarsızdı — `WorkflowActionApi`
`/api/records/{recordId}/workflow/actions` kullanıyordu, yani aynı kaynak iki
farklı önek altında sunuluyordu.

### 2.6 Sözleşme §12 netleştirmeleri — *ertelendi*

Sayfalama indeksi, çoklu `status` formatı, dosya adedi sınırı ve hata `code`
listesi sonraki turda ele alınacak. CORS ayrıca yazılıyor
([EKSIK_SINIFLAR_VE_ONCELIK.md](EKSIK_SINIFLAR_VE_ONCELIK.md) — *Hacer*).

### 2.7 Sözleşme §13 açık ürün kararları 🔴

- `records` tablosunda kullanıcıya gösterilecek benzersiz `record_no` alanı
  kesinleşmedi. Eklenecekse migration gerekir.
- `notifications.record_id` zorunlu olduğu için kayıttan bağımsız sistem
  duyuruları desteklenmiyor. Gerekiyorsa şema değişmeli — 1.3'teki geçmiş
  bildirim ekranı bunu görünür kılacak.

---

## 3. Adres değişiklikleri özeti

| Şu anki adres | Yeni adres | Sorumlu | Durum |
|---|---|---|---|
| `/api/v1/records` | `/api/records` | — | ✅ yapıldı |
| `/api/v1/categories` | `/api/categories` | — | ✅ yapıldı |
| `POST /api/files/upload?recordId=` | `POST /api/records/{id}/files` | Ecesu | 🟡 |
| `PUT /api/notifications/{id}/read` | `PATCH .../read` | Melih | 🟡 |
| `GET /api/audit-logs/record/{id}` | `GET /api/records/{id}/history` | Ebrar | 🟡 |
| `GET /api/records/search` | `GET /api/records` içinde birleşecek | Alperen · Fevzi · Irmak | 🟡 |

---

## 4. Öncelik

| Sıra | İş | Sorumlu | Gerekçe |
|---|---|---|---|
| 1 | CORS | Hacer | Bu olmadan frontend hiçbir uca bağlanamaz |
| 2 | Frontend başvuru akışının kaldırılması | Frontend ekibi | Karar verildi, iki taraf ayrışmasın |
| 3 | Çalışma notu modülü (1.1) | Alperen · Fevzi · Esra · Burak | En büyük parça; frontend'i ve workflow transaction'ını birden etkiliyor |
| 4 | Geçmiş bildirim ucu (1.3) | Melih | Arayüzde gösterilecek, şu an erişilemiyor |
| 5 | Rol kuralları (1.6) | Nisan · Sümeyye | Başkan tarafı onay akışını 500'e düşürebiliyor |
| 6 | Listeleme birleştirme (1.5) | Alperen · Fevzi · Irmak | Güvenlik kuralının iki kopyası sürdürülemez |
| 7 | Adres düzeltmeleri (1.2 · 1.4) | Ebrar · Ecesu | 2.1 kararı çıktıktan sonra mekanik iş |
