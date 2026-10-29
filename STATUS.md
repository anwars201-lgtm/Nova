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
| SHA-256 | `b58e4a2314cae03f...` (lengkap di build.log) |
| Ukuran | 11.796.299 byte |

## Selesai (terverifikasi)

Siklus 1-6: kerangka Compose, SQLite + SecureStore (13/13 diagnostik di device), pengaturan https, chat streaming, memori lokal + injeksi system prompt.

Siklus 7:

- [x] `ChatOptions` + `ChatParams` — validasi max tokens (1..200.000), field hanya dikirim bila diisi (11 tes)
- [x] `ChatClient.complete` dan `stream` menerima opsi; payload memuat `reasoning_effort` dan `max_tokens` bila ada
- [x] Pengaturan: pemilih effort (Tidak dipakai / low / medium / high / Nilai lain…) + kolom max tokens
- [x] Nilai effort khusus bisa diketik; default tidak mengirim parameter sama sekali
- [x] Galat 400 kini menyebut kemungkinan nama model, effort, atau max tokens
- [x] Unit test JVM: **60 tes, 0 gagal** (ChatParams 11, sisanya 49)
- [x] Build lulus: `BUILD SUCCESSFUL in 1m 1s`
- [x] Signature v2 terverifikasi
- [x] APK baru: 11.796.299 byte, sha256 `b58e4a2314cae03f...`

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 1m 1s
→ EXIT=0
→ 60 tes, 0 gagal
→ sha256 b58e4a2314cae03f...
→ apksigner verify: Verifies (v2)
```

## Tidak selesai (sengaja)

- Embedding atau pencarian relevansi tidak dipakai (keputusan siklus 6).
- Temperature tidak ditambahkan (keputusan pengguna: effort + max tokens saja).
- Belum ada retry per pesan atau hapus pesan.
- Belum ada ekspor/impor memori.

## Catatan

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
