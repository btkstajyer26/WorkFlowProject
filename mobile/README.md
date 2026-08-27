# EBYS Mobil

EBYS'nin React Native + Expo Router + TypeScript tabanlı mobil istemcisi.

Mobil, mevcut Spring Boot REST API'sini kullanır; **iş kuralı mobilde yazılmaz.**
Uç sözleşmesi için [Mobil API envanteri](../docs/MOBIL_API_ENVANTERI.md),
sprint planı için [Mobil entegrasyon görev dağılımı](../docs/MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md).

## Gereksinimler

- Node.js 22.13 veya üzeri
- npm
- Android geliştirme için Android Studio veya aynı ağdaki fiziksel cihaz

## Çalıştırma

```powershell
npm install
npm start
```

Platform komutları:

```powershell
npm run android
npm run web
```

iOS yerel derlemesi macOS gerektirir.

## API adresi

Backend adresi `EXPO_PUBLIC_API_BASE_URL` ile verilir. `mobile/.env` commit
edilmez; `mobile/.env.example` yerel geliştirme için IP placeholder'ı taşır.

```env
EXPO_PUBLIC_API_BASE_URL=http://192.168.1.x:8080
```

Ekipçe erişilebilen TEST ortamına bağlanmak için:

```env
EXPO_PUBLIC_API_BASE_URL=https://workflowproject-test.duckdns.org
```

EAS build'lerinde aynı değişken build environment'ına **tam adıyla** verilmelidir;
`eas.json` bunu kendiliğinden sağlamaz. Ayrıntı:
[TEST ortamı dağıtım notu](../docs/TEST_ORTAMI_NOTU.md).

## Mevcut kapsam

| Alan | Durum |
| --- | --- |
| Giriş, token yenileme, güvenli saklama (`expo-secure-store`) | ✅ |
| Zorunlu parola değişimi ve şifre sıfırlama akışı | ✅ |
| Kayıt listesi, detay ve işlem geçmişi | ✅ |
| Kayıt oluşturma ve workflow aksiyonları (gönder / onayla / reddet / geri gönder) | ✅ |
| Dosya seçme, sıralı yükleme kuyruğu, doğrulama, indirme ve paylaşma (`expo-sharing`) | ✅ |
| Profil | ✅ |
| Bildirim merkezi | ✅ Uygulama içi bildirim merkezi, okunmamış sayacı ve tab rozeti |
| Global offline bildirimi ve bağlantı sonrası otomatik veri yenileme | ✅ |
| Push bildirimi (FCM) | ✅ Firebase FCM entegrasyonu, cihaz token yaşam döngüsü ve deep-link |
| Otomatik test paketi | ✅ Auth, API, dosya paylaşımı, bildirimler, offline banner ve workflow aksiyonlarında 61 Jest testi |

Route yapısı `src/app/` altındadır:

```text
src/app/
├── (auth)/       giris, sifre-sifirla, yeni-sifre
├── (password)/   sifre-degistir  (mustChangePassword akışı)
└── (app)/        index, kayitlar/, olustur, bildirimler, profil
```

## Deep-link

`app.json` içinde şema `ebys` olarak tanımlıdır. Push'a dokunulduğunda açılacak
kanonik route:

| | |
| --- | --- |
| Şema | `ebys://kayitlar/{recordId}` |
| Dosya | `src/app/(app)/kayitlar/[id].tsx` |
| Kaynak alan | Push `data.recordId` |

Web'deki `/kayitlar/:recordId` ile bilerek aynıdır.

## Kalite komutları

```powershell
npm run typecheck
npm run lint
npm test -- --runInBand
npx expo-doctor
```

> [!NOTE]
> GitHub CI, mobil paket için bağımlılık kurulumu, lint, typecheck ve Jest
> testlerini çalıştırır. Cihaza özgü akışlar ayrıca fiziksel Android/iOS
> cihazlarda doğrulanmalıdır.

## Yayın öncesi bilinen açıklar

- `app.json` içindeki Android paket adı `com.anonymous.ebysmobile` Firebase Console'da tanımlıdır.
- `LICENSE`, `AGENTS.md`, `CLAUDE.md` ve `scripts/reset-project.js` dosyaları
  `create-expo-app` şablonundan gelmiştir ve bu projeye ait değildir.
  Özellikle `LICENSE` Expo'nun (650 Industries, Inc.) telif satırını taşır;
  projenin lisansını yansıtmaz.

Özellik ekranları alan sahipleri tarafından eklenir; ortak altyapı feature
componentlerinin içine kopyalanmaz.
