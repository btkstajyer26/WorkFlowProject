# Mimari Kararlar

Bu dizin, projeyi uzun vadede etkileyen kararları Architecture Decision Record (ADR) biçiminde saklar. ADR'ler kararın bağlamını ve sonuçlarını kodla aynı sürüm geçmişinde görünür tutar.

## Kayıtlar

| ADR | Konu | Durum |
| --- | --- | --- |
| [0001](0001-modul-bazli-paketleme.md) | Backend'i modül (feature) bazlı paketleme | Kabul Edildi |
| [0002](0002-mobil-istemci-teknolojisi.md) | Mobil istemci teknolojisi — React Native + Expo | Kabul Edildi |
| [0003](0003-veri-tanimli-akis-motoru-ve-birim-bazli-roller.md) | Veri tanımlı akış motoru ve birim bazlı roller | Yerine Geçildi (ADR-0005 · ADR-0007) |
| [0005](0005-departman-atamasi-ve-akis-kurali.md) | Departman ataması ve akış kuralı | Kabul Edildi |
| [0006](0006-departman-hedefli-target-strategy.md) | Departman hedefli `target_strategy` ve gönderim sözleşmesi | Kabul Edildi |
| [0007](0007-rol-kapasitesi-ve-birim-tekilligi.md) | Rol kapasitesi ve birim tekilliği | Kabul Edildi |
| [0008](0008-hedef-rol-semantigi-ve-onceki-aktore-donus.md) | Hedef rol semantiği ve önceki aktöre dönüş (`B02`) | Önerildi |

> **ADR-0003 tarihsel öneridir; yerini yeni kararlar aldı.** ADR, rolün **daire başına**
> tekil olmasını öneriyor: `roles.scope` (`GLOBAL`/`UNIT`/`MULTI`),
> `users.org_unit_id` ve `assignee_rule = ROLE_IN_UNIT`. Uygulanan model ise
> **sistem genelinde** tekil — tekillik `roles.max_users` kolonuyla taşınıyor
> (`V12` ile geldi; `BASKAN`, `BASKAN_YARDIMCISI` ve `ADMIN` için `1`).
> V18–V22 departman/üyelik/routing ve kayıt ataması kalıcılığını eklemiştir;
> departman runtime'ı henüz bağlanmamıştır.
>
> **Bu fark [ADR-0005](0005-departman-atamasi-ve-akis-kurali.md) ile karara
> bağlandı (3 Eylül 2026):** birim semantiği `departments` + `department_members`
> ile taşınır, yetki global `users.role_id` ile çözülür; `roles.scope` ve
> `users.org_unit_id` açılmaz. ADR-0003 bu nedenle `Yerine Geçildi` durumundadır ve
> seçenek tartışması için kayıtta kalır.
>
> ADR-0003'ün diğer yarısı — tekilliğin **nerede zorlanacağı** —
> [ADR-0007](0007-rol-kapasitesi-ve-birim-tekilligi.md) ile karara bağlandı:
> `roles.max_users` Workflow V1 boyunca korunur, invariant uygulama katmanında
> (`RoleCapacityService` + rol satırı kilidi) zorlanır ve ADR-0003'ün kısmi `UNIQUE`
> indeks önerisi reddedilir. Tekilliğin uygulama katmanında zorlanması bilinçli
> bir karardır; eksik DB indeks işi olarak takip edilmez.
>
> Buradaki fark yalnız **rol kapsamı** içindir. ADR-0003'ün motor tarafı
> (kuralların veriye taşınması, `hasAuthority` dönüşümü, `RoleId`) fiilen
> uygulandı — ayrıntı ADR'nin kendi "uygulanma durumu" notundadır. Departman
> yönü ise ADR-0005 ile ayrıca karara bağlandı.

[ADR-0008](0008-hedef-rol-semantigi-ve-onceki-aktore-donus.md) `B02` bulgusunu
kapatır: `expected_target_role_id` kolonunun taşıdığı üç anlam ayrıştırılır, statik
hedef rol dayatması kimliği çalışma zamanında belirlenen stratejilerde yerini
role bağlı olmayan bir yetenek kontrolüne bırakır ve uygunsuz önceki aktör sessizce
yönlendirilmez. İstemci ve bildirim tarafındaki karşılığı
[APP-9/APP-10/B11 sözleşmesindedir](../APP9_APP10_B11_ISTEMCI_SOZLESMESI.md).
Karar **Önerildi** durumundadır; `V24` ve kod değişikliği henüz uygulanmamıştır.

ADR-0004 WebSocket kararı henüz bu dizinde yoktur. Kabul edilmiş ADR-0005/0006/0007,
WF-5/WF-6 uygulaması ve ürün kabulü yerine geçmez. Güncel hazır/açık kapsam
[dokümantasyon dizininde](../README.md) izlenir.

Bu dizinde her mimari karar için ADR yoktur ve olması da beklenmez. Mevcut
mimarinin çoğu [architecture.md](../architecture.md),
[workflow.md](../workflow.md) ve [database.md](../database.md) içinde gerekçesiyle
anlatılıyor; onları ADR'ye kopyalamak iki doğruluk kaynağı yaratırdı. Buraya
yalnız **gerekçesi başka yerde yazılı olmayan** ve seçenekleri tartışılmış
kararlar girer.

## Ne Zaman ADR Yazılır?

Aşağıdaki konular için ADR oluşturulmalıdır:

- kimlik doğrulama ve oturum/token yaklaşımı;
- tekli veya çoklu kullanıcı rolü modeli;
- dosya depolama sağlayıcısı ve yaşam döngüsü;
- Outlook e-posta entegrasyon yöntemi;
- audit kayıtlarının değişmezlik yöntemi;
- modüller arası önemli sınır veya bağımlılık değişiklikleri;
- kalıcı veri modelini ya da işletim topolojisini etkileyen teknoloji seçimleri.

Küçük, kolay geri alınabilir uygulama ayrıntıları için ADR gerekmez.

## Dosya Adlandırma

Dosyalar sıralı numara ve kısa kebab-case başlıkla adlandırılır:

```text
0001-kimlik-dogrulama-yontemi.md
0002-kullanici-rol-modeli.md
```

Numaralar yeniden kullanılmaz. Karar değiştiğinde eski kayıt silinmez; yeni ADR eskisinin yerini aldığını belirtir.

## Durumlar

| Durum | Anlamı |
| --- | --- |
| `Önerildi` | İnceleme ve ekip kararı bekliyor |
| `Kabul Edildi` | Uygulanması onaylandı |
| `Reddedildi` | Değerlendirildi ancak seçilmedi |
| `Yerine Geçildi` | Daha yeni bir ADR tarafından değiştirildi |

## ADR Şablonu

```markdown
# ADR-NNNN: Karar Başlığı

- Durum: Önerildi
- Tarih: YYYY-MM-DD
- Karar sahipleri: İsim veya ekip

## Bağlam

Kararı gerektiren problem, kısıtlar ve mevcut durum.

## Değerlendirilen Seçenekler

1. Seçenek ve temel artı/eksileri
2. Seçenek ve temel artı/eksileri

## Karar

Seçilen yaklaşım ve seçim gerekçesi.

## Sonuçlar

Olumlu sonuçlar, maliyetler, riskler ve gerekli takip işleri.

## Bağlantılar

İlgili issue, PR, doküman veya önceki ADR bağlantıları.
```

## Süreç

1. ADR, `Önerildi` durumuyla ayrı bir değişiklik olarak hazırlanır.
2. Teknik ve iş etkileri ilgili ekiplerle gözden geçirilir.
3. Sonuç ve tarih kaydedilir; karar dizini güncellenir.
4. Uygulama değişikliği ADR'ye bağlantı verir.
5. Karar değişirse eski ADR düzenlenmek yerine yeni bir ADR oluşturulur.
