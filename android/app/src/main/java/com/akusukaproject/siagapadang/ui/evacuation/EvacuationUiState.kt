package com.akusukaproject.siagapadang.ui.evacuation

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.BmkgStatus
import com.akusukaproject.siagapadang.data.model.DatasetUpdateStatus
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
import com.akusukaproject.siagapadang.data.model.LocalDatasetManifest
import com.akusukaproject.siagapadang.data.model.OfflineRoadOverlay
import com.akusukaproject.siagapadang.data.model.TsunamiZoneOverlay
import com.akusukaproject.siagapadang.data.remote.model.OccupancyStatusResponseDto
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot
import com.akusukaproject.siagapadang.domain.DirectOrientation

/** Jenis kendala yang dipilih pengguna dari tombol "Ada kendala?". */
enum class EvacuationObstacleType {
    ROAD_BLOCKED,
    DESTINATION_UNREACHABLE,
}

data class EvacuationUiState(
    val hasLocationPermission: Boolean = false,
    val familyMeetingPointName: String? = null,
    val isLoadingRoute: Boolean = false,
    val currentLocation: GeoCoordinate? = null,
    val locationAccuracyMeters: Float? = null,
    val isNetworkAvailable: Boolean? = null,
    val bmkgStatus: BmkgStatus? = null,
    val isLoadingBmkgStatus: Boolean = false,
    val bmkgErrorMessage: String? = null,
    val localDatasetManifest: LocalDatasetManifest? = null,
    val datasetUpdateStatus: DatasetUpdateStatus? = null,
    val isCheckingDataUpdate: Boolean = false,
    val isInstallingDataUpdate: Boolean = false,
    val dataUpdateErrorMessage: String? = null,
    val dataUpdateInstallMessage: String? = null,
    val route: EvacuationRoute? = null,
    val previousRoutes: List<EvacuationRoute> = emptyList(),
    val offlineRoadOverlay: OfflineRoadOverlay? = null,
    val tsunamiZoneOverlay: TsunamiZoneOverlay? = null,
    val currentZoneStatus: InundationZoneStatus? = null,
    val isCheckingInitialZone: Boolean = false,
    val isOutsideInundationZoneAtStart: Boolean = false,
    val initialZoneCheckMessage: String? = null,
    val zoneTransitionVersion: Int = 0,
    val zoneTransitionMessage: String? = null,
    val guidance: RouteGuidanceSnapshot? = null,
    val deviceHeadingDegrees: Float? = null,
    val remainingEvacuationSeconds: Int = EVACUATION_WINDOW_SECONDS,
    val remainingAlternativeCount: Int = MAX_ALTERNATIVE_COUNT,
    val alternativeRouteVersion: Int = 0,
    val alternativeRouteMessage: String? = null,
    val directOrientation: DirectOrientation? = null,
    val hasArrived: Boolean = false,
    val arrivalReason: EvacuationArrivalReason? = null,
    val arrivalDistanceMeters: Int? = null,
    val errorMessage: String? = null,
    val compassMessage: String? = null,
    val activeEdgeId: Long? = null,
    val checkinStatus: CheckinStatus = CheckinStatus.IDLE,
    val checkinMessage: String? = null,
    val checkedInAt: String? = null,
    val blockedEdgeIds: Set<Long> = emptySet(),
    val confirmedBlockedEdgeIds: Set<Long> = emptySet(),
    val obstructionReportMessage: String? = null,
    val pendingObstructionCount: Int = 0,
    val occupancyStatus: OccupancyStatusResponseDto? = null,
    val isReportingOccupancy: Boolean = false,
    val occupancyReportMessage: String? = null,
    val showCheckinConfirmationDialog: Boolean = false,
) {
    val isCheckingIn: Boolean
        get() = checkinStatus == CheckinStatus.CHECKING_IN

    val destinationExternalId: String?
        get() = route?.destinationExternalId

    val datasetVersion: String
        get() = route?.datasetVersion.orEmpty()

    val locationQuality: LocationQuality
        get() = when (val accuracy = locationAccuracyMeters) {
            null -> LocationQuality.SEARCHING
            in 0f..15f -> LocationQuality.GOOD
            in 15f..35f -> LocationQuality.FAIR
            else -> LocationQuality.WEAK
        }

    val canSelectAlternative: Boolean
        get() = route != null && remainingAlternativeCount > 0 && !isLoadingRoute &&
            !hasArrived && !hasEvacuationWindowExpired

    val hasEvacuationWindowExpired: Boolean
        get() = remainingEvacuationSeconds <= 0 && !hasArrived &&
            !isOutsideInundationZoneAtStart

    val canReportBlockedRoute: Boolean
        get() = route != null && !isLoadingRoute && !hasArrived && directOrientation == null &&
            !hasEvacuationWindowExpired

    companion object {
        const val EVACUATION_WINDOW_SECONDS = 20 * 60
        const val MAX_ALTERNATIVE_COUNT = 2
    }
}

enum class LocationQuality {
    SEARCHING,
    GOOD,
    FAIR,
    WEAK,
}

enum class CheckinStatus {
    IDLE,
    CHECKING_IN,
    SUCCESS,
    FAILED,
}

enum class EvacuationArrivalReason {
    EVACUATION_POINT,
    OUTSIDE_INUNDATION_ZONE,
}
