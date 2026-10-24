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

---

## D9 — Kebijakan versi, signature, dan migrasi

**Keputusan:** Tiga hal dikunci sejak rilis pertama: identitas aplikasi, kunci signature, dan cara migrasi data. Fitur dan tampilan tetap bebas berubah.

**1. Versi**
- `versionName` memakai bentuk `MAJOR.MINOR.PATCH`. Selama tahap awal tetap `0.x`; MINOR naik saat ada fitur baru, PATCH untuk perbaikan.
- `versionCode` bilangan bulat yang selalu naik dan tidak pernah dipakai ulang, termasuk untuk build yang hanya dibagikan ke satu orang.
- Build debug memakai akhiran package `.debug` sehingga bisa terpasang berdampingan dengan build rilis.

**2. Signature**
- Satu keystore rilis untuk selamanya. Disimpan di luar repository, dengan cadangan di dua tempat berbeda dan password di pengelola kata sandi.
- Kehilangan kunci berarti pengguna harus uninstall — dan datanya hilang — karena Android menolak update dengan kunci berbeda. Tidak ada jalan pintas untuk ini.
- Kredensial keystore tidak pernah masuk git; dibaca dari file properti di luar repo.

**3. Migrasi data**
- Setiap perubahan skema menaikkan `NovaDatabase.VERSION` dan menulis langkah migrasi eksplisit di `onUpgrade`.
- Tidak ada `DROP TABLE` pada data pengguna. Kalau kolom dihapus, nilainya diabaikan dulu, tidak dibuang.
- Setiap migrasi wajib punya jalur uji: buat data di versi lama, pasang versi baru, pastikan data masih terbaca.

**Alasan:** Ketiga hal ini satu arah. Setelah ada pengguna terpasang, memperbaikinya menuntut pengguna mengorbankan data. Sebaliknya, tampilan dan fitur bisa diubah berkali-kali tanpa biaya semacam itu.

**Konsekuensi:**
- Rilis pertama belum boleh disebut siap sebelum ada: build release dengan R8, signing config rilis, dan jalur migrasi yang diuji. Ini menjadi satu siklus tersendiri ("release hardening").
- Selama fase `0.x`, perubahan yang merusak kompatibilitas masih boleh, tetapi harus disertai migrasi.

---

## D10 — Model dipilih dari daftar provider, manual sebagai cadangan

**Keputusan:** Nama model diambil dari endpoint daftar model provider dan dipilih lewat dialog. Kolom teks tetap bisa diisi manual.

**Alasan:** Nama model berubah cepat dan berbeda antar provider; mengetik manual rawan salah dan menghasilkan galat 400 yang membingungkan. Tetapi tidak semua provider menyediakan daftar model, jadi jalur manual tidak boleh dihapus.

**Konsekuensi:**
- Memuat daftar butuh endpoint + key yang valid; dialog menampilkan galat bila gagal.
- Daftar diurutkan dan dibersihkan dari duplikat sebelum ditampilkan.
- Pilihan disimpan sebagai nama model saja, bukan id internal provider.

---

## D11 — Streaming memakai SSE, hasil disimpan sekali di akhir

**Keputusan:** Balasan diambil lewat Server-Sent Events dengan `stream: true`. Potongan ditampilkan langsung tetapi **tidak** ditulis ke database per token; hanya hasil akhir yang disimpan.

**Alasan:** Satu baris database per token berarti puluhan sampai ribuan tulis untuk satu balasan, dengan manfaat nol — yang perlu bertahan setelah app ditutup hanya teks akhirnya. Membaca ulang riwayat selalu menghasilkan yang sama.

**Konsekuensi:**
- Kalau app mati di tengah stream, balasan yang sedang berjalan hilang. Ini diterima; alternatifnya jauh lebih mahal.
- Tombol Stop menyimpan teks sebagian dengan status `FAILED`, sehingga usaha pengguna tidak lenyap.
- Provider yang mengabaikan `stream: true` tetap dilayani: balasan JSON biasa dibaca lewat jalur non-streaming.
