# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **1 — kerangka aplikasi** (selesai)
Tahap: kerangka terpasang, belum ada fitur

## Posisi sekarang

Aplikasi `Nova` sudah ada, bisa di-build, dan bisa dipasang. Isinya baru satu layar beranda statis.

| Hal | Nilai |
|---|---|
| Nama kerja | Nova |
| Package debug | `dev.local.nova.debug` |
| Package release | `dev.local.nova` |
| Versi | 0.1.0 |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| SHA-256 | `79f9d73f879a8a1f5744d6473b54379110a2409352ce0eb36fa165910a48d864` |
| Ukuran | 11.567.262 byte |

## Selesai (terverifikasi)

- [x] Struktur Gradle: root + modul `app`, plugin AGP 9.4.1 dan Compose 2.4.21
- [x] Tema Material3 dengan dukungan dynamic color dan mode gelap
- [x] Layar beranda `HomeScreen` berisi daftar langkah berikutnya
- [x] `scripts/build.sh` — satu perintah untuk build dan hash
- [x] Build lulus: `BUILD SUCCESSFUL in 1m 16s`, 36 task
- [x] Signature v2 terverifikasi (`apksigner verify` → Verifies)
- [x] `aapt2 dump badging` membaca package, minSdk 26, targetSdk 36

### Bukti

```
bash scripts/build.sh assembleDebug
→ BUILD SUCCESSFUL in 1m 16s
→ app/build/outputs/apk/debug/app-debug.apk
→ sha256 79f9d73f...a48d864
```

## Tidak selesai (sengaja)

- Tidak ada data layer, API, atau navigasi. Itu siklus 2 dan seterusnya.
- Ikon launcher masih ikon bawaan Android.
- Build release (R8) belum diuji.

## Catatan

- Gradle tidak butuh `org.gradle.daemon=true`; daemon dimatikan karena native services juga dimatikan. Konsekuensi: build selalu cold start, ~1–2 menit.
- `Column` tidak punya parameter `contentPadding`; padding dipasang lewat modifier. Ini pernah menggagalkan build pertama.
- `local.properties` berisi `sdk.dir` dan tidak masuk git.

## Berikutnya (usulan siklus 2)

1. Lapisan data: skema SQLite `threads` dan `messages` + `NovaDatabase` (tanpa Room, sesuai D5)
2. Penyimpanan kredensial AES-GCM di AndroidKeyStore (D6)
3. Uji: unit test kecil untuk CRUD thread dan pesan

## Belum diputuskan

- Basis URL backend: milik sendiri atau mock lokal
- Apakah memori dan skills dibuat server-side seperti target, atau lokal
- Apakah remote coding session masuk ruang lingkup awal
- Nama final dan package final (sebelum rilis)
