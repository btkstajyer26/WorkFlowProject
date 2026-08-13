# Frontend'e: Kayıt Talebi Akışı Kaldırılacak, Kullanıcıyı Admin Oluşturur

**Tarih:** 12 Ağustos 2026
**Muhatap:** Frontend ekibi
**Karar veren:** Proje ekibi (12 Ağustos)
**İlgili:** [FRONTEND_BACKEND_SOZLESMESI.md §13](FRONTEND_BACKEND_SOZLESMESI.md#L459) ·
[EKSIK_CONTROLLERLAR_VE_KARARLAR.md §3.1](EKSIK_CONTROLLERLAR_VE_KARARLAR.md)

---

## Karar

Frontend'deki **öz-kayıt / başvuru (registration request)** akışı kaldırılacak.
Kullanıcı hesapları yalnızca Admin tarafından oluşturulacak.

Gerekçe: sözleşme §13 zaten *"Self-service kayıt/signup ekranı kapsam dışıdır;
kullanıcı hesaplarını yetkili sistem yöneticisi oluşturur"* diyor. Backend bu
karara göre yazıldı — `registration_requests` diye bir tablo veya uç yok, hiç
planlanmadı. Frontend'deki akış mock üzerinde geliştirildiği için fark
edilmemiş.

Backend'e başvuru modülü **eklenmeyecek**. Frontend bu akışı kaldırıp Admin
tarafından hesap açma ekranına bağlanacak.

---

## 1. Kaldırılacaklar

Tamamen silinecek dosyalar:

| Dosya |
|---|
| `src/mocks/registrationRequests.ts` |
| `src/mocks/registrationRequests.test.ts` |
| `src/types/registration.ts` |
| `src/components/admin/RegistrationReviewDialog.tsx` |

İçinden başvuru bölümü çıkarılacak dosyalar:

| Dosya | Ne yapılacak |
|---|---|
| `src/pages/LoginPage.tsx` | Başvuru formu ve "hesap talep et" akışı çıkar. Giriş ekranı yalnız e-posta + parola. Hesabı olmayan kullanıcıya yönlendirme değil, bilgilendirme metni: hesabı sistem yöneticisi açar |
| `src/pages/LoginPage.test.tsx` | Başvuru senaryoları çıkar |
| `src/context/AdminContext.tsx` | Başvuru state'i, onay/ret aksiyonları çıkar |
| `src/context/adminState.ts` | Başvuru alanları çıkar |
| `src/pages/admin/AdminDashboardPage.tsx` | "Bekleyen başvurular" kartı/sayacı çıkar |
| `src/pages/admin/AdminUsersPage.tsx` | Başvuru listesi ve inceleme diyaloğu çağrısı çıkar; yerine "Yeni kullanıcı" formu |
| `src/pages/admin/AdminUsersPage.test.tsx` | Başvuru senaryoları çıkar |
| `src/schemas/auth.ts` · `src/schemas/forms.test.ts` | Başvuru şeması ve testleri çıkar |
| `src/App.tsx` | Varsa başvuru rotası çıkar |

En çok dokunulacak yer `LoginPage.tsx` (49 satırda geçiyor).

---

## 2. Yerine gelen akış

Admin → **Yeni kullanıcı** formu → `POST /api/admin/users`

Hesap **daima Çalışan (`CALISAN`) rolüyle** açılır. Formda rol seçimi
**olmayacak** — backend istekteki rolü zaten okumuyor. Rol, hesap açıldıktan
sonra ayrı bir işlemle değiştirilir (bkz. §4).

---

## 3. Kullanıcı oluşturma ucu

```http
POST /api/admin/users
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "firstName": "Ayşe",
  "lastName": "Kaya",
  "email": "ayse.kaya@kurum.gov.tr",
  "password": "gecici-parola"
}
```

Cevap `200` — `UserResponse` (parola hash'i **dönmez**):

```json
{
  "id": "user-uuid",
  "firstName": "Ayşe",
  "lastName": "Kaya",
  "email": "ayse.kaya@kurum.gov.tr",
  "roleName": "CALISAN",
  "createdAt": "2026-08-12T10:15:00"
}
```

> **Dikkat:** `UserResponse` şu an **`active` alanı taşımıyor**. Kullanıcı
> listesi ve pasifleştirme ekranları bu bilgiye ihtiyaç duyacak; alan,
> `GET /api/admin/users` ucuyla birlikte eklenecek. Liste ekranını `active`
> alanı gelecekmiş gibi tasarlayın.

### Doğrulama kuralları (backend uygular)

| Alan | Kural | Hata mesajı |
|---|---|---|
| `firstName` | boş olamaz | "Ad boş olamaz" |
| `lastName` | boş olamaz | "Soyad boş olamaz" |
| `email` | boş olamaz, geçerli e-posta | "Email boş olamaz" / "Geçerli bir email adresi girin" |
| `password` | boş olamaz, en az 6 karakter | "Şifre boş olamaz" / "Şifre en az 6 karakter olmalı" |

Doğrulama hatası **`400`** döner ve alan bazlı liste taşır:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Girilen veriler geçersiz",
  "status": 400,
  "timestamp": "2026-08-12T10:15:00",
  "fieldErrors": [
    { "field": "email", "message": "Geçerli bir email adresi girin" }
  ]
}
```

`fieldErrors` doğrudan form alanlarının altına basılabilir.

### Hata kodları

| Durum | `code` | Anlamı |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Alan doğrulaması; `fieldErrors` dolu |
| `401` | `UNAUTHORIZED` | Token yok / geçersiz — filtre reddi |
| `401` | `INVALID_CREDENTIALS` | Girişte e-posta/parola hatalı (bu uçta değil, login'de) |
| `403` | `FORBIDDEN` | Admin olmayan rol |
| `409` | `CONFLICT` | E-posta zaten kayıtlı |

> **Not:** Yetkisiz bir kullanıcı **geçersiz gövde** gönderirse `403` yerine
> `400` alır. Doğrulama, yetki kontrolünden önce çalışıyor. Ekranı buna göre
> kurgulamayın — yetki kontrolünü sunucu cevabına değil, kullanıcının rolüne
> göre yapın.

---

## 4. Rol değiştirme ucu

```http
PATCH /api/admin/users/{id}/role
Content-Type: application/json

{ "roleName": "BASKAN_YARDIMCISI" }
```

Cevap `200` — güncellenmiş `UserResponse`.

Geçerli değerler: `CALISAN`, `BASKAN_YARDIMCISI`, `BASKAN`, `ADMIN`.

### Arayüzü etkileyen kurallar

- **Sistemde aynı anda tek bir aktif Başkan Yardımcısı ve tek bir aktif Başkan
  bulunur.** Zaten aktif biri varken ikincisini atamaya çalışmak `409` döner.
- **Başkan Yardımcısı ve Başkan rolündeki bir kullanıcı başka bir role
  çevrilemez.** Görevden alma, rol değiştirerek değil **hesabı pasife çekerek**
  yapılır. Bu deneme de `409` döner.
- Görev devri iki adımdır: önce mevcut kişi pasife çekilir, sonra yeni kişiye rol
  atanır. Arayüz bunu tek bir "devret" akışı gibi gösterecekse iki isteği sırayla
  yapmalı ve ilkinin başarısında ikinciye geçmeli.

| Durum | `code` |
|---|---|
| `409` | `ADMIN_LIMIT_EXCEEDED` — ikinci Admin ataması |
| `409` | *(kod netleşecek)* — ikinci Başkan Yardımcısı / Başkan ataması |
| `409` | *(kod netleşecek)* — Başkan Yardımcısı / Başkan rolünden çıkış denemesi |
| `400` | `ROLE_NOT_FOUND` — tanımsız rol adı |

---

## 5. Henüz hazır olmayan uçlar

Bu ekranları şimdilik mock ile bırakın; uçlar yazıldığında haber verilecek:

| Uç | Ne için | Durum |
|---|---|---|
| `GET /api/admin/users?page&size&q&role&active` | Kullanıcı listesi, arama, filtre | ❌ yazılmadı |
| `PATCH /api/admin/users/{id}/active` | Hesabı pasife çekme — **§4'teki devir akışı buna bağlı** | ❌ yazılmadı |
| `GET /api/admin/roles` | Atanabilir roller | ❌ yazılmadı |
| `GET /api/users/me` | Giriş sonrası kimlik ve rol | ❌ yazılmadı |

Takibi: [AUTH_USER_YAPILACAKLAR.md](AUTH_USER_YAPILACAKLAR.md)

---

## 6. Bilinmesi gereken iki boşluk

1. **Geçici parola akışı yok.** Sözleşme §8 *"cevap geçici parolayı bir kez
   döner, hesap `mustChangePassword=true` başlar"* diyor. Şu an Admin parolayı
   kendisi giriyor ve `mustChangePassword` işaretlenmiyor; parola değiştirme ucu
   da yok. Yani kullanıcıya parolayı Admin sözlü/yazılı iletiyor. Bu davranış
   sonra değişecek — form tasarımını buna göre esnek bırakın.

2. **CORS henüz yok.** `http://localhost:5173` izni yazılıyor. O eklenene kadar
   tarayıcıdan gerçek uçlara istek atılamaz.

---

## 7. Adres değişikliği

`/api/v1` öneki kaldırıldı. Mock'tan gerçek API'ye geçerken:

| Eski | Yeni |
|---|---|
| `/api/v1/records` | `/api/records` |
| `/api/v1/categories` | `/api/categories` |

Diğer tüm uçlar zaten `/api/...` altındaydı.
