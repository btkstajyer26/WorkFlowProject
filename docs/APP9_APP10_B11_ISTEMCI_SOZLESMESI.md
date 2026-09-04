# APP-9 / APP-10 / B11 — Kullanılabilir Aksiyon, Atama ve Alıcı Sözleşmesi

- **Durum:** Önerildi — 4 Eylül 2026
- **Sahip:** Burak (`WF` / `APP`)
- **Tüketiciler:** Tamer (`B10` / `WEB-1`), Bahadır (`B09` / `MOB-1`, `NT-5`), Alperen (`B12`)
- **Kapsadığı bulgular:** `B09`, `B10`, `B11` · Kapsadığı işler: `APP-9`, `APP-10`
- **Kod tabanı:** `codex/ap-2-frontend-uyum` @ `c9b0297`

Bu belge üç kulvarı aynı anda açan tek sözleşmedir. Üçü de aynı ilkeye dayanır:

> **Workflow yetkisi tek bir yerde hesaplanır.** İstemci, bildirim kanalı ve
> görünürlük sorgusu bu hesabı tüketir; hiçbiri kendi kuralını kurmaz.

Bugün bu ilke üç yerde birden çiğneniyor: web paneli düğmeleri `systemKey`
sabitlerine bakıyor (`B10`), mobil kendi `getAvailableActions()` fonksiyonunu
yazmış (`B09`), bildirim dinleyicisi departman kolunda boş küme dönüyor (`NT-5`).
Sözleşme bu üçünü tek kaynağa bağlar.

## İçindekiler

- [1. APP-9 — Kullanılabilir aksiyonlar](#1-app-9--kullanılabilir-aksiyonlar)
- [2. APP-9 — Uygun hedef departman keşfi](#2-app-9--uygun-hedef-departman-keşfi)
- [3. B11 — Ortak atama sözleşmesi](#3-b11--ortak-atama-sözleşmesi)
- [4. B11 — Kayıt sürümü ve bayat istemci](#4-b11--kayıt-sürümü-ve-bayat-istemci)
- [5. APP-10 — Bildirim alıcı çözümü](#5-app-10--bildirim-alıcı-çözümü)
- [6. İstemci kabul maddeleri](#6-i̇stemci-kabul-maddeleri)
- [7. Kapatılan sorular](#7-kapatılan-sorular)

---

## 1. APP-9 — Kullanılabilir aksiyonlar

```http
GET /api/records/{recordId}/workflow/available-actions
```

### Karar

Aksiyon listesi, `performAction` ile **aynı** kural snapshot'ı ve **aynı**
`WorkflowTransitionValidator` üzerinden hesaplanır. İkinci bir kural motoru
yazılmaz; bu uç, mevcut doğrulamanın yazma yapmayan (`dry-run`) koşumudur.

Hesap şu şekildedir: snapshot içinde `from = record.status` **ve**
`actorRoleId = aktörün rolü` olan her kural için doğrulama çalıştırılır; sonucu
`Allowed` olan (veya yalnız "hedef henüz çözülmedi" nöbetçisiyle duran) aksiyonlar
listeye girer.

### Yanıt

```json
{
  "recordId": "3f1c…",
  "status": "BSK_YRD_INCELEMESINDE",
  "version": 7,
  "actions": [
    {
      "action": "BASKANA_ILET",
      "displayName": "Başkana İlet",
      "commentRequired": false,
      "targetUserRequired": false,
      "targetDepartmentRequired": false
    },
    {
      "action": "CALISANA_GERI_GONDER",
      "displayName": "Çalışana Geri Gönder",
      "commentRequired": true,
      "targetUserRequired": false,
      "targetDepartmentRequired": false
    }
  ]
}
```

`displayName`, `workflow_actions.display_name` kolonundan gelir; istemci aksiyon
etiketlerini kendi sözlüğünde tutmaz. `commentRequired` ve `target*Required`
alanları istemcinin form davranışını belirler.

### Bağlayıcı kurallar

1. **Yetki sızdırılmaz.** Kullanılamayan aksiyon listede **yer almaz**; neden
   kullanılamadığı (eksik permission, aktör bağı yok, ilişki uygun değil)
   döndürülmez. İstemciye "şu yetkin eksik" ipucu verilmez.
2. **Liste bir taahhüt değildir.** `performAction` tek yetkili yoldur ve kendi
   doğrulamasını baştan yapar. İki çağrı arasında rol, permission, routing,
   üyelik veya kayıt sürümü değişebilir. İstemci, listede görünen bir aksiyonun
   `409`/`403` alabileceğini varsaymak zorundadır. Bu uç **UI görünürlüğü**
   içindir, authorization değildir.
3. **Boş liste hata değildir.** Kaydı görebilen ama üzerinde işlem yapamayan
   kullanıcı `200` ve `"actions": []` alır. `403` dönmez.
4. **Görünürlük `GET /api/records/{id}` ile aynıdır.** Kapsam dışı kayıt `403`,
   bulunmayan/silinmiş kayıt `404` verir — detay ucuyla birebir aynı davranış.
5. **Terminal durumda liste boştur.** `ONAYLANDI` / `REDDEDILDI` kayıtlarında
   hiçbir geçiş tanımlı değildir.
6. `version` alanı §4'teki kayıt sürümüdür ve her yanıtta bulunur.

### Neden ayrı uç, neden detay yanıtına gömülü değil

Aksiyon hesabı kayıt detayından daha sık değişir (rol/permission/routing
değişikliği kaydı değiştirmez) ve detay yanıtı bütün okuma yollarında paylaşılır.
Ayrı uç, detay DTO'sunu şişirmeden aksiyonların ayrı yenilenmesine izin verir.
İstemci detay ile aksiyonları paralel çeker.

---

## 2. APP-9 — Uygun hedef departman keşfi

```http
GET /api/records/{recordId}/workflow/target-departments
```

### Karar

Normal kullanıcı departman seçicisini bu uçtan doldurur. Uç **kayıt kapsamlıdır**:
global bir departman listesi değildir.

Dönen küme, mevcut `DepartmentRoutingResolver.hasUsableRoutingInto(...)`
koşulunu sağlayan aktif departmanlardır — yani gönderim yapıldığında `409`
almayacağı **önceden doğrulanmış** departmanlar.

```json
{
  "departments": [
    { "id": 12, "name": "Hukuk" },
    { "id": 15, "name": "Satın Alma" }
  ]
}
```

### Bağlayıcı kurallar

1. **Kullanılamayacak departman listelenmez.** Routing kuralı olmayan, hedef rolü
   uygun permission taşımayan veya uygun aktif üyesi bulunmayan departman kümede
   yoktur. İstemcinin kullanıcıya kesin `409` verecek bir seçenek sunması
   engellenir.
2. **Pasif departman listelenmez.**
3. **Organizasyon dizini açılmaz.** Yanıt yalnız `id` ve `name` taşır; üye
   kimlikleri, üye sayısı, hedef rol veya hiyerarşi **döndürülmez**. Gönderen
   kullanıcının bu bilgilere ihtiyacı yoktur ve tekil rol kararı gereği kullanıcı
   listeleme ucu ona açılmaz. Tam departman listesi `AP-4` yönetim yüzeyidir ve
   `DEPARTMENT_VIEW` ister.
4. **Yalnız `DEPARTMANA_GONDER` kullanılabilirken anlamlıdır.** Aksiyon listede
   yoksa istemci bu ucu çağırmaz; çağırırsa boş liste döner.
5. Boş liste hata değildir; istemci "gönderilebilecek departman yok" durumunu
   gösterir.

---

## 3. B11 — Ortak atama sözleşmesi

### Karar

Kişi ve departman ataması **tek bir gömülü nesne** ile taşınır ve
`RecordResponse` (detay), `RecordSearchResponse` (liste satırı) ve
`WorkflowActionResponse` (aksiyon sonucu) yanıtlarına birlikte eklenir.

```json
"assignment": {
  "kind": "DEPARTMENT",
  "userId": null,
  "userFullName": null,
  "departmentId": 12,
  "departmentName": "Hukuk"
}
```

`kind` üç değer alır: `USER` · `DEPARTMENT` · `NONE`.

| `kind` | Dolu alanlar | Ne zaman |
|---|---|---|
| `USER` | `userId`, `userFullName` | Kayıt bir kişiye atanmış |
| `DEPARTMENT` | `departmentId`, `departmentName` | Kayıt bir departman kuyruğunda |
| `NONE` | — | `TASLAK` ve terminal durumlar |

### Bağlayıcı kurallar

1. **`kind` tek doğruluk kaynağıdır.** İstemci atama türünü iki nullable alanı
   karşılaştırarak **çıkarsamaz**; `kind` üzerinden dallanır. Böylece DB'deki
   karşılıklı dışlama kısıtı (`chk_records_assignment_exclusive`) tel
   biçiminde de görünür olur ve "ikisi de null" ile "departman kuyruğu" durumu
   istemcide karışmaz — `B11`'in asıl kırılması budur.
2. **`NONE` geçerli bir durumdur**, hata veya eksik veri değildir.
3. **Gösterim adı yanıtla birlikte gelir.** Gerekçe `createdByFullName` ile
   aynıdır: normal kullanıcının kullanıcı veya departman çözebileceği bir uç
   yoktur; ad verilmezse istemci denetim izini tarar ve yanlış ad gösterir.
4. **Departman adı, kaydı görebilen herkese gösterilir.** Bu yönlendirme
   bilgisidir, üyelik bilgisi değildir. Üye kimlikleri hiçbir yanıtta yer almaz.
5. **`WorkflowActionResponse.assignedTo` korunur.** Mevcut istemciler ve testler
   kırılmasın diye alan kaldırılmaz; `assignment.userId` ile aynı değeri taşır ve
   belgede türetilmiş/emekliye ayrılacak olarak işaretlenir. Yeni istemci kodu
   `assignment` kullanır.
6. **Web adapteri sabit `null` doldurmayı bırakır.** `frontend/src/api/recordDetails.ts`
   içindeki sabit atama alanları kaldırılır.
7. OpenAPI, web üretilmiş istemcisi ve mobil şeması **aynı PR'da** güncellenir.

---

## 4. B11 — Kayıt sürümü ve bayat istemci

### Karar

Kayıt sürümü (`records.version`) **bütün okuma yanıtlarında açılır**; istekte
gönderilmesi **opsiyoneldir**.

- `RecordResponse`, `RecordSearchResponse`, `WorkflowActionResponse` ve
  `available-actions` yanıtları `version` alanı taşır.
- `WorkflowActionRequest` ve kayıt güncelleme istekleri opsiyonel
  `expectedVersion` alanı kabul eder.
- `expectedVersion` **gönderildiyse** ve kaydın güncel sürümünden farklıysa istek
  `409 WORKFLOW_VERSION_CONFLICT` (workflow) veya `409 VERSION_CONFLICT` (CRUD)
  ile reddedilir — yazma denenmeden.
- Gönderilmediyse bugünkü davranış aynen korunur.

### Gerekçe

Bugün optimistic locking yalnız **eşzamanlı sunucu transaction'larını** koruyor.
Kullanıcı ekranı 10 dakika açık bırakıp bayat veriyle istek gönderdiğinde
sunucunun bunu ayırt etmesinin bir yolu yok (risk `R05`). Sürümü zorunlu kılmak
mevcut istemcileri ve testleri kırardı; opsiyonel yapmak yeni istemcilere korumayı
verirken geriye uyumu korur. Zorunlu hâle getirme V1 sonrası kararıdır.

> `expectedVersion` bir yarış çözümü değildir. `B03` (görev devrinin sürümü
> artırmaması) ve `B04` (dosya yüklemenin sürüme dokunmaması) ayrıca düzeltilmeden
> bu alan bayat istemciyi güvenilir biçimde yakalamaz. İki iş birbirine bağlıdır.

---

## 5. APP-10 — Bildirim alıcı çözümü

### Karar

Bildirim alıcıları **görünürlük ve aksiyon yetkisiyle aynı eligibility'den**
türetilir. Bildirim katmanı kendi kuralını kurmaz.

Bağlayıcı invariant:

> **Bildirim alıcıları ⊆ kaydı görebilen kullanıcılar.**
> Bir kullanıcıya, listesinde göremeyeceği bir kayıt için bildirim gitmez.

`recipientsOf(event)` üç kola ayrılır:

| Kol | Koşul | Alıcı kümesi |
|---|---|---|
| **Kişi** | `assignedTo != null` | Yalnız o kullanıcı |
| **Departman** | `assignedDepartmentId != null` | `DepartmentRoutingPort.resolve(departmentId, newStatus, …)` sonucundaki `Resolved.eligibleUserIds()` — yani routing kuralının işaret ettiği roldeki, `RECORD_VIEW` ve ilgili geçiş permission'ını taşıyan aktif üyeler |
| **Terminal** | ikisi de `null` | Kaydı oluşturan + son ileten aktör (`last_deputy_id`) |

Departman kolu bugün bilinçli olarak boş küme dönüyor; `NT-5` bu satırı yukarıdaki
kümeyle değiştirir. Hesap **yeni bir çözümleyici yazmaz**: `DepartmentRoutingAdapter`
ve `DepartmentVisibilityAdapter` ile aynı port ve aynı koşulları kullanır.

### Bağlayıcı kurallar

1. **Kör fan-out yasaktır.** Departmanın bütün üyelerine gönderilmez; yalnız
   uygun rol + permission + aktif üyelik üçlüsünü sağlayanlara gönderilir.
   Departman bir iş kuyruğudur, yetki değildir.
2. **Aktörün kendisi alıcı kümesinden çıkarılır.** İşlemi yapan kişiye kendi
   işleminin bildirimi gitmez.
3. **Tekilleştirme zorunludur.** Aynı kullanıcı iki koldan geldiğinde tek bildirim
   alır; mevcut `LinkedHashSet` sırası korunur.
4. **Kanallar alıcıyı yeniden türetmez.** Uygulama içi bildirim, WebSocket, push
   ve e-posta aynı kümeyi tüketir. Kanal bazlı ek filtre yalnız kullanıcı tercihi
   veya cihaz durumu olabilir; yetki filtresi olamaz.
5. **Boş küme sessizce yutulmaz.** Gönderim anında
   `hasUsableRoutingInto` en az bir uygun üyeyi garanti eder; bildirim anında küme
   boşsa aradaki sürede organizasyon değişmiş demektir. Bu durum uyarı seviyesinde
   loglanır, **geçiş geri alınmaz** — kayıt kuyruktadır ve routing boşluğu Admin
   tarafında çözülür.
6. **Mail hızlı işlem anahtarı yalnız o aksiyonu gerçekten yapabilecek alıcıya
   üretilir.** `B01` düzeltmesi bu kuralı değiştirmez, yalnız anahtarın commit
   sonrası kalıcı olmasını sağlar.

---

## 6. İstemci kabul maddeleri

**Web (`B10` / `WEB-1` — Tamer)**

- `RecordActionPanel` içindeki `systemKey === CALISAN/BASKAN_YARDIMCISI/BASKAN`
  koşulları **tamamen kaldırılır**; düğmeler `available-actions` yanıtından
  üretilir.
- `systemKey = null` olan dinamik rol yetkili aksiyonunu görür.
- Yetkisiz kullanıcı düğmeyi görmez **ve** doğrudan API çağrısı da reddedilir;
  kabul yalnız düğmenin gizlenmesiyle gösterilmez.
- Departman seçici `target-departments` ucundan doldurulur; kişi seçici
  **yapılmaz** (mevcut karar korunur).
- Atama alanları `assignment.kind` üzerinden gösterilir; sabit `null` doldurma
  kaldırılır.

**Mobil (`B09` / `MOB-1` — Bahadır)**

- `roleName` Zod enum'u kaldırılır; profil şeması `roleId` + `systemKey` +
  gösterim adı modeline geçer. `systemKey` nullable'dır.
- `RecordWorkflowActions` içindeki istemci tarafı `getAvailableActions()`
  kaldırılır ve yerini `available-actions` yanıtı alır.
- Dashboard, oluşturma yetkisi ve etiketler aynı modele taşınır.
- Kabul: yeni dinamik rol **ve** yeniden adlandırılmış yerleşik rolle giriş
  sonrası profil, liste, detay ve yetkili işlem çalışır.

**Bildirim (`NT-5` — Bahadır)**

- Departman kolu §5'teki kümeyi kullanır; dedupe ve kanal testleri eklenir.
- Yetkisiz departman üyesinin bildirim **almadığı** negatif testle gösterilir.

**Veri (`B12` — Alperen)**

- Workflow audit'i önceki/yeni atamayı §3'teki `kind` ayrımıyla uyumlu biçimde,
  yapılandırılmış olarak taşır. Serbest metin açıklamasına güvenilmez.

---

## 7. Kapatılan sorular

- **Kullanılabilir aksiyonlar detay yanıtına mı gömülür?** Hayır; ayrı uç (§1).
- **Aksiyon listesi authorization yerine geçer mi?** Hayır; `performAction`
  yetkilidir ve baştan doğrular (§1.2).
- **Kullanılamayan aksiyonun sebebi döndürülür mü?** Hayır (§1.1).
- **Departman keşfi global liste mi döndürür?** Hayır; kayıt kapsamlıdır ve
  organizasyon dizini açmaz (§2.3).
- **Atama türü nullable alanlardan çıkarsanabilir mi?** Hayır; `kind` zorunludur (§3.1).
- **`assignedTo` kaldırılıyor mu?** Hayır; korunur ve emekliye ayrılacak olarak
  işaretlenir (§3.5).
- **`expectedVersion` zorunlu mu?** Hayır; V1'de opsiyoneldir (§4).
- **Departmana kör fan-out yapılır mı?** Hayır (§5.1).
- **Bildirim alıcısı kaydı göremeyen biri olabilir mi?** Hayır; invariant yasaklar (§5).

## Bağlantılar

- [ADR-0008](decisions/0008-hedef-rol-semantigi-ve-onceki-aktore-donus.md) — hedef rol semantiği ve önceki aktöre dönüş
- [ADR-0005](decisions/0005-departman-atamasi-ve-akis-kurali.md) · [ADR-0006](decisions/0006-departman-hedefli-target-strategy.md)
- [WF-2C2 / DB-8 görünürlük sözleşmesi](WF2C2_DB8_GORUNURLUK_SOZLESMESI.md) — alıcı kümesinin üst sınırı
- [Web API sözleşmesi](FRONTEND_BACKEND_SOZLESMESI.md) · [Mobil API envanteri](MOBIL_API_ENVANTERI.md)
- [Tekrar üretim kanıtı](reviews/2026-09-04/TEKRAR_URETIM.md) — B01–B08 problarının adları ve koşum sonuçları. B09–B12 kod incelemesiyle doğrulandı; kırılmaların kod konumları §6'daki kabul maddelerinde adlandırılmıştır
