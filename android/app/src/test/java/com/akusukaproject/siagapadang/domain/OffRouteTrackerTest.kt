package com.akusukaproject.siagapadang.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OffRouteTrackerTest {
    private val goodAccuracy = 12f

    @Test
    fun `tetap di jalur tidak pernah memicu pengalihan`() {
        val tracker = OffRouteTracker()
        repeat(10) {
            assertFalse(tracker.shouldRecalculate(distanceFromRouteMeters = 20, accuracyMeters = goodAccuracy))
        }
    }

    @Test
    fun `satu lompatan GPS tidak cukup untuk mengganti rute`() {
        val tracker = OffRouteTracker()
        assertFalse(tracker.shouldRecalculate(distanceFromRouteMeters = 400, accuracyMeters = goodAccuracy))
        assertFalse(tracker.shouldRecalculate(distanceFromRouteMeters = 15, accuracyMeters = goodAccuracy))
        assertFalse(tracker.shouldRecalculate(distanceFromRouteMeters = 400, accuracyMeters = goodAccuracy))
    }

    @Test
    fun `tiga pembaruan berturut-turut yang jauh memicu pengalihan`() {
        val tracker = OffRouteTracker()
        assertFalse(tracker.shouldRecalculate(distanceFromRouteMeters = 300, accuracyMeters = goodAccuracy))
        assertFalse(tracker.shouldRecalculate(distanceFromRouteMeters = 310, accuracyMeters = goodAccuracy))
        assertTrue(tracker.shouldRecalculate(distanceFromRouteMeters = 320, accuracyMeters = goodAccuracy))
    }

    @Test
    fun `hitungan dimulai lagi setelah pengalihan terjadi`() {
        val tracker = OffRouteTracker()
        repeat(2) { tracker.shouldRecalculate(300, goodAccuracy) }
        assertTrue(tracker.shouldRecalculate(300, goodAccuracy))
        assertFalse(tracker.shouldRecalculate(300, goodAccuracy))
    }

    @Test
    fun `posisi dengan ketelitian buruk diabaikan tanpa menghapus kesepakatan`() {
        val tracker = OffRouteTracker()
        assertFalse(tracker.shouldRecalculate(300, goodAccuracy))
        assertFalse(tracker.shouldRecalculate(300, accuracyMeters = 90f))
        assertFalse(tracker.shouldRecalculate(300, goodAccuracy))
        assertTrue(tracker.shouldRecalculate(300, goodAccuracy))
    }

    @Test
    fun `kembali ke jalur menghapus kesepakatan yang sudah terkumpul`() {
        val tracker = OffRouteTracker()
        repeat(2) { tracker.shouldRecalculate(300, goodAccuracy) }
        assertFalse(tracker.shouldRecalculate(10, goodAccuracy))
        assertFalse(tracker.shouldRecalculate(300, goodAccuracy))
        assertFalse(tracker.shouldRecalculate(300, goodAccuracy))
        assertTrue(tracker.shouldRecalculate(300, goodAccuracy))
    }
}
