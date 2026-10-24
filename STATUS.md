# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: belum ada siklus fitur
Tahap: persiapan selesai, belum mulai menulis aplikasi

## Posisi sekarang

Belum ada aplikasi. Yang sudah ada: toolchain yang terbukti dan target pembangunan yang sudah dibedah.

Terbukti jalan di device ini:

- Build APK Kotlin + Compose (AGP 9.4.1, Gradle 9.8.2, compileSdk 36)
- Sign v2, terpasang, dan tampil di HP (`out/composetest`)
- Build APK Java tanpa Gradle (`out/buildtest`)
- Decompile APK dengan jadx (`out/talknova` = hasil bedah TalkNova)

## Spesifikasi target

Hasil bedah `TalkNova 0.1.17` (`site.talknova.app`), lihat `DECISIONS.md` untuk batasan peniruan.

Fitur yang jadi acuan: chat streaming, katalog model multi-provider (vision, web search, reasoning effort), personalisasi (memori, skills, custom instructions), generasi gambar dan video, sinkronisasi, remote coding session ke desktop, update bertanda tangan.

## Berikutnya

1. Sepakati ruang lingkup siklus 1
2. Kerangka proyek: struktur Gradle, tema, navigasi dasar, build script
3. Lapisan data: SQLite threads/messages, penyimpanan kredensial terenkripsi
4. Klien API: base URL, auth, daftar model
5. Streaming chat

## Belum diputuskan

- Nama dan package aplikasi
- Basis URL backend: pakai milik sendiri, atau mock lokal dulu
- Apakah memori/skills dibuat server-side seperti target, atau lokal dulu
- Apakah remote coding session masuk ruang lingkup awal

## Catatan

- Wrapper `jadx`, `d8`, `apksigner` di repo rusak (hardcode Termux); resep pengganti ada di memori lingkungan.
- Build pertama Kotlin+Compose butuh ~12 menit karena mengunduh dependensi; incremental jauh lebih cepat.
- Folder `out/` tidak masuk git; isinya artefak yang bisa dibuat ulang.
