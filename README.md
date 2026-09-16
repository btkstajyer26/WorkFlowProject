# İş Akışı ve Onay Yönetim Sistemi

Kurum içi kayıtların oluşturulması, rol bazlı inceleme/onay akışından geçirilmesi, dosyalanması ve denetlenmesi için geliştirilmiş web ve mobil uygulamadır. İş kuralları Spring Boot backend'inde uygulanır; React ve Expo istemcileri aynı REST sözleşmesini kullanır.

## Mimari görünüm

```text
React web ─┬── REST / OpenAPI ─┐
           └── STOMP /ws ──────┼── Spring Boot ── PostgreSQL
Expo mobil ───── REST ─────────┘       ├── dosya deposu
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

16 Eylül 2026, `feature/nt-realtime-notifications` dalında üç realtime commit'i
(`202cbf0`, `885fbfc`, `963f0b4`) ve korunmuş Paket 1–4 çalışma ağacı
incelenmiştir; uzak CI ve `main` durumu ayrıca doğrulanmalıdır:

- Dinamik roller tanımlı geçiş, permission ve kayıt ilişkisiyle workflow aksiyonu alabilir. `RECORD_VIEW` ile oluşturdukları veya doğrudan atandıkları kayıtları okuyabilir.
- **Departman runtime'ı V23 ile uygulanmıştır:** `DEPARTMANA_GONDER` aksiyonu, `DEPARTMENT` hedef stratejisi, `DepartmentRoutingResolver`/`DepartmentRoutingAdapter` ve `DepartmentVisibilityAdapter` çalışır durumdadır. Şema `V1`–`V24`'tür.
- **AP-2 rol yönetim ekranı bu dalda mevcuttur** (`frontend/src/pages/admin/RolesPage.tsx`). Rol CRUD backend uçları ve kullanımdaki rolün korunması hazırdır.
- **WF-8 backend servisi hazırdır** fakat HTTP adapter'ı yoktur: `workflow` altındaki tek yönetim ucu `POST /api/workflow/rules/reload`'dur. `AP-8` açıktır.
- **Yönetim HTTP katmanı eksiktir:** `AP-3` (permission matrisi), `AP-4` (departman/üyelik) ve `AP-5` (routing) için controller bulunmaz; Admin bu nesneleri panelden yönetemez.
- **NT-5 departman fan-out'u hazırdır:** uygun departman üyeleri ortak routing/eligibility çözümüyle bulunur, aktör çıkarılır ve alıcı kimlikleri tekilleştirilir.
- **Web realtime kanalı hazırdır:** `/ws`, STOMP CONNECT Bearer doğrulaması,
  `/user/queue/notifications`, commit-sonrası `NotificationResponse` yayını,
  `@stomp/stompjs` reconnect ve query invalidation uygulanmıştır. 30 saniyelik
  REST polling fallback'i korunur.
- **Mobil B09/MOB-1 hazırdır:** dinamik rol kimliği, backend kaynaklı
  available-actions/target-departments, `assignment.kind`/`version` tüketimi ve
  kayıt detayında atama gösterimi uygulanmıştır.
- **Android push lifecycle kodu hazırdır:** registration, token renewal,
  foreground/background/cold-start ve notification tap → `recordId` yolları
  testlidir; fiziksel cihaz kabulü bekler.
- Grafik düzenleme, workflow definition/versioning ve draft/publish Workflow V2 kapsamındadır; V1 eksiği sayılmaz.

**Davranış problemleri:** 4 Eylül 2026 tarihli inceleme, çalıştırılmış
regresyon problarıyla sekiz backend davranış ihlali (B01–B08) ve beş istemci /
sözleşme boşluğu (B09–B13) doğrulamıştır. Bu dalda **dokuzu kapanmıştır** — `B01`,
`B02`, `B04`, `B05`, `B07`, `B08`, `B09`, `B11`, `B13` — ve kod/test kanıtıyla
sabitlenmiştir. **Açık kalan dört bulgu:** `B03` (görev devrinde sürüm artışı),
`B06` (görünür içerikle arama), `B10` (web aksiyon paneli), `B12` (atama audit'i).
10 Eylül Workflow V1
teslim tanımı bugün hâlâ karşılanmamaktadır. Sekiz backend probunun koşum sonuçları
ve tekrar üretim adımları [kanıt klasöründedir](docs/reviews/2026-09-04/TEKRAR_URETIM.md).

Son kayıtlı backend `verify`: **831 test, 0 failure/error/skipped; JAR üretildi**
(7 Eylül 2026, yerel, ayrı `b04_b08_test` veritabanına karşı). Bu sayı rev.4'teki
816 testin üzerine bu turda eklenen 15 regresyon testini içerir. Frontend, mobil ve
Playwright **bu turda çalıştırılmadı**; 126/126, 64/64 ve 15/15 önceki turların
tarihli sonuçlarıdır. Yeşil sonuç açık kalan dört bulguyu kapatmaz; CI, TEST deploy
veya ürün kabulü bu sonuçtan çıkarılmaz. Kaynaklar ve devam bağımlılıkları
[dokümantasyon dizinindedir](docs/README.md).

Paket 5 dar doğrulamasında (16 Eylül 2026) notification/realtime, STOMP
güvenliği, departman alıcısı, mail-action servisleri ve push servisi için **57
backend testi** geçti. Reconnect gerçek browser kabulü PASS durumundadır; izole
polling fallback ve Android fiziksel cihaz kabulü otomatik test sonucu sayılmaz; güncel ayrım
[D04 rehberinde](docs/D04_NOTIFICATION_MOBILE_REALTIME_KABUL_REHBERI.md) ve
[final handoff'ta](docs/reviews/2026-09-16-bahadir-final-handoff.md) kayıtlıdır.

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
| [D04 notification/mobile/realtime kabul rehberi](docs/D04_NOTIFICATION_MOBILE_REALTIME_KABUL_REHBERI.md) | Yerel çalışma, browser, Mailpit, MOB-1 ve fiziksel Android kabul adımları |
| [Bahadır final handoff](docs/reviews/2026-09-16-bahadir-final-handoff.md) | B01/B09/MOB-1/NT/ADR/D04 kanıt matrisi ve kalan manuel kabuller |
| [Mimari kararlar](docs/decisions/README.md) | ADR dizini |

Tarihsel belgeler aktif gereksinim kaynağı değildir:

- [Backend açık işler ve görev dağılımı](docs/archive/BACKEND_ACIK_ISLER_VE_GOREV_DAGILIMI.md)
- [Eksik controllerlar ve kararlar](docs/archive/EKSIK_CONTROLLERLAR_VE_KARARLAR.md)
- [Eksik sınıflar ve öncelik](docs/archive/EKSIK_SINIFLAR_VE_ONCELIK.md)
- [Mobil entegrasyon görev dağılımı](docs/archive/MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md)
- [M9 TEST kabul kanıtı](docs/archive/M9_TEST_KABUL_KANITI.md)
