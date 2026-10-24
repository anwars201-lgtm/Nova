# Keputusan

Hanya keputusan yang bertahan lama. Satu keputusan = satu blok, berisi apa dan kenapa. Jangan dihapus; kalau berubah, tambahkan keputusan baru yang membatalkannya.

---

## D1 — Membangun ulang fungsi, bukan menyalin karya

**Keputusan:** Aplikasi baru ditulis dari nol. Yang diambil dari hasil bedah hanya pemahaman fungsi, struktur data, dan kontrak API. Kode, aset, ikon, teks, dan nama merek tidak disalin.

**Alasan:** Meniru fungsi itu sah; menyalin karya orang lain bukan. Ini juga menghindari membawa bug dan utang teknis yang tidak dipahami.

**Konsekuensi:** Setiap layar akan tampak berbeda dari target. Ukuran keberhasilan adalah fungsinya setara, bukan tampilannya identik.

---

## D2 — Kotlin + Jetpack Compose

**Keputusan:** Stack UI Kotlin dengan Compose Material3.

**Alasan:** Sama dengan target, sehingga pemetaan fitur ke komponen langsung. Sudah terbukti bisa di-build dan berjalan di device ini.

**Konsekuensi:** Terikat pada kombinasi versi yang sudah diverifikasi: AGP 9.4.1, Gradle 9.8.2, compileSdk 36, Compose BOM 2026.06.01, activity-compose 1.12.4. Menaikkan compileSdk ke 37 atau BOM 2026.09 memerlukan platform API 37.

---

## D3 — Build lokal di device, tanpa CI

**Keputusan:** Seluruh build dilakukan di device ini. Tidak memakai GitHub Actions atau mesin lain.

**Alasan:** Terbukti lengkap sampai APK terpasang dan tampil. Menambah CI berarti menambah satu sistem yang harus dijaga tanpa manfaat yang jelas untuk proyek satu orang.

**Konsekuensi:** Beberapa flag wajib (`org.gradle.native=false`, `-Djava.io.tmpdir`, `android.aapt2FromMavenOverride`); tanpanya build gagal. Detail di `.harness/memory/workspace-capabilities.md`.

---

## D4 — Siklus kerja dengan STATUS.md

**Keputusan:** Pekerjaan dibagi per siklus. Setiap siklus berakhir dengan verifikasi, pembaruan `STATUS.md`, dan satu commit.

**Alasan:** Konteks percakapan akan habis. Status tertulis ditambah commit memungkinkan lanjut di thread baru tanpa kehilangan posisi.

**Konsekuensi:** Ada biaya berhenti di setiap siklus. Karena itu siklus harus dipotong pada batas yang bermakna, bukan terlalu kecil.

---

## D5 — SQLite langsung, tanpa Room

**Keputusan:** Penyimpanan lokal memakai SQLite API Android langsung.

**Alasan:** Skema target sederhana (threads, messages, sync_state, sync_deletes) dan siklus tulis-baca jelas. Room menambah anotasi processor dan waktu build untuk manfaat yang kecil di skala ini.

**Konsekuensi:** Migrasi skema dan pemetaan cursor ditulis manual. Perlu disiplin saat skema berubah.

---

## D6 — Kredensial di AndroidKeyStore, bukan plaintext

**Keputusan:** Token dan API key disimpan terenkripsi AES-GCM dengan kunci di AndroidKeyStore, sebagaimana target melakukannya.

**Alasan:** Pola yang sudah benar dan murah ditiru. Menyimpan token plaintext di SharedPreferences terlalu berisiko untuk aplikasi yang memegang riwayat percakapan.

**Konsekuensi:** Uninstall menghapus kunci keystore, sehingga pengguna harus login ulang. Ini perilaku yang diterima, bukan bug.

---

## D7 — Tanpa backend sendiri: endpoint dan API key milik pengguna

**Keputusan:** Aplikasi tidak memakai server Nova. Pengguna memasukkan sendiri base URL yang kompatibel OpenAI dan API key-nya. Tidak ada akun, tidak ada sinkronisasi, tidak ada data yang lewat server perantara.

**Alasan:** Menghilangkan seluruh beban backend (auth, kuota, penyimpanan, moderasi, biaya). Semua data percakapan tetap di perangkat. Ini juga membuat aplikasi berguna untuk provider apa pun: OpenAI, OpenRouter, Groq, Together, vLLM, LM Studio, Ollama, llama.cpp.

**Konsekuensi:**
- Tidak ada sinkronisasi antar perangkat. Fitur ini keluar dari ruang lingkup.
- Kualitas dan biaya sepenuhnya tanggungan pengguna; aplikasi tidak bisa menegakkan kuota.
- Model list diambil dari `GET {base}/models`; tidak semua provider mengimplementasikannya dengan bentuk sama, jadi perlu fallback manual.
- Pengaturan endpoint disimpan bersama API key dan bisa diubah kapan saja.

---

## D8 — Hanya https, cleartext ditolak

**Keputusan:** Validasi endpoint menolak skema selain `https`. Peringatan ditampilkan di layar Pengaturan.

**Alasan:** API key dikirim pada setiap permintaan. Membuka cleartext berarti mengizinkan kredensial lewat jaringan tanpa enkripsi — risiko yang tidak sepadan demi kenyamanan memakai server lokal.

**Konsekuensi:**
- LM Studio, Ollama, dan llama.cpp di jaringan lokal tidak bisa dipakai langsung; perlu proksi https atau tunnel.
- Tidak perlu `networkSecurityConfig` khusus, karena perilaku default Android (blokir cleartext) sudah sesuai.
- Kalau nanti dibutuhkan, keputusan ini bisa dibatalkan dengan D9 yang menyebut pengecualiannya secara eksplisit.
