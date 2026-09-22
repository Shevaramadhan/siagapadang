package com.akusukaproject.siagapadang.ui.evacuation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.heightIn
import com.akusukaproject.siagapadang.ui.theme.SiagaTailGray
import com.akusukaproject.siagapadang.ui.theme.SiagaRustDeep
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.akusukaproject.siagapadang.data.remote.model.OccupancyStatusResponseDto
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.data.model.BmkgStatus
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
import com.akusukaproject.siagapadang.domain.EarthquakeRelevance
import com.akusukaproject.siagapadang.domain.EarthquakeAgeFormatter
import com.akusukaproject.siagapadang.domain.ManeuverGuidance
import com.akusukaproject.siagapadang.domain.ManeuverType
import com.akusukaproject.siagapadang.domain.BearingCalculator
import com.akusukaproject.siagapadang.domain.DirectOrientation
import com.akusukaproject.siagapadang.domain.RemainingRouteCalculator
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot
import com.akusukaproject.siagapadang.ui.map.OfflineMap
import com.akusukaproject.siagapadang.ui.theme.SiagaCream
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaCalmBackground
import com.akusukaproject.siagapadang.ui.theme.SiagaSafeGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaOnNavyMuted
import com.akusukaproject.siagapadang.ui.theme.SiagaNextGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaRust
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EvacuationScreen(
    viewModel: EvacuationViewModel = viewModel(),
    showArrivalEvidence: Boolean = false,
    evidenceDestinationName: String = "TES tujuan",
    evidenceDestinationCapacity: Int? = null,
    onOpenMenu: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        viewModel.onLocationPermissionChanged(
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }

    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.refreshFamilyMeetingPoint()
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onLocationPermissionChanged(true)
        } else {
            requestLocationPermission()
        }
    }

    EvacuationContent(
        state = state,
        showArrivalEvidence = showArrivalEvidence,
        evidenceDestinationName = evidenceDestinationName,
        evidenceDestinationCapacity = evidenceDestinationCapacity,
        onRequestLocationPermission = ::requestLocationPermission,
        onRetryRoute = viewModel::retryRoute,
        onSelectPreviousRoute = viewModel::selectPreviousRoute,
        onRecheckInitialZone = viewModel::recheckInitialZone,
        onDismissInitialZoneCheckMessage = viewModel::dismissInitialZoneCheckMessage,
        onRefreshBmkgStatus = viewModel::refreshBmkgStatus,
        onCheckDataUpdates = viewModel::checkDataUpdates,
        onInstallDataUpdate = viewModel::installDataUpdate,
        onReportObstacle = viewModel::reportEvacuationObstacle,
        onMapViewportChanged = viewModel::onMapViewportChanged,
        onPerformCheckin = viewModel::requestCheckinConfirmation,
        onConfirmCheckin = viewModel::performShelterCheckin,
        onDismissCheckinConfirmation = viewModel::dismissCheckinConfirmation,
        onDismissObstructionMessage = viewModel::dismissObstructionMessage,
        onReportOccupancy = viewModel::reportShelterOccupancy,
        onOpenMenu = onOpenMenu,
    )
}

@Composable
private fun EvacuationContent(
    state: EvacuationUiState,
    showArrivalEvidence: Boolean,
    evidenceDestinationName: String,
    evidenceDestinationCapacity: Int?,
    onRequestLocationPermission: () -> Unit,
    onRetryRoute: () -> Unit,
    onSelectPreviousRoute: (EvacuationRoute) -> Unit,
    onRecheckInitialZone: () -> Unit,
    onDismissInitialZoneCheckMessage: () -> Unit,
    onRefreshBmkgStatus: () -> Unit,
    onCheckDataUpdates: () -> Unit,
    onInstallDataUpdate: () -> Unit,
    onReportObstacle: (EvacuationObstacleType) -> Unit,
    onMapViewportChanged: (GeoCoordinate) -> Unit,
    onPerformCheckin: () -> Unit,
    onConfirmCheckin: () -> Unit = {},
    onDismissCheckinConfirmation: () -> Unit = {},
    onDismissObstructionMessage: () -> Unit = {},
    onReportOccupancy: (String) -> Unit = {},
    onOpenMenu: () -> Unit = {},
) {
    var showBlockedRouteDialog by rememberSaveable { mutableStateOf(false) }
    var showArrivalDialog by rememberSaveable(showArrivalEvidence) {
        mutableStateOf(showArrivalEvidence)
    }
    var selectedStatusDetail by rememberSaveable { mutableStateOf<StatusDetailType?>(null) }
    var collapsedContentHeightPx by remember { mutableIntStateOf(0) }
    var acknowledgedBmkgAlertId by rememberSaveable { mutableStateOf<String?>(null) }
    val mapPanelState = remember { AnchoredDraggableState(MapPanelValue.COLLAPSED) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(state.hasArrived, showArrivalEvidence) {
        if (state.hasArrived || showArrivalEvidence) showArrivalDialog = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SiagaNavy)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val scale = (maxWidth.value / FIGMA_WIDTH_DP).coerceIn(0.82f, 1.25f)
        fun scaled(value: Float): Dp = (value * scale).dp

        val collapsedMapHeight = if (collapsedContentHeightPx > 0) {
            val contentBottom = TOP_BAR_SPACE + with(density) { collapsedContentHeightPx.toDp() } + MAP_HANDLE_SPACE
            (maxHeight - contentBottom).coerceIn(MIN_COLLAPSED_MAP_HEIGHT, maxHeight * 0.6f)
        } else {
            scaled(FIGMA_MAP_HEIGHT_DP).coerceAtMost(maxHeight * 0.40f)
        }
        val expandedMapHeight = maxHeight
        val dragRangePx = with(density) {
            (expandedMapHeight - collapsedMapHeight).toPx().coerceAtLeast(1f)
        }
        LaunchedEffect(dragRangePx) {
            mapPanelState.updateAnchors(
                DraggableAnchors {
                    MapPanelValue.COLLAPSED at 0f
                    MapPanelValue.EXPANDED at -dragRangePx
                },
            )
        }
        val panelOffset = mapPanelState.offset.takeUnless(Float::isNaN) ?: 0f
        val expansionProgress = (-panelOffset / dragRangePx).coerceIn(0f, 1f)
        val mapHeight = lerp(collapsedMapHeight, expandedMapHeight, expansionProgress)
        val collapsedContentAlpha = (1f - expansionProgress * 1.7f).coerceIn(0f, 1f)

        BackHandler(enabled = expansionProgress > 0.01f) {
            coroutineScope.launch {
                mapPanelState.animateTo(
                    targetValue = MapPanelValue.COLLAPSED,
                    animationSpec = MAP_PANEL_SPRING,
                )
            }
        }

        EvacuationMapPanel(
            state = state,
            mapHeight = mapHeight,
            scale = scale,
            expansionProgress = expansionProgress,
            onBlockedRouteClick = {
                selectedStatusDetail = null
                showBlockedRouteDialog = true
            },
            onRecheckInitialZone = onRecheckInitialZone,
            onSelectPreviousRoute = onSelectPreviousRoute,
            onMapViewportChanged = onMapViewportChanged,
            modifier = Modifier.align(Alignment.BottomCenter),
            onExpandMap = {
                coroutineScope.launch { mapPanelState.animateTo(MapPanelValue.EXPANDED, MAP_PANEL_SPRING) }
            },
            onCollapseMap = {
                coroutineScope.launch { mapPanelState.animateTo(MapPanelValue.COLLAPSED, MAP_PANEL_SPRING) }
            },
        )

        // Tombol berdiri tepat di garis batas antara kartu dan peta pada kedua mode: setengah
        // badannya di kartu, setengah lagi di peta. Kotak gagangnya 48 dp, jadi titik tengahnya
        // berada 24 dp di bawah tepi atas kotak.
        val handleTop = maxHeight - mapHeight +
            lerp((-24f * scale).dp, EXPANDED_HEADER_HEIGHT - 24.dp, expansionProgress)
        MapOpenHandle(
            expansionProgress = expansionProgress,
            dragState = mapPanelState,
            onClick = {
                coroutineScope.launch {
                    val target = if (expansionProgress >= 0.5f) {
                        MapPanelValue.COLLAPSED
                    } else {
                        MapPanelValue.EXPANDED
                    }
                    mapPanelState.animateTo(target, MAP_PANEL_SPRING)
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = handleTop)
                .zIndex(20f),
        )

        // Kabar BMKG dianggap sudah dibaca setelah kartunya dibuka.
        var seenBmkgEventKey by rememberSaveable { mutableStateOf<String?>(null) }
        val bmkgEventKey = bmkgEventKey(state)
        val bmkgIsUnread = bmkgEventKey != null &&
            bmkgEventKey != seenBmkgEventKey &&
            !bmkgEventIsFarAway(state)
        LaunchedEffect(selectedStatusDetail, bmkgEventKey) {
            if (selectedStatusDetail == StatusDetailType.BMKG) seenBmkgEventKey = bmkgEventKey
        }
        if (expansionProgress < 0.5f) {
            EvacuationTopBar(
                state = state,
                selected = selectedStatusDetail,
                onSelect = { detail ->
                    selectedStatusDetail = if (selectedStatusDetail == detail) null else detail
                },
                onOpenMenu = {
                    selectedStatusDetail = null
                    onOpenMenu()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .zIndex(30f),
                bmkgIsUnread = bmkgIsUnread,
            )
        } else {
            StatusColumnV3(
                state = state,
                selected = selectedStatusDetail,
                onSelect = { detail ->
                    selectedStatusDetail = if (selectedStatusDetail == detail) null else detail
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = EXPANDED_HEADER_HEIGHT + 12.dp)
                    .graphicsLayer(alpha = expansionProgress)
                    .zIndex(30f),
                bmkgIsUnread = bmkgIsUnread,
            )
        }
        // Detail terakhir dipertahankan selama animasi keluar agar isi popup tidak hilang mendadak.
        var lastStatusDetail by remember { mutableStateOf(StatusDetailType.GPS) }
        LaunchedEffect(selectedStatusDetail) { selectedStatusDetail?.let { lastStatusDetail = it } }
        val isExpandedLayout = expansionProgress >= 0.5f
        val statusIndex = StatusDetailType.entries.indexOf(selectedStatusDetail ?: lastStatusDetail)
        AnimatedVisibility(
            visible = selectedStatusDetail != null,
            enter = fadeIn(tween(UI_ANIMATION_MILLIS)) + scaleIn(
                initialScale = 0.9f,
                transformOrigin = if (isExpandedLayout) TransformOrigin(0f, 0f) else TransformOrigin(1f, 0f),
                animationSpec = tween(UI_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
            ),
            exit = fadeOut(tween(UI_ANIMATION_MILLIS / 2)) + scaleOut(
                targetScale = 0.95f,
                transformOrigin = if (isExpandedLayout) TransformOrigin(0f, 0f) else TransformOrigin(1f, 0f),
            ),
            modifier = if (isExpandedLayout) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 72.dp, top = EXPANDED_HEADER_HEIGHT + 12.dp + (statusIndex * 56).dp)
                    .zIndex(31f)
            } else {
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 66.dp)
                    .zIndex(31f)
            },
        ) {
            StatusDetailCard(
                detail = selectedStatusDetail ?: lastStatusDetail,
                state = state,
                onDismiss = { selectedStatusDetail = null },
                onRefreshBmkgStatus = onRefreshBmkgStatus,
                scale = scale,
                // Jarak penunjuk dari tepi kanan kartu ke tengah ikon (ikon 48 dp, jarak 6 dp, tepi 16 dp).
                caretEndOffset = if (isExpandedLayout) null else (40 + (2 - statusIndex) * 54 - 12 - 8).dp,
            )
        }

        val route = state.route
        if (state.isOutsideInundationZoneAtStart) {
            OutsideZoneStartState(
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TOP_BAR_SPACE, start = 16.dp, end = 16.dp)
                    .widthIn(max = 480.dp)
                    .onSizeChanged { collapsedContentHeightPx = it.height }
                    .graphicsLayer(
                        alpha = collapsedContentAlpha,
                        translationY = -expansionProgress * with(density) { scaled(45f).toPx() },
                    ),
            )
        } else if (route != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TOP_BAR_SPACE, start = 16.dp, end = 16.dp)
                    .widthIn(max = 480.dp)
                    .onSizeChanged { collapsedContentHeightPx = it.height }
                    .graphicsLayer(
                        alpha = collapsedContentAlpha,
                        translationY = -expansionProgress * with(density) { scaled(45f).toPx() },
                    ),
            ) {
                if (state.hasEvacuationWindowExpired) {
                    ExpiredEvacuationCardV3()
                } else if (state.directOrientation != null) {
                    DirectOrientationCard(
                        orientation = state.directOrientation,
                        deviceHeadingDegrees = state.deviceHeadingDegrees,
                        scale = scale,
                    )
                    DirectOrientationWarning(scale = scale)
                } else {
                    InstructionCardV3(route = route, guidance = state.guidance)
                    TimeCardV3(
                        route = route,
                        guidance = state.guidance,
                        remainingSeconds = state.remainingEvacuationSeconds,
                        compassMessage = state.compassMessage,
                    )
                }
            }
        } else {
            RoutePreparationState(
                state = state,
                onRequestLocationPermission = onRequestLocationPermission,
                onRetryRoute = onRetryRoute,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = scaled(75f)),
            )
        }

        state.obstructionReportMessage?.let { message ->
            LaunchedEffect(message) {
                delay(OBSTRUCTION_MESSAGE_VISIBLE_MILLIS)
                onDismissObstructionMessage()
            }
            ObstructionReportPopup(
                message = message,
                onDismiss = onDismissObstructionMessage,
            )
        }
    }

    state.initialZoneCheckMessage
        ?.takeIf { state.isOutsideInundationZoneAtStart && !state.isCheckingInitialZone }
        ?.let { message ->
            LaunchedEffect(message) {
                delay(ZONE_CHECK_RESULT_POPUP_MILLIS)
                onDismissInitialZoneCheckMessage()
            }
            ZoneRecheckResultPopup(
                message = message,
                onDismiss = onDismissInitialZoneCheckMessage,
            )
        }

    if (showBlockedRouteDialog) {
        ObstacleSheet(
            hasAlternativeRoute = state.remainingAlternativeCount > 0,
            onDismiss = { showBlockedRouteDialog = false },
            onSelect = { type ->
                showBlockedRouteDialog = false
                onReportObstacle(type)
            },
        )
    }

    if (state.showCheckinConfirmationDialog) {
        CheckinConfirmationDialog(
            destinationName = state.route?.destinationName ?: "TES",
            onConfirm = onConfirmCheckin,
            onDismiss = onDismissCheckinConfirmation,
        )
    }

    if (showArrivalDialog && (state.hasArrived || showArrivalEvidence)) {
        ArrivalDialog(
            arrivalReason = if (showArrivalEvidence) {
                EvacuationArrivalReason.EVACUATION_POINT
            } else {
                state.arrivalReason ?: EvacuationArrivalReason.EVACUATION_POINT
            },
            destinationName = if (showArrivalEvidence) {
                evidenceDestinationName
            } else {
                state.route?.destinationName ?: "TES"
            },
            destinationCapacityPeople = if (showArrivalEvidence) {
                evidenceDestinationCapacity
            } else {
                state.route?.destinationCapacityPeople
            },
            familyMeetingPointName = state.familyMeetingPointName,
            checkinStatus = state.checkinStatus,
            checkinMessage = state.checkinMessage,
            checkedInAt = state.checkedInAt,
            occupancyStatus = state.occupancyStatus,
            isReportingOccupancy = state.isReportingOccupancy,
            occupancyReportMessage = state.occupancyReportMessage,
            onPerformCheckin = onPerformCheckin,
            onReportOccupancy = onReportOccupancy,
            onAcknowledge = { showArrivalDialog = false },
        )
    }


    state.bmkgStatus
        ?.takeIf { it.hasTsunamiPotential && !it.isStale }
        // BMKG menerbitkan satu gempa terbaru untuk seluruh Indonesia. Gempa yang terlalu jauh
        // tidak memicu layar penuh, tetapi tetap terbaca pada kartu status beserta jaraknya.
        ?.takeIf { EarthquakeRelevance.shouldShowFullScreenAlert(it.epicenter, state.currentLocation) }
        ?.takeIf { it.fetchedAt != acknowledgedBmkgAlertId }
        ?.let { status ->
            BmkgTsunamiAlertDialog(
                status = status,
                onContinueEvacuation = { acknowledgedBmkgAlertId = status.fetchedAt },
            )
        }
}

@Composable
private fun DataUpdateShortcut(
    isChecking: Boolean,
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((11f * scale).dp),
        border = BorderStroke(1.dp, SiagaNextGreen),
        shadowElevation = 4.dp,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Buka pembaruan data" },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((6f * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = (11f * scale).dp,
                vertical = (8f * scale).dp,
            ),
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = SiagaNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size((14f * scale).dp),
                )
            } else {
                Text(
                    text = "↻",
                    fontSize = (16f * scale).sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = "DATA",
                fontSize = (10f * scale).sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun FamilyPlanShortcut(
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((11f * scale).dp),
        border = BorderStroke(1.dp, SiagaWarning),
        shadowElevation = 4.dp,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Buka rencana titik temu keluarga" },
    ) {
        Text(
            text = "KELUARGA",
            fontSize = (10f * scale).sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(
                horizontal = (11f * scale).dp,
                vertical = (10f * scale).dp,
            ),
        )
    }
}

@Composable
internal fun DataUpdateDialog(
    state: EvacuationUiState,
    onDismiss: () -> Unit,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    val local = state.localDatasetManifest
    val update = state.datasetUpdateStatus
    val statusColor = when {
        state.dataUpdateErrorMessage != null -> STATUS_ERROR_COLOR
        state.dataUpdateInstallMessage != null -> SiagaNextGreen
        update?.updateAvailable == true -> SiagaWarning
        update != null -> SiagaNextGreen
        else -> SiagaNavy.copy(alpha = 0.14f)
    }
    val statusTitle = when {
        state.isInstallingDataUpdate -> "Memasang pembaruan…"
        state.isCheckingDataUpdate -> "Memeriksa versi terbaru…"
        state.dataUpdateErrorMessage != null && update?.updateAvailable == true -> "Pembaruan gagal"
        state.dataUpdateErrorMessage != null -> "Pemeriksaan belum berhasil"
        state.dataUpdateInstallMessage != null -> "Pembaruan siap diaktifkan"
        update?.updateAvailable == true -> "Pembaruan tersedia"
        update != null -> "Data sudah terbaru"
        else -> "Belum diperiksa"
    }
    val statusMessage = when {
        state.isInstallingDataUpdate -> "Mengunduh, memeriksa checksum, dan memvalidasi database."
        state.isCheckingDataUpdate -> "Menghubungi backend SIAGA PADANG."
        state.dataUpdateErrorMessage != null -> state.dataUpdateErrorMessage
        state.dataUpdateInstallMessage != null -> state.dataUpdateInstallMessage
        update?.updateAvailable == true ->
            "Versi ${update.latestVersionLabel} tersedia. Data lokal tetap digunakan sampai pembaruan diterapkan."
        update != null -> "Versi perangkat sesuai dengan versi aktif di server."
        else -> "Periksa saat internet tersedia sebelum keadaan darurat."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = SiagaCream,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, SiagaNextGreen),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(end = 44.dp),
                    ) {
                        Text(
                            text = "Pembaruan Data",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Data evakuasi tersimpan di perangkat",
                            color = SiagaNavy.copy(alpha = 0.68f),
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(44.dp),
                    ) {
                        Text("×", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SiagaNavy.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(15.dp),
                    ) {
                        DatasetInfoRow("Versi di perangkat", local?.version ?: "Membaca data…")
                        DatasetInfoRow(
                            "Versi di server",
                            update?.latestVersionLabel ?: "Belum diperiksa",
                        )
                        DatasetInfoRow(
                            "Ukuran data lokal",
                            local?.sizeBytes?.let(::formatDatasetSize) ?: "—",
                        )
                        DatasetInfoRow(
                            "Terakhir diperiksa",
                            update?.checkedAtMillis?.let(::formatDatasetCheckTime) ?: "Belum pernah",
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, statusColor),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(14.dp),
                    ) {
                        Text(statusTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            statusMessage,
                            color = SiagaNavy.copy(alpha = 0.78f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }

                Button(
                    onClick = if (update?.downloadableVersion != null) {
                        onInstallUpdate
                    } else {
                        onCheckUpdates
                    },
                    enabled = !state.isCheckingDataUpdate &&
                        !state.isInstallingDataUpdate &&
                        state.dataUpdateInstallMessage == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SiagaNavy,
                        contentColor = SiagaCream,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.isCheckingDataUpdate || state.isInstallingDataUpdate) {
                        CircularProgressIndicator(
                            color = SiagaCream,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            when {
                                state.dataUpdateInstallMessage != null -> "Buka kembali aplikasi"
                                update?.downloadableVersion != null -> "Unduh dan pasang pembaruan"
                                else -> "Periksa pembaruan"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = "Jika unduhan atau pemeriksaan gagal, navigasi tetap memakai data lokal yang tersedia.",
                    color = SiagaNavy.copy(alpha = 0.62f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DatasetInfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = SiagaNavy.copy(alpha = 0.66f),
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = SiagaNavy,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatDatasetSize(sizeBytes: Long): String =
    String.format(Locale.forLanguageTag("id-ID"), "%.1f MB", sizeBytes / 1_000_000.0)

private fun formatDatasetCheckTime(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")).format(Date(timestamp))

@Composable
private fun BmkgTsunamiAlertDialog(
    status: BmkgStatus,
    onContinueEvacuation: () -> Unit,
) {
    val ageLabel = rememberBmkgAgeLabel(status)
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            color = STATUS_ERROR_COLOR,
            contentColor = SiagaNavy,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 56.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BmkgStatusIcon(color = SiagaNavy, modifier = Modifier.size(96.dp))
                    Text(
                        text = "PERINGATAN TSUNAMI BMKG",
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = status.potential.ifBlank { "BMKG menyatakan ada potensi tsunami." },
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = listOfNotNull(
                            ageLabel,
                            listOf(status.eventDate, status.eventTime)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                                .takeIf { it.isNotBlank() },
                            status.region.takeIf { it.isNotBlank() },
                        ).joinToString("\n"),
                        fontSize = 18.sp,
                        lineHeight = 25.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Evakuasi segera. Ikuti rute pada aplikasi dan arahan petugas di lapangan.",
                        fontSize = 18.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(
                    onClick = onContinueEvacuation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SiagaNavy,
                        contentColor = SiagaCream,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                ) {
                    Text("Lanjutkan evakuasi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatusIconRow(
    state: EvacuationUiState,
    selected: StatusDetailType?,
    onSelect: (StatusDetailType) -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val gpsColor = gpsStatusColor(state)
    val networkColor = networkStatusColor(state.isNetworkAvailable)
    val bmkgColor = bmkgStatusColor(state)
    Row(
        horizontalArrangement = Arrangement.spacedBy((1f * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        StatusIconButton(
            color = gpsColor,
            selected = selected == StatusDetailType.GPS,
            showProblemBadge = !state.hasLocationPermission ||
                state.locationQuality == LocationQuality.FAIR ||
                state.locationQuality == LocationQuality.WEAK,
            contentDescription = "Lihat status GPS",
            scale = scale,
            onClick = { onSelect(StatusDetailType.GPS) },
        ) {
            GpsStatusIcon(color = gpsColor, modifier = Modifier.fillMaxSize())
        }
        StatusIconButton(
            color = networkColor,
            selected = selected == StatusDetailType.NETWORK,
            showProblemBadge = state.isNetworkAvailable == false,
            contentDescription = "Lihat status jaringan",
            scale = scale,
            onClick = { onSelect(StatusDetailType.NETWORK) },
        ) {
            NetworkStatusIcon(
                color = networkColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
        StatusIconButton(
            color = bmkgColor,
            selected = selected == StatusDetailType.BMKG,
            showProblemBadge = state.bmkgErrorMessage != null ||
                state.bmkgStatus?.isStale == true ||
                state.bmkgStatus?.hasTsunamiPotential == true,
            contentDescription = "Lihat informasi BMKG",
            scale = scale,
            onClick = { onSelect(StatusDetailType.BMKG) },
        ) {
            BmkgStatusIcon(color = bmkgColor, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StatusIconColumn(
    state: EvacuationUiState,
    selected: StatusDetailType?,
    onSelect: (StatusDetailType) -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val gpsColor = gpsStatusColor(state)
    val networkColor = networkStatusColor(state.isNetworkAvailable)
    val bmkgColor = bmkgStatusColor(state)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((1f * scale).dp),
        modifier = modifier,
    ) {
        StatusIconButton(
            color = gpsColor,
            selected = selected == StatusDetailType.GPS,
            showProblemBadge = !state.hasLocationPermission ||
                state.locationQuality == LocationQuality.FAIR ||
                state.locationQuality == LocationQuality.WEAK,
            contentDescription = "Lihat status GPS",
            scale = scale,
            buttonSizeDp = 40f,
            onClick = { onSelect(StatusDetailType.GPS) },
        ) {
            GpsStatusIcon(color = gpsColor, modifier = Modifier.fillMaxSize())
        }
        StatusIconButton(
            color = networkColor,
            selected = selected == StatusDetailType.NETWORK,
            showProblemBadge = state.isNetworkAvailable == false,
            contentDescription = "Lihat status jaringan",
            scale = scale,
            buttonSizeDp = 40f,
            onClick = { onSelect(StatusDetailType.NETWORK) },
        ) {
            NetworkStatusIcon(color = networkColor, modifier = Modifier.fillMaxSize())
        }
        StatusIconButton(
            color = bmkgColor,
            selected = selected == StatusDetailType.BMKG,
            showProblemBadge = state.bmkgErrorMessage != null ||
                state.bmkgStatus?.isStale == true ||
                state.bmkgStatus?.hasTsunamiPotential == true,
            contentDescription = "Lihat informasi BMKG",
            scale = scale,
            buttonSizeDp = 40f,
            onClick = { onSelect(StatusDetailType.BMKG) },
        ) {
            BmkgStatusIcon(color = bmkgColor, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StatusIconButton(
    color: Color,
    selected: Boolean,
    showProblemBadge: Boolean,
    contentDescription: String,
    scale: Float,
    buttonSizeDp: Float = 48f,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size((buttonSizeDp * scale).dp)
            .clip(CircleShape)
            .background(if (selected) color.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(modifier = Modifier.size((29f * scale).dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size((23f * scale).dp),
            ) {
                content()
            }
            if (showProblemBadge) {
                Surface(
                    color = STATUS_ERROR_COLOR,
                    contentColor = SiagaNavy,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, SiagaNavy),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size((13f * scale).dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "!",
                            fontSize = (9f * scale).sp,
                            lineHeight = (9f * scale).sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GpsStatusIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.11f
        val center = Offset(size.width / 2f, size.height / 2f)
        val innerRadius = size.minDimension * 0.27f
        val tickStart = size.minDimension * 0.04f
        val tickEnd = size.minDimension * 0.25f
        drawCircle(color = color, radius = innerRadius, center = center, style = Stroke(strokeWidth))
        drawCircle(color = color, radius = size.minDimension * 0.09f, center = center)
        drawLine(color, Offset(center.x, tickStart), Offset(center.x, tickEnd), strokeWidth, StrokeCap.Round)
        drawLine(
            color,
            Offset(center.x, size.height - tickStart),
            Offset(center.x, size.height - tickEnd),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(color, Offset(tickStart, center.y), Offset(tickEnd, center.y), strokeWidth, StrokeCap.Round)
        drawLine(
            color,
            Offset(size.width - tickStart, center.y),
            Offset(size.width - tickEnd, center.y),
            strokeWidth,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun NetworkStatusIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.11f
        val arcStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.05f, size.height * 0.03f),
            size = Size(size.width * 0.90f, size.height * 0.90f),
            style = arcStyle,
        )
        drawArc(
            color = color,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.24f, size.height * 0.23f),
            size = Size(size.width * 0.52f, size.height * 0.52f),
            style = arcStyle,
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.09f,
            center = Offset(size.width / 2f, size.height * 0.79f),
        )
    }
}

@Composable
private fun BmkgStatusIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.10f
        val triangle = Path().apply {
            moveTo(size.width / 2f, size.height * 0.08f)
            lineTo(size.width * 0.92f, size.height * 0.86f)
            lineTo(size.width * 0.08f, size.height * 0.86f)
            close()
        }
        drawPath(triangle, color = color, style = Stroke(strokeWidth, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawLine(
            color = color,
            start = Offset(size.width / 2f, size.height * 0.36f),
            end = Offset(size.width / 2f, size.height * 0.61f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = strokeWidth * 0.55f,
            center = Offset(size.width / 2f, size.height * 0.73f),
        )
    }
}

/**
 * Isi kartu gempa disusun berjenjang: wilayah dan jarak dibaca lebih dulu karena itu yang
 * menentukan apakah kejadiannya menyangkut pengguna, potensi tsunami ditonjolkan tersendiri,
 * sedangkan waktu dan sumber menjadi catatan kaki.
 */
@Composable
private fun BmkgDetailBody(
    status: BmkgStatus,
    currentLocation: GeoCoordinate?,
) {
    val ageLabel = rememberBmkgAgeLabel(status)
    val distanceKm = EarthquakeRelevance.distanceKm(status.epicenter, currentLocation)
    val distanceLabel = EarthquakeRelevance.distanceLabel(
        distanceKm = distanceKm,
        usingUserLocation = currentLocation != null,
    )
    val isFarAway = distanceKm != null && distanceKm > EarthquakeRelevance.ALERT_RADIUS_KM
    if (isFarAway) {
        FarAwayBmkgBody(status = status, distanceLabel = distanceLabel)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.region.isNotBlank()) {
            Text(
                text = status.region,
                color = SiagaNavy,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        distanceLabel?.let { label ->
            BmkgFactRow(
                iconRes = R.drawable.ic_ms_location_on,
                text = label,
                emphasised = !isFarAway,
            )
        }
        if (status.hasTsunamiPotential) {
            BmkgHighlight(
                text = if (isFarAway) {
                    "BMKG menyebut potensi tsunami, tetapi jaraknya jauh dari Padang."
                } else {
                    "BMKG menyebut potensi tsunami."
                },
                background = if (isFarAway) Color(0xFFFBE3D9) else SiagaWarning,
            )
        } else if (status.potential.isNotBlank()) {
            BmkgFactRow(iconRes = R.drawable.ic_ms_info, text = status.potential, emphasised = false)
        }
        if (status.felt.isNotBlank()) {
            BmkgFactRow(
                iconRes = R.drawable.ic_ms_vibration,
                text = "Dirasakan " + status.felt,
                emphasised = false,
            )
        }
        val timestamp = listOf(status.eventDate, status.eventTime).filter { it.isNotBlank() }.joinToString(" ")
        Text(
            text = listOfNotNull(
                ageLabel,
                timestamp.takeIf { it.isNotBlank() },
                if (status.isStale) "Tersimpan, belum diperbarui" else null,
                "Sumber: BMKG",
            ).joinToString(" \u00b7 "),
            color = SiagaTextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Ringkasan satu baris untuk gempa di luar radius peringatan. Kejadiannya tetap disebut lengkap
 * dengan jaraknya supaya pengguna dapat memeriksa sendiri, tetapi tidak lagi memakan ruang
 * sebanyak kejadian yang benar-benar menyangkut Padang.
 */
@Composable
private fun FarAwayBmkgBody(
    status: BmkgStatus,
    distanceLabel: String?,
) {
    val ageLabel = rememberBmkgAgeLabel(status)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Gempa terbaru yang dilaporkan BMKG berada jauh dari Sumatera Barat.",
            color = SiagaNavy,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (status.hasTsunamiPotential) {
            // Pernyataan potensi tsunami tetap ditonjolkan berapa pun jaraknya; itu kewenangan BMKG.
            BmkgHighlight(
                text = "BMKG menyebut potensi tsunami pada kejadian ini, tetapi jaraknya jauh dari Padang.",
                background = Color(0xFFFBE3D9),
            )
        }
        Text(
            text = "Gempa terbaru BMKG: " + listOfNotNull(
                "M${status.magnitude}".takeIf { status.magnitude.isNotBlank() },
                status.region.takeIf { it.isNotBlank() },
                distanceLabel,
            ).joinToString(" · "),
            color = SiagaTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = listOfNotNull(
                ageLabel,
                listOf(status.eventDate, status.eventTime)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() },
                "Sumber: BMKG",
            ).joinToString(" · "),
            color = SiagaTextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun rememberBmkgAgeLabel(status: BmkgStatus): String? {
    var nowMillis by remember(status.isoDateTime, status.eventDate, status.eventTime) {
        mutableStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(status.isoDateTime, status.eventDate, status.eventTime) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    return remember(status.isoDateTime, status.eventDate, status.eventTime, nowMillis) {
        EarthquakeAgeFormatter.relativeAge(
            isoDateTime = status.isoDateTime,
            eventDate = status.eventDate,
            eventTime = status.eventTime,
            nowMillis = nowMillis,
        )
    }
}

@Composable
private fun BmkgFactRow(
    iconRes: Int,
    text: String,
    emphasised: Boolean,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            tint = if (emphasised) SiagaNavy else SiagaTextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = if (emphasised) SiagaNavy else SiagaTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun BmkgHighlight(
    text: String,
    background: Color,
) {
    Surface(
        color = background,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_ms_campaign),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = text, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusDetailCard(
    detail: StatusDetailType,
    state: EvacuationUiState,
    onDismiss: () -> Unit,
    onRefreshBmkgStatus: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
    caretEndOffset: Dp? = null,
) {
    val bmkgAgeLabel = if (state.bmkgStatus != null) {
        rememberBmkgAgeLabel(state.bmkgStatus)
    } else {
        null
    }
    val title: String
    val message: String
    val color: Color
    when (detail) {
        StatusDetailType.GPS -> {
            title = gpsStatusTitle(state)
            message = gpsStatusMessage(state)
            color = gpsStatusColor(state)
        }
        StatusDetailType.NETWORK -> {
            title = when (state.isNetworkAvailable) {
                true -> "Jaringan tersedia"
                false -> "Tanpa jaringan"
                null -> "Memeriksa jaringan"
            }
            message = when (state.isNetworkAvailable) {
                true -> "Perangkat terhubung. Navigasi dan data rute tetap diproses dari data luring."
                false -> "GPS, kompas, zona, dan rute evakuasi tetap dapat digunakan tanpa jaringan."
                null -> "Aplikasi sedang memeriksa koneksi perangkat."
            }
            color = networkStatusColor(state.isNetworkAvailable)
        }
        StatusDetailType.BMKG -> {
            val bmkg = state.bmkgStatus
            val bmkgIsFarAway = bmkgEventIsFarAway(state)
            title = when {
                state.isLoadingBmkgStatus -> "Memuat info gempa"
                bmkg?.isStale == true -> "Info gempa tersimpan"
                // Kabar yang paling berguna ketika gempanya jauh adalah bahwa tidak ada gempa
                // di dekat sini — bukan magnitudo kejadian di seberang Indonesia.
                bmkg != null && bmkgIsFarAway -> "Tidak ada gempa dekat Padang"
                bmkg?.magnitude?.isNotBlank() == true -> "Gempa M${bmkg.magnitude}"
                bmkg != null -> "Gempa terbaru BMKG"
                else -> "Info gempa belum tersedia"
            }
            message = when {
                state.isLoadingBmkgStatus -> "Mengambil data resmi BMKG."
                state.bmkgErrorMessage != null ->
                    "Info gempa belum dapat diperbarui. Rute evakuasi tetap aktif."
                bmkg != null -> {
                    val distanceKm = EarthquakeRelevance.distanceKm(bmkg.epicenter, state.currentLocation)
                    val isFarAway = distanceKm != null && distanceKm > EarthquakeRelevance.ALERT_RADIUS_KM
                    listOfNotNull(
                        bmkgAgeLabel,
                        bmkg.region.takeIf { it.isNotBlank() },
                        EarthquakeRelevance.distanceLabel(
                            distanceKm = distanceKm,
                            usingUserLocation = state.currentLocation != null,
                        ),
                        bmkg.potential.takeIf { it.contains("tsunami", ignoreCase = true) },
                        "Jaraknya jauh dari Padang, jadi tidak ditampilkan sebagai peringatan."
                            .takeIf { isFarAway && bmkg.hasTsunamiPotential },
                        listOf(bmkg.eventDate, bmkg.eventTime)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .takeIf { it.isNotBlank() },
                        "Sumber: BMKG",
                    ).joinToString("\n")
                }
                else -> "Hubungkan ke internet untuk memperbarui info gempa."
            }
            color = bmkgStatusColor(state)
        }
    }
    Column(horizontalAlignment = Alignment.End, modifier = modifier.width(280.dp)) {
        caretEndOffset?.let { offset ->
            Canvas(
                modifier = Modifier
                    .padding(end = offset)
                    .size(width = 16.dp, height = 8.dp),
            ) {
                drawPath(
                    Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        close()
                    },
                    color = Color.White,
                )
            }
        }
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 44.dp, bottom = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusTintOnLight(color)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    val bmkgStatus = state.bmkgStatus
                    if (detail == StatusDetailType.BMKG && bmkgStatus != null && state.bmkgErrorMessage == null) {
                        BmkgDetailBody(status = bmkgStatus, currentLocation = state.currentLocation)
                    } else {
                        Text(
                            text = message,
                            color = SiagaTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (detail == StatusDetailType.BMKG) {
                        // Tombol tindakan di kanan bawah, sejajar arah baca.
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = onRefreshBmkgStatus,
                                enabled = !state.isLoadingBmkgStatus,
                                border = BorderStroke(1.dp, SiagaLine),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                if (state.isLoadingBmkgStatus) {
                                    CircularProgressIndicator(color = SiagaNavy, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(
                                        painterResource(R.drawable.ic_ms_refresh),
                                        contentDescription = null,
                                        tint = SiagaNavy,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Perbarui", color = SiagaNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_ms_close),
                        contentDescription = "Tutup",
                        tint = SiagaTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationInstructionCard(
    route: EvacuationRoute,
    guidance: RouteGuidanceSnapshot?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val isApproachingRoute = guidance?.isApproachingRoute == true
    val displayedDestinationName = route.destinationName
    val instruction = guidance?.currentInstruction ?: ManeuverGuidance(
        type = ManeuverType.STRAIGHT,
        distanceMeters = estimatedDistanceMeters(route),
    )
    val presentation = maneuverPresentation(instruction.type)
    val instructionLabel = maneuverInstructionLabel(
        defaultLabel = presentation.label,
        type = instruction.type,
        isApproachingRoute = isApproachingRoute,
    )
    val shape = RoundedCornerShape((18f * scale).dp)
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = shape,
        border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
        modifier = modifier
            .size(width = (213f * scale).dp, height = (239f * scale).dp)
            .shadow(4.dp, shape),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = (10f * scale).dp),
        ) {
            Image(
                painter = painterResource(presentation.drawableRes),
                contentDescription = instructionLabel,
                colorFilter = if (presentation.tint) ColorFilter.tint(SiagaNavy) else null,
                modifier = Modifier
                    .size((126f * scale).dp)
                    .graphicsLayer(
                        rotationZ = presentation.assetRotationDegrees,
                    ),
            )
            Text(
                text = instructionLabel,
                color = SiagaNavy,
                fontSize = (24f * scale).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height((12f * scale).dp))
            Text(
                text = maneuverDistanceMessage(instruction, isApproachingRoute),
                color = SiagaNavy,
                fontSize = (16f * scale).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = displayedDestinationName,
                color = SiagaNavy,
                fontSize = (destinationCardFontSize(displayedDestinationName) * scale).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = ((destinationCardFontSize(displayedDestinationName) + 1f) * scale).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DirectOrientationCard(
    orientation: DirectOrientation,
    deviceHeadingDegrees: Float?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val arrowRotation = deviceHeadingDegrees?.let { heading ->
        BearingCalculator.relativeRotationDegrees(
            targetBearing = orientation.bearingDegrees,
            deviceHeading = heading.toDouble(),
        )
    } ?: orientation.bearingDegrees.toFloat()
    val shape = RoundedCornerShape((28f * scale).dp)
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = shape,
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, shape),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = (18f * scale).dp,
                    vertical = (18f * scale).dp,
                ),
            ) {
                Surface(
                    color = SiagaNavy,
                    contentColor = Color.White,
                    shape = RoundedCornerShape((22f * scale).dp),
                    modifier = Modifier.size((112f * scale).dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_figma_straight_arrow),
                            contentDescription =
                                "Arah lurus ${cardinalDirection(orientation.bearingDegrees)}",
                            tint = Color.White,
                            modifier = Modifier
                                .size((72f * scale).dp)
                                .graphicsLayer(rotationZ = arrowRotation),
                        )
                    }
                }
                Spacer(Modifier.width((16f * scale).dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = SiagaWarning,
                        contentColor = SiagaNavy,
                        shape = RoundedCornerShape((9f * scale).dp),
                    ) {
                        Text(
                            text = "ORIENTASI TERAKHIR",
                            fontSize = (10f * scale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(
                                horizontal = (8f * scale).dp,
                                vertical = (4f * scale).dp,
                            ),
                        )
                    }
                    Spacer(Modifier.height((8f * scale).dp))
                    Text(
                        text = "Ikuti arah ${cardinalDirection(orientation.bearingDegrees)}",
                        fontSize = (25f * scale).sp,
                        lineHeight = (29f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = "±${formatDistance(orientation.distanceMeters)} lurus",
                        color = SiagaTextSecondary,
                        fontSize = (15f * scale).sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SiagaTailGray)
                    .padding(horizontal = (18f * scale).dp, vertical = (12f * scale).dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ms_location_on),
                    contentDescription = null,
                    tint = SiagaNavy,
                    modifier = Modifier.size((22f * scale).dp),
                )
                Spacer(Modifier.width((8f * scale).dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TUJUAN ORIENTASI",
                        color = SiagaTextSecondary,
                        fontSize = (10f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                    )
                    Text(
                        text = orientation.destinationName,
                        fontSize = (15f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectOrientationWarning(
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((22f * scale).dp),
        border = BorderStroke(1.5.dp, SiagaRustDeep),
        shadowElevation = 5.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = (16f * scale).dp,
                vertical = (14f * scale).dp,
            ),
        ) {
            Surface(
                color = SiagaRustDeep.copy(alpha = 0.12f),
                contentColor = SiagaRustDeep,
                shape = CircleShape,
                modifier = Modifier.size((44f * scale).dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ms_warning),
                        contentDescription = null,
                        modifier = Modifier.size((25f * scale).dp),
                    )
                }
            }
            Spacer(Modifier.width((12f * scale).dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Arah darurat, bukan rute jalan",
                    color = SiagaRustDeep,
                    fontSize = (15f * scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Jauhi pantai. Ikuti petugas, rambu, dan kondisi jalan di sekitar Anda.",
                    color = SiagaTextSecondary,
                    fontSize = (12f * scale).sp,
                    lineHeight = (16f * scale).sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun EvacuationTiming(
    route: EvacuationRoute,
    remainingSeconds: Int,
    compassMessage: String?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = "Berjalan cepat ±${estimatedMinutes(route)} menit",
            color = SiagaCream,
            fontSize = (15f * scale).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height((20f * scale).dp))
        CountdownCard(
            remainingSeconds = remainingSeconds,
            scale = scale,
        )
        compassMessage?.let { message ->
            Spacer(modifier = Modifier.height((4f * scale).dp))
            Text(
                text = message,
                color = Color(0xFFFFD8A8),
                fontSize = (10f * scale).sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CountdownCard(remainingSeconds: Int, scale: Float) {
    Box(
        modifier = Modifier.size(width = (180f * scale).dp, height = (91f * scale).dp),
    ) {
        Surface(
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
            shape = RoundedCornerShape((9f * scale).dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (12f * scale).dp)
                .width((144f * scale).dp)
                .height((61f * scale).dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = formatDuration(remainingSeconds, spaced = true),
                    color = Color.White,
                    fontSize = (32f * scale).sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Surface(
            color = SiagaNavy,
            border = BorderStroke(1.dp, SiagaCream),
            shape = RoundedCornerShape((8f * scale).dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height((23f * scale).dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = (10f * scale).dp),
            ) {
                Text(
                    text = "Perkiraan sisa waktu",
                    color = SiagaCream,
                    fontSize = (9f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = "Dihitung sejak aplikasi dibuka",
            color = SiagaCream.copy(alpha = 0.78f),
            fontSize = (8f * scale).sp,
            lineHeight = (10f * scale).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun NextInstructionStrip(
    guidance: RouteGuidanceSnapshot?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val instructions = guidance?.instructions.orEmpty().ifEmpty {
        listOf(ManeuverGuidance(ManeuverType.STRAIGHT, 0))
    }.take(4)
    Box(
        modifier = modifier.size(width = (314f * scale).dp, height = (72f * scale).dp),
    ) {
        Surface(
            color = Color.Transparent,
            border = BorderStroke(1.dp, SiagaCream),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height((50f * scale).dp),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                repeat(MAX_VISIBLE_INSTRUCTIONS) { index ->
                    val instruction = instructions.getOrNull(index)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        instruction?.let { item ->
                            val presentation = maneuverPresentation(item.type)
                            MiniInstruction(
                                drawableRes = presentation.drawableRes,
                                label = if (item.type == ManeuverType.ARRIVE) {
                                    "TES"
                                } else {
                                    formatDistance(item.distanceMeters)
                                },
                                rotationDegrees = presentation.assetRotationDegrees,
                                tint = presentation.tint || item.type != ManeuverType.ARRIVE,
                                contentColor = if (index == 0) Color.White else SiagaNextGreen,
                                scale = scale,
                            )
                        }
                        if (index < MAX_VISIBLE_INSTRUCTIONS - 1 && instructions.getOrNull(index + 1) != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = (2f * scale).dp),
                            ) {
                                StepDot(scale, SiagaNextGreen)
                            }
                        }
                    }
                }
            }
        }
        Surface(
            color = SiagaNextGreen,
            border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
            shape = RoundedCornerShape(topStart = (12f * scale).dp, topEnd = (12f * scale).dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width((127f * scale).dp)
                .height((23f * scale).dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Berikutnya",
                    color = SiagaNavy,
                    fontSize = (12f * scale).sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun MiniInstruction(
    drawableRes: Int,
    label: String,
    scale: Float,
    rotationDegrees: Float = 0f,
    tint: Boolean = false,
    contentColor: Color = Color.White,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width((45f * scale).dp),
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            colorFilter = if (tint) ColorFilter.tint(contentColor) else null,
            modifier = Modifier
                .size((22f * scale).dp)
                .graphicsLayer(rotationZ = rotationDegrees),
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = (8f * scale).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StepDot(scale: Float, color: Color = Color.White) {
    Text(
        text = "•",
        color = color,
        fontSize = (12f * scale).sp,
    )
}

@Composable
private fun MapPanelHandle(
    scale: Float,
    expansionProgress: Float,
    dragState: AnchoredDraggableState<MapPanelValue>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .anchoredDraggable(
                state = dragState,
                orientation = Orientation.Vertical,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (expansionProgress > 0.5f) {
                    "Tarik ke bawah untuk mengecilkan peta"
                } else {
                    "Tarik ke atas untuk memperbesar peta"
                }
            },
    ) {
        Surface(
            color = SiagaNextGreen,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape((11f * scale).dp),
            border = BorderStroke(1.dp, SiagaNavy.copy(alpha = 0.72f)),
            shadowElevation = 7.dp,
            modifier = Modifier
                .width((74f * scale).dp)
                .height((26f * scale).dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = (8f * scale).dp,
                        vertical = (6f * scale).dp,
                    )
                    .graphicsLayer(rotationZ = expansionProgress * 180f),
            ) {
                val strokeWidth = (2f * scale).dp.toPx()
                val arrowTip = Offset(size.width / 2f, strokeWidth / 2f)
                val arrowBaseY = size.height - strokeWidth / 2f
                drawLine(
                    color = SiagaNavy,
                    start = Offset(strokeWidth / 2f, arrowBaseY),
                    end = arrowTip,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = SiagaNavy,
                    start = arrowTip,
                    end = Offset(size.width - strokeWidth / 2f, arrowBaseY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun EvacuationMapPanel(
    state: EvacuationUiState,
    mapHeight: Dp,
    scale: Float,
    expansionProgress: Float,
    onBlockedRouteClick: () -> Unit,
    onRecheckInitialZone: () -> Unit,
    onSelectPreviousRoute: (EvacuationRoute) -> Unit,
    onMapViewportChanged: (GeoCoordinate) -> Unit,
    modifier: Modifier = Modifier,
    onExpandMap: () -> Unit = {},
    onCollapseMap: () -> Unit = {},
) {
    var followUserLocation by rememberSaveable { mutableStateOf(true) }
    var recenterRequest by rememberSaveable { mutableIntStateOf(0) }
    // Tinggi tombol tindakan di bawah diukur agar tombol pusatkan berdiri di atasnya dengan
    // jarak tetap, berapa pun banyak baris teks yang dipakai tombol itu.
    var bottomActionHeightPx by remember { mutableIntStateOf(0) }
    var zoneLegendHeightPx by remember { mutableIntStateOf(0) }
    var isZoneLegendExpanded by rememberSaveable { mutableStateOf(false) }
    var routeOverviewRequest by rememberSaveable { mutableIntStateOf(0) }
    var routeChangeNotice by remember { mutableStateOf<String?>(null) }
    var zoneStatusNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.alternativeRouteVersion) {
        if (state.alternativeRouteVersion > 0) {
            followUserLocation = false
            routeOverviewRequest = state.alternativeRouteVersion
            routeChangeNotice = state.alternativeRouteMessage
            delay(ROUTE_CHANGE_NOTICE_MILLIS)
            routeChangeNotice = null
        }
    }
    LaunchedEffect(state.zoneTransitionVersion) {
        if (state.zoneTransitionVersion > 0) {
            zoneStatusNotice = state.zoneTransitionMessage
            delay(ZONE_STATUS_NOTICE_MILLIS)
            zoneStatusNotice = null
        }
    }
    val shape = RoundedCornerShape(
        topStart = (25f * scale * (1f - expansionProgress)).dp,
        topEnd = (25f * scale * (1f - expansionProgress)).dp,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
            .clip(shape),
    ) {
        val mapDensity = LocalDensity.current
        val controlBottomPadding = if (state.isOutsideInundationZoneAtStart) {
            28.dp
        } else if (bottomActionHeightPx > 0) {
            with(mapDensity) { bottomActionHeightPx.toDp() } + 24.dp
        } else {
            108.dp
        }
        val collapsedZoneHeightPx = with(mapDensity) { 48.dp.roundToPx() }
        val expandedZoneExtraHeight = if (
            isZoneLegendExpanded && zoneLegendHeightPx > collapsedZoneHeightPx
        ) {
            with(mapDensity) { (zoneLegendHeightPx - collapsedZoneHeightPx).toDp() }
        } else {
            0.dp
        }
        val routeHistoryBottomPadding = controlBottomPadding + 64.dp + expandedZoneExtraHeight
        val isApproachingRoute = state.guidance?.isApproachingRoute == true
        val nearestRouteCoordinate = state.guidance?.nearestRouteCoordinate
        val routeCoordinates = if (
            state.directOrientation == null && !state.hasEvacuationWindowExpired
        ) {
            state.route?.coordinates.orEmpty()
        } else {
            emptyList()
        }
        val remainingRouteCoordinates = remember(
            routeCoordinates,
            state.guidance?.nearestRouteIndex,
            nearestRouteCoordinate,
        ) {
            RemainingRouteCalculator.calculate(
                routeCoordinates = routeCoordinates,
                nearestRouteIndex = state.guidance?.nearestRouteIndex,
                nearestRouteCoordinate = nearestRouteCoordinate,
            )
        }
        OfflineMap(
            offlineRoadOverlay = state.offlineRoadOverlay,
            isNetworkAvailable = state.isNetworkAvailable,
            tsunamiZoneOverlay = state.tsunamiZoneOverlay,
            routeCoordinates = remainingRouteCoordinates,
            approachRouteCoordinates = if (
                state.directOrientation == null && !state.hasEvacuationWindowExpired &&
                isApproachingRoute && state.currentLocation != null && nearestRouteCoordinate != null
            ) {
                listOf(state.currentLocation, nearestRouteCoordinate)
            } else {
                emptyList()
            },
            approachTargetLocation = if (
                isApproachingRoute && !state.hasEvacuationWindowExpired
            ) nearestRouteCoordinate else null,
            previousRouteCoordinates = if (
                state.directOrientation == null && !state.hasEvacuationWindowExpired
            ) {
                state.previousRoutes.map { route -> route.coordinates }
            } else {
                emptyList()
            },
            currentLocation = state.currentLocation,
            destinationLocation = state.route?.destinationCoordinate
                ?.takeUnless { state.hasEvacuationWindowExpired },
            destinationName = state.route?.destinationName
                ?.takeUnless { state.hasEvacuationWindowExpired },
            destinationKindLabel = state.route?.destinationKind
                ?.takeUnless { state.hasEvacuationWindowExpired },
            destinationDurationLabel = (
                state.directOrientation?.distanceMeters?.takeUnless { state.hasEvacuationWindowExpired }
                    ?: state.guidance?.remainingDistanceMeters
                        ?.takeUnless { state.hasEvacuationWindowExpired }
                )?.let { meters -> formatWalkingDuration(meters) },
            destinationDistanceLabel = state.directOrientation?.distanceMeters
                ?.takeUnless { state.hasEvacuationWindowExpired }
                ?.let(::formatDistance)
                ?: state.guidance?.remainingDistanceMeters
                    ?.takeUnless { state.hasEvacuationWindowExpired }
                    ?.let(::formatDistance),
            deviceHeadingDegrees = state.deviceHeadingDegrees,
            followUserLocation = followUserLocation,
            recenterRequest = recenterRequest,
            routeOverviewRequest = routeOverviewRequest,
            onViewportChanged = onMapViewportChanged,
            onUserMapGesture = { followUserLocation = false },
            modifier = Modifier.fillMaxSize(),
            onMapDoubleTap = if (expansionProgress < 0.5f) onExpandMap else null,
        )

        if (state.tsunamiZoneOverlay != null) {
            if (expansionProgress < 0.5f) {
                CompactZoneStatusPill(
                    status = state.currentZoneStatus,
                    onClick = {
                        isZoneLegendExpanded = true
                        onExpandMap()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 16.dp,
                            bottom = if (state.isOutsideInundationZoneAtStart) {
                                84.dp
                            } else {
                                controlBottomPadding
                            },
                        )
                        .zIndex(10f),
                )
            } else {
                ZoneStatusPill(
                    status = state.currentZoneStatus,
                    expanded = isZoneLegendExpanded,
                    onExpandedChange = { isZoneLegendExpanded = it },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 16.dp,
                            end = 88.dp,
                            bottom = controlBottomPadding,
                        )
                        .onSizeChanged { zoneLegendHeightPx = it.height }
                        .zIndex(10f),
                )
            }
        }

        if (routeChangeNotice != null ||
            (state.previousRoutes.isNotEmpty() && expansionProgress >= 0.5f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 16.dp,
                        end = 88.dp,
                        bottom = routeHistoryBottomPadding,
                    )
                    .zIndex(9f),
            ) {
                if (routeChangeNotice != null) {
                    RouteChangeNotice(message = routeChangeNotice.orEmpty(), scale = scale)
                }
                // Daftar tujuan sebelumnya tetap terlihat walau pemberitahuan sedang tampil,
                // supaya pengguna masih bisa membaca nama tempat yang baru saja ditinggalkan.
                if (state.previousRoutes.isNotEmpty() && expansionProgress >= 0.5f) {
                    PreviousRoutesPill(
                        routes = state.previousRoutes,
                        onSelectRoute = onSelectPreviousRoute,
                    )
                }
            }
        }

        if (state.isOutsideInundationZoneAtStart) {
            ExpandedOutsideZoneHeader(
                modifier = Modifier.graphicsLayer(alpha = expansionProgress),
            )
        }

        if (state.hasEvacuationWindowExpired) {
            ExpiredMapHeader(
                scale = scale,
                modifier = Modifier
                    .graphicsLayer(alpha = expansionProgress)
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onCollapseMap() }) },
            )
        } else if (state.directOrientation != null) {
            ExpandedDirectOrientationHeader(
                orientation = state.directOrientation,
                deviceHeadingDegrees = state.deviceHeadingDegrees,
                modifier = Modifier
                    .graphicsLayer(alpha = expansionProgress)
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onCollapseMap() }) },
            )
        } else state.route?.let { route ->
            ExpandedMapHeader(
                route = route,
                guidance = state.guidance,
                remainingSeconds = state.remainingEvacuationSeconds,
                scale = scale,
                modifier = Modifier
                    .graphicsLayer(alpha = expansionProgress)
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onCollapseMap() }) },
            )

            VerticalInstructionStrip(
                guidance = state.guidance,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = EXPANDED_HEADER_HEIGHT + 84.dp)
                    .graphicsLayer(alpha = expansionProgress),
            )
        }

        NavigationCompass(
            headingDegrees = state.deviceHeadingDegrees ?: 0f,
            scale = scale,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = -lerp((12f * scale).dp, (14f * scale).dp, expansionProgress),
                    y = lerp((18f * scale).dp, EXPANDED_HEADER_HEIGHT + 12.dp, expansionProgress),
                )
                .size(lerp((60f * scale).dp, (60f * scale).dp, expansionProgress)),
        )

        zoneStatusNotice?.takeIf { expansionProgress >= 0.5f }?.let { message ->
            ZoneStatusNotice(
                message = message,
                status = state.currentZoneStatus,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = lerp(
                            (47f * scale).dp,
                            (184f * scale).dp,
                            expansionProgress,
                        ),
                    )
                    .zIndex(12f),
            )
        }

        if (state.currentLocation != null) {
            RecenterMapButton(
                isFollowing = followUserLocation,
                onClick = {
                    followUserLocation = true
                    recenterRequest++
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = controlBottomPadding,
                    )
                    .zIndex(9f),
            )
        }

        if (state.isOutsideInundationZoneAtStart) {
            RecheckPositionButton(
                isChecking = state.isCheckingInitialZone,
                onClick = onRecheckInitialZone,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .widthIn(min = 190.dp, max = 220.dp),
            )
        }

        if (!state.isOutsideInundationZoneAtStart) {
            ObstacleButton(
                enabled = state.canReportBlockedRoute,
                isLoading = state.isLoadingRoute,
                hasArrived = state.hasArrived,
                arrivalReason = state.arrivalReason,
                isDirectOrientationActive = state.directOrientation != null,
                isEvacuationWindowExpired = state.hasEvacuationWindowExpired,
                onClick = onBlockedRouteClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = (15f * scale).dp, vertical = (12f * scale).dp)
                    .onSizeChanged { bottomActionHeightPx = it.height },
            )
        }

    }
}

/** Warna isian dan tepi sama dengan lapisan zona di peta. */
private enum class ZoneLegendEntry(val label: String, val fill: Color, val mapOpacity: Float, val outline: Color, val outlineWidth: Float) {
    SAFE("Kawasan aman", ZONE_SAFE_COLOR, ZONE_SAFE_MAP_OPACITY, Color(0xFF00A152), 1.5f),
    LOW("Bahaya rendah", ZONE_LOW_COLOR, ZONE_LOW_MAP_OPACITY, Color(0xFFB38F00), 1.5f),
    MEDIUM("Bahaya sedang", ZONE_MEDIUM_COLOR, ZONE_MEDIUM_MAP_OPACITY, Color(0xFFE65100), 2.2f),
    HIGH("Bahaya tinggi", ZONE_HIGH_COLOR, ZONE_HIGH_MAP_OPACITY, Color(0xFFC62828), 3.2f),
}

@Composable
private fun ZoneSwatch(entry: ZoneLegendEntry, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(3.dp))
            .background(zoneLegendDisplayColor(entry.fill, entry.mapOpacity))
            .border(entry.outlineWidth.dp, entry.outline, RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun RouteChangeNotice(
    message: String,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SiagaNextGreen,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((10f * scale).dp),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Text(
            text = message,
            fontSize = (12f * scale).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            modifier = Modifier.padding(
                horizontal = (11f * scale).dp,
                vertical = (8f * scale).dp,
            ),
        )
    }
}

@Composable
private fun ZoneStatusNotice(
    message: String,
    status: InundationZoneStatus?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = zoneStatusColor(status)
    val contentColor = when (status) {
        is InundationZoneStatus.InsideRecordedZone -> Color.White
        InundationZoneStatus.OutsideRecordedZone -> SiagaNavy
        else -> SiagaNavy
    }
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape((14f * scale).dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
        shadowElevation = 6.dp,
        modifier = modifier.semantics { contentDescription = message },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((8f * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = (13f * scale).dp,
                vertical = (9f * scale).dp,
            ),
        ) {
            Surface(
                color = contentColor,
                shape = CircleShape,
                modifier = Modifier.size((8f * scale).dp),
            ) {}
            Text(
                text = message,
                fontSize = (12f * scale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ZoneStatusPill(
    status: InundationZoneStatus?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = zoneStatusAppearance(status)
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = if (expanded) RoundedCornerShape(18.dp) else CircleShape,
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 4.dp,
        modifier = modifier
            .clip(if (expanded) RoundedCornerShape(18.dp) else CircleShape)
            .clickable(role = Role.Button) { onExpandedChange(!expanded) }
            .animateContentSize(animationSpec = tween(UI_ANIMATION_MILLIS))
            .semantics {
                contentDescription = if (expanded) {
                    "Ciutkan keterangan warna zona"
                } else {
                    "${appearance.label}. Buka keterangan warna zona"
                }
            },
    ) {
        if (!expanded) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                Icon(
                    painterResource(R.drawable.ic_ms_warning),
                    contentDescription = null,
                    tint = appearance.dotColor,
                    modifier = Modifier.size(26.dp),
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(appearance.dotColor),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = appearance.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painterResource(R.drawable.ic_ms_expand_more),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "Warna pada peta",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SiagaTextSecondary,
                )
                ZoneLegendEntry.entries.forEach { legend -> ZoneLegendItem(legend) }
            }
        }
    }
}

/**
 * Status ringkas untuk peta kecil. Lebarnya dibatasi agar tidak mencapai penanda lokasi yang
 * dipusatkan pada peta; ketukan membuka peta besar dan legenda lengkap.
 */
@Composable
private fun CompactZoneStatusPill(
    status: InundationZoneStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = zoneStatusAppearance(status)
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CircleShape,
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 4.dp,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "${appearance.label}. Perbesar peta untuk melihat keterangan zona"
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_ms_warning),
                contentDescription = null,
                tint = appearance.dotColor,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

private class ZoneStatusAppearance(val label: String, val dotColor: Color)

/**
 * Label mengikuti nilai `tingkat_bahaya` pada basis data: Rendah, Sedang, Tinggi. Posisi yang
 * hanya diketahui berisiko lewat penanda simpul tidak diberi tingkat, karena tingkatnya memang
 * tidak diketahui.
 */
private fun zoneStatusAppearance(status: InundationZoneStatus?): ZoneStatusAppearance = when (status) {
    null, InundationZoneStatus.DataUnavailable ->
        ZoneStatusAppearance("Status zona belum dipastikan", SiagaTailGray)
    InundationZoneStatus.OutsideRecordedZone ->
        ZoneStatusAppearance("Di luar zona rendaman", ZoneLegendEntry.SAFE.outline)
    is InundationZoneStatus.InsideRecordedZone -> when (status.dangerLevel.trim().lowercase()) {
        "tinggi" -> ZoneStatusAppearance("Zona bahaya tinggi", ZoneLegendEntry.HIGH.outline)
        "sedang" -> ZoneStatusAppearance("Zona bahaya sedang", ZoneLegendEntry.MEDIUM.outline)
        "rendah" -> ZoneStatusAppearance("Zona bahaya rendah", ZoneLegendEntry.LOW.outline)
        else -> ZoneStatusAppearance("Di dalam zona risiko tsunami", SiagaRustDeep)
    }
}

@Composable
private fun ZoneLegendItem(entry: ZoneLegendEntry) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ZoneSwatch(entry, Modifier.size(width = 22.dp, height = 16.dp))
        Spacer(Modifier.width(8.dp))
        Text(entry.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PreviousRoutesPill(
    routes: List<EvacuationRoute>,
    onSelectRoute: (EvacuationRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp,
        modifier = modifier.width(if (expanded) 208.dp else 144.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(
                horizontal = if (expanded) 12.dp else 8.dp,
                vertical = if (expanded) 8.dp else 0.dp,
            ),
        ) {
            if (expanded) {
                Text(
                    text = "Ketuk tujuan untuk menggunakannya kembali. Rute saat ini akan tetap tersedia.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SiagaTextSecondary,
                )
                routes.forEachIndexed { index, route ->
                    PreviousRouteItem(
                        order = if (index == 0) "Rute terakhir" else "Rute sebelumnya",
                        route = route,
                        onClick = { onSelectRoute(route) },
                    )
                }
            }
            // Judul diletakkan terakhir agar tetap menempel pada posisi bawah saat daftar dibuka.
            // Sasaran sentuh tidak berpindah, sehingga ketukan kedua selalu menutup daftar.
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .semantics {
                        contentDescription = if (expanded) {
                            "Ciutkan daftar rute sebelumnya"
                        } else {
                            "Buka daftar ${routes.size} rute sebelumnya"
                        }
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PreviousRouteDash()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Rute lalu (${routes.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painterResource(
                            if (expanded) R.drawable.ic_ms_expand_more else R.drawable.ic_ms_expand_less,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Satu baris tujuan yang ditinggalkan dan dapat dipilih kembali. */
@Composable
private fun PreviousRouteItem(
    order: String,
    route: EvacuationRoute,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(max = 208.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 6.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_ms_route),
            contentDescription = null,
            tint = SiagaTailGray,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FacilityKindBadge(kind = route.destinationKind)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = route.destinationName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "$order · ±${estimatedMinutes(route)} menit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SiagaTextSecondary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            painterResource(R.drawable.ic_ms_arrow_forward),
            contentDescription = "Pilih ${route.destinationName}",
            tint = SiagaNavy,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Penanda jenis fasilitas. TES adalah gedung bertingkat, TEA kawasan perbukitan; keduanya
 * dibedakan agar pengguna tahu tempat seperti apa yang dituju.
 */
@Composable
private fun FacilityKindBadge(kind: String?) {
    val label = kind?.uppercase() ?: return
    Surface(
        color = if (label == "TEA") SiagaSafeGreen else SiagaWarning,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PreviousRouteDash() {
    Box(
        modifier = Modifier
            .size(width = 22.dp, height = 4.dp)
            .clip(CircleShape)
            .background(SiagaTailGray),
    )
}

@Composable
private fun NavigationCompass(
    headingDegrees: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    // Rotasi dianimasikan melalui sudut terpendek agar jarum tidak berputar penuh saat melewati 0°.
    var displayed by remember { mutableFloatStateOf(-headingDegrees) }
    val target = -headingDegrees
    val delta = ((target - displayed) % 360f + 540f) % 360f - 180f
    val rotation by animateFloatAsState(
        targetValue = displayed + delta,
        animationSpec = tween(250),
        label = "kompas",
    )
    LaunchedEffect(target) { displayed += delta }
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CircleShape,
        shadowElevation = 6.dp,
        modifier = modifier.semantics {
            contentDescription = "Kompas, arah utara ${headingDegrees.toInt()} derajat"
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer(rotationZ = rotation)) {
                CompassDial(scale = scale)
                Text(
                    "U",
                    color = SiagaRust,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun CompassDial(scale: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        repeat(12) { index ->
            val angle = Math.toRadians(index * 30.0 - 90.0)
            val isCardinal = index % 3 == 0
            if (index == 0) return@repeat
            val outer = radius * 0.92f
            val inner = radius * if (isCardinal) 0.76f else 0.83f
            drawLine(
                color = SiagaNavy.copy(alpha = if (isCardinal) 0.8f else 0.35f),
                start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                strokeWidth = (if (isCardinal) 2f else 1.2f).dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val needleHalfWidth = radius * 0.14f
        val north = Path().apply {
            moveTo(center.x, center.y - radius * 0.56f)
            lineTo(center.x - needleHalfWidth, center.y)
            lineTo(center.x + needleHalfWidth, center.y)
            close()
        }
        val south = Path().apply {
            moveTo(center.x, center.y + radius * 0.56f)
            lineTo(center.x - needleHalfWidth, center.y)
            lineTo(center.x + needleHalfWidth, center.y)
            close()
        }
        drawPath(north, color = SiagaRust)
        drawPath(south, color = SiagaNavy.copy(alpha = 0.85f))
        drawCircle(color = Color.White, radius = radius * 0.09f, center = center)
        drawCircle(color = SiagaNavy, radius = radius * 0.09f, center = center, style = Stroke(1.5.dp.toPx()))
    }
}

/**
 * Kartu ringkas di mode peta besar ketika posisi awal berada di luar zona rendaman. Isinya sama
 * dengan kartu pada mode peta kecil, dipadatkan agar peta tetap lega — sama seperti perlakuan
 * kartu arah pada mode evakuasi.
 */
@Composable
private fun ExpandedOutsideZoneHeader(
    modifier: Modifier = Modifier,
) {
    Surface(
        color = WidgetSafeBlue,
        contentColor = Color.White,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(EXPANDED_HEADER_HEIGHT),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.figma_widget_safe_bg),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-12).dp)
                    .width(193.dp)
                    .height(EXPANDED_HEADER_HEIGHT),
            )
            Image(
                painter = painterResource(R.drawable.figma_widget_safe_character),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 24.dp)
                    .width(233.dp)
                    .height(EXPANDED_HEADER_HEIGHT),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.72f)
                    .padding(start = 16.dp, top = 28.dp),
            ) {
                Text(
                    text = "Anda berada di luar zona rendaman",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Berdasarkan data zona yang tersimpan di HP.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
@Composable
private fun RecheckPositionButton(
    isChecking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, SiagaNavy),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, enabled = !isChecking, onClick = onClick)
            .semantics {
                contentDescription = if (isChecking) {
                    "Sedang memeriksa posisi"
                } else {
                    "Periksa posisi lagi"
                }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = SiagaNavy,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Memeriksa...", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            } else {
                Icon(
                    painterResource(R.drawable.ic_ms_my_location),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Periksa posisi lagi",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
        }
    }
}
@Composable
private fun ExpandedDirectOrientationHeader(
    orientation: DirectOrientation,
    deviceHeadingDegrees: Float?,
    modifier: Modifier = Modifier,
) {
    val arrowRotation = deviceHeadingDegrees?.let { heading ->
        BearingCalculator.relativeRotationDegrees(
            targetBearing = orientation.bearingDegrees,
            deviceHeading = heading.toDouble(),
        )
    } ?: orientation.bearingDegrees.toFloat()
    Surface(
        color = SiagaNavy,
        contentColor = Color.White,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(EXPANDED_HEADER_HEIGHT),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 86.dp, top = 14.dp, bottom = 34.dp),
        ) {
            Surface(
                color = SiagaWarning,
                contentColor = SiagaNavy,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.size(84.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_figma_straight_arrow),
                        contentDescription =
                            "Arah lurus ${cardinalDirection(orientation.bearingDegrees)}",
                        modifier = Modifier
                            .size(50.dp)
                            .graphicsLayer(rotationZ = arrowRotation),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Orientasi terakhir",
                    color = SiagaWarning,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                )
                Text(
                    text = "Arah ${cardinalDirection(orientation.bearingDegrees)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "±${formatDistance(orientation.distanceMeters)} lurus",
                    color = SiagaOnNavyMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = orientation.destinationName,
                    color = SiagaOnNavyMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExpandedMapHeader(
    route: EvacuationRoute,
    guidance: RouteGuidanceSnapshot?,
    remainingSeconds: Int,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val isApproachingRoute = guidance?.isApproachingRoute == true
    val instruction = guidance?.currentInstruction ?: ManeuverGuidance(
        ManeuverType.STRAIGHT,
        estimatedDistanceMeters(route),
    )
    val presentation = maneuverPresentation(instruction.type)
    val label = maneuverInstructionLabel(presentation.label, instruction.type, isApproachingRoute)
    Surface(
        color = SiagaNavy,
        contentColor = Color.White,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(EXPANDED_HEADER_HEIGHT),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 34.dp),
        ) {
            Surface(
                color = Color.White,
                contentColor = SiagaNavy,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.size(84.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painterResource(presentation.drawableRes),
                        contentDescription = label,
                        modifier = Modifier.size(46.dp),
                    )
                    Text(
                        text = if (instruction.type == ManeuverType.ARRIVE) "TES" else formatDistance(instruction.distanceMeters),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(
                    text = "Menuju ${route.destinationName}",
                    color = SiagaOnNavyMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = SiagaWarning,
                contentColor = SiagaNavy,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("SISA", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(formatDuration(remainingSeconds), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ExpiredMapHeader(
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SiagaRustDeep,
        contentColor = Color.White,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(EXPANDED_HEADER_HEIGHT),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 86.dp, top = 14.dp, bottom = 34.dp),
        ) {
            Surface(
                color = Color.White,
                contentColor = SiagaRustDeep,
                shape = CircleShape,
                modifier = Modifier.size((66f * scale).dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ms_warning),
                        contentDescription = null,
                        modifier = Modifier.size((38f * scale).dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Waktu evakuasi habis",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Lakukan evakuasi vertikal. Gunakan tangga dan naik ke lantai paling atas.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CompactManeuverCard(
    instruction: ManeuverGuidance,
    isApproachingRoute: Boolean,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val presentation = maneuverPresentation(instruction.type)
    val instructionLabel = maneuverInstructionLabel(
        defaultLabel = presentation.label,
        type = instruction.type,
        isApproachingRoute = isApproachingRoute,
    )
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((18f * scale).dp),
        border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
        modifier = modifier.size((92f * scale).dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(presentation.drawableRes),
                contentDescription = instructionLabel,
                colorFilter = if (presentation.tint) ColorFilter.tint(SiagaNavy) else null,
                modifier = Modifier
                    .size((53f * scale).dp)
                    .graphicsLayer(
                        rotationZ = presentation.assetRotationDegrees,
                    ),
            )
            Text(
                text = instructionLabel,
                color = SiagaNavy,
                fontSize = (11f * scale).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun VerticalInstructionStrip(
    guidance: RouteGuidanceSnapshot?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    // Langkah saat ini sudah tampil besar di header, jadi strip hanya berisi langkah sesudahnya.
    val nextSteps = guidance?.instructions.orEmpty().drop(1).take(MAX_VISIBLE_INSTRUCTIONS)
    if (nextSteps.isEmpty()) return
    Surface(
        color = SiagaNavy,
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        modifier = modifier.width(58.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 12.dp),
        ) {
            Text("Lalu", color = SiagaOnNavyMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            nextSteps.forEach { step ->
                val presentation = maneuverPresentation(step.type)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(presentation.drawableRes),
                        contentDescription = presentation.label,
                        tint = SiagaNextGreen,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = if (step.type == ManeuverType.ARRIVE) "TES" else formatDistance(step.distanceMeters),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecenterMapButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Saat peta masih mengikuti posisi, tombol ini tidak ada gunanya ditekan, jadi tampil tenang.
    // Begitu pengguna menggeser peta, tombol berubah jadi navy pekat — perbedaannya terbaca
    // sekilas tanpa harus membandingkan dua keadaan berdampingan.
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        // Gelembung keterangan hanya muncul saat peta tidak lagi mengikuti posisi, supaya
        // pengguna tahu apa yang akan terjadi sebelum menekan tombolnya.
        AnimatedVisibility(
            visible = !isFollowing,
            enter = fadeIn(tween(UI_ANIMATION_MILLIS)) + scaleIn(
                initialScale = 0.9f,
                transformOrigin = TransformOrigin(1f, 1f),
                animationSpec = tween(UI_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
            ),
            exit = fadeOut(tween(UI_ANIMATION_MILLIS / 2)),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = SiagaNavy,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        text = "Ke posisi Anda",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Canvas(
                    modifier = Modifier
                        .padding(end = 18.dp)
                        .size(width = 14.dp, height = 7.dp),
                ) {
                    drawPath(
                        Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        },
                        color = SiagaNavy,
                    )
                }
            }
        }
        Surface(
            color = if (isFollowing) Color.White else SiagaNavy,
            contentColor = if (isFollowing) SiagaTextSecondary else Color.White,
            shape = CircleShape,
            border = if (isFollowing) BorderStroke(1.dp, SiagaLine) else null,
            shadowElevation = if (isFollowing) 4.dp else 8.dp,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = if (isFollowing) {
                        "Peta mengikuti posisi Anda"
                    } else {
                        "Peta tidak lagi terpusat. Pusatkan ke posisi Anda"
                    }
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(if (isFollowing) R.drawable.ic_ms_my_location else R.drawable.ic_ms_navigation),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun BlockedRouteButton(
    enabled: Boolean,
    isLoading: Boolean,
    hasArrived: Boolean,
    arrivalReason: EvacuationArrivalReason?,
    isDirectOrientationActive: Boolean,
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val label = when {
        hasArrived && arrivalReason == EvacuationArrivalReason.OUTSIDE_INUNDATION_ZONE ->
            "Di luar zona rendaman"
        hasArrived -> "Anda telah sampai di TES"
        isDirectOrientationActive -> "Orientasi terakhir aktif"
        isLoading -> "Mencari alternatif tujuan…"
        enabled -> "Jalur terhalang?"
        else -> "Alternatif tujuan tidak tersedia"
    }
    Surface(
        color = when {
            hasArrived -> SiagaNextGreen
            enabled && !isLoading -> SiagaWarning
            else -> Color(0xFFD5D7A5)
        },
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((11f * scale).dp),
        modifier = modifier
            .fillMaxWidth()
            .height((56f * scale).dp)
            .clickable(
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = SiagaNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size((22f * scale).dp),
                )
                Spacer(modifier = Modifier.width((10f * scale).dp))
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_figma_warning),
                    contentDescription = null,
                    modifier = Modifier.size((22f * scale).dp),
                )
                Spacer(modifier = Modifier.width((10f * scale).dp))
            }
            Text(
                text = label,
                color = SiagaNavy,
                fontSize = (20f * scale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RoutePreparationState(
    state: EvacuationUiState,
    onRequestLocationPermission: () -> Unit,
    onRetryRoute: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val title: String
    val detail: String
    when {
        !state.hasLocationPermission -> {
            title = "Izinkan akses lokasi"
            detail = "Lokasi diperlukan untuk menentukan arah evakuasi dan diproses di perangkat."
        }
        state.errorMessage != null -> {
            title = "Arahan belum tersedia"
            detail = state.errorMessage
        }
        state.currentLocation == null -> {
            title = "Mencari lokasi…"
            detail = "Pastikan GPS perangkat aktif. Arahan tetap disiapkan tanpa jaringan."
        }
        state.isCheckingInitialZone -> {
            title = if (state.initialZoneCheckMessage == null) {
                "Memeriksa zona…"
            } else {
                "Menunggu GPS lebih akurat…"
            }
            detail = state.initialZoneCheckMessage
                ?: "Posisi awal diperiksa dari data zona yang tersimpan di perangkat."
        }
        else -> {
            title = "Menyiapkan arahan…"
            detail = "Rute sedang dibaca dari data luring."
        }
    }

    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((18f * scale).dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = (32f * scale).dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((14f * scale).dp),
            modifier = Modifier.padding((24f * scale).dp),
        ) {
            if (state.isLoadingRoute || state.isCheckingInitialZone ||
                state.currentLocation == null && state.hasLocationPermission
            ) {
                CircularProgressIndicator(color = SiagaNavy)
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_figma_destination),
                    contentDescription = null,
                    modifier = Modifier.size((48f * scale).dp),
                )
            }
            Text(
                text = title,
                fontSize = (24f * scale).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = detail,
                fontSize = (14f * scale).sp,
                textAlign = TextAlign.Center,
            )
            when {
                !state.hasLocationPermission -> ActionButton(
                    text = "Izinkan",
                    onClick = onRequestLocationPermission,
                )
                state.errorMessage != null -> ActionButton(
                    text = "Coba lagi",
                    onClick = onRetryRoute,
                )
            }
        }
    }
}

@Composable
private fun OutsideZoneStartState(
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = WidgetSafeBlue,
        contentColor = Color.White,
        shape = RoundedCornerShape((22f * scale).dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height((238f * scale).dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape((22f * scale).dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.figma_widget_safe_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomStart,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-16).dp)
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight(),
            )
            Image(
                painter = painterResource(R.drawable.figma_widget_safe_character),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomEnd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp)
                    .fillMaxWidth(0.72f)
                    .fillMaxHeight(),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy((5f * scale).dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.76f)
                    .padding(start = (18f * scale).dp, top = (18f * scale).dp),
            ) {
                Text(
                    text = "Anda berada di luar zona rendaman",
                    fontSize = (22f * scale).sp,
                    lineHeight = (25f * scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Berdasarkan data zona yang tersimpan di HP.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = (12f * scale).sp,
                    lineHeight = (15f * scale).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                color = SiagaNavy,
                contentColor = Color.White,
                shape = RoundedCornerShape((14f * scale).dp),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = (16f * scale).dp, bottom = (16f * scale).dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = (12f * scale).dp,
                        vertical = (9f * scale).dp,
                    ),
                ) {
                    Image(
                        painter = painterResource(R.drawable.figma_widget_safe_button_icon),
                        contentDescription = null,
                        modifier = Modifier.size((19f * scale).dp),
                    )
                    Spacer(Modifier.width((7f * scale).dp))
                    Text(
                        text = "Jauhi pantai dan sungai",
                        fontSize = (12f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}


@Composable
private fun ObstructionReportPopup(
    message: String,
    onDismiss: () -> Unit,
) {
    val normalized = message.lowercase()
    val presentation = when {
        "ditolak" in normalized || "berbeda" in normalized -> ObstructionPopupPresentation(
            title = "Laporan belum diterima",
            iconRes = R.drawable.ic_ms_warning,
            accent = SiagaRustDeep,
        )
        "offline" in normalized || "koneksi" in normalized || "server" in normalized -> ObstructionPopupPresentation(
            title = "Laporan disimpan",
            iconRes = R.drawable.ic_ms_wifi_off,
            accent = SiagaWarning,
        )
        "diproses" in normalized -> ObstructionPopupPresentation(
            title = "Laporan sedang diproses",
            iconRes = R.drawable.ic_ms_sync,
            accent = SiagaNavy,
        )
        else -> ObstructionPopupPresentation(
            title = "Laporan diterima",
            iconRes = R.drawable.ic_ms_check_circle,
            accent = SiagaSafeGreen,
        )
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 330.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
            ) {
                Surface(
                    color = presentation.accent.copy(alpha = 0.14f),
                    contentColor = presentation.accent,
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(presentation.iconRes),
                            contentDescription = null,
                            tint = presentation.accent,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Text(
                    text = presentation.title,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    color = SiagaNavy.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SiagaNavy,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Mengerti", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class ObstructionPopupPresentation(
    val title: String,
    val iconRes: Int,
    val accent: Color,
)

@Composable
private fun ZoneRecheckResultPopup(
    message: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 330.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
            ) {
                Surface(
                    color = ZONE_SAFE_COLOR.copy(alpha = 0.14f),
                    contentColor = ZONE_SAFE_COLOR,
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ms_location_on),
                            contentDescription = null,
                            tint = ZONE_SAFE_COLOR,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Text(
                    text = "Hasil pemeriksaan posisi",
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    color = SiagaNavy.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SiagaNavy,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Tutup",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = SiagaNavy,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BlockedRouteDialog(
    hasAlternativeRoute: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.White,
            contentColor = Color.Black,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(if (hasAlternativeRoute) 250.dp else 290.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_figma_close),
                        contentDescription = "Tutup",
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp, start = 24.dp, end = 24.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_figma_dialog_warning),
                        contentDescription = null,
                        modifier = Modifier.size(68.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (hasAlternativeRoute) {
                            "Laporkan jalur terhalang?"
                        } else {
                            "Rute terakhir terhalang?"
                        },
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasAlternativeRoute) {
                            "Sistem akan memilih rute offline yang paling cepat menjauh dari jalur ini."
                        } else {
                            "Tidak ada rute jalan lain yang dapat diverifikasi. Sistem hanya akan menampilkan arah dan jarak lurus sebagai orientasi terakhir."
                        },
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color(0xFF7F7F7F)),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = "Batal",
                            color = Color(0xFF7F7F7F),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A4D7A),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = if (hasAlternativeRoute) {
                                "Ya, cari alternatif"
                            } else {
                                "Tampilkan orientasi"
                            },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyMeetingPointReminder(meetingPointName: String) {
    Surface(
        color = SiagaCalmBackground,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SiagaLine),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Titik temu keluarga",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SiagaNavy.copy(alpha = 0.78f),
            )
            Text(
                text = meetingPointName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Temui keluarga di sana setelah petugas menyatakan aman. Jangan kembali untuk menjemput.",
                fontSize = 12.sp,
                color = SiagaNavy.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CheckinConfirmationDialog(
    destinationName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_ms_where_to_vote),
                contentDescription = null,
                tint = SiagaNavy,
                modifier = Modifier.size(36.dp),
            )
        },
        title = {
            Text(
                text = "Lapor Tiba & Selamat?",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Data lokasi dan kehadiran Anda di \"$destinationName\" akan dikirimkan ke posko BPBD " +
                    "agar tercatat selamat. Pastikan Anda sudah benar-benar berada di tempat evakuasi.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SiagaNavy,
                    contentColor = SiagaCream,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(text = "Ya, Lapor Selamat", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, SiagaNavy.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(text = "Batal", color = SiagaNavy)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun ArrivalDialog(
    arrivalReason: EvacuationArrivalReason = EvacuationArrivalReason.EVACUATION_POINT,
    destinationName: String,
    destinationCapacityPeople: Int?,
    familyMeetingPointName: String? = null,
    checkinStatus: CheckinStatus = CheckinStatus.IDLE,
    checkinMessage: String? = null,
    checkedInAt: String? = null,
    occupancyStatus: OccupancyStatusResponseDto? = null,
    isReportingOccupancy: Boolean = false,
    occupancyReportMessage: String? = null,
    onPerformCheckin: () -> Unit = {},
    onReportOccupancy: (String) -> Unit = {},
    onAcknowledge: () -> Unit,
) {
    val arrivedOutsideZone = arrivalReason == EvacuationArrivalReason.OUTSIDE_INUNDATION_ZONE
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Surface(
                    color = Color(0xFFDDF1E5),
                    shape = CircleShape,
                    modifier = Modifier.size(76.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ms_where_to_vote),
                            contentDescription = null,
                            tint = SiagaSafeGreen,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (arrivedOutsideZone) {
                        "Anda berada di luar zona rendaman"
                    } else {
                        "Anda sudah sampai di TES"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                if (!arrivedOutsideZone) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = destinationName,
                        color = SiagaRust,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    destinationCapacityPeople?.takeIf { capacity -> capacity > 0 }?.let { capacity ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Kapasitas rancangan BPBD: ${formatPeople(capacity)} orang. " +
                                "Bukan data keterisian langsung.",
                            color = SiagaNavy.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    color = SiagaNavy,
                    contentColor = SiagaCream,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (arrivedOutsideZone) {
                            "Tetap menjauh dari arah pantai dan jangan kembali ke zona rendaman. " +
                                "Ikuti arahan petugas atau rambu evakuasi di lapangan."
                        } else {
                            "Tetap berada di TES dan tunggu arahan RT/RW selama 30 menit. " +
                                "Jangan kembali ke zona pantai."
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                familyMeetingPointName?.let { meetingPoint ->
                    Spacer(modifier = Modifier.height(10.dp))
                    FamilyMeetingPointReminder(meetingPointName = meetingPoint)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (arrivedOutsideZone) {
                        "Navigasi dihentikan setelah perpindahan keluar zona dikonfirmasi oleh beberapa pembacaan GPS."
                    } else {
                        "Navigasi dan hitung mundur telah dihentikan."
                    },
                    color = SiagaNavy.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                if (!arrivedOutsideZone) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                    color = when (checkinStatus) {
                        CheckinStatus.SUCCESS -> SiagaNextGreen.copy(alpha = 0.15f)
                        CheckinStatus.FAILED -> SiagaRust.copy(alpha = 0.12f)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        when (checkinStatus) {
                            CheckinStatus.SUCCESS -> SiagaNextGreen
                            CheckinStatus.FAILED -> SiagaRust
                            else -> SiagaNavy.copy(alpha = 0.2f)
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (checkinStatus) {
                            CheckinStatus.IDLE -> {
                                Text(
                                    text = "Lapor Kehadiran ke Posko Bencana",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SiagaNavy,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Jika ada koneksi seluler/internet, laporkan kehadiran agar terdata selamat oleh BPBD.",
                                    fontSize = 11.sp,
                                    color = SiagaNavy.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onPerformCheckin,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SiagaNavy,
                                        contentColor = SiagaCream,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                ) {
                                    Text(
                                        text = "Lapor Tiba & Selamat (Check-in)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            CheckinStatus.CHECKING_IN -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = SiagaNavy,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Menghubungi posko bencana...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SiagaNavy,
                                    )
                                }
                            }
                            CheckinStatus.SUCCESS -> {
                                Text(
                                    text = "✅ Terdata Selamat di Posko",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F5132),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = checkinMessage ?: "Kehadiran Anda berhasil dicatat di sistem posko.",
                                    fontSize = 12.sp,
                                    color = SiagaNavy.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                )
                                checkedInAt?.let { timestamp ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Waktu: $timestamp",
                                        fontSize = 11.sp,
                                        color = SiagaNavy.copy(alpha = 0.65f),
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(SiagaNavy.copy(alpha = 0.15f)),
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Status Kepadatan Tempat Evakuasi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SiagaNavy,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val levelText = when (occupancyStatus?.level) {
                                    "LOW" -> "🟢 Sepi / Masih Banyak Tempat"
                                    "MODERATE" -> "🟡 Mulai Padat"
                                    "FULL" -> "🔴 Penuh"
                                    else -> "⚪ Belum ada data laporan warga"
                                }
                                Text(
                                    text = levelText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (occupancyStatus?.level) {
                                        "LOW" -> Color(0xFF0F5132)
                                        "MODERATE" -> Color(0xFF856404)
                                        "FULL" -> SiagaRust
                                        else -> SiagaNavy.copy(alpha = 0.7f)
                                    },
                                )
                                Text(
                                    text = if (occupancyStatus != null && occupancyStatus.reportCount > 0) {
                                        "Sumber: ${occupancyStatus.source} (${occupancyStatus.reportCount} laporan)"
                                    } else {
                                        "Sumber: Belum ada laporan warga dalam 30 menit terakhir"
                                    },
                                    fontSize = 10.sp,
                                    color = SiagaNavy.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Bagikan kondisi keterisian terkini:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SiagaNavy.copy(alpha = 0.85f),
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (isReportingOccupancy) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = SiagaNavy,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Mengirim status...",
                                            fontSize = 11.sp,
                                            color = SiagaNavy,
                                        )
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Button(
                                            onClick = { onReportOccupancy("LOW") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD4EDDA),
                                                contentColor = Color(0xFF155724),
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        ) {
                                            Text(text = "Sepi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { onReportOccupancy("MODERATE") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFF3CD),
                                                contentColor = Color(0xFF856404),
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        ) {
                                            Text(text = "Sedang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { onReportOccupancy("FULL") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFF8D7DA),
                                                contentColor = Color(0xFF721C24),
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        ) {
                                            Text(text = "Penuh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                occupancyReportMessage?.let { msg ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg,
                                        fontSize = 10.sp,
                                        color = SiagaNavy.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            CheckinStatus.FAILED -> {
                                Text(
                                    text = "⚠️ Belum Berhasil Terhubung ke Posko",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SiagaRust,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = checkinMessage ?: "Gagal mengirim data keselamatan ke server posko.",
                                    fontSize = 11.sp,
                                    color = SiagaNavy.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onPerformCheckin,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SiagaRust,
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                ) {
                                    Text(
                                        text = "Coba Kirim Ulang",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAcknowledge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SiagaWarning,
                        contentColor = SiagaNavy,
                    ),
                    border = BorderStroke(2.dp, SiagaNavy),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        text = "Saya mengerti",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

internal data class DirectionPresentation(
    val label: String,
    val drawableRes: Int,
    val assetRotationDegrees: Float = 0f,
    val tint: Boolean = false,
)

internal fun maneuverPresentation(type: ManeuverType): DirectionPresentation = when (type) {
        ManeuverType.STRAIGHT -> DirectionPresentation("Lurus", R.drawable.ic_ms_straight, tint = true)
        ManeuverType.U_TURN -> DirectionPresentation("Putar balik", R.drawable.ic_ms_u_turn_left, tint = true)
        ManeuverType.SLIGHT_RIGHT -> DirectionPresentation("Sedikit ke kanan", R.drawable.ic_ms_turn_slight_right, tint = true)
        ManeuverType.RIGHT -> DirectionPresentation("Belok kanan", R.drawable.ic_ms_turn_right, tint = true)
        ManeuverType.SHARP_RIGHT -> DirectionPresentation("Belok tajam kanan", R.drawable.ic_ms_turn_sharp_right, tint = true)
        ManeuverType.SLIGHT_LEFT -> DirectionPresentation("Sedikit ke kiri", R.drawable.ic_ms_turn_slight_left, tint = true)
        ManeuverType.LEFT -> DirectionPresentation("Belok kiri", R.drawable.ic_ms_turn_left, tint = true)
        ManeuverType.SHARP_LEFT -> DirectionPresentation("Belok tajam kiri", R.drawable.ic_ms_turn_sharp_left, tint = true)
        ManeuverType.ARRIVE -> DirectionPresentation("Tiba di TES", R.drawable.ic_ms_flag, tint = true)
    }

private fun estimatedMinutes(route: EvacuationRoute): Int =
    ceil(route.estimatedSeconds / 60.0).toInt().coerceAtLeast(1)

internal fun estimatedDistanceMeters(route: EvacuationRoute): Int =
    (route.estimatedSeconds * WALKING_SPEED_METERS_PER_SECOND).toInt().coerceAtLeast(0)

/** Lama berjalan cepat pada 1,2 m/s sesuai SOP BPBD Kota Padang. */
internal fun formatWalkingDuration(distanceMeters: Int): String {
    val minutes = ceil(distanceMeters / WALKING_SPEED_METERS_PER_SECOND / 60.0).toInt().coerceAtLeast(1)
    return "±$minutes mnt"
}

internal fun formatDistance(distanceMeters: Int): String = when {
    distanceMeters < 1_000 -> "${(distanceMeters / 10) * 10} m"
    else -> "%.1f km".format(distanceMeters / 1_000.0)
}

private fun cardinalDirection(bearingDegrees: Double): String {
    val directions = listOf(
        "Utara",
        "Timur Laut",
        "Timur",
        "Tenggara",
        "Selatan",
        "Barat Daya",
        "Barat",
        "Barat Laut",
    )
    val normalizedBearing = (bearingDegrees % 360.0 + 360.0) % 360.0
    val index = ((normalizedBearing + 22.5) / 45.0).toInt() % directions.size
    return "${directions[index]} (${normalizedBearing.toInt()}°)"
}

internal fun maneuverDistanceMessage(
    instruction: ManeuverGuidance,
    isApproachingRoute: Boolean = false,
): String = when {
    isApproachingRoute && instruction.distanceMeters <= ROAD_ENTRY_REACHED_DISTANCE_METERS ->
        "Jalan di depan"
    isApproachingRoute -> "${formatDistance(instruction.distanceMeters)} ke jalan"
    instruction.type == ManeuverType.ARRIVE -> "Tujuan di depan"
    instruction.distanceMeters <= MANEUVER_NOW_DISTANCE_METERS &&
        instruction.type == ManeuverType.STRAIGHT -> "Lanjut lurus"
    instruction.distanceMeters <= MANEUVER_NOW_DISTANCE_METERS -> "Belok sekarang"
    else -> "${formatDistance(instruction.distanceMeters)} lagi"
}

internal fun maneuverInstructionLabel(
    defaultLabel: String,
    type: ManeuverType,
    isApproachingRoute: Boolean,
): String {
    if (!isApproachingRoute) return defaultLabel
    return when (type) {
        ManeuverType.STRAIGHT -> "Ke depan"
        ManeuverType.SLIGHT_LEFT, ManeuverType.LEFT, ManeuverType.SHARP_LEFT -> "Ke kiri"
        ManeuverType.SLIGHT_RIGHT, ManeuverType.RIGHT, ManeuverType.SHARP_RIGHT -> "Ke kanan"
        ManeuverType.U_TURN -> "Putar balik"
        ManeuverType.ARRIVE -> "Jalan tercapai"
    }
}

internal fun formatDuration(totalSeconds: Int, spaced: Boolean = false): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    val separator = if (spaced) " : " else ":"
    return "%02d%s%02d".format(minutes, separator, seconds)
}

private fun formatPeople(value: Int): String = String.format("%,d", value).replace(',', '.')

private fun destinationCardFontSize(destinationName: String): Float = when {
    destinationName.length <= 18 -> 20f
    destinationName.length <= 26 -> 17f
    destinationName.length <= 34 -> 15f
    else -> 13f
}

private fun expandedDestinationFontSize(destinationName: String): Float = when {
    destinationName.length <= 16 -> 20f
    destinationName.length <= 24 -> 17f
    destinationName.length <= 34 -> 14f
    else -> 12f
}

internal fun gpsStatusColor(state: EvacuationUiState): Color = when {
    !state.hasLocationPermission -> STATUS_ERROR_COLOR
    state.locationQuality == LocationQuality.GOOD -> SiagaNextGreen
    state.locationQuality == LocationQuality.FAIR -> STATUS_CAUTION_COLOR
    state.locationQuality == LocationQuality.WEAK -> STATUS_ERROR_COLOR
    else -> STATUS_UNKNOWN_COLOR
}

private fun gpsStatusTitle(state: EvacuationUiState): String = when {
    !state.hasLocationPermission -> "GPS tidak aktif"
    state.locationQuality == LocationQuality.GOOD -> "GPS akurat"
    state.locationQuality == LocationQuality.FAIR -> "GPS cukup akurat"
    state.locationQuality == LocationQuality.WEAK -> "Sinyal GPS lemah"
    else -> "Mencari lokasi"
}

private fun gpsStatusMessage(state: EvacuationUiState): String {
    if (!state.hasLocationPermission) {
        return "Izin lokasi diperlukan agar posisi dan arahan evakuasi dapat ditentukan."
    }
    val accuracyMessage = when (state.locationQuality) {
        LocationQuality.GOOD -> "Perkiraan akurasi ±${state.locationAccuracyMeters?.toInt() ?: 0} meter."
        LocationQuality.FAIR ->
            "Perkiraan akurasi ±${state.locationAccuracyMeters?.toInt() ?: 0} meter. Tetap perhatikan jalan sekitar."
        LocationQuality.WEAK ->
            "Akurasi hanya sekitar ±${state.locationAccuracyMeters?.toInt() ?: 0} meter. Cari area yang lebih terbuka."
        LocationQuality.SEARCHING -> "Tunggu sebentar atau berpindah ke area yang lebih terbuka."
    }
    val routeDeviation = state.guidance?.distanceFromRouteMeters
    return if (
        state.locationQuality != LocationQuality.WEAK &&
        routeDeviation != null &&
        routeDeviation >= OFF_ROUTE_WARNING_METERS
    ) {
        "$accuracyMessage Posisi terdeteksi sekitar $routeDeviation meter dari garis rute."
    } else {
        accuracyMessage
    }
}

internal fun networkStatusColor(isAvailable: Boolean?): Color = when (isAvailable) {
    true -> SiagaNextGreen
    false -> STATUS_ERROR_COLOR
    null -> STATUS_UNKNOWN_COLOR
}

internal fun bmkgStatusColor(state: EvacuationUiState): Color = when {
    state.bmkgErrorMessage != null -> STATUS_ERROR_COLOR
    state.bmkgStatus?.hasTsunamiPotential == true -> STATUS_ERROR_COLOR
    state.bmkgStatus?.isStale == true -> STATUS_CAUTION_COLOR
    state.bmkgStatus != null -> BMKG_INFO_COLOR
    else -> STATUS_UNKNOWN_COLOR
}

private fun zoneStatusColor(status: InundationZoneStatus?): Color = when (status) {
    InundationZoneStatus.OutsideRecordedZone -> SiagaCream
    is InundationZoneStatus.InsideRecordedZone -> SiagaRust
    else -> STATUS_UNKNOWN_COLOR
}

private fun zoneLegendDisplayColor(baseColor: Color, mapOpacity: Float): Color {
    val backdrop = SiagaCream
    return Color(
        red = baseColor.red * mapOpacity + backdrop.red * (1f - mapOpacity),
        green = baseColor.green * mapOpacity + backdrop.green * (1f - mapOpacity),
        blue = baseColor.blue * mapOpacity + backdrop.blue * (1f - mapOpacity),
        alpha = 1f,
    )
}

internal enum class StatusDetailType {
    GPS,
    NETWORK,
    BMKG,
}

internal enum class MapPanelValue {
    COLLAPSED,
    EXPANDED,
}

private val MAP_PANEL_SPRING = tween<Float>(durationMillis = 340, easing = FastOutSlowInEasing)

private const val OBSTRUCTION_MESSAGE_VISIBLE_MILLIS = 8_000L
internal const val UI_ANIMATION_MILLIS = 280
private val WidgetSafeBlue = Color(0xFF6AC6FF)
private val TOP_BAR_SPACE = 76.dp
private val EXPANDED_HEADER_HEIGHT = 150.dp
private val MAP_HANDLE_SPACE = 28.dp
private val MIN_COLLAPSED_MAP_HEIGHT = 170.dp
private const val FIGMA_WIDTH_DP = 390f
private const val FIGMA_MAP_HEIGHT_DP = 269f
internal const val WALKING_SPEED_METERS_PER_SECOND = 1.2
private const val ROUTE_CHANGE_NOTICE_MILLIS = 3_500L
private const val ZONE_STATUS_NOTICE_MILLIS = 5_000L
private const val ZONE_CHECK_RESULT_POPUP_MILLIS = 4_000L
private const val MANEUVER_NOW_DISTANCE_METERS = 20
private const val ROAD_ENTRY_REACHED_DISTANCE_METERS = 6
private const val OFF_ROUTE_WARNING_METERS = 40
private const val MAX_VISIBLE_INSTRUCTIONS = 4
private const val EXPANDED_STRIP_BASE_HEIGHT_DP = 67f
private const val EXPANDED_STRIP_STEP_HEIGHT_DP = 74f
// Nilai warna dan opasitas ini harus tetap sama dengan layer pada OfflineMap.
private val ZONE_SAFE_COLOR = Color(0xFF00D26A)
private val ZONE_LOW_COLOR = Color(0xFFFFD400)
private val ZONE_MEDIUM_COLOR = Color(0xFFFF6D00)
private val ZONE_HIGH_COLOR = Color(0xFFFF1744)
private const val ZONE_SAFE_MAP_OPACITY = 0.16f
private const val ZONE_LOW_MAP_OPACITY = 0.18f
private const val ZONE_MEDIUM_MAP_OPACITY = 0.26f
private const val ZONE_HIGH_MAP_OPACITY = 0.36f
internal val STATUS_CAUTION_COLOR = Color(0xFFFFD166)
internal val STATUS_ERROR_COLOR = Color(0xFFFF6B6B)
private val STATUS_UNKNOWN_COLOR = Color(0xFFB9C4C9)
internal val BMKG_INFO_COLOR = Color(0xFF28AEFF)
