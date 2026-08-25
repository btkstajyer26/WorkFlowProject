# Frontend API ve MSW Test Mimarisi

## Çalışma zamanı

Frontend geliştirme ve production ortamlarında yalnızca gerçek Spring Boot API'sine bağlanır. Sunucu adresi `VITE_API_BASE_URL` ile belirlenir; uygulamada mock/backend çalışma modu veya rol değiştiren demo paneli bulunmaz.

```text
UI → src/api facade → OpenAPI istemcisi → Spring Boot
```

## Test ortamı

MSW yalnızca Vitest kurulumu tarafından Node ortamında başlatılır. Testlerin gerçek veritabanı veya çalışan bir backend gerektirmeden HTTP isteklerini ve hata senaryolarını doğrulamasını sağlar. `src/mocks/` altındaki fixture, handler ve sahte veritabanı production giriş noktası tarafından import edilmez ve Vite uygulama paketine dahil edilmez.

```text
Vitest → UI veya API facade → HTTP isteği → MSW Node test sunucusu
```

## Kurallar

1. Backend HTTP sözleşmesinin kaynağı `/v3/api-docs` çıktısıdır.
2. `frontend/src/api/generated/` elle değiştirilmez; `npm run api:generate` ile yenilenir.
3. Sayfalar kendi `fetch` veya authorization header uygulamasını yazmaz; merkezi API katmanını kullanır.
4. API hataları `ApiClientError` modeline dönüştürülür.
5. Runtime kodu `src/mocks/` altından import yapamaz.
6. MSW handler'ları yalnızca test senaryolarının ihtiyaç duyduğu sözleşmeyi taklit eder; frontend için alternatif çalışma modu değildir.
7. Endpoint veya DTO değiştiğinde üretilen istemci, adapterlar ve ilgili MSW test handler'ları birlikte güncellenir.

## Kontrol komutları

```bash
npm run lint
npm test
npm run build
```

Bu ayrım sayesinde tarayıcıda görülen veri daima backend ve veritabanından gelir; sahte veriler yalnızca otomatik testlerin sınırları içinde kalır.
