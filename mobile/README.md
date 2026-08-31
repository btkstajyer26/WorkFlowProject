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
- FCM için native cihaz tokenı kaydı ve sıcak bildirim yönlendirmesi

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
4. `EXPO_PUBLIC_API_BASE_URL` değerini build environment'ına verip development/preview build alın.

## Kontroller

```powershell
npm run lint
npm run typecheck
npm test -- --runInBand
npx expo-doctor
npx expo export --platform web
```

Cihaza özgü akışlar ayrıca fiziksel Android/iOS cihazlarda doğrulanmalıdır.

## Yayın öncesi bilinen kısıtlar

- `app.json` içindeki `com.anonymous.ebysmobile` yer tutucu paket adı kalıcı ada çevrilmeli; Firebase uygulaması ve `google-services.json` aynı adla eşleştirilmelidir.
- `expo-notifications` config plugin'i henüz ekli değildir.
- Token yenileme dinleyicisi ve tamamen kapalı uygulamada soğuk açılış yönlendirmesi eksiktir.
- Push alımı ve doğru kayda yönlendirme release adayıyla gerçek cihazda doğrulanmalıdır.

Özellik ekranları alan sahipleri tarafından eklenir; ortak altyapı feature bileşenlerinin içine kopyalanmaz.
