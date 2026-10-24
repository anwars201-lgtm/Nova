# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **2 — lapisan data & penyimpanan rahasia** (selesai, terverifikasi di device)
Tahap: fondasi data siap, belum ada klien API

## Posisi sekarang

Aplikasi `Nova` punya database lokal, repository thread/pesan, dan penyimpanan token terenkripsi.
Layar beranda menampilkan kartu diagnostik yang menjalankan pemeriksaan nyata di perangkat.

Arah produk berubah: **tanpa backend sendiri** (D7). Pengguna memasukkan base URL kompatibel OpenAI
dan API key sendiri. Tidak ada akun, tidak ada sinkronisasi.

| Hal | Nilai |
|---|---|
| Nama kerja | Nova |
| Package debug | `dev.local.nova.debug` |
| Versi | 0.1.0 |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| SHA-256 | `3cdbbe3504cab3bf4bbe46aae2ba4d254fd321f320b4f0e8782ebeeb7eb897ea` |
| Ukuran | 11.604.270 byte |

## Selesai (terverifikasi)

- [x] Model: `Thread`, `Message`, `MessageRole`, `MessageStatus`
- [x] `ThreadTitle.derive` — judul dari pesan pertama, potong di batas kata
- [x] `NovaDatabase` — SQLite v1: tabel `threads`, `messages`, `app_meta` + indeks; foreign key diaktifkan
- [x] `ThreadRepository` — buat/baca/rename/pin/hapus thread, tambah/baca/hitung pesan, cascade
- [x] `SecureStore` — AES-256-GCM dengan kunci di AndroidKeyStore, ciphertext di prefs
- [x] `SelfTest` — 8 pemeriksaan di perangkat (tabel, insert, urutan, cascade, enkripsi, hapus, kebersihan)
- [x] `DiagnosticsCard` di layar beranda, hanya build debug
- [x] Unit test JVM: **10 tes, 0 gagal** (`ThreadTitleTest` 6, `WireValueTest` 4)
- [x] Build lulus: `BUILD SUCCESSFUL in 2m 30s`, 44 task
- [x] Signature v2 terverifikasi
- [x] Diagnostik di perangkat: **8/8 lulus**, mode gelap aktif (konfirmasi pengguna, 2026-10-24)

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 2m 30s
→ app/build/test-results/testDebugUnitTest: 10 tes, 0 gagal
→ app/build/outputs/apk/debug/app-debug.apk
→ sha256 3cdbbe35...b897ea
```

Di perangkat: "Diagnostik: 8 pemeriksaan lulus" — semua baris centang.

## Tidak selesai (sengaja)

- Belum ada migrasi skema (VERSION masih 1).
- Belum ada klien API, pengaturan, atau layar chat.
- Fitur sinkronisasi dan akun dibatalkan oleh D7.

## Catatan

- SQLite dan AndroidKeyStore tidak bisa diuji di JVM pada lingkungan ini (bionic, tanpa emulator). Kebenarannya dibuktikan lewat `SelfTest` di perangkat.
- `scripts/build.sh` menerima beberapa task Gradle sekaligus (`assembleDebug testDebugUnitTest`).
- `buildConfig = true` diaktifkan agar layar diagnostik bisa dibatasi ke build debug.
- `Column` tidak punya `contentPadding`; padding lewat modifier.

## Berikutnya (usulan siklus 3)

Siklus pengaturan koneksi — belum ada chat, hanya memastikan koneksi bisa diatur dan diuji.

1. Layar Pengaturan: kolom base URL dan API key, disimpan lewat `SecureStore`
2. Normalisasi input: buang trailing slash, tolak skema selain http/https
3. Tombol "Uji koneksi" memanggil endpoint daftar model standar OpenAI
4. Tampilkan hasil: jumlah model, contoh beberapa nama, atau pesan galat yang bisa dibaca
5. API key disimpan terenkripsi dan hanya ditampilkan tersamar (misal `sk-…abcd`)

## Belum diputuskan

- Kebijakan HTTP: apakah endpoint `http://` (LAN, LM Studio, Ollama) diizinkan atau wajib https
- Apakah memori dan skills dibuat lokal atau tidak sama sekali
- Dukungan provider yang tidak menyediakan endpoint daftar model
