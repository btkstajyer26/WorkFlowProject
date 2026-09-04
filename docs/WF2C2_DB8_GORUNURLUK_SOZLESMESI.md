# WF-2C2 / DB-8 — Ortak kayıt görünürlüğü sözleşmesi

**Tarih:** 4 Eylül 2026  
**Sahiplik:** Burak — business/application policy; Alperen — persistence ve departman sorguları.  
**Teslim:** Mevcut şema üzerinde ortak scope, dinamik rol okuma erişimi ve tüketici entegrasyonu. Departman görünürlüğü henüz uygulanmadı; WF-2C2 ve DB-8'in departman kabulü açık kalır.

## 1. Karar ve sınır

Görünürlük, workflow aksiyonu yapabilmekten ayrıdır. Okuma kapsamına girmek aksiyon yetkisi vermez; doğrudan atanan/oluşturan kullanıcıdan workflow aktörü olması istenmez. Bu değişiklik migration, permission seed'i, workflow transition veya HTTP DTO değişikliği getirmez.

Aktör doğrulanmış oturumdan gelir. Kullanıcı ve rol aktif olmalıdır. Her istekte mevcut rol ve aktif permission'ları kullanan authentication yolu korunur; eski JWT'deki görüntüleme adı yetki kaynağı değildir.

Temel ifade:

```text
aktif kullanıcı ve rol
AND RECORD_VIEW
AND system_key != ADMIN
AND deleted_at IS NULL
AND (creator OR direct assignee OR açık sistem rolü kapsamı)
```

İleride yetkili departman üyesi son OR grubuna eklenecektir. Sadece `RECORD_VIEW`, başka bir permission veya rol adının belirli bir metne benzemesi kayıt erişimi sağlamaz. ADMIN'e `RECORD_VIEW` verilse de deny korunur.

## 2. Davranış matrisi

Aşağıdaki tüm izinler aktif kullanıcı/rol, `RECORD_VIEW` ve silinmemiş kayıt önkoşullarını taşır:

| Aktör | Kapsam |
|---|---|
| Dinamik rol (`system_key=NULL`) | `created_by=actor.id` veya `assigned_to=actor.id` |
| CALISAN | Oluşturan veya doğrudan atanan |
| BASKAN_YARDIMCISI | Oluşturan veya doğrudan atanan; ayrıca `DUZENLEME_BEKLIYOR` durumundaki tüm kayıtlar veya `last_deputy_id=actor.id` |
| BASKAN | Oluşturan veya doğrudan atanan; ayrıca `BASKAN_INCELEMESINDE`, `ONAYLANDI`, `REDDEDILDI` durumları |
| ADMIN | Hiçbir kayıt |

Yardımcı ve başkanın geniş takip kapsamları mevcut ürün davranışını koruyan **açık istisnalardır**. Dinamik rollere aktarılamaz. Geçmişte işlem yapmak veya `last_deputy_id` olmak dinamik rol için tek başına yeterli değildir. Dinamik aktörün ataması kaldırıldığında, oluşturucu değilse erişim de biter.

## 3. İçerik ve geçmiş, erişimden ayrı kararlardır

- Yardımcı, `DUZENLEME_BEKLIYOR` durumundaki kayıt kendisine atanmış değilken devir anındaki başlık/açıklama/kategori kopyasını görür. Liste ve detay aynı `RecordContentView` üzerinden üretilir. Mevcut snapshot bulunamama fallback'i korunur.
- Donmuş dosya listesi devir tarihinde var olan dosyalar üzerinden hesaplanır. Devirden sonra yüklenen dosya doğrudan indirilemez/önizlenemez; `404` döner. Mevcut silinmiş dosya indirme davranışı değişmez.
- Yardımcının bu aralıktaki audit geçmişi son düzeltmeye geri gönderme anında biter. Başkanın geçmişi ilk başkana iletimden başlar; son iletimden kesilmez.
- Dinamik roller görünür kaydın güncel içeriğini ve tam audit geçmişini görür. Ek `AUDIT_VIEW` veya workflow permission şartı yoktur.

## 4. Java ve HTTP sözleşmesi

`VisibilityActor(UUID id, RoleId roleId, Optional<SystemRoleKey> systemRole, Set<String> permissionCodes)` değişmez kimliktir. `VisibilityActor.from(AuthenticatedUser)` aktifliği doğrular. `CurrentVisibilityActorProvider` aynı oturum sınırını korur, dinamik rolleri kabul eder. Geçersiz principal varsayılan bir kullanıcı/role çevrilmez.

`RecordVisibilityScope.forActor(actor)` bütün rol/permission seçimlerini tek yerde yapar. Çıktı actor ID'si, kayıt ilişkileri (`CREATOR`, `ASSIGNEE`, `PREVIOUS_DEPUTY`) ve durum kümesidir. Boş kümeler hiçbir kayıt erişimi vermez. `allows(...)` silinmiş kaydı reddeder ve ilişkileri/durumları OR ile değerlendirir. Bu katman repository, SQL veya Spring servisi kullanmaz.

`RecordAccessPolicy` tekil kaydı bu scope ile değerlendirir. `RecordSpecifications` yalnız scope'un SQL karşılığını üretir; rol adına veya sistem rolüne göre ikinci bir kural seçimi yapmaz. Arama filtreleri scope ile AND'lenir; soft-delete koşulu her scope için zorunludur. Eleme SQL'de, sayfalama ve toplam sayı hesaplanmadan önce yapılır.

| Okuma | Davranış |
|---|---|
| `GET /api/records` | Yalnız kapsam içi kayıtlar; permission yoksa veya ADMIN ise boş sayfa, toplam 0 |
| `GET /api/records/{id}` | Görünür kayıt ve aktöre uygun içerik |
| `GET /api/audit-logs/record/{id}` | Aynı kapsam ve aktöre uygun geçmiş kesimi |
| `GET /api/records/{id}/files` | Aynı kapsam ve mevcut dosya görünümü |
| `GET /api/files/{id}/download`, `/preview` | Ana kaydın kapsamı ve dosyanın görünürlüğü doğrulandıktan sonra storage okunur |

Mevcut fakat kapsam dışındaki tekil kayıt `403 FORBIDDEN`; bulunmayan veya soft-delete edilmiş ana kayıt `404 RESOURCE_NOT_FOUND` döner. Oturumsuz/pasif kimlik mevcut authentication katmanında reddedilir. İstek/yanıt alanları değişmez. Oluşturma, düzenleme, silme ve workflow aksiyonlarının mevcut yetkileri korunur.

## 5. Alperen için departman / DB-8 entegrasyon koşulları

Bu bölüm gelecekteki bağlantının sözleşmesidir; mevcut kodun departman desteği verdiği anlamına gelmez.

Departman kolunun sağlanması için kayıt departmana atanmış olmalı; aktör aktif departmanın aktif üyesi olmalı ve mevcut durum için **en az bir** aksiyonda aşağıdakiler birlikte sağlanmalıdır:

1. Aktif routing satırı `(department_id, from_status_id, action_id)` için aktörün rol ID'sini işaret eder.
2. Aynı durum/aksiyon/aktör rolü için aktif workflow transition vardır; kullanıcı aktif bir workflow aktörüdür.
3. Kullanıcının transition'ın istediği aktif permission'ı vardır; kayıt ilişkisi `ActorRequirement` ile uyumludur. `CREATOR_AND_ASSIGNEE` ayrıca oluşturucu eşitliği gerektirir.

Üyelik kendi scoped rolünü taşımaz; `users.role_id` kullanılır. Birden çok üyelik ve uygun aksiyon tek kaydı çoğaltmamalıdır: sorgu korele `EXISTS`/eşdeğer predicate kullanmalı; sayfalama doğru kalmalıdır. `parent_department_id` üzerinden yetki veya routing devralınmaz. Comment veya o an gönderilmemiş bir hedef alanı, okuma için bir aksiyon isteği gibi doğrulanmaz.

`RECORD_VIEW`, ADMIN deny ve soft-delete koşulları departman kolu için de geçerlidir. Eksik/pasif routing, yanlış rol, eksik permission veya üyelik yokluğu departman kolunu kapatır; başka bir geçerli creator/direct/system kapsamını kaldırmaz. Listeleme, uygun routing bulunmayan ilgisiz bir kayıt yüzünden workflow hatası fırlatmaz. Aksiyon gönderiminin hata kodları WF-6'ya aittir.

Tekil policy, SQL predicate ve notification eligibility aynı koşulları izlemelidir. Persistence bağlandığında ortak scope departman ilişkisini ifade edecek şekilde genişletilmeli; yalnız boolean kontrolü veya yalnız liste sorgusu değiştirilmemelidir. Geçici allow-all/deny-all departman adapter'ı bu teslimde yoktur.

**4 Eylül 2026 durumu:** DB-11/12/13'ün departman/member/routing/assignment şeması, entity ve repository katmanı V18–V22 ile hazırdır. **Açık bağımlılıklar:** WF-5/6 assignment ve resolver; DB-13'ün gönderim stratejisi/aksiyonu/seed migration'ı; departman scope–SQL bağlantısı, parity ve V1 E2E kabulü. Şema testlerinin geçmesi WF-2C2 departman kabulünü kapatmaz.

## 6. Kabul kanıtı

**Yerel doğrulama — 4 Eylül 2026, 11:27 TRT:** İlgili 111 test ve ardından tam backend `verify` başarılıdır: **667 test / 0 failure / 0 error / 0 skipped**. JAR paketleme de tamamlandı. Komut backend dizininde `DB_PORT=5433` ortam değişkeniyle `mvn -o verify` olarak çalıştırıldı (mevcut Maven repository yolu açıkça verildi). Bu kayıt yerel çalışma ağacının kanıtıdır; PR/CI/merge veya departman V1 kabulü değildir.

- `RecordAccessPolicyTest`: dinamik rol ilişkileri, sistem kapsamları, capability ve ADMIN deny, içerik/geçmiş seçimleri, silinmiş kayıt.
- `RecordVisibilityIntegrationTest`: PostgreSQL'de policy–sorgu ID eşitliği, filtre/sayfalama toplamları; gerçek JWT ile bütün okuma uçları, permission/atama kaldırma, pasif hesap/rol, soft-delete ve ADMIN deny.
- Mevcut content, audit ve file testleri devir kopyası/geçmiş kısıtlarını korur. `DynamicWorkflowRoleIntegrationTest` workflow capability'sinin tek başına görünürlük sağlamadığını doğrular.
- DB testlerinden önce Compose, çalışan konteyner ve PostgreSQL host portu incelenir. Bu çalışma için sağlıklı `workflow-db` konteyneri `127.0.0.1:5433 → 5432` olarak doğrulandı. Test fixture'ları transaction sonunda geri alınır.

Departman testleri ve V1 ürün kabulü bu kanıtlara dahil değildir. WF-2C2 bütünüyle bitmiş işaretlenmez.
