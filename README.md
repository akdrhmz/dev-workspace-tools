# 🎬 KekikStream & WatchBuddy → CloudStream 3 Eklenti Deposu

Bu depo, [WatchBuddy](https://stream.watchbuddy.tv/) (170+ site) ve [KekikStream](https://github.com/keyiflerolsun/KekikStream) Türkçe medya sağlayıcılarını **CloudStream 3** (Android Telefon & Android TV) ile uyumlu hale getiren hibrit ve otomatik bir eklenti deposudur.

---

## 📱 CloudStream'e Nasıl Eklenir?

### Yöntem 1: Tek Tıkla Ekleme (Mobil / TV Tarayıcısı)
Android cihazınızda aşağıdaki butona tıklayın:

[![CloudStream'e Ekle](https://img.shields.io/badge/CloudStream'e%20Ekle-Repository-blue?style=for-the-badge&logo=android)](https://recloudstream.github.io/install/?repo=https%3A%2F%2FKULLANICI_ADI.github.io%2FCSRepo%2Fplugins.json)

### Yöntem 2: Manuel Depo Ekleme
1. **CloudStream 3** uygulamasını açın.
2. **Ayarlar (Settings) > Eklentiler (Extensions)** bölümüne gidin.
3. **Depo Ekle (Add Repository)** butonuna tıklayın.
4. **Depo Adı:** `KekikStream & WatchBuddy`
5. **Depo URL:**
   ```
   https://KULLANICI_ADI.github.io/CSRepo/plugins.json
   ```

---

## ✨ İçerdiği Eklentiler ve Özellikler

### 🌐 1. WatchBuddy Evrensel Köprüsü (`WatchBuddyBridge`)
* `stream.watchbuddy.tv` üzerindeki **170+ dizi, film, anime ve çizgi film sitesinin tamamına** tek bir eklenti üzerinden erişim sağlar.
* Upstream backend güncellendiğinde veya yeni site eklendiğinde CloudStream uygulamanızda otomatik güncellenir.

### 🚀 2. Yerel / Bağımsız Türkçe Sağlayıcılar (Native Providers)
Sunucuya veya harici API'ye ihtiyaç duymadan doğrudan cihazınızdan siteleri kazıyan bağımsız eklentiler:
* 🎬 **FilmMakinesi** (`FilmMakinesiProvider`)
* 📺 **DiziBox** (`DiziBoxProvider`)
* 🔥 **HDFilmCehennemi** (`HDFilmCehennemiProvider`)
* 🍿 **SineWix** (`SineWixProvider`)
* ⚡ **Dizilla** (`DizillaProvider`)

### 🔌 3. Ortak Video Çözücüler (Extractors)
* **RapidVid**, **VidMoxy**, **Tortuga**, **CloseLoad**, **Vidmoly** ve daha fazlası.

---

## 🤖 GitHub Actions Otomasyonu

Bu depo **tamamen ücretsiz ve otomatik** olarak GitHub üzerinde çalışır:
* **Cron Takibi:** Her 6 saatte bir upstream depoları (`keyiflerolsun/KekikStream` ve `WatchBuddy-tv/Stream`) kontrol eder.
* **Otomatik Kod Çevirici:** `generator/sync_upstream.py` yeni Python eklentilerini anında Kotlin `MainAPI` sınıflarına çevirir.
* **Otomatik Derleme:** Gradle ile `.cs3` paketlerini derleyip `gh-pages` dalına dağıtır.

---

## ⚖️ Yasal Sorumluluk Reddi (Disclaimer)

Bu yazılım yalnızca eğitim, araştırma ve kişisel kullanım amacıyla geliştirilmiştir.
1. **Barındırma Yoktur:** Bu depo ve üretilen eklentiler hiçbir video, ses veya medya dosyasını **barındırmaz, saklamaz veya sunucularında tutmaz**.
2. **Dizinleme / Arama:** Eklentiler, yalnızca internet üzerinde herkese açık web sitelerindeki genel HTML/JSON etiketlerini ayrıştıran birer web kazıyıcıdır (Web Scraper).
3. **Telif Sorumluluğu:** İzlenen içeriklerin telif haklarına uygunluğu ve yerel yasalara riayet edilmesi tamamen son kullanıcının sorumluluğundadır.
