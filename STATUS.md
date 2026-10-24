# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **3 — pengaturan koneksi** (selesai, menunggu uji di device)
Tahap: koneksi bisa diatur, belum ada chat

## Posisi sekarang

`Nova` punya database lokal, penyimpanan token terenkripsi, layar Pengaturan berisi
base URL + API key, dan tombol "Uji koneksi" yang memanggil endpoint daftar model.

Tidak ada backend sendiri (D7). Hanya endpoint **https** yang diterima (D8).

| Hal | Nilai |
|---|---|
| Nama kerja | Nova |
| Package debug | `dev.local.nova.debug` |
| Versi | 0.1.0 |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| SHA-256 | `fd3bb1e2c4823417387a2c2a6548d11b04f9151c37add1ba710d24f537234004` |
| Ukuran | 11.642.274 byte |

## Selesai (terverifikasi)

- [x] `Endpoint.parse` — normalisasi URL, wajib https, tolak userinfo/query/spasi, port dipertahankan
- [x] `Endpoint.modelsUrl` — menambahkan `/v1` hanya bila belum ada
- [x] `Endpoint.maskKey` — key tidak pernah ditampilkan utuh
- [x] `ApiClient.listModels` — `HttpURLConnection`, timeout 15/20 detik, pesan galat per kode HTTP
- [x] `AppSettings` — base URL di prefs, API key di `SecureStore`
- [x] `SettingsScreen` — dua kolom, tombol Simpan, Uji koneksi, status, indikator proses
- [x] Navigasi Home ↔ Pengaturan lewat `rememberSaveable`
- [x] Unit test JVM: **21 tes, 0 gagal** (EndpointTest 11, WireValueTest 4, ThreadTitleTest 6)
- [x] Build lulus: `BUILD SUCCESSFUL in 1m 22s`, 44 task
- [x] Signature v2 terverifikasi

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 1m 22s
→ EndpointTest 11, WireValueTest 4, ThreadTitleTest 6 — 0 gagal
→ app/build/outputs/apk/debug/app-debug.apk
→ sha256 fd3bb1e2...234004
→ apksigner verify: Verifies (v2)
```

## Tidak selesai (sengaja)

- Belum ada layar chat, pengiriman pesan, atau streaming.
- Uji koneksi belum dijalankan terhadap provider nyata (butuh key dari pengguna).
- Migrasi skema masih belum ada (VERSION 1).

## Catatan

- `org.json` dan `HttpURLConnection` dipakai dari SDK, bukan pustaka tambahan. Build di perangkat ini mahal, jadi menambah dependensi harus punya alasan kuat.
- `Endpoint` sengaja bebas dari kelas Android agar bisa diuji di JVM.
- Semua field teks pada layar Pengaturan memakai `remember`, bukan `rememberSaveable`: nilai sensitif tidak perlu masuk Bundle.
- `MainActivity` memakai rute string sederhana, belum memakai Navigation Compose.

## Berikutnya (usulan siklus 4)

1. Layar chat: daftar thread, buat thread baru, judul otomatis dari pesan pertama
2. Kirim pesan non-streaming: simpan pesan user, panggil chat completions, simpan balasan
3. Tampilkan riwayat dari database setelah app ditutup
4. Uji: percakapan bertahan setelah restart

## Antrean siklus

| Siklus | Isi | Syarat |
|---|---|---|
| 4 | Layar chat + kirim pesan non-streaming | koneksi sudah diuji |
| 5 | Streaming token + tombol batalkan | siklus 4 |
| 6 | Personalisasi: system prompt, memori, reasoning effort | siklus 5 |
| 7 | **Release hardening** (D9): R8, keystore rilis, jalur migrasi teruji | sebelum rilis pertama |
| 8+ | Perubahan tampilan, fitur tambahan, penyempurnaan | kapan saja setelah rilis |

Perubahan tampilan dan fitur baru tetap boleh setelah rilis; yang dikunci hanya identitas, signature, dan migrasi (D9).

## Belum diputuskan

- Model default: dipilih manual atau diambil dari hasil uji koneksi
- Penanganan galat saat kirim pesan: dialog atau pesan sistem di dalam thread
- Apakah streaming masuk siklus 5 atau langsung di siklus 4
