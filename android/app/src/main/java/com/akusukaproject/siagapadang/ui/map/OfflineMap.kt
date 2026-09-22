package com.akusukaproject.siagapadang.ui.map

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.OfflineRoadOverlay
import com.akusukaproject.siagapadang.data.model.TsunamiZoneOverlay
import com.akusukaproject.siagapadang.domain.BearingCalculator
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.domain.PolylineSimplifier
import com.akusukaproject.siagapadang.R
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.expressions.Expression
import com.akusukaproject.siagapadang.data.model.Facility
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val DEVELOPMENT_MAP_STYLE = """
    {
      "version": 8,
      "name": "Siaga Padang",
      "sources": {
        "openstreetmap": {
          "type": "raster",
          "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
          "tileSize": 256,
          "maxzoom": 19,
          "attribution": "© OpenStreetMap contributors"
        }
      },
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": { "background-color": "#0E2A47" }
        },
        {
          "id": "openstreetmap",
          "type": "raster",
          "source": "openstreetmap",
          "paint": {
            "raster-opacity": 0.90,
            "raster-saturation": -0.35,
            "raster-brightness-max": 0.88
          }
        }
      ]
    }
"""

@Suppress("DEPRECATION")
@Composable
fun OfflineMap(
    offlineRoadOverlay: OfflineRoadOverlay?,
    isNetworkAvailable: Boolean?,
    tsunamiZoneOverlay: TsunamiZoneOverlay?,
    routeCoordinates: List<GeoCoordinate>,
    approachRouteCoordinates: List<GeoCoordinate>,
    approachTargetLocation: GeoCoordinate?,
    previousRouteCoordinates: List<List<GeoCoordinate>>,
    currentLocation: GeoCoordinate?,
    destinationLocation: GeoCoordinate?,
    destinationName: String?,
    destinationDistanceLabel: String?,
    destinationKindLabel: String? = null,
    destinationDurationLabel: String? = null,
    deviceHeadingDegrees: Float?,
    followUserLocation: Boolean,
    recenterRequest: Int,
    routeOverviewRequest: Int,
    onViewportChanged: (GeoCoordinate) -> Unit,
    onUserMapGesture: () -> Unit,
    modifier: Modifier = Modifier,
    facilityMarkers: List<Facility> = emptyList(),
    selectedFacilityId: String? = null,
    onFacilityClick: (String) -> Unit = {},
    focusCoordinates: List<GeoCoordinate> = emptyList(),
    focusRequest: Int = 0,
    focusBottomPaddingPx: Int = 0,
    onMapDoubleTap: (() -> Unit)? = null,
) {
    val latestFacilities = rememberUpdatedState(facilityMarkers)
    val latestSelectedFacility = rememberUpdatedState(selectedFacilityId)
    val latestOnFacilityClick = rememberUpdatedState(onFacilityClick)
    val context = LocalContext.current
    val latestOnMapDoubleTap = rememberUpdatedState(onMapDoubleTap)
    val doubleTapDetector = remember(context) {
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val handler = latestOnMapDoubleTap.value ?: return false
                    handler()
                    return true
                }
            },
        )
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val displayRouteCoordinates = remember(routeCoordinates) {
        PolylineSimplifier.simplify(routeCoordinates)
    }
    val displayPreviousRouteCoordinates = remember(previousRouteCoordinates) {
        previousRouteCoordinates.map { coordinates -> PolylineSimplifier.simplify(coordinates) }
    }
    val latestRoute = rememberUpdatedState(displayRouteCoordinates)
    val latestApproachRoute = rememberUpdatedState(approachRouteCoordinates)
    val latestApproachTarget = rememberUpdatedState(approachTargetLocation)
    val latestOfflineRoadOverlay = rememberUpdatedState(offlineRoadOverlay)
    val latestNetworkAvailable = rememberUpdatedState(isNetworkAvailable)
    val latestTsunamiZoneOverlay = rememberUpdatedState(tsunamiZoneOverlay)
    val latestPreviousRoutes = rememberUpdatedState(displayPreviousRouteCoordinates)
    val latestLocation = rememberUpdatedState(currentLocation)
    val latestDestination = rememberUpdatedState(destinationLocation)
    val latestDestinationName = rememberUpdatedState(destinationName)
    val latestDestinationDistance = rememberUpdatedState(destinationDistanceLabel)
    val latestDestinationKind = rememberUpdatedState(destinationKindLabel)
    val latestDestinationDuration = rememberUpdatedState(destinationDurationLabel)
    val latestHeading = rememberUpdatedState(deviceHeadingDegrees)
    val latestFollowUser = rememberUpdatedState(followUserLocation)
    val latestOnViewportChanged = rememberUpdatedState(onViewportChanged)
    val latestOnUserMapGesture = rememberUpdatedState(onUserMapGesture)
    val userMarkerBitmap = remember(context) { createUserMarkerBitmap(context) }
    val destinationMarkerBitmap = remember(context) { createDestinationMarkerBitmap(context) }
    val destinationAnnotationBitmap = remember(
        context,
        destinationName,
        destinationDistanceLabel,
        destinationKindLabel,
        destinationDurationLabel,
    ) {
        createDestinationAnnotationBitmap(
            context = context,
            destinationName = destinationName ?: "Tujuan evakuasi",
            distanceLabel = destinationDistanceLabel.orEmpty(),
            kindLabel = destinationKindLabel,
            durationLabel = destinationDurationLabel,
        )
    }
    val cameraTracker = remember { CameraTracker() }
    val overlayTracker = remember { OverlayTracker() }
    val mapView = remember {
        val mapOptions = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
        MapView(context, mapOptions).apply {
            onCreate(null)
            setOnTouchListener { view, event ->
                doubleTapDetector.onTouchEvent(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_MOVE -> {
                        if (latestFollowUser.value) {
                            cameraTracker.isFollowing = false
                            latestOnUserMapGesture.value()
                        }
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(DEFAULT_PADANG_CENTER)
                    .zoom(DEFAULT_ZOOM)
                    .build()
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isRotateGesturesEnabled = true
                map.uiSettings.isScrollGesturesEnabled = true
                map.uiSettings.isHorizontalScrollGesturesEnabled = true
                map.uiSettings.isZoomGesturesEnabled = true
                map.uiSettings.isDoubleTapGesturesEnabled = true
                map.uiSettings.isQuickZoomGesturesEnabled = true
                map.addOnCameraMoveStartedListener { reason ->
                    if (
                        reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE &&
                        latestFollowUser.value
                    ) {
                        cameraTracker.isFollowing = false
                        latestOnUserMapGesture.value()
                    }
                }
                map.addOnMapClickListener { point ->
                    val screenPoint = map.projection.toScreenLocation(point)
                    val facilityId = map.queryRenderedFeatures(screenPoint, FACILITY_LAYER_ID)
                        .firstOrNull()
                        ?.getStringProperty(FACILITY_ID_PROPERTY)
                    facilityId?.let(latestOnFacilityClick.value)
                    facilityId != null
                }
                map.addOnCameraIdleListener {
                    map.cameraPosition.target?.let { target ->
                        latestOnViewportChanged.value(target.toGeoCoordinate())
                    }
                    map.style?.let { style ->
                        updateDestinationCardPlacement(
                            style = style,
                            routeCoordinates = latestRoute.value,
                            mapBearingDegrees = map.cameraPosition.bearing,
                        )
                    }
                }
                map.setStyle(Style.Builder().fromJson(DEVELOPMENT_MAP_STYLE)) { style ->
                    updateMapOverlays(
                        style = style,
                        offlineRoadOverlay = latestOfflineRoadOverlay.value,
                        isNetworkAvailable = latestNetworkAvailable.value,
                        tsunamiZoneOverlay = latestTsunamiZoneOverlay.value,
                        routeCoordinates = latestRoute.value,
                        approachRouteCoordinates = latestApproachRoute.value,
                        approachTargetLocation = latestApproachTarget.value,
                        previousRouteCoordinates = latestPreviousRoutes.value,
                        currentLocation = latestLocation.value,
                        destinationLocation = latestDestination.value,
                        destinationAnnotationBitmap = createDestinationAnnotationBitmap(
                            context = context,
                            destinationName = latestDestinationName.value ?: "Tujuan evakuasi",
                            distanceLabel = latestDestinationDistance.value.orEmpty(),
                            kindLabel = latestDestinationKind.value,
                            durationLabel = latestDestinationDuration.value,
                        ),
                        destinationMarkerBitmap = destinationMarkerBitmap,
                        mapBearingDegrees = map.cameraPosition.bearing,
                        userMarkerBitmap = userMarkerBitmap,
                        offlineRoadOverlayTracker = cameraTracker,
                    )
                    updateFacilityOverlay(style, latestFacilities.value, latestSelectedFacility.value)
                    // Gaya baru berarti seluruh sumber hilang, jadi pembanding dikosongkan agar
                    // pembaruan berikutnya membangun ulang lapisannya.
                    overlayTracker.snapshot = null
                    map.cameraPosition.target?.let { target ->
                        latestOnViewportChanged.value(target.toGeoCoordinate())
                    }
                    if (latestFollowUser.value) latestLocation.value?.let { location ->
                        updateNavigationCamera(
                            map = map,
                            location = location,
                            headingDegrees = latestHeading.value,
                            tracker = cameraTracker,
                            animate = false,
                            force = true,
                        )
                        cameraTracker.isFollowing = true
                    }
                }
            }
        }
    }
    val lifecycleController = remember(mapView) { MapViewLifecycleController(mapView) }

    DisposableEffect(lifecycle, lifecycleController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> lifecycleController.start()
                Lifecycle.Event.ON_RESUME -> lifecycleController.resume()
                Lifecycle.Event.ON_PAUSE -> lifecycleController.pause()
                Lifecycle.Event.ON_STOP -> lifecycleController.stop()
                Lifecycle.Event.ON_DESTROY -> lifecycleController.destroy()
                else -> Unit
            }
        }
        val applicationContext = context.applicationContext
        val memoryCallbacks = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    mapView.onLowMemory()
                }
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onLowMemory() {
                mapView.onLowMemory()
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit
        }

        lifecycle.addObserver(observer)
        applicationContext.registerComponentCallbacks(memoryCallbacks)
        lifecycleController.synchronizeWith(lifecycle.currentState)
        onDispose {
            lifecycle.removeObserver(observer)
            applicationContext.unregisterComponentCallbacks(memoryCallbacks)
            lifecycleController.destroy()
        }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            view.getMapAsync { map ->
                // Saat ketuk ganda dipakai untuk memperbesar panel, zoom bawaan dimatikan agar tidak bentrok.
                map.uiSettings.isDoubleTapGesturesEnabled = onMapDoubleTap == null
                map.style?.let { style ->
                    // Layar evakuasi disusun ulang terus-menerus — arah kompas berubah pada laju
                    // sensor dan hitung mundur setiap detik. Tanpa pembanding ini seluruh GeoJSON
                    // (rute, 73 poligon zona, jaringan jalan, penanda) ditulis ulang setiap kali,
                    // thread utama tersita, dan sentuhan pengguna terlewat.
                    val snapshot = OverlaySnapshot(
                        offlineRoadOverlay = offlineRoadOverlay,
                        isNetworkAvailable = isNetworkAvailable,
                        tsunamiZoneOverlay = tsunamiZoneOverlay,
                        routeCoordinates = displayRouteCoordinates,
                        approachRouteCoordinates = approachRouteCoordinates,
                        approachTargetLocation = approachTargetLocation,
                        previousRouteCoordinates = displayPreviousRouteCoordinates,
                        currentLocation = currentLocation,
                        destinationLocation = destinationLocation,
                        destinationAnnotationBitmap = destinationAnnotationBitmap,
                        facilityMarkers = facilityMarkers,
                        selectedFacilityId = selectedFacilityId,
                    )
                    if (overlayTracker.snapshot != snapshot) {
                        overlayTracker.snapshot = snapshot
                        updateMapOverlays(
                            style = style,
                            offlineRoadOverlay = offlineRoadOverlay,
                            isNetworkAvailable = isNetworkAvailable,
                            tsunamiZoneOverlay = tsunamiZoneOverlay,
                            routeCoordinates = displayRouteCoordinates,
                            approachRouteCoordinates = approachRouteCoordinates,
                            approachTargetLocation = approachTargetLocation,
                            previousRouteCoordinates = displayPreviousRouteCoordinates,
                            currentLocation = currentLocation,
                            destinationLocation = destinationLocation,
                            destinationAnnotationBitmap = destinationAnnotationBitmap,
                            destinationMarkerBitmap = destinationMarkerBitmap,
                            mapBearingDegrees = map.cameraPosition.bearing,
                            userMarkerBitmap = userMarkerBitmap,
                            offlineRoadOverlayTracker = cameraTracker,
                        )
                        updateFacilityOverlay(style, facilityMarkers, selectedFacilityId)
                    }
                }
                if (focusRequest != cameraTracker.focusRequest && focusCoordinates.isNotEmpty()) {
                    showFocus(map, focusCoordinates, focusBottomPaddingPx)
                    cameraTracker.focusRequest = focusRequest
                }
                if (followUserLocation) currentLocation?.let { location ->
                    if (routeOverviewRequest != cameraTracker.routeOverviewRequest) {
                        showRouteOverview(map, displayRouteCoordinates, location)
                        cameraTracker.isFollowing = false
                        cameraTracker.routeOverviewRequest = routeOverviewRequest
                    } else {
                        val forceRecenter = recenterRequest != cameraTracker.recenterRequest
                        updateNavigationCamera(
                            map = map,
                            location = location,
                            headingDegrees = deviceHeadingDegrees,
                            tracker = cameraTracker,
                            animate = true,
                            force = !cameraTracker.isFollowing || forceRecenter,
                        )
                        cameraTracker.isFollowing = true
                        cameraTracker.recenterRequest = recenterRequest
                    }
                } else {
                    cameraTracker.isFollowing = false
                    if (routeOverviewRequest != cameraTracker.routeOverviewRequest) {
                        showRouteOverview(map, displayRouteCoordinates, currentLocation)
                        cameraTracker.routeOverviewRequest = routeOverviewRequest
                    }
                }
            }
        },
        modifier = modifier,
    )
}

/** Penanda TES (kuning) dan TEA (hijau) untuk halaman TES & TEA; kosong di layar evakuasi. */
private fun updateFacilityOverlay(
    style: Style,
    facilities: List<Facility>,
    selectedFacilityId: String?,
) {
    val existingSource = style.getSource(FACILITY_SOURCE_ID) as? GeoJsonSource
    if (existingSource == null && facilities.isEmpty()) return
    val collection = FeatureCollection.fromFeatures(
        facilities.map { facility ->
            Feature.fromGeometry(
                Point.fromLngLat(facility.coordinate.longitude, facility.coordinate.latitude),
            ).apply {
                addStringProperty(FACILITY_ID_PROPERTY, facility.id)
                addStringProperty(FACILITY_KIND_PROPERTY, facility.kind.name)
                addBooleanProperty(FACILITY_SELECTED_PROPERTY, facility.id == selectedFacilityId)
            }
        },
    )
    if (existingSource != null) {
        existingSource.setGeoJson(collection)
        return
    }
    style.addSource(GeoJsonSource(FACILITY_SOURCE_ID, collection))
    val selected = Expression.eq(Expression.get(FACILITY_SELECTED_PROPERTY), Expression.literal(true))
    style.addLayer(
        CircleLayer(FACILITY_LAYER_ID, FACILITY_SOURCE_ID).withProperties(
            circleRadius(Expression.switchCase(selected, Expression.literal(13f), Expression.literal(8f))),
            circleColor(
                Expression.match(
                    Expression.get(FACILITY_KIND_PROPERTY),
                    Expression.literal("TEA"),
                    Expression.color(android.graphics.Color.parseColor("#58D68D")),
                    Expression.color(android.graphics.Color.parseColor("#F7FF0C")),
                ),
            ),
            circleStrokeColor("#01346D"),
            circleStrokeWidth(Expression.switchCase(selected, Expression.literal(3.5f), Expression.literal(1.5f))),
        ),
    )
}

/** Mengarahkan kamera ke satu titik atau ke kumpulan titik (halaman TES & TEA). */
private fun showFocus(
    map: org.maplibre.android.maps.MapLibreMap,
    coordinates: List<GeoCoordinate>,
    bottomPaddingPx: Int,
) {
    val points = coordinates.map { LatLng(it.latitude, it.longitude) }
    if (points.size == 1) {
        map.easeCamera(CameraUpdateFactory.newLatLngZoom(points.first(), FOCUS_SINGLE_ZOOM), ROUTE_OVERVIEW_ANIMATION_MILLIS)
        return
    }
    val bounds = LatLngBounds.Builder().includes(points).build()
    val padding = ROUTE_OVERVIEW_PADDING_PX
    map.easeCamera(
        CameraUpdateFactory.newLatLngBounds(bounds, padding, padding * 4, padding, padding + bottomPaddingPx),
        ROUTE_OVERVIEW_ANIMATION_MILLIS,
    )
}

private fun showRouteOverview(
    map: org.maplibre.android.maps.MapLibreMap,
    routeCoordinates: List<GeoCoordinate>,
    currentLocation: GeoCoordinate?,
) {
    if (routeCoordinates.size < 2) return
    val points = routeCoordinates.map { coordinate ->
        LatLng(coordinate.latitude, coordinate.longitude)
    }.toMutableList()
    currentLocation?.let { location ->
        points += LatLng(location.latitude, location.longitude)
    }
    val bounds = LatLngBounds.Builder().includes(points).build()
    map.easeCamera(
        CameraUpdateFactory.newLatLngBounds(bounds, ROUTE_OVERVIEW_PADDING_PX),
        ROUTE_OVERVIEW_ANIMATION_MILLIS,
    )
}

private fun updateMapOverlays(
    style: Style,
    offlineRoadOverlay: OfflineRoadOverlay?,
    isNetworkAvailable: Boolean?,
    tsunamiZoneOverlay: TsunamiZoneOverlay?,
    routeCoordinates: List<GeoCoordinate>,
    approachRouteCoordinates: List<GeoCoordinate>,
    approachTargetLocation: GeoCoordinate?,
    previousRouteCoordinates: List<List<GeoCoordinate>>,
    currentLocation: GeoCoordinate?,
    destinationLocation: GeoCoordinate?,
    destinationAnnotationBitmap: Bitmap,
    destinationMarkerBitmap: Bitmap,
    mapBearingDegrees: Double,
    userMarkerBitmap: Bitmap,
    offlineRoadOverlayTracker: CameraTracker,
) {
    offlineRoadOverlay?.let { overlay ->
        updateOfflineRoadOverlay(style, overlay, isNetworkAvailable, offlineRoadOverlayTracker)
    }
    updateTsunamiZoneOverlays(style, tsunamiZoneOverlay)
    updatePreviousRouteOverlays(style, previousRouteCoordinates)

    val existingRouteSource = style.getSource(ROUTE_SOURCE_ID) as? GeoJsonSource
    if (routeCoordinates.size >= 2) {
        val geometry = LineString.fromLngLats(
            routeCoordinates.map { coordinate ->
                Point.fromLngLat(coordinate.longitude, coordinate.latitude)
            },
        )
        if (existingRouteSource == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, geometry))
            val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                lineColor("#007BFA"),
                lineWidth(7f),
                lineOpacity(0.96f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            )
            when {
                style.getLayer(DESTINATION_LAYER_ID) != null ->
                    style.addLayerBelow(routeLayer, DESTINATION_LAYER_ID)
                style.getLayer(LOCATION_LAYER_ID) != null ->
                    style.addLayerBelow(routeLayer, LOCATION_LAYER_ID)
                else -> style.addLayer(routeLayer)
            }
        } else {
            existingRouteSource.setGeoJson(geometry)
        }
    } else {
        existingRouteSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    }

    updateApproachRouteOverlay(style, approachRouteCoordinates)
    updateApproachTargetOverlay(style, approachTargetLocation)

    destinationLocation?.let { coordinate ->
        val feature = Feature.fromGeometry(
            Point.fromLngLat(coordinate.longitude, coordinate.latitude),
        )
        val existingDestinationSource = style.getSource(DESTINATION_SOURCE_ID) as? GeoJsonSource
        style.addImage(DESTINATION_CARD_IMAGE_ID, destinationAnnotationBitmap)
        style.addImage(DESTINATION_MARKER_IMAGE_ID, destinationMarkerBitmap)
        if (existingDestinationSource == null) {
            style.addSource(GeoJsonSource(DESTINATION_SOURCE_ID, feature))
            style.addLayer(
                SymbolLayer(DESTINATION_CARD_LAYER_ID, DESTINATION_SOURCE_ID).withProperties(
                    iconImage(DESTINATION_CARD_IMAGE_ID),
                    iconSize(1f),
                    iconAnchor(Property.ICON_ANCHOR_CENTER),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                ),
            )
            style.addLayer(
                SymbolLayer(DESTINATION_LAYER_ID, DESTINATION_SOURCE_ID).withProperties(
                    iconImage(DESTINATION_MARKER_IMAGE_ID),
                    iconSize(1f),
                    iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                ),
            )
        } else {
            existingDestinationSource.setGeoJson(feature)
        }
        updateDestinationCardPlacement(
            style = style,
            routeCoordinates = routeCoordinates,
            mapBearingDegrees = mapBearingDegrees,
        )
    } ?: run {
        (style.getSource(DESTINATION_SOURCE_ID) as? GeoJsonSource)
            ?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    }

    currentLocation?.let { coordinate ->
        val feature = Feature.fromGeometry(
            Point.fromLngLat(coordinate.longitude, coordinate.latitude),
        )
        val existingLocationSource = style.getSource(LOCATION_SOURCE_ID) as? GeoJsonSource
        if (existingLocationSource == null) {
            style.addImage(USER_LOCATION_IMAGE_ID, userMarkerBitmap)
            style.addSource(GeoJsonSource(LOCATION_SOURCE_ID, feature))
            style.addLayer(
                SymbolLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID).withProperties(
                    iconImage(USER_LOCATION_IMAGE_ID),
                    iconSize(1f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                ),
            )
        } else {
            existingLocationSource.setGeoJson(feature)
        }
    }
}

private fun updateOfflineRoadOverlay(
    style: Style,
    overlay: OfflineRoadOverlay,
    isNetworkAvailable: Boolean?,
    tracker: CameraTracker,
) {
    val existingSource = style.getSource(OFFLINE_ROADS_SOURCE_ID) as? GeoJsonSource
    if (existingSource == null) {
        style.addSource(GeoJsonSource(OFFLINE_ROADS_SOURCE_ID, overlay.geoJson))
        // Tepi gelap terbaca di atas ubin OSM yang terang; inti terang terbaca di atas latar
        // polos gelap ketika ubin tidak tersedia. Keduanya diperlukan karena aplikasi tidak
        // dapat mengetahui apakah ubin untuk area ini ada di cache.
        val casingLayer = LineLayer(OFFLINE_ROADS_CASING_LAYER_ID, OFFLINE_ROADS_SOURCE_ID).withProperties(
            lineColor("#0B1F33"),
            lineWidth(4.2f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
        )
        val roadLayer = LineLayer(OFFLINE_ROADS_LAYER_ID, OFFLINE_ROADS_SOURCE_ID).withProperties(
            lineColor("#DCE6F0"),
            lineWidth(2.1f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
        )
        // Jaringan jalan selalu di bawah rute dan penanda agar garis terang tidak memotong rute.
        val navigationAnchor = listOf(
            PREVIOUS_ROUTES_LAYER_ID,
            ROUTE_LAYER_ID,
            APPROACH_ROUTE_LAYER_ID,
            APPROACH_TARGET_LAYER_ID,
            DESTINATION_LAYER_ID,
            LOCATION_LAYER_ID,
        ).firstOrNull { layerId -> style.getLayer(layerId) != null }
        if (navigationAnchor == null) {
            style.addLayer(casingLayer)
            style.addLayer(roadLayer)
        } else {
            style.addLayerBelow(casingLayer, navigationAnchor)
            style.addLayerBelow(roadLayer, navigationAnchor)
        }
        tracker.offlineRoadViewportId = overlay.viewportId
    } else if (tracker.offlineRoadViewportId != overlay.viewportId) {
        existingSource.setGeoJson(overlay.geoJson)
        tracker.offlineRoadViewportId = overlay.viewportId
    }

    val roadOpacity = if (isNetworkAvailable == true) {
        ONLINE_ROAD_OPACITY
    } else {
        OFFLINE_ROAD_OPACITY
    }
    listOf(OFFLINE_ROADS_CASING_LAYER_ID, OFFLINE_ROADS_LAYER_ID).forEach { layerId ->
        style.getLayerAs<LineLayer>(layerId)?.setProperties(lineOpacity(roadOpacity))
    }
}

private fun LatLng.toGeoCoordinate() = GeoCoordinate(
    latitude = latitude,
    longitude = longitude,
)

private fun updateApproachRouteOverlay(
    style: Style,
    coordinates: List<GeoCoordinate>,
) {
    val featureCollection = if (coordinates.size >= 2) {
        FeatureCollection.fromFeatures(
            listOf(
            Feature.fromGeometry(
                LineString.fromLngLats(
                    coordinates.map { coordinate ->
                        Point.fromLngLat(coordinate.longitude, coordinate.latitude)
                    },
                ),
            ),
            ),
        )
    } else {
        FeatureCollection.fromFeatures(emptyList<Feature>())
    }
    val existingSource = style.getSource(APPROACH_ROUTE_SOURCE_ID) as? GeoJsonSource
    if (existingSource == null) {
        style.addSource(GeoJsonSource(APPROACH_ROUTE_SOURCE_ID, featureCollection))
        val layer = LineLayer(APPROACH_ROUTE_LAYER_ID, APPROACH_ROUTE_SOURCE_ID).withProperties(
            lineColor("#FFF2D7"),
            lineWidth(5f),
            lineOpacity(0.96f),
            lineDasharray(arrayOf(1.1f, 1.7f)),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
        )
        when {
            style.getLayer(DESTINATION_LAYER_ID) != null ->
                style.addLayerBelow(layer, DESTINATION_LAYER_ID)
            style.getLayer(LOCATION_LAYER_ID) != null ->
                style.addLayerBelow(layer, LOCATION_LAYER_ID)
            else -> style.addLayer(layer)
        }
    } else {
        existingSource.setGeoJson(featureCollection)
    }
}

private fun updateApproachTargetOverlay(
    style: Style,
    coordinate: GeoCoordinate?,
) {
    val featureCollection = if (coordinate == null) {
        FeatureCollection.fromFeatures(emptyList<Feature>())
    } else {
        FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(
                    Point.fromLngLat(coordinate.longitude, coordinate.latitude),
                ),
            ),
        )
    }
    val existingSource = style.getSource(APPROACH_TARGET_SOURCE_ID) as? GeoJsonSource
    if (existingSource == null) {
        style.addSource(GeoJsonSource(APPROACH_TARGET_SOURCE_ID, featureCollection))
        val layer = CircleLayer(APPROACH_TARGET_LAYER_ID, APPROACH_TARGET_SOURCE_ID).withProperties(
            circleColor("#FFF2D7"),
            circleRadius(8f),
            circleOpacity(0.98f),
            circleStrokeColor("#063A51"),
            circleStrokeWidth(3f),
        )
        when {
            style.getLayer(DESTINATION_LAYER_ID) != null ->
                style.addLayerBelow(layer, DESTINATION_LAYER_ID)
            style.getLayer(LOCATION_LAYER_ID) != null ->
                style.addLayerBelow(layer, LOCATION_LAYER_ID)
            else -> style.addLayer(layer)
        }
    } else {
        existingSource.setGeoJson(featureCollection)
    }
}

private fun updateTsunamiZoneOverlays(
    style: Style,
    overlay: TsunamiZoneOverlay?,
) {
    if (overlay == null) {
        listOf(
            SAFE_ZONE_SOURCE_ID,
            LOW_RISK_ZONE_SOURCE_ID,
            MEDIUM_RISK_ZONE_SOURCE_ID,
            HIGH_RISK_ZONE_SOURCE_ID,
        ).forEach { sourceId ->
            (style.getSource(sourceId) as? GeoJsonSource)
                ?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
        return
    }
    ensureZoneLayer(
        style = style,
        sourceId = SAFE_ZONE_SOURCE_ID,
        fillLayerId = SAFE_ZONE_FILL_LAYER_ID,
        geoJson = overlay.safeGeoJson,
        fill = SAFE_ZONE_FILL_COLOR,
        opacity = SAFE_ZONE_OPACITY,
        lineLayerId = SAFE_ZONE_LINE_LAYER_ID,
        lineColor = SAFE_ZONE_LINE_COLOR,
        lineWidthDp = 1.5f,
        lineDash = null,
    )
    ensureZoneLayer(
        style = style,
        sourceId = LOW_RISK_ZONE_SOURCE_ID,
        fillLayerId = LOW_RISK_ZONE_FILL_LAYER_ID,
        geoJson = overlay.lowRiskGeoJson,
        fill = LOW_RISK_ZONE_FILL_COLOR,
        opacity = LOW_RISK_ZONE_OPACITY,
        lineLayerId = LOW_RISK_ZONE_LINE_LAYER_ID,
        lineColor = LOW_RISK_ZONE_LINE_COLOR,
        lineWidthDp = 1.5f,
        lineDash = arrayOf(2f, 2f),
    )
    ensureZoneLayer(
        style = style,
        sourceId = MEDIUM_RISK_ZONE_SOURCE_ID,
        fillLayerId = MEDIUM_RISK_ZONE_FILL_LAYER_ID,
        geoJson = overlay.mediumRiskGeoJson,
        fill = MEDIUM_RISK_ZONE_FILL_COLOR,
        opacity = MEDIUM_RISK_ZONE_OPACITY,
        lineLayerId = MEDIUM_RISK_ZONE_LINE_LAYER_ID,
        lineColor = MEDIUM_RISK_ZONE_LINE_COLOR,
        lineWidthDp = 2.2f,
        lineDash = null,
    )
    ensureZoneLayer(
        style = style,
        sourceId = HIGH_RISK_ZONE_SOURCE_ID,
        fillLayerId = HIGH_RISK_ZONE_FILL_LAYER_ID,
        geoJson = overlay.highRiskGeoJson,
        fill = HIGH_RISK_ZONE_FILL_COLOR,
        opacity = HIGH_RISK_ZONE_OPACITY,
        lineLayerId = HIGH_RISK_ZONE_LINE_LAYER_ID,
        lineColor = HIGH_RISK_ZONE_LINE_COLOR,
        lineWidthDp = 3.2f,
        lineDash = null,
    )
}

private fun ensureZoneLayer(
    style: Style,
    sourceId: String,
    fillLayerId: String,
    geoJson: String,
    fill: String,
    opacity: Float,
    lineLayerId: String,
    lineColor: String,
    lineWidthDp: Float,
    lineDash: Array<Float>?,
) {
    val existingSource = style.getSource(sourceId) as? GeoJsonSource
    if (existingSource != null) {
        existingSource.setGeoJson(geoJson)
        return
    }

    style.addSource(GeoJsonSource(sourceId, geoJson))
    val fillLayer = FillLayer(fillLayerId, sourceId).withProperties(
        fillColor(fill),
        fillOpacity(opacity),
    )
    // Garis tepi dengan ketebalan berbeda per tingkat agar zona tetap terbedakan tanpa bergantung warna saja.
    val outlineLayer = LineLayer(lineLayerId, sourceId).withProperties(
        lineColor(lineColor),
        lineWidth(lineWidthDp),
        lineOpacity(0.9f),
        lineJoin(Property.LINE_JOIN_ROUND),
    ).apply { lineDash?.let { setProperties(lineDasharray(it)) } }
    val navigationAnchor = listOf(
        FACILITY_LAYER_ID,
        PREVIOUS_ROUTES_LAYER_ID,
        ROUTE_LAYER_ID,
        DESTINATION_LAYER_ID,
        LOCATION_LAYER_ID,
    ).firstOrNull { layerId -> style.getLayer(layerId) != null }
    if (navigationAnchor == null) {
        style.addLayer(fillLayer)
        style.addLayer(outlineLayer)
    } else {
        style.addLayerBelow(fillLayer, navigationAnchor)
        style.addLayerBelow(outlineLayer, navigationAnchor)
    }
}

private fun updatePreviousRouteOverlays(
    style: Style,
    previousRouteCoordinates: List<List<GeoCoordinate>>,
) {
    val features = previousRouteCoordinates.mapNotNull { coordinates ->
        if (coordinates.size < 2) return@mapNotNull null
        Feature.fromGeometry(
            LineString.fromLngLats(
                coordinates.map { coordinate ->
                    Point.fromLngLat(coordinate.longitude, coordinate.latitude)
                },
            ),
        )
    }
    val featureCollection = FeatureCollection.fromFeatures(features)
    val existingSource = style.getSource(PREVIOUS_ROUTES_SOURCE_ID) as? GeoJsonSource
    if (existingSource == null) {
        if (features.isEmpty()) return
        style.addSource(GeoJsonSource(PREVIOUS_ROUTES_SOURCE_ID, featureCollection))
        val layer = LineLayer(PREVIOUS_ROUTES_LAYER_ID, PREVIOUS_ROUTES_SOURCE_ID).withProperties(
            lineColor("#7D8588"),
            lineWidth(5f),
            lineOpacity(0.72f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
        )
        if (style.getLayer(ROUTE_LAYER_ID) != null) {
            style.addLayerBelow(layer, ROUTE_LAYER_ID)
        } else {
            style.addLayer(layer)
        }
    } else {
        existingSource.setGeoJson(featureCollection)
    }
}

private fun updateNavigationCamera(
    map: org.maplibre.android.maps.MapLibreMap,
    location: GeoCoordinate,
    headingDegrees: Float?,
    tracker: CameraTracker,
    animate: Boolean,
    force: Boolean = false,
) {
    val normalizedHeading = ((headingDegrees ?: tracker.headingDegrees) % 360f + 360f) % 360f
    val movedMeters = tracker.location?.let { previous ->
        NearestNodeFinder.distanceMeters(previous, location)
    } ?: Double.POSITIVE_INFINITY
    val headingDelta = angularDifferenceDegrees(tracker.headingDegrees, normalizedHeading)
    if (!force && movedMeters < MIN_CAMERA_MOVE_METERS && headingDelta < MIN_CAMERA_TURN_DEGREES) return

    val cameraPosition = CameraPosition.Builder()
        .target(LatLng(location.latitude, location.longitude))
        .zoom(USER_LOCATION_ZOOM)
        .bearing(normalizedHeading.toDouble())
        .build()
    val update = CameraUpdateFactory.newCameraPosition(cameraPosition)
    if (animate && tracker.location != null) {
        map.easeCamera(update, CAMERA_ANIMATION_MILLIS)
    } else {
        map.moveCamera(update)
    }
    tracker.location = location
    tracker.headingDegrees = normalizedHeading
}

private fun angularDifferenceDegrees(first: Float, second: Float): Float {
    val difference = kotlin.math.abs(first - second) % 360f
    return minOf(difference, 360f - difference)
}

private fun createUserMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val outerSize = (USER_MARKER_OUTER_DP * density).toInt().coerceAtLeast(1)
    val leafSize = (USER_MARKER_LEAF_DP * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(outerSize, outerSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = outerSize / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(USER_MARKER_BACKGROUND_ALPHA, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - density, paint)
    paint.apply {
        color = Color.rgb(9, 78, 255)
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    canvas.drawCircle(center, center, center - density, paint)

    context.getDrawable(R.drawable.ic_figma_map_arrow)?.let { drawable ->
        val inset = (outerSize - leafSize) / 2
        drawable.setBounds(inset, inset, inset + leafSize, inset + leafSize)
        drawable.draw(canvas)
    }
    return bitmap
}

private fun createDestinationAnnotationBitmap(
    context: Context,
    destinationName: String,
    distanceLabel: String,
    kindLabel: String?,
    durationLabel: String?,
): Bitmap {
    val density = context.resources.displayMetrics.density
    fun px(dp: Float): Float = dp * density

    val width = px(144f).toInt()
    val height = px(68f).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val bounds = RectF(px(2f), px(2f), width - px(2f), height - px(3f))
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(px(3f), 0f, px(1f), Color.argb(72, 0, 0, 0))
    }
    canvas.drawRoundRect(bounds, px(12f), px(12f), cardPaint)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(207, 218, 224)
        style = Paint.Style.STROKE
        strokeWidth = px(1f)
    }
    canvas.drawRoundRect(bounds, px(12f), px(12f), borderPaint)

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (kindLabel?.uppercase() == "TEA") Color.rgb(32, 156, 91) else Color.rgb(0, 85, 150)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(px(3f), px(11f), px(8f), height - px(12f)),
        px(3f),
        px(3f),
        accentPaint,
    )

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(12, 24, 31)
        textSize = px(13f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val detailsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(73, 91, 101)
        textSize = px(11.5f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textStart = px(16f)
    val textWidth = width - px(28f)
    val kind = kindLabel?.uppercase()?.takeIf { it.isNotBlank() }
    val title = listOfNotNull(kind, destinationName).joinToString(" · ")
    canvas.drawText(
        fitText(title, namePaint, textWidth),
        textStart,
        px(29f),
        namePaint,
    )
    val details = listOfNotNull(
        distanceLabel.takeIf { it.isNotBlank() }?.let { "± $it" },
        durationLabel?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    if (details.isNotBlank()) {
        canvas.drawText(
            fitText(details, detailsPaint, textWidth),
            textStart,
            px(49f),
            detailsPaint,
        )
    }
    return bitmap
}

private fun createDestinationMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (34f * density).toInt().coerceAtLeast(1)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        context.getDrawable(R.drawable.ic_figma_destination)?.let { drawable ->
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
    }
}

/**
 * Memindahkan kartu ke sisi lanjutan arah perjalanan. Garis rute masuk ke tujuan dari sisi
 * sebaliknya, sehingga kartu tidak lagi menutup ruas terakhir yang harus diikuti pengguna.
 */
private fun updateDestinationCardPlacement(
    style: Style,
    routeCoordinates: List<GeoCoordinate>,
    mapBearingDegrees: Double,
) {
    val layer = style.getLayerAs<SymbolLayer>(DESTINATION_CARD_LAYER_ID) ?: return
    val offset = destinationCardOffset(routeCoordinates, mapBearingDegrees)
    layer.setProperties(iconOffset(arrayOf(offset.first, offset.second)))
}

private fun destinationCardOffset(
    routeCoordinates: List<GeoCoordinate>,
    mapBearingDegrees: Double,
): Pair<Float, Float> {
    // MapLibre Android menafsirkan icon-offset sebagai dp, jadi jangan dikalikan density layar.
    if (routeCoordinates.size < 2) return 76f to -44f
    val previous = routeCoordinates[routeCoordinates.lastIndex - 1]
    val destination = routeCoordinates.last()
    val travelBearing = BearingCalculator.bearingDegrees(previous, destination)
    val relativeRadians = Math.toRadians(
        BearingCalculator.relativeRotationDegrees(travelBearing, mapBearingDegrees).toDouble(),
    )
    val screenX = sin(relativeRadians).toFloat()
    val screenY = (-cos(relativeRadians)).toFloat()
    // Gunakan sisi tegak lurus terhadap ruas terakhir. Untuk ruas hampir vertikal, pilih sisi
    // kanan agar kartu tidak terdorong ke bawah header; untuk ruas hampir horizontal, pilih bawah.
    val perpendicularX = abs(screenY)
    return if (perpendicularX >= 0.42f) {
        76f to screenX * 24f
    } else {
        0f to 64f
    }
}

private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
        end--
    }
    return text.substring(0, end).trimEnd() + ellipsis
}

/**
 * Pembanding isi lapisan peta. Selama seluruh isinya masih rujukan yang sama, perbandingan ini
 * berhenti pada pemeriksaan identitas, jauh lebih murah daripada menulis ulang sumber GeoJSON.
 */
private data class OverlaySnapshot(
    val offlineRoadOverlay: OfflineRoadOverlay?,
    val isNetworkAvailable: Boolean?,
    val tsunamiZoneOverlay: TsunamiZoneOverlay?,
    val routeCoordinates: List<GeoCoordinate>,
    val approachRouteCoordinates: List<GeoCoordinate>,
    val approachTargetLocation: GeoCoordinate?,
    val previousRouteCoordinates: List<List<GeoCoordinate>>,
    val currentLocation: GeoCoordinate?,
    val destinationLocation: GeoCoordinate?,
    val destinationAnnotationBitmap: Bitmap,
    val facilityMarkers: List<Facility>,
    val selectedFacilityId: String?,
)

private class OverlayTracker {
    var snapshot: OverlaySnapshot? = null
}

private class CameraTracker {
    var location: GeoCoordinate? = null
    var headingDegrees: Float = 0f
    var isFollowing: Boolean = false
    var recenterRequest: Int = -1
    var routeOverviewRequest: Int = 0
    var offlineRoadViewportId: String? = null
    var focusRequest: Int = 0
}

private class MapViewLifecycleController(
    private val mapView: MapView,
) {
    private var started = false
    private var resumed = false
    private var destroyed = false

    fun synchronizeWith(state: Lifecycle.State) {
        when {
            state == Lifecycle.State.DESTROYED -> destroy()
            state.isAtLeast(Lifecycle.State.RESUMED) -> resume()
            state.isAtLeast(Lifecycle.State.STARTED) -> start()
        }
    }

    fun start() {
        if (!destroyed && !started) {
            mapView.onStart()
            started = true
        }
    }

    fun resume() {
        if (!destroyed && !resumed) {
            start()
            mapView.onResume()
            resumed = true
        }
    }

    fun pause() {
        if (!destroyed && resumed) {
            mapView.onPause()
            resumed = false
        }
    }

    fun stop() {
        if (!destroyed && started) {
            pause()
            mapView.onStop()
            started = false
        }
    }

    fun destroy() {
        if (!destroyed) {
            stop()
            mapView.onDestroy()
            destroyed = true
        }
    }
}

private val DEFAULT_PADANG_CENTER = LatLng(-0.9471, 100.4172)
private const val DEFAULT_ZOOM = 12.5
private const val USER_LOCATION_ZOOM = 16.0
private const val ROUTE_OVERVIEW_PADDING_PX = 120
private const val ROUTE_OVERVIEW_ANIMATION_MILLIS = 900
private const val CAMERA_ANIMATION_MILLIS = 180
private const val MIN_CAMERA_MOVE_METERS = 1.5
private const val MIN_CAMERA_TURN_DEGREES = 2f
private const val USER_MARKER_OUTER_DP = 51f
private const val USER_MARKER_LEAF_DP = 39f
private const val USER_MARKER_BACKGROUND_ALPHA = 145
private const val ROUTE_SOURCE_ID = "evacuation-route-source"
private const val OFFLINE_ROADS_SOURCE_ID = "offline-roads-source"
private const val OFFLINE_ROADS_LAYER_ID = "offline-roads-layer"
private const val OFFLINE_ROADS_CASING_LAYER_ID = "offline-roads-casing-layer"
private const val OFFLINE_ROAD_OPACITY = 0.78f
private const val ONLINE_ROAD_OPACITY = 0.16f
private const val ROUTE_LAYER_ID = "evacuation-route-layer"
private const val APPROACH_ROUTE_SOURCE_ID = "approach-route-source"
private const val APPROACH_ROUTE_LAYER_ID = "approach-route-layer"
private const val APPROACH_TARGET_SOURCE_ID = "approach-target-source"
private const val APPROACH_TARGET_LAYER_ID = "approach-target-layer"
private const val PREVIOUS_ROUTES_SOURCE_ID = "previous-evacuation-routes-source"
private const val PREVIOUS_ROUTES_LAYER_ID = "previous-evacuation-routes-layer"
private const val LOCATION_SOURCE_ID = "user-location-source"
private const val LOCATION_LAYER_ID = "user-location-layer"
private const val USER_LOCATION_IMAGE_ID = "user-location-navigation-image"
private const val DESTINATION_SOURCE_ID = "evacuation-destination-source"
private const val DESTINATION_CARD_LAYER_ID = "evacuation-destination-card-layer"
private const val DESTINATION_LAYER_ID = "evacuation-destination-layer"
private const val FACILITY_SOURCE_ID = "facility-source"
private const val FOCUS_SINGLE_ZOOM = 15.0
private const val FACILITY_LAYER_ID = "facility-layer"
private const val FACILITY_ID_PROPERTY = "id"
private const val FACILITY_KIND_PROPERTY = "kind"
private const val FACILITY_SELECTED_PROPERTY = "selected"
private const val DESTINATION_CARD_IMAGE_ID = "evacuation-destination-card-image"
private const val DESTINATION_MARKER_IMAGE_ID = "evacuation-destination-marker-image"
private const val SAFE_ZONE_SOURCE_ID = "tsunami-safe-zone-source"
private const val SAFE_ZONE_FILL_LAYER_ID = "tsunami-safe-zone-fill-layer"
private const val LOW_RISK_ZONE_SOURCE_ID = "tsunami-low-risk-zone-source"
private const val LOW_RISK_ZONE_FILL_LAYER_ID = "tsunami-low-risk-zone-fill-layer"
private const val MEDIUM_RISK_ZONE_SOURCE_ID = "tsunami-medium-risk-zone-source"
private const val MEDIUM_RISK_ZONE_FILL_LAYER_ID = "tsunami-medium-risk-zone-fill-layer"
private const val HIGH_RISK_ZONE_SOURCE_ID = "tsunami-high-risk-zone-source"
private const val HIGH_RISK_ZONE_FILL_LAYER_ID = "tsunami-high-risk-zone-fill-layer"
private const val SAFE_ZONE_FILL_COLOR = "#00D26A"
private const val LOW_RISK_ZONE_FILL_COLOR = "#FFD400"
private const val MEDIUM_RISK_ZONE_FILL_COLOR = "#FF6D00"
private const val HIGH_RISK_ZONE_FILL_COLOR = "#FF1744"
// Kepekatan naik sesuai tingkat bahaya agar perbedaannya terbaca di peta terang.
private const val SAFE_ZONE_OPACITY = 0.16f
private const val LOW_RISK_ZONE_OPACITY = 0.18f
private const val MEDIUM_RISK_ZONE_OPACITY = 0.26f
private const val HIGH_RISK_ZONE_OPACITY = 0.36f
private const val SAFE_ZONE_LINE_COLOR = "#00A152"
private const val LOW_RISK_ZONE_LINE_COLOR = "#B38F00"
private const val MEDIUM_RISK_ZONE_LINE_COLOR = "#E65100"
private const val HIGH_RISK_ZONE_LINE_COLOR = "#C62828"
private const val SAFE_ZONE_LINE_LAYER_ID = "tsunami-safe-zone-line-layer"
private const val LOW_RISK_ZONE_LINE_LAYER_ID = "tsunami-low-risk-zone-line-layer"
private const val MEDIUM_RISK_ZONE_LINE_LAYER_ID = "tsunami-medium-risk-zone-line-layer"
private const val HIGH_RISK_ZONE_LINE_LAYER_ID = "tsunami-high-risk-zone-line-layer"
