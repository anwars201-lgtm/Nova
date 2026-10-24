# Status proyek

Diperbarui: 2026-10-24
Siklus terakhir: **4 — layar chat + kirim pesan non-streaming** (selesai, menunggu uji di device)
Tahap: percakapan bisa dikirim dan disimpan, belum streaming

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
| SHA-256 | `20dabc1927c9db087f24a0c36736da61864502aafe8b6b66e192368ba6529977` |
| Ukuran | 11.711.187 byte |

## Selesai (terverifikasi)

Siklus 1-3 sebelumnya: kerangka Compose, SQLite + SecureStore (8/8 diagnostik di device), layar Pengaturan https dengan uji koneksi.

Siklus 4:

- [x] `ChatClient.complete` — POST chat completions, `stream: false`, timeout baca 120 detik, galat per kode HTTP
- [x] `Endpoint.chatUrl` — versi ditambahkan sekali saja
- [x] `MessageText.prepare` — rapikan input, tolak kosong dan >32.000 karakter
- [x] Kolom **Model** manual di Pengaturan, disimpan di prefs
- [x] `HomeScreen` → daftar thread + tombol "Chat baru"
- [x] `ChatScreen` → riwayat dari database, kirim, tampilkan galat, auto-scroll
- [x] Judul thread otomatis dari pesan pertama (`ThreadTitle.derive`)
- [x] Navigasi Home ↔ Chat ↔ Pengaturan lewat rute string
- [x] Unit test JVM: **26 tes, 0 gagal** (Endpoint 12, MessageText 4, Wire 4, ThreadTitle 6)
- [x] Build lulus: `BUILD SUCCESSFUL in 1m 49s`
- [x] Signature v2 terverifikasi

### Bukti

```
bash scripts/build.sh assembleDebug testDebugUnitTest
→ BUILD SUCCESSFUL in 1m 49s
→ 26 tes, 0 gagal
→ app/build/outputs/apk/debug/app-debug.apk
→ sha256 20dabc19...6529977
→ apksigner verify: Verifies (v2)
```

## Tidak selesai (sengaja)

- Streaming token belum ada (siklus 5).
- Belum ada tombol batalkan, retry, atau hapus pesan.
- Belum ada pemilihan model dari daftar hasil uji koneksi — model diketik manual (keputusan pengguna).
- Migrasi skema masih belum ada (VERSION 1).

## Catatan

- `scripts/build.sh` sekarang memakai `pipefail`; sebelumnya kegagalan Gradle tertutup oleh `tail` dan skrip melaporkan exit 0.
- Tiga kegagalan kompilasi di siklus ini: `ExtendedFloatingActionButton(text = {...})` tidak cocok (pakai lambda konten), `mutableStateOf` tanpa impor `setValue`, dan `produceState(initialValue = null)` yang menyimpulkan tipe `Nothing` (perlu `produceState<Thread?>`).
- Riwayat selalu dibaca ulang dari database setelah kirim, bukan dari state memori.

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
| 4 | ~~Layar chat + kirim pesan non-streaming~~ (selesai) | — |
| 5 | Streaming token + tombol batalkan | siklus 4 selesai |
| 6 | Personalisasi: system prompt, memori, reasoning effort | siklus 5 |
| 7 | **Release hardening** (D9): R8, keystore rilis, jalur migrasi teruji | sebelum rilis pertama |
| 8+ | Perubahan tampilan, fitur tambahan, penyempurnaan | kapan saja setelah rilis |

Perubahan tampilan dan fitur baru tetap boleh setelah rilis; yang dikunci hanya identitas, signature, dan migrasi (D9).

## Belum diputuskan

- ~~Model default: manual atau otomatis~~ — manual (keputusan pengguna)
- Penanganan galat saat kirim pesan: dialog atau pesan sistem di dalam thread
- Penanganan galat: banner saat ini; apakah perlu tombol "coba lagi" per pesan
