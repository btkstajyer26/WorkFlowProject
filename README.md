# İş Akışı ve Onay Yönetim Sistemi

Kurum içindeki belge, kayıt ve onay süreçlerini dijitalleştirmeyi amaçlayan web tabanlı bir EBYS modülüdür.

> Backend şu anda altyapı ve modül iskeleti aşamasındadır.
> React kullanıcı arayüzü çalışır durumdadır; işlevsel REST API'ler ve güvenlik entegrasyonu tamamlandığında mock veri katmanı gerçek API istemcileriyle değiştirilecektir.

## Mevcut Durum

| Bileşen | Durum |
| --- | --- |
| Java 21 ve Spring Boot 4.1 backend iskeleti | Hazır |
| Spring Data JPA, PostgreSQL ve Flyway altyapısı | Hazır |
| Docker Compose, PostgreSQL 15 ve Mailpit | Hazır |
| PostgreSQL healthcheck ve kalıcı DB/upload volume'leri | Hazır |
| Backend modül paketleri | Hazır, henüz boş |
| Spring Security tabanlı kimlik doğrulama ve rol bazlı yetkilendirme | Altyapı mevcut; yöntem için ekip kararı bekleniyor |
| Admin kontrollü kullanıcı oluşturma ve rol atama | Hedef kapsam |
| Kayıt, iş akışı, dosya, audit ve bildirim API'leri | Hedef kapsam |
| Outlook e-posta entegrasyonu | Hedef kapsam |
| React frontend | Vite tabanlı uygulama, rol ekranları ve frontend testleri hazır; backend API entegrasyonu bekleniyor |

## Faz I Hedefi

- Spring Security tabanlı kimlik doğrulama ve rol bazlı erişim kontrolü
- Admin tarafından kullanıcı hesabı oluşturma ve rol atama
- Yeni kullanıcıların varsayılan olarak Çalışan rolüyle başlatılması
- Kayıt oluşturma, taslak kaydetme, düzenleme ve silme
- Hiyerarşik onay akışı ve kontrollü durum geçişleri
- Birden fazla dosya yükleme, indirme ve tarayıcıda önizleme
- Durum, kullanıcı, kategori, tarih ve metne göre arama/filtreleme
- Kayıt bazlı detaylı işlem geçmişini gösteren Audit Log paneli
- Silinemez, append-only işlem geçmişi ve denetim izi
- Gönderme, iletme, geri gönderme, onay ve ret işlemlerinde uygulama içi ve Outlook e-posta bildirimleri
- E-postadan ilgili kayda yönlendiren deep link desteği

## Faz II Hedefi
- Android / iOS Mobil Uygulama (React Native / Flutter / Native).
- Mobil Push Notification alımı ve mobil cihaz üzerinden hızlı onay/red mekanizmaları.

## Roller ve Yetkiler

Sistemde üç iş akışı rolü ve bir yönetim rolü bulunur:

| Rol | Görünürlük ve işlemler |
| --- | --- |
| **Çalışan** | Varsayılan kullanıcı rolüdür. Yalnız kendi kayıtlarını görür; kayıt ve ek oluşturur, kendi taslağını düzenler/siler, Başkan Yardımcısına gönderir, geri dönen kaydı düzenleyip yeniden gönderir. |
| **Başkan Yardımcısı** | Atanan veya kendisine gelen kayıtları görür; not ekler, zorunlu açıklamayla Çalışana geri gönderir veya uygun bulduğu kayda ara kademe onayı vererek Başkana iletir. Nihai onay/ret yapamaz. |
| **Başkan** | Başkan incelemesine ulaşan kayıtları görür; not ekler, onaylar, reddeder veya Çalışana/Başkan Yardımcısına geri gönderir. |
| **Admin** | Kullanıcı hesabı oluşturur ve kullanıcıların rollerini kurumsal görevlendirmeye göre değiştirir. Onay akışına kendiliğinden katılmaz ve yalnızca Admin olması nedeniyle evraklara erişim kazanmaz. |

Rol yönetimi kuralları:

- İlk Admin bootstrap istisnası dışında her kullanıcı hesabı Admin tarafından daima **Çalışan** rolüyle oluşturulur. Kullanıcı oluşturma isteğinde farklı bir başlangıç rolü seçilemez.
- Başkan Yardımcısı, Başkan ve Admin rolleri yalnız hesap oluşturulduktan sonra, ayrı ve audit kaydı üreten bir Admin işlemiyle atanır.
- Güncel v2 veritabanı taslağı `users.role_id` ile tek aktif rol öngörür. Bir kullanıcının aynı anda birden fazla rol taşıyıp taşımayacağı ayrıca karara bağlanacaktır; gerekirse model `user_roles` benzeri çoklu rol ilişkisine dönüştürülür.
- İlk Admin yalnızca sistemde Admin yoksa ve `BOOTSTRAP_ADMIN_EMAIL` ile `BOOTSTRAP_ADMIN_PASSWORD` açıkça verilmişse oluşturulur. Varsayılan parola yasaktır; parola hash'lenir, ilk girişte değişim zorlanır ve bootstrap bilgileri deployment yapılandırmasından kaldırılır.
- Son aktif Admin'in rolünün kaldırılması veya hesabının devre dışı bırakılması engellenmelidir.

## Kayıt Yaşam Döngüsü

```text
TASLAK -> BSK_YRD_INCELEMESINDE -> BASKAN_INCELEMESINDE -> ONAYLANDI
```

| Durum | Anlamı |
| --- | --- |
| `TASLAK` | Çalışanın oluşturduğu, henüz onaya göndermediği kayıt |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısının incelemesini bekleyen kayıt |
| `BASKAN_INCELEMESINDE` | Başkanın nihai kararını bekleyen kayıt |
| `DUZENLEME_BEKLIYOR` | Eksik veya hatalı bulunarak düzenleme için geri gönderilen kayıt |
| `ONAYLANDI` | Başkan tarafından onaylanan ve kilitlenen kayıt |
| `REDDEDILDI` | Başkan tarafından reddedilen ve süreci sonlanan kayıt |

Temel kurallar:

- Çalışana geri gönderme işlemleri açıklamasız yapılamaz.
- Nihai ret yetkisi yalnızca Başkana aittir.
- `ONAYLANDI` ve `REDDEDILDI` durumundaki kayıtlar değiştirilemez.
- Çalışanın düzelterek yeniden gönderdiği kayıt, geri gönderen makamdan bağımsız olarak hiyerarşik kontrol için yeniden Başkan Yardımcısı incelemesine gider.

| Mevcut durum | Yetkili işlem | Hedef durum | Atama / yeniden gönderim |
| --- | --- | --- | --- |
| `TASLAK` | Çalışan gönderir | `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısına atanır |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısı Çalışana geri gönderir | `DUZENLEME_BEKLIYOR` | Kaydı oluşturan Çalışana atanır |
| `DUZENLEME_BEKLIYOR` | Çalışan yeniden gönderir | `BSK_YRD_INCELEMESINDE` | İş akışındaki Başkan Yardımcısına atanır |
| `BSK_YRD_INCELEMESINDE` | Başkan Yardımcısı Başkana iletir | `BASKAN_INCELEMESINDE` | Başkana atanır |
| `BASKAN_INCELEMESINDE` | Başkan Çalışana geri gönderir | `DUZENLEME_BEKLIYOR` | Kaydı oluşturan Çalışana atanır |
| `BASKAN_INCELEMESINDE` | Başkan Başkan Yardımcısına geri gönderir | `BSK_YRD_INCELEMESINDE` | Kaydı Başkana ileten Başkan Yardımcısına atanır |
| `BASKAN_INCELEMESINDE` | Başkan onaylar | `ONAYLANDI` | Süreç tamamlanır ve kayıt kilitlenir |
| `BASKAN_INCELEMESINDE` | Başkan reddeder | `REDDEDILDI` | Süreç sonlanır ve kayıt kilitlenir |

## Dosya, Audit ve Bildirim Hedefleri

- Desteklenen dosyalar: `.pdf`, `.docx`, `.doc`, `.xlsx`, `.xls`, `.png`, `.jpeg`, `.jpg`.
- Dosyalar GUID tabanlı adla saklanmalı; boyut, uzantı ve gerçek MIME type doğrulanmalıdır.
- Kayıt oluşturma, taslak düzenleme/silme, dosya ekleme/kaldırma, durum değişiklikleri, notlar, ara kademe onayı, nihai onay, ret ve geri gönderme işlemleri kullanıcı/rol/zaman bilgisiyle append-only `audit_logs` kaydı üretmelidir.
- Audit Log kayıtları uygulama üzerinden değiştirilememeli veya silinememeli; kayıt detayında kronolojik işlem geçmişi olarak görüntülenebilmelidir.
- Gönderme ve iletmede ilgili yetkiliye; geri göndermede ilgili çalışan/yöneticiye; onay ve rette tüm ilgililere uygulama içi ve e-posta bildirimi gönderilmelidir.
- E-posta içeriği kayıt özeti, mevcut durum, son not ve kayıt detayına deep link içermelidir.

## Teknik Altyapı

| Katman | Teknoloji | Durum |
| --- | --- | --- |
| Backend | Java 21, Spring Boot 4.1.0 | Mevcut |
| REST | Spring Web MVC | Altyapı mevcut |
| Veritabanı | PostgreSQL 15 | Mevcut ve ekip kararıyla sabit |
| Persistence | Hibernate, Spring Data JPA | Mevcut |
| Migration | Flyway | Mevcut |
| Güvenlik | Spring Security | Altyapı mevcut; kimlik doğrulama yaklaşımı için ekip kararı bekleniyor |
| E-posta | Spring Mail, Outlook SMTP/Exchange | Hedef |
| Yerel e-posta testi | Mailpit | Mevcut |
| Frontend | React, TypeScript, Vite | Uygulama ve frontend testleri mevcut; backend API entegrasyonu bekleniyor |
| Test | JUnit 5, Spring Test, Mockito | Temel test altyapısı mevcut |

## Proje Yapısı

```text
WorkFlowProject/
├── .env.example
├── .gitignore
├── README.md
├── docker-compose.yml
├── backend/
│   ├── .dockerignore
│   ├── Dockerfile
│   ├── HELP.md
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/btk/staj/WorkFlowProject/
│       │   │   ├── WorkFlowProjectApplication.java
│       │   │   ├── attachment/
│       │   │   ├── audit/
│       │   │   ├── auth/
│       │   │   ├── common/
│       │   │   ├── notification/
│       │   │   ├── rbac/
│       │   │   ├── record/
│       │   │   ├── search/
│       │   │   ├── user/
│       │   │   └── workflow/
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── db/migration/V1__init.sql
│       │       └── templates/mail/
│       └── test/java/btk/staj/WorkFlowProject/
│           ├── WorkFlowProjectApplicationTests.java
│           └── {attachment,audit,auth,common,notification,rbac,record,search,user,workflow}/
├── frontend/
│   ├── .dockerignore
│   ├── Dockerfile
│   ├── package.json
│   ├── index.html
│   ├── public/
│   └── src/
└── docs/
    ├── architecture.md
    ├── database.md
    ├── workflow.md
    └── decisions/
        └── README.md
```

Modül ve test klasörlerindeki `.gitkeep` dosyaları okunabilirlik için gösterilmemiştir. Frontend dizini çalıştırılabilir React/Vite uygulamasını içerir.

## Backend Modülleri

| Modül | Hedef sorumluluk |
| --- | --- |
| `auth` | Giriş, çıkış ve seçilecek kimlik doğrulama yöntemine ait token/oturum işlemleri |
| `user` | Admin kontrollü kullanıcı oluşturma, varsayılan Çalışan rolü ve rol atama işlemleri |
| `rbac` | Rol, kayıt görünürlüğü ve işlem yetkisi kuralları |
| `record` | Kayıt, kategori ve taslak yönetimi |
| `workflow` | Durum geçişleri ve onay akışı |
| `attachment` | Dosya doğrulama, saklama ve erişim |
| `audit` | Denetim izi ve işlem geçmişi |
| `search` | Arama, filtreleme ve sayfalama |
| `notification` | Uygulama içi ve e-posta bildirimleri |
| `common` | Ortak yapılandırma, DTO ve hata tipleri |

Paketler şu anda çoğunlukla `.gitkeep` içeren klasör iskeletleridir.

## Veritabanı Yönetimi

Migration dosyaları `backend/src/main/resources/db/migration` altında tutulur. Şema Flyway tarafından yönetilir; Hibernate yalnızca entity/tablo uyumunu doğrular.

Referans ER modeli `roles`, `users`, `records`, `statuses`, `categories`, `files`, `notifications` ve `audit_logs` tablolarından oluşur. `tokens` tablosunun gerekip gerekmediği seçilecek kimlik doğrulama yaklaşımına göre kesinleştirilecektir.

`roles` tablosunda Çalışan, Başkan Yardımcısı, Başkan ve Admin rolleri bulunmalıdır. İlk Admin bootstrap işlemi dışında her yeni hesap önce Çalışan olarak oluşturulmalı; farklı bir rol ancak daha sonra ayrı bir Admin işlemiyle atanmalıdır.

Mevcut `V1__init.sql` yalnızca farklı alan ve kimlik tiplerine sahip `app_user` tablosunu oluşturur; hedef şema henüz Flyway'e aktarılmamıştır.

Uygulanmış migration dosyaları değiştirilmemeli, her şema değişikliği için yeni ve sıralı migration eklenmelidir. V1 ortak bir veritabanına hiç uygulanmadıysa birleştirmeden önce kanonik şemayla hizalanabilir; uygulandıysa dönüşüm yeni bir migration ile yapılmalıdır.

## Docker ile Hızlı Başlangıç

Gereksinimler: Git, Docker Desktop ve Docker Compose.

```powershell
git clone https://github.com/btkstajyer26/WorkFlowProject.git
Set-Location WorkFlowProject
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
```

Linux/macOS ortamında `.env` dosyası `cp .env.example .env` komutuyla oluşturulabilir.

| Servis | Varsayılan adres |
| --- | --- |
| Backend | `http://localhost:8080` |
| PostgreSQL | `localhost:5432` |
| Mailpit Web UI | `http://localhost:8025` |
| Mailpit SMTP | `localhost:1025` |

Backend içinde henüz işlevsel API veya health endpoint'i yoktur. Frontend servisi `docker compose --profile frontend up` komutuyla çalıştırılabilir; bu aşamada arayüz mock veriler kullanır.

Servisleri durdurmak için `docker compose down` kullanılır. `docker compose down -v` ayrıca yerel veritabanı ve yükleme volume'lerini kalıcı olarak siler.

PostgreSQL 15, `db-data-pg15` adlı ayrı volume kullanır. Daha önce PostgreSQL 17 ile oluşturulmuş olabilecek `db-data` volume'ü otomatik olarak silinmez veya bağlanmaz; gerekli veriler `pg_dump`/`pg_restore` ile kontrollü biçimde taşınmalıdır.

## Ortam Değişkenleri

| Değişken | Varsayılan | Açıklama |
| --- | --- | --- |
| `DB_HOST` | `localhost` | PostgreSQL sunucusu |
| `DB_PORT` | `5432` | PostgreSQL portu |
| `DB_NAME` | `workflowdb` | Veritabanı adı |
| `DB_USER` | `postgres` | Veritabanı kullanıcısı |
| `DB_PASSWORD` | `postgres` | Yalnızca yerel geliştirme parolası |
| `MAIL_HOST` | `localhost` | SMTP sunucusu |
| `MAIL_PORT` | `1025` | SMTP portu |
| `UPLOAD_DIR` | `./uploads` | Dosya saklama dizini |
| `BOOTSTRAP_ADMIN_EMAIL` | Yok | İlk Admin için tek kullanımlık e-posta |
| `BOOTSTRAP_ADMIN_PASSWORD` | Yok | İlk Admin için tek kullanımlık güçlü parola |

Gerçek parola, token imzalama anahtarı, OAuth2 istemci sırrı, ilk Admin parolası veya Outlook hesabı bilgileri repository'ye eklenmemelidir. `.env` Git tarafından izlenmez.

## Git Branching ve Geliştirme İş Akışı

Projeye katkıda bulunan tüm geliştiriciler aşağıdaki branch akışına uymalıdır:

```text
main (Production / Stable)
  ▲
  │ testleri tamamlanmış ve doğrulanmış sürüm
  │
test (Staging / QA)
  ▲
  │ Pull Request ve Code Review
  │
feature/* veya bugfix/*
```

### Branch Oluşturma

Yeni özellik ve hata düzeltme branch'leri güncel `test` branch'inden açılır:

```bash
git checkout test
git pull origin test
git checkout -b feature/kayit-olusturma-api
```

Branch adlarında işin türü ve kapsamı açıkça belirtilmelidir. Örnekler:

- `feature/kayit-olusturma-api`
- `feature/admin-role-management`
- `bugfix/workflow-transition-validation`

### Commit Standardı

Commit mesajları yapılan değişikliği açıkça anlatmalıdır. Conventional Commits benzeri önekler kullanılmalıdır:

```text
feat: kayıt oluşturma endpoint'i eklendi
fix: Başkan onay geçişi doğrulaması düzeltildi
test: rol yetki senaryoları eklendi
docs: README iş akışı kuralları güncellendi
```

### Pull Request ve Merge Süreci

1. Geliştirme tamamlandığında branch uzak repository'ye gönderilir ve hedefi `test` olan Pull Request açılır.
2. Code Review ve varsa static analysis kontrolleri tamamlanır.
3. İlgili birim ve entegrasyon testleri başarılı olmadan PR merge edilmez.
4. Onaylanan PR `test` branch'ine merge edilir.
5. `test` üzerinde entegrasyon testleri ve manuel senaryolar tamamlandıktan sonra `main` için release PR açılır.
6. `main` branch'ine alınan kararlı sürüm etiketlenir.

`main` ve `test` branch'lerine doğrudan push yasaktır.

## Kod Kalitesi, Hata Yönetimi ve Test Standartları

- Kod okunabilir, modüler ve SOLID prensiplerine uygun olmalıdır.
- Controller katmanı iş kuralı içermemeli; iş kuralları ilgili service/domain katmanında uygulanmalıdır.
- İstek doğrulamaları Bean Validation ile yapılmalıdır.
- Uygulama hataları merkezi olarak Spring Boot `@ControllerAdvice` yapısıyla ele alınmalıdır.
- Tüm hata yanıtları ortak bir JSON DTO sözleşmesine uymalıdır.
- Hata yanıtında en az zaman, HTTP durum kodu, uygulama hata kodu, mesaj ve istek yolu bulunmalıdır.
- Hassas iç hata ayrıntıları ve stack trace istemciye gönderilmemelidir.
- Kritik iş kuralları, rol kontrolleri, kayıt görünürlüğü ve durum geçişleri birim ve entegrasyon testleriyle doğrulanmalıdır.
- PostgreSQL davranışına bağlı entegrasyon testlerinde Testcontainers tabanlı bağımsız test veritabanı kullanılmalıdır.

## Kısa Geliştirme Kuralları

- Kayıt durumları yalnızca `workflow` modülü üzerinden değiştirilmelidir.
- Kullanıcı oluşturma ve rol atama işlemleri yalnızca yetkili Admin endpoint'leri üzerinden yapılmalıdır.
- Kullanıcı oluşturma API'si başlangıç rolü kabul etmemeli; backend her yeni normal hesabı Çalışan olarak oluşturmalıdır. Sonraki rol değişikliği ayrı bir Admin işlemi olmalı ve audit kaydı üretmelidir.
- Yetki kontrolleri frontend'e bırakılmamalı, backend endpoint'lerinde uygulanmalıdır.
- Admin rolü, iş akışı kayıtlarına otomatik erişim sağlamamalıdır.
- Uygulanmış Flyway migration'ları değiştirilmemelidir.
- Kritik iş kuralları, rol kontrolleri ve durum geçişleri için testler yazılmadan PR tamamlanmış kabul edilmemelidir.

## Proje Dokümantasyonu

Ayrıntılı teknik belgeler `/docs` dizininde tutulacak:

- [Sistem Mimarisi](docs/architecture.md)
- [İş Akışı ve Durum Geçişleri](docs/workflow.md)
- [Veritabanı Tasarımı](docs/database.md)
- [Mimari Kararlar](docs/decisions/README.md)
