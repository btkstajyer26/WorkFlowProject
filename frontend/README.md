# EBYS Frontend

İş Akışı ve Onay Yönetim Sistemi için geliştirilen React tabanlı frontend uygulamasıdır.

## Teknolojiler

- React 19 ve TypeScript
- Vite ve Tailwind CSS
- React Router
- React Hook Form ve Zod
- Axios tabanlı, OpenAPI'den üretilen API istemcisi
- MSW ile geliştirme ve test API simülasyonu
- Vitest ve React Testing Library
- Lucide React ve Oxlint

## Yerel geliştirme

Node.js 22 veya üzeri gereklidir.

```bash
npm ci
npm run dev
```

Varsayılan geliştirme modu MSW kullanır. Yerel `.env` dosyası şu şekilde oluşturulabilir:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_API_MODE=mock
```

Gerçek Spring Boot backend'ine bağlanmak için:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_API_MODE=backend
```

`VITE_API_BASE_URL` yalnızca sunucu kökünü taşır. Endpoint yolları zaten `/api/...` ile başladığı için değerin sonuna `/api` eklenmez.

## OpenAPI istemcisini güncelleme

Backend `http://localhost:8080` üzerinde çalışırken:

```bash
npm run api:generate
```

Bu komut `src/api/generated/` altındaki dosyaları `/v3/api-docs` sözleşmesinden üretir ve Vite için yalnızca tip olan importları otomatik olarak normalize eder. Üretilen dosyalar elle değiştirilmez. Uygulamaya özel hata dönüşümü, token ekleme ve uyumluluk adapterleri `src/api/` altında ayrı tutulur.

## Kontroller

```bash
npm run lint
npm test
npm run build
```

## Klasör yapısı

```text
src/
├── api/            # OpenAPI istemcisi, token ve hata sınırı
├── components/     # Ortak layout ve kayıt bileşenleri
├── config/         # Rol bazlı navigasyon ayarları
├── context/        # Henüz API sözleşmesi tamamlanmayan UI state'i
├── domain/         # Saf iş akışı ve görünürlük kuralları
├── mocks/          # UI fixture'ları ve MSW sahte API sunucusu
├── pages/          # Route seviyesindeki ekranlar
├── schemas/        # Form doğrulama şemaları
├── test/           # Ortak Vitest/MSW kurulumu
└── types/          # Uygulama domain tipleri
```

API ve MSW sınırları [frontend API mock mimarisi](../docs/FRONTEND_API_MOCK_MIMARISI.md) belgesinde, beklenen backend kararları ise [frontend-backend sözleşmesinde](../docs/FRONTEND_BACKEND_SOZLESMESI.md) tutulur.
