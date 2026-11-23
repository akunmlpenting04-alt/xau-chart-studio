# XAU Chart Studio

Custom web app / PWA untuk analisis chart XAU/USD. Tidak memakai embed TradingView. Mesin chart, semua drawing tools, editing handles, auto-save, toolbar kiri/kanan, dan eksport dibuat mandiri menggunakan HTML Canvas + JavaScript, tanpa langkah `npm install`.

## Cara jalankan di komputer

Jalankan server statis di folder ini:

```bash
python -m http.server 8080
```

Buka `http://localhost:8080`. **Mode DEMO** langsung berfungsi dan menampilkan candle simulasi ber-watermark, supaya alat gambar dapat dicoba tanpa API. Header berusaha mengambil harga spot XAU dari https://api.gold-api.com/price/XAU (harga asli, diperbarui penyedia sekitar 30 detik; header terpisah dari candle demo).

## Deploy ke Netlify — cara paling mudah supaya candle market asli aktif

1. Upload folder **XAU-Chart-Studio** ke repo GitHub (isi folder sebagai root repo).
2. Netlify → Add new site → Import from Git → pilih repo ini. Build command: **kosong**; Publish directory: **.**. Konfigurasi `netlify.toml` sudah tersedia.
3. Buat API key di https://twelvedata.com/ dan pastikan paket mengizinkan **XAU/USD** dan interval yang digunakan.
4. Di Netlify → Site configuration → Environment variables → Add variable:
   - Key: `TWELVE_DATA_API_KEY`
   - Value: key Twelve Data milikmu (jangan taruh di kode/commit Git).
5. Deploy ulang situs agar Netlify Function dapat membaca variabel.
6. Buka situs, Pengaturan → **Mode Market Asli** → Hubungkan.
7. Pilih timeframe; chart memakai historical OHLC Twelve Data dan refresh berkala ~65 detik (tergantung paket). Status dan error ditampilkan eksplisit.

**Penting:** Serverless function `/api/candles` hanya menerima simbol tetap XAU/USD dan timeframe valid, tetapi endpoint publik dapat dipanggil pengunjung. Untuk situs publik dengan trafik tinggi tambahkan autentikasi, edge cache, pembatasan request, dan cek ketentuan lisensi redistribusi data Twelve Data. Limit akun Twelve Data tetap berlaku; ini bukan tick-by-tick feed (kecuali dikembangkan dengan WebSocket yang didukung paket API).

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