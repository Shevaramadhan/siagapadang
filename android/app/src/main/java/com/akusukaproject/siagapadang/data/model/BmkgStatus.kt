package com.akusukaproject.siagapadang.data.model

/**
 * Informasi gempa regional terdekat dari Padang (radius 1.500 km),
 * diambil dari daftar gempa terkini BMKG dan disaring oleh backend.
 */
data class RegionalEarthquake(
    val eventDate: String,
    val eventTime: String,
    val magnitude: String,
    val depth: String,
    val region: String,
    val potential: String,
    val distanceKmFromPadang: Double,
    val epicenter: GeoCoordinate? = null,
)

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
    /** Gempa regional terdekat dari Padang (radius 1.500 km); null jika tidak ada. */
    val regionalEvent: RegionalEarthquake? = null,
)
