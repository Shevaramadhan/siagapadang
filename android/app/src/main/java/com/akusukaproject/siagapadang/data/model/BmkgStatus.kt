package com.akusukaproject.siagapadang.data.model

data class BmkgStatus(
    val eventDate: String,
    val eventTime: String,
    val magnitude: String,
    val depth: String,
    val region: String,
    val potential: String,
    val felt: String,
    val hasTsunamiPotential: Boolean,
    val isStale: Boolean,
    val fetchedAt: String,
    val source: String,
    /** Titik pusat gempa; null bila BMKG tidak menyertakan koordinat yang terbaca. */
    val epicenter: GeoCoordinate? = null,
)
