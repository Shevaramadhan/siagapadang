# Log Implementasi SIAGA PADANG

Dokumen ini mencatat perubahan pengembangan, hasil pemeriksaan, dan validasi yang masih ditunda.

## 19 September 2026 — Pengukuran NF-02

- Perangkat: Infinix X6855, Android 16, APK debug arm64, **mode pesawat**, GPS hangat.
- Metode: cold start berulang (`am force-stop` lalu `am start -W`). Pencatat sementara
  `System.currentTimeMillis()` dipasang di `EvacuationViewModel`, lalu dihapus setelah pengukuran.
- 29 putaran dijalankan. 8 putaran (run 12–19) dibuang karena layar mati atau tertutup layar kunci.
  Tersisa 21 putaran valid.

| Yang diukur | Min | Median | P90 | Maks |
|---|---|---|---|---|
| Lokasi masuk → rute siap (NF-02) | 415 ms | 597 ms | 696 ms | 727 ms |
| Layar evakuasi dibuat → rute siap | 951 ms | 1.189 ms | 1.338 ms | 1.477 ms |
| Aplikasi dibuka → frame pertama (`TotalTime`) | 751 ms | 857 ms | 1.026 ms | 1.803 ms |

- Rute siap mencakup pencarian simpul terdekat, pembacaan `tb_routes`, pengambilan ruas `tb_edges`,
  dan perangkaian polyline.
- Maksimum `TotalTime` 1.803 ms terjadi pada putaran pertama setelah pemasangan APK.
- Kesimpulan: NF-02 (< 1 detik) tercapai pada perangkat ini, 21/21 putaran. NF-04 memerlukan
  pengukuran yang sama pada tiga perangkat lain.

## 19 September 2026 — F-07 Rencana Titik Temu Keluarga dan Latar Peta Gelap

- Branch: `feat/f07-family-plan`, digabung langsung ke `main` atas arahan pengguna (tanpa review PR).
- Commit: `6a7eff9` (F-07), `b3d7fda` (pengingat titik temu, latar peta gelap).
- Layar Rencana Keluarga: titik temu keluarga dan TES tujuan tiap anggota. Pilihan TES berasal
  dari `tb_tes` (urut jarak dari posisi HP) atau dari rute rank 1 pada posisi HP saat ini.
- Rencana disimpan di `SharedPreferences` (`siaga_padang_family_plan`), terpisah dari
  `ranah_siaga.db` yang read-only. Dibagikan sebagai teks tanpa koordinat.
- Dialog kedatangan menampilkan titik temu keluarga bila sudah diisi, dan kini dapat digulir.
- Latar peta tanpa ubin menjadi navy gelap; jaringan jalan diberi tepi gelap dan inti terang,
  serta selalu digambar di bawah rute. Pintasan DATA/KELUARGA disembunyikan saat peta diperbesar.
- Unit test: 92 lulus (7 baru untuk penyimpanan rencana dan teks bagikan).
- Uji perangkat (Infinix X6855, Android 16, mode pesawat): memilih titik temu, menambah anggota,
  saran TES dari posisi (< 1 detik setelah perbaikan), data bertahan setelah force-stop dan pasang
  ulang, dialog kedatangan, peta tanpa cache ubin.
- Temuan yang diperbaiki saat uji: tombol Simpan nonaktif hampir tak terlihat (kontras), saran TES
  menunggu ~5 detik dalam mode pesawat, pintasan menutupi nama tujuan saat peta diperbesar, dan
  garis jalan menutupi rute.
- **Belum diuji di perangkat:** tombol Bagikan dan Hapus anggota.
- **NF-02:** pengamatan manual pengguna menunjukkan arahan tampil sekitar 1 detik (< 2 detik).
  Target < 1 detik belum terbukti; pengukuran terinstrumentasi masih diperlukan.
- Temuan data: basis data belum memuat TEA sebagai tujuan — issue #1.

## 16 September 2026 — P1-01 Sinkronisasi Dataset yang Aman

- Branch: `feat/p1-safe-dataset-sync`
- Commit: `5ddf489` (`feat: implement safe dataset package updates`)
- Backend menyediakan paket SQLite jaringan lengkap melalui endpoint sinkronisasi beserta versi,
  versi skema, ukuran file, checksum SHA-256, versi minimum aplikasi, dan URL unduhan.
- Android mengunduh paket ke file sementara, memeriksa metadata, checksum, dan struktur SQLite,
  lalu menjadwalkan aktivasi pada pembukaan aplikasi berikutnya.
- Database lama dipertahankan ketika unduhan atau pemeriksaan gagal. Jika Room gagal membuka
  database baru, aplikasi mengembalikan database cadangan.
- File unduhan yang terputus dibersihkan dan tidak mengganti dataset aktif.
- Unit test Android selesai dan APK debug pernah dipasang serta dibuka pada perangkat terhubung.
- **Validasi ditunda atas arahan pengguna:** pembaruan end-to-end memakai dua versi dataset nyata,
  penolakan checksum salah, gangguan unduhan, dan rollback pada perangkat/backend.

## 16 September 2026 — P1-03 Kedatangan di Luar Zona Rendaman

- Branch: `feat/p1-arrival-zone-transition`
- Menambahkan `ZoneExitConfirmationTracker` untuk membedakan posisi awal di luar zona dari
  perpindahan nyata dari dalam ke luar zona rendaman.
- Keluar zona dikonfirmasi setelah tiga pembacaan berturut-turut dengan akurasi GPS maksimal
  35 meter. Pembacaan dengan akurasi buruk, data zona yang tidak tersedia, atau pembacaan kembali
  di dalam zona memutus rangkaian konfirmasi.
- Pemeriksaan zona dijalankan setelah perpindahan 12 meter atau paling lambat setiap 1,5 detik
  selama data lokasi diterima.
- Menambahkan alasan penyelesaian evakuasi: sampai di titik evakuasi atau keluar dari zona rendaman.
- Dialog dan tombol navigasi menampilkan redaksi yang sesuai dengan alasan tersebut. Redaksi keluar
  zona tidak memakai kata “aman” dan tetap menyuruh pengguna menjauhi pantai serta mengikuti petugas.
- Check-in posko dan pelaporan okupansi hanya dapat dilakukan ketika kedatangan dikonfirmasi di TES.
- Unit test `:android:app:testDebugUnitTest` lulus, termasuk empat skenario baru untuk posisi awal
  di luar zona, tiga konfirmasi keluar, pembacaan batas/dalam, dan akurasi GPS buruk.
- **Validasi ditunda atas arahan pengguna:** simulasi lokasi pada perangkat, pengujian batas poligon
  secara end-to-end, dan pemeriksaan tampilan dialog di berbagai ukuran layar.

## 16 September 2026 — P1-04 Fallback Setelah Semua Rute Alternatif Habis

- Branch: `feat/p1-route-fallback`
- Tombol pelaporan jalur tetap tersedia pada rute ketiga agar pengguna dapat menyatakan bahwa
  seluruh rute utama dan alternatif telah terhalang.
- Setelah rute terakhir ditolak, aplikasi menghentikan panduan belokan dan menyembunyikan garis
  rute aktif maupun riwayat rute yang sudah ditolak dari peta.
- Aplikasi menampilkan orientasi terakhir berupa mata angin, derajat kompas, dan jarak lurus ke
  tujuan. Arah relatif diperbarui terhadap heading perangkat dan jarak diperbarui dari lokasi GPS.
- Jika koordinat resmi tujuan tidak tersedia, ujung geometri rute dipakai sebagai sasaran orientasi.
- Peringatan pada layar menyatakan bahwa arah lurus bukan rute aman atau rute yang telah diperiksa,
  serta mengarahkan pengguna menjauhi pantai dan mengikuti petugas atau rambu evakuasi.
- Laporan hambatan terakhir tetap masuk antrean luring dan akan dikirim ketika backend tersedia.
- Unit test `:android:app:testDebugUnitTest` lulus, termasuk kalkulasi arah/jarak dan fallback ke
  koordinat terakhir geometri rute.
- **Validasi ditunda atas arahan pengguna:** alur penolakan tiga rute pada perangkat, perubahan arah
  terhadap kompas/GPS nyata, dan pemeriksaan visual pada berbagai ukuran layar.

## 20 September 2026 — P1-08 Perilaku Saat Waktu Evakuasi Habis

- Branch: `feat/p1-countdown-expired`
- Menambahkan keadaan `hasEvacuationWindowExpired` ketika waktu mencapai nol dan pengguna belum tiba.
- Kartu arah dan waktu diganti dengan satu kartu tindakan yang memprioritaskan evakuasi vertikal.
- Garis rute, jalur pendekatan, penanda tujuan, ETA, arahan belokan, riwayat rute, dan pilihan kendala
  dinonaktifkan setelah waktu habis agar aplikasi tidak tetap menyuruh pengguna menuju TES/TEA jauh.
- Mode peta besar menampilkan header khusus “Waktu evakuasi habis” tanpa panduan rute lama.
- Arahan menyebut bangunan evakuasi bertingkat atau bangunan beton bertulang yang tidak tampak rusak,
  penggunaan tangga dan bukan lift, lantai paling atas (sedikitnya lantai 3), menjauhi pantai dan
  sungai, serta mengikuti petugas atau rambu evakuasi.
- Redaksi diperiksa terhadap panduan BMKG dan Pedoman Sosialisasi Penanggulangan Bencana BNPB 2024.
  Validasi terminologi dan arahan lokal dengan BPBD Kota Padang tetap masuk Q-03.
- Unit test `:android:app:testDebugUnitTest` lulus, termasuk keadaan waktu nol dan pengecualian ketika
  pengguna sudah tiba.
- Pemeriksaan visual dengan durasi uji sementara 8 detik lulus pada Infinix X6855 (Android 16):
  kartu ringkas dan header peta besar beralih ke arahan evakuasi vertikal tanpa crash.
- Durasi produksi dikembalikan ke 20 menit setelah pengujian. Pengujian transisi nyata selama
  20 menit serta validasi redaksi oleh BPBD Kota Padang masih diperlukan.

## 20 September 2026 — Status Zona Tidak Menutupi Penanda Pengguna

- Pada peta ringkas, status zona memakai pil pendek di sisi kiri agar tidak mencapai penanda lokasi
  pengguna yang berada di tengah peta.
- Pil ringkas menampilkan status inti dan membuka peta besar ketika diketuk; peta besar tetap
  menampilkan status lengkap beserta legenda warna zona.
- Banner transisi zona yang panjang hanya ditampilkan pada peta besar. Peta ringkas memakai pil
  status tetap sehingga informasi tidak hilang dan panah pengguna tidak tertutup.
- Build debug berhasil dipasang dan kedua mode diperiksa pada Infinix X6855 tanpa crash.
## 20 September 2026 — P1-09 Posisi Awal di Luar Zona Rendaman

- Branch: `feat/p1-outside-zone-start`; belum di-push atas arahan pengguna sambil menunggu perbaikan
  dari Sheva.
- Pemeriksaan zona lokal sekarang dilakukan setelah lokasi pertama diterima dan sebelum permintaan
  rute maupun hitung mundur dimulai.
- Posisi di luar zona hanya diterima bila akurasi GPS maksimal 35 meter. Aplikasi lalu menampilkan
  layar khusus tanpa rute, ETA, hitung mundur, atau tombol pelaporan kendala.
- Layar tetap menampilkan peta dan posisi pengguna, arahan untuk menjauhi pantai dan sungai, serta
  tombol **Periksa posisi lagi**. Hasil pemeriksaan ulang ditampilkan langsung pada kartu.
- Jika GPS belum akurat atau data zona tidak tersedia, aplikasi tetap menyiapkan arahan evakuasi
  sebagai fallback dan terus memeriksa zona pada pembaruan lokasi berikutnya.
- Hitung mundur sekarang dimulai ketika rute berhasil disiapkan, bukan ketika `ViewModel` dibuat.
  Jika pemeriksaan ulang menyatakan pengguna masuk zona rendaman, rute baru dan hitung mundur penuh
  20 menit dimulai.
- Unit test `:android:app:testDebugUnitTest`: 130 lulus, 0 gagal. Empat tes baru mencakup posisi awal
  di luar/dalam zona, GPS lemah, dan data zona tidak tersedia.
- Pemeriksaan perangkat lulus pada Infinix X6855 (Android 16): layar khusus muncul saat aplikasi
  dibuka di luar zona, tidak ada hitung mundur atau tombol **Ada kendala?**, pemeriksaan ulang
  menampilkan “Posisi masih berada di luar zona rendaman.”, dan mode peta besar tetap bersih dari
  kontrol rute.
## 21 September 2026 — Kontrol Lapisan Peta TES & TEA

- Branch: `feat/facility-map-layers`; belum di-push.
- Mode peta pada daftar TES & TEA kini memuat poligon zona dari basis data lokal.
- Tombol **Lapisan** membuka sakelar terpisah untuk zona rendaman, titik TES, dan titik TEA.
- Legenda membedakan area di luar zona rendaman serta bahaya rendah, sedang, dan tinggi.
- Lapisan dapat dimatikan dan dihidupkan kembali tanpa membuka ulang layar. Fasilitas terpilih dan
  kartunya ikut disembunyikan jika jenis fasilitas dimatikan.
- Unit test Android: 130 lulus, 0 gagal.
- Pemeriksaan Infinix X6855 lulus: zona, TES, dan TEA dapat dimatikan secara terpisah; siklus zona
  mati lalu hidup kembali memulihkan poligon dan garis batas.

## 21 September 2026 — Tampilan Widget untuk Posisi di Luar Zona Rendaman

- Branch: `feat/outside-zone-widget-design`; belum di-push.
- Kartu posisi awal di luar zona pada layar evakuasi memakai desain yang sama dengan widget:
  latar biru langit, ilustrasi pegunungan, karakter, dan arahan singkat untuk menjauhi pantai serta
  sungai.
- Header pada mode peta besar memakai ilustrasi dan susunan informasi yang sama. Pesan hasil
  pemeriksaan ulang tetap ditampilkan pada header tanpa mengembalikan kontrol rute atau hitung mundur.
- Tombol **Periksa posisi lagi** dipadatkan dan ditempatkan di kiri bawah. Tombol pusatkan peta berada
  di kanan bawah sehingga kedua tombol tidak saling menutupi.
- Unit test Android: 130 lulus, 0 gagal.
- APK ARM64 dipasang pada Infinix X6855. Tampilan peta kecil, peta besar, serta hasil pemeriksaan ulang
  diperiksa langsung; ilustrasi dan teks terbaca, dan kedua tombol memiliki jarak yang cukup.
## 21 September 2026 — Pembaruan Otomatis dan Interaksi Widget

- Branch: `feat/widget-auto-refresh`; belum di-push.
- Lokasi yang diterima layar evakuasi sekarang langsung dikirim ke widget lebar dan ringkas. Frekuensi
  pembaruan dibatasi paling sering setiap 15 detik agar aliran GPS tidak membuka database setiap detik.
- Kedua widget juga meminta pembaruan berkala sistem setiap 30 menit ketika aplikasi tidak aktif.
- Seluruh permukaan kartu widget membuka aplikasi. Tombol tindakan tetap membuka tujuan yang sama.
- Judul dan rincian status memakai maksimal dua baris; ukuran teks dan posisi informasi rute disesuaikan
  agar kalimat tidak dipotong atau bertumpuk pada widget 2 × 2 maupun 4 × 2.
- Unit test Android: 130 lulus, 0 gagal.
- Pengujian Infinix X6855 lulus: sentuhan pada judul membuka aplikasi, status berubah otomatis dari
  “Lokasi belum terbaca” menjadi “Lokasi di luar zona rendaman” setelah GPS aplikasi memperoleh lokasi,
  dan judul serta rincian tampil penuh tanpa elipsis. Tidak ditemukan crash pada log perangkat.