# RidoPlay (OtoMüzik) 🎵🚗

Android tabanlı, özellikle araç içi multimedya (Android Automotive / tablet) ekranları için optimize edilmiş, modern ve şık bir yerel müzik çalar uygulaması.

## 🌟 Özellikler

* **Araç İçi Arayüz (Car-Friendly UI):** Yatay ekranlara uygun geniş butonlar, göz yormayan karanlık tema ve şık turkuaz/cyan vurgular.
* **Gelişmiş Sıra ve Liste Yönetimi:** Klasör gezgini, tüm şarkılar, çalma listeleri ve favoriler sekmeleri. Çalma başladığında akıllı sıra geçişi.
* **Senkronize Şarkı Sözleri (LRC & Düz Metin):**
  * Gömülü ID3 sözleri ve harici `.lrc` dosyaları desteği.
  * Senkronize karaoke tarzı söz takibi (aktif satır parlak beyaz, yaklaşan satırlar gri).
  * Türkçe karakter kodlama düzeltici (`TurkishStringFixer`).
* **Çevrimiçi Albüm Kapağı & ID3 Tag Arama:**
  * iTunes ve Deezer servisleri üzerinden yüksek çözünürlüklü albüm kapakları arama ve önizleme.
  * Sayfalanmış (10'lu gruplar) görsel sonuç listesi.
  * ID3 etiketleri düzenleme ve gerekirse şarkı dosya adını doğrudan uygulama içinden yeniden adlandırma.
* **Jest Kontrolleri:** Albüm kapağı üzerinde sol taraftan yukarı/aşağı kaydırarak sezgisel ses seviyesi kontrolü.
* **Ekolayzır & Uyku Zamanlayıcısı:** Araç akustiğine uygun ses ayarları ve otomatik durdurma zamanlayıcısı.
* **Arka Plan Servisi & Medya Butonları:** Android Media3 entegrasyonu ve direksiyon kumandası medya tuşları uyumluluğu.

## 🛠️ Teknolojiler

* **Dil:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Medya Oynatıcı:** AndroidX Media3 / ExoPlayer & MediaSession
* **Ses Etiketleri:** JAudioTagger
* **Veritabanı:** SQLite / Android Room / SharedPreferences

---
*Geliştirici: Rıdvan Korkut (@rkorkut70)*
