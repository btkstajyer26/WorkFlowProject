# Mimari Kararlar

Bu dizin, projeyi uzun vadede etkileyen kararları Architecture Decision Record (ADR) biçiminde saklar. ADR'ler kararın bağlamını ve sonuçlarını kodla aynı sürüm geçmişinde görünür tutar.

## Kayıtlar

| ADR | Konu | Durum |
| --- | --- | --- |
| [0001](0001-modul-bazli-paketleme.md) | Backend'i modül (feature) bazlı paketleme | Kabul Edildi |
| [0002](0002-mobil-istemci-teknolojisi.md) | Mobil istemci teknolojisi — React Native + Expo | Kabul Edildi |

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
