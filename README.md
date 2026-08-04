# 📋 İş Akışı ve Onay Yönetim Sistemi (EBYS Modülü)

> **Staj Projesi Teknik Şartnamesi ve Geliştirici Kılavuzu**  

---

## 📌 1. Proje Özeti
Bu proje, kurum içindeki tüm evrak, kayıt ve onay süreçlerini dijitalleştirmek amacıyla tasarlanmış bir **İş Akışı ve Onay Yönetim Sistemi**dir. 

Sistem; evrak oluşturma, ek doküman yükleme, hiyerarşik onay zincirini (Çalışan → Başkan Yardımcısı → Başkan) yönetme, detaylı denetim izi (Audit Log) tutma ve Outlook e-posta bildirimleri gönderme işlevlerini sunar.

---

## 🛠 2. Önerilen Teknik Mimari Stack

| Katman | Teknoloji / Framework |
| :--- | :--- |
| **Backend** | Java Spring Boot (Java 21+) |
| **Frontend** | React.js / Vue.js |
| **Database** | PostgreSQL |
| **ORM / Persistence** | Hibernate / Spring Data JPA |
| **Güvenlik & Auth** | JWT (JSON Web Token) / OAuth2 |
| **E-Posta Servisi** | Spring Mail (Outlook SMTP / Exchange Entegrasyonu) |

---

## 🔄 3. Rol Bazlı İş Akışı ve Durum Yönetimi

### Kullanıcı Roller
* 👤 **Çalışan:** Kayıt oluşturur, dosya yükler, taslakları yönetir ve geri gönderilen kayıtları düzenler.
* 👨‍💼 **Başkan Yardımcısı:** Çalışandan gelen kayıtları inceler; onaylayıp Başkana iletir veya açıklama ekleyerek Çalışana geri gönderir.
* 👔 **Başkan:** Nihai karar merciidir. Kaydı onaylar, reddeder veya alt kademelere geri gönderir.

### Kayıt Yaşam Döngüsü (State Machine)
`[Taslak]` ➔ `[Başkan Yardımcısı İncelemesinde]` ➔ `[Başkan İncelemesinde]` ➔ `[Onaylandı]`
* **Düzenleme Bekliyor:** İnceleme sırasında geri gönderilen kayıtlar bu duruma geçer.
* **Reddedildi:** Başkan tarafından reddedilen kayıtlar süreç dışı kalır ve kilitlenir.

---

## 🚀 4. Proje Fazları

### Faz I (Öncelikli Web Kapsamı)
* Web tabanlı yönetim arayüzü ve REST API backend servisi.
* Çoklu dosya yükleme (PDF, Word, Excel, Görsel) ve MIME-type güvenlik kontrolleri.
* Arama, filtreleme ve detaylı işlem geçmişi (Audit Log) paneli.
* **Outlook E-posta Entegrasyonu:** Durum değişikliklerinde otomatik e-posta ve e-posta içerisinden evraka yönlendiren **Deep Link** desteği.

### Faz II (Gelecek Kapsam)
* Android / iOS Mobil Uygulama (React Native / Flutter / Native).
* Mobil Push Notification alımı ve mobil cihaz üzerinden hızlı onay/red mekanizmaları.

---

## 🌿 5. Git Branching ve Geliştirme İş Akışı Kuralları

Projeye katkıda bulunacak tüm geliştiricilerin aşağıdaki Git branching stratejisine uyması **zorunludur**:

```text
  main (Production / Stable)
    ▲
    │ (Test tamamlandı, hatasız kod)
  test (Staging / QA)
    ▲
    │ (PR / Code Review ile merge)
  feature/*  /  bugfix/*
```

### Git Akış Kuralları:
1. **Branch Oluşturma (`test`'ten dallanma):**
   * Yeni bir özellik (feature) veya hata düzeltmesi (bugfix) geliştirilirken **kesinlikle `test` branch'inden** yeni bir branch açılır:
     ```bash
     git checkout test
     git pull origin test
     git checkout -b feature/kayit-olusturma-api
     ```
2. **Geliştirme ve Commit Standartları:**
   * Commit mesajları yapılan işi net açıklamalıdır (Örn: `feat: kayıt oluşturma REST endpoint eklendi`).
3. **Merge & Pull Request (PR) Süreci:**
   * Geliştirme tamamlandıktan sonra kod kontrolleri (Code Review ve static analysis) gerçekleştirilir.
   * Kontroller tamamsa, açılan PR onaylanarak kod **`test` branch'ine merge edilir**.
4. **Test ve Doğrulama Aşaması:**
   * `test` branch'i üzerinde tüm entegrasyon ve birim testleri koşulur, manuel senaryolar dener.
5. **Production Release (`main`'e alma):**
   * Kod `test` branch'inde tamamen test edilip **hatasız olduğu doğrulandıktan sonra** `main` branch'ine merge atılır ve sürüm etiketlenir (Tagging).
   * Direct push to `main` veya `test` kesinlikle yasaktır!

---

## 🧪 6. Kod Kalitesi ve Test Standartları
* **Clean Code & SOLID:** Kod okunabilir, modüler ve SOLID prensiplerine uygun olmalıdır.
* **Hata Yönetimi:** Spring Boot `@ControllerAdvice` yapısı ile tüm hatalar standart JSON DTO formatında dönmelidir.
