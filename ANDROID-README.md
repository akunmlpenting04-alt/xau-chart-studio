# APK XAU Chart Studio (Android)

Sumber Android berada di folder `android-app/`. Proyek ini menggunakan Android WebView + androidx.webkit.WebViewAssetLoader. Kode web chart asli dikemas sebagai asset APK, sehingga tampilan dan drawing tools bisa digunakan tanpa membuka browser.

Setiap push ke `main` menjalankan GitHub Actions → Build Android APK. Hasil uji (debug-signed) tersedia di tab **Actions**, dan versi v1.1.0 disalin ke **Releases** sebagai `XAU-Chart-Studio.apk`.

Fitur: 6 drawing tools, timeframe, workspace, penyimpanan lokal dan quote spot online (jika API bisa dijangkau).

**Data candle:** Mode DEMO menggunakan data simulasi dan diberi label demo. Untuk mode LIVE di APK v1.1: buka Setelan → API Key Twelve Data → tempel key pribadi → Simpan API Key → Hubungkan. Key disimpan secara terenkripsi menggunakan Android Keystore, tidak disertakan dalam APK maupun GitHub dan dikirim langsung dari perangkat ke Twelve Data melalui HTTPS Authorization header. Koneksi online dan paket Twelve Data dengan izin akses XAU/USD diperlukan. Polling sekitar 2 menit menghemat kuota Basic; bukan streaming tick-by-tick. Jangan bagikan key atau unggah key ke repository publik.

**Catatan:** APK ini bertanda tangan debug untuk instalasi pribadi, bukan distribusi Play Store. Untuk distribusi produksi gunakan release keystore yang disimpan aman dan review privacy/security. Pada beberapa WebView, unduh screenshot dengan blob URL belum didukung meskipun drawing dan penyimpanan tetap berjalan.
