package com.akusukaproject.siagapadang.data.local

import androidx.room.ColumnInfo

data class NodeRow(
    @ColumnInfo(name = "node_id") val nodeId: Long,
    val lat: Double,
    val lon: Double,
    @ColumnInfo(name = "is_safe") val isSafe: Int,
)

data class RoadSegmentRow(
    @ColumnInfo(name = "from_lat") val fromLatitude: Double,
    @ColumnInfo(name = "from_lon") val fromLongitude: Double,
    @ColumnInfo(name = "to_lat") val toLatitude: Double,
    @ColumnInfo(name = "to_lon") val toLongitude: Double,
)

data class RouteRow(
    @ColumnInfo(name = "origin_node_id") val originNodeId: Long,
    @ColumnInfo(name = "rank_1_tes") val rank1Tes: String,
    @ColumnInfo(name = "rank_1_path") val rank1Path: String,
    @ColumnInfo(name = "rank_1_eta") val rank1Eta: Double,
    @ColumnInfo(name = "rank_2_tes") val rank2Tes: String,
    @ColumnInfo(name = "rank_2_path") val rank2Path: String,
    @ColumnInfo(name = "rank_2_eta") val rank2Eta: Double,
    @ColumnInfo(name = "rank_3_tes") val rank3Tes: String,
    @ColumnInfo(name = "rank_3_path") val rank3Path: String,
    @ColumnInfo(name = "rank_3_eta") val rank3Eta: Double,
)

data class EdgeRow(
    @ColumnInfo(name = "edge_id") val edgeId: Long,
    val u: Long,
    val v: Long,
    val length: Double,
    val geometry: String,
)

data class TesRow(
    @ColumnInfo(name = "tes_id") val tesId: String,
    @ColumnInfo(name = "nama_tes") val name: String,
    val zona: String,
    val kapasitas: Double,
    val lat: Double,
    val lon: Double,
)

data class FacilityRow(
    @ColumnInfo(name = "facility_id") val facilityId: String,
    val nama: String,
    val jenis: String,
    @ColumnInfo(name = "zona_sektor") val zonaSektor: String?,
    val kapasitas: Double?,
    val lat: Double,
    val lon: Double,
)

data class InundationZoneRow(
    @ColumnInfo(name = "zone_id") val zoneId: Long,
    @ColumnInfo(name = "nama_zona") val name: String,
    @ColumnInfo(name = "tingkat_bahaya") val dangerLevel: String,
    @ColumnInfo(name = "geometry_wkt") val geometryWkt: String,
)

data class ZoneGeometryRow(
    val name: String,
    val level: String,
    @ColumnInfo(name = "geometry_wkt") val geometryWkt: String,
)

data class TeaRow(
    @ColumnInfo(name = "tea_id") val teaId: String,
    val kapasitas: Double,
    val lat: Double,
    val lon: Double,
)

data class TeaRouteRow(
    @ColumnInfo(name = "origin_node_id") val originNodeId: Long,
    @ColumnInfo(name = "nearest_tea_id") val nearestTeaId: String,
    @ColumnInfo(name = "alt_tea_id") val altTeaId: String,
)

data class TeaNextRow(
    @ColumnInfo(name = "tea_id") val teaId: String,
    @ColumnInfo(name = "node_id") val nodeId: Long,
    @ColumnInfo(name = "next_node_id") val nextNodeId: Long,
)

data class TeaPathStep(
    val nodeId: Long,
    val nextNodeId: Long,
)
