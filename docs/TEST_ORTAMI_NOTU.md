# TEST Ortamı — Dağıtım ve Operasyon Notu

Bu belge TEST ortamının güncel topolojisini, dağıtım kontrollerini ve işletim yönergelerini tanımlar. Tarihli dağıtım, hesap, seed ve cihaz kabul kanıtları [M9 arşiv belgesinde](archive/M9_TEST_KABUL_KANITI.md) korunur.

Bu topoloji repo yapılandırmasını anlatır. **7 Eylül 2026 itibarıyla çalışan bir TEST
sunucusu yoktur** (aşağıdaki karara bakınız). Repo tarafındaki 831 testlik yerel
backend kabulü (`8adcf21`) deploy veya ürün kabulü sayılmaz.
[Teslim durumu](README.md).

> **Karar (7 Eylül 2026) — TEST web barındırma ertelendi.** Çalışan bir TEST
> sunucusu ve alan adı bulunmuyor; M9 kabulünün yapıldığı ortam kullanılmıyor ve
> genişletme kararı alınmadı. Bu nedenle aşağıdaki **API-only topoloji korunur** ve
> Workflow V1'in web/mail kabulü **yerel ortamda** yapılır (bkz. *Yerel kabul yolu*).
> Karar sahibi Burak; gerekçesi ve bedeli görev dağılımı belgesinin kapsam kararı
> bölümünde kayıtlıdır.
>
> **Sunucu sağlandığı gün yapılacak iş** (yeniden keşfedilmesin diye):
> 1. Frontend için statik build imajı (`npm run build` çıktısını servis eden aşama).
> 2. `docker-compose.test.yml`'e host portu yayınlamayan bir servis.
> 3. `deploy/Caddyfile`'da yol bölmesi: `/api*`, `/ws*`, `/actuator*`, `/swagger-ui*`,
>    `/v3/api-docs*` backend'de kalır; `/mail*` Mailpit'te kalır; geri kalan her şey
>    web'e gider ve bilinmeyen yollarda `index.html` döner (SPA fallback — istemci
>    tarafı `/hizli-islem`, `/kayitlar/:id` gibi rotalar bunsuz 404 verir).
> 4. `deploy/preflight.sh`'daki `FRONTEND_URL == TEST_DOMAIN` engelinin tersine
>    çevrilmesi — aynı adres artık beklenen değerdir.
> 5. `.github/workflows/ci.yml`'deki `has("frontend")` kontrolünün güncellenmesi;
>    yeni servis host portu yayınlamamalı, yoksa "yalnız Caddy yayınlar" kontrolü düşer.

TEST ortamı, ayağa kaldırıldığında tek sunucuda Docker Compose ile backend, PostgreSQL, Mailpit ve Caddy çalıştıracak biçimde yapılandırılmıştır; aşağıdaki topoloji bu yapılandırmayı anlatır, şu an çalışan bir kurulumu değil. Sunucuya özgü Elastic IP, SSH anahtarı ve bölge bilgileri repository dışında ortam sahibinde tutulur.

## Topoloji: API-only

TEST ortamında ürün web frontend'i yayınlanmaz. `docker-compose.yml` içindeki `frontend` servisi profil arkasındadır ve TEST birleşiminde başlatılmaz.

| Dış yol | Hedef | Koruma |
| --- | --- | --- |
| `/api/**` | backend:8080 | JWT; `/api/auth/**` kimlik uçları (giriş, token yenileme, çıkış, parola sıfırlama) ve mail-action uçları public |
| `/api/public/mail-actions/preview`, `/consume` | backend:8080 | Süreli, tek kullanımlık token |
| `/actuator/health` | backend:8080 | Ayrıntısız sağlık cevabı |
| `/swagger-ui.html`, `/v3/api-docs` | backend:8080 | Public; kalıcı ortamda korunmalı |
| `/mail*` | mailpit:8025 | Caddy basic auth |

Birleştirilmiş TEST yapılandırması `db`, `backend` ve `mailpit` host portlarını kaldırır; `frontend` zaten `profiles` arkasında olduğu için TEST birleşiminde hiç oluşmaz. dışarıya yalnız Caddy'nin `80/443` portları açılır. Temel Compose dosyasında backend mobil LAN geliştirmesi için `0.0.0.0:8080` yayınladığından TEST'te `docker-compose.test.yml` mutlaka kullanılmalıdır. `!reset` desteği için Docker Compose 2.24 veya üzeri gerekir.

## E-posta derin bağlantısı sınırlaması

Backend derin bağlantıları `FRONTEND_URL` üzerinden üretir. Bu yalnız **evrak durum
değişikliği bildirimi** için geçerlidir: `MailService.render` `deepLink`'i
`FRONTEND_URL + "/records/{id}"`, `quickActionLink`'i
`FRONTEND_URL + "/hizli-islem#token=..."` olarak kurar. **Parola sıfırlama maili
bağlantı içermez** — yalnız doğrulama kodu taşır ve kod arayüzdeki forma elle girilir;
7 Eylül'de yerel Mailpit üzerinde doğrulandı (gövdede hiçbir `http` adresi yok).
Dolayısıyla web arayüzü yayınlanmasa bile parola akışı çalışır; kırılan yalnız evrak
bildirimindeki iki derin bağlantıdır.

`FRONTEND_URL` alanına API adresi yazılmamalıdır; API-only topoloji korunduğu sürece bu
kural geçerlidir ve `deploy/preflight.sh` bunu engelleyici bulgu olarak raporlar. Mobil
istemci `EXPO_PUBLIC_API_BASE_URL` ile doğrudan API'ye bağlandığı için bu sınırlamadan
etkilenmez.

## Yerel kabul yolu

TEST sunucusu bulunmadığı için Workflow V1'in web ve mail kabulü geliştirici
makinesinde yapılır. Ürün web arayüzü temel Compose dosyasında **profil arkasındadır**;
backend'in `FRONTEND_URL` varsayılanı zaten `http://localhost:5173`'tür ve
`CORS_ALLOWED_ORIGINS` varsayılanı bu adresi içerir — yani yerelde ek yapılandırma
gerekmez.

```bash
docker compose up -d --build backend      # --build zorunlu; bkz. bayat imaj tuzağı
docker compose --profile frontend up -d
```

Aşağıdaki kontroller **7 Eylül 2026'da `8adcf21` üzerinde çalıştırıldı**:

| Kontrol | Sonuç |
| --- | --- |
| `GET :8080/actuator/health` | `{"status":"UP"}` |
| `http://localhost:5173/giris` | Giriş ekranı render edildi |
| `http://localhost:5173/hizli-islem` (token'sız) | Sayfa açıldı; beklenen "Bağlantı eksik veya bozuk görünüyor" durumunu gösterdi |
| Tarayıcıdan `:5173` → `:8080/api/categories` | `401` — CORS zinciri çalışıyor, backend kimliksiz isteği reddediyor |
| `POST /api/auth/forgot-password` → Mailpit | `202`; mail `http://localhost:8025` kutusuna düştü |

**Bu koşumda doğrulanmayanlar:** giriş, workflow aksiyonu ve evrak bildirimi mailindeki
iki derin bağlantı (`/records/{id}` ve `/hizli-islem#token=...`). Bunlar hesap parolası
gerektirir. Ayrıca hızlı işlem düğmesi `B01` kapanmadan üretilmediği için mail → işlem
zinciri bugün uçtan uca gösterilemez; sayfanın kendisinin çalışıyor olması bu zincirin
kabulü değildir. Yerel hesaplar: `calisan@local.test`, `byardimci@local.test`,
`baskan@local.test`, `admin@local.test`.

### Bayat imaj tuzağı

`V24` uygulanmış bir veritabanına **eski backend imajı** bağlanırsa uygulama açılışta
düşer ve konteyner yeniden başlatma döngüsüne girer:

```
workflow-backend  Restarting (1)
Caused by: targetStrategy PREVIOUS_ACTOR requires expectedTargetRoleId
```

7 Eylül 2026'da yerel ortamda gerçekleşti: imaj 3 Eylül'de üretilmişti (`V24` ve
`c0e08d7` öncesi), veritabanında ise `flyway_schema_history` **24**'ü gösteriyordu.
Eski kodun invariant'ı `PREVIOUS_ACTOR` satırında `expected_target_role_id` beklerken
`V24` o kolonu boşaltmıştır. Kod hatası değildir; `docker compose up -d --build backend`
ile imaj yenilendiğinde servis sağlıklı hâle gelir.

Kural: **migration uygulanmış bir ortamda `--build` olmadan `up` yapmayın.** Aynı sebeple
`V24` ve kod değişikliği tek teslimde dağıtılır; ayrı dağıtılırsa uygulama açılmaz.

## Dağıtım öncesi kontrol

Sunucudaki `.env` dosyasını hiçbir servisi başlatmadan denetleyin:

```bash
./deploy/preflight.sh
```

Betik dosya iznini (`600`), zorunlu değerleri, JWT anahtarı uzunluğunu, alan adı/CORS ayarlarını, `FRONTEND_URL` sınırını ve örnek sırların değiştirilmiş olmasını denetler. Çıktı sırları maskeler; engelleyici bulguda kod `1` ile çıkar.

## Test verisi yükleme

Repo **V24** migration'ına kadar olan zinciri içerir. Dağıtım öncesinde hedef
ortamın `flyway_schema_history` sürümü ve departman verisi incelenmelidir. V22
kendine-parent verisi bulursa tamamen geri alınır; otomatik veri düzeltmez.
Paylaşılmış V18–V21 dosyaları değiştirilmez.
[V22 yükseltme davranışı](database.md#v22-yükseltme-ve-geri-alma-davranışı).

V18–V22 temel departman şemasıdır; V23 `DEPARTMENT` hedef stratejisini,
`DEPARTMANA_GONDER` aksiyonunu ve iki geçişi ekler (toplam 10 geçiş). V24
`expected_target_role_id` kolonunu yalnız `ROLE` stratejisine daraltır; geçiş
sayısını değiştirmez ve kod değişikliğiyle birlikte dağıtılmalıdır. V23 tek
başına eski bir backend üzerine dağıtılmaz; WF-5/WF-6 runtime'ı ile birlikte
gider. Mevcut seed betiği departman gönderim kabulünü hâlâ kanıtlamaz: departman,
üyelik ve routing için yönetim ucu bulunmadığından bu veriler TEST'te yalnız SQL
ile oluşturulabilir. Departman kabul senaryosu ancak `AP-4`/`AP-5` uçlarıyla
uçtan uca gösterilebilir.

`deploy/seed-test-data.sh`, rol bazlı hesapları ve workflow örneklerini SQL yerine API üzerinden üretir; böylece parola hash'leri, audit ve geçişler uygulama kurallarıyla uyumlu kalır.

Parolaları komut satırının önüne `VAR=...` biçiminde yazmayın. İzni `600` olan bir dosyadan yükleyin veya betiğin gizli terminal istemini kullanın:

```bash
chmod 600 seed.env
set -a; . ./seed.env; set +a
./deploy/seed-test-data.sh
unset TEST_TEMP_PASSWORD TEST_USER_PASSWORD TEST_ADMIN_FINAL_PASSWORD BOOTSTRAP_ADMIN_PASSWORD
```

Zorunlu değişkenler:

| Değişken | Amaç |
| --- | --- |
| `BASE` | TEST API adresi |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | Ortamın bootstrap Admin'i |
| `TEST_TEMP_PASSWORD` | Kullanıcı oluşturma geçici parolası |
| `TEST_USER_PASSWORD` | Çalışan, Başkan Yardımcısı ve Başkan test parolası |
| `TEST_ADMIN_FINAL_PASSWORD` | Yalnız Admin için ayrı parola |

Betik idempotent değildir; önceki seed'i görürse değişiklik yapmadan durur. Yarım kalan koşum `SEED_RESUME_AFTER_ADMIN=1` ile sürdürülebilir. Hedefli toparlama adımları betiğin başındadır; `docker compose down -v` uploads verisini de sildiği için son çaredir.

## Mobil yapılandırma

`EXPO_PUBLIC_API_BASE_URL` EAS build environment'ına tam adıyla verilir. Yerel IP değeri yalnız geliştiricinin commit edilmeyen `mobile/.env` dosyasında tutulur.

## Bilinen operasyonel eksikler

- Yedekleme ve geri yükleme prosedürü
- Reboot dayanıklılığı doğrulaması
- İzleme ve alarm kuralları
- Log saklama politikası
- Image sürümü sabitleme
- Secret rotasyonu
- Bulut kaynaklarını kapatma prosedürü
- Swagger/OpenAPI yüzeyini kimlik doğrulama arkasına alma
