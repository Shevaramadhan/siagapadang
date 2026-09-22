package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteHistoryTest {
    private fun route(name: String) = EvacuationRoute(
        originNodeId = 1,
        rank = 1,
        destinationName = name,
        estimatedSeconds = 300,
        coordinates = emptyList(),
        destinationCoordinate = null,
    )

    @Test
    fun `rute yang dipilih keluar dari daftar dan rute aktif masuk sebagai pilihan`() {
        val first = route("TES Pertama")
        val second = route("TES Kedua")
        val third = route("TES Ketiga")

        assertEquals(
            listOf("TES Pertama", "TES Ketiga"),
            RouteHistory.availableAfterSelection(
                currentRoute = first,
                selectedRoute = second,
                availableRoutes = listOf(second, third),
            ).map { it.destinationName },
        )
    }

    @Test
    fun `tujuan yang sama tidak diduplikasi`() {
        val current = route("TES Aktif")
        val selected = route("TES Lama")

        assertEquals(
            listOf("TES Aktif"),
            RouteHistory.availableAfterSelection(
                currentRoute = current,
                selectedRoute = selected,
                availableRoutes = listOf(selected, current),
            ).map { it.destinationName },
        )
    }
}
