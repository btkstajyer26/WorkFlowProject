# EBYS Frontend

İş Akışı ve Onay Yönetim Sistemi için geliştirilen React tabanlı frontend uygulamasıdır. Uygulama, Spring Boot API tamamlanana kadar mock verilerle çalışır.

## Teknolojiler

- React 19 ve TypeScript
- Vite
- Tailwind CSS
- React Router
- React Hook Form ve Zod
- Vitest ve React Testing Library
- Lucide React
- Oxlint

## Yerel geliştirme

Node.js 22 veya üzeri önerilir.

```bash
npm ci
npm run dev
```

Varsayılan backend adresi `.env.example` dosyasında gösterilir. Yerel ayar için `.env` oluşturulabilir:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## Kontroller

```bash
npm run lint
npm test
npm run build
```

## Klasör yapısı

```text
src/
├── components/    # Ortak layout ve kayıt bileşenleri
├── config/        # Rol bazlı navigasyon ayarları
├── context/       # Backend gelene kadar mock uygulama state'i
├── domain/        # Saf iş akışı ve yetkilendirme kuralları
├── mocks/         # Backend gelene kadar kullanılan örnek veriler
├── pages/         # Route seviyesindeki ekranlar
├── schemas/       # Form doğrulama şemaları
├── test/          # Ortak test kurulumu
└── types/         # Uygulama veri tipleri
docs/
└── FRONTEND_BACKEND_SOZLESMESI.md
```

## Geliştirme akışı

Özellikler `feature/*` branch'lerinde geliştirilir. Değişiklikler anlamlı ve küçük commit'lere ayrılır; doğrudan stabil branch'e push yapılmaz.

Bu entegrasyon çalışmasının branch'i:

```text
feature/frontend-vite-entegrasyonu
```

Backend ile birleştirme, frontend doğrulamaları tamamlandıktan sonra `WorkFlowProject` reposunun `test` branch'inden açılacak ayrı bir feature branch üzerinden yapılacaktır.

API entegrasyonunda beklenen endpointler, JWT akışı, filtreler ve hata formatı [frontend-backend sözleşmesinde](docs/FRONTEND_BACKEND_SOZLESMESI.md) tutulur.
