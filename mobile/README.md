# EBYS Mobil

EBYS'nin React Native, Expo Router ve TypeScript tabanlı mobil istemcisidir. İş kuralları mobilde tekrarlanmaz; uygulama Spring Boot REST API'sini kullanır. Güncel uç sözleşmesi [Mobil API envanterindedir](../docs/MOBIL_API_ENVANTERI.md).

## Gereksinimler ve çalıştırma

- Node.js 22.13 veya üzeri
- npm
- Android geliştirme için Android Studio veya aynı ağdaki fiziksel cihaz

```powershell
npm ci
npm start
```

```powershell
npm run android
npm run web
```

iOS yerel derlemesi macOS gerektirir.

## API adresi

Backend adresi `EXPO_PUBLIC_API_BASE_URL` ile verilir. Commit edilmeyen `mobile/.env` dosyasını `.env.example` üzerinden oluşturun:

```env
EXPO_PUBLIC_API_BASE_URL=http://192.168.1.x:8080
```

EAS build'lerinde değişken build environment'ına tam adıyla verilmelidir; `eas.json` bunu kendiliğinden sağlamaz. TEST topolojisi için [dağıtım notuna](../docs/TEST_ORTAMI_NOTU.md) bakın.

## Güncel kapsam

- Giriş, token yenileme, güvenli saklama ve zorunlu parola değişimi
- Parola sıfırlama
- Kayıt listesi, detay, oluşturma, işlem geçmişi ve workflow aksiyonları
- Dosya seçme, sıralı yükleme, doğrulama, indirme ve paylaşma
- Profil ve uygulama içi bildirim merkezi
- Offline bildirimi ve bağlantı sonrası otomatik yenileme
- FCM için native cihaz tokenı kaydı/yenilemesi ve sıcak/soğuk açılış bildirim yönlendirmesi

Route'lar `src/app/` altındadır:

```text
src/app/
├── (auth)/       giris, sifre-sifirla, yeni-sifre
├── (password)/   sifre-degistir
└── (app)/        index, kayitlar/, olustur, bildirimler, profil
```

## Deep-link ve push

Kanonik kayıt route'u `ebys://kayitlar/{recordId}` biçimindedir. Push payload'ındaki `data.recordId`, `src/app/(app)/kayitlar/[id].tsx` ekranına yönlendirilir.

Push için Expo Go yeterli değildir; `expo-notifications` içeren development veya preview build ve fiziksel cihaz gerekir. Android kurulumu:

1. Firebase'de `app.json` içindeki `android.package` ile aynı paket adına sahip uygulama oluşturun.
2. Gerçek `google-services.json` dosyasını `mobile/google-services.json` olarak kaydedin; dosya Git tarafından izlenmez.
3. `google-services.json.example` yalnız alan biçimini gösterir, kimlik bilgisi değildir.
4. Backend'e `FCM_PROJECT_ID`, `FCM_CLIENT_EMAIL` ve `FCM_PRIVATE_KEY` değerlerini güvenli ortam değişkenleriyle verin.
5. `EXPO_PUBLIC_API_BASE_URL` değerini cihazın erişebildiği LAN adresine ayarlayıp development/preview build alın; fiziksel cihazda `localhost` veya `127.0.0.1` kullanmayın.

## Kontroller

```powershell
npm run lint
npm run typecheck
npm test -- --runInBand
npx expo-doctor
npx expo export --platform web
```

Cihaza özgü akışlar ayrıca fiziksel Android/iOS cihazlarda doğrulanmalıdır.

Registration, token renewal, foreground, background, cold-start ve notification
tap → `recordId` kodu/testleri tamamdır. Firebase bağlı fiziksel Android uçtan
uca turu yapılmadı: **MANUAL DEVICE ACCEPTANCE PENDING**. Tekrarlanabilir kurulum
ve kabul adımları [D04 rehberindedir](../docs/D04_NOTIFICATION_MOBILE_REALTIME_KABUL_REHBERI.md#e-nt-89-android-push-kabulü).

## Yayın öncesi bilinen kısıtlar

- `app.json` içindeki `com.anonymous.ebysmobile` yer tutucu paket adı kalıcı ada çevrilmeli; Firebase uygulaması ve `google-services.json` aynı adla eşleştirilmelidir.
- Push alımı ve doğru kayda yönlendirme release adayıyla gerçek cihazda doğrulanmalıdır.

Özellik ekranları alan sahipleri tarafından eklenir; ortak altyapı feature bileşenlerinin içine kopyalanmaz.
