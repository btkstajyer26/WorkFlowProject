# Sistem Mimarisi

Bu belge, İş Akışı ve Onay Yönetim Sistemi'nin mevcut iskeletini ve hedef mimari sınırlarını tanımlar. Proje henüz altyapı aşamasındadır; aşağıda **hedef** olarak işaretlenen bileşenler işlevsel kabul edilmemelidir.

## Sistem Bağlamı

```text
Kullanıcı
   │
   ▼
React istemcisi (seçildi, henüz uygulanmadı)
   │ REST/JSON
   ▼
Spring Boot backend
   ├── PostgreSQL 15
   ├── Dosya depolama alanı
   └── Mailpit (yerel) / Outlook (hedef)
```

## Bileşenler

| Bileşen | Sorumluluk | Mevcut durum |
| --- | --- | --- |
| React istemcisi | Kayıt, onay, arama ve yönetim ekranları | React seçildi; yalnız Docker ve boş HTML iskeleti var |
| Spring Boot backend | REST API, iş kuralları, yetkilendirme ve entegrasyonlar | Uygulama ve paket iskeleti var; işlevsel API yok |
| PostgreSQL 15 | Kalıcı uygulama verisi | Sürüm kararı kesinleşti; Docker ve bağlantı yapılandırması hazır |
| Flyway | Sıralı şema değişiklikleri | Etkin; yalnız başlangıç migration'ı var |
| Dosya depolama | Kayıt eklerinin saklanması | Yerel dizin ve Docker volume yapılandırması var |
| Mailpit | Yerel e-posta yakalama ve görüntüleme | Docker Compose servisi hazır |
| Outlook | Üretim e-posta bildirimi | Hedef; entegrasyon yöntemi kesinleşmedi |

## Backend Modül Sınırları

Ana paket `btk.staj.WorkFlowProject` altındadır.

| Modül | Sınır |
| --- | --- |
| `auth` | Kimlik doğrulama, giriş/çıkış ve token ya da oturum akışı |
| `user` | Kullanıcı oluşturma, etkinlik durumu ve rol atama |
| `rbac` | Rol, görünürlük ve işlem yetkisi kuralları |
| `record` | Kayıt, kategori ve taslak işlemleri |
| `workflow` | İzin verilen durum geçişleri ve atamalar |
| `attachment` | Dosya doğrulama, saklama ve erişim |
| `audit` | Değiştirilemez işlem geçmişi |
| `search` | Arama, filtreleme ve sayfalama |
| `notification` | Uygulama içi ve e-posta bildirimleri |
| `common` | Ortak yapılandırma, DTO, hata ve yardımcı tipler |

Modüller şu anda çoğunlukla `.gitkeep` dosyalarıyla korunan boş paketlerden oluşur.

## Katmanlama Kuralları

Bir HTTP isteğinin olağan bağımlılık yönü aşağıdaki gibi olmalıdır:

```text
controller -> service/domain -> repository
                    │
                    ├── audit
                    └── notification
```

- Controller katmanı yalnız HTTP sözleşmesi, doğrulama ve yanıt eşlemesiyle ilgilenir.
- İş kuralları ilgili servis veya domain katmanında tutulur.
- Kayıt durumları yalnız `workflow` modülü üzerinden değiştirilir.
- Yetki ve kayıt görünürlüğü backend'de uygulanır; istemci kontrolleri güvenlik sınırı değildir.
- Dış modüller başka bir modülün repository katmanına doğrudan bağlanmak yerine o modülün servis sınırını kullanır.
- Ortak hata yanıtları ve çapraz kesen yapılandırmalar `common` altında tutulur.

## Veri ve Dosya Yönetimi

- PostgreSQL şemasının tek otoritesi Flyway'dir.
- Hibernate `ddl-auto=validate` ile yalnız entity ve şema uyumunu doğrular.
- Open Session in View kapalıdır; gerekli ilişkiler servis işlemi içinde yüklenmelidir.
- Yüklenen dosyalar veritabanında ikili veri olarak değil, yapılandırılmış depolama dizininde GUID tabanlı adla saklanır; metadata veritabanında tutulur.
- Audit kayıtları append-only olmalı ve uygulama üzerinden güncellenememeli veya silinememelidir.

## Yerel Çalışma Topolojisi

Docker Compose varsayılan olarak üç servis başlatır:

| Servis | Bağımlılık / veri | Port |
| --- | --- | --- |
| `db` | `db-data-pg15` volume | `5432` |
| `mailpit` | Yerel SMTP yakalayıcı | `1025`, Web UI `8025` |
| `backend` | Sağlıklı `db`, `uploads` volume | `8080` |

Frontend servisi `frontend` profili arkasındadır. `package.json` ve React uygulaması eklenene kadar bu servis build edilemez.

## Güvenlik Sınırları

Aşağıdaki maddeler hedef davranıştır ve henüz uygulanmış değildir:

- İlk Admin bootstrap istisnası dışında yeni kullanıcıları yalnız Admin oluşturur ve her hesap daima Çalışan rolüyle başlar.
- Başkan Yardımcısı, Başkan veya Admin rolü kullanıcı oluşturulurken seçilemez; rol daha sonra ayrı ve audit'lenen bir Admin işlemiyle değiştirilir.
- Admin rolü tek başına iş akışı kayıtlarına erişim vermez.
- Nihai onay ve ret yalnız Başkan tarafından yapılır.
- Son aktif Admin'in yetkisi kaldırılamaz ve hesabı devre dışı bırakılamaz.
- Parolalar yalnız güçlü tek yönlü hash ile saklanır; sırlar repository'ye yazılmaz.
