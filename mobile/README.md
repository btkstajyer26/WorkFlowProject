# EBYS Mobil

EBYS mobil istemcisinin React Native, Expo Router ve TypeScript tabanlı uygulama
iskeletidir.

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

iOS yerel derlemesi macOS gerektirir. İlerleyen aşamada EAS development build
ile gerçek cihaz doğrulaması yapılacaktır.

## Mevcut kapsam

- Expo SDK 57 ve TypeScript iskeleti
- Expo Router kök düzeni
- `(auth)` ve `(app)` route sınırları
- Henüz API, auth, global tema veya özellik ekranı bulunmayan başlangıç yapısı

## Kalite komutları

```powershell
npm run typecheck
npm run lint
npx expo-doctor
```

Özellik ekranları alan sahipleri tarafından eklenecek; ortak altyapı feature
componentlerinin içine kopyalanmayacaktır.
