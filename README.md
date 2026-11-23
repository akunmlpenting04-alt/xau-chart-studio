# XAU Chart Studio

Custom web app / PWA untuk analisis chart XAU/USD. Tidak memakai embed TradingView. Mesin chart, semua drawing tools, editing handles, auto-save, toolbar kiri/kanan, dan eksport dibuat mandiri menggunakan HTML Canvas + JavaScript, tanpa langkah `npm install`.

## Cara jalankan di komputer

Jalankan server statis di folder ini:

```bash
python -m http.server 8080
```

Buka `http://localhost:8080`. **Mode DEMO** langsung berfungsi dan menampilkan candle simulasi ber-watermark, supaya alat gambar dapat dicoba tanpa API. Header berusaha mengambil harga spot XAU dari https://api.gold-api.com/price/XAU (harga asli, diperbarui penyedia sekitar 30 detik; header terpisah dari candle demo).

## Deploy Netlify + akses aman dari website dan APK

1. Buka Netlify → **Add new project** → **Import from Git** → pilih `akunmlpenting04-alt/xau-chart-studio`.
2. Build command **kosong**, Publish directory **.**. Netlify membaca `netlify.toml` dan memasang Function `netlify/functions/candles.js`.
3. Di dashboard Netlify → **Project configuration → Environment variables**, tambahkan kedua variabel berikut pada scope **Functions** (atau All):
   - `TWELVE_DATA_API_KEY`: API key Twelve Data baru. Rotasi key lama yang terlanjur terkirim melalui chat. **Jangan tempel key ini ke GitHub atau APK.**
   - `XAU_APP_ACCESS_TOKEN`: buat kode akses acak minimal 16 karakter yang **berbeda** dari API key Twelve Data. Simpan secara pribadi.
4. Netlify → **Deploys → Trigger deploy** untuk memastikan Functions menerima variabel baru. Variable dalam `netlify.toml` tidak dibaca oleh Functions.
5. Di **website Netlify**, buka Data & Pengaturan → masukkan kode akses aplikasi → **Simpan Koneksi** → **Hubungkan**.
6. Di **APK Android v1.2.0**, buka Data & Pengaturan → masukkan URL Netlify (contoh `https://nama-situs.netlify.app`) dan kode akses yang sama → **Simpan Koneksi** → **Hubungkan**.
7. Jika berhasil, chart menampilkan candle XAU/USD dari Twelve Data (polling setiap ~65 detik di web, ~120 detik di APK). Bukan streaming tick.

**Menguji endpoint:** `https://nama-situs.netlify.app/api/candles?tf=M5` akan mengembalikan HTTP 401 jika variabel lengkap tetapi kode akses tidak dikirim. Itu **normal**. HTTP 503 artinya konfigurasi variabel belum selesai. Candle live memerlukan paket Twelve Data dengan hak akses XAU/USD.

**Keamanan dan kuota:** kunci Twelve Data hanya berada di Netlify. Kode akses aplikasi dikirim via header HTTPS `X-XAU-Access-Token` dan disimpan dalam localStorage di perangkat; jangan menyamakannya dengan penyimpanan rahasia berstandar tinggi. Endpoint backend membatasi simbol/timeframe, memeriksa kode akses, membatasi CORS untuk origin APK dan Netlify yang sesuai, serta cache sementara per instance. Penggunaan publik besar tetap memerlukan rate limiting terpusat dan monitoring kuota.

## Deploy statis tanpa serverless (GitHub Pages, Cloudflare Pages static, dll.)

Bisa untuk **mode demo, semua drawing tools dan quote spot dari Gold API**, tetapi **mode candle asli tidak bisa aktif** tanpa membuat adapter backend `/api/candles` sendiri. Ini disengaja agar API key tidak bocor di kode browser. Deploy di root domain untuk jalur PWA `/`. Pada subpath GitHub Pages service worker dan script paths harus diubah menjadi relatif.

## Fitur drawing

- Garis (trendline, horizontal, vertical, ray, extended)
- Fibonacci retracement (0, 0.236, 0.382, 0.5, 0.618, 0.786, 1, 1.618)
- Persegi (rectangle)
- Long & Short Position (entry, SL, TP, RR)
- Path/jalur dengan multi titik
- Parallel Channel
- Warna custom, solid/dashed/dotted, stroke width, fill opacity
- Selection + drag, handles, lock, duplicate, delete, undo/redo
- Workspace terpisah, localStorage autosave, backup JSON, screenshot PNG
- M1/M5/M15/M30/H1/H4/D1, zoom/pan, crosshair preview OHLC, fullscreen, mobile toolbar kiri/kanan

## Status data dan keterbatasan

- Candle saat demo **SELALU simulasi**, dan ada label/watermark.
- Quote spot header diambil dari **Gold API**; mungkin berbeda dari quote Twelve Data atau broker. Harga bukan nasihat investasi.
- Mode market asli menggunakan Twelve Data REST API (bukan WebSocket/tick streaming) melalui Netlify, kira-kira refresh tiap menit, dengan cache 60 detik. Ketersediaan interval tergantung langganan. Status bukan jaminan waktu real-time tick.
- Drawing disimpan di localStorage perangkat/browser, belum sinkron cloud. Backup JSON disediakan.
- Tidak ada eksekusi transaksi; Long/Short hanya alat visual perencanaan.
- Performa & akurasi uji produksi terutama untuk multi-touch Android, gap data akhir pekan, dan objek paralel perlu pengujian lebih luas pada perangkat sasaran.
- Service worker men-cache file aplikasi, tetapi harga asli tetap membutuhkan koneksi online.

## Struktur

- `index.html`: UI dan layout
- `styles.css`: responsif desktop/mobile
- `app.js`: mesin chart Canvas + drawing engine, data fetch, penyimpanan, event
- `netlify/functions/candles.js`: proxy Twelve Data server-side
- `netlify.toml`: build + redirect function
- `sw.js`, `manifest.webmanifest`, `assets/icon.svg`: PWA

## Sumber & lisensi

- Quote: https://gold-api.com/docs (endpoint live tanpa API key; CORS didukung).
- Candle: https://twelvedata.com/docs (API key pemilik aplikasi).
- Kode aplikasi proyek ini ditulis khusus untuk blueprint XAU Chart Studio. Tidak menggunakan library/chart dari TradingView.