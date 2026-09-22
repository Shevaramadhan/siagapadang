package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute

/** Menjaga daftar tujuan lain ketika pengguna berpindah maju atau kembali ke rute sebelumnya. */
object RouteHistory {
    fun availableAfterSelection(
        currentRoute: EvacuationRoute,
        selectedRoute: EvacuationRoute,
        availableRoutes: List<EvacuationRoute>,
    ): List<EvacuationRoute> = (
        listOf(currentRoute) + availableRoutes.filterNot {
            it.destinationName == selectedRoute.destinationName
        }
        ).distinctBy { it.destinationName }
}
