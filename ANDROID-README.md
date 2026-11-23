# XAU Chart Studio — Android v1.2.0

Source berada di `android-app/`. GitHub Actions otomatis menghasilkan APK bertanda tangan debug untuk pengujian pribadi dan menerbitkan file pada **Releases**.

## Mengaktifkan XAU/USD real candles
1. Deploy GitHub repo `akunmlpenting04-alt/xau-chart-studio` di Netlify.
2. Simpan environment variables **TWELVE_DATA_API_KEY** dan **XAU_APP_ACCESS_TOKEN** (scope Functions); jangan masukkan API key Twelve Data ke APK atau repository.
3. APK → Data & Setelan → **URL Situs Netlify**: `https://nama-situs.netlify.app` → **Kode Akses Aplikasi**: nilai XAU_APP_ACCESS_TOKEN → **Simpan Koneksi** → **Mode Market Asli → Hubungkan**.
4. Tunggu request OHLC; aplikasi tetap memberi label DEMO bila memakai candle simulasi, dan menampilkan kesalahan koneksi jika API live gagal.

API key Twelve Data hanya dipakai di Netlify Functions. Kode akses aplikasi (terpisah dari API key) disimpan lokal dalam WebView localStorage agar request berikutnya bisa jalan. Kode akses dapat dicabut dari Netlify.

Jangan lupa **rotasi API key lama** yang pernah dibagikan dalam chat.

**Keterbatasan:** data real membutuhkan izin XAU/USD di paket Twelve Data, koneksi internet dan kuota cukup. App bukan eksekutor order, tidak tick-by-tick, dan belum diuji produksi pada semua perangkat. APK memakai debug signature dan tidak ditujukan untuk Play Store.
