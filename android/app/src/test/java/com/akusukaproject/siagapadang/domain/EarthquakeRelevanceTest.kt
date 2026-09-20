package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EarthquakeRelevanceTest {
    private val mentawai = GeoCoordinate(latitude = -1.95, longitude = 99.35)
    private val ruteng = GeoCoordinate(latitude = -8.05, longitude = 120.86)

    @Test
    fun `gempa Mentawai memicu peringatan layar penuh`() {
        assertTrue(EarthquakeRelevance.shouldShowFullScreenAlert(mentawai, reference = null))
    }

    @Test
    fun `gempa Nusa Tenggara tidak memicu peringatan layar penuh`() {
        assertFalse(EarthquakeRelevance.shouldShowFullScreenAlert(ruteng, reference = null))
    }

    @Test
    fun `koordinat tidak diketahui tetap memicu peringatan`() {
        assertTrue(EarthquakeRelevance.shouldShowFullScreenAlert(epicenter = null, reference = null))
    }

    @Test
    fun `jarak dihitung dari posisi pengguna bila tersedia`() {
        val user = GeoCoordinate(latitude = -1.90, longitude = 99.40)
        val fromUser = EarthquakeRelevance.distanceKm(mentawai, user)
        val fromCity = EarthquakeRelevance.distanceKm(mentawai, reference = null)
        requireNotNull(fromUser)
        requireNotNull(fromCity)
        assertTrue("jarak dari pengguna harus jauh lebih dekat", fromUser < fromCity)
    }

    @Test
    fun `label memakai acuan yang sesuai dan pemisah ribuan`() {
        assertEquals("±38 km dari posisi Anda", EarthquakeRelevance.distanceLabel(38.4, usingUserLocation = true))
        assertEquals("±1.900 km dari Padang", EarthquakeRelevance.distanceLabel(1_896.0, usingUserLocation = false))
        assertNull(EarthquakeRelevance.distanceLabel(null, usingUserLocation = false))
    }
}
