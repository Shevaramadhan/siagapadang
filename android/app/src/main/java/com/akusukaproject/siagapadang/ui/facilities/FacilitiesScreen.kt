package com.akusukaproject.siagapadang.ui.facilities

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.data.model.FacilityKind
import com.akusukaproject.siagapadang.ui.common.BackPill
import com.akusukaproject.siagapadang.ui.common.CalmCardShape
import com.akusukaproject.siagapadang.ui.common.LightStatusBarIcons
import com.akusukaproject.siagapadang.ui.map.OfflineMap
import com.akusukaproject.siagapadang.ui.theme.SiagaCalmBackground
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning

private val TeaGreen = Color(0xFFCDEBD9)
private val TeaGreenText = Color(0xFF155F37)

@Composable
fun FacilitiesScreen(
    onBack: () -> Unit,
    viewModel: FacilitiesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var followUser by rememberSaveable { mutableStateOf(true) }
    var showLayerControls by rememberSaveable { mutableStateOf(false) }
    var showTsunamiZones by rememberSaveable { mutableStateOf(true) }
    var showTesMarkers by rememberSaveable { mutableStateOf(true) }
    var showTeaMarkers by rememberSaveable { mutableStateOf(true) }
    BackHandler(onBack = onBack)
    LightStatusBarIcons()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiagaCalmBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackPill(label = "Menu", onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Text(
                    "TES & TEA",
                    color = SiagaNavy,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            ModeSwitch(mode = state.mode, onSelect = viewModel::setMode)
            if (state.mode == FacilitiesMode.LIST) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Cari nama TES atau TEA") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_ms_search), contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = SiagaNavy,
                        unfocusedTextColor = SiagaNavy,
                        focusedBorderColor = SiagaNavy,
                        unfocusedBorderColor = SiagaLine,
                        cursorColor = SiagaNavy,
                        focusedLeadingIconColor = SiagaTextSecondary,
                        unfocusedLeadingIconColor = SiagaTextSecondary,
                        focusedPlaceholderColor = SiagaTextSecondary,
                        unfocusedPlaceholderColor = SiagaTextSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Semua · ${state.countOf(null)}", state.kindFilter == null) { viewModel.setKindFilter(null) }
                FilterChip("TES · ${state.countOf(FacilityKind.TES)}", state.kindFilter == FacilityKind.TES) {
                    viewModel.setKindFilter(FacilityKind.TES)
                }
                FilterChip("TEA · ${state.countOf(FacilityKind.TEA)}", state.kindFilter == FacilityKind.TEA) {
                    viewModel.setKindFilter(FacilityKind.TEA)
                }
            }
        }

        when {
            state.isLoading -> Unit
            state.errorMessage != null -> Text(
                state.errorMessage.orEmpty(),
                color = SiagaNavy,
                modifier = Modifier.padding(20.dp),
            )
            state.mode == FacilitiesMode.LIST -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    Text(
                        text = if (state.userLocation != null) "TERDEKAT DARI POSISI ANDA" else "URUT NAMA · POSISI BELUM DIKETAHUI",
                        color = SiagaTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
                items(state.visibleItems, key = { it.facility.id }) { item ->
                    FacilityTile(
                        item = item,
                        onClick = {
                            viewModel.select(item.facility.id)
                            viewModel.setMode(FacilitiesMode.MAP)
                            followUser = false
                        },
                    )
                }
            }
            else -> Box(modifier = Modifier.weight(1f)) {
                val mapFacilities = state.visibleItems
                    .map { it.facility }
                    .filter { facility ->
                        when (facility.kind) {
                            FacilityKind.TES -> showTesMarkers
                            FacilityKind.TEA -> showTeaMarkers
                        }
                    }
                val selectedMapItem = state.selected?.takeIf { item ->
                    when (item.facility.kind) {
                        FacilityKind.TES -> showTesMarkers
                        FacilityKind.TEA -> showTeaMarkers
                    }
                }
                val mapFocusCoordinates = selectedMapItem
                    ?.let { listOfNotNull(state.userLocation, it.facility.coordinate) }
                    ?: (listOfNotNull(state.userLocation) +
                        mapFacilities.take(NEAREST_IN_OVERVIEW).map { it.coordinate })
                OfflineMap(
                    offlineRoadOverlay = null,
                    isNetworkAvailable = null,
                    tsunamiZoneOverlay = state.tsunamiZoneOverlay.takeIf { showTsunamiZones },
                    routeCoordinates = emptyList(),
                    approachRouteCoordinates = emptyList(),
                    approachTargetLocation = null,
                    previousRouteCoordinates = emptyList(),
                    currentLocation = state.userLocation,
                    destinationLocation = selectedMapItem?.facility?.coordinate,
                    destinationName = selectedMapItem?.facility?.name,
                    destinationDistanceLabel = selectedMapItem?.distanceMeters?.let(::formatFacilityDistance),
                    deviceHeadingDegrees = null,
                    followUserLocation = false,
                    recenterRequest = 0,
                    routeOverviewRequest = 0,
                    onViewportChanged = {},
                    onUserMapGesture = { followUser = false },
                    facilityMarkers = mapFacilities,
                    selectedFacilityId = selectedMapItem?.facility?.id,
                    onFacilityClick = viewModel::select,
                    focusCoordinates = mapFocusCoordinates,
                    focusRequest = listOf(
                        state.selectedId,
                        state.kindFilter,
                        showTesMarkers,
                        showTeaMarkers,
                    ).hashCode() or 1,
                    focusBottomPaddingPx = if (selectedMapItem != null) {
                        with(LocalDensity.current) { 230.dp.roundToPx() }
                    } else {
                        0
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                MapLayerControls(
                    expanded = showLayerControls,
                    onExpandedChange = { showLayerControls = it },
                    showTsunamiZones = showTsunamiZones,
                    onShowTsunamiZonesChange = { showTsunamiZones = it },
                    zoneOverlayAvailable = state.tsunamiZoneOverlay != null,
                    zoneOverlayErrorMessage = state.zoneOverlayErrorMessage,
                    showTesMarkers = showTesMarkers,
                    onShowTesMarkersChange = { showTesMarkers = it },
                    showTeaMarkers = showTeaMarkers,
                    onShowTeaMarkersChange = { showTeaMarkers = it },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
                selectedMapItem?.let { selected ->
                    SelectedFacilityCard(
                        item = selected,
                        message = state.meetingPointMessage,
                        onSetMeetingPoint = { viewModel.setAsFamilyMeetingPoint(selected) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLayerControls(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    showTsunamiZones: Boolean,
    onShowTsunamiZonesChange: (Boolean) -> Unit,
    zoneOverlayAvailable: Boolean,
    zoneOverlayErrorMessage: String?,
    showTesMarkers: Boolean,
    onShowTesMarkersChange: (Boolean) -> Unit,
    showTeaMarkers: Boolean,
    onShowTeaMarkersChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SiagaLine),
            shadowElevation = 5.dp,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(role = Role.Button) { onExpandedChange(!expanded) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_ms_layers),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Lapisan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(
                    painterResource(if (expanded) R.drawable.ic_ms_expand_less else R.drawable.ic_ms_expand_more),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (expanded) {
            Surface(
                color = Color.White,
                contentColor = SiagaNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SiagaLine),
                shadowElevation = 7.dp,
                modifier = Modifier.widthIn(min = 250.dp, max = 290.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text("Lapisan peta", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    LayerToggleRow(
                        title = "Zona rendaman",
                        detail = when {
                            zoneOverlayAvailable -> "Batas tingkat bahaya"
                            zoneOverlayErrorMessage != null -> "Data zona belum tersedia"
                            else -> "Memuat data lokal…"
                        },
                        checked = showTsunamiZones && zoneOverlayAvailable,
                        enabled = zoneOverlayAvailable,
                        onCheckedChange = onShowTsunamiZonesChange,
                    )
                    if (showTsunamiZones && zoneOverlayAvailable) ZoneRiskLegend()
                    LayerToggleRow(
                        title = "Titik TES",
                        detail = "Gedung evakuasi sementara",
                        checked = showTesMarkers,
                        onCheckedChange = onShowTesMarkersChange,
                    )
                    LayerToggleRow(
                        title = "Titik TEA",
                        detail = "Kawasan evakuasi akhir",
                        checked = showTeaMarkers,
                        onCheckedChange = onShowTeaMarkersChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) },
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) SiagaNavy else SiagaTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(detail, color = SiagaTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SiagaNavy,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF9AA6B2),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ZoneRiskLegend() {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        Text(
            text = "Keterangan warna",
            color = SiagaTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        // Hijau mudah dikira "aman", padahal yang diketahui hanya bahwa petak itu berada di luar
        // poligon rendaman yang tercatat. Labelnya ditulis utuh supaya tidak disalahartikan.
        ZoneLegendDot("Di luar zona rendaman", Color(0xFF00A152))
        ZoneLegendDot("Zona bahaya rendah", Color(0xFFE0B900))
        ZoneLegendDot("Zona bahaya sedang", Color(0xFFE65C00))
        ZoneLegendDot("Zona bahaya tinggi", Color(0xFFC62828))
    }
}

@Composable
private fun ZoneLegendDot(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = SiagaTextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun ModeSwitch(mode: FacilitiesMode, onSelect: (FacilitiesMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE8E3D8))
            .padding(4.dp),
    ) {
        listOf(FacilitiesMode.LIST to "Daftar", FacilitiesMode.MAP to "Peta").forEach { (value, label) ->
            val active = mode == value
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = { onSelect(value) }),
            ) {
                Text(label, color = SiagaNavy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) SiagaNavy else Color.White,
        contentColor = if (active) Color.White else SiagaNavy,
        shape = RoundedCornerShape(20.dp),
        border = if (active) null else BorderStroke(1.dp, SiagaLine),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KindBadge(kind: FacilityKind) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 46.dp, height = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (kind == FacilityKind.TES) SiagaWarning else TeaGreen),
    ) {
        Text(kind.label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (kind == FacilityKind.TES) SiagaNavy else TeaGreenText)
    }
}

@Composable
private fun FacilityTile(item: FacilityItem, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CalmCardShape,
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CalmCardShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            KindBadge(item.facility.kind)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.facility.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(capacityLabel(item), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
            }
            item.distanceMeters?.let {
                Spacer(Modifier.width(8.dp))
                Text(formatFacilityDistance(it), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SelectedFacilityCard(
    item: FacilityItem,
    message: String?,
    onSetMeetingPoint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindBadge(item.facility.kind)
                Spacer(Modifier.width(12.dp))
                Text(item.facility.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                item.distanceMeters?.let { Text(formatFacilityDistance(it), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            Text(capacityLabel(item), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
            Surface(
                color = SiagaNavy,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onSetMeetingPoint),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_ms_flag), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Jadikan titik temu keluarga", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            message?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SiagaTextSecondary) }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private const val NEAREST_IN_OVERVIEW = 6

private fun capacityLabel(item: FacilityItem): String {
    val people = String.format("%,d", item.facility.capacityPeople).replace(',', '.')
    return when (item.facility.kind) {
        FacilityKind.TES -> "Gedung · kapasitas rancangan $people orang"
        FacilityKind.TEA -> "Kawasan terbuka · kapasitas perkiraan $people orang"
    }
}

private fun formatFacilityDistance(meters: Int): String =
    if (meters < 1_000) "${(meters / 10) * 10} m" else "%.1f km".format(meters / 1_000.0)
