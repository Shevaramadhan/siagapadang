package com.akusukaproject.siagapadang.ui.evacuation

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.domain.OffRouteTracker
import com.akusukaproject.siagapadang.sensor.DeviceLocation
import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
import com.akusukaproject.siagapadang.data.remote.ApiHttpException
import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportRequestDto
import com.akusukaproject.siagapadang.data.repository.ObstructionDeliverySummary
import com.akusukaproject.siagapadang.data.remote.model.OccupancyReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.ShelterCheckinRequestDto
import kotlinx.coroutines.Dispatchers
import com.akusukaproject.siagapadang.domain.ActiveEdgeFinder
import com.akusukaproject.siagapadang.domain.ArrivalConfirmationTracker
import com.akusukaproject.siagapadang.domain.DirectOrientationCalculator
import com.akusukaproject.siagapadang.domain.ManeuverGuidance
import com.akusukaproject.siagapadang.domain.ManeuverType
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.domain.RouteGuidanceCalculator
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot
import com.akusukaproject.siagapadang.domain.ZoneExitConfirmationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.abs
import kotlin.math.roundToInt

class EvacuationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SiagaPadangApplication
    private val repository = app.evacuationRepository
    private val zoneRepository = app.zoneRepository
    private val mutableUiState = MutableStateFlow(EvacuationUiState())
    val uiState: StateFlow<EvacuationUiState> = mutableUiState.asStateFlow()

    private var locationJob: Job? = null
    private var compassJob: Job? = null
    private var routeJob: Job? = null
    private var countdownJob: Job? = null
    private var zoneStatusJob: Job? = null
    private var initialZoneJob: Job? = null
    private var offlineRoadOverlayJob: Job? = null
    private var initialRouteRequested = false
    private var initialLocationResolved = false
    private var minimumRouteIndex = 0
    private var minimumRouteSegmentFraction = 0.0
    private var announcedManeuverIndex: Int? = null
    private var lastZoneCheckLocation: GeoCoordinate? = null
    private var lastZoneCheckElapsedMillis = 0L
    private var lastOfflineRoadCenter: GeoCoordinate? = null
    private var confirmedZoneKey: String? = null
    private var pendingZoneKey: String? = null
    private var pendingZoneConfirmationCount = 0
    private val rejectedDestinationNames = mutableSetOf<String>()
    private val offRouteTracker = OffRouteTracker()
    private var rerouteJob: Job? = null
    private val arrivalTracker = ArrivalConfirmationTracker()
    private val zoneExitTracker = ZoneExitConfirmationTracker()

    init {
        loadTsunamiZoneOverlay()
        loadLocalDatasetManifest()
        monitorNetworkStatus()
    }

    fun onMapViewportChanged(center: GeoCoordinate) {
        val previousCenter = lastOfflineRoadCenter
        if (
            previousCenter != null &&
            NearestNodeFinder.distanceMeters(previousCenter, center) < ROAD_VIEWPORT_RELOAD_METERS
        ) {
            return
        }
        lastOfflineRoadCenter = center
        offlineRoadOverlayJob?.cancel()
        offlineRoadOverlayJob = viewModelScope.launch {
            delay(ROAD_VIEWPORT_DEBOUNCE_MILLIS)
            runCatching { repository.loadOfflineRoadOverlay(center) }
                .onSuccess { overlay ->
                    mutableUiState.update { state ->
                        state.copy(offlineRoadOverlay = overlay)
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Jaringan jalan lokal tidak dapat dimuat", error)
                }
        }
    }

    private fun monitorNetworkStatus() {
        viewModelScope.launch {
            app.networkStatusProvider.availability()
                .catch {
                    mutableUiState.update { state -> state.copy(isNetworkAvailable = false) }
                }
                .collect { isAvailable ->
                    val shouldLoadBmkg = isAvailable &&
                        mutableUiState.value.bmkgStatus == null &&
                        !mutableUiState.value.isLoadingBmkgStatus
                    mutableUiState.update { state ->
                        state.copy(isNetworkAvailable = isAvailable)
                    }
                    if (shouldLoadBmkg) refreshBmkgStatus()
                    if (isAvailable) {
                        flushPendingObstructionReports()
                        refreshConfirmedObstructions()
                    }
                }
        }
    }

    private fun loadTsunamiZoneOverlay() {
        viewModelScope.launch {
            runCatching { zoneRepository.loadMapOverlay() }
                .onSuccess { overlay ->
                    mutableUiState.update { state ->
                        state.copy(tsunamiZoneOverlay = overlay)
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Layer zona tsunami tidak dapat dimuat", error)
                }
        }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        mutableUiState.update { state ->
            state.copy(
                hasLocationPermission = granted,
                errorMessage = if (granted) null else "Izin lokasi diperlukan untuk mencari rute evakuasi.",
            )
        }
        if (granted) {
            startLocationAndCompass()
        } else {
            locationJob?.cancel()
            compassJob?.cancel()
            locationJob = null
            compassJob = null
        }
    }

    fun retryRoute() {
        if (mutableUiState.value.isOutsideInundationZoneAtStart) return
        initialRouteRequested = false
        mutableUiState.value.currentLocation?.let(::requestInitialRoute)
    }

    fun refreshFamilyMeetingPoint() {
        viewModelScope.launch {
            val meetingPoint = withContext(Dispatchers.IO) {
                runCatching { app.familyPlanRepository.load().meetingPointName }.getOrNull()
            }
            mutableUiState.update { state -> state.copy(familyMeetingPointName = meetingPoint) }
        }
    }

    fun refreshBmkgStatus() {
        if (mutableUiState.value.isLoadingBmkgStatus) return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isLoadingBmkgStatus = true, bmkgErrorMessage = null)
            }
            runCatching { app.bmkgApiClient.getLatestStatus() }
                .onSuccess { status ->
                    mutableUiState.update {
                        it.copy(
                            bmkgStatus = status,
                            isLoadingBmkgStatus = false,
                            bmkgErrorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Status BMKG tidak dapat dimuat", error)
                    mutableUiState.update {
                        it.copy(
                            isLoadingBmkgStatus = false,
                            bmkgErrorMessage = "Informasi BMKG belum dapat diambil.",
                        )
                    }
                }
        }
    }

    private fun loadLocalDatasetManifest() {
        viewModelScope.launch {
            runCatching { app.dataUpdateApiClient.loadLocalManifest() }
                .onSuccess { manifest ->
                    mutableUiState.update { state ->
                        state.copy(localDatasetManifest = manifest)
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Manifest dataset lokal tidak dapat dibaca", error)
                }
        }
    }

    fun checkDataUpdates() {
        if (mutableUiState.value.isCheckingDataUpdate || mutableUiState.value.isInstallingDataUpdate) return
        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isCheckingDataUpdate = true,
                    dataUpdateErrorMessage = null,
                    dataUpdateInstallMessage = null,
                )
            }
            runCatching { app.dataUpdateApiClient.checkForUpdates() }
                .onSuccess { status ->
                    mutableUiState.update { state ->
                        state.copy(
                            localDatasetManifest = status.local,
                            datasetUpdateStatus = status,
                            isCheckingDataUpdate = false,
                            dataUpdateErrorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Pembaruan dataset tidak dapat diperiksa", error)
                    mutableUiState.update { state ->
                        state.copy(
                            isCheckingDataUpdate = false,
                            dataUpdateErrorMessage =
                                "Versi terbaru belum dapat diperiksa. Data lokal tetap dapat digunakan.",
                        )
                    }
                }
        }
    }

    fun installDataUpdate() {
        val remote = mutableUiState.value.datasetUpdateStatus?.downloadableVersion ?: return
        if (mutableUiState.value.isInstallingDataUpdate) return
        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isInstallingDataUpdate = true,
                    dataUpdateErrorMessage = null,
                    dataUpdateInstallMessage = "Mengunduh dan memeriksa paket data…",
                )
            }
            runCatching { app.datasetPackageInstaller.downloadAndStage(remote) }
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            isInstallingDataUpdate = false,
                            dataUpdateInstallMessage =
                                "Pembaruan telah diverifikasi. Tutup dan buka kembali aplikasi untuk mengaktifkannya.",
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Paket dataset gagal dipasang", error)
                    mutableUiState.update { state ->
                        state.copy(
                            isInstallingDataUpdate = false,
                            dataUpdateErrorMessage =
                                error.message ?: "Paket pembaruan gagal dipasang. Data lama tetap digunakan.",
                            dataUpdateInstallMessage = null,
                        )
                    }
                }
        }
    }

    /**
     * Menangani tombol "Ada kendala?". Semua keputusan diambil dari data lokal tanpa jaringan.
     *
     * - Jalan tidak bisa dilewati: cari jalan memutar ke TES yang sama lewat simpang tetangga;
     *   bila tidak ada atau lebih lambat, pindah ke alternatif tujuan (rank 2/3).
     * - Tidak bisa masuk ke TES/TEA: langsung ke alternatif tujuan tanpa melaporkan jalan.
     */
    fun reportEvacuationObstacle(type: EvacuationObstacleType) {
        val currentState = mutableUiState.value
        val currentRoute = currentState.route ?: return
        val currentLocation = currentState.currentLocation ?: return
        if (!currentState.canReportBlockedRoute || routeJob?.isActive == true) return

        val isRoadBlocked = type == EvacuationObstacleType.ROAD_BLOCKED
        val blockedEdgeId = currentState.activeEdgeId.takeIf { isRoadBlocked }
        val updatedBlockedEdgeIds = if (blockedEdgeId != null) {
            currentState.blockedEdgeIds + blockedEdgeId
        } else {
            currentState.blockedEdgeIds
        }
        val excludedEdgeIds = updatedBlockedEdgeIds + currentState.confirmedBlockedEdgeIds

        routeJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoadingRoute = true, errorMessage = null) }
            val detour = if (isRoadBlocked) {
                runCatching {
                    repository.findDetourToSameDestination(
                        location = currentLocation,
                        currentRoute = currentRoute,
                        blockedEdgeId = blockedEdgeId,
                        blockedEdgeIds = excludedEdgeIds,
                    )
                }.getOrNull()
            } else {
                null
            }
            Log.i(
                LOG_TAG,
                "Kendala $type: jalan memutar ke TES sama ${if (detour != null) "ditemukan (${detour.estimatedSeconds} s)" else "tidak ada"}",
            )
            val alternative = if (currentState.remainingAlternativeCount > 0) {
                runCatching {
                    repository.findAlternativeRoute(
                        location = currentLocation,
                        currentRoute = currentRoute,
                        excludedDestinationNames = rejectedDestinationNames,
                        excludedEdgeIds = excludedEdgeIds,
                    )
                }.getOrNull()
            } else {
                null
            }

            when {
                detour != null && (alternative == null || detour.estimatedSeconds <= alternative.estimatedSeconds) ->
                    applyNewRoute(
                        currentRoute = currentRoute,
                        newRoute = detour,
                        consumesAlternative = false,
                        message = "Jalan dialihkan. Tetap menuju ${detour.destinationName} lewat jalan lain.",
                        blockedEdgeIds = updatedBlockedEdgeIds,
                    )
                alternative != null -> {
                    rejectedDestinationNames += currentRoute.destinationName
                    applyNewRoute(
                        currentRoute = currentRoute,
                        newRoute = alternative,
                        consumesAlternative = true,
                        message = buildAlternativeRouteMessage(currentRoute, alternative),
                        blockedEdgeIds = updatedBlockedEdgeIds,
                    )
                }
                isRoadBlocked -> showDirectOrientation(currentRoute, currentLocation, updatedBlockedEdgeIds)
                else -> mutableUiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        alternativeRouteVersion = it.alternativeRouteVersion + 1,
                        alternativeRouteMessage =
                            "Tidak ada alternatif tujuan lain di data. Ikuti arahan petugas di lokasi.",
                    )
                }
            }

            if (isRoadBlocked) {
                queueObstructionReport(
                    blockedEdgeId = blockedEdgeId,
                    location = currentLocation,
                    message = "Rute dialihkan. Laporan jalan terhalang sedang diproses...",
                )
            }
        }
    }

    private fun applyNewRoute(
        currentRoute: EvacuationRoute,
        newRoute: EvacuationRoute,
        consumesAlternative: Boolean,
        message: String,
        blockedEdgeIds: Set<Long>,
    ) {
        arrivalTracker.reset()
        resetRouteProgress()
        mutableUiState.update { state ->
            withGuidance(
                state.copy(
                    route = newRoute,
                    previousRoutes = if (newRoute.destinationName == currentRoute.destinationName) {
                        state.previousRoutes
                    } else {
                        (state.previousRoutes + currentRoute).distinctBy { it.destinationName }
                    },
                    isLoadingRoute = false,
                    remainingAlternativeCount = if (consumesAlternative) {
                        (state.remainingAlternativeCount - 1).coerceAtLeast(0)
                    } else {
                        state.remainingAlternativeCount
                    },
                    alternativeRouteVersion = state.alternativeRouteVersion + 1,
                    alternativeRouteMessage = message,
                    directOrientation = null,
                    hasArrived = false,
                    arrivalReason = null,
                    arrivalDistanceMeters = null,
                    checkinStatus = CheckinStatus.IDLE,
                    checkinMessage = null,
                    checkedInAt = null,
                    blockedEdgeIds = blockedEdgeIds,
                ),
            )
        }
    }

    private fun showDirectOrientation(
        currentRoute: EvacuationRoute,
        currentLocation: GeoCoordinate,
        blockedEdgeIds: Set<Long>,
    ) {
        val directOrientation = DirectOrientationCalculator.calculate(
            currentLocation = currentLocation,
            destinationName = currentRoute.destinationName,
            destinationCoordinate = currentRoute.destinationCoordinate,
            routeCoordinates = currentRoute.coordinates,
        )
        if (directOrientation == null) {
            mutableUiState.update { state ->
                state.copy(
                    isLoadingRoute = false,
                    errorMessage = "Tidak ada rute jalan atau titik tujuan yang dapat digunakan untuk orientasi.",
                )
            }
            return
        }
        rejectedDestinationNames += currentRoute.destinationName
        resetRouteProgress()
        mutableUiState.update { state ->
            state.copy(
                isLoadingRoute = false,
                previousRoutes = (state.previousRoutes + currentRoute).distinctBy { it.destinationName },
                directOrientation = directOrientation,
                guidance = null,
                activeEdgeId = null,
                alternativeRouteVersion = state.alternativeRouteVersion + 1,
                alternativeRouteMessage = "Semua rute jalan telah ditolak. Orientasi garis lurus diaktifkan.",
                blockedEdgeIds = blockedEdgeIds,
                errorMessage = null,
            )
        }
    }

    private fun queueObstructionReport(
        blockedEdgeId: Long?,
        location: GeoCoordinate,
        message: String,
    ) {
        if (blockedEdgeId == null) {
            mutableUiState.update { state -> state.copy(obstructionReportMessage = message) }
            return
        }
        val report = ObstructionReportRequestDto(
            latitude = location.latitude,
            longitude = location.longitude,
            // ID versi dataset server baru ditentukan saat laporan dikirim.
            datasetVersionId = UNRESOLVED_DATASET_VERSION_ID,
            edgeExternalId = blockedEdgeId.toString(),
            description = "Jalur terhalang dilaporkan warga via aplikasi",
        )
        app.obstructionReportQueue.enqueue(report)
        mutableUiState.update { state ->
            state.copy(
                pendingObstructionCount = app.obstructionReportQueue.getPendingReports().size,
                obstructionReportMessage = message,
            )
        }
        flushPendingObstructionReports()
    }

    fun flushPendingObstructionReports() {
        viewModelScope.launch(Dispatchers.IO) {
            val pendingReports = app.obstructionReportQueue.getPendingReports()
            if (pendingReports.isEmpty()) {
                mutableUiState.update { it.copy(pendingObstructionCount = 0) }
                return@launch
            }

            val summary = resolveServerDatasetVersionId().fold(
                onSuccess = { serverVersionId ->
                    if (serverVersionId == null) {
                        // ID ruas di HP bisa menunjuk jalan lain pada dataset server yang berbeda,
                        // sehingga laporan tidak dikirim dan tidak diulang.
                        app.obstructionReportQueue.clear()
                        ObstructionDeliverySummary(isDatasetMismatch = true)
                    } else {
                        sendPendingObstructionReports(pendingReports, serverVersionId)
                    }
                },
                onFailure = { error ->
                    val remaining = pendingReports.size
                    if (error is ApiHttpException) {
                        ObstructionDeliverySummary(serverErrorCount = remaining, remainingCount = remaining)
                    } else {
                        ObstructionDeliverySummary(offlineCount = remaining, remainingCount = remaining)
                    }
                },
            )

            mutableUiState.update { state ->
                state.copy(
                    pendingObstructionCount = summary.remainingCount,
                    obstructionReportMessage = summary.message() ?: state.obstructionReportMessage,
                )
            }
        }
    }

    private suspend fun sendPendingObstructionReports(
        pendingReports: List<ObstructionReportRequestDto>,
        serverVersionId: Int,
    ): ObstructionDeliverySummary {
        var summary = ObstructionDeliverySummary()
        for (report in pendingReports) {
            app.emergencyApiClient
                .reportObstruction(report.copy(datasetVersionId = serverVersionId))
                .onSuccess { response ->
                    app.obstructionReportQueue.remove(report.edgeExternalId)
                    summary = summary.copy(
                        sentCount = summary.sentCount + 1,
                        confirmedCount = summary.confirmedCount +
                            if (response.isConfirmedBlocked) 1 else 0,
                    )
                }
                .onFailure { error ->
                    summary = when {
                        error is ApiHttpException && error.isRejectedByServer -> {
                            // Penolakan 4xx tidak berubah bila diulang, jadi dikeluarkan dari antrean.
                            app.obstructionReportQueue.remove(report.edgeExternalId)
                            summary.copy(
                                rejectedCount = summary.rejectedCount + 1,
                                rejectionReason = error.message,
                            )
                        }
                        error is ApiHttpException ->
                            summary.copy(serverErrorCount = summary.serverErrorCount + 1)
                        else -> summary.copy(offlineCount = summary.offlineCount + 1)
                    }
                }
        }
        return summary.copy(remainingCount = app.obstructionReportQueue.getPendingReports().size)
    }

    fun refreshConfirmedObstructions() {
        viewModelScope.launch(Dispatchers.IO) {
            // Ruas terkonfirmasi hanya dipakai bila dataset server sama dengan dataset di HP.
            val serverVersionId = resolveServerDatasetVersionId().getOrNull() ?: return@launch
            app.emergencyApiClient.getConfirmedObstructions(serverVersionId)
                .onSuccess { externalIds ->
                    val edgeIds = externalIds.mapNotNull { it.toLongOrNull() }.toSet()
                    if (edgeIds.isNotEmpty()) {
                        mutableUiState.update { state ->
                            state.copy(confirmedBlockedEdgeIds = state.confirmedBlockedEdgeIds + edgeIds)
                        }
                    }
                }
        }
    }

    /**
     * ID versi dataset server yang checksum-nya sama dengan dataset di HP. Sukses dengan null
     * berarti server terjangkau tetapi tidak memiliki dataset yang sama.
     */
    private suspend fun resolveServerDatasetVersionId(): Result<Int?> = runCatching {
        app.dataUpdateApiClient.checkForUpdates().serverNetworkVersionIdForLocal
    }

    fun dismissObstructionMessage() {
        mutableUiState.update { it.copy(obstructionReportMessage = null) }
    }

    /** Meminta konfirmasi sebelum mengirim check-in ke posko. */
    fun requestCheckinConfirmation() {
        mutableUiState.update { it.copy(showCheckinConfirmationDialog = true) }
    }

    /** Menutup dialog konfirmasi check-in tanpa mengirim data. */
    fun dismissCheckinConfirmation() {
        mutableUiState.update { it.copy(showCheckinConfirmationDialog = false) }
    }

    fun performShelterCheckin() {
        mutableUiState.update { it.copy(showCheckinConfirmationDialog = false) }
        val currentState = mutableUiState.value
        if (
            !currentState.hasArrived ||
            currentState.arrivalReason != EvacuationArrivalReason.EVACUATION_POINT ||
            currentState.isCheckingIn
        ) return

        val tesId = currentState.destinationExternalId
        if (tesId.isNullOrBlank()) {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.FAILED,
                    checkinMessage = "ID tempat evakuasi tidak tersedia pada rute.",
                )
            }
            return
        }

        val location = currentState.currentLocation
        if (location == null) {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.FAILED,
                    checkinMessage = "Lokasi GPS belum tersedia untuk lapor selamat.",
                )
            }
            return
        }

        val accuracy = currentState.locationAccuracyMeters ?: 50f
        if (accuracy > MAX_CHECKIN_ACCURACY_METERS) {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.FAILED,
                    checkinMessage = "Akurasi GPS (${accuracy.roundToInt()}m) melebihi batas maksimal 35m. Tunggu sinyal GPS membaik.",
                )
            }
            return
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.CHECKING_IN,
                    checkinMessage = "Menghubungi posko bencana...",
                )
            }

            val request = ShelterCheckinRequestDto(
                evacuationPointExternalId = tesId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = accuracy,
                status = "Selamat",
            )

            val result = app.emergencyApiClient.checkIn(request)
            result.onSuccess { response ->
                mutableUiState.update {
                    it.copy(
                        checkinStatus = CheckinStatus.SUCCESS,
                        checkinMessage = response.message.ifBlank { "Lapor selamat berhasil dicatat posko." },
                        checkedInAt = response.checkedInAt,
                    )
                }
                fetchShelterOccupancy(tesId)
            }.onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        checkinStatus = CheckinStatus.FAILED,
                        checkinMessage = error.message ?: "Gagal terhubung ke posko bencana.",
                    )
                }
            }
        }
    }

    fun resetCheckinStatus() {
        mutableUiState.update {
            it.copy(
                checkinStatus = CheckinStatus.IDLE,
                checkinMessage = null,
                checkedInAt = null,
                occupancyStatus = null,
                isReportingOccupancy = false,
                occupancyReportMessage = null,
            )
        }
    }

    fun reportShelterOccupancy(level: String) {
        val currentState = mutableUiState.value
        if (currentState.checkinStatus != CheckinStatus.SUCCESS || currentState.isReportingOccupancy) return
        val tesId = currentState.destinationExternalId ?: return

        viewModelScope.launch(Dispatchers.IO) {
            mutableUiState.update {
                it.copy(
                    isReportingOccupancy = true,
                    occupancyReportMessage = "Mengirim laporan kondisi TES...",
                )
            }
            val result = app.emergencyApiClient.reportOccupancy(
                OccupancyReportRequestDto(
                    evacuationPointExternalId = tesId,
                    level = level,
                ),
            )
            result.onSuccess { statusResponse ->
                mutableUiState.update {
                    it.copy(
                        isReportingOccupancy = false,
                        occupancyStatus = statusResponse,
                        occupancyReportMessage = "Terima kasih, laporan kondisi TES berhasil diperbarui.",
                    )
                }
            }.onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        isReportingOccupancy = false,
                        occupancyReportMessage = error.message ?: "Gagal mengirim laporan kondisi TES.",
                    )
                }
            }
        }
    }

    fun fetchShelterOccupancy(evacuationPointExternalId: String? = null) {
        val tesId = evacuationPointExternalId ?: mutableUiState.value.destinationExternalId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = app.emergencyApiClient.getOccupancyStatus(tesId)
            result.onSuccess { statusResponse ->
                mutableUiState.update {
                    it.copy(occupancyStatus = statusResponse)
                }
            }
        }
    }

    private fun startLocationAndCompass() {
        if (locationJob == null) {
            locationJob = viewModelScope.launch {
                app.locationProvider.locations()
                    .catch { error ->
                        mutableUiState.update {
                            it.copy(errorMessage = error.message ?: "Lokasi perangkat tidak tersedia.")
                        }
                    }
                    .collect { deviceLocation ->
                        var arrivalConfirmedNow = false
                        mutableUiState.update { state ->
                            val updatedState = withArrivalEvaluation(
                                state.copy(
                                    currentLocation = deviceLocation.coordinate,
                                    locationAccuracyMeters = deviceLocation.accuracyMeters,
                                ),
                            )
                            arrivalConfirmedNow = !state.hasArrived && updatedState.hasArrived
                            updatedState
                        }
                        if (arrivalConfirmedNow) onArrivalConfirmed()
                        if (mutableUiState.value.isOutsideInundationZoneAtStart) {
                            return@collect
                        }
                        if (!initialLocationResolved) {
                            inspectInitialZone(deviceLocation)
                            return@collect
                        }
                        if (!arrivalConfirmedNow) maybeVibrateUpcomingManeuver()
                        requestInitialRoute(deviceLocation.coordinate)
                        maybeRecalculateRouteForNewPosition(deviceLocation)
                        evaluateCurrentZone(
                            location = deviceLocation.coordinate,
                            accuracyMeters = deviceLocation.accuracyMeters,
                        )
                    }
            }
        }

        if (compassJob == null) {
            compassJob = viewModelScope.launch {
                app.compassProvider.headings()
                    .catch { error ->
                        mutableUiState.update {
                            it.copy(compassMessage = error.message ?: "Kompas tidak tersedia.")
                        }
                    }
                    .collect { heading ->
                        mutableUiState.update { state ->
                            withGuidance(
                                state.copy(
                                    deviceHeadingDegrees = heading,
                                    compassMessage = null,
                                ),
                            )
                        }
                    }
            }
        }
    }

    private fun inspectInitialZone(deviceLocation: DeviceLocation) {
        if (initialZoneJob?.isActive == true || initialLocationResolved) return
        initialZoneJob = viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isCheckingInitialZone = true,
                    initialZoneCheckMessage = null,
                )
            }
            val result = runCatching { zoneRepository.findStatus(deviceLocation.coordinate) }
            val status = result.getOrNull()
            if (status != null) {
                applyZoneStatus(status, deviceLocation.accuracyMeters)
            } else {
                Log.w(LOG_TAG, "Status zona awal tidak dapat diperiksa", result.exceptionOrNull())
            }
            if (mutableUiState.value.isOutsideInundationZoneAtStart) return@launch

            // Jika data zona atau akurasi GPS belum cukup, arahan evakuasi tetap disiapkan.
            // Pembaruan lokasi berikutnya akan terus memeriksa zona dan dapat menghentikan
            // navigasi bila posisi luar zona kemudian terkonfirmasi dengan akurat.
            initialLocationResolved = true
            mutableUiState.update { it.copy(isCheckingInitialZone = false) }
            requestInitialRoute(mutableUiState.value.currentLocation ?: deviceLocation.coordinate)
        }
    }

    private fun requestInitialRoute(location: GeoCoordinate) {
        if (initialRouteRequested || mutableUiState.value.isOutsideInundationZoneAtStart) return
        initialRouteRequested = true
        routeJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            mutableUiState.update { it.copy(isLoadingRoute = true, errorMessage = null) }
            runCatching {
                repository.findRouteFromLocation(location)
            }.onSuccess { route ->
                val elapsedMillis = System.currentTimeMillis() - startedAt
                logRouteTiming(elapsedMillis)
                rejectedDestinationNames.clear()
                arrivalTracker.reset()
                resetRouteProgress()
                mutableUiState.update { state ->
                    withGuidance(
                        state.copy(
                            route = route,
                            previousRoutes = emptyList(),
                            isLoadingRoute = false,
                            remainingAlternativeCount = EvacuationUiState.MAX_ALTERNATIVE_COUNT,
                            alternativeRouteMessage = null,
                            directOrientation = null,
                            hasArrived = false,
                            arrivalReason = null,
                            arrivalDistanceMeters = null,
                            errorMessage = null,
                            checkinStatus = CheckinStatus.IDLE,
                            checkinMessage = null,
                            checkedInAt = null,
                        ),
                    )
                }
                startCountdown()
            }.onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        errorMessage = error.message ?: "Rute evakuasi tidak dapat disiapkan.",
                    )
                }
            }
        }
    }

    private fun evaluateCurrentZone(location: GeoCoordinate, accuracyMeters: Float?) {
        val movedMeters = lastZoneCheckLocation?.let { previous ->
            NearestNodeFinder.distanceMeters(previous, location)
        } ?: Double.POSITIVE_INFINITY
        val now = SystemClock.elapsedRealtime()
        val elapsedMillis = now - lastZoneCheckElapsedMillis
        if (
            (movedMeters < MIN_ZONE_CHECK_MOVEMENT_METERS &&
                elapsedMillis < MAX_ZONE_CHECK_INTERVAL_MILLIS) ||
            zoneStatusJob?.isActive == true
        ) return
        lastZoneCheckLocation = location
        lastZoneCheckElapsedMillis = now
        zoneStatusJob = viewModelScope.launch {
            runCatching { zoneRepository.findStatus(location) }
                .onSuccess { status -> applyZoneStatus(status, accuracyMeters) }
                .onFailure { error -> Log.w(LOG_TAG, "Status zona tidak dapat diperbarui", error) }
        }
    }

    private fun applyZoneStatus(status: InundationZoneStatus, accuracyMeters: Float?) {
        if (status == InundationZoneStatus.DataUnavailable) {
            zoneExitTracker.update(status, accuracyMeters)
            pendingZoneKey = null
            pendingZoneConfirmationCount = 0
            return
        }
        val exitConfirmed = zoneExitTracker.update(status, accuracyMeters)
        val initialDecision = decideInitialZone(status, accuracyMeters, MAX_ZONE_ACCURACY_METERS)
        if (initialDecision == InitialZoneDecision.UNCONFIRMED) {
            pendingZoneKey = null
            pendingZoneConfirmationCount = 0
            return
        }
        val candidateKey = status.zoneCategoryKey()
        if (confirmedZoneKey == null) {
            confirmZoneStatus(status, candidateKey, isInitial = true)
            if (initialDecision == InitialZoneDecision.OUTSIDE_RECORDED_ZONE) {
                enterInitialOutsideZone()
                return
            }
            if (exitConfirmed) confirmOutsideZoneArrival()
            return
        }
        if (candidateKey == confirmedZoneKey) {
            pendingZoneKey = null
            pendingZoneConfirmationCount = 0
            if (exitConfirmed) confirmOutsideZoneArrival()
            return
        }
        if (pendingZoneKey == candidateKey) {
            pendingZoneConfirmationCount += 1
        } else {
            pendingZoneKey = candidateKey
            pendingZoneConfirmationCount = 1
        }
        if (pendingZoneConfirmationCount >= REQUIRED_ZONE_TRANSITION_CONFIRMATIONS) {
            confirmZoneStatus(status, candidateKey, isInitial = false)
        }
        if (exitConfirmed) confirmOutsideZoneArrival()
    }

    private fun enterInitialOutsideZone() {
        routeJob?.cancel()
        rerouteJob?.cancel()
        countdownJob?.cancel()
        routeJob = null
        rerouteJob = null
        countdownJob = null
        initialRouteRequested = true
        initialLocationResolved = true
        arrivalTracker.reset()
        offRouteTracker.reset()
        resetRouteProgress()
        rejectedDestinationNames.clear()
        mutableUiState.update { state ->
            state.copy(
                isCheckingInitialZone = false,
                isOutsideInundationZoneAtStart = true,
                initialZoneCheckMessage = null,
                route = null,
                previousRoutes = emptyList(),
                guidance = null,
                directOrientation = null,
                isLoadingRoute = false,
                hasArrived = false,
                arrivalReason = null,
                arrivalDistanceMeters = null,
                activeEdgeId = null,
                remainingEvacuationSeconds = EvacuationUiState.EVACUATION_WINDOW_SECONDS,
                errorMessage = null,
            )
        }
    }

    fun recheckInitialZone() {
        val location = mutableUiState.value.currentLocation
        val accuracyMeters = mutableUiState.value.locationAccuracyMeters
        if (location == null || initialZoneJob?.isActive == true) return
        initialZoneJob = viewModelScope.launch {
            mutableUiState.update {
                it.copy(isCheckingInitialZone = true, initialZoneCheckMessage = null)
            }
            runCatching { zoneRepository.findStatus(location) }
                .onSuccess { status ->
                    when (decideInitialZone(status, accuracyMeters, MAX_ZONE_ACCURACY_METERS)) {
                        InitialZoneDecision.OUTSIDE_RECORDED_ZONE -> {
                            confirmZoneStatus(status, status.zoneCategoryKey(), isInitial = true)
                            mutableUiState.update {
                                it.copy(
                                    isCheckingInitialZone = false,
                                    initialZoneCheckMessage =
                                        "Posisi masih berada di luar zona rendaman.",
                                )
                            }
                        }
                        InitialZoneDecision.INSIDE_RECORDED_ZONE -> {
                            zoneExitTracker.reset()
                            zoneExitTracker.update(status, accuracyMeters)
                            confirmZoneStatus(status, status.zoneCategoryKey(), isInitial = true)
                            initialRouteRequested = false
                            initialLocationResolved = true
                            mutableUiState.update {
                                it.copy(
                                    isCheckingInitialZone = false,
                                    isOutsideInundationZoneAtStart = false,
                                    initialZoneCheckMessage = null,
                                    remainingEvacuationSeconds =
                                        EvacuationUiState.EVACUATION_WINDOW_SECONDS,
                                )
                            }
                            requestInitialRoute(location)
                        }
                        InitialZoneDecision.UNCONFIRMED -> {
                            mutableUiState.update {
                                it.copy(
                                    isCheckingInitialZone = false,
                                    initialZoneCheckMessage =
                                        "Akurasi GPS belum cukup untuk memastikan posisi. Coba lagi di tempat terbuka.",
                                )
                            }
                        }
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Status zona awal tidak dapat diperiksa ulang", error)
                    mutableUiState.update {
                        it.copy(
                            isCheckingInitialZone = false,
                            initialZoneCheckMessage =
                                "Status zona belum dapat diperiksa. Data lokal tetap digunakan.",
                        )
                    }
                }
        }
    }

    private fun confirmOutsideZoneArrival() {
        var confirmedNow = false
        mutableUiState.update { state ->
            if (state.hasArrived || state.route == null) return@update state
            confirmedNow = true
            state.copy(
                hasArrived = true,
                arrivalReason = EvacuationArrivalReason.OUTSIDE_INUNDATION_ZONE,
                arrivalDistanceMeters = null,
                directOrientation = null,
                guidance = outsideZoneArrivalGuidance(),
            )
        }
        if (confirmedNow) onArrivalConfirmed()
    }

    private fun confirmZoneStatus(
        status: InundationZoneStatus,
        categoryKey: String,
        isInitial: Boolean,
    ) {
        confirmedZoneKey = categoryKey
        pendingZoneKey = null
        pendingZoneConfirmationCount = 0
        mutableUiState.update { state ->
            state.copy(
                currentZoneStatus = status,
                zoneTransitionVersion = state.zoneTransitionVersion + 1,
                zoneTransitionMessage = status.zoneMessage(isInitial),
            )
        }
    }

    private fun InundationZoneStatus.zoneCategoryKey(): String = when (this) {
        InundationZoneStatus.DataUnavailable -> "unknown"
        InundationZoneStatus.OutsideRecordedZone -> "safe"
        is InundationZoneStatus.InsideRecordedZone ->
            "risk-${dangerLevel.trim().lowercase()}"
    }

    private fun InundationZoneStatus.zoneMessage(isInitial: Boolean): String = when (this) {
        InundationZoneStatus.DataUnavailable -> "Status zona belum tersedia"
        InundationZoneStatus.OutsideRecordedZone ->
            if (isInitial) {
                "Lokasi Anda di luar zona rendaman"
            } else {
                "Lokasi Anda keluar dari zona rendaman"
            }
        is InundationZoneStatus.InsideRecordedZone ->
            if (isInitial) {
                "Lokasi Anda di zona rendaman"
            } else {
                "Lokasi Anda memasuki zona rendaman"
            }
    }

    private fun startCountdown() {
        val state = mutableUiState.value
        if (countdownJob?.isActive == true || state.hasArrived ||
            state.isOutsideInundationZoneAtStart
        ) return
        val startedAt = SystemClock.elapsedRealtime()
        countdownJob = viewModelScope.launch {
            while (isActive && !mutableUiState.value.hasArrived) {
                val elapsedSeconds = (
                    (SystemClock.elapsedRealtime() - startedAt) / 1_000L
                    ).toInt()
                val remainingSeconds = (
                    EvacuationUiState.EVACUATION_WINDOW_SECONDS - elapsedSeconds
                    ).coerceAtLeast(0)
                mutableUiState.update { state ->
                    state.copy(remainingEvacuationSeconds = remainingSeconds)
                }
                if (remainingSeconds == 0) break
                delay(1_000)
            }
        }
    }

    private fun withGuidance(state: EvacuationUiState): EvacuationUiState {
        val location = state.currentLocation ?: return state
        val route = state.route ?: return state
        if (state.directOrientation != null) {
            val directOrientation = DirectOrientationCalculator.calculate(
                currentLocation = location,
                destinationName = route.destinationName,
                destinationCoordinate = route.destinationCoordinate,
                routeCoordinates = route.coordinates,
            )
            return state.copy(
                directOrientation = directOrientation ?: state.directOrientation,
                guidance = null,
                activeEdgeId = null,
            )
        }
        val routeCoordinates = route.coordinates
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = location,
            routeCoordinates = routeCoordinates,
            minimumRouteIndex = minimumRouteIndex,
            minimumSegmentFraction = minimumRouteSegmentFraction,
            deviceHeadingDegrees = state.deviceHeadingDegrees,
            previousApproachType = state.guidance
                ?.takeIf(RouteGuidanceSnapshot::isApproachingRoute)
                ?.currentInstruction
                ?.type,
        )
        guidance?.let { snapshot ->
            when {
                snapshot.nearestRouteIndex > minimumRouteIndex -> {
                    minimumRouteIndex = snapshot.nearestRouteIndex
                    minimumRouteSegmentFraction = snapshot.routeSegmentFraction
                }
                snapshot.nearestRouteIndex == minimumRouteIndex -> {
                    minimumRouteSegmentFraction = maxOf(
                        minimumRouteSegmentFraction,
                        snapshot.routeSegmentFraction,
                    )
                }
            }
        }
        val activeEdgeId = route.let { currentRoute ->
            ActiveEdgeFinder.findActiveEdgeId(
                location = location,
                route = currentRoute,
                nearestRouteCoordinateIndex = guidance?.nearestRouteIndex,
            )
        }
        return state.copy(
            guidance = guidance,
            activeEdgeId = activeEdgeId,
        )
    }

    private fun withArrivalEvaluation(state: EvacuationUiState): EvacuationUiState {
        val guidedState = withGuidance(state)
        val location = guidedState.currentLocation ?: return guidedState
        val route = guidedState.route ?: return guidedState
        val targets = buildList {
            route.destinationCoordinate?.let(::add)
            route.coordinates.lastOrNull()?.let(::add)
        }
        if (targets.isEmpty()) return guidedState
        val distanceMeters = targets.minOf { target ->
            NearestNodeFinder.distanceMeters(location, target)
        }.roundToInt().coerceAtLeast(0)

        val hasArrived = guidedState.hasArrived || arrivalTracker.update(
            distanceMeters = distanceMeters.toDouble(),
            accuracyMeters = guidedState.locationAccuracyMeters,
        )
        return if (hasArrived) {
            guidedState.copy(
                hasArrived = true,
                arrivalReason = EvacuationArrivalReason.EVACUATION_POINT,
                arrivalDistanceMeters = distanceMeters,
                directOrientation = null,
                guidance = arrivalGuidance(distanceMeters),
            )
        } else {
            guidedState.copy(arrivalDistanceMeters = distanceMeters)
        }
    }

    private fun arrivalGuidance(distanceMeters: Int) = RouteGuidanceSnapshot(
        instructions = listOf(
            ManeuverGuidance(
                type = ManeuverType.ARRIVE,
                distanceMeters = distanceMeters,
            ),
        ),
        remainingDistanceMeters = distanceMeters,
    )

    private fun outsideZoneArrivalGuidance() = RouteGuidanceSnapshot(
        instructions = listOf(
            ManeuverGuidance(
                type = ManeuverType.ARRIVE,
                distanceMeters = 0,
            ),
        ),
        remainingDistanceMeters = 0,
    )

    private fun onArrivalConfirmed() {
        countdownJob?.cancel()
        countdownJob = null
        vibrateArrivalPattern()
    }

    /**
     * Ketika pengguna sudah jauh meninggalkan jalur, arahan "kembali ke jalur" tidak lagi masuk
     * akal — jaraknya bisa berkilometer. Rute dibaca ulang dari simpang terdekat dengan posisi
     * sekarang. Ini murni pembacaan basis data prakomputasi, bukan pencarian lintasan.
     *
     * Pengalihan hanya dilakukan setelah beberapa pembaruan posisi berturut-turut sepakat, agar
     * lompatan GPS sesaat tidak mengganti rute yang sedang diikuti.
     */
    private fun maybeRecalculateRouteForNewPosition(deviceLocation: DeviceLocation) {
        val state = mutableUiState.value
        val shouldSkip = state.hasArrived ||
            state.route == null ||
            state.directOrientation != null ||
            state.isLoadingRoute ||
            rerouteJob?.isActive == true
        if (shouldSkip) {
            offRouteTracker.reset()
            return
        }
        val distanceFromRoute = state.guidance?.distanceFromRouteMeters ?: return
        val shouldRecalculate = offRouteTracker.shouldRecalculate(
            distanceFromRouteMeters = distanceFromRoute,
            accuracyMeters = deviceLocation.accuracyMeters,
        )
        if (shouldRecalculate) recalculateRouteFromCurrentPosition(deviceLocation.coordinate)
    }

    private fun recalculateRouteFromCurrentPosition(location: GeoCoordinate) {
        rerouteJob = viewModelScope.launch {
            val currentRoute = mutableUiState.value.route ?: return@launch
            val route = runCatching {
                val nearest = repository.findRouteFromLocation(location)
                if (nearest.destinationName in rejectedDestinationNames) {
                    // Tujuan yang sudah ditolak pengguna tidak ditawarkan lagi.
                    repository.findAlternativeRoute(
                        location = location,
                        currentRoute = currentRoute,
                        excludedDestinationNames = rejectedDestinationNames,
                    )
                } else {
                    nearest
                }
            }.getOrNull() ?: return@launch

            if (route.originNodeId == currentRoute.originNodeId &&
                route.destinationName == currentRoute.destinationName
            ) {
                return@launch
            }
            arrivalTracker.reset()
            resetRouteProgress()
            mutableUiState.update { state ->
                if (state.hasArrived) return@update state
                withGuidance(
                    state.copy(
                        route = route,
                        isLoadingRoute = false,
                        alternativeRouteMessage = "Rute disesuaikan dengan posisi Anda sekarang.",
                        alternativeRouteVersion = state.alternativeRouteVersion + 1,
                        errorMessage = null,
                    ),
                )
            }
        }
    }

    private fun resetRouteProgress() {
        minimumRouteIndex = 0
        minimumRouteSegmentFraction = 0.0
        announcedManeuverIndex = null
    }

    private fun maybeVibrateUpcomingManeuver() {
        if (!app.settingsRepository.vibrateBeforeTurn) return
        val instruction = mutableUiState.value.guidance?.currentInstruction ?: return
        if (
            instruction.type == ManeuverType.STRAIGHT ||
            instruction.type == ManeuverType.ARRIVE ||
            instruction.distanceMeters > MANEUVER_ALERT_DISTANCE_METERS ||
            instruction.routeCoordinateIndex == announcedManeuverIndex
        ) {
            return
        }
        announcedManeuverIndex = instruction.routeCoordinateIndex
        vibratePattern(longArrayOf(0L, 120L))
    }

    private fun buildAlternativeRouteMessage(
        previousRoute: EvacuationRoute,
        newRoute: EvacuationRoute,
    ): String {
        val differenceSeconds = newRoute.estimatedSeconds - previousRoute.estimatedSeconds
        val comparison = when {
            abs(differenceSeconds) < 30 -> "waktu hampir sama"
            differenceSeconds > 0 ->
                "+${ceil(differenceSeconds / 60.0).toInt()} menit"
            else ->
                "${ceil(abs(differenceSeconds) / 60.0).toInt()} menit lebih cepat"
        }
        return "Rute baru ke ${newRoute.destinationName} · $comparison"
    }

    @Suppress("DEPRECATION")
    private fun vibrateArrivalPattern() {
        vibratePattern(longArrayOf(0L, 180L, 100L, 260L))
    }

    @Suppress("DEPRECATION")
    private fun vibratePattern(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            app.getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    @SuppressLint("LogNotTimber")
    private fun logRouteTiming(elapsedMillis: Long) {
        // Pencatatan ini menjadi bukti pengukuran NF-02 pada perangkat uji.
        Log.i(LOG_TAG, "Arahan siap dalam $elapsedMillis ms")
    }

    companion object {
        private const val LOG_TAG = "EvacuationTiming"
        private const val UNRESOLVED_DATASET_VERSION_ID = 0
        private const val MANEUVER_ALERT_DISTANCE_METERS = 30
        private const val MIN_ZONE_CHECK_MOVEMENT_METERS = 12.0
        private const val MAX_ZONE_CHECK_INTERVAL_MILLIS = 1_500L
        private const val MAX_ZONE_ACCURACY_METERS = 35f
        private const val REQUIRED_ZONE_TRANSITION_CONFIRMATIONS = 2
        private const val ROAD_VIEWPORT_RELOAD_METERS = 500.0
        private const val ROAD_VIEWPORT_DEBOUNCE_MILLIS = 120L
        const val MAX_CHECKIN_ACCURACY_METERS = 35f
    }
}
