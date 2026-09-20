# Backlog Fitur SIAGA PADANG yang Belum Selesai

Dokumen ini menjadi daftar kerja untuk pengembangan lanjutan SIAGA PADANG. Audit awal dilakukan pada branch `main`, commit `61bed05`, tanggal 14 September 2026. **Status diperbarui 20 September 2026** pada commit `6361710`, setelah P0, P1-01, P1-03, P1-04, F-07, dataset 2026.09.19, dan desain V3 digabung ke `main`.

Dokumen ini adalah sumber status utama. `CONTEXT.md` dan tabel status lama lainnya bersifat historis. Alasan di balik keputusan, hasil pengukuran, dan jebakan yang sudah ditemui dicatat di `docs/HANDOFF.md`.

## Arti Status

- **Belum**: fitur belum tersedia pada aplikasi yang digunakan pengguna.
- **Sebagian**: sebagian alur sudah bekerja, tetapi belum memenuhi perilaku akhir.
- **Backend siap**: endpoint tersedia dan sudah diuji, tetapi aplikasi Android belum memakainya.
- **Perlu validasi**: kode tersedia, tetapi bukti pengujian atau validasi lapangan belum cukup.
- **Selesai**: kode sudah ada di `main`. Validasi yang masih tertunda disebutkan pada masing-masing butir.

## Fitur yang Sudah Ada

Bagian berikut tidak perlu dibuat ulang:

- Widget akses cepat SIAGA PADANG.
- Pembacaan GPS dan pencarian simpul jalan terdekat.
- Penentuan status zona dari data lokal.
- Penyajian rute utama dan dua alternatif dari SQLite lokal.
- Petunjuk arah, kompas, jarak, ETA, dan hitung mundur.
- Pergantian rute lokal ketika pengguna menekan tombol **Ada kendala?** — mencari jalan lain ke TES yang sama lebih dahulu (satu lompatan tetangga, tanpa pencarian lintasan), lalu `rank_2`/`rank_3`, lalu orientasi terakhir.
- Konfirmasi tiba di dekat TES atau ujung rute.
- Overlay lokal jaringan jalan, rute, tujuan, dan zona.
- Integrasi Android dengan status gempa terbaru BMKG melalui backend.
- Layar peringatan ketika data resmi BMKG menyatakan potensi tsunami.
- Backend untuk check-in, laporan hambatan, okupansi, event aktif, dan metadata sinkronisasi dasar.
- P0-01 s/d P0-06: ID lokal di state navigasi, klien API daring, check-in, laporan jalur terhalang dengan antrean luring, okupansi TES, serta konfigurasi deployment/release.
- Deploy otomatis backend ke Hugging Face Space melalui GitHub Actions (`.github/workflows/deploy-hf.yml`).
- P1-01 sinkronisasi dataset aman, P1-03 kedatangan di luar zona rendaman, P1-04 orientasi terakhir setelah semua rute ditolak.
- F-07 rencana titik temu keluarga, termasuk pengingat titik temu pada dialog kedatangan.
- Latar peta gelap dan jaringan jalan lokal yang terbaca ketika ubin peta tidak tersedia.
- Desain V3 diterapkan pada seluruh layar: fondasi tipografi Inter dan 55 ikon Material Symbols, layar evakuasi navy, mode peta besar, layar masa tenang berlatar krem (Menu dashboard, Daftar Fasilitas, Panduan, Pengaturan, Tentang), dialog kedatangan, dan onboarding pertama kali.
- Dataset 2026.09.19 dengan TEA (`tb_tea`, `tb_tea_routes`, `tb_tea_next`, view `v_fasilitas_evakuasi`); TEA tampil sebagai informasi fasilitas, belum sebagai tujuan rute.

## Prioritas 0 — Dikerjakan Lebih Dahulu

### P0-01 — Membawa ID Lokal sampai ke State Navigasi Android

**Status:** Selesai — dikerjakan Habib, commit `3c2def5`, digabung ke `main` 19 September 2026. Hasil uji kriteria selesai lintas perangkat belum tercatat.

**Status awal (14 September):** Belum
**Tujuan:** Menyediakan ID yang diperlukan saat Android mengirim check-in dan laporan jalur.

Saat ini `tes_id` dan `edge_id` ada di SQLite, tetapi belum seluruhnya dibawa ke `EvacuationRoute` dan UI state. API tidak boleh mengandalkan nama TES karena nama dapat berubah atau tidak unik.

Pekerjaan:

- Tambahkan `destinationExternalId` pada model rute.
- Pertahankan urutan ID ruas yang membentuk setiap rute.
- Simpan versi dataset lokal yang sedang digunakan.
- Tentukan ruas aktif atau ruas terdekat saat pengguna melaporkan hambatan.
- Tambahkan tes DAO dan repository untuk memastikan ID tidak hilang.

Lokasi kode awal:

- `android/app/src/main/java/com/akusukaproject/siagapadang/data/local/DatabaseRows.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/data/local/EvacuationDao.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/data/model/EvacuationRoute.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/data/repository/EvacuationRepository.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/ui/evacuation/EvacuationUiState.kt`

Kriteria selesai:

- Rute aktif memiliki ID tujuan, versi dataset, dan daftar ID ruas.
- Pergantian rank tidak mencampurkan ID dari rute sebelumnya.
- Tes membuktikan ID dari SQLite sama dengan payload yang akan dikirim ke backend.

### P0-02 — Infrastruktur API Android untuk Fitur Daring

**Status:** Selesai — dikerjakan Habib, commit `d1f6b22`, digabung ke `main` 19 September 2026. Hasil uji kriteria selesai lintas perangkat belum tercatat.

**Status awal (14 September):** Sebagian
**Tujuan:** Menyediakan klien bersama untuk endpoint selain BMKG tanpa mengganggu navigasi luring.

Pekerjaan:

- Buat ID perangkat anonim yang stabil dan simpan pada penyimpanan aplikasi.
- Kirim ID tersebut melalui header `X-Device-ID`.
- Tambahkan model request/response untuk event aktif, check-in, laporan hambatan, dan okupansi.
- Gunakan timeout singkat serta pesan `berhasil`, `tertunda`, atau `gagal` yang jujur.
- Pastikan semua permintaan berjalan di thread I/O dan dapat dibatalkan.
- Jangan menunggu respons API sebelum menampilkan atau mengganti rute lokal.
- Tambahkan pengujian parsing JSON dan kegagalan jaringan.

Lokasi kode awal:

- `android/app/src/main/java/com/akusukaproject/siagapadang/data/remote/BmkgApiClient.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/SiagaPadangApplication.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/ui/evacuation/EvacuationViewModel.kt`

Kriteria selesai:

- Setiap endpoint mempunyai DTO dan penanganan error yang teruji.
- Putusnya internet tidak menutup, menunda, atau mereset navigasi lokal.
- Tidak ada token, rahasia, atau identitas pribadi yang ditanam di APK.

### P0-03 — Check-in Keselamatan pada Android

**Status:** Selesai — dikerjakan Habib, commit `6b03e22`, digabung ke `main` 19 September 2026. Hasil uji kriteria selesai lintas perangkat belum tercatat.

**Status awal (14 September):** Backend siap; Android belum
**Dependensi:** P0-01 dan P0-02.

Pekerjaan:

- Tampilkan tombol check-in hanya setelah kedatangan dikonfirmasi.
- Ambil event aktif dari `GET /api/v1/status/emergency`.
- Kirim `POST /api/v1/shelter/checkin` dengan ID TES/TEA, koordinat, dan akurasi GPS.
- Tampilkan alasan yang jelas saat event tidak aktif, lokasi terlalu jauh, atau akurasi lebih buruk dari batas backend.
- Cegah pengguna mengira check-in berhasil ketika permintaan masih tertunda atau gagal.
- Simpan status pengiriman saat rotasi layar atau aplikasi masuk latar belakang.

Kriteria selesai:

- Check-in berhasil dari dua perangkat uji menuju event dan TES yang sama.
- Percobaan sebelum tiba tidak tersedia dari alur normal.
- Respons duplikat, timeout, GPS buruk, dan event tidak aktif ditangani di UI.
- Navigasi dan dialog kedatangan tetap bekerja tanpa internet.

### P0-04 — Pengiriman Laporan Jalur Terhalang

**Status:** Selesai — dikerjakan Habib, commit `8e705d8`, digabung ke `main` 19 September 2026. Hasil uji kriteria selesai lintas perangkat belum tercatat.

**Terhambat (20 September):** laporan belum pernah benar-benar sampai ke server. Checksum dataset di Railway (`6c12d8…`) berbeda dengan dataset aplikasi (`5d1549f0…`), sehingga aplikasi sengaja tidak mengirim laporan dengan `dataset_version_id` yang tidak cocok. Pesan kegagalan sudah dibedakan secara jujur (luring / galat server / ditolak / versi dataset tidak cocok). Perbaikan ada di sisi backend: muat dataset yang identik ke Railway, lalu ulangi uji ujung ke ujung.

**Status awal (14 September):** Pergantian lokal selesai; pengiriman Android belum
**Dependensi:** P0-01 dan P0-02.

Pekerjaan:

- Pertahankan urutan: ganti rute lokal terlebih dahulu, lalu kirim laporan.
- Kirim `edge_external_id`, `dataset_version_id`, posisi, dan keterangan ke `POST /api/v1/reports/obstruction`.
- Buat antrean lokal untuk laporan ketika internet tidak tersedia.
- Hapus atau tandai selesai laporan setelah backend menerimanya.
- Tampilkan status laporan tanpa menghalangi petunjuk evakuasi.
- Tambahkan endpoint backend untuk membaca daftar ruas yang sudah dikonfirmasi terhalang pada event dan versi dataset aktif.
- Hubungkan daftar hambatan terkonfirmasi ke Android agar laporan benar-benar bermanfaat bagi perangkat lain.

Kriteria selesai:

- Rute berganti seketika dalam mode pesawat.
- Laporan tertunda terkirim ketika koneksi kembali.
- Tiga perangkat berbeda dapat mengubah status hambatan menjadi terkonfirmasi sesuai aturan backend.
- Perangkat lain dapat menerima daftar hambatan terkonfirmasi tanpa salah versi dataset.

### P0-05 — Okupansi TES/TEA pada Android

**Status:** Selesai — dikerjakan Habib, commit `d3dbf7c`, digabung ke `main` 19 September 2026. Hasil uji kriteria selesai lintas perangkat belum tercatat.

**Status awal (14 September):** Backend siap; Android belum
**Dependensi:** P0-02 dan P0-03.

Pekerjaan:

- Setelah check-in berhasil, tampilkan pilihan `LOW`, `MODERATE`, atau `FULL` dengan istilah Indonesia yang mudah dipahami.
- Kirim laporan ke `POST /api/v1/shelter/occupancy`.
- Ambil status agregat dari `GET /api/v1/shelter/{external_id}/occupancy`.
- Tampilkan jumlah laporan, waktu pembaruan, dan sumber **laporan pengguna yang sudah check-in**.
- Tampilkan `belum ada data` untuk status `UNKNOWN`.
- Jangan menyebut okupansi sebagai data luring atau kapasitas aktual terverifikasi.

Kriteria selesai:

- Pengguna yang belum check-in tidak dapat mengirim laporan.
- Status berubah sesuai laporan terbaru dan tetap membawa sumber serta waktu.
- Gangguan jaringan tidak mengubah rute lokal secara otomatis.

### P0-06 — Deployment Backend dan Konfigurasi Release Android

**Status:** Selesai — dikerjakan Habib, commit `c885b37`, `63def35`, `b393126`, digabung ke `main` 19 September 2026. Hasil uji kriteria selesai lintas perangkat belum tercatat.

**Status awal (14 September):** Belum
**Tujuan:** Membuat fitur daring dapat dipakai tanpa `adb reverse` atau komputer pengembang.

Pekerjaan:

- Deploy FastAPI dan PostGIS pada server yang dapat dijangkau perangkat.
- Gunakan HTTPS dan domain tetap.
- Atur `DATABASE_URL`, `HMAC_SECRET`, CORS, logging, backup, dan health check melalui secret server.
- Isi `SIAGA_BACKEND_BASE_URL` saat build release; saat ini nilai default release kosong.
- Pisahkan konfigurasi development, staging, dan production.
- Uji APK release pada jaringan seluler tanpa kabel USB.

Kriteria selesai:

- APK release dapat mengambil status BMKG dari jaringan seluler.
- Check-in dan laporan berhasil tanpa `adb reverse`.
- Tidak ada kredensial server di repository atau APK.
- Gangguan backend menghasilkan pesan singkat dan navigasi luring tetap berjalan.

## Prioritas 1 — Menyelesaikan Klaim Offline-First

### P1-01 — Sinkronisasi Dataset yang Aman

**Status:** Selesai di `main` (commit `5ddf489`); pengujian update dengan dua versi dataset nyata masih diperlukan.

Implementasi saat ini menyediakan paket SQLite lengkap, metadata versi/skema/ukuran/checksum,
unduhan ke file sementara, validasi SHA-256 dan struktur SQLite, aktivasi pada pembukaan aplikasi
berikutnya, serta rollback otomatis jika Room gagal membuka database baru. File unduhan yang
terputus diabaikan dan dibersihkan tanpa mengganti data aktif.

Pekerjaan:

- Tetapkan format paket untuk nodes, edges, routes, TES/TEA, zona rendaman, dan versi data.
- Selesaikan `GET /api/v1/sync/network`; endpoint ini masih placeholder.
- Pastikan `/sync/check` hanya menyatakan update jika versi server lebih baru dari versi lokal klien.
- Sertakan ukuran, checksum, schema version, minimum app version, dan URL unduh.
- Unduh ke file sementara, validasi checksum, lalu terapkan secara atomik.
- Pertahankan data lama jika unduhan, validasi, atau migrasi gagal.
- Sediakan rollback dan tes saat aplikasi ditutup di tengah pembaruan.

Kriteria selesai:

- Instalasi dengan dataset lama dapat diperbarui tanpa memasang ulang APK.
- File rusak atau checksum salah selalu ditolak.
- Navigasi masih menggunakan data lama setelah update gagal.

### P1-02 — Basemap Benar-benar Luring

**Status:** Sebagian.

Sudah: ketika ubin tidak tersedia, peta menampilkan latar navy gelap (`#0E2A47`) dengan jaringan jalan lokal bertepi gelap dan berinti terang, sehingga terbaca di atas ubin OSM maupun latar polos. Jaringan jalan selalu berada di bawah rute. Diuji tanpa cache ubin dalam mode pesawat (Infinix X6855, 19 September 2026).

Belum: paket ubin luring. Ubin OpenStreetMap masih bergantung pada jaringan atau cache. Menurut `CLAUDE.md` Bagian 6 paket ubin bersifat opsional.

Pekerjaan:

- Pilih format dan pipeline ubin luring yang kompatibel dengan MapLibre.
- Batasi cakupan pada wilayah percontohan dan level zoom yang diperlukan.
- Sertakan atribusi dan lisensi sumber peta.
- Pastikan paket tidak membuat APK atau data aplikasi melebihi target ukuran.
- Uji instalasi bersih dalam mode pesawat; jangan mengandalkan cache dari pengujian sebelumnya.

Kriteria selesai:

- Jalan dasar, label penting, overlay, dan rute tampil setelah instalasi bersih tanpa internet.
- Peta tetap dapat digeser dan diperbesar dalam batas cakupan paket.
- Ukuran paket dan lisensinya terdokumentasi.

### P1-03 — Kedatangan di Luar Zona Rendaman

**Status:** Selesai di `main` (commit `9e048a6`); validasi GPS lapangan/perangkat masih ditunda.

Kedatangan kini dapat dikonfirmasi melalui dua keadaan: pengguna sampai di TES/ujung rute,
atau pengguna berpindah dari dalam ke luar poligon zona rendaman. Transisi keluar zona baru
diterima setelah tiga pembacaan GPS berturut-turut dengan akurasi maksimal 35 meter. Aplikasi
yang mulai digunakan di luar zona tidak langsung dianggap telah menyelesaikan evakuasi.

Dialog dan tombol status membedakan kedatangan di TES dari keberadaan di luar zona rendaman.
Check-in dan laporan okupansi hanya tersedia untuk kedatangan di TES, sedangkan redaksi keluar
zona mengarahkan pengguna menjauhi pantai tanpa memberikan klaim keselamatan.

Pekerjaan:

- Tentukan aturan kedatangan untuk TES/TEA dan keluar dari poligon rendaman.
- Bedakan teks **tiba di tujuan** dan **berada di luar zona rendaman**.
- Jangan menggunakan kata **aman** hanya berdasarkan pemeriksaan poligon.
- Pertahankan syarat akurasi dan beberapa pembacaan GPS agar dialog tidak muncul akibat satu titik yang melompat.

Kriteria selesai:

- Transisi zona diuji pada sisi dalam, batas, dan sisi luar poligon.
- Pembacaan GPS buruk tidak memicu kedatangan.
- Redaksi UI tidak memberikan jaminan keselamatan yang tidak dapat diketahui aplikasi.

### P1-04 — Fallback Setelah Semua Rute Alternatif Habis

**Status:** Selesai di `main` (commit `3a86755`); validasi tampilan dan GPS pada perangkat masih ditunda.

Rute utama dan dua alternatif tetap dapat ditandai terhalang. Setelah rute ketiga ditolak,
aplikasi menyembunyikan seluruh garis rute yang sudah ditolak dan menampilkan orientasi terakhir
berupa arah kompas serta jarak lurus menuju tujuan. Nilai tersebut diperbarui ketika posisi GPS
berubah. Tampilan menyatakan dengan jelas bahwa orientasi itu bukan rute aman atau rute yang telah
diperiksa serta tetap mengarahkan pengguna menjauhi pantai dan mengikuti petugas atau rambu.

Pekerjaan:

- Setelah rank 1–3 ditolak, tampilkan bahwa tidak ada jalur jalan yang dapat diverifikasi.
- Tampilkan arah dan jarak garis lurus sebagai orientasi terakhir.
- Nyatakan bahwa garis tersebut bukan rute aman atau rute yang sudah diperiksa.
- Ingatkan pengguna untuk menjauhi arah pantai dan mengikuti petugas atau rambu lapangan.
- Jangan membuat garis melewati bangunan terlihat sebagai jalur jalan.

Kriteria selesai:

- Aplikasi tidak macet atau kembali diam-diam ke rute yang sudah ditolak.
- Pesan keterbatasan dan tindakan berikutnya mudah dibaca dalam satu layar.

### P1-07 — TEA sebagai Tujuan Evakuasi

**Status:** Belum — issue [#1](https://github.com/AkuSukaProject/siagapadang/issues/1), ditugaskan ke Habib.

`ranah_siaga.db` versi `2026.09.13` hanya memuat 143 TES gedung. Seluruh tujuan `tb_routes` rank 1–3 ada di `tb_tes`; tidak ada TEA. `tb_safe_zones` (31 poligon kelurahan) dipakai untuk deteksi keluar zona, bukan sebagai titik tujuan. Pekerjaan utama ada di `spatial/`: tambahkan 29 TEA ke prakomputasi, hasilkan ulang basis data, lalu Android menambahkan label TES/TEA.

### P1-05 — Administrasi Event Darurat

**Status:** Endpoint baca event aktif tersedia; pengelolaan belum.

Pekerjaan:

- Buat mekanisme berotorisasi untuk membuat, mengaktifkan, menutup, dan membatalkan event.
- Pastikan hanya satu event tsunami aktif pada satu waktu.
- Simpan sumber, alasan perubahan, waktu, dan identitas operator.
- Jangan membuka endpoint administrasi tanpa autentikasi.
- Pisahkan event simulasi/drill dari kejadian nyata.

Kriteria selesai:

- Operator dapat mengelola siklus event tanpa mengubah database secara manual.
- Event simulasi selalu berlabel simulasi pada API dan Android.
- Audit log perubahan tersedia.

### P1-06 — Migrasi Database Produksi

**Status:** Belum aman untuk data produksi.

Pekerjaan:

- Buat migrasi Alembic baru untuk `shelter_occupancy_reports`.
- Hapus operasi `TRUNCATE` dan perubahan enum destruktif dari jalur upgrade produksi.
- Uji upgrade dari schema yang saat ini dipakai tanpa kehilangan check-in atau laporan hambatan.
- Buat backup otomatis dan latihan restore.
- Dokumentasikan perintah migrasi dan rollback.

Kriteria selesai:

- Migrasi diuji pada salinan database berisi data.
- Jumlah record sebelum dan sesudah migrasi tetap sesuai.
- Deployment dapat dibatalkan tanpa menghapus data operasional.

## Prioritas 2 — Fitur Lanjutan

### P2-01 — Rencana Evakuasi Keluarga

**Status:** Selesai (F-07) — commit `6a7eff9` dan `b3d7fda`, diuji pada Infinix X6855 dalam mode pesawat.

Fitur ini disusun pada masa tenang dan disimpan lokal. Fitur ini bukan pelacakan lokasi anggota keluarga secara langsung.

Implementasi: layar Rencana Keluarga dari tombol **KELUARGA**; titik temu keluarga dan TES tujuan tiap anggota dipilih dari `tb_tes` (urut jarak) atau dari rute rank 1 di posisi HP; disimpan di `SharedPreferences`, terpisah dari `ranah_siaga.db`; dibagikan sebagai teks tanpa koordinat; titik temu ditampilkan pada dialog kedatangan.

Belum: tujuan TEA (lihat P1-07) dan uji tombol **Bagikan** serta **Hapus** di perangkat.

Pekerjaan:

- Tambah anggota keluarga dan lokasi rutinnya.
- Pilih tujuan evakuasi untuk setiap anggota/lokasi.
- Simpan rencana secara lokal dan sediakan tampilan ringkas yang dapat dibuka tanpa internet.
- Sediakan ekspor atau berbagi rencana tanpa membagikan lokasi real-time.

Kriteria selesai:

- Seluruh rencana tetap dapat dibaca dalam mode pesawat.
- Tidak ada klaim bahwa aplikasi mengetahui posisi anggota keluarga saat bencana.

### P2-02 — Mode Drill

**Status:** Belum.

Pekerjaan:

- Buat event latihan yang tidak dapat disalahartikan sebagai peringatan nyata.
- Catat waktu mulai, waktu tiba, tujuan, dan hasil latihan dengan persetujuan pengguna.
- Sediakan ringkasan hasil dan ekspor data anonim.
- Pisahkan warna, label, notifikasi, dan backend drill dari kejadian nyata.

Kriteria selesai:

- Setiap layar latihan menampilkan label **SIMULASI/DRILL**.
- Tidak ada data latihan yang masuk ke okupansi atau event darurat nyata.

### P2-03 — Simulasi Pergerakan Kolektif yang Reproducible

**Status:** Hasil/data ada; source pipeline belum lengkap di repository.

Pekerjaan:

- Masukkan source code pembentukan graf, pembangkitan populasi, pembobotan kapasitas, dan perhitungan rute.
- Simpan konfigurasi, seed, versi dataset, dan parameter kecepatan.
- Bandingkan rute berbobot kapasitas dengan lintasan terpendek pada input identik.
- Hasil minimum: persentase tiba dalam target waktu, beban tiap ruas, antrean titik sumbat, dan kelebihan kapasitas tujuan.
- Buat perintah tunggal untuk menghasilkan ulang SQLite Android dan laporan simulasi.

Kriteria selesai:

- Anggota tim lain dapat menjalankan simulasi dari repository dan memperoleh hasil yang sama.
- Setiap database Android dapat ditelusuri ke konfigurasi dan versi dataset pembentuknya.
- Klaim keunggulan metode didukung angka pembanding.

### P2-04 — Dashboard Analisis untuk Pengelola

**Status:** Belum.

Pekerjaan:

- Tampilkan distribusi beban TES/TEA dan titik sumbat hasil simulasi.
- Tampilkan status event, laporan hambatan terkonfirmasi, check-in, dan okupansi dengan sumber/waktu.
- Pisahkan data simulasi dari data kejadian nyata.
- Terapkan autentikasi dan pembatasan peran.

Kriteria selesai:

- Pengelola dapat melihat data tanpa menjalankan query database manual.
- Setiap angka menampilkan sumber, versi dataset, dan waktu pembaruan.

## Pekerjaan Validasi dan Kualitas

### Q-01 — Pengujian Perangkat dan Kinerja

- Ukur waktu dari aplikasi dibuka sampai arahan pertama tampil.
  - ✅ **NF-02 tercapai pada Infinix X6855** (Android 16, mode pesawat, 19 September 2026, 21 cold start valid). Lokasi masuk → rute siap: median **597 ms**, P90 696 ms, maksimum **727 ms**; 21/21 di bawah 1 detik. Layar evakuasi dibuat → rute siap: median 1.189 ms (termasuk menunggu posisi pertama dari GPS, sekitar 0,6 detik). Aplikasi dibuka → frame pertama: median 857 ms. Rincian di `docs/IMPLEMENTATION_LOG.md`.
  - Keterbatasan: GPS dalam keadaan hangat (posisi terakhir tersedia). Pada GPS dingin, posisi pertama dapat butuh beberapa detik di luar kendali aplikasi, sehingga klaim NF-02 dihitung sejak posisi diperoleh.
  - Belum: NF-04 — ulangi pengukuran yang sama pada tiga perangkat lain. Pencatat `EvacuationTiming` (`adb logcat -s EvacuationTiming`) mencatat waktu lokasi → rute siap di setiap pembukaan.
- Uji mode pesawat pada instalasi bersih.
- Uji minimal 3–5 perangkat berbeda merek dan versi Android.
- Uji GPS buruk, izin ditolak, sensor kompas tidak tersedia, baterai hemat, rotasi layar, dan aplikasi kembali dari latar belakang.
- Catat ukuran APK per ABI dan ukuran dataset setelah basemap luring ditambahkan.
- Putuskan status NF-03. Dataset 2026.09.19 berukuran 76.419.072 byte — 72,9 MiB, tetapi 76,4 MB desimal, yaitu melewati batas 75 MB bila dihitung dengan satuan itu. Pilihannya: perbarui batas di proposal dengan alasan penambahan TEA, atau minta Habib memadatkan. Jangan mengubah angka target tanpa mencatat alasannya.

### Q-06 — Verifikasi Perangkat untuk Desain V3

**Status:** Belum diuji di perangkat. Kode lulus kompilasi dan 113 uji unit, tetapi tampilan belum pernah dilihat di layar nyata. Penundaan ini disengaja atas permintaan Mikail, bukan kelalaian.

Pekerjaan:

- Uji tujuh perapian UI pada commit `6361710`: kompas, legenda zona, kontras zona, ketuk dua kali peta untuk membesarkan/mengecilkan, kehalusan animasi, ketebalan panah, popup putih yang muncul dari ikonnya, dan hitung mundur yang terlihat bergerak.
- Uji tombol **Bagikan** dan **Hapus** pada rencana keluarga.
- Uji ulang seluruh layar masa tenang pada font sistem besar dan layar kecil.
- Catatan: galat seperti tombol tak terlihat saat nonaktif, banner menutupi kartu arah, dan ikon status bar putih di layar krem hanya ditemukan lewat pengujian perangkat. Jangan menyatakan pekerjaan UI selesai sebelum dilihat di layar nyata.

### UI-01 — Rapikan `EvacuationScreen.kt`

**Status:** Belum. Berkas sudah 3.479 baris dan masih memuat composable lama dari desain sebelum V3 yang tidak lagi dipanggil. Keluarkan yang mati, dan pindahkan bagian yang masih dipakai ke `EvacuationV3Components.kt` sesuai aturan satu file satu tanggung jawab (`CLAUDE.md` Bagian 10).

### Q-02 — Aksesibilitas dan Keterbacaan Darurat

- Audit kontras minimum, target sentuh minimal 48 dp, ukuran teks, TalkBack, dan content description.
- Uji layar kecil, font sistem besar, mode gelap, serta penggunaan di bawah cahaya luar ruangan.
- Pastikan informasi utama tidak hanya dibedakan menggunakan warna.

### Q-03 — Validasi Data dan Redaksi dengan BPBD

- Validasi lokasi, nama, kapasitas, kondisi, dan akses masuk TES/TEA.
- Validasi zona rendaman, terminologi, waktu sasaran, dan arahan setelah tiba.
- Catat sumber, tanggal, penanggung jawab, serta versi setiap dataset.
- Ganti data kandidat atau OSM dengan data resmi ketika tersedia.

### Q-04 — Release dan Otomasi

- Siapkan signing release di luar repository.
- Tambahkan CI untuk unit test Android, build debug, pemeriksaan Python, tes backend, dan validasi OpenAPI.
- Perbarui `backend/openapi.json` secara otomatis ketika kontrak API berubah.
- Buat catatan versi, prosedur rollback APK/data, serta pemeriksaan lisensi aset.

### Q-05 — Rapikan Dokumentasi yang Tertinggal

- ✅ `CONTEXT.md` diberi penanda dokumen historis dan status Android diperbarui (19 September 2026).
- ✅ Dokumen ini ditetapkan sebagai sumber status utama.
- ✅ Pemeriksaan karakter rusak pada `README.md`, `CONTEXT.md`, dan `docs/*.md` tidak menemukan masalah.
- ✅ `docs/HANDOFF.md` dibuat 20 September 2026 sebagai dokumen serah terima: keadaan aplikasi, alasan keputusan desain V3, fakta dataset, jebakan build, dan pembagian pekerjaan yang tertunda.
- Selanjutnya: catat pekerjaan baru sebagai issue GitHub berjudul kode kebutuhan (`CLAUDE.md` Bagian 9) agar keterlacakan kebutuhan → kode terlihat.

## Saran Pembagian kepada Teman

Pekerjaan berikut dapat dimulai paralel:

| Bagian | Tugas | Dependensi |
|---|---|---|
| Android data | P0-01 | Tidak ada |
| Android API dasar | P0-02 | Tidak ada |
| Backend | Endpoint baca hambatan pada P0-04, P1-05, P1-06 | Tidak ada |
| Peta | P1-02 | Tidak ada |
| Simulasi | P2-03 | Tidak ada |
| Validasi | Q-01, Q-02, Q-03 | Dapat dimulai dari fitur yang sudah tersedia |

Setelah P0-01 dan P0-02 selesai, lanjutkan P0-03, P0-04, dan P0-05. P0-06 diperlukan untuk pengujian lintas perangkat melalui internet. Sinkronisasi dataset pada P1-01 sebaiknya dikerjakan setelah format data dan migrasi pada P1-06 disepakati.

## Aturan Keselamatan yang Harus Dipertahankan

- Sistem tidak mendeteksi gempa secara otomatis.
- Potensi tsunami selalu mengikuti pernyataan resmi BMKG.
- Gunakan istilah **TES**, **TEA**, dan **Tsunami Safe Zone** sesuai konteks.
- Gunakan arahan **berjalan cepat**, bukan berlari.
- Gunakan teks **di luar zona rendaman**, bukan klaim **aman**, jika sistem hanya memeriksa poligon.
- Pergantian rute lokal tidak boleh menunggu internet.
- Okupansi harus mempunyai sumber dan waktu pembaruan; jangan mengklaim data tersebut tersedia saat luring.
- Navigasi inti harus tetap berjalan ketika seluruh layanan backend gagal.
