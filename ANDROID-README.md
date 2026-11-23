# APK XAU Chart Studio (Android)

Sumber Android berada di folder `android-app/`. Proyek ini menggunakan Android WebView + androidx.webkit.WebViewAssetLoader. Kode web chart asli dikemas sebagai asset APK, sehingga tampilan dan drawing tools bisa digunakan tanpa membuka browser.

Setiap push ke `main` menjalankan GitHub Actions → Build Android APK. Hasil uji (debug-signed) tersedia di tab **Actions**, dan versi v1.0.0 disalin ke **Releases** sebagai `XAU-Chart-Studio.apk`.

Fitur: 6 drawing tools, timeframe, workspace, penyimpanan lokal dan quote spot online (jika API bisa dijangkau).

**Data candle:** Mode DEMO menggunakan data simulasi dan akan tetap diberi label demo. Koneksi API candle Twelve Data membutuhkan backend, saat ini belum ada endpoint khusus untuk APK. Jangan menganggap candle DEMO sebagai data pasar asli. Mode live belum aktif secara mandiri di APK.

**Catatan:** APK ini bertanda tangan debug untuk instalasi pribadi, bukan distribusi Play Store. Untuk distribusi produksi gunakan release keystore yang disimpan aman dan review privacy/security. Pada beberapa WebView, unduh screenshot dengan blob URL belum didukung meskipun drawing dan penyimpanan tetap berjalan.
