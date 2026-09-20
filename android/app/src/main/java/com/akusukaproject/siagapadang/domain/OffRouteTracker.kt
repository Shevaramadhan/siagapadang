package com.akusukaproject.siagapadang.domain

/**
 * Memutuskan kapan pengguna dianggap benar-benar meninggalkan jalur, sehingga rute perlu dibaca
 * ulang dari simpang terdekat dengan posisi sekarang.
 *
 * Keputusan dipisahkan dari ViewModel agar dapat diuji tanpa emulator, dan agar ambangnya tidak
 * tersebar di beberapa tempat.
 */
class OffRouteTracker(
    private val distanceThresholdMeters: Int = DEFAULT_DISTANCE_THRESHOLD_METERS,
    private val requiredConfirmations: Int = DEFAULT_REQUIRED_CONFIRMATIONS,
    private val maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS,
) {
    private var confirmations = 0

    fun reset() {
        confirmations = 0
    }

    /**
     * Mengembalikan true hanya bila beberapa pembaruan posisi berturut-turut sepakat bahwa
     * pengguna sudah jauh dari jalur. Posisi dengan ketelitian buruk diabaikan — tidak menambah
     * maupun menghapus hitungan — karena lompatan GPS tidak boleh mengganti rute yang sedang
     * diikuti, dan juga tidak boleh membatalkan kesepakatan yang sudah terkumpul.
     */
    fun shouldRecalculate(distanceFromRouteMeters: Int, accuracyMeters: Float): Boolean {
        if (accuracyMeters > maxAccuracyMeters) return false
        if (distanceFromRouteMeters < distanceThresholdMeters) {
            confirmations = 0
            return false
        }
        confirmations += 1
        if (confirmations < requiredConfirmations) return false
        confirmations = 0
        return true
    }

    companion object {
        /** Jarak dari garis rute yang dianggap keluar jalur, bukan sekadar melipir ke tepi jalan. */
        const val DEFAULT_DISTANCE_THRESHOLD_METERS = 150

        const val DEFAULT_REQUIRED_CONFIRMATIONS = 3

        const val DEFAULT_MAX_ACCURACY_METERS = 50f
    }
}
