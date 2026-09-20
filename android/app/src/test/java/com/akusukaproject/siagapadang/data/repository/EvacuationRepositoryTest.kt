package com.akusukaproject.siagapadang.data.repository

import com.akusukaproject.siagapadang.data.local.EdgeRow
import com.akusukaproject.siagapadang.data.local.EvacuationDao
import com.akusukaproject.siagapadang.data.local.NodeRow
import com.akusukaproject.siagapadang.data.local.RoadSegmentRow
import com.akusukaproject.siagapadang.data.local.RouteRow
import com.akusukaproject.siagapadang.data.local.TesRow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EvacuationRepositoryTest {

    private class FakeEvacuationDao : EvacuationDao {
        override suspend fun findNodesInBounds(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double,
        ): List<NodeRow> = emptyList()

        override suspend fun findNodesByIds(nodeIds: List<Long>): List<NodeRow> = listOf(
            NodeRow(nodeId = 1L, lat = -0.95, lon = 100.35, isSafe = 0),
            NodeRow(nodeId = 2L, lat = -0.94, lon = 100.36, isSafe = 0),
            NodeRow(nodeId = 3L, lat = -0.93, lon = 100.37, isSafe = 1),
            NodeRow(nodeId = 4L, lat = -0.92, lon = 100.38, isSafe = 1),
        )

        override suspend fun findRoadSegmentsInBounds(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double,
        ): List<RoadSegmentRow> = emptyList()

        override suspend fun findRoute(originNodeId: Long): RouteRow? {
            if (originNodeId != 1L) return null
            return RouteRow(
                originNodeId = 1L,
                rank1Tes = "TES Masjid Raya",
                rank1Path = "1,2,3",
                rank1Eta = 10.0,
                rank2Tes = "TES Kantor Gubernur",
                rank2Path = "1,2,4",
                rank2Eta = 15.0,
                rank3Tes = "TES Lapangan Imam Bonjol",
                rank3Path = "1,3,4",
                rank3Eta = 20.0,
            )
        }

        override suspend fun findEdgesForNodes(nodeIds: List<Long>): List<EdgeRow> = listOf(
            EdgeRow(edgeId = 101L, u = 1L, v = 2L, length = 100.0, geometry = "LINESTRING (100.35 -0.95, 100.36 -0.94)"),
            EdgeRow(edgeId = 102L, u = 2L, v = 3L, length = 150.0, geometry = "LINESTRING (100.36 -0.94, 100.37 -0.93)"),
            EdgeRow(edgeId = 103L, u = 2L, v = 4L, length = 200.0, geometry = "LINESTRING (100.36 -0.94, 100.38 -0.92)"),
            EdgeRow(edgeId = 104L, u = 1L, v = 3L, length = 300.0, geometry = "LINESTRING (100.35 -0.95, 100.37 -0.93)"),
            EdgeRow(edgeId = 105L, u = 3L, v = 4L, length = 120.0, geometry = "LINESTRING (100.37 -0.93, 100.38 -0.92)"),
        )

        override suspend fun findAllTes(): List<TesRow> = emptyList()

        override suspend fun findEdgesTouchingNode(nodeId: Long): List<EdgeRow> = emptyList()

        override suspend fun findAllFacilities(): List<com.akusukaproject.siagapadang.data.local.FacilityRow> = emptyList()

        override suspend fun findFacilityKind(name: String): String? = null

        // Rute TEA belum dipakai pada uji ini; cukup dijawab kosong.
        override suspend fun findTeaRoute(originNodeId: Long): com.akusukaproject.siagapadang.data.local.TeaRouteRow? = null

        override suspend fun findTeaById(teaId: String): com.akusukaproject.siagapadang.data.local.TeaRow? = null

        override suspend fun findTeaPathSteps(
            teaId: String,
            originNodeId: Long,
        ): List<com.akusukaproject.siagapadang.data.local.TeaPathStep> = emptyList()

        override suspend fun findTesByName(name: String): TesRow? = when (name) {
            "TES Masjid Raya" -> TesRow(
                tesId = "TES_01",
                name = "TES Masjid Raya",
                zona = "ZONA_A",
                kapasitas = 2000.0,
                lat = -0.93,
                lon = 100.37,
            )
            "TES Kantor Gubernur" -> TesRow(
                tesId = "TES_02",
                name = "TES Kantor Gubernur",
                zona = "ZONA_B",
                kapasitas = 1500.0,
                lat = -0.92,
                lon = 100.38,
            )
            else -> null
        }
    }

    @Test
    fun `loadRoute menghasilkan destinationExternalId, edgeIds, dan datasetVersion yang sesuai`() = runBlocking {
        val fakeDao = FakeEvacuationDao()
        val repository = EvacuationRepository(fakeDao, datasetVersion = "2026.09.13")

        val routeRank1 = repository.loadRoute(originNodeId = 1L, rank = 1)

        assertEquals("TES_01", routeRank1.destinationExternalId)
        assertEquals("TES Masjid Raya", routeRank1.destinationName)
        assertEquals(listOf(101L, 102L), routeRank1.edgeIds)
        assertEquals("2026.09.13", routeRank1.datasetVersion)
        assertEquals(2, routeRank1.edgeCoordinateRanges.size)
    }

    @Test
    fun `pergantian rank rute tidak mencampurkan ID tujuan dan ID ruas sebelumnya`() = runBlocking {
        val fakeDao = FakeEvacuationDao()
        val repository = EvacuationRepository(fakeDao, datasetVersion = "2026.09.13")

        val routeRank1 = repository.loadRoute(originNodeId = 1L, rank = 1)
        val routeRank2 = repository.loadRoute(originNodeId = 1L, rank = 2)

        // Verifikasi rute rank 1
        assertEquals("TES_01", routeRank1.destinationExternalId)
        assertEquals(listOf(101L, 102L), routeRank1.edgeIds)

        // Verifikasi rute rank 2 memiliki ID terisolasi
        assertEquals("TES_02", routeRank2.destinationExternalId)
        assertEquals("TES Kantor Gubernur", routeRank2.destinationName)
        assertEquals(listOf(101L, 103L), routeRank2.edgeIds)

        // Pastikan tidak ada kebocoran ID antar rank
        assertEquals(2, routeRank2.edgeIds.size)
        assertEquals(103L, routeRank2.edgeIds[1])
    }
}
