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

Workflow geçişleri `workflow_transitions` tablosundan okunur. `ReloadableTransitionRuleSource`, doğrulanmış kuralları `TransitionRuleSource` sınırının arkasında sunar. Her workflow işlemi başlangıçta tek snapshot alır. Kurallar `POST /api/workflow/rules/reload` ile veya WF-8 rol bağlama servisi üzerinden başarılı değişiklik sonrasında yeniden başlatmadan yenilenir. İstemciler hedef durumu hesaplamaz.

## Mevcut teslim durumu

4 Eylül 2026, `test` @ `3eb3691` (PR #66) tabanında:

- Dinamik roller tanımlı geçiş, permission ve kayıt ilişkisiyle workflow aksiyonu alabilir. `RECORD_VIEW` ile oluşturdukları veya doğrudan atandıkları kayıtları okuyabilir.
- **WF-8 backend servisi hazırdır:** mevcut geçişe dinamik aktör rolü bağlar/pasifleştirir. Admin API/UI entegrasyonu (`AP-8`) açıktır.
- **Departman veri katmanı V18–V22 ile hazırdır:** departman, üyelik, routing, kayıt ataması ve ilgili şema korumaları bulunur. Departmana gönderim, yetki çözümleme, görünürlük ve bildirim bağlantısı henüz uygulanmadı.
- Sıradaki teslim WF-5/WF-6 runtime'ı, DB-13'ün kalan gönderim migration'ı ve WF-2C2/DB-8 departman görünürlüğüdür. Grafik düzenleme/versioning Workflow V2 kapsamındadır.

Son kayıtlı backend `verify`: **712 test, 0 failure/error/skipped; JAR üretildi**
(4 Eylül 12:37 TRT). Test edilen dosya ağacı PR #66 sonrası tabanla aynıdır;
CI/TEST ortamı veya departman ürün kabulü bu sonuçtan çıkarılmaz. Kaynaklar ve
devam bağımlılıkları [dokümantasyon dizinindedir](docs/README.md).

## Hızlı başlangıç

Gereksinimler: Docker ve Docker Compose. Yerel geliştirme için ayrıca Java 21 ve Node.js 22.13 veya üzeri gerekir; Maven'ı ayrıca kurmanız gerekmez, wrapper (`backend/mvnw`) repoda gelir. TEST dağıtımı `!reset` kullandığı için orada Compose 2.24 veya üzeri şarttır (bkz. [TEST ortamı notu](docs/TEST_ORTAMI_NOTU.md)).

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

Backend doğrulaması PostgreSQL gerektirir. Önce Compose dosyasındaki DB servisini
ve kullandığınız override'ları inceleyip çalışan konteyneri/host portunu doğrulayın:

```bash
docker compose ps db
docker compose port db 5432
```

DB çalışmıyorsa `docker compose up -d db` ile başlatıp aynı kontrolleri tekrarlayın.
DB'nin sağlıklı olması gerekir. Maven `.env` dosyasını okumaz; doğruladığınız portu
ortam değişkeniyle verin. Aşağıdaki `5433` son yerel kabuldeki örnektir,
her kurulum için sabit değildir.

Git Bash / Linux / macOS:

```bash
cd backend
DB_PORT=5433 ./mvnw verify
```

PowerShell:

```powershell
$env:DB_PORT = '5433'
Set-Location backend
.\mvnw.cmd verify
```

Veritabanı testleri için ayrı test veritabanı/şeması kullanımı ve V22 yükseltme
koşulları [veritabanı belgesinde](docs/database.md#v22-yükseltme-ve-geri-alma-davranışı)
açıklanır.

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
| [Dokümantasyon dizini ve teslim sınırları](docs/README.md) | Güncel kanıt, hazır/açık işler ve WF-5/WF-6 öncesi bağımlılıklar |
| [Sistem mimarisi](docs/architecture.md) | Modül sınırları, port/adapter yapısı ve topoloji |
| [Workflow](docs/workflow.md) | Durumlar, geçişler, yetki, audit ve bildirim davranışı |
| [Veritabanı](docs/database.md) | Şema ve Flyway yönetimi |
| [DB-1 veri modeli sözleşmesi](docs/DB_1_VERI_MODELI_SOZLESMESI.md) | Bağlayıcı şema, atama ve kapasite kuralları |
| [WF-2C2 / DB-8 görünürlük](docs/WF2C2_DB8_GORUNURLUK_SOZLESMESI.md) | Mevcut ortak policy/SQL scope ve açık departman entegrasyonu |
| [WF-8 / AP-8 aktör rolü bağlama](docs/WF8_AP8_AKTOR_ROL_BAGLAMA_SOZLESMESI.md) | Hazır Java servisi, transaction ve Admin API/UI sözleşmesi |
| [Frontend–backend sözleşmesi](docs/FRONTEND_BACKEND_SOZLESMESI.md) | Web istemcisinin dayandığı alan ve hata sözleşmeleri |
| [Mobil API envanteri](docs/MOBIL_API_ENVANTERI.md) | Mobil istemcinin kullandığı güncel REST sözleşmesi |
| [OpenAPI anlık görüntüsü](docs/openapi.json) | Kod incelemesi için sürümlenmiş API şeması. **Elle bakımlıdır:** canlı şema `localhost:8080/v3/api-docs` adresinde; uç eklendiğinde ilgili bölüm bu dosyaya mevcut biçim korunarak işlenir. Dosyayı toptan yeniden üretmeyin — biçimlendirme ve `servers.url` ortama göre değişip gereksiz diff üretir. Frontend istemcisi ayrı üretilir (`cd frontend && npm run api:generate`) |
| [TEST ortamı notu](docs/TEST_ORTAMI_NOTU.md) | Güncel topoloji, dağıtım ve operasyon yönergeleri |
| [Mimari kararlar](docs/decisions/README.md) | ADR dizini |

Tarihsel belgeler aktif gereksinim kaynağı değildir:

- [Backend açık işler ve görev dağılımı](docs/archive/BACKEND_ACIK_ISLER_VE_GOREV_DAGILIMI.md)
- [Eksik controllerlar ve kararlar](docs/archive/EKSIK_CONTROLLERLAR_VE_KARARLAR.md)
- [Eksik sınıflar ve öncelik](docs/archive/EKSIK_SINIFLAR_VE_ONCELIK.md)
- [Mobil entegrasyon görev dağılımı](docs/archive/MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md)
- [M9 TEST kabul kanıtı](docs/archive/M9_TEST_KABUL_KANITI.md)
