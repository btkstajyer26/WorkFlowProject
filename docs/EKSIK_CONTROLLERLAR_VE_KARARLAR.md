# Eksik Controller'lar ve Kararlar

**Tarih:** 13 Ağustos 2026
**Kaynak:** `integration/tum-feature-branchleri` × [FRONTEND_BACKEND_SOZLESMESI.md](FRONTEND_BACKEND_SOZLESMESI.md)
**Kapsam:** `auth` ve `user` modülleri **hariç** — o iş listesi tamamlandığı
için ayrı dosya artık tutulmuyor; kalan test boşlukları
[EKSIK_SINIFLAR_VE_ONCELIK.md](EKSIK_SINIFLAR_VE_ONCELIK.md) altında.

13 Ağustos'un ikinci turunda karar bekleyen maddelerin tamamı kapandı
(listeleme birleştirmesi dahil). **Backend tarafında yazılacak uç kalmadı;
geriye frontend'in yeni ucu bağlaması, bir ölü kod kalıntısı ve sızan bir
kimlik bilgisinin geçersiz kılınması kaldı.**

13 Ağustos'un üçüncü turunda `fix/workflow-flush` ve `feature/nisan-sumeyye`
entegre edildi: kayıt güncellemesi artık `saveAndFlush`, refresh ucu pasif
hesabı reddediyor, `/api/auth/change-password` artık kimlik doğrulaması
istiyor, geçersiz `sort` parametresi 500 yerine 400 dönüyor. Bu turda
Başkan Yardımcısı koltuk devri kararı **bir kez daha** değişti — bkz. 2.4.

13 Ağustos'un dördüncü turunda `feature/notification-service` yeniden entegre
edildi: e-posta gövdesi Thymeleaf şablonuna taşındı ve **kalan tek backend ucu
`GET /api/notifications` yazıldı** — bkz. 2.6. Aynı turda sözleşmenin koddan
geride kalan iki yeri (§8 koltuk devri, §6 örnek JSON) koddan doğrulanarak
düzeltildi; **artık sözleşme ile backend arasında bilinen bir sapma yok.**

---

## 1. Kalan işler

### 1.1 Frontend'de ölü kod: `RecordSearchController.ts` 🔴

**Sorumlu:** Frontend ekibi (backend'deki listeleme birleştirmesinin —
bkz. 2.2 — frontend karşılığı)

Backend'de `RecordSearchController` (`/api/records/search`) kaldırıldı,
listeleme `GET /api/records`'e taşındı (bkz. 2.2). Ama frontend hâlâ eski
uca göre üretilmiş kod taşıyor:

- `frontend/src/api/generated/RecordSearchController.ts`
- `frontend/src/api/recordSearch.ts` (facade)
- `frontend/src/mocks/api/handlers/recordSearchHandlers.ts` (MSW handler)

Şu an hiçbir sayfa veya context bu facade'ı çağırmıyor (yalnız kendi testi
ve `api/index.ts` barrel export'u referans veriyor) — yani **çalışan bir
akışı bozmuyor**, ama `VITE_API_MODE=backend` ile gerçek backend'e
bağlanıldığında bu dosyalar 404 üreten bir uç için hâlâ orada duruyor
olacak. OpenAPI yeniden üretildiğinde (`npm run api:generate`) otomatik
silinir; elle temizlenmesi de düşünülebilir.

`/api/v1` önekiyle aynı kökten geliyor: ikisi de backend'in listeleme
birleştirmesinden (§2) önce üretilmiş generated kod kalıntısı. `/api/v1`
tarafı düzeltildi (bkz. 2.5); bu, aynı kategoriden kalan tek parça.

---

## 2. Bu turlarda kapananlar

| İş | Nasıl kapandı |
|---|---|
| `/api/v1` öneki | ✅ Backend'de kaldırıldı; frontend generated client'ta bir kez düzeltilip yanlışlıkla geri alınmış, tekrar düzeltildi (bkz. 2.5) |
| CORS | ✅ `CorsConfig` — izinli origin `CORS_ALLOWED_ORIGINS`'ten, `*` yok |
| Dosya yükleme adresi | ✅ `POST /api/records/{id}/files` |
| Kayıt talebi akışı | ✅ Frontend'den kaldırıldı; kullanıcıyı Admin oluşturuyor |
| Başkan Yrd. / Başkan rol kuralları | ✅ Rol ataması `UserService`'te; koltuk devri 2.4'te — **karar iki kez değişti, sözleşme geride kaldı** |
| Çalışma notu modülü | ⛔ Kapsamdan çıktı, sonra kod da geri alındı — bkz. 2.3 |
| Kayıt geçmişi ucu | ⛔ Kapsamdan çıktı — backend'in mevcut `/api/audit-logs/record/{id}` adresi kabul edildi |
| Denetim izi cevap modeli | ✅ Karar fiilen verildi: düz alanlar (`userFullName`) korunuyor |
| **Kayıt listeleme birleştirmesi** | ✅ **Uygulandı** — bkz. 2.2 |
| **Başkan Yrd. pasifleştirme** | ✅ **Backend hizalandı** — bkz. 2.4 |
| **`GET /api/notifications`** | ✅ **Yazıldı** — bkz. 2.6 |

### 2.1 Çalışma notu modülü — koddan da temizlendi

Sözleşme §6 zaten kapsam dışı bırakmıştı: açıklama artık doğrudan workflow
isteğinin `comment` alanında gönderiliyor. Bu turda `record` ekibi
`RecordNote` modülünü (controller, DTO, entity, repository, service) kısa
süreliğine geri ekleyip hemen ardından tamamen kaldırdı — net etki sıfır,
ama artık kod da sözleşmeyle tutarlı. `record_notes` tablosu
`V6__drop_record_notes.sql` ile düşürüldü.

### 2.2 Kayıt listeleme birleştirmesi uygulandı

Önceki kararın ("uç `record`'da kalır, mantık `search`'ten gelir") uygulaması
geldi: `RecordSearchController` silindi, `RecordController.getAllRecords`
artık `RecordSearchService`'i çağırıyor.

**Ancak bu değişiklik ciddi bir güvenlik gerilemesi taşıyordu** ve merge
sırasında düzeltildi:

- `getRecordById`'deki `RecordAccessPolicy.assertCanView` çağrısı düşmüştü;
  herhangi bir rol herhangi bir kaydı görebilir hale gelmişti.
- Tipli exception'lar (`ForbiddenException`, `ResourceNotFoundException`,
  `BusinessRuleException`) düz `RuntimeException`'a geri dönmüştü.
- `createRecord`/`updateRecord`/`deleteRecord`'daki `PermissionService`
  kontrolleri düşmüştü.

`RecordServiceImpl` bu kontroller korunarak yeniden yazıldı; yalnızca
listeleme metodu kaldırıldı (artık `RecordSearchService`'te). Ayrıca
`RecordController`, `RecordSearchCriteria`'nın güncel alan adlarını
(`q`/`from`/`to`) değil eski adlarını (`setText`/`setStartDate`/
`setEndDate`) çağırıyordu — derleme hatası olurdu, düzeltildi.

Güvenlik kontrolünün geri geldiğini doğrulayan test zaten vardı
(`RecordServiceImplTest`), yani bu artık koruma altında.

### 2.3 Denetim izi cevap modeli — karar fiilen verildi

Frontend'in üretilmiş istemcisi `data-contracts.ts` içinde **`userFullName`**
kullanıyor — backend'in mevcut düz modeli benimsendi. `AuditLogResponse`
değişmeyecek.

✅ Sözleşme §6'daki örnek JSON düzeltildi (dördüncü tur): artık `actor{}`
nesnesi değil, `AuditLogResponse`'un gerçek düz alanları gösteriliyor
(`userId`, `userFullName`, `roleId`, `roleName` ve durum alanları).

### 2.4 Başkan Yardımcısı koltuk devri — karar bir kez daha değişti

Bu madde iki kez ters yöne döndü, sırasıyla:

1. **İlk hâl (sözleşmenin eski metni):** aktif yardımcı rol devredilmeden
   pasifleştirilemez, `409 DEPUTY_TRANSFER_REQUIRED`.
2. **Birinci tur:** sözleşme §8 doğrudan pasifleştirmeye izin verecek
   şekilde güncellendi, frontend buna göre değişti
   (`AdminContext.tsx`'teki engelleyici kontrol kaldırıldı), backend'deki
   `UserService.setActive` kontrolü de kaldırıldı.
3. **İkinci tur (şu anki hâl):** ekip kararı tersine döndü. Devir artık
   `changeRole` üzerinden yapılıyor — Başkan Yardımcısı başka bir tekil role
   (Başkan veya Admin) geçerken **aynı istekte** zorunlu bir
   `replacementBaskanYardimcisiId` gönderilmeli; devir aynı transaction
   içinde uygulanıyor. `setActive`'deki devirsiz-pasifleştirme engeli geri
   eklendi.

✅ **Sözleşme dördüncü turda hizalandı.** §8'e `8.1 Başkan Yardımcısı
koltuğunun devri` alt başlığı eklendi: devir isteğinin gövdesi
(`roleName` + `replacementBaskanYardimcisiId`), zorunluluk koşulu ve hata
kodları koddan doğrulanarak yazıldı. Yanlış olan iki madde de düzeltildi:

- *"Aktif Başkan Yardımcısı doğrudan pasifleştirilebilir"* — artık
  pasifleştirilemiyor, `setActive` `400 BUSINESS_RULE_VIOLATION` dönüyor.
- *"`BASKAN_YARDIMCISI` rolü verildiğinde mevcut aktif yardımcı `CALISAN`
  yapılır"* — backend böyle bir otomatik düşürme yapmıyor; tekil rol
  doluysa istek `409 ADMIN_LIMIT_EXCEEDED` ile reddediliyor. Koltuk yalnızca
  sahibi başka bir role geçerken, gösterdiği kişiye devrediliyor.

Ayrıca aynı bölümdeki hesap açma cümlesi de düzeltildi: istek `role` alanı
taşımıyor, `password` taşıyor; hesap her zaman `CALISAN` açılıyor.

### 2.5 `/api/v1` öneki — bir kez düzeltildi, yanlışlıkla geri alındı, tekrar düzeltildi

Backend'deki önek kaldırma daha önce yapılmıştı. Frontend ekibinden biri
generated client'taki karşılığını da doğru şekilde düzeltmiş (adresler,
mock handler'lar, normalize script'ine kalıcı bir koruma) ama commit hemen
ardından bir başka commit'le **açıklamasız geri alınmış**. Entegrasyon
sırasında bu fark edildi; orijinal düzeltme commit'i tekrar uygulandı.

### 2.6 `feature/notification-service` yeniden entegre edildi

Dalın kendisi hâlâ projenin ilk iskeletine (`btk.staj.WorkFlowProject.controller`,
`...service`, `...entity` düz paketleri) dayanıyordu; modül klasörlemesine geçen
ana yapıya olduğu gibi merge edilemezdi. Bu yüzden dalın *getirdiği yenilik*
mevcut `notification` modülüne taşındı:

- **E-posta gövdesi Thymeleaf şablonuna alındı.** `MailService` içindeki devasa
  `text block` yerine `templates/mail/workflow-status.html` işleniyor. Yan
  kazanç: evrak başlığı ve açıklama artık şablon motoru tarafından kaçışlı
  yazılıyor, yani kayıt başlığına HTML yazarak e-posta gövdesine kod
  sokulamıyor.
- **`GET /api/notifications` yazıldı** (1.1'deki tek kalan uç). Cevap
  `PagedResponse<NotificationResponse>`; sayfa 0'dan başlar, `size` 100'le
  sınırlanır ve sıralama istemciden alınmaz — liste her zaman en yeniden
  eskiye döner. `NotificationRepository`'deki sayfalı sorgu zaten duruyordu,
  yalnızca servis ve controller'a bağlanması gerekiyordu.

Daldaki şu parçalar **bilerek alınmadı**:

| Parça | Neden |
|---|---|
| `application.properties` | Dalda **Mailtrap API parolası düz metin commit'lenmiş**. Buradaki dosya kimlik bilgilerini ortam değişkeninden okuyor; değiştirilmedi. Sızan parola geçersiz kılınmalı. |
| `QuickApproveController` | Sözleşmede yok; kimlik doğrulaması olmayan bir `GET` ucu ve gerçekte hiçbir şey onaylamıyor — yalnızca "onaylandı" HTML'i döndürüyor. Onay akışının tek girişi `WorkflowActionController`. |
| `SecurityConfig`, `TestRunner`, `.vscode/settings.json` | Projede karşılığı olan (ve daha kapsamlı) sürümler zaten var. |
| `@CrossOrigin(origins = "*")` | CORS merkezi `CorsConfig`'ten yönetiliyor, izinli origin `CORS_ALLOWED_ORIGINS`'ten geliyor (bkz. §2 tablosu). |
| `backend/target/**` | Derleme çıktısı; dalda `.class` dosyaları commit'lenmiş. |

Dalın son commit'i şablon dosyasını **boş** bırakmıştı ve "endpoints updated per
spec" açıklamasına rağmen uç değişikliği içermiyordu; ikisi de bu turda
tamamlandı. Bildirim modülü artık `NotificationServiceTest`,
`NotificationControllerTest` ve `MailServiceTest` ile kaplı — sonuncusu şablonu
gerçek dosyadan işleyip kaçış davranışını da doğruluyor.

---

## 3. Karar bekleyenler 🔴

### 3.1 Sözleşme §12 netleştirmeleri

Sayfalama indeksi (backend `Pageable` kullanıyor → **0**), çoklu `status`
filtresi formatı, hata `code` listesinin belgelenmesi. Dosya türü/boyutu
zaten kodda hazır (`FileContentValidator`), yalnızca sözleşmeye yazılması
gerekiyor. Ertelendi.

### 3.2 Sözleşme §13 açık ürün kararları

- `records` tablosunda kullanıcıya gösterilecek benzersiz `record_no` alanı.
- `notifications.record_id` zorunlu olduğu için kayıttan bağımsız sistem
  duyuruları desteklenmiyor.

---

## 4. Öncelik

| Sıra | İş | Sorumlu | Durum |
|---|---|---|---|
| 1 | **Sızan Mailtrap parolasının geçersiz kılınması** (2.6) | Melih | 🔴 açık — yalnız Mailtrap hesabına erişimi olan yapabilir |
| 2 | `GET /api/notifications`'ın frontend'e bağlanması: OpenAPI'nin yeniden üretilmesi, MSW handler'ı ve "Tümü" görünümünün gerçek API'ye taşınması | Frontend ekibi | 🟢 açık |
| 3 | Frontend'deki ölü `RecordSearchController.ts` kalıntısının temizlenmesi | Frontend ekibi | 🟢 açık |
| 4 | §12 netleştirmeleri | Ekip | 🟡 ertelendi |
| — | ~~Sözleşme §8 koltuk devri metni (2.4)~~ | Nisan · Sümeyye | ✅ dördüncü turda yazıldı |
| — | ~~Sözleşme §6 örnek JSON'u (2.3)~~ | Ebrar | ✅ dördüncü turda düzeltildi |
