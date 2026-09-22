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
## 21 September 2026 — Tombol Peta dan Sudut Header Luar Zona

- Branch: `fix/outside-zone-map-toggle`; belum di-push.
- Klik tombol **Perbesar peta** dan **Perkecil peta** dipisahkan dari pengenal gerakan tarik. Gerakan
  kecil jari tidak lagi membuat klik pada tombol terlihat batal, sementara peta masih dapat ditarik.
- Ilustrasi header peta besar pada keadaan luar zona diperlebar dan digeser melewati batas kiri/kanan.
  Sudut putih yang menjadi bagian dari gambar widget tidak lagi masuk ke area header.
- Unit test Android: 130 lulus, 0 gagal; APK debug berhasil dibangun dan dipasang pada Infinix X6855.
- Siklus Perbesar → Perkecil lulus melalui area tombol yang dilaporkan UI perangkat dan tidak ada crash.
  Status luar zona tidak dapat dipicu ulang pada sesi validasi akhir karena akurasi GPS sekitar 46,6 m,
  sedangkan pemeriksaan awal mensyaratkan maksimal 35 m.

## 21 September 2026 — Posisi Pil Status Luar Zona

- Branch: `fix/outside-zone-pill-position`; belum di-push.
- Pada peta kecil, pil **Di luar zona** dipindahkan dari tengah sisi kiri ke kiri bawah, tepat di atas
  tombol **Periksa posisi lagi**. Posisi status lain tetap seperti sebelumnya.
- Unit test Android: 130 lulus, 0 gagal; APK debug berhasil dibangun dan dipasang pada Infinix X6855.
- Validasi langsung pada keadaan luar zona menunjukkan pil dan tombol sejajar pada sisi kiri, memiliki
  jarak vertikal 49 piksel, dan tidak bertabrakan dengan tombol pusatkan peta di sisi kanan.

## 21 September 2026 — Pemeriksaan Zona Sebelum Menampilkan Rute

- Branch: `fix/initial-zone-before-route`; belum di-push.
- Jika pembacaan awal berada di luar zona tetapi akurasi GPS masih di atas 35 meter, aplikasi kini
  menahan pembuatan rute dan menampilkan **Menunggu GPS lebih akurat…**.
- Rute baru disiapkan setelah posisi terbaca di dalam zona. Jika data zona tidak tersedia, rute tetap
  disiapkan sebagai arahan keselamatan cadangan.
- Unit test Android: 134 lulus, 0 gagal; APK debug berhasil dibangun dan dipasang pada Infinix X6855.
- Enam sampel keadaan layar setelah aplikasi dimulai ulang menunjukkan urutan **Menunggu GPS lebih
  akurat** lalu **Anda berada di luar zona rendaman** tanpa kartu atau teks rute muncul di antaranya.

## 21 September 2026 — Popup Pemeriksaan, Ilustrasi, dan Orientasi Terakhir

- Branch: `fix/outside-zone-popup-artwork`; belum di-push.
- Hasil tombol **Periksa posisi lagi** kini muncul sebagai popup di tengah layar, dapat ditutup, dan
  hilang otomatis setelah empat detik. Pesan hasil tidak lagi mengganti keterangan pada kartu aman.
- Ilustrasi luar zona pada kartu kecil sekarang memenuhi tinggi kartu dengan crop terarah. Pada header
  peta besar, rasio asli aset dipertahankan agar karakter dan latar tidak terlihat gepeng.
- Halaman **Orientasi terakhir** memakai kartu putih lebar, panel arah biru, bagian tujuan yang jelas,
  serta kartu peringatan terpisah. Mode peta besar kini memiliki header orientasi dengan arah, jarak,
  dan nama tujuan.
- Unit test Android: 134 lulus, 0 gagal; APK debug berhasil dibangun dan dipasang pada Infinix X6855.
- Popup pemeriksaan serta orientasi terakhir pada peta kecil dan besar sudah diperiksa langsung. State
  orientasi dipicu dengan menghabiskan tiga rute sebelumnya; teks, ikon, dan kontrol tidak terpotong.

## 21 September 2026 — Kontrol Peta dan Umur Informasi BMKG

- Branch: `fix/map-controls-bmkg-age`; belum di-push.
- Tombol pusatkan dinaikkan 12 dp agar tidak terlalu dekat dengan kontrol bawah.
- Pil rute sebelumnya ditempatkan di atas indikator zona sehingga tidak lagi menutup status bahaya
  rendah. Indikator zona pada peta besar sekarang berupa tombol ikon 48 dp dan tetap dapat diketuk
  untuk membuka nama status serta legenda lengkap.
- Ikon toa BMKG yang belum dibaca tetap merah, kini berkedip, memiliki garis merah berkedip, dan
  selalu menampilkan titik merah agar statusnya terlihat pada setiap fase animasi.
- Respons `datetime` utama BMKG sekarang ikut dibaca Android. Kartu info dan dialog tsunami
  menampilkan umur kejadian seperti **17 menit lalu** atau **3 jam lalu**, diperbarui setiap menit.
- Audit notifikasi Android: aplikasi belum memiliki notification channel, izin `POST_NOTIFICATIONS`,
  worker/push receiver, atau pengiriman notifikasi ke status bar. Yang tersedia saat ini adalah
  indikator di dalam aplikasi, dialog layar penuh untuk potensi tsunami yang relevan, dan widget.
- Potensi tsunami sudah dibedakan: kejadian baru dalam radius relevansi 1.500 km memicu dialog layar
  penuh; kejadian yang jauh tetap dijelaskan pada kartu BMKG tanpa mengambil alih layar.
- Unit test Android: 138 lulus, 0 gagal; APK debug berhasil dibangun dan dipasang pada Infinix X6855.
  Validasi visual perangkat belum selesai karena layar perangkat terkunci saat pemeriksaan terakhir.

## 22 September 2026 — Ukuran Ikon Launcher

- Branch: `fix/smaller-launcher-icon`; belum di-push.
- Ruang tepi adaptive icon ditambah dari 12 dp menjadi 20 dp. Logo yang terlihat pada home screen
  menjadi lebih kecil dan tidak memenuhi bidang ikon, sedangkan ukuran area sentuh launcher tetap
  mengikuti standar Android.
- Perubahan hanya berlaku pada ikon launcher; ukuran logo splash screen tidak berubah.
- APK debug berhasil dibangun dan dipasang pada Infinix X6855. Ikon diperiksa melalui hasil pencarian
  app drawer: logo memiliki ruang tepi yang jelas, tidak terpotong, dan label aplikasi tetap utuh.

## 22 September 2026 — Kontrol Zona, Riwayat Rute, dan Kartu Tujuan

- Branch: `feat/route-history-map-polish`; belum di-push.
- Tombol status zona pada peta kecil kini berupa ikon bulat 48 dp, sama dengan tombol pusatkan. Nama
  zona tetap tersedia lewat deskripsi aksesibilitas dan legenda lengkap pada mode peta besar.
- Daftar **Rute sebelumnya** kini interaktif. Memilih tujuan lama menghitung ulang rute dari posisi
  pengguna terbaru, memindahkan rute aktif ke daftar, dan memungkinkan pengguna kembali lagi ke
  alternatif tanpa memakai garis lama yang sudah tertinggal.
- Daftar rute lama dibatasi lebarnya agar tidak bertabrakan dengan gelembung tombol pusatkan. Teks
  tombol pusatkan dipadatkan dari **Kembali ke titik Anda** menjadi **Ke posisi Anda**.
- Kartu nama, jarak, dan waktu tujuan dipisahkan dari pin tujuan. Kartu memakai susunan lebih ringkas
  dan berpindah tegak lurus terhadap ruas terakhir ketika arah/rotasi peta berubah, sehingga garis
  jalan menuju tujuan tetap terlihat.
- Status pengiriman laporan hambatan, termasuk penolakan posko, sekarang muncul sebagai dialog tengah
  bergaya V3 dengan keadaan diproses, diterima, disimpan, atau belum diterima; banner lama dihapus.
- Audit keadaan darurat: backend menganggap darurat hanya ketika ada `EmergencyEvent` nyata berstatus
  `ACTIVE`. Android sudah memiliki `getActiveEvent()`, tetapi belum memanggilnya dan belum mengubah UI
  berdasarkan event backend. Potensi tsunami BMKG saat ini hanya memerahkan indikator serta membuka
  dialog layar penuh bila kejadian baru dan relevan; status itu tidak otomatis mengaktifkan event backend.
- Unit test Android: 140 lulus, 0 gagal. APK ARM64 dipasang pada Infinix X6855. Pemilihan TEA → TES →
  TEA berhasil; tujuan yang ditinggalkan berganti di daftar rute lama, dan daftar tidak menutupi tombol
  pusatkan. Kartu tujuan serta garis rute diperiksa pada mode peta besar.