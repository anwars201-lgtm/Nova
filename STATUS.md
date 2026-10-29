# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **7 — reasoning effort & max tokens** (selesai, menunggu uji di device)
Tahap: parameter model bisa dikendalikan pengguna

## Posisi sekarang

`Nova` adalah klien chat Android tanpa backend. Endpoint https + API key milik pengguna,
model dipilih dari daftar provider, balasan mengalir (SSE), dan setiap permintaan
menyertakan system prompt manual serta blok memori aktif dari SQLite.

| Hal | Nilai |
|---|---|
| Nama kerja | Nova |
| Package debug | `dev.local.nova.debug` |
| Versi | 0.1.0 |
| Skema database | **v2** (tabel `memories` ditambahkan) |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| SHA-256 | `5ad8ddbfb342917f...` (lengkap di build.log) |
| Ukuran | 11.828.304 byte |

## Selesai (terverifikasi)

Siklus 1-6: kerangka Compose, SQLite + SecureStore (13/13 diagnostik di device), pengaturan https, chat streaming, memori lokal + injeksi system prompt.

Siklus 7:

- [x] `ChatOptions` + `ChatParams` — validasi max tokens (1..200.000), field hanya dikirim bila diisi (11 tes)
- [x] `ChatClient.complete` dan `stream` menerima opsi; payload memuat `reasoning_effort` dan `max_tokens` bila ada
- [x] Pengaturan: pemilih effort (Tidak dipakai / low / medium / high / Nilai lain…) + kolom max tokens
- [x] Nilai effort khusus bisa diketik; default tidak mengirim parameter sama sekali
- [x] Galat 400 kini menyebut kemungkinan nama model, effort, atau max tokens
- [x] `HttpErrorText` — galat HTTP diterjemahkan, isi balasan provider ikut ditampilkan (9 tes)
- [x] Stream kosong kini menampilkan cuplikan balasan mentah provider
- [x] Unit test JVM: **69 tes, 0 gagal**
- [x] Build lulus: `BUILD SUCCESSFUL in 1m 1s`
- [x] Signature v2 terverifikasi
- [x] APK baru: 11.828.304 byte, sha256 `5ad8ddbfb342917f...`

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 55s
→ EXIT=0
→ 69 tes, 0 gagal
→ sha256 5ad8ddbfb342917f...
→ apksigner verify: Verifies (v2)
```

## Tidak selesai (sengaja)

- Embedding atau pencarian relevansi tidak dipakai (keputusan siklus 6).
- Temperature tidak ditambahkan (keputusan pengguna: effort + max tokens saja).
- Belum ada retry per pesan atau hapus pesan.
- Belum ada ekspor/impor memori.

## Catatan

- Klaim "provider menolak reasoning_effort" belum terbukti dan kemungkinan salah.
  Aplikasi lain berhasil memakai `reasoning_effort: "high"` ke model yang sama, jadi
  parameternya didukung provider. Penyebab kegagalan Nova belum diketahui; bukti
  dikumpulkan dari balasan mentah provider (ditampilkan sejak build `5ad8ddbf`).
  Dugaan sementara: (a) isi balasan datang di `delta.reasoning_content` yang belum dibaca
  `SseParser`, atau (b) ada perbedaan lain pada payload Nova.
- Migrasi v1 → v2 hanya menambah tabel; chat lama tidak disentuh. **Sudah diuji di device:**
  APK dipasang di atas instalasi siklus 5, riwayat chat tetap ada.
- Diagnostik sempat memberi 1 gagal palsu: pemeriksaan "sisa data uji" menghitung seluruh tabel thread, sehingga 8 chat pengguna dianggap sisa. Sudah diperbaiki dengan prefiks judul `[self-test] `; 13/13 lulus setelah perbaikan.
- Batas karakter menghitung seluruh system prompt (manual + memori), supaya penambahan
  prompt manual otomatis mengurangi ruang memori.
- Entri yang tidak kebagian tempat dilewati utuh, bukan dipotong di tengah kalimat.

## Berikutnya (antrean)

| Siklus | Isi | Syarat |
|---|---|---|
| 7 | ~~Reasoning effort + max tokens~~ (selesai) | — |
| 8 | **Release hardening** (D9): R8, keystore rilis, migrasi teruji | sebelum rilis pertama |
| 9+ | Perubahan tampilan dan fitur tambahan | kapan saja setelah rilis |

## Belum diputuskan

- Apakah memori perlu dikelompokkan ketika jumlahnya membesar
- Apakah perlu indikator pemakaian token dari respons (`usage`) ditampilkan
