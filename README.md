# Siaga Padang

Siaga Padang adalah aplikasi Android navigasi evakuasi tsunami yang dirancang agar fungsi intinya tetap berjalan tanpa koneksi internet.

## Sumber kebenaran

Dokumen dibaca dengan urutan berikut:

1. [`CLAUDE.md`](CLAUDE.md) — keputusan teknis dan ruang lingkup terbaru; menang jika ada pertentangan.
2. [`CONTEXT.md`](CONTEXT.md) — latar proyek, kompetisi, tim, dan alasan keputusan.
3. [`PRD.md`](PRD.md) — spesifikasi produk historis yang masih berlaku selama tidak bertentangan dengan `CLAUDE.md`.

## Struktur repositori

```text
.
|-- android/   # Aplikasi Android Kotlin + Jetpack Compose
|-- backend/   # Layanan jaringan pelengkap
|-- spatial/   # Pipeline data spasial dan prakomputasi rute
|-- docs/      # Arsitektur, proposal, ERD, dan dokumentasi
|-- CLAUDE.md
|-- CONTEXT.md
`-- PRD.md
```

Ketiga area kode tidak saling mengimpor. Artefak penghubungnya adalah `ranah_siaga.db`, yang ditempatkan secara lokal di `android/app/src/main/assets/` dan tidak dimasukkan ke Git.

## Fokus Android saat ini

Alur evaluasi utama adalah GPS → node terdekat → rute lokal → perakitan geometri → polyline → kompas → hitung mundur. Android hanya membaca hasil prakomputasi; aplikasi tidak menjalankan Dijkstra, A*, atau BFS.

Lihat [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) sebelum membuat modul atau kelas baru.

Sebelum melanjutkan pekerjaan yang sudah berjalan, baca [`docs/HANDOFF.md`](docs/HANDOFF.md) — keadaan aplikasi terkini, alasan di balik keputusan desain, jebakan build, dan pekerjaan yang tertunda — lalu [`docs/BACKLOG_FITUR_BELUM_SELESAI.md`](docs/BACKLOG_FITUR_BELUM_SELESAI.md) sebagai sumber status fitur.

## Lisensi

Kode sumber dilisensikan di bawah MIT License (lihat berkas [`LICENSE`](LICENSE)).

Data jaringan jalan bersumber dari OpenStreetMap, dilisensikan di bawah
Open Database License (ODbL). Atribusi peta: © OpenStreetMap contributors.
Data titik evakuasi bersumber dari BPBD Kota Padang dan digunakan untuk
keperluan akademik.

Komponen pihak ketiga tetap mengikuti lisensi masing-masing. Salinan lisensi
kode untuk paket penyerahan tersedia di [`deliverables/LICENSE`](deliverables/LICENSE).
