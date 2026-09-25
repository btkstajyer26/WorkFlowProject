# M9 TEST Ortamı ve Cihaz Kabul Kanıtı

> **Arşiv belgesi.** Aşağıdaki bilgiler 21–31 Ağustos 2026 dönemindeki M9 kabulünü kaydeder; güncel ortam durumu veya operasyon talimatı olarak kullanılmamalıdır. Güncel yönergeler [TEST ortamı notunda](../TEST_ORTAMI_NOTU.md), API sözleşmesi [Mobil API envanterinde](../MOBIL_API_ENVANTERI.md) tutulur.

## Kabul dağıtımı

| Alan | Tarihsel değer |
| --- | --- |
| Base URL | `https://workflowproject-test.duckdns.org` |
| Kabul tarihi | 21 Ağustos 2026 |
| Backend commit | `4726d6974ae30f54120a7423d288acf18465da8c` (`4726d69`) |
| Son kaydedilmiş sağlık kontrolü | 31 Ağustos 2026 12:37 TRT, `200 UP` |

Health cevabı commit SHA'sını yayınlamadığı için son sağlık kontrolü, çalışan servisin kabul commit'iyle aynı sürümde olduğunu tek başına kanıtlamaz.

## Hesap ve veri kanıtı

Parolalar belgeye yazılmadı; Admin parolası ekip geneline paylaşılmadı.

| E-posta | Rol | Görünür kayıt |
| --- | --- | ---: |
| `calisan1@ebys-test.local` | `CALISAN` | 6 |
| `calisan2@ebys-test.local` | `CALISAN` | 1 |
| `bskyrd@ebys-test.local` | `BASKAN_YARDIMCISI` | 5 |
| `baskan@ebys-test.local` | `BASKAN` | 3 |
| `m9-admin@workflow.test` | `ADMIN` | Mobil kapsam dışı |

Toplam yedi kayıt üretildi. `calisan1`, altı workflow durumunun her birinde bir kayda sahipti: `TASLAK`, `BSK_YRD_INCELEMESINDE`, `BASKAN_INCELEMESINDE`, `DUZENLEME_BEKLIYOR`, `ONAYLANDI`, `REDDEDILDI`. Yedinci kayıt `calisan2` taslağıydı ve görünürlük kapsamının negatif tarafını doğruladı.

Seed, [`deploy/seed-test-data.sh`](../../deploy/seed-test-data.sh) ile API üzerinden yapıldı; doğrudan SQL kullanılmadı.

## Fiziksel cihaz kabulü

| Alan | Sonuç |
| --- | --- |
| Cihaz | Samsung Galaxy A34 |
| İşletim sistemi | Android 16 |
| Ağ | Mobil veri |
| Tarih | 21 Ağustos 2026, yaklaşık 23:00 TRT |
| Hesap | `calisan1@ebys-test.local` |
| Mobil build | EAS Android preview `cdeede67-8124-4cd9-81ac-11296e380c7c` |
| Backend commit | `4726d69` |
| Sonuç | Giriş başarılı; kayıt listesinde 6/6 kayıt görüldü |

Cihazda görülen kayıt sayısı, seed betiğinin hesapladığı görünür kayıt sayısıyla eşleşti.

## Kabul sırasındaki ortam yüzeyi

Dışarıya açık tek servis Caddy (`80/443`) idi. PostgreSQL, backend, Mailpit ve frontend portları dış ağdan kapalıydı; `/mail` basic auth ile korunuyordu. TLS sertifikası Let's Encrypt üzerinden yönetiliyordu. Ürün web frontend'i yayınlanmadığı için e-posta derin bağlantıları kabul kapsamı dışındaydı.
