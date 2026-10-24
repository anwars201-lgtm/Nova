# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **5 — pemilih model dari provider + streaming** (selesai, menunggu uji di device)
Tahap: chat streaming berjalan, model dipilih dari daftar provider

## Posisi sekarang

`Nova` adalah klien chat Android tanpa backend. Pengguna mengisi endpoint https + API key,
memilih model dari daftar yang diambil langsung dari provider, lalu mengobrol dengan
balasan yang mengalir token demi token.

| Hal | Nilai |
|---|---|
| Nama kerja | Nova |
| Package debug | `dev.local.nova.debug` |
| Versi | 0.1.0 |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| SHA-256 | `486301e751d78442057d6f79ac3265c450b7d09fde3bc966c5a4ec65e57f30f8` |
| Ukuran | 11.737.993 byte |

## Selesai (terverifikasi)

Siklus 1–4: kerangka Compose, SQLite + SecureStore (8/8 diagnostik di device), layar
Pengaturan https dengan uji koneksi, layar chat dengan kirim non-streaming.

Siklus 5:

- [x] `SseParser` — pembaca `data:` baris SSE, penanda `[DONE]`, galat tengah stream (8 tes)
- [x] `ModelList` — normalisasi (rapi, unik, terurut) dan pencarian tanpa peduli huruf (4 tes)
- [x] `ChatClient.stream` — POST `stream: true`, fallback ke JSON biasa bila server mengabaikan SSE
- [x] Pembatalan lewat `Job.cancel()` + `ensureActive()`, koneksi ditutup di `finally`
- [x] Dialog "Pilih model": muat dari provider, kolom cari, klik untuk memilih
- [x] Kolom model tetap bisa diketik manual bila provider tidak punya daftar
- [x] `ChatScreen`: teks mengalir saat masuk, tombol **Stop** saat berjalan
- [x] Hasil akhir disimpan sebagai satu pesan; teks sebagian saat Stop disimpan status `FAILED`
- [x] Unit test JVM: **38 tes, 0 gagal** (Endpoint 12, SseParser 8, ThreadTitle 6, MessageText 4, ModelList 4, Wire 4)
- [x] Build lulus: `BUILD SUCCESSFUL in 2m 3s`
- [x] Signature v2 terverifikasi

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 2m 3s
→ 38 tes, 0 gagal
→ app/build/outputs/apk/debug/app-debug.apk
→ sha256 486301e7...57f30f8
→ apksigner verify: Verifies (v2)
```

## Tidak selesai (sengaja)

- Belum ada retry per pesan, hapus pesan, atau salin teks.
- Belum ada `max_tokens`, temperature, atau parameter lanjutan.
- Belum ada system prompt / memori (siklus 6).
- Migrasi skema masih belum ada (VERSION 1).

## Catatan

- Streaming menulis ke database **sekali** di akhir, bukan per token. Isi sementara ada di memori.
- `testImplementation("org.json:json:20240303")` ditambahkan agar `SseParser` bisa diuji di JVM; aplikasi tetap memakai `org.json` dari Android SDK.
- SSE diuji dengan `org.json` sungguhan di JVM, bukan tiruan.

## Berikutnya (antrean)

| Siklus | Isi | Syarat |
|---|---|---|
| 6 | Personalisasi: system prompt, memori, reasoning effort | siklus 5 |
| 7 | **Release hardening** (D9): R8, keystore rilis, jalur migrasi teruji | sebelum rilis pertama |
| 8+ | Perubahan tampilan dan fitur tambahan | kapan saja setelah rilis |

## Belum diputuskan

- Apakah memori disimpan lokal di SQLite atau sebagai bagian system prompt yang dikirim setiap permintaan
- Penanganan galat per pesan: banner (sekarang) atau tombol "coba lagi"
- Apakah perlu pilihan temperature/max tokens di Pengaturan
