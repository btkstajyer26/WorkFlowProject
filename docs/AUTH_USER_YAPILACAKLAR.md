# Nisan Tat · Sümeyye Baykan — Yapılacaklar

**Modüller:** `auth`, `user`
**Tarih:** 12 Ağustos 2026
**Kaynak:** `integration/tum-feature-branchleri`

11 Ağustos'taki listedeki **beş açık sorunun beşi de kapandı**;
`feature/nisan-sumeyye` ve `feature/record` dalları entegre edildi. Kalan iş
sözleşmede tanımlı ama yazılmamış uçlar, denetim izi bağlantısı ve parola /
token yönetimi.

---

## Tamamlananlar

| İş | Durum |
|---|---|
| Pasifleştirme (eski 1.1) | ✅ `AuthService.login` artık `isActive` kontrol ediyor; `JwtAuthenticationFilter` pasif kullanıcının geçerli token'ını da reddediyor |
| Hatalı giriş 500 dönüyordu (eski 1.2) | ✅ `InvalidCredentialsException` → **401** + `INVALID_CREDENTIALS` kodu. Kod bilerek `UNAUTHORIZED` değil: o kod filtre zincirinin "token yok/geçersiz" reddine ayrıldı, arayüz ikisini ayırt edebiliyor |
| Admin doğrudan Başkan hesabı açabiliyordu (eski 1.3) | ✅ `POST /api/admin/users` artık rol almıyor, hesap daima **Çalışan** rolüyle açılıyor. Rol atama ayrı uçta |
| Doğrulama yoktu (eski 1.4) | ✅ `LoginRequest`, `LogoutRequest`, `RefreshTokenRequest`, `CreateUserRequest`, `ChangeRoleRequest` → `@NotBlank`/`@Email`/`@Size`; controller'larda `@Valid`. Yinelenen e-posta artık 500 değil **409** (`DataIntegrityViolationException` → `CONFLICT`) |
| Sistemde hiç kullanıcı yoktu (eski 1.5) | ✅ `BootstrapAdminRunner`. `BOOTSTRAP_ADMIN_EMAIL` ve `BOOTSTRAP_ADMIN_PASSWORD` **ikisi birden** açıkça verilmedikçe çalışmaz; varsayılan parola yok. Koşul "hiç kullanıcı yok" değil "hiç aktif Admin yok". `must_change_password = true` işaretleniyor |
| `auth` testleri (eski 6) | ✅ 27 test: `AuthControllerTest`, `AuthServiceTest`, `JwtAuthenticationFilterTest`, `CustomUserDetailsServiceTest` |

Not: `PATCH /api/admin/users/{id}/role` yazıldı — tek aktif Admin kuralıyla
birlikte (ikinci Admin ataması `AdminLimitExceededException` → 409).

---

## 1. Sözleşmede tanımlı, yazılmamış uçlar

Frontend bunları bekliyor; `AdminUsersPage`, `ChangeRoleDialog`,
`AccountStatusDialog` ve `AdminLogsPage` hazır, mock veriyle çalışıyor.

| Uç | Durum | Not |
|---|---|---|
| `GET /api/users/me` | ❌ | Sözleşme [s.77](FRONTEND_BACKEND_SOZLESMESI.md#L77). Girişten sonra kimlik ve rol buradan okunacak. **Sıradaki iş** |
| `GET /api/admin/users` | ❌ | Liste + arama (`q`) + rol/aktiflik filtresi + sayfalama |
| `PATCH /api/admin/users/{id}/active` | ❌ | Hesap etkinleştirme/pasifleştirme. Artık anlamlı: pasiflik hem girişte hem filtrede uygulanıyor |
| `GET /api/admin/roles` | ❌ | Atanabilir roller |
| `GET /api/admin/audit-logs` | ❌ | `audit` modülünde yalnızca `GET /api/audit-logs/record/{recordId}` var; Admin panelinin kullanıcı işlemleri görünümü ayrı |
| `PATCH /api/admin/users/{id}/role` | ✅ | Eklendi |
| `POST /api/admin/users` | ✅ | Rol alanı kaldırıldı, `@Valid` eklendi |

---

## 2. Denetim izi bağlanmamış 🔴

[`UserAuditLogService`](../backend/src/main/java/btk/staj/WorkFlowProject/audit/service/UserAuditLogService.java)
ve `user_audit_logs` tablosu tam olarak bu modül için yazıldı — kullanıcı
oluşturma, rol değiştirme, hesap açma/kapama. Sınıf **hâlâ hiçbir yerden
çağrılmıyor**.

Artık daha görünür bir eksik: `createUser` ve `changeRole` çalışıyor ama
arkalarında hiç iz kalmıyor. Şemadaki `BOOTSTRAP_ADMIN_CREATED` aksiyonu da
yazılmıyor — `BootstrapAdminRunner` Admin'i oluşturuyor, kaydetmiyor.

Yukarıdaki her yazma işlemi `UserAuditLogService.logIslem(...)` ile kayda
geçmeli.

---

## 3. Parola yönetimi

| İş | Not |
|---|---|
| Parola değiştirme ucu | Yok. `POST /api/auth/change-password` — mevcut parola doğrulanarak |
| `must_change_password` akışı | Alan yazılıyor (bootstrap Admin'de `true`) ama **hiç okunmuyor**. İlk girişte parola değişimi zorlanmalı; login yanıtında bu bilgi dönmeli ki frontend yönlendirsin. Bootstrap Admin eklendiği için artık gerçek bir boşluk |
| Parola kuralları | `@Size(min = 6)` dışında karmaşıklık kuralı yok |

---

## 4. Token yönetimi

| İş | Not |
|---|---|
| Refresh token rotation | `refresh()` yeni access token üretiyor ama **aynı refresh token'ı geri döndürüyor**. Çalınan token 7 gün geçerli kalır; her yenilemede eskisi iptal edilip yenisi verilmeli |
| Süresi geçen token temizliği | `tokens` tablosunda `expired` kolonu var, hiç kullanılmıyor. Her giriş yeni satır ekliyor, hiçbiri silinmiyor — tablo sonsuz büyür |
| Tüm oturumları kapat | Opsiyonel; parola değişiminde diğer refresh token'lar iptal edilmeli |

---

## 5. Kalan testler 🟡

`auth` artık test kapsamında. Listedeki maddelerden karşılananlar: doğru
bilgiyle giriş, hatalı parola 401, pasif kullanıcı giriş yapamaz, süresi
dolmuş / iptal edilmiş refresh token reddi, `POST /api/admin/users` ADMIN
olmayan role kapalı (`AuthorizationMatrixTest`).

Kalanlar:

- Pasif kullanıcının **mevcut token'ı** reddedilir — `JwtAuthenticationFilter`
  kontrolü eklendi ama testi yok
- Aynı e-postayla ikinci kullanıcı açılamaz (409)
- Rol değişikliği ve hesap kapatma `user_audit_logs`'a yazılır — 2. madde
  bağlandıktan sonra
- `user` modülünde yalnızca `UserResponseTest` var; `UserService.createUser`
  ve `changeRole` (tek Admin kuralı dahil) test edilmeli

---

## Küçük notlar

- `JwtUtil` şu an `rbac/config` altında ama tamamen `auth` modülünün işi.
  Taşınması gerekmez, sadece sahiplik karışıklığı yaratmasın diye not.
- DTO doğrulaması argüman çözümlemesi sırasında çalıştığı için, yetkisiz bir
  kullanıcı geçersiz gövde gönderdiğinde 403 yerine 400 alıyor. Spring'in
  standart sıralaması; sızan bilgi yalnızca "hangi alanlar zorunlu" düzeyinde.
  Rahatsız ederse yetki kontrolünün filtre seviyesine alınması gerekir.
