package com.akusukaproject.siagapadang.data.model

data class EvacuationRoute(
    val originNodeId: Long,
    val rank: Int,
    val destinationName: String,
    val estimatedSeconds: Int,
    val coordinates: List<GeoCoordinate>,
    val destinationCoordinate: GeoCoordinate?,
    val destinationCapacityPeople: Int? = null,
    val destinationZoneCode: String? = null,
    val destinationExternalId: String? = null,
    /** "TES" atau "TEA", dibaca dari v_fasilitas_evakuasi; null bila nama tidak dikenali. */
    val destinationKind: String? = null,
    val nodeIds: List<Long> = emptyList(),
    val edgeIds: List<Long> = emptyList(),
    val edgeCoordinateRanges: List<IntRange> = emptyList(),
    val datasetVersion: String = "",
)
