# TEST Ortamı — Dağıtım Notu

**Kapsam:** M9 TEST ortamının topolojisi, bilinen sınırlamaları ve dağıtım
öncesi/sonrası çalıştırılacak betikler. Ortam kurulduktan sonra adres, hesap ve
veri bilgisi [MOBIL_API_ENVANTERI.md](MOBIL_API_ENVANTERI.md) tablosuna yazılır.

---

## Topoloji: API-only

TEST ortamında **ürün web frontend'i yayınlanmaz.** `docker-compose.yml`
içindeki `frontend` servisi `frontend` profiline bağlıdır ve TEST birleşiminde
başlatılmaz; CI bunu her PR'da doğrular.

Dışarıya bakan tek servis Caddy'dir:

| Yol | Hedef | Koruma |
|---|---|---|
| `/api/**`, `/actuator/health` | backend:8080 | JWT |
| `/swagger-ui.html`, `/v3/api-docs` | backend:8080 | **yok** — aşağıya bakın |
| `/mail*` | mailpit:8025 | Caddy basic auth |

`db` (5432), `backend` (8080), `mailpit` (8025/1025) ve `frontend` (5173) host
portu yayınlamaz. Temel dosyada portlar zaten `127.0.0.1`'e bağlı;
`docker-compose.test.yml` bunları `!reset` ile tamamen kaldırır (Compose
`>= 2.24` gerekir).

---

## Bilinen sınırlama: e-posta derin bağlantıları çalışmaz

Backend, e-postadaki bağlantıları `app.frontend-url` üzerinden üretir
([application.properties:33](../backend/src/main/resources/application.properties#L33)).
TEST'te gerçek bir web arayüzü yayınlanmadığı için bu değer boşta kalır.

> **API adresini `FRONTEND_URL` diye tanıtmayın.** Ettiğinizde kullanıcılara
> gönderilen kayıt/parola bağlantıları HTML bekleyen bir tarayıcıyı JSON API'ye
> götürür ve akış sessizce kırılır. `deploy/preflight.sh` bu durumu engelleyici
> bulgu olarak raporlar.

**Etkilenen akış:** e-posta üzerinden gelen derin bağlantılar (kayıt linki,
parola sıfırlama linki). Bunlar TEST'te desteklenmez; kodları Mailpit
arayüzünden (`/mail`) elle okunur.

**Etkilenmeyen akış:** mobil uygulama. `EXPO_PUBLIC_API_BASE_URL` ile doğrudan
API'ye bağlanır, deep-link kullanmaz. M9 kabulü mobil üzerinden yapıldığı için
bu sınırlama M9'u bloke etmez.

**Swagger UI aynı şekilde korumasızdır.** Sözleşmeyi ekiple paylaşmayı
kolaylaştırdığı için TEST'te açık bırakılmıştır. Ortam uzun süre yaşayacaksa
Mailpit'le aynı basic auth'un arkasına alınması M9 sonrası backlog'undadır.

---

## Dağıtım öncesi: `deploy/preflight.sh`

Sunucudaki `.env` dosyasını denetler; hiçbir şeyi ayağa kaldırmaz.

```bash
./deploy/preflight.sh
```

Kontrol ettikleri: dosya izni `600`, zorunlu anahtarların doluluğu,
`JWT_SECRET` uzunluğu (≥ 32), `TEST_DOMAIN`'in IP olmaması, CORS listesinde
`https://` origin bulunması, `FRONTEND_URL`'nin API adresi olmaması ve **gizli
anahtarların hiçbirinin `.env.example` içindeki örnek değerde kalmaması.**

Yasak listesi elle tutulmaz; `.env.example` ile karşılaştırılır, böylece örnek
dosya değiştiğinde kontrol kendiliğinden güncel kalır. Çıktı maskelidir —
hiçbir değerin tamamı yazılmaz, ekip kanalına yapıştırılabilir.

Engelleyici bulgu varsa çıkış kodu `1`'dir; dağıtım yapılmaz.

---

## Veri yükleme: `deploy/seed-test-data.sh`

Rol bazlı hesapları ve altı durumun her birinden örnek kaydı **API üzerinden**
üretir (doğrudan SQL değil — bcrypt, audit satırları ve durum geçişleri ancak
servis katmanından geçince tutarlı oluşur).

### Güvenli çalıştırma

Parolaları komut satırında `VAR=... ./seed...` biçiminde **önüne yazmayın.** O
biçim hem kabuk geçmişine hem de sunucudaki herkesin okuyabildiği
`/proc/<pid>/environ` çıktısına düşer. İki güvenli yol var:

```bash
# 1) İzni kısıtlı bir dosyadan source edin
chmod 600 seed.env
set -a; . ./seed.env; set +a
./deploy/seed-test-data.sh
unset TEST_TEMP_PASSWORD TEST_USER_PASSWORD TEST_ADMIN_FINAL_PASSWORD BOOTSTRAP_ADMIN_PASSWORD
```

```bash
# 2) Hiç tanımlamayın; betik terminalden, ekrana yazmadan sorar
./deploy/seed-test-data.sh
```

### Zorunlu değişkenler

Varsayılan değerleri **yoktur**; eksikse betik ilk HTTP çağrısından önce durur.

| Değişken | Ne |
|---|---|
| `BASE` | TEST API adresi, örn. `https://ornek.duckdns.org` |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | `.env` içindeki bootstrap Admin |
| `TEST_TEMP_PASSWORD` | `createUser`'ın açtığı geçici parola |
| `TEST_USER_PASSWORD` | ekiple paylaşılacak Çalışan / Bşk. Yrd. / Başkan parolası |
| `TEST_ADMIN_FINAL_PASSWORD` | **yalnız Admin**; ekiple paylaşılmaz |

Admin parolasının ayrı olması zorunludur: test hesabı parolası ekip kanalında
dolaşır, Admin de aynı parolayı kullansaydı parolayı alan herkes kullanıcı
açıp rol değiştirebilir ve denetim kayıtlarını okuyabilirdi. Betik dördünün de
birbirinden farklı olduğunu ve backend'in parola kuralına uyduğunu ilk saniyede
doğrular.

### Temiz başlangıç ve yarım kalma

Betik idempotent **değildir**, ama korumasızca da çalışmaz: seed'in daha önce
koştuğunu görürse hiçbir değişiklik yapmadan durur. Yarım kalmış bir koşumdan
sonra `SEED_RESUME_AFTER_ADMIN=1` ile kaldığı yerden devam edilebilir; hesaplar
kısmen açılmışsa betiğin başındaki **TOPARLAMA** bölümündeki hedefli `psql`
temizliği kullanılır. `docker compose down -v` son çaredir — uploads
volume'ünü de siler.

### Çıktı

Özet bilerek **parola içermez**: yalnız e-posta, rol ve rol başına görünür
kayıt sayısı yazılır. Kanıt paketine olduğu gibi konabilir.

---

## Kurulum sonrası envantere yazılacaklar

[MOBIL_API_ENVANTERI.md](MOBIL_API_ENVANTERI.md) ortam tablosuna:

- [ ] TEST HTTPS base URL
- [ ] Hesapların e-posta + rol bilgisi (**parola yazılmaz**)
- [ ] Örnek veri özeti (rol başına görünür kayıt sayısı)
- [ ] Deploy edilen `test` merge SHA'sı
- [ ] Kabul cihazı, işletim sistemi, build kimliği ve doğrulama tarihi

Mobil tarafta `EXPO_PUBLIC_API_BASE_URL` EAS build environment'ına tam adıyla
verilir; `eas.json` bu değeri bugün kendiliğinden sağlamıyor.
`mobile/.env.example` içindeki yerel IP placeholder'ı gerçek TEST URL'si
belirlendiğinde güncellenir.
