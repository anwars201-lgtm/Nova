# Aturan kerja

Dokumen ini mengikat semua siklus kerja di workspace ini, termasuk thread baru.

## 1. Siklus, bukan marathon

Pekerjaan dibagi menjadi siklus. Satu siklus = satu potong kerja yang bisa diselesaikan dan diverifikasi dalam satu sesi.

Batas siklus harus memenuhi salah satu:

- berakhir dengan build yang lulus, atau
- berakhir dengan sesuatu yang bisa dilihat di HP, atau
- berakhir dengan hasil terukur yang bisa dijalankan ulang.

Kalau sebuah siklus hanya berakhir dengan "kode sudah ditulis", ruang lingkupnya kurang dipotong.

## 2. Setiap siklus berakhir dengan laporan

Laporan memuat tiga bagian saja:

1. Apa yang selesai, disertai bukti perintah dan hasilnya
2. Apa yang tidak selesai dan kenapa
3. Apa yang dilakukan di siklus berikutnya

## 3. STATUS.md diperbarui sebelum laporan dikirim

Urutannya: kerjakan, verifikasi, perbarui `STATUS.md`, commit, baru kirim laporan. Status tidak boleh pernah tertinggal dari kenyataan.

## 4. "Selesai" harus berarti terverifikasi

Setiap butir yang ditandai selesai wajib punya bukti:

- perintah yang dijalankan, dan
- hasil yang teramati (BUILD SUCCESSFUL, ukuran file, hash, tampilan di device)

Tidak ada butir selesai tanpa bukti. Kalau belum diuji, tulis "belum diverifikasi" — bukan "selesai".

## 5. Commit per siklus

Satu siklus berakhir dengan satu commit. Pesan commit menyebut nomor siklus dan isi utamanya. Commit adalah titik balik; `STATUS.md` adalah peta.

## 6. Protokol lanjut-thread

Saat melanjutkan di thread baru:

1. Baca `STATUS.md`, `DECISIONS.md`, dan memori lingkungan
2. Verifikasi: jalankan build dan cek bahwa file yang diklaim ada benar-benar ada
3. Laporkan hasil verifikasi: cocok, atau ada penyimpangan
4. Sepakati ruang lingkup siklus berikutnya dengan pengguna
5. Baru mulai bekerja

Langkah 2 tidak boleh dilewati. `STATUS.md` ditulis oleh agen yang sama yang bisa keliru; status hanya dipercaya setelah dicocokkan dengan kenyataan.

## 7. Ruang lingkup disepakati sebelum mulai

Agen mengusulkan isi siklus dalam satu kalimat, pengguna menyetujui atau memotongnya. Tidak ada pekerjaan besar yang dimulai tanpa persetujuan.

## 8. Keputusan dicatat, percobaan gagal dicatat satu baris

- Keputusan arsitektur yang bertahan lama masuk `DECISIONS.md` beserta alasannya.
- Percobaan yang gagal cukup dicatat sebagai satu baris di bagian "Catatan" pada `STATUS.md`. Tidak perlu esai.

Alasannya: tanpa catatan keputusan, thread baru akan menanyakan ulang hal yang sudah diputuskan.

## 9. Batas yang tidak dilanggar

- Tidak menyalin kode, aset, atau branding milik pihak lain. Membangun ulang fungsi dengan kode baru: boleh.
- Tidak memasang alat baru tanpa alasan yang bisa dijelaskan.
- Tidak mengklaim sesuatu sudah diuji kalau belum dijalankan.

## 10. Lingkungan

Batas teknis environment ini (bionic, noexec, aapt2 x86_64, wrapper rusak) sudah terdokumentasi di `.harness/memory/workspace-capabilities.md`. Baca itu sebelum menyalahkan kode.

## 11. Menunggu build

1. Build dijalankan di depan hanya bila perkiraan selesai di bawah 8 menit — alat shell mematikan perintah pada 600 detik, dan build yang terpotong hanya menyisakan ekor log tanpa vonis. Vonisnya adalah kode exit perintah itu sendiri: 0 sukses, selain 0 gagal.
2. Build yang lebih lama dijalankan di latar sebagai satu perintah. Log dikosongkan lebih dulu, dan skrip wajib menulis baris `EXIT=<kode>` ke log yang sama. `scripts/build.sh` sudah melakukan ini.
3. Dilarang menunggu tanpa membaca apa pun. Selama menunggu, log diperiksa setiap 45–90 detik, dan setiap pemeriksaan wajib melaporkan informasi baru: task terbaru dari log (mis. `> Task :app:...`), atau status proses.
4. Berhenti menunggu segera setelah muncul `BUILD SUCCESSFUL`, `BUILD FAILED`, atau baris `EXIT=`. Yang menjadi vonis adalah `EXIT=`, bukan kode exit perintah pemeriksaan.
5. Baris error atau `What went wrong` dilaporkan saat itu juga, dengan baris persisnya, tanpa menunggu build selesai.
6. Setelah `EXIT=0`, artefak diverifikasi: kalau ada perubahan kode, sha256 atau waktu ubah APK harus baru. `BUILD SUCCESSFUL` yang menghasilkan file lama padahal sumber berubah tetap dianggap gagal. Sebaliknya, bila semua task `UP-TO-DATE` dan tidak ada perubahan, file lama memang benar — nyatakan itu secara eksplisit, jangan mengklaim artefak baru.
7. Dua kegagalan berturut-turut dengan baris error yang sama berarti penyebabnya belum dipahami. Berhenti, analisis, jangan mengulang perintah yang sama.

**Catatan penerapan:** `scripts/build.sh` menulis seluruh keluaran ke `build.log`, menambahkan `EXIT=<kode>`, mencetak 25 baris terakhir, lalu `sha256` dan waktu ubah APK. Log tidak masuk git.
