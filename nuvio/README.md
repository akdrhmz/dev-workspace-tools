# AtlasStream Nuvio Addon (Universal TR)

[Nuvio](https://github.com/yoruix/nuvio) için tek pakette toplanmış en popüler Türkçe Film, Dizi ve Belgesel sağlayıcıları koleksiyonu.

---

## 📱 Nuvio'ya Nasıl Eklenir?

1. Nuvio uygulamasını açın.
2. **Ayarlar (Settings) → Eklentiler (Plugins / Addons)** bölümüne gidin.
3. **Plugin Manifest URL** alanına aşağıdaki bağlantıyı yapıştırıp **Ekle (Install)** butonuna tıklayın:

```text
https://raw.githubusercontent.com/akdrhmz/dev-workspace-tools/master/nuvio/manifest.json
```

---

## 📺 Dahil Edilen Kaynaklar (Providers)

| Sağlayıcı | Tür | İçerik | Format |
|---|---|---|---|
| **Film Makinesi** | Film & Dizi | Türkçe Dublaj & Altyazılı Arşiv | m3u8, mp4 |
| **DiziBal** | Film & Dizi | Yabancı / Yerli Dizi & Film | m3u8 |
| **DiziFilm** | Film & Dizi | Popüler Dizi & Filmler | m3u8 |
| **FullHDFilmizlesene** | Film | 1080p Türkçe Filmler | m3u8, mp4 |
| **DiziPal** | Film & Dizi | Güncel Dizi ve Filmler | m3u8, mp4 |
| **SineWix API** | Film & Dizi | Hızlı Doğrudan API Akışları | m3u8, mp4 |
| **Sezonluk Dizi** | Dizi | Kapsamlı Yabancı Dizi Arşivi | m3u8, mp4 |
| **BelgeselX** | Belgesel | Türkçe Belgesel Arşivi | m3u8, mp4 |

---

## ⚙️ Geliştirme ve Derleme

Gereksinimler: Node.js 18+

```bash
cd nuvio
npm install
node build.js
```

Tüm sağlayıcılar `src/<provider>/index.js` üzerinden derlenerek `providers/<provider>.js` altında bağımsız tekil dosyalara dönüştürülür.
