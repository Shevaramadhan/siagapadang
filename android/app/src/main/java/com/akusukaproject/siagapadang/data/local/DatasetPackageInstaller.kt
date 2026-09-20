package com.akusukaproject.siagapadang.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.akusukaproject.siagapadang.data.model.LocalDatasetManifest
import com.akusukaproject.siagapadang.data.model.RemoteDatasetVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class DatasetInstallResult(
    val manifest: LocalDatasetManifest,
    val restartRequired: Boolean,
)

class DatasetPackageInstaller(
    context: Context,
    private val baseUrl: String,
    private val appVersion: String,
    private val storage: DatasetStorage,
) {
    private val appContext = context.applicationContext

    suspend fun downloadAndStage(remote: RemoteDatasetVersion): DatasetInstallResult =
        withContext(Dispatchers.IO) {
            require(remote.downloadUrl.isNotBlank()) { "URL paket pembaruan belum tersedia." }
            require(remote.sizeBytes != null && remote.sizeBytes > 0L) {
                "Ukuran paket pembaruan tidak valid."
            }
            val expectedSize = requireNotNull(remote.sizeBytes)
            require(expectedSize <= MAX_PACKAGE_BYTES) { "Paket pembaruan terlalu besar." }
            require(remote.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
                "Skema dataset ${remote.schemaVersion.ifBlank { "tidak diketahui" }} belum didukung."
            }
            require(isVersionAtLeast(appVersion, remote.minimumAppVersion)) {
                "Versi aplikasi perlu diperbarui sebelum memasang dataset ini."
            }

            val finalName = databaseNameFor(remote)
            val finalFile = appContext.getDatabasePath(finalName)
            check(finalFile.parentFile?.mkdirs() != false) { "Folder database tidak dapat dibuat." }
            val temporaryFile = File(finalFile.parentFile, "$finalName.download")
            temporaryFile.delete()

            try {
                download(remote, temporaryFile)
                verifySqlitePackage(temporaryFile)

                if (finalFile.exists() && !finalFile.delete()) {
                    error("Paket lama dengan versi yang sama tidak dapat diganti.")
                }
                check(temporaryFile.renameTo(finalFile)) {
                    "Paket pembaruan tidak dapat dipindahkan secara atomik."
                }

                val manifest = LocalDatasetManifest(
                    version = remote.version,
                    checksum = remote.checksum,
                    sizeBytes = expectedSize,
                )
                if (!storage.activateOnNextLaunch(finalName, manifest)) {
                    finalFile.delete()
                    error("Aktivasi pembaruan tidak dapat disimpan.")
                }
                DatasetInstallResult(manifest = manifest, restartRequired = true)
            } finally {
                temporaryFile.delete()
            }
        }

    private fun download(remote: RemoteDatasetVersion, destination: File) {
        check(baseUrl.isNotBlank()) { "Alamat backend belum dikonfigurasi." }
        val resolvedUrl = URL(URL(baseUrl.trimEnd('/') + "/"), remote.downloadUrl)
        val connection = resolvedUrl.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.sqlite3")
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val detail = runCatching { JSONObject(body).optString("detail") }.getOrNull()
                error(detail?.takeIf(String::isNotBlank) ?: "Unduhan gagal ($responseCode).")
            }
            val expectedSize = requireNotNull(remote.sizeBytes)
            val contentLength = connection.contentLengthLong
            if (contentLength > 0L && contentLength != expectedSize) {
                error("Ukuran unduhan tidak sesuai metadata server.")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > expectedSize || totalBytes > MAX_PACKAGE_BYTES) {
                            error("Ukuran paket melebihi metadata yang diizinkan.")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            check(totalBytes == expectedSize) { "Unduhan dataset tidak lengkap." }
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            check(checksum.equals(remote.checksum, ignoreCase = true)) {
                "Checksum paket pembaruan tidak sesuai."
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifySqlitePackage(file: File) {
        val database = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
        )
        try {
            database.rawQuery("PRAGMA quick_check", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)) {
                    "Pemeriksaan integritas SQLite gagal."
                }
            }
            val tables = mutableSetOf<String>()
            database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table'",
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
            check(tables.containsAll(REQUIRED_TABLES)) {
                "Paket dataset tidak memiliki seluruh tabel navigasi yang diperlukan."
            }
            REQUIRED_COLUMNS.forEach { (table, expectedColumns) ->
                val columns = mutableSetOf<String>()
                database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
                }
                check(columns.containsAll(expectedColumns)) {
                    "Struktur tabel $table tidak kompatibel dengan aplikasi."
                }
            }
        } finally {
            database.close()
        }
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = "android-v1"
        private const val CONNECT_TIMEOUT_MILLIS = 5_000
        private const val READ_TIMEOUT_MILLIS = 120_000
        private const val MAX_PACKAGE_BYTES = 250L * 1024L * 1024L
        private val REQUIRED_TABLES = setOf(
            "tb_nodes",
            "tb_edges",
            "tb_routes",
            "tb_tes",
            "tb_inundation_zones",
            "tb_safe_zones",
            "tb_tea",
            "tb_tea_routes",
            "tb_tea_next",
        )
        private val REQUIRED_COLUMNS = mapOf(
            "tb_nodes" to setOf("node_id", "lat", "lon", "is_safe"),
            "tb_edges" to setOf("edge_id", "u", "v", "length", "geometry"),
            "tb_routes" to setOf(
                "origin_node_id",
                "rank_1_tes",
                "rank_1_path",
                "rank_1_eta",
                "rank_2_tes",
                "rank_2_path",
                "rank_2_eta",
                "rank_3_tes",
                "rank_3_path",
                "rank_3_eta",
            ),
            "tb_tes" to setOf("tes_id", "nama_tes", "kapasitas", "lat", "lon"),
            "tb_inundation_zones" to setOf("zone_id", "geometry_wkt"),
            "tb_safe_zones" to setOf("safe_zone_id", "geometry_wkt"),
            "tb_tea" to setOf("tea_id", "kapasitas", "lat", "lon"),
            "tb_tea_routes" to setOf("origin_node_id", "nearest_tea_id", "alt_tea_id"),
            "tb_tea_next" to setOf("tea_id", "node_id", "next_node_id"),
        )

        fun databaseNameFor(remote: RemoteDatasetVersion): String =
            "ranah_siaga_${remote.checksum.take(16).lowercase()}.db"

        fun isVersionAtLeast(current: String, minimum: String?): Boolean {
            if (minimum.isNullOrBlank()) return true
            val currentParts = current.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val minimumParts = minimum.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val length = maxOf(currentParts.size, minimumParts.size)
            repeat(length) { index ->
                val currentPart = currentParts.getOrElse(index) { 0 }
                val minimumPart = minimumParts.getOrElse(index) { 0 }
                if (currentPart != minimumPart) return currentPart > minimumPart
            }
            return true
        }
    }
}
