# Gerçek Backend E2E Testleri

Playwright testleri runtime mock kullanmaz. Tarayıcı, gerçek Spring Boot API'sine;
backend de izole PostgreSQL veritabanına bağlanır.

## Yerel kullanım

Repo kökünde yalnız E2E için kullanılan geçici Docker ortamını başlatın:

```powershell
docker compose -p workflow-e2e -f docker-compose.e2e.yml up -d --build --wait
```

Ardından frontend klasöründe hesabın otomatik hazırlanmasını açıp testleri
çalıştırın:

```powershell
$env:E2E_PROVISION_USER = "true"
npm run test:e2e
```

Test bittiğinde geçici veritabanını ve container'ları kaldırın:

```powershell
docker compose -p workflow-e2e -f docker-compose.e2e.yml down -v
```

`E2E_PROVISION_USER=true`, bootstrap Admin'in zorunlu parola değişimini API
üzerinden tamamlar; ardından yalnız E2E veritabanında kullanılacak Çalışan,
Başkan Yardımcısı ve Başkan hesaplarını oluşturur. Bu mod ortak geliştirme veya
production veritabanına karşı çalıştırılmamalıdır.

## Kapsanan kritik akışlar

- Giriş, güvenli çıkış ve isteğe bağlı şifre değiştirme ekranı (bootstrap Admin'in **zorunlu** parola değişimi ekranda değil, `global-setup.ts` içinde API ile tüketilir)
- Mailpit üzerinden şifremi unuttum kodu, yanlış kod, tek kullanımlık sıfırlama anahtarı ve hesap gizliliği
- Admin arayüzünden Çalışan hesabı oluşturma
- Admin kullanıcı araması, rol/aktiflik filtreleri, sunucu sayfalaması ve Başkan Yardımcısı görev devri
- Kayıt oluşturma, dosya yükleme, kalıcılık ve indirme
- Taslak dosyasını değiştirme ve gönderim sonrasında dosya işlemlerinin kilitlenmesi
- Üç kaydın eş zamanlı olarak onay, ret ve düzeltme dallarında ilerlemesi
- Çalışan, Başkan Yardımcısı ve Başkan rol sınırları
- Kayıt arama, kategori, durum, tarih, oluşturan ve sunucu sayfalamasının birlikte uygulanması
- Bildirim sayfasından ilgili kayda gidilmesi ve bildirimin okundu yapılması
- Bildirimlerin kullanıcılar arasında izole olması ve okunmamış sayacının listeyle tutarlı kalması
- Geri gönderme notunun detay, düzenleme ve işlem geçmişinde görünmesi
- İşlem geçmişinin doğrudan API yanıtıyla sıra, açıklama ve durum bakımından doğrulanması
- Masaüstü/mobil yatay taşma ve temel erişilebilirlik kontrolleri

Testler ortak PostgreSQL, Mailpit ve tekil rol koltuklarını kullandığı için
Playwright dosyaları tek worker ile sıralı çalışır.

İsteğe bağlı adres değişkenleri:

- `E2E_API_BASE_URL` (varsayılan `http://127.0.0.1:18080`)
- `E2E_BASE_URL` (varsayılan `http://127.0.0.1:5174`)
