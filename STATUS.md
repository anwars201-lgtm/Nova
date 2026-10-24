# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **6 — memori lokal + injeksi system prompt** (selesai, menunggu uji di device)
Tahap: personalisasi dasar berjalan, skema database v2

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
| SHA-256 | `e3e81a8ff0357fa084a3784ac4b9dd31912c634893f101a8b17d448149dd8c85` |
| Ukuran | 11.749.266 byte |

## Selesai (terverifikasi)

- [x] Skema v2: tabel `memories(id, content, created_at, active)` + migrasi `onUpgrade` bertahap
- [x] `MemoryRepository` — tambah, baca semua/aktif, ubah, aktif-nonaktif, hapus, hitung
- [x] `MemoryText` — rapikan spasi, tolak kosong dan >500 karakter (4 tes)
- [x] `PromptBuilder` — urutan manual lalu memori, batas 50 entri / 4.000 karakter (7 tes)
- [x] `AppSettings` — `systemPrompt` dan `memoryEnabled` (saklar global, default aktif)
- [x] Layar **Memori**: daftar, tambah, ubah, hapus, saklar per entri, saklar global, peringatan batas
- [x] Kolom **System prompt** di Pengaturan + pintasan "Kelola memori"
- [x] Injeksi system prompt di setiap kirim; bila saklar mati, memori tidak dibaca sama sekali
- [x] `SelfTest` bertambah 5 pemeriksaan memori (total 13) termasumber urutan prompt
- [x] Unit test JVM: **49 tes, 0 gagal**
- [x] Build lulus: `BUILD SUCCESSFUL in 1m 40s`
- [x] Signature v2 terverifikasi

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 1m 40s
→ 49 tes, 0 gagal
→ app/build/outputs/apk/debug/app-debug.apk
→ sha256 e3e81a8f...49dd8c85
→ apksigner verify: Verifies (v2)
```

## Tidak selesai (sengaja)

- Embedding atau pencarian relevansi tidak dipakai (keputusan siklus 6).
- Reasoning effort belum ada; ditunda agar siklus ini fokus pada memori.
- Belum ada retry per pesan, hapus pesan, atau pengaturan temperature/max tokens.
- Belum ada ekspor/impor memori.

## Catatan

- Migrasi v1 → v2 hanya menambah tabel; chat lama tidak disentuh. Uji nyata: pasang APK ini
  di atas instalasi siklus 5, riwayat harus tetap ada dan diagnostik memori lulus.
- Batas karakter menghitung seluruh system prompt (manual + memori), supaya penambahan
  prompt manual otomatis mengurangi ruang memori.
- Entri yang tidak kebagian tempat dilewati utuh, bukan dipotong di tengah kalimat.

## Berikutnya (antrean)

| Siklus | Isi | Syarat |
|---|---|---|
| 7 | Reasoning effort + parameter model (temperature, max tokens) | siklus 6 |
| 8 | **Release hardening** (D9): R8, keystore rilis, migrasi teruji | sebelum rilis pertama |
| 9+ | Perubahan tampilan dan fitur tambahan | kapan saja setelah rilis |

## Belum diputuskan

- Apakah reasoning effort per model perlu dibaca dari daftar `/models` atau diisi manual
- Apakah memori perlu dikelompokkan (misal kategori) ketika jumlahnya membesar
