# Serah Terima Pengembangan Android — SIAGA PADANG

**Ditulis:** 20 September 2026 · **diperbarui hari yang sama** setelah sepuluh commit lanjutan
**Keadaan repositori saat diperbarui:** branch `main`, commit `11740af` (sama dengan `origin/main`)
**Untuk:** siapa pun — manusia atau asisten AI — yang melanjutkan pekerjaan sisi Android tanpa mengikuti riwayat percakapan sebelumnya.

Dokumen ini berisi hal-hal yang **tidak terbaca dari kode dan riwayat commit**: alasan di balik keputusan, hasil pengukuran, jebakan yang sudah ditemui, dan pekerjaan yang masih tertunda. Aturan proyek yang mengikat tetap ada di `CLAUDE.md` — dokumen itu yang menang bila bertentangan dengan dokumen ini.

Urutan membaca yang disarankan: `CLAUDE.md` → dokumen ini → `docs/BACKLOG_FITUR_BELUM_SELESAI.md`.

---

## 1. Keadaan aplikasi hari ini

Aplikasi sudah berjalan utuh pada satu perangkat nyata (Infinix X6855, Android 14) dalam mode pesawat: buka aplikasi → GPS → simpul terdekat → rute dari SQLite → polyline di peta → panah arah mengikuti kompas → hitung mundur. Seluruh fitur inti F-01 sampai F-08 sudah ada di `main`.

| | |
|---|---|
| Berkas Kotlin | 68 |
| Berkas uji unit | 33 berkas, 124 uji, semuanya lulus |
| Layar | Onboarding, Evakuasi, Menu, Rencana Keluarga, Daftar Fasilitas, Panduan, Pengaturan, Tentang |
| Dataset | `ranah_siaga.db` v2026.09.19 |
| Backend | Railway, `https://siagapadang-production.up.railway.app/` |

**Pengukuran NF-02 (waktu penyajian arahan), mode pesawat, Infinix X6855:** median **597 ms**, P90 696 ms, maksimum 727 ms, 21 dari 21 percobaan di bawah 1 detik. Delapan percobaan lain dibuang karena layar mati atau terkunci saat pengukuran. Angka ini baru dari **satu** perangkat — NF-04 (selisih antar-perangkat ≤ 1 detik) belum terbukti.

---

## 2. Cara menjalankan dari nol

1. **Dataset tidak ada di Git.** Ambil `ranah_siaga.db` dari Habib (Google Drive), taruh di `android/app/src/main/assets/`. Tanpa berkas ini aplikasi gagal saat membuka Room.
2. Periksa berkas itu benar sebelum dipakai:
   ```bash
   sha256sum android/app/src/main/assets/ranah_siaga.db
   # harus: ecd93df142533b2625d329df7128f9e7057c5ffd38e76de02fd9d4745fd74786
   ```
   Cocokkan juga dengan `android/app/src/main/assets/dataset_manifest.json`. Kalau checksum berbeda, **jangan** ubah manifest agar cocok — cari tahu dulu dataset mana yang benar. Manifest adalah alat pemeriksa, bukan catatan yang mengikuti berkas.
3. Build: **jangan** `cd android && ./gradlew` — tidak ada `gradlew` di sana, dan tidak ada pula di root. Akar proyek Gradle adalah root repositori, modulnya bernama `:android:app`:

   ```bash
   # Gradle di PATH (9.7.0) menolak AGP. Pakai distribusi yang ditunjuk wrapper:
   GRADLE_BIN=$(echo ~/.gradle/wrapper/dists/gradle-8.14-bin/*/gradle-8.14/bin/gradle)
   "$GRADLE_BIN" :android:app:assembleDebug :android:app:testDebugUnitTest
   ```

   `gradle/wrapper/gradle-wrapper.properties` menyebut Gradle **8.14**, sedangkan `gradle` di PATH mesin Mikail adalah 9.7.0 yang menolak AGP dengan pesan tentang `InternalProblems`. `JAVA_HOME` sudah menunjuk JDK 21 meskipun `java` di PATH adalah Java 8 — biarkan begitu, Gradle memakai `JAVA_HOME`.

   URL backend sudah ada sebagai default di `android/app/build.gradle.kts` dan dapat ditimpa lewat `gradle.properties` (`SIAGA_BACKEND_BASE_URL`).

### Jebakan build: `R.jar` terkunci

Di mesin Mikail, Gradle beberapa kali gagal dengan `java.io.IOException: Couldn't delete ... R.jar` / `Device or resource busy`. Penyebabnya **Kotlin Language Server milik VS Code** yang memegang berkas itu, bukan kesalahan kode. Jangan mematikan proses milik pengguna. Jalan pintas yang dipakai: build ke direktori lain dengan init script.

```kotlin
// altbuild.gradle.kts (taruh di luar repositori, mis. direktori sementara)
allprojects {
    if (path == ":android:app") {
        layout.buildDirectory.set(file("build-verify"))
    }
}
```

```bash
"$GRADLE_BIN" --init-script /path/ke/altbuild.gradle.kts :android:app:assembleDebug
```

`android/app/build-verify/` sudah masuk `.gitignore`. Cara ini pernah **menemukan galat kompilasi nyata** yang tersembunyi di balik kegagalan kunci berkas, jadi jangan menganggap kegagalan build selalu soal kunci berkas.

---

## 3. Aturan yang tidak boleh dilanggar

Sebagian sudah ada di `CLAUDE.md`, diulang di sini karena paling sering nyaris dilanggar:

- **Tidak ada Dijkstra, A*, atau BFS di Android.** Semua rute sudah dihitung Habib. Aplikasi hanya membaca. Ini pernah nyaris terlanggar saat merancang fitur "jalan lain ke TES yang sama" — lihat Bagian 5.
- **Basis data read-only.** Tidak ada `INSERT`/`UPDATE`/`DELETE`, tidak ada migrasi Room terhadap `ranah_siaga.db`.
- **Tabel bawaan tidak punya `@Entity`.** Kueri DAO memakai `@SkipQueryVerification`. Menambahkan `@Entity` untuk tabel-tabel itu membuat Room memvalidasi skema saat runtime dan berisiko menolak berkas 73 MB yang sudah jadi. Jangan "merapikan" ini.
- **`tb_edges` menyimpan satu arah saja.** Semua kueri ruas memakai `WHERE u = :x OR v = :x`, dan koordinat dibalik bila `u` bukan simpul asal. Logika ini hanya boleh ada di satu tempat (`PolylineAssembler`).
- **WKT berurutan `lon lat`.** Tertukar = peta pindah ke Samudra Hindia.
- **Jangan commit `*.db`.**
- **Tidak mengedit folder milik orang lain.** `backend/` milik Sheva, `spatial/` milik Habib. Berkas di root dan `docs/` hanya diedit Mikail.
- **Aturan kata (CLAUDE.md §7):** TES/TEA bukan "shelter", "berjalan cepat" bukan "lari", "alternatif tujuan" bukan "TES penuh". Berlaku juga di nama variabel, komentar, dan pesan commit. BPBD Kota Padang melarang evakuasi dengan berlari — ini bukan soal gaya bahasa.
- **Token admin hanya di variabel lingkungan Railway**, tidak pernah di repositori atau di dalam APK, dan dibandingkan dengan `hmac.compare_digest`.

---

## 4. Desain V3 — apa dan mengapa

Desain V3 dikerjakan di Figma lalu diterapkan ke kode dalam enam commit: `c4acff4` (fondasi) → `72c56df` → `27fb464` → `e8a28b6` → `482c826` → `83392ac` → `6361710` (perapian).

Tata letaknya kemudian dirapikan lagi pada 20 September: `35c064b`, `5420640`, dan `6a727dc`. Yang berlaku sekarang — kompas di kanan atas, tombol berteks "Perbesar peta"/"Perkecil peta" tepat di garis batas kartu dan peta, pil status zona dan pil rute sebelumnya berwarna putih di kiri bawah, tombol pusatkan yang berubah navy beserta gelembung "Kembali ke titik Anda" ketika peta tidak lagi terpusat.

**Berkas Figma:** `kFsajBR2QE1XkEFdxR1rzv`, halaman **App**, node `58:2670`. Berkas itu belum memuat perapian 20 September, jadi kode lebih baru daripada Figma. Desain digambar langsung sebagai layar, bukan sebagai pustaka komponen terpisah — atas permintaan Mikail, untuk menghemat waktu dan token. Jangan membongkarnya menjadi komponen kecuali diminta.

### Keputusan warna

- **Layar evakuasi memakai latar biru tua `#01346D`** (navy aplikasi). Sempat dicoba krem, ditolak: layar darurat harus berbeda secara tegas dari layar masa tenang.
- **Layar masa tenang memakai krem `#F5F2EA` dan kartu putih.** Alasannya pemisahan mode, bukan selera: pengguna harus tahu dalam sekejap apakah sedang dalam mode darurat.
- **Kuning `#F7FF0C` hanya boleh jadi latar dengan teks navy**, tidak pernah jadi warna teks di atas putih — kontrasnya gagal memenuhi 4.5:1.
- Ikon status di layar terang memakai `statusTintOnLight()` agar warna status tetap terbaca di atas putih.

### Tipografi dan ikon

- **Inter**, lima bobot statis di `res/font/` (regular, medium, semibold, bold, extrabold). Lisensi OFL, tercatat di `docs/THIRD_PARTY_LICENSES.md`.
- **57 ikon Material Symbols Rounded** sebagai vector drawable `ic_ms_*.xml` (Apache 2.0, juga tercatat). Ikon panah diunduh ulang dengan `wght700grad200` karena panah tipis tidak terbaca sambil berjalan.
- Pernah terjadi: `sed` menghapus karakter `>` penutup di 55 berkas drawable sekaligus. Kalau memproses XML secara massal, validasi hasilnya (`xml.dom.minidom`) sebelum commit.

### Keputusan antarmuka

- **Fitur yang belum benar-benar ada disembunyikan**, bukan ditampilkan dalam keadaan mati. Halaman Panduan tampil dengan label draf karena isinya belum divalidasi BPBD. TEA ditampilkan sebagai informasi saja, belum sebagai tujuan rute.
- **Tombol "Ada kendala?"** menggantikan "Jalur terhalang". Alasannya: pengguna panik tidak selalu bisa memilih istilah teknis, dan kendala bisa berupa jalan tertutup *atau* TES tidak bisa dimasuki — dua penanganan berbeda.
- **Hitung mundur harus terlihat bergerak.** Angka statis membuat pengguna ragu aplikasi masih hidup, maka ada `RollingDuration` (digit bergulir) dan bilah yang menyusut tiap detik.
- **Ketuk dua kali pada peta** memperbesar/mengecilkan panel peta.
- **Popup status muncul dari ikonnya** (`TransformOrigin` mengikuti posisi ikon) dengan latar putih dan penunjuk kecil, supaya jelas informasi itu milik ikon yang ditekan.

---

## 5. "Jalan lain ke TES yang sama" — batas yang disengaja

Ketika pengguna melapor jalan tertutup, aplikasi lebih dulu mencari **jalan lain menuju TES yang sama** sebelum pindah ke `rank_2`/`rank_3`. Pencariannya hanya **satu lompatan tetangga** (`findEdgesTouchingNode`) — murni pembacaan data, bukan pencarian lintasan.

Simulasi di atas basis data nyata (skrip `detour_sim.py`, dijalankan September 2026):

| Kedalaman | Kasus yang dapat jalan lain | Lebih cepat daripada `rank_2` |
|---|---|---|
| 1 lompatan | 22% | 21% |
| 2 lompatan | 39% | 35% |
| 3 lompatan | 53% | 45% |

Dua dan tiga lompatan memang lebih baik, **tetapi menuntut BFS di perangkat** — dilarang. Kalau angka ini mau dinaikkan, jalannya adalah tabel prakomputasi baru dari Habib, bukan algoritma di Android. Jangan menaikkan kedalaman lompatan diam-diam.

---

## 6. Dataset v2026.09.19

| | |
|---|---|
| Ukuran | 76.414.976 byte (72,9 MiB) |
| SHA-256 | `ecd93df142533b2625d329df7128f9e7057c5ffd38e76de02fd9d4745fd74786` |
| Tabel lama | `tb_nodes` (31.813), `tb_edges` (39.216), `tb_routes` (31.813), `tb_tes` (143), `tb_inundation_zones` (**75**), `tb_safe_zones` (**29**) |
| Tabel baru | `tb_tea` (12), `tb_tea_routes` (31.813), `tb_tea_next` (63.614) |
| View baru | `v_fasilitas_evakuasi` (143 TES + 12 TEA) |

**Berkas ini diganti Habib pada 20 September sore.** Dibanding salinan sebelumnya, zona rendaman bertambah dari 73 menjadi 75 poligon dan kawasan aman berkurang dari 31 menjadi 29. **Label versinya belum berubah** — masih `2026.09.19` — sehingga `dataset_manifest.json` menyebut versi lama untuk isi yang baru. Tanyakan label yang benar kepada Habib, lalu perbarui manifest. Checksum dan ukuran di manifest sudah dihitung ulang dari berkas yang benar-benar dipaketkan.

> ⚠️ Manifest sempat terisi nilai yang tidak cocok dengan berkas mana pun (ukuran 8.557.481 byte) lewat commit `bce00dd`. Kalau angkanya terlihat aneh, hitung ulang langsung dari `assets/ranah_siaga.db`, jangan percaya isi manifest.

Dua hal yang sudah diverifikasi pada versi sebelumnya dan tidak perlu diulang:

- **Enam tabel lama identik dengan versi 2026.09.13** (dibandingkan per baris dengan hash). Artinya `edge_id` lama masih sah, sehingga laporan jalur terhalang yang mengacu ke ID ruas tidak rusak.
- **Pemadatan `tb_tea_next` tidak kehilangan informasi**: 63.626 lintasan, 0 konflik next-hop, semua lintasan berawal di simpul asal dan berakhir di simpul TEA.

**Ukuran melampaui target NF-03 (≤ 75 MB).** 72,9 MiB masih di bawah 75 MB bila dihitung sebagai MB desimal (76,4 MB) — ini sebenarnya **melewati** batas. Perlu diputuskan: naikkan batasnya di proposal dengan alasan penambahan TEA, atau minta Habib memadatkan. Jangan diam-diam mengubah angka target.

### Masalah yang masih terbuka: checksum server ≠ checksum aplikasi

Backend menyimpan dataset dengan checksum `6c12d8…`, aplikasi memakai `5d1549f0…`. Karena laporan jalur terhalang harus menyertakan `dataset_version_id` yang cocok, **laporan sengaja tidak dikirim** selama keduanya berbeda — ini perilaku yang benar, bukan bug. Perbaikannya ada di sisi Sheva: unggah dataset yang identik ke Railway.

---

## 7. Backend Railway — aturan yang ditemukan lewat pengujian

Diuji ujung ke ujung pada September 2026.

- Check-in diterima bila jarak ke TES ≤ `min(20 + akurasi_m, 55)` meter **dan** akurasi GPS ≤ 35 m.
- Laporan okupansi menuntut check-in lebih dulu; tanpa itu server menjawab **403**.
- Laporan jalur terhalang menuntut `dataset_version_id` yang dikenal server (saat diuji: 19 dan 20). Aplikasi dulu mengirim `1` yang dipaku di kode — sudah diperbaiki agar mengambil dari server.
- **Pesan kegagalan harus jujur.** Sebelumnya aplikasi selalu berkata "tersimpan luring, akan dikirim saat sinyal tersedia" walaupun server menolak laporan. Sekarang dibedakan: luring / galat server / ditolak / versi dataset tidak cocok. Kalau menambah jalur kegagalan baru, pertahankan kejujuran ini — prinsip §7 CLAUDE.md.
- Batas tunggu semua permintaan jaringan 2–3 detik, lalu aplikasi lanjut tanpa menunggu.

---

## 8. Apa yang sudah diuji di perangkat, apa yang belum

**Sudah diuji di Infinix X6855 (mode pesawat bila relevan):** alur evakuasi penuh, pembacaan zona, pergantian rute, dialog kedatangan, rencana keluarga (simpan dan pilih titik temu), daftar fasilitas, pengukuran NF-02.

**Sudah diuji pada 20 September, seluruhnya di perangkat:** tujuh perapian UI V3, lembar "Ada kendala?" (ketuk di luar, tombol Batal, seret ke bawah), pil status zona, pil rute sebelumnya, tombol perbesar/perkecil peta, kompas di kanan atas, gelembung "Kembali ke titik Anda", lencana TES/TEA, kartu tujuan di peta, kartu gempa berjenjang, dan ikon BMKG yang memerah serta bergetar.

**Belum diuji di perangkat sama sekali:**

1. **Pengalihan rute saat keluar jalur** (`OffRouteTracker`, commit `6a727dc`). Aturannya tertutup enam uji unit, tetapi perilakunya di lapangan menuntut berjalan lebih dari 150 m dari rute.
2. Tombol **Bagikan** dan **Hapus** pada rencana keluarga.
3. Laporan jalur terhalang yang benar-benar sampai ke server — terhalang beda checksum (Bagian 6).
4. Blok "gempa terdekat dari Padang" — menunggu backend menyertakan `regional_event`.
5. NF-04 pada tiga perangkat lain.

Galat yang sudah pernah ditemukan **hanya karena diuji di perangkat**, bukan lewat kompilasi: tombol Simpan tak terlihat saat nonaktif, saran GPS muncul setelah 5 detik, pintasan menutupi nama tujuan, garis jalan tergambar di atas rute, banner menutupi kartu arah, ikon status bar putih di layar krem, penanda fasilitas tak terlihat. **Jangan menyatakan pekerjaan UI selesai sebelum dilihat di layar nyata.**

---

## 9. Pekerjaan yang tertunda

### Mikail (Android)

- **Perilaku saat hitung mundur habis.** Sekarang tidak terjadi apa-apa: `startCountdown()` berhenti di `00:00` lalu perulangannya putus. Arahan "terus berjalan" menjadi keliru pada titik itu. Kalimat penggantinya arahan keselamatan, jadi harus divalidasi BPBD (Q-03).
- **P1-09 posisi awal di luar zona rendaman sudah dikerjakan di branch `feat/p1-outside-zone-start`.** Pemeriksaan zona lokal kini mendahului rute dan hitung mundur. Posisi luar zona dengan akurasi GPS maksimal 35 meter menampilkan layar khusus serta tombol pemeriksaan ulang; GPS lemah atau data zona tidak tersedia tetap memakai arahan evakuasi sebagai fallback. Unit test dan uji Infinix X6855 lulus. Branch belum di-push atas arahan pengguna.
- **Tampilan lanjutan P1-09 dikerjakan di branch `feat/outside-zone-widget-design`.** Kartu layar evakuasi dan header peta besar kini mengikuti desain widget dengan ilustrasi gunung serta karakter. Tombol **Periksa posisi lagi** dibuat ringkas di kiri bawah dan tombol pusatkan tetap di kanan bawah. Sebanyak 130 unit test lulus dan tampilan peta kecil, peta besar, serta hasil pemeriksaan ulang sudah diuji pada Infinix X6855. Branch belum di-push.
- **Pemberitahuan bila pengguna mengikuti rute lama** setelah berpindah ke alternatif tujuan.
- Uji lapangan pengalihan keluar jalur.
- Bersihkan composable lama yang tidak terpakai di `EvacuationScreen.kt` (berkas sudah di atas 3.500 baris).
- P1-02 basemap benar-benar luring (rencana ada di `docs/OFFLINE_BASEMAP_PLAN.md`).
- NF-04 pada tiga perangkat tambahan.
- Putuskan status NF-03 setelah dataset membesar (Bagian 6).

### Sheva (backend)

- Muat dataset dengan checksum `ecd93df1…` ke Railway agar laporan jalur terhalang dapat terkirim.
- Deploy ulang backend: balikan `/api/v1/status/bmkg` yang hidup masih versi lama, tanpa `regional_event`, walau kodenya sudah ada di `main` (`87a72a3`).
- Berhenti mengubah berkas di `android/` (lihat Bagian 13).
- Perbaiki galat 500 pada endpoint laporan jalur terhalang.
- `/sync/check` hanya boleh melaporkan versi yang benar-benar lebih baru.
- Periksa penghitungan check-in ganda.
- Hapus data uji: perangkat `e2e-uji-claude-1`, `TES_56`, event `EVENT-PADANG-TEST-01`.
- Keluarkan `backend/cache/` dari Git (14,7 MB JSON; ini yang membuat `git fetch` gagal di koneksi lambat).
- Nonaktifkan atau hapus `deploy-hf.yml` — Hugging Face tidak jadi dipakai, yang dipakai Railway.
- Jalankan `pytest`.

### Habib (spasial)

- Konfirmasi jumlah TEA: dokumen menyebut 29 kawasan, dataset berisi 12. Mana yang benar?
- Sumber klasifikasi `tingkat_bahaya` pada `tb_inundation_zones` — dipakai untuk warna legenda zona, jadi harus bisa dipertanggungjawabkan.
- Tambahkan indeks `(lat, lon)` pada `tb_nodes`.
- Putuskan TEA berbatas kapasitas atau sekadar terdekat.
- Jalankan ulang angka simulasi dengan TES + TEA.
- Issue [#1](https://github.com/AkuSukaProject/siagapadang/issues/1) — TEA sebagai tujuan evakuasi (P1-07).

---

## 10. Cara kerja repositori

- Satu branch per pekerjaan: `feat/f03-routing`, lalu PR ke `main`.
- **Catatan penting:** Mikail meminta review antar-anggota dilewati untuk mempercepat; pekerjaan digabung langsung ke `main`. Ini berbeda dari janji Subbab 4.5 proposal. Kalau juri membuka repositori, siapkan penjelasannya.
- Awalan commit: `feat(android):`, `fix(backend):`, `chore(spatial):`, `docs:`.
- Judul issue merujuk kode kebutuhan: `[F-03] …`, `[NF-03] …`.
- Papan GitHub Projects: Backlog · Sprint Berjalan · Review · Selesai.

**Kalau `git fetch` gagal dengan `unpack-objects failed`:** biasanya koneksi lambat ditambah objek besar di `backend/cache/`. Ulangi `git pull`, atau gabungkan lewat PR di web GitHub.

**Kalau berkas tampak "kembali ke versi lama":** periksa `git branch --show-current` lebih dulu. Hal ini pernah terjadi — checkout tertinggal di `feat/p1-route-fallback` sementara seluruh pekerjaan V3 ada di `main`. Tidak ada yang hilang.

---

## 11. Yang tidak ikut berpindah antar-asisten

Kalau pengembangan dilanjutkan oleh asisten AI lain, yang **terbawa** hanyalah isi repositori: kode, `CLAUDE.md`, `docs/`, riwayat commit, issue GitHub, dan berkas Figma. Yang **tidak terbawa**: riwayat percakapan, memori asisten yang tersimpan lokal di mesin Mikail, serta alasan-alasan yang tidak pernah dituliskan. Dokumen ini dibuat untuk menutup sebagian celah itu.

Satu aturan kerja yang selama ini berlaku dan layak diteruskan: **sebelum memulai tugas besar, sarankan tingkat usaha (effort) yang sesuai dan tunggu Mikail menetapkannya.**

---

## 12. Keputusan yang sudah ditolak — jangan diusulkan ulang

Daftar lengkap ada di `CLAUDE.md` §5. Ringkasnya: deteksi guncangan lewat akselerometer, pelacakan posisi keluarga langsung, tingkat keterisian TES saat luring, ambang magnitudo sendiri, dan perhitungan rute di perangkat. Semuanya sudah dipertimbangkan dan ditolak dengan alasan tercatat. Kalau menurut Anda salah satu keputusan itu keliru, sampaikan argumennya — jangan mengusulkannya kembali seolah belum pernah dibahas.

---

## 13. Catatan kerja 20 September

### Kinerja peta — jangan dibatalkan tanpa alasan

`OfflineMap.kt` punya `OverlaySnapshot`, pembanding isi sebelum sumber GeoJSON ditulis ulang. Tanpa itu, seluruh overlay dibangun ulang setiap recomposition, padahal layar evakuasi disusun ulang terus-menerus karena arah kompas dan hitung mundur. Diukur dengan `dumpsys gfxinfo` pada enam geseran peta yang sama: frame tersendat **0,92% → 0%**, persentil 95 **101 ms → 29 ms**, persentil 99 **200 ms → 77 ms**. Frame 200 ms itulah yang dulu membuat ketukan kedua tidak terbaca sebagai ketuk ganda.

### Alat pengujian yang terbukti berguna

- **Jangan percaya `Log.d` di Infinix X6855.** ROM-nya membuang log level debug dari aplikasi; log proyek yang terlihat hanya level I dan W. Aktifkan dengan `adb shell setprop log.tag.NamaTag VERBOSE`, atau pakai `Log.i`. Dua jam sempat terbuang karena menyangka fitur tidak jalan, padahal lognya yang tidak muncul.
- **Mengukur kelancaran:** `adb shell dumpsys gfxinfo <paket> reset`, lakukan geseran, lalu baca `Janky frames` dan persentil.
- **Membuktikan animasi:** ambil belasan tangkapan layar berurutan lalu bandingkan selisih pikselnya. Getaran ikon BMKG (0,4 detik tiap 3 detik) terbukti dengan cara ini.
- **Menguji ketuk ganda:** `adb shell "input tap X Y; input tap X Y"` — kadang gagal karena jeda antar-proses melewati 300 ms, jadi kegagalan sekali bukan bukti fitur rusak.

### Batas yang dipilih dan alasannya

| Nilai | Berkas | Alasan |
|---|---|---|
| 150 m, 3 pembaruan, ketelitian ≤ 50 m | `OffRouteTracker` | Ambang pengalihan rute; lompatan GPS sesaat tidak boleh mengganti rute yang sedang diikuti |
| 1.500 km | `EarthquakeRelevance` | Radius peringatan tsunami layar penuh; mencakup Mentawai, Nias, Bengkulu, Aceh |
| 96 dp | `ObstacleSheet` | Jarak seret untuk menutup lembar |
| 0,4 detik tiap 3 detik | `StatusCircle` | Getaran ikon kabar BMKG |

### Insiden: commit lintas folder

Commit `bce00dd` dari Sheva mengubah **sepuluh berkas di `android/`** — termasuk `EvacuationScreen.kt`, `EvacuationViewModel.kt`, dan `dataset_manifest.json` — padahal `CLAUDE.md` Bagian 9 menyatakan tidak ada yang mengedit folder milik orang lain. Akibatnya: uji unit tidak dapat dikompilasi (tiga kueri TEA ditambahkan tanpa melengkapi `FakeEvacuationDao`), manifest terisi angka yang tidak cocok dengan berkas mana pun, dan berkas sesi kompiler Kotlin ikut ter-commit. Ketiganya sudah diperbaiki pada `dcf64ac`.

Isi perubahannya sendiri berguna — dialog konfirmasi check-in dan pembacaan rute TEA (`findTeaRoute`, `findTeaPathSteps`). Kueri TEA itu menelusuri `tb_tea_next` per lompatan dengan CTE rekursif; itu **sah** karena membaca data prakomputasi, bukan pencarian lintasan, tetapi waktunya belum diukur terhadap NF-02. Pekerjaan TEA juga milik issue #1 yang ditugaskan ke Habib, jadi perlu disepakati siapa yang melanjutkan.

**Pelajaran untuk penerus:** setelah `git pull`, jalankan `:android:app:testDebugUnitTest` sebelum melanjutkan. Rebase yang bersih tidak berarti kodenya masih dapat dikompilasi.
