# Frontend API ve MSW Mimarisi

## Amaç

Frontend, geliştirme sırasında gerçek backend varmış gibi HTTP istekleri gönderir. MSW yalnızca bu istekleri yakalayıp OpenAPI sözleşmesine uygun cevaplar üretir. Gerçek backend hazır olduğunda sayfalar veya API çağıran kod değişmeden yalnızca `VITE_API_MODE=backend` seçilir.

```text
UI / Context
    ↓
src/api facade
    ↓
OpenAPI'den üretilen istemci
    ↓
HTTP
    ├── VITE_API_MODE=mock    → MSW handlers
    └── VITE_API_MODE=backend → Spring Boot
```

## Tek doğruluk kaynağı kuralları

1. Backend HTTP sözleşmesinin kaynağı `/v3/api-docs` çıktısıdır.
2. `frontend/src/api/generated/` elle değiştirilmez; `npm run api:generate` ile yenilenir.
3. Authorization header yalnızca merkezi API istemcisinde eklenir. Sayfalar kendi header veya `fetch` çağrılarını yazmaz.
4. API hataları tek noktada `ApiClientError` modeline dönüştürülür.
5. MSW handler'ları üretilen istemciyle aynı endpoint sabitini paylaşmaz. Endpoint değiştiğinde entegrasyon testinin kırılması amaçlanır.
6. MSW sahte veritabanı handler'ların tek durum kaynağıdır. Handler dışında ikinci bir API state'i tutulmaz.
7. OpenAPI'de bulunmayan alanlar mevcut response modellerine eklenmez.

## Şu anda MSW ile kapsanan kesin sözleşmeler

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/categories`
- `GET /api/records`
- `GET /api/records/{id}`
- `POST /api/records`
- `PUT /api/records/{id}`
- `DELETE /api/records/{id}`
- `POST /api/records/{recordId}/workflow/actions`
- `GET /api/audit-logs/record/{recordId}`
- `POST /api/admin/users`

Handler'lar JWT yerine yalnızca test amaçlı opaque token kullanır; istemcinin `Authorization: Bearer ...` davranışı gerçeğiyle aynıdır. Şifreleme veya gerçek JWT doğrulaması frontend/MSW sorumluluğu değildir.

## UI'a bağlanan akışlar

- Giriş formu `POST /api/auth/login` çağrısını yapar.
- Başarılı girişte access token yalnızca merkezi API istemcisinin belleğine verilir; refresh token ve geçici kullanıcı özeti sürümlü `localStorage` anahtarlarında saklanır.
- Uygulama yeniden açıldığında `POST /api/auth/refresh` çağrısıyla access token yenilenir. Refresh başarısızsa kalıcı ve bellek içi oturum birlikte temizlenir.
- Çıkış onayı `POST /api/auth/logout` çağrısını başlatır ve ağ sonucu ne olursa olsun yerel oturumu kapatır.
- Kategori dropdown ve filtreleri `GET /api/categories` cevabını tek bir merkezi provider üzerinden kullanır; frontend içinde ikinci bir sabit kategori listesi tutulmaz.
- Bildirimlerin `GET /api/notifications/unread`, `GET /api/notifications/unread/count` ve `PUT /api/notifications/{id}/read` sözleşmeleri ortak MSW veritabanı üzerinden çalışır ve sahiplik kontrolünü taklit eder.
- `GET /api/records` için RBAC kapsamı, metin/durum/kategori/oluşturulma tarihi filtreleri ve sayfalama aynı MSW kayıt durumundan üretilir. API adapterı `categoryId` değerini merkezi kategori listesindeki adla eşleştirir. (Ayrı `/api/records/search` ucu ve ona ait `recordSearchHandlers` kaldırıldı; `recordSearch.ts` facade'ı artık `/api/records`'e gidiyor.)
- Token yenileme uygulama açılışına bağlı ve testlidir; henüz korumalı isteklerde otomatik 401 retry akışına bağlanmamıştır.

Login cevabında kullanıcı adı ve rolü bulunmadığından kullanıcı görünümü geçici olarak mevcut demo profiliyle e-posta üzerinden eşleştirilir. Backend kullanıcı özeti döndürdüğünde bu geçici eşleştirme kaldırılacaktır.

`localStorage` seçimi mevcut yerel geliştirme ve mobil istemciyle ortak refresh-token-body sözleşmesini kolaylaştırmak için alınmış bilinçli bir ara karardır. Web production güvenlik modeli değişirse refresh token HttpOnly cookie'ye taşınabilir; access tokenın bellek ve Authorization header davranışı değişmez.

## Bilinçli olarak UI'a bağlanmayan alanlar

Mevcut ekranların domain modeli, backend `RecordResponse` modelinden daha zengindir. Backend kayıt için `id`, `title`, `description`, `categoryId`, `status`, `createdAt`, `createdBy` ve `createdByFullName` döndürür. UI ise ayrıca kayıt numarası, atanan ve geçmiş bilgilerini kullanır.

Bu nedenle kayıt ekranlarının mevcut `WorkflowContext` state'i hemen kaldırılmamıştır. Eksik alanları local fixture ile API cevabına eklemek iki doğruluk kaynağı oluşturacağı için yasaktır. Aşağıdaki sözleşmeler tamamlandıkça ilgili domain state'i MSW/API katmanına taşınacaktır:

- kullanıcı profili: `GET /api/users/me` backend'de hazır, üretilmiş istemcisi ve
  MSW handler'ı da var; **ekranlar hâlâ demo profiliyle eşleştiriyor** — bağlanması kaldı;
- zengin kayıt detay cevabı ya da gerekli ayrı endpointler (kayıt numarası ve
  `assignedTo` detay cevabında hâlâ yok);
- ~~kayda ait dosyaları listeleme~~ **tamamlandı** — `GET /api/records/{id}/files`
  `api/files.ts` üzerinden `RecordFilesPanel`'e bağlı (detay ve düzenleme ekranı);
- ~~oluşturan kişinin adı~~ **tamamlandı** — `createdByFullName` hem liste hem
  detay cevabında geliyor; işlem geçmişinden türetme kaldırıldı;
- okunmuş bildirim geçmişini de sağlayan `GET /api/notifications` listeleme endpointi **backend'de tamamlandı**, frontend'in `listNotifications` facade'ı ve sayfalı MSW handler'ı da hazır; geriye yalnız “Tümü” görünümünün bu facade'a bağlanması kaldı.

Bildirimleri topluca okundu yapma işlemi kapsam dışıdır. Frontend “Tümü” ve “Okunmamış” görünümlerini korur. Okuma işlemi yalnızca `PUT /api/notifications/{id}/read` ile tekil olarak yapılır.

`GET /api/notifications` cevabı diğer listelerle aynı `PagedResponse` gövdesini taşır (`content`, `page`, `size`, `totalElements`, `totalPages`) ve `page`/`size` query parametrelerini kabul eder. `sort` gönderilse de yok sayılır; sıra her zaman en yeniden eskiyedir. Yani `src/api/records.ts` içindeki `Pageable` adapterının bu uçta yalnızca `page` ve `size` kısmı anlamlıdır.

## Mevcut OpenAPI açıklıkları

- Spring `Pageable`, OpenAPI'de tek `pageable` nesnesi olarak çıkıyor; gerçek HTTP query biçimi `page`, `size`, `sort`. Bu fark `src/api/records.ts` içindeki tek bir adapterda tutulur.
- Controller hata cevapları OpenAPI'de tanımlı değil; Swagger çoğunlukla yalnızca `200` gösteriyor. Ortak `ApiError` gövdesi backend kodundan modellenmiştir.
- `RecordResponse` alanları OpenAPI'de zorunlu olarak işaretlenmediği için üretilen TypeScript alanları optionaldır.
- `RecordSearchResponse`, kayıt numarası ve son işlem metni döndürmez; liste UI'ı bu alanları göstermez. Tarih filtresi backend davranışıyla uyumlu olarak `createdAt` alanını kullanır.
- `DELETE /api/records/{id}` controller kodunda `204` döndürürken Swagger `200` gösteriyor.
- Dosya upload cevabı yalnızca genel `object` olarak tanımlı ve bir kayda ait dosya listesi endpoint'i yok. Bu yüzden dosya handler'ları bu aşamada eklenmedi.

## Sonraki entegrasyon sırası

1. Backend sözleşme açıklıklarını netleştir ve OpenAPI'yi güncelle.
2. `npm run api:generate` çalıştır.
3. MSW handler ve API entegrasyon testlerini yeni sözleşmeye göre güncelle.
4. İlgili Context fonksiyonunu async API çağrısına taşı.
5. Aynı veriyi üreten eski fixture/state'i kaldır.
6. MSW ve gerçek backend modlarında aynı kullanıcı akışını doğrula.
