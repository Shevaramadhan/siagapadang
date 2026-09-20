package com.akusukaproject.siagapadang.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

@Dao
interface EvacuationDao {
    @SkipQueryVerification
    @Query(
        """
        SELECT node_id, lat, lon, is_safe
        FROM tb_nodes
        WHERE lat BETWEEN :minLat AND :maxLat
          AND lon BETWEEN :minLon AND :maxLon
        """,
    )
    suspend fun findNodesInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<NodeRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT node_id, lat, lon, is_safe
        FROM tb_nodes
        WHERE node_id IN (:nodeIds)
        """,
    )
    suspend fun findNodesByIds(nodeIds: List<Long>): List<NodeRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT DISTINCT
               origin.lat AS from_lat,
               origin.lon AS from_lon,
               destination.lat AS to_lat,
               destination.lon AS to_lon
        FROM tb_edges AS edge
        INNER JOIN tb_nodes AS origin ON origin.node_id = edge.u
        INNER JOIN tb_nodes AS destination ON destination.node_id = edge.v
        WHERE (origin.lat BETWEEN :minLat AND :maxLat
               AND origin.lon BETWEEN :minLon AND :maxLon)
           OR (destination.lat BETWEEN :minLat AND :maxLat
               AND destination.lon BETWEEN :minLon AND :maxLon)
        """,
    )
    suspend fun findRoadSegmentsInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<RoadSegmentRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT origin_node_id,
               rank_1_tes, rank_1_path, rank_1_eta,
               rank_2_tes, rank_2_path, rank_2_eta,
               rank_3_tes, rank_3_path, rank_3_eta
        FROM tb_routes
        WHERE origin_node_id = :originNodeId
        LIMIT 1
        """,
    )
    suspend fun findRoute(originNodeId: Long): RouteRow?

    @SkipQueryVerification
    @Query(
        """
        SELECT edge_id, u, v, length, geometry
        FROM tb_edges
        WHERE u IN (:nodeIds) OR v IN (:nodeIds)
        ORDER BY length ASC
        """,
    )
    suspend fun findEdgesForNodes(nodeIds: List<Long>): List<EdgeRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT tes_id, nama_tes, zona, kapasitas, lat, lon
        FROM tb_tes
        WHERE nama_tes = :name
        LIMIT 1
        """,
    )
    suspend fun findTesByName(name: String): TesRow?

    @SkipQueryVerification
    @Query(
        """
        SELECT tes_id, nama_tes, zona, kapasitas, lat, lon
        FROM tb_tes
        ORDER BY nama_tes ASC
        """,
    )
    suspend fun findAllTes(): List<TesRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT facility_id, nama, jenis, zona_sektor, kapasitas, lat, lon
        FROM v_fasilitas_evakuasi
        """,
    )
    suspend fun findAllFacilities(): List<FacilityRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT jenis FROM v_fasilitas_evakuasi
        WHERE nama = :name
        LIMIT 1
        """,
    )
    suspend fun findFacilityKind(name: String): String?

    // tb_edges hanya menyimpan satu arah, jadi tetangga dicari dari kedua ujung ruas.
    @SkipQueryVerification
    @Query(
        """
        SELECT edge_id, u, v, length, geometry
        FROM tb_edges
        WHERE u = :nodeId OR v = :nodeId
        """,
    )
    suspend fun findEdgesTouchingNode(nodeId: Long): List<EdgeRow>
}
