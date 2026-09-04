# EBYS Frontend

İş Akışı ve Onay Yönetim Sistemi'nin React ve TypeScript tabanlı web istemcisidir.

## Yerel geliştirme

Node.js 22.13 veya üzeri gereklidir.

```bash
npm ci
npm run dev
```

Yerel `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Değer sunucu köküdür; endpoint yolları zaten `/api/...` ile başladığı için sonuna `/api` eklenmez.

## API ve test sınırları

Çalışma zamanı her zaman gerçek Spring Boot API'sine bağlanır:

```text
UI → src/api facade → üretilmiş OpenAPI istemcisi → Spring Boot
```

MSW yalnızca Vitest'in Node test ortamında başlatılır. `src/mocks/` içindeki fixture ve handler'lar runtime giriş noktasından import edilmez ve uygulama paketine alınmaz.

Kurallar:

1. HTTP sözleşmesinin kaynağı backend'in `/v3/api-docs` çıktısıdır.
2. `src/api/generated/` elle değiştirilmez.
3. Sayfalar kendi `fetch` veya authorization header uygulamasını yazmaz; merkezi API katmanını kullanır.
4. API hataları `ApiClientError` modeline dönüştürülür.
5. Runtime kodu `src/mocks/` altından import yapamaz.
6. MSW alternatif bir uygulama modu değil, yalnız test sunucusudur.
7. Endpoint veya DTO değiştiğinde üretilen istemci, adapterlar ve ilgili MSW handler'ları birlikte güncellenir.

Beklenen backend davranışları [frontend–backend sözleşmesinde](../docs/FRONTEND_BACKEND_SOZLESMESI.md) tutulur.

## OpenAPI istemcisini güncelleme

Backend `http://localhost:8080` üzerinde çalışırken:

```bash
npm run api:generate
```

Komut `src/api/generated/` içeriğini canlı sözleşmeden üretir ve Vite için tip importlarını normalize eder. [`docs/openapi.json`](../docs/openapi.json) inceleme amaçlı anlık görüntüdür; üretim girdisi değildir. Uygulamaya özel hata dönüşümü, token ekleme ve uyumluluk adapterleri `src/api/` altında tutulur.

## Kontroller

```bash
npm run lint
npm run typecheck:e2e
npm run build
npm test
```

Playwright testleri gerçek backend ve izole test veritabanıyla çalışır:

```bash
npm run test:e2e
```

Kurulum için [E2E rehberine](./e2e/README.md) bakın.

## Klasör yapısı

```text
src/
├── api/            OpenAPI istemcisi, token ve hata sınırı
├── auth/           Oturum durumu ve token saklama
├── components/     Ortak layout ve kayıt bileşenleri
├── config/         Rol bazlı navigasyon
├── context/        Uygulama çapındaki UI ve önbellek koordinasyonu
├── hooks/          Bildirim merkezi ve paylaşılan ekran hook'ları
├── mocks/          Yalnız Vitest fixture ve handler'ları
├── pages/          Route ekranları
├── query/          TanStack Query istemcisi ve sorgu anahtarları
├── schemas/        Form doğrulama şemaları
├── test/           Ortak Vitest/MSW kurulumu
└── types/          Uygulama domain tipleri
```
