# EBYS Frontend

İş Akışı ve Onay Yönetim Sistemi için geliştirilen React tabanlı frontend uygulamasıdır.

## Teknolojiler

- React 19 ve TypeScript
- Vite ve Tailwind CSS
- React Router
- React Hook Form ve Zod
- Axios tabanlı, OpenAPI'den üretilen API istemcisi
- MSW ile izole API entegrasyon testleri
- Vitest ve React Testing Library
- Lucide React ve Oxlint

## Yerel geliştirme

Node.js 22 veya üzeri gereklidir.

```bash
npm ci
npm run dev
```

Yerel `.env` dosyası şu şekilde oluşturulabilir:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Uygulama geliştirme ve production sırasında her zaman gerçek Spring Boot backend'ine bağlanır. `VITE_API_BASE_URL` yalnızca sunucu kökünü taşır. Endpoint yolları zaten `/api/...` ile başladığı için değerin sonuna `/api` eklenmez. MSW yalnızca Vitest test ortamında başlatılır.

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
├── context/        # Uygulama çapındaki UI ve önbellek koordinasyonu
├── mocks/          # Yalnızca Vitest için fixture ve MSW test sunucusu
├── pages/          # Route seviyesindeki ekranlar
├── schemas/        # Form doğrulama şemaları
├── test/           # Ortak Vitest/MSW kurulumu
└── types/          # Uygulama domain tipleri
```

Test API sınırları [frontend API ve MSW mimarisi](../docs/FRONTEND_API_MOCK_MIMARISI.md) belgesinde, beklenen backend kararları ise [frontend-backend sözleşmesinde](../docs/FRONTEND_BACKEND_SOZLESMESI.md) tutulur.
