# TEST Ortamı — Dağıtım ve Operasyon Notu

Bu belge TEST ortamının güncel topolojisini, dağıtım kontrollerini ve işletim yönergelerini tanımlar. Tarihli dağıtım, hesap, seed ve cihaz kabul kanıtları [M9 arşiv belgesinde](archive/M9_TEST_KABUL_KANITI.md) korunur.

Bu topoloji repo yapılandırmasını anlatır; çalışan TEST sunucusunun son commit'i
ve `flyway_schema_history` sürümü bu dokümantasyon turunda doğrulanmadı. Repo
tarafındaki 776 testlik yerel backend kabulü (`c9b0297`) TEST'e deploy veya
departman ürün kabulü sayılmaz. [Teslim durumu](README.md).

> **Açık karar — web barındırma.** Aşağıdaki API-only topoloji ile Workflow V1'in
> web ve mail kabulü çelişir: `deploy/Caddyfile` bütün ürün yollarını backend'e
> gönderdiği için `/hizli-islem` sayfası bu ortamda çalışmaz ve NT-7 mail üzerinden
> işlem kabulü TEST'te gösterilemez. V1 teslimi için erişilebilir bir frontend
> adresi ve deploy sorumluluğu karara bağlanmalıdır.

TEST ortamı tek sunucuda Docker Compose ile backend, PostgreSQL, Mailpit ve Caddy çalıştırır. Sunucuya özgü Elastic IP, SSH anahtarı ve bölge bilgileri repository dışında ortam sahibinde tutulur.

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

Backend e-posta bağlantılarını `FRONTEND_URL` üzerinden üretir. TEST'te ürün web arayüzü bulunmadığı için e-posta kayıt/parola bağlantıları ve `/hizli-islem#token=...` sayfası desteklenmez; kodlar Mailpit arayüzünden elle okunur.

`FRONTEND_URL` alanına API adresi yazılmamalıdır. Aksi hâlde tarayıcı bağlantıları HTML arayüzü yerine JSON API'ye gider. `deploy/preflight.sh` bunu engelleyici bulgu olarak raporlar. Mobil istemci `EXPO_PUBLIC_API_BASE_URL` ile doğrudan API'ye bağlandığı için bu sınırlamadan etkilenmez.

## Dağıtım öncesi kontrol

Sunucudaki `.env` dosyasını hiçbir servisi başlatmadan denetleyin:

```bash
./deploy/preflight.sh
```

Betik dosya iznini (`600`), zorunlu değerleri, JWT anahtarı uzunluğunu, alan adı/CORS ayarlarını, `FRONTEND_URL` sınırını ve örnek sırların değiştirilmiş olmasını denetler. Çıktı sırları maskeler; engelleyici bulguda kod `1` ile çıkar.

## Test verisi yükleme

Repo **V23** migration'ına kadar olan zinciri içerir. Dağıtım öncesinde hedef
ortamın `flyway_schema_history` sürümü ve departman verisi incelenmelidir. V22
kendine-parent verisi bulursa tamamen geri alınır; otomatik veri düzeltmez.
Paylaşılmış V18–V21 dosyaları değiştirilmez.
[V22 yükseltme davranışı](database.md#v22-yükseltme-ve-geri-alma-davranışı).

V18–V22 temel departman şemasıdır; V23 `DEPARTMENT` hedef stratejisini,
`DEPARTMANA_GONDER` aksiyonunu ve iki geçişi ekler (toplam 10 geçiş). V23 tek
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
