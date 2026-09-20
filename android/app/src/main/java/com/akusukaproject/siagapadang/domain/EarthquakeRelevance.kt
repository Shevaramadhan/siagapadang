package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.roundToInt

/**
 * BMKG menerbitkan satu gempa terbaru untuk seluruh Indonesia, jadi berita yang masuk bisa saja
 * berasal dari Maluku. Objek ini menghitung jaraknya agar pengguna di Padang tahu seberapa dekat
 * kejadiannya, dan menahan layar peringatan penuh untuk gempa yang terlalu jauh.
 *
 * Objek ini tidak menilai besaran gempa maupun ada tidaknya potensi tsunami — keduanya kewenangan
 * BMKG dan dipakai apa adanya.
 */
object EarthquakeRelevance {
    /** Titik acuan kota ketika posisi pengguna belum diperoleh. */
    val PADANG_CENTER = GeoCoordinate(latitude = -0.9471, longitude = 100.4172)

    /**
     * Batas jarak layar peringatan tsunami. Dipilih longgar agar seluruh zona subduksi yang dapat
     * mengirim gelombang ke pantai barat Sumatra tetap tercakup — Mentawai (±150 km), Nias
     * (±450 km), Bengkulu (±500 km), Aceh (±1.100 km). Gempa di luar radius ini tetap ditampilkan
     * pada kartu status, hanya tidak memicu layar penuh.
     */
    const val ALERT_RADIUS_KM = 1_500.0

    fun distanceKm(epicenter: GeoCoordinate?, reference: GeoCoordinate?): Double? {
        if (epicenter == null) return null
        val from = reference ?: PADANG_CENTER
        return NearestNodeFinder.distanceMeters(from, epicenter) / 1_000.0
    }

    /**
     * Peringatan layar penuh ditahan hanya bila jaraknya benar-benar diketahui dan melampaui
     * radius. Koordinat yang tidak terbaca berarti peringatan tetap ditampilkan.
     */
    fun shouldShowFullScreenAlert(epicenter: GeoCoordinate?, reference: GeoCoordinate?): Boolean {
        val distance = distanceKm(epicenter, reference) ?: return true
        return distance <= ALERT_RADIUS_KM
    }

    /** Contoh keluaran: "±38 km dari posisi Anda", "±1.900 km dari Padang". */
    fun distanceLabel(distanceKm: Double?, usingUserLocation: Boolean): String? {
        if (distanceKm == null) return null
        val rounded = if (distanceKm < 100) {
            distanceKm.roundToInt()
        } else {
            (distanceKm / 10).roundToInt() * 10
        }
        val reference = if (usingUserLocation) "posisi Anda" else "Padang"
        return "±${formatThousands(rounded)} km dari $reference"
    }

    private fun formatThousands(value: Int): String {
        val digits = value.toString()
        return digits.reversed().chunked(3).joinToString(".").reversed()
    }
}
