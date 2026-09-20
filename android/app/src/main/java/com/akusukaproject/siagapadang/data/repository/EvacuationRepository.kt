package com.akusukaproject.siagapadang.data.repository

import com.akusukaproject.siagapadang.data.local.EvacuationDao
import com.akusukaproject.siagapadang.data.local.RouteRow
import com.akusukaproject.siagapadang.data.model.EvacuationPoint
import com.akusukaproject.siagapadang.data.model.Facility
import com.akusukaproject.siagapadang.data.model.FacilityKind
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.EvacuationSummary
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.OfflineRoadOverlay
import com.akusukaproject.siagapadang.domain.AlternativeRouteSelector
import com.akusukaproject.siagapadang.domain.DetourSelector
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.domain.PolylineAssembler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point
import kotlin.math.roundToInt

import com.akusukaproject.siagapadang.domain.ActiveEdgeFinder

class EvacuationRepository(
    private val dao: EvacuationDao,
    private val datasetVersion: String = DEFAULT_DATASET_VERSION,
) {
    @Volatile
    private var cachedOfflineRoadOverlay: OfflineRoadOverlay? = null

    suspend fun loadOfflineRoadOverlay(center: GeoCoordinate): OfflineRoadOverlay {
        val latitudeCell = (center.latitude / ROAD_VIEWPORT_GRID_DEGREES).roundToInt()
        val longitudeCell = (center.longitude / ROAD_VIEWPORT_GRID_DEGREES).roundToInt()
        val viewportId = "$latitudeCell:$longitudeCell"
        cachedOfflineRoadOverlay?.takeIf { overlay -> overlay.viewportId == viewportId }?.let {
            return it
        }
        val queryLatitude = latitudeCell * ROAD_VIEWPORT_GRID_DEGREES
        val queryLongitude = longitudeCell * ROAD_VIEWPORT_GRID_DEGREES
        val segments = dao.findRoadSegmentsInBounds(
            minLat = queryLatitude - ROAD_VIEWPORT_HALF_SPAN_DEGREES,
            maxLat = queryLatitude + ROAD_VIEWPORT_HALF_SPAN_DEGREES,
            minLon = queryLongitude - ROAD_VIEWPORT_HALF_SPAN_DEGREES,
            maxLon = queryLongitude + ROAD_VIEWPORT_HALF_SPAN_DEGREES,
        )
        return withContext(Dispatchers.Default) {
            val lineCoordinates = segments.map { segment ->
                listOf(
                    Point.fromLngLat(segment.fromLongitude, segment.fromLatitude),
                    Point.fromLngLat(segment.toLongitude, segment.toLatitude),
                )
            }
            val feature = Feature.fromGeometry(MultiLineString.fromLngLats(lineCoordinates))
            OfflineRoadOverlay(
                viewportId = viewportId,
                geoJson = FeatureCollection.fromFeature(feature).toJson(),
                segmentCount = lineCoordinates.size,
            )
        }.also { overlay -> cachedOfflineRoadOverlay = overlay }
    }

    suspend fun findSummaryFromLocation(location: GeoCoordinate): EvacuationSummary {
        val nearestNode = findNearestNode(location)
        val routeRow = dao.findRoute(nearestNode.nodeId)
            ?: throw IllegalStateException("Rute evakuasi tidak tersedia untuk lokasi ini")
        val selection = routeRow.select(rank = 1)
        return EvacuationSummary(
            destinationName = selection.destinationName,
            estimatedSeconds = (selection.etaMinutes * 60.0).roundToInt(),
            estimatedDistanceMeters = (selection.etaMinutes * 60.0 * WALKING_SPEED_METERS_PER_SECOND)
                .roundToInt(),
        )
    }

    suspend fun loadEvacuationPoints(): List<EvacuationPoint> =
        dao.findAllTes().map { tes ->
            EvacuationPoint(
                externalId = tes.tesId,
                name = tes.name,
                zoneCode = tes.zona,
                capacityPeople = tes.kapasitas.roundToInt(),
                coordinate = GeoCoordinate(latitude = tes.lat, longitude = tes.lon),
            )
        }

    suspend fun loadFacilities(): List<Facility> =
        dao.findAllFacilities().mapNotNull { row ->
            val kind = FacilityKind.entries.firstOrNull { it.name == row.jenis } ?: return@mapNotNull null
            Facility(
                id = row.facilityId,
                name = row.nama,
                kind = kind,
                zone = row.zonaSektor.orEmpty(),
                capacityPeople = row.kapasitas?.roundToInt() ?: 0,
                coordinate = GeoCoordinate(latitude = row.lat, longitude = row.lon),
            )
        }

    suspend fun findRouteFromLocation(
        location: GeoCoordinate,
        rank: Int = 1,
    ): EvacuationRoute {
        val nearestNode = findNearestNode(location)
        val tesRoute = loadRoute(originNodeId = nearestNode.nodeId, rank = rank)
        
        val teaRouteRow = dao.findTeaRoute(nearestNode.nodeId)
        if (teaRouteRow != null) {
            val teaId = if (rank == 2) teaRouteRow.altTeaId else teaRouteRow.nearestTeaId
            if (teaId.isNotBlank()) {
                val teaRoute = runCatching { 
                    loadTeaRoute(nearestNode.nodeId, teaId, isAlternative = (rank == 2)) 
                }.getOrNull()
                
                if (teaRoute != null && teaRoute.estimatedSeconds <= tesRoute.estimatedSeconds) {
                    return teaRoute
                }
            }
        }
        
        return tesRoute
    }

    suspend fun loadRoute(originNodeId: Long, rank: Int): EvacuationRoute {
        require(rank in 1..3) { "Peringkat rute harus 1, 2, atau 3" }
        val routeRow = dao.findRoute(originNodeId)
            ?: throw IllegalStateException("Rute evakuasi tidak tersedia untuk lokasi ini")
        val selection = routeRow.select(rank)
        return buildRoute(
            originNodeId = originNodeId,
            rank = rank,
            destinationName = selection.destinationName,
            estimatedSeconds = (selection.etaMinutes * 60.0).roundToInt(),
            pathNodeIds = parsePath(selection.path),
        )
    }

    suspend fun loadTeaRoute(originNodeId: Long, teaId: String, isAlternative: Boolean = false): EvacuationRoute {
        val steps = dao.findTeaPathSteps(teaId, originNodeId)
        if (steps.isEmpty()) throw IllegalStateException("Rute ke TEA tidak tersedia untuk lokasi ini")
        val pathNodeIds = steps.map { it.nodeId } + steps.last().nextNodeId
        return buildTeaRoute(
            originNodeId = originNodeId,
            teaId = teaId,
            pathNodeIds = pathNodeIds,
            rank = if (isAlternative) 2 else 1,
        )
    }

    private suspend fun buildTeaRoute(
        originNodeId: Long,
        teaId: String,
        pathNodeIds: List<Long>,
        rank: Int,
    ): EvacuationRoute {
        val distinctNodeIds = pathNodeIds.distinct()
        val edges = dao.findEdgesForNodes(distinctNodeIds)
        val nodeCoordinates = dao.findNodesByIds(distinctNodeIds).associate { node ->
            node.nodeId to GeoCoordinate(latitude = node.lat, longitude = node.lon)
        }
        val assembled = withContext(Dispatchers.Default) {
            PolylineAssembler.assembleWithEdges(pathNodeIds, edges, nodeCoordinates)
        }
        val destination = dao.findTeaById(teaId)
        
        // Calculate ETA manually by summing edge lengths in meters / speed
        var totalLength = 0.0
        val pathEdgeIds = assembled.edgeIds
        for (edge in edges) {
            if (edge.edgeId in pathEdgeIds) {
                totalLength += edge.length
            }
        }
        val estimatedSeconds = (totalLength / WALKING_SPEED_METERS_PER_SECOND).roundToInt()

        return EvacuationRoute(
            originNodeId = originNodeId,
            rank = rank,
            destinationName = teaId,
            estimatedSeconds = estimatedSeconds,
            coordinates = assembled.coordinates,
            destinationCoordinate = destination?.let { tea ->
                GeoCoordinate(latitude = tea.lat, longitude = tea.lon)
            },
            destinationCapacityPeople = destination?.kapasitas?.roundToInt(),
            destinationZoneCode = "Perbukitan (TEA)",
            destinationExternalId = teaId,
            nodeIds = pathNodeIds,
            edgeIds = assembled.edgeIds,
            edgeCoordinateRanges = assembled.edgeCoordinateRanges,
            datasetVersion = datasetVersion,
        )
    }

    /**
     * Jalan memutar ke TES yang sama lewat simpang tetangga dari simpang sebelum ruas terhalang.
     * Hanya membaca rute prakomputasi milik simpang tetangga; mengembalikan null bila tidak ada.
     */
    suspend fun findDetourToSameDestination(
        location: GeoCoordinate,
        currentRoute: EvacuationRoute,
        blockedEdgeId: Long?,
        blockedEdgeIds: Set<Long>,
    ): EvacuationRoute? {
        val blockedIndex = blockedEdgeId?.let(currentRoute.edgeIds::indexOf) ?: -1
        val startNodeId = currentRoute.nodeIds.getOrNull(blockedIndex)
            ?: findNearestNode(location).nodeId
        val candidates = mutableListOf<EvacuationRoute>()
        for (connector in dao.findEdgesTouchingNode(startNodeId)) {
            if (connector.edgeId in blockedEdgeIds) continue
            val neighborId = if (connector.u == startNodeId) connector.v else connector.u
            val row = dao.findRoute(neighborId) ?: continue
            for (rank in 1..3) {
                val selection = row.select(rank)
                if (selection.destinationName != currentRoute.destinationName) continue
                val neighborPath = runCatching { parsePath(selection.path) }.getOrNull() ?: continue
                val walkSeconds = connector.length / WALKING_SPEED_METERS_PER_SECOND
                candidates += runCatching {
                    buildRoute(
                        originNodeId = startNodeId,
                        rank = currentRoute.rank,
                        destinationName = selection.destinationName,
                        estimatedSeconds = (walkSeconds + selection.etaMinutes * 60.0).roundToInt(),
                        pathNodeIds = listOf(startNodeId) + neighborPath,
                    )
                }.getOrNull() ?: continue
            }
        }
        return DetourSelector.select(
            startNodeId = startNodeId,
            destinationName = currentRoute.destinationName,
            candidates = candidates,
            blockedEdgeIds = blockedEdgeIds,
        )
    }

    private fun parsePath(path: String): List<Long> = path
        .split(',')
        .map { value ->
            value.trim().toLongOrNull()
                ?: throw IllegalStateException("Data node pada rute tidak valid")
        }
        .also { require(it.size >= 2) { "Data rute terlalu pendek" } }

    private suspend fun buildRoute(
        originNodeId: Long,
        rank: Int,
        destinationName: String,
        estimatedSeconds: Int,
        pathNodeIds: List<Long>,
    ): EvacuationRoute {

        val distinctNodeIds = pathNodeIds.distinct()
        val edges = dao.findEdgesForNodes(distinctNodeIds)
        val nodeCoordinates = dao.findNodesByIds(distinctNodeIds).associate { node ->
            node.nodeId to GeoCoordinate(latitude = node.lat, longitude = node.lon)
        }
        val assembled = withContext(Dispatchers.Default) {
            PolylineAssembler.assembleWithEdges(pathNodeIds, edges, nodeCoordinates)
        }
        val destination = dao.findTesByName(destinationName)
        val destinationKind = runCatching { dao.findFacilityKind(destinationName) }
            .getOrNull()
            ?: destination?.let { "TES" }

        return EvacuationRoute(
            originNodeId = originNodeId,
            rank = rank,
            destinationName = destinationName,
            estimatedSeconds = estimatedSeconds,
            coordinates = assembled.coordinates,
            destinationCoordinate = destination?.let { tes ->
                GeoCoordinate(latitude = tes.lat, longitude = tes.lon)
            },
            destinationCapacityPeople = destination?.kapasitas?.roundToInt(),
            destinationZoneCode = destination?.zona,
            destinationExternalId = destination?.tesId,
            destinationKind = destinationKind,
            nodeIds = pathNodeIds,
            edgeIds = assembled.edgeIds,
            edgeCoordinateRanges = assembled.edgeCoordinateRanges,
            datasetVersion = datasetVersion,
        )
    }

    fun findActiveEdgeId(
        location: GeoCoordinate,
        route: EvacuationRoute,
        nearestCoordinateIndex: Int? = null,
    ): Long? = ActiveEdgeFinder.findActiveEdgeId(
        location = location,
        route = route,
        nearestRouteCoordinateIndex = nearestCoordinateIndex,
    )

    suspend fun findAlternativeRoute(
        location: GeoCoordinate,
        currentRoute: EvacuationRoute,
        excludedDestinationNames: Set<String>,
        excludedEdgeIds: Set<Long> = emptySet(),
    ): EvacuationRoute {
        val nearestNode = findNearestNode(location)
        val candidates = (1..3).mapNotNull { rank ->
            runCatching { loadRoute(nearestNode.nodeId, rank) }.getOrNull()
        }
        return AlternativeRouteSelector.select(
            currentLocation = location,
            currentRoute = currentRoute,
            candidates = candidates,
            excludedDestinationNames = excludedDestinationNames,
            excludedEdgeIds = excludedEdgeIds,
        ) ?: throw IllegalStateException("Rute lain yang menghindari jalur ini tidak tersedia")
    }

    private suspend fun findNearestNode(location: GeoCoordinate) =
        SEARCH_WINDOWS.firstNotNullOfOrNull { window ->
            val candidates = dao.findNodesInBounds(
                minLat = location.latitude - window,
                maxLat = location.latitude + window,
                minLon = location.longitude - window,
                maxLon = location.longitude + window,
            )
            NearestNodeFinder.findNearest(location, candidates)
        } ?: throw IllegalStateException("Posisi berada di luar cakupan jaringan evakuasi")

    private fun RouteRow.select(rank: Int): RouteSelection = when (rank) {
        1 -> RouteSelection(rank1Tes, rank1Path, rank1Eta)
        2 -> RouteSelection(rank2Tes, rank2Path, rank2Eta)
        3 -> RouteSelection(rank3Tes, rank3Path, rank3Eta)
        else -> error("Peringkat rute tidak didukung")
    }

    private data class RouteSelection(
        val destinationName: String,
        val path: String,
        val etaMinutes: Double,
    )

    companion object {
        const val DEFAULT_DATASET_VERSION = "2026.09.19"
        val SEARCH_WINDOWS = listOf(0.005, 0.02)
        const val WALKING_SPEED_METERS_PER_SECOND = 1.2
        const val ROAD_VIEWPORT_GRID_DEGREES = 0.006
        const val ROAD_VIEWPORT_HALF_SPAN_DEGREES = 0.016
    }
}
