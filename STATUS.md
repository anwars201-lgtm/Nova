# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **2 — lapisan data & penyimpanan rahasia** (selesai, menunggu uji di device)
Tahap: fondasi data siap, belum ada API

## Posisi sekarang

Aplikasi `Nova` punya database lokal, repository thread/pesan, dan penyimpanan token terenkripsi.
Layar beranda menampilkan kartu diagnostik yang menjalankan pemeriksaan nyata di perangkat.

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

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 2m 30s
→ app/build/test-results/testDebugUnitTest: 10 tes, 0 gagal
→ app/build/outputs/apk/debug/app-debug.apk
→ sha256 3cdbbe35...b897ea
```

## Tidak selesai (sengaja)

- Pemeriksaan `SelfTest` di device belum dikonfirmasi pengguna untuk siklus ini.
- Belum ada migrasi skema (VERSION masih 1).
- Belum ada API, auth, atau layar chat.

## Catatan

- SQLite dan AndroidKeyStore tidak bisa diuji di JVM pada lingkungan ini (bionic, tanpa emulator). Karena itu kebenarannya dibuktikan lewat `SelfTest` di perangkat, bukan unit test.
- `scripts/build.sh` sekarang menerima beberapa task Gradle sekaligus (`assembleDebug testDebugUnitTest`).
- `buildConfig = true` diaktifkan agar layar diagnostik bisa dibatasi ke build debug.
- `Column` tidak punya `contentPadding`; padding lewat modifier.

## Berikutnya (usulan siklus 3)

1. Klien API: base URL, header `Authorization`, endpoint `/v1/models`
2. DTO + parsing JSON (kotlinx.serialization)
3. Layar chat minimal: daftar pesan dari database, kirim pesan tanpa streaming dulu
4. Pengujian: pesan tersimpan dan terbaca setelah app ditutup

## Belum diputuskan

- Basis URL backend: milik sendiri atau mock lokal
- Format kredensial: API key saja atau juga OAuth
- Apakah memori dan skills dibuat server-side atau lokal
