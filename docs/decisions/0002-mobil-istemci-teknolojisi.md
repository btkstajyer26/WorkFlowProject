# ADR-0002: Mobil istemci teknolojisi — React Native + Expo

- Durum: Kabul Edildi
- Tarih: 2026-08-20
- Karar sahipleri: Proje ekibi
- Uygulanma durumu (3 Eylül 2026): `MOB-1`–`MOB-15` uygulandı (`mobile/`); iOS
  release/imza ve fiziksel cihaz push kanıtı açık. Karar metnindeki "onay
  alınmadan `MOB-1` başlatılmamalıdır" koşulu geçmiştedir.

## Bağlam

Faz II kapsamında Android/iOS istemcisi eklenecek. Mobil entegrasyon
şartnamesi teknoloji olarak **Flutter** diyor ve klasör yapısı olarak
`mobile/lib/main.dart` öneriyor.

Ekibin mevcut durumu:

- Frontend ekibi React + TypeScript ile çalışıyor; TanStack Query, React Hook
  Form ve Zod kullanıyor.
- Ekipte Dart/Flutter deneyimi olan kimse yok.
- Web istemcisinde mobilde yeniden kullanılabilecek hazır katmanlar var:
  üretilmiş OpenAPI istemcisi, tip tanımları, Zod şemaları, Axios hata modeli
  (`ApiError`), query key sözlüğü, rol ve tarih yardımcıları.

Backend her iki durumda da aynı: iş kuralları mobilde yazılmayacak, mobil
mevcut REST API'yi kullanacak. Yani bu karar **yalnız istemci tarafını**
etkiliyor; [tarihsel mobil görev dağılımındaki](../archive/MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md)
M0–M8 backend işleri her iki seçenekte de aynı.

## Değerlendirilen Seçenekler

1. **Flutter** (şartnamenin dediği)
   - Artı: Şartnameye birebir uyum; sapma gerekçelendirmesi gerekmez.
   - Eksi: Ekip sıfırdan Dart öğrenecek. Web'deki hiçbir katman taşınamaz;
     API sözleşmesi dışında paylaşım yok. Staj takviminde öğrenme süresi
     doğrudan teslim riskine dönüşüyor.

2. **React Native + Expo + TypeScript**
   - Artı: Ekip dili ve araç zincirini zaten biliyor. Tipler, OpenAPI DTO'ları,
     Zod şemaları, hata modeli ve query key'ler doğrudan taşınabiliyor.
   - Eksi: **Şartnameden sapma** — onay gerektirir. Web bileşenleri
     kopyalanamaz (`div` yerine `View`/`Text`/`Pressable`/`FlatList`); "React
     biliyoruz, aynısı" beklentisi yanlış. Push bildirimi Expo Go ile test
     edilemez, development build ve gerçek cihaz gerekir.

## Karar

React Native + Expo + TypeScript seçildi; kod `mobile/` altında duracak.

Karar **danışman onayına bağlıdır.** Onay alınmadan MOB-1 (proje kurulumu)
başlatılmamalıdır. Onay çıkmazsa seçenek 1'e dönülür; backend görevleri
etkilenmediği için bu dönüş yalnız mobil takvimini geciktirir.

## Sonuçlar

**Olumlu:** Mobil geliştirme ilk günden üretken başlıyor; API sözleşmesi ve
doğrulama şemaları tek kaynaktan geliyor, ikinci bir doğruluk kaynağı doğmuyor.

**Maliyet ve riskler:**

- Şartnameden sapma kabul edilmezse iş yeniden yapılır.
- Ekran bileşenleri sıfırdan yazılacak; web'den kopyalanamaz.
- Push testi için Firebase (Android + iOS) kaydı, iOS tarafında APNs anahtarı
  ve development build gerekir. Ayrıca ekipçe erişilebilen bir TEST API adresi
  olmadan gerçek cihazdan doğrulama yapılamaz.

**Takip işleri:** Sprint 0'da Firebase/APNs kurulumu, development build ve TEST
ortamı sahibinin belirlenmesi.

## Bağlantılar

- [Mobil entegrasyon görev dağılımı (arşiv)](../archive/MOBIL_ENTEGRASYON_GOREV_DAGILIMI.md)
- [Frontend–backend çalışma sözleşmesi](../FRONTEND_BACKEND_SOZLESMESI.md)
