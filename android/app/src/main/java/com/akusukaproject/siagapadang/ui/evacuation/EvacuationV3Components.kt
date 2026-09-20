package com.akusukaproject.siagapadang.ui.evacuation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.domain.ManeuverGuidance
import com.akusukaproject.siagapadang.domain.ManeuverType
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaNextGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaRustDeep
import com.akusukaproject.siagapadang.ui.theme.SiagaSafeGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaTailGray
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning

private val CardShape = RoundedCornerShape(28.dp)

/** Merah tua yang tetap memenuhi kontras 4.5:1 di atas lingkaran putih. */
private val BMKG_UNREAD_COLOR = Color(0xFFC62828)

/** Satu putaran getaran: sekitar 0,4 detik bergetar lalu diam sampai detik ketiga. */
private const val SHAKE_CYCLE_MILLIS = 3_000

/**
 * Penanda kabar gempa yang belum dibuka. Dibentuk dari waktu kejadian dan magnitudonya, bukan
 * dari waktu pengambilan data, supaya menyegarkan data yang sama tidak dihitung sebagai kabar baru.
 */
internal fun bmkgEventKey(state: EvacuationUiState): String? = state.bmkgStatus?.let { status ->
    listOf(status.eventDate, status.eventTime, status.magnitude)
        .filter { it.isNotBlank() }
        .joinToString("|")
        .takeIf { it.isNotBlank() }
}

@Composable
internal fun EvacuationTopBar(
    state: EvacuationUiState,
    selected: StatusDetailType?,
    onSelect: (StatusDetailType) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    bmkgIsUnread: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClick = onOpenMenu)
                .semantics { contentDescription = "Buka menu persiapan" },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_ms_grid_view), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Menu", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.weight(1f))
        StatusCircle(
            iconRes = R.drawable.ic_ms_my_location,
            tint = statusTintOnLight(gpsStatusColor(state)),
            hasProblem = gpsHasProblem(state),
            selected = selected == StatusDetailType.GPS,
            description = "Lihat status GPS",
            onClick = { onSelect(StatusDetailType.GPS) },
        )
        Spacer(Modifier.width(6.dp))
        StatusCircle(
            iconRes = if (state.isNetworkAvailable == false) R.drawable.ic_ms_wifi_off else R.drawable.ic_ms_wifi,
            tint = statusTintOnLight(networkStatusColor(state.isNetworkAvailable)),
            hasProblem = state.isNetworkAvailable == false,
            selected = selected == StatusDetailType.NETWORK,
            description = "Lihat status jaringan",
            onClick = { onSelect(StatusDetailType.NETWORK) },
        )
        Spacer(Modifier.width(6.dp))
        StatusCircle(
            iconRes = R.drawable.ic_ms_campaign,
            // Merah selama kabarnya belum dibuka, agar bedanya terbaca tanpa menunggu getaran.
            tint = if (bmkgIsUnread) BMKG_UNREAD_COLOR else statusTintOnLight(bmkgStatusColor(state)),
            hasProblem = bmkgHasProblem(state),
            selected = selected == StatusDetailType.BMKG,
            description = if (bmkgIsUnread) "Ada kabar gempa baru dari BMKG" else "Lihat informasi BMKG",
            onClick = { onSelect(StatusDetailType.BMKG) },
            needsAttention = bmkgIsUnread,
        )
    }
}

@Composable
internal fun StatusColumnV3(
    state: EvacuationUiState,
    selected: StatusDetailType?,
    onSelect: (StatusDetailType) -> Unit,
    modifier: Modifier = Modifier,
    bmkgIsUnread: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        StatusCircle(
            iconRes = R.drawable.ic_ms_my_location,
            tint = statusTintOnLight(gpsStatusColor(state)),
            hasProblem = gpsHasProblem(state),
            selected = selected == StatusDetailType.GPS,
            description = "Lihat status GPS",
            onClick = { onSelect(StatusDetailType.GPS) },
        )
        StatusCircle(
            iconRes = if (state.isNetworkAvailable == false) R.drawable.ic_ms_wifi_off else R.drawable.ic_ms_wifi,
            tint = statusTintOnLight(networkStatusColor(state.isNetworkAvailable)),
            hasProblem = state.isNetworkAvailable == false,
            selected = selected == StatusDetailType.NETWORK,
            description = "Lihat status jaringan",
            onClick = { onSelect(StatusDetailType.NETWORK) },
        )
        StatusCircle(
            iconRes = R.drawable.ic_ms_campaign,
            // Merah selama kabarnya belum dibuka, agar bedanya terbaca tanpa menunggu getaran.
            tint = if (bmkgIsUnread) BMKG_UNREAD_COLOR else statusTintOnLight(bmkgStatusColor(state)),
            hasProblem = bmkgHasProblem(state),
            selected = selected == StatusDetailType.BMKG,
            description = if (bmkgIsUnread) "Ada kabar gempa baru dari BMKG" else "Lihat informasi BMKG",
            onClick = { onSelect(StatusDetailType.BMKG) },
            needsAttention = bmkgIsUnread,
        )
    }
}

@Composable
internal fun StatusCircle(
    iconRes: Int,
    tint: Color,
    hasProblem: Boolean,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    needsAttention: Boolean = false,
) {
    // Getaran pendek berulang, bukan goyangan terus-menerus: cukup menarik mata tanpa
    // berubah menjadi gangguan selama pengguna sedang berjalan.
    val shake by rememberInfiniteTransition(label = "getar-ikon").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = SHAKE_CYCLE_MILLIS
                0f at 0
                -12f at 70
                12f at 140
                -9f at 210
                9f at 280
                -5f at 350
                0f at 420
                0f at SHAKE_CYCLE_MILLIS
            },
        ),
        label = "sudut-getar",
    )
    Box(modifier = modifier.size(48.dp)) {
        Surface(
            color = Color.White,
            shape = CircleShape,
            shadowElevation = 4.dp,
            border = if (selected) BorderStroke(2.dp, SiagaNavy) else null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = if (needsAttention) shake else 0f),
                )
            }
        }
        if (hasProblem) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC62828)),
            )
        }
    }
}

@Composable
internal fun InstructionCardV3(
    route: EvacuationRoute,
    guidance: RouteGuidanceSnapshot?,
    modifier: Modifier = Modifier,
) {
    val isApproachingRoute = guidance?.isApproachingRoute == true
    val instruction = guidance?.currentInstruction ?: ManeuverGuidance(
        type = ManeuverType.STRAIGHT,
        distanceMeters = estimatedDistanceMeters(route),
    )
    val presentation = maneuverPresentation(instruction.type)
    val label = maneuverInstructionLabel(presentation.label, instruction.type, isApproachingRoute)
    val nextSteps = guidance?.instructions.orEmpty().drop(1).take(MAX_TAIL_STEPS)
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CardShape,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(presentation.drawableRes),
                contentDescription = label,
                tint = SiagaNavy,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(104.dp),
            )
            Text(
                text = label,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = maneuverDistanceMessage(instruction, isApproachingRoute),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SiagaTextSecondary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                route.destinationKind?.let { kind ->
                    Surface(
                        color = if (kind.uppercase() == "TEA") SiagaSafeGreen else SiagaWarning,
                        contentColor = SiagaNavy,
                        shape = RoundedCornerShape(7.dp),
                    ) {
                        Text(
                            text = kind.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = route.destinationName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (nextSteps.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SiagaTailGray)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text("Lalu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SiagaTextSecondary)
                    nextSteps.forEach { step ->
                        val stepPresentation = maneuverPresentation(step.type)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(stepPresentation.drawableRes),
                                contentDescription = stepPresentation.label,
                                tint = SiagaNavy,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = if (step.type == ManeuverType.ARRIVE) "TES" else formatDistance(step.distanceMeters),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Angka waktu yang setiap digitnya bergulir saat berubah, agar hitung mundur terlihat berjalan. */
@Composable
internal fun RollingDuration(text: String, fontSize: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = text }) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (slideInVertically(tween(260)) { -it / 2 } + fadeIn(tween(260))) togetherWith
                        (slideOutVertically(tween(260)) { it / 2 } + fadeOut(tween(200)))
                },
                label = "digit-$index",
            ) { digit ->
                Text(
                    text = digit.toString(),
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                )
            }
        }
    }
}

/** Nada kartu waktu, termasuk keadaan akhir ketika jendela evakuasi sudah habis. */
internal enum class TimeTone { ENOUGH, TIGHT, NOT_ENOUGH, EXPIRED }

internal fun timeTone(remainingSeconds: Int, walkingSeconds: Int): TimeTone = when {
    remainingSeconds <= 0 -> TimeTone.EXPIRED
    remainingSeconds < walkingSeconds -> TimeTone.NOT_ENOUGH
    remainingSeconds < walkingSeconds * TIGHT_TIME_FACTOR -> TimeTone.TIGHT
    else -> TimeTone.ENOUGH
}

@Composable
internal fun TimeCardV3(
    route: EvacuationRoute,
    guidance: RouteGuidanceSnapshot?,
    remainingSeconds: Int,
    compassMessage: String?,
    modifier: Modifier = Modifier,
) {
    val remainingDistance = guidance?.remainingDistanceMeters ?: estimatedDistanceMeters(route)
    val walkingSeconds = (remainingDistance / WALKING_SPEED_METERS_PER_SECOND).toInt()
    val tone = timeTone(remainingSeconds, walkingSeconds)
    val background = when (tone) {
        TimeTone.ENOUGH -> Color.White
        TimeTone.TIGHT -> SiagaWarning
        TimeTone.NOT_ENOUGH, TimeTone.EXPIRED -> SiagaRustDeep
    }
    val primary = if (tone == TimeTone.NOT_ENOUGH || tone == TimeTone.EXPIRED) Color.White else SiagaNavy
    val secondary = when (tone) {
        TimeTone.ENOUGH -> SiagaTextSecondary
        TimeTone.TIGHT -> SiagaNavy
        TimeTone.NOT_ENOUGH, TimeTone.EXPIRED -> Color.White
    }
    val walkingMinutes = ((walkingSeconds + 59) / 60).coerceAtLeast(1)
    Surface(
        color = background,
        contentColor = primary,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SISA WAKTU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = secondary, letterSpacing = 0.5.sp)
                    RollingDuration(text = formatDuration(remainingSeconds), fontSize = 40)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(56.dp)
                        .background(if (tone == TimeTone.ENOUGH) SiagaLine else primary.copy(alpha = 0.3f)),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("BERJALAN CEPAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = secondary, letterSpacing = 0.5.sp)
                    Text("±$walkingMinutes mnt", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                    Text("${formatDistance(remainingDistance)} ke tujuan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = secondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Bilah yang menyusut setiap detik menandakan hitung mundur sedang berjalan.
            val progress by animateFloatAsState(
                targetValue = (remainingSeconds / EvacuationUiState.EVACUATION_WINDOW_SECONDS.toFloat()).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 900, easing = LinearEasing),
                label = "sisa-waktu",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (tone == TimeTone.ENOUGH) SiagaNavy else primary),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_ms_info), contentDescription = null, tint = secondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = when {
                        compassMessage != null -> compassMessage
                        tone == TimeTone.NOT_ENOUGH -> "Waktu tempuh melebihi sisa waktu. Tetap berjalan cepat, jangan berhenti."
                        tone == TimeTone.EXPIRED -> "Jendela waktu evakuasi telah habis."
                        else -> "Sisa waktu dihitung sejak aplikasi dibuka"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondary,
                )
            }
        }
    }
}

@Composable
internal fun ExpiredEvacuationCardV3(modifier: Modifier = Modifier) {
    Surface(
        color = SiagaRustDeep,
        contentColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Waktu evakuasi habis. Lakukan evakuasi vertikal sekarang."
            },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White,
                    contentColor = SiagaRustDeep,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ms_warning),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "WAKTU EVAKUASI HABIS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = "Lakukan evakuasi vertikal sekarang",
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Text(
                text = "TES/TEA belum tercapai. Hentikan perjalanan jauh dan cari tempat evakuasi vertikal di sekitar Anda.",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                color = Color.White,
                contentColor = SiagaNavy,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    ExpiredGuidanceRow(
                        number = "1",
                        text = "Cari bangunan evakuasi bertingkat terdekat. Jika tidak ada, pilih bangunan beton bertulang yang tidak tampak rusak.",
                    )
                    ExpiredGuidanceRow(
                        number = "2",
                        text = "Gunakan tangga, jangan lift. Naik ke lantai paling atas, sedikitnya lantai 3.",
                    )
                    ExpiredGuidanceRow(
                        number = "3",
                        text = "Jauhi pantai dan sungai. Ikuti petugas atau rambu evakuasi.",
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpiredGuidanceRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = SiagaWarning,
            contentColor = SiagaNavy,
            shape = CircleShape,
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun MapOpenHandle(
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
            .anchoredDraggable(state = dragState, orientation = Orientation.Vertical)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (expansionProgress > 0.5f) "Kecilkan peta" else "Perbesar peta"
            },
    ) {
        // Tetap di tengah tepi peta seperti gagang sebelumnya, tetapi kini bertuliskan
        // tindakannya. Hanya teksnya yang berganti, tidak ada tombol kedua yang menimpa.
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.height(40.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_ms_expand_less),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer(rotationZ = expansionProgress * 180f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (expansionProgress > 0.5f) "Perkecil peta" else "Perbesar peta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun ObstacleButton(
    enabled: Boolean,
    isLoading: Boolean,
    hasArrived: Boolean,
    arrivalReason: EvacuationArrivalReason?,
    isDirectOrientationActive: Boolean,
    isEvacuationWindowExpired: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when {
        hasArrived && arrivalReason == EvacuationArrivalReason.OUTSIDE_INUNDATION_ZONE -> "Di luar zona rendaman"
        hasArrived -> "Anda sudah sampai di TES"
        isEvacuationWindowExpired -> "Evakuasi vertikal sekarang"
        isDirectOrientationActive -> "Orientasi terakhir aktif"
        isLoading -> "Mencari jalan lain…"
        else -> "Ada kendala?"
    }
    val active = enabled && !isLoading
    Surface(
        color = when {
            hasArrived -> Color(0xFF58D68D)
            isEvacuationWindowExpired -> SiagaRustDeep
            active -> SiagaWarning
            else -> Color(0xFFE1E3C0)
        },
        contentColor = if (isEvacuationWindowExpired) Color.White else SiagaNavy,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, SiagaNavy),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (active) "Ada kendala? Jalan terhalang atau tidak bisa masuk TES" else title },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = SiagaNavy, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    painterResource(
                        if (isEvacuationWindowExpired) R.drawable.ic_ms_warning else R.drawable.ic_ms_report,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                if (active) {
                    Text("Jalan terhalang · Tidak bisa masuk TES", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun ObstacleSheet(
    hasAlternativeRoute: Boolean,
    onDismiss: () -> Unit,
    onSelect: (EvacuationObstacleType) -> Unit,
) {
    // targetState disetel sekali lewat LaunchedEffect. Kalau disetel langsung saat komposisi,
    // setiap recomposition akan membatalkan penutupan dan lembar ini tidak pernah bisa ditutup.
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    fun close(action: () -> Unit) {
        pendingAction = action
        visibleState.targetState = false
    }
    LaunchedEffect(visibleState.currentState, visibleState.isIdle) {
        if (visibleState.isIdle && !visibleState.currentState) pendingAction?.invoke()
    }
    Dialog(
        onDismissRequest = { close(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(visibleState = visibleState, enter = fadeIn(tween(UI_ANIMATION_MILLIS)), exit = fadeOut(tween(UI_ANIMATION_MILLIS))) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { close(onDismiss) },
                )
            }
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(tween(UI_ANIMATION_MILLIS, easing = FastOutSlowInEasing)) { it },
                exit = slideOutVertically(tween(UI_ANIMATION_MILLIS, easing = FastOutSlowInEasing)) { it },
            ) {
                Surface(
                    color = Color.White,
                    contentColor = SiagaNavy,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                        // Diseret ke bawah melewati 96 dp berarti ditutup; kurang dari itu kembali ke tempatnya.
                        .pointerInput(Unit) {
                            val dismissThreshold = 96.dp.toPx()
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, delta ->
                                    change.consume()
                                    scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) }
                                },
                                onDragEnd = {
                                    if (dragOffset.value > dismissThreshold) {
                                        close(onDismiss)
                                    } else {
                                        scope.launch { dragOffset.animateTo(0f, tween(UI_ANIMATION_MILLIS, easing = FastOutSlowInEasing)) }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { dragOffset.animateTo(0f, tween(UI_ANIMATION_MILLIS, easing = FastOutSlowInEasing)) }
                                },
                            )
                        },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    ) {
                        Box(Modifier.size(width = 40.dp, height = 5.dp).clip(CircleShape).background(Color(0xFFC9CED6)))
                        Text("Apa kendalanya?", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                        Text("Rute di HP langsung diganti. Tidak perlu internet.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
                        ObstacleOption(
                            iconRes = R.drawable.ic_ms_block,
                            iconBackground = SiagaWarning,
                            iconTint = SiagaNavy,
                            title = "Jalan tidak bisa dilewati",
                            description = if (hasAlternativeRoute) {
                                "Cari jalan lain ke TES yang sama. Jika tidak ada, ke alternatif tujuan."
                            } else {
                                "Cari jalan lain ke TES yang sama. Jika tidak ada, tampilkan orientasi terakhir."
                            },
                            onClick = { close { onSelect(EvacuationObstacleType.ROAD_BLOCKED) } },
                        )
                        ObstacleOption(
                            iconRes = R.drawable.ic_ms_door_front,
                            iconBackground = Color(0xFFFBE3D9),
                            iconTint = SiagaRustDeep,
                            title = "Tidak bisa masuk ke TES/TEA",
                            description = "Cari alternatif tujuan terdekat.",
                            onClick = { close { onSelect(EvacuationObstacleType.DESTINATION_UNREACHABLE) } },
                        )
                        Surface(
                            color = Color(0xFFEEF1F5),
                            contentColor = SiagaNavy,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(role = Role.Button) { close(onDismiss) },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Batal", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObstacleOption(
    iconRes: Int,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(2.dp, SiagaNavy),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBackground),
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(description, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
            }
            Icon(painterResource(R.drawable.ic_ms_chevron_right), contentDescription = null, modifier = Modifier.size(26.dp))
        }
    }
}

internal fun gpsHasProblem(state: EvacuationUiState): Boolean =
    !state.hasLocationPermission ||
        state.locationQuality == LocationQuality.FAIR ||
        state.locationQuality == LocationQuality.WEAK

internal fun bmkgHasProblem(state: EvacuationUiState): Boolean =
    state.bmkgErrorMessage != null ||
        state.bmkgStatus?.isStale == true ||
        state.bmkgStatus?.hasTsunamiPotential == true

/** Warna status dirancang untuk latar navy; di atas tombol putih dipakai padanan yang lebih gelap. */
internal fun statusTintOnLight(color: Color): Color = when (color) {
    SiagaNextGreen -> SiagaSafeGreen
    STATUS_CAUTION_COLOR -> Color(0xFF8A5A00)
    STATUS_ERROR_COLOR -> Color(0xFFC62828)
    BMKG_INFO_COLOR -> Color(0xFF0B63C7)
    else -> SiagaNavy
}

private const val MAX_TAIL_STEPS = 4
private const val TIGHT_TIME_FACTOR = 1.25
