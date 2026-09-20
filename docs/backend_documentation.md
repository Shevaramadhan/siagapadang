# Dokumentasi Komprehensif Backend Siaga Padang

Dokumen ini berisi panduan teknis mendalam dan spesifikasi menyeluruh mengenai *backend* aplikasi **Siaga Padang** (Gemastik 2026). *Backend* ini bertindak sebagai pusat validasi data (baik dari warga maupun sumber resmi), pemrosesan spasial (geografis), penyedia *Over-the-Air* (OTA) *mapping database*, serta agregasi dan caching data peringatan dini (BMKG).

---

## 1. Arsitektur dan Teknologi (Tech Stack)

Sistem *backend* dibangun dengan mengedepankan **performa (asynchronous)** dan **kemampuan komputasi spasial (*geographic computation*)**.

### A. Teknologi Utama
1. **Bahasa Pemrograman:** Python 3.12+ (Mendukung asinkronisasi tingkat lanjut dengan `asyncio`).
2. **Web Framework:** **FastAPI**
   - Sangat cepat karena berbasis ASGI (*Asynchronous Server Gateway Interface*).
   - Mendukung validasi data *payload* masuk dan keluar secara otomatis melalui **Pydantic**.
   - Otomatis menghasilkan dokumentasi Swagger UI / OpenAPI (`/docs`).
3. **Database Relasional & Geospasial:** **PostgreSQL + PostGIS**
   - Menggantikan SQLite murni karena SQLite terbatas dalam menghitung jarak *real-time* berbasis lengkung bumi (Spherical) tanpa add-on rumit. PostGIS mendukung tipe data `Geometry(POINT)` untuk titik lokasi secara *native*.
4. **ORM (Object-Relational Mapping):** **SQLAlchemy 2.0 (Async)**
   - Menggunakan sintaks 2.0 yang modern dengan fitur `async_session` untuk menghindari pemblokiran *thread* jaringan (non-blocking I/O).
5. **Ekstensi Geospasial ORM:** **GeoAlchemy2**
   - Menyediakan tipe `Geometry` dan fungsi-fungsi spasial seperti `ST_DWithin` untuk dipakai langsung dari SQLAlchemy Python.
6. **Database Migration:** **Alembic**
   - Melacak perubahan skema database (contoh: tambah kolom, buat tabel spasial) layaknya *version control* (Git) untuk database.
7. **HTTP Client:** **`httpx`**
   - Digunakan untuk melakukan *request API* secara asinkron (khususnya ke server BMKG).

---

## 2. Struktur Direktori

Kode *backend* dipecah menjadi modul-modul (MVT - *Model View Template* pattern yang disesuaikan untuk API):
```text
backend/
├── alembic/                # Berisi file migrasi riwayat perubahan struktur database (script)
├── app/
│   ├── api/
│   │   └── endpoints/      # Controller API (shelters.py, bmkg.py, reports.py, sync.py)
│   ├── core/
│   │   ├── config.py       # Pengaturan environment variables (Database URL, padang coord)
│   │   └── database.py     # Setup koneksi SQLAlchemy Asynchronous ke PostgreSQL
│   ├── models/
│   │   └── domain.py       # Definisi Entity SQLAlchemy (Tabel database di PostGIS)
│   ├── schemas/            # Definisi Pydantic (Validasi Request Payload & Response JSON)
│   └── main.py             # Entry point FastAPI, inisialisasi router dan middleware (CORS)
├── tests/                  # Script unit & integration tests (misal: test_v3_comprehensive.py)
├── alembic.ini             # Konfigurasi Alembic
└── requirements.txt        # Daftar dependency Python
```

---

## 3. Desain Model Data dan Database (PostGIS)

Semua entitas yang membutuhkan komputasi jarak didefinisikan menggunakan GeoAlchemy2 dengan sistem proyeksi EPSG 4326 (WGS 84 - Standar GPS Latitude & Longitude).

### A. Tabel `tb_shelters`
Menyimpan titik Tempat Evakuasi Akhir (TEA) dan Tempat Evakuasi Sementara (TES).
- `id` (String): Primary Key, kode shelter (misal: TEA_G1).
- `name` (String): Nama asli tempat evakuasi.
- `capacity` (Integer): Batas maksimal tampungan warga.
- `current_occupancy` (Integer): Jumlah warga yang telah lapor selamat di lokasi.
- `location` (Geometry(POINT, 4326)): Titik koordinat latitude dan longitude shelter tersebut.

### B. Tabel `tb_obstacles` (Hambatan Rute)
Menyimpan pengaduan masyarakat mengenai rute (jalan) yang tidak bisa dilewati.
- `id` (Integer): Primary Key (Auto-increment).
- `device_id` (String): ID unik (UUID) ponsel pelapor, mencegah pengaduan ganda berulang-ulang (spam).
- `type` (String): Jenis halangan (Banjir, Longsor, Gedung Runtuh, dll).
- `location` (Geometry(POINT, 4326)): Titik halangan tersebut berada.
- `is_verified` (Boolean): Jika `True`, artinya titik ini terkonfirmasi dan disiarkan balik ke warga lain (OTA Sync). Default: `False`.
- `created_at` (Timestamp): Waktu lapor.

---

## 4. Rincian Fungsi dan Endpoint API

Sistem backend mengekspos beberapa jalur (endpoint) yang dihubungkan di file `app/main.py`.

### 4.1. Integrasi dan Peringatan Gempa (BMKG)
File: `app/api/endpoints/bmkg.py`
API untuk mengambil, menyaring, dan menyimpan sementara (cache) data gempa nasional & regional.

*   **Endpoint:** `GET /api/v1/status/bmkg`
*   **Logika Kritis:**
    1.  **5-Minute Caching:** Mengambil JSON langsung dari URL resmi BMKG (`autogempa.json` & `gempaterkini.json`). Untuk mencegah aplikasi memblokir/DDoS server BMKG, *backend* menyimpan hasil panggilannya di dalam memori (`bmkg_cache` dict) selama 5 menit. Jika ada permintaan sebelum 5 menit, *backend* merespons dari memori (dengan tambahan status `regional_data_status="stale"` atau `"live"`).
    2.  **Graceful Degradation (NoneType Fix):** Jika sewaktu-waktu server BMKG tumbang atau memberikan *response dictionary* kosong `{}` saat proses penyegaran (refresh), sistem akan tetap hidup. Jika ada *cache* lama, akan dikembalikan dengan label `"stale"`. Jika server baru nyala dan *cache* belum ada (kosong), API tidak akan *crash* melainkan melempar HTTP 503 ("Service Unavailable") agar aplikasi Android bisa menanganinya dengan aman.
    3.  **Filtrasi Jarak Regional (1.500 km):** Kota Padang di-*hardcode* di titik Latitude `-0.9471`, Longitude `100.4172`. Semua gempa terkini dari BMKG di-loop. *Backend* akan menghitung jarak episentrum BMKG dengan kota Padang memakai fungsi *Haversine* di Python. Jika terdapat gempa $\le$ 1500 km, sistem akan mencari dan memfilter jarak absolut **terpendek** (`min_distance`) lalu disematkan ke field `regional_event` ke dalam JSON.

### 4.2. Validasi Kedatangan Warga (Shelter Check-in)
File: `app/api/endpoints/shelters.py`

*   **Endpoint:** `GET /api/v1/shelters/occupancy`
    *   Mengembalikan daftar JSON seluruh shelter, mencakup batas `capacity` dan `current_occupancy`.
*   **Endpoint:** `POST /api/v1/shelters/occupancy`
    *   **Payload (JSON):** `{"shelter_id": "TEA_01", "user_lat": -0.9, "user_lon": 100.4}`
    *   **Logika PostGIS (Radius 35 Meter):** Memanggil *function* khusus `ST_DWithin` bawaan PostGIS dengan format _Geography_ `ST_DWithin(location, ST_GeogFromText('POINT(lon lat)'), 35.0)`.
    *   Artinya, radius kelonggaran akurasi GPS HP maksimal 35 meter. Jika `user_lat` dan `user_lon` melenceng lebih dari itu dari titik `location` di database, maka *backend* menolak permintaan *check-in* (HTTP 400 - Location Verification Failed).
    *   Jika cocok, kolom `current_occupancy` bertambah +1. (Gagal jika `current_occupancy` $\ge$ `capacity`).

### 4.3. Pelaporan Kerusakan Jalan (Crowdsourced Obstacles)
File: `app/api/endpoints/reports.py`

*   **Endpoint:** `GET /api/v1/reports/obstacles`
    *   Mengembalikan titik koordinat (GeoJSON / koordinat array) hanya untuk laporan yang **`is_verified = true`**.
*   **Endpoint:** `POST /api/v1/reports/obstacles`
    *   **Payload (JSON):** `{"type": "banjir", "lat": -0.91, "lon": 100.42, "device_id": "UUID-1234"}`
    *   **Logika Ambang Batas 3 Laporan (Crowdsource Validation):** Mencegah orang usil yang mengklaim jalan rusak padahal tidak. 
    *   Pertama, database dicek dengan radius 15 meter dari titik lapor: Berapa banyak laporan yang ada di radius itu dengan `device_id` yang berbeda dari pelapor ini? (`distinct()`)
    *   Jika sudah ada $\ge 2$ laporan (artinya dengan laporan sekarang menjadi $\ge 3$), maka secara otomatis atribut `is_verified` akan berubah menjadi `True`. Laporan inilah yang akan berdampak mematikan rute peta warga lain di *update* OTA selanjutnya.

### 4.4. Distribusi Peta Offline (Over-the-Air Sync)
File: `app/api/endpoints/sync.py`

Karena Siaga Padang menuntut aplikasi tetap bisa bernavigasi tanpa jaringan internet saat bencana (menggunakan algoritma A* Pathfinding bawaan Android), maka topologi jalannya harus disinkronisasikan terlebih dahulu.
*   **Endpoint:** `GET /api/v1/sync/ota/check`
    *   Mengembalikan metadata tentang status database navigasi (file SQLite terenkripsi/zip). Isinya mencakup `version_name` (mis. "2026.3") dan `checksum_sha256`. 
    *   Android menggunakan ini untuk mencocokkan *checksum* file SQLite lokal di ponsel.
*   **Endpoint:** `GET /api/v1/sync/ota/download`
    *   Memberikan respons berupa `FileResponse` berisi file `ranah_siaga_v3.db`. File ini berisi node dan edge jalan raya yang nantinya jika digabungkan dengan data dari endpoint obstacles, Android bisa menemukan jalan alternatif di sekitar jalan terputus.

---

## 5. Menjalankan Server & Migrasi Lingkungan Lokal (Dev)

### 5.1 Kebutuhan Lingkungan (Environment)
Aplikasi ini wajib dihubungkan ke PostgreSQL dengan *plugin* PostGIS.
1. Install PostgreSQL.
2. Pasang ekstensi postgis (di pgAdmin atau SQL shell: `CREATE EXTENSION postgis;`).
3. Buat database, contoh: `siagapadang_db`.
4. Salin file `.env.example` ke `.env` (atau deklarasikan) dan atur URL:
   `DATABASE_URL=postgresql+asyncpg://postgres:password@localhost/siagapadang_db`

### 5.2 Migrasi Alembic (Inisialisasi Tabel)
Alembic menangani pembentukan tabel secara otomatis. Masuk ke direktori `backend/` lalu jalankan:
```bash
alembic upgrade head
```
Perintah ini akan mencari migrasi terbaru (contoh: `618783807a2f` yang merupakan *squashed migration* awal kita) dan menerapkan tipe Geometri di database secara mulus.

### 5.3 Menjalankan Server Uvicorn
Menyalakan *backend* untuk testing API atau koneksi aplikasi.
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
Server akan menyala dan *Interactive Swagger Docs* dapat diakses di browser melalui URL: `http://localhost:8000/docs`.

---

## 6. Pengujian Otomatis (Unit Testing)

File pengujian di-host di `/tests/test_v3_comprehensive.py`. Pengujian ini dibuat menggunakan kerangka kerja asinkron bawaan FastAPI dan Pytest.

**Cakupan Tes meliputi:**
1. **Mocking BMKG:** Mensimulasikan server BMKG yang *down*, memberikan data palsu/kosong, lalu memverifikasi bahwa *backend* kita tidak crash dan merespons `(Tersimpan)` saat mengandalkan cache, atau *503 Unavailable* jika sepenuhnya kosong. Memverifikasi juga kalau radius $>1500$ km akan dibuang (Null).
2. **Mocking PostGIS Spatial:** Memverifikasi jarak check-in 34 meter (diterima) vs 36 meter (ditolak) lewat validasi koordinat murni. 

*Catatan: Tes ini bergantung langsung pada fungsi spasial di PostgreSQL, sehingga unit test lokal di dalam SQLite memori tidak dapat dilakukan (karena `ST_DWithin` dan `Geometry` adalah fungsi eksklusif PostGIS).*
