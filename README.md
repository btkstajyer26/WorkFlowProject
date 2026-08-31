# İş Akışı ve Onay Yönetim Sistemi

Kurum içi kayıtların oluşturulması, rol bazlı inceleme/onay akışından geçirilmesi, dosyalanması ve denetlenmesi için geliştirilmiş web ve mobil uygulamadır. İş kuralları Spring Boot backend'inde uygulanır; React ve Expo istemcileri aynı REST sözleşmesini kullanır.

## Mimari görünüm

```text
React web ─┐
           ├── REST / OpenAPI ── Spring Boot ── PostgreSQL
Expo mobil ┘                         ├── dosya deposu
                                    ├── SMTP / Mailpit
                                    └── FCM push
```

| Katman | Teknoloji |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring Security, JPA, Flyway |
| Web | React, TypeScript, Vite, TanStack Query |
| Mobil | React Native, Expo Router, TypeScript |
| Veri ve yerel servisler | PostgreSQL 15, Docker Compose, Mailpit |

Workflow geçişleri merkezi bir statik tablodan gelir. `StaticTransitionRuleSource`, bu tabloyu `TransitionRuleSource` sınırının arkasında hem doğrulayıcıya hem yetki servisine sunar. İstemciler hedef durumu hesaplamaz.

## Hızlı başlangıç

Gereksinimler: Docker Compose 2.24 veya üzeri. Yerel geliştirme için ayrıca Java 21, Maven ve Node.js 22.13 veya üzeri gerekir.

```bash
cp .env.example .env
docker compose up --build -d
```

Servisler:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Mailpit: `http://localhost:8025`

Web uygulamasını da Compose ile başlatmak için:

```bash
docker compose --profile frontend up --build -d
```

Web arayüzü `http://localhost:5173` adresinde açılır. Ortam değişkenlerinin güncel listesi ve açıklamaları [`.env.example`](.env.example) dosyasındadır.

Paketleri ayrı ayrı çalıştırmak için ilgili rehberi kullanın:

- [Frontend geliştirme](frontend/README.md)
- [Mobil geliştirme](mobile/README.md)
- [Frontend E2E kurulumu](frontend/e2e/README.md)

## Kalite komutları

Backend doğrulaması PostgreSQL gerektirir:

```bash
docker compose up -d db
cd backend
mvn verify
```

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm run typecheck:e2e
npm run build
npm test
```

Mobil:

```bash
cd mobile
npm ci
npm run lint
npm run typecheck
npm test -- --runInBand
npx expo export --platform web
```

## Kanonik belgeler

| Belge | Kapsam |
| --- | --- |
| [Sistem mimarisi](docs/architecture.md) | Modül sınırları, port/adapter yapısı ve topoloji |
| [Workflow](docs/workflow.md) | Durumlar, geçişler, yetki, audit ve bildirim davranışı |
| [Veritabanı](docs/database.md) | Şema ve Flyway yönetimi |
| [Frontend–backend sözleşmesi](docs/FRONTEND_BACKEND_SOZLESMESI.md) | Web istemcisinin dayandığı alan ve hata sözleşmeleri |
| [Mobil API envanteri](docs/MOBIL_API_ENVANTERI.md) | Mobil istemcinin kullandığı güncel REST sözleşmesi |
| [OpenAPI anlık görüntüsü](docs/openapi.json) | Kod incelemesi için sürümlenmiş API şeması |
| [TEST ortamı notu](docs/TEST_ORTAMI_NOTU.md) | Güncel topoloji, dağıtım ve operasyon yönergeleri |
| [Mimari kararlar](docs/decisions/README.md) | ADR dizini |

Tarihsel belgeler aktif gereksinim kaynağı değildir:

- [Backend açık işler ve görev dağılımı](docs/archive/BACKEND_ACIK_ISLER_VE_GOREV_DAGILIMI.md)
- [Eksik controllerlar ve kararlar](docs/archive/EKSIK_CONTROLLERLAR_VE_KARARLAR.md)
- [Eksik sınıflar ve öncelik](docs/archive/EKSIK_SINIFLAR_VE_ONCELIK.md)
- [Mobil entegrasyon görev dağılımı](docs/archive/MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md)
- [M9 TEST kabul kanıtı](docs/archive/M9_TEST_KABUL_KANITI.md)
