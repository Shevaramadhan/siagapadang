package com.akusukaproject.siagapadang.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver.PendingResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import com.akusukaproject.siagapadang.MainActivity
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.data.model.EvacuationSummary
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

abstract class BaseEvacuationWidgetProvider(
    private val layoutId: Int,
) : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: allWidgetIds(context, manager)
                refreshWidgets(context, manager, ids, goAsync())
            }

            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                val ids = if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    allWidgetIds(context, manager)
                } else {
                    intArrayOf(widgetId)
                }
                refreshWidgets(context, manager, ids, goAsync())
            }

            ACTION_DATA_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                refreshWidgets(
                    context = context,
                    manager = manager,
                    widgetIds = allWidgetIds(context, manager),
                    pendingResult = goAsync(),
                    coordinateHint = intent.coordinateHint(),
                )
            }

            else -> super.onReceive(context, intent)
        }
    }

    private fun refreshWidgets(
        context: Context,
        manager: AppWidgetManager,
        widgetIds: IntArray,
        pendingResult: PendingResult,
        coordinateHint: GeoCoordinate? = null,
    ) {
        if (widgetIds.isEmpty()) {
            pendingResult.finish()
            return
        }

        val loadingViews = createViews(context) { views ->
            bindUnknown(
                views = views,
                context = context,
                title = context.getString(R.string.widget_location_unreadable),
                detail = context.getString(R.string.widget_reading_location),
            )
        }
        widgetIds.forEach { widgetId -> manager.updateAppWidget(widgetId, loadingViews) }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val state = loadState(context, coordinateHint)
                val views = createViews(context) { remoteViews ->
                    bindState(remoteViews, context, state)
                }
                widgetIds.forEach { widgetId -> manager.updateAppWidget(widgetId, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun loadState(
        context: Context,
        coordinateHint: GeoCoordinate?,
    ): WidgetState {
        if (!hasLocationPermission(context)) return WidgetState.PermissionRequired

        val application = context.applicationContext as SiagaPadangApplication
        val coordinate = coordinateHint ?: runCatching {
            application.locationProvider.currentOrLastKnownLocation()?.coordinate
        }.getOrNull() ?: return WidgetState.LocationUnavailable

        return coroutineScope {
            val zoneStatus = async {
                runCatching { application.zoneRepository.findStatus(coordinate) }
                    .getOrDefault(InundationZoneStatus.DataUnavailable)
            }
            val summary = async {
                runCatching {
                    application.evacuationRepository.findSummaryFromLocation(coordinate)
                }.getOrNull()
            }
            WidgetState.Ready(
                zoneStatus = zoneStatus.await(),
                summary = summary.await(),
            )
        }
    }

    private fun bindState(views: RemoteViews, context: Context, state: WidgetState) {
        when (state) {
            WidgetState.PermissionRequired -> bindUnknown(
                views = views,
                context = context,
                title = context.getString(R.string.widget_location_unreadable),
                detail = context.getString(R.string.widget_enable_location_permission),
            )

            WidgetState.LocationUnavailable -> bindUnknown(
                views = views,
                context = context,
                title = context.getString(R.string.widget_location_unreadable),
                detail = context.getString(R.string.widget_enable_gps),
            )

            is WidgetState.Ready -> when (val status = state.zoneStatus) {
                InundationZoneStatus.DataUnavailable -> bindUnknown(
                    views = views,
                    context = context,
                    title = context.getString(R.string.widget_location_unreadable),
                    detail = context.getString(R.string.widget_zone_data_unavailable_short),
                )

                InundationZoneStatus.OutsideRecordedZone -> bindSafe(views, context)

                is InundationZoneStatus.InsideRecordedZone -> bindDanger(
                    views = views,
                    context = context,
                    status = status,
                    summary = state.summary,
                )
            }
        }
    }

    private fun bindSafe(views: RemoteViews, context: Context) {
        showTheme(views, WidgetTheme.SAFE)
        views.setTextViewText(
            R.id.widget_zone_status,
            context.getString(R.string.widget_safe_title),
        )
        views.setTextViewText(
            R.id.widget_zone_detail,
            context.getString(R.string.widget_safe_detail),
        )
        views.setTextViewText(
            R.id.widget_action_left_text,
            context.getString(R.string.widget_view_evacuation_guide),
        )
        views.setImageViewResource(
            R.id.widget_action_left_icon,
            R.drawable.figma_widget_safe_button_icon,
        )
    }

    private fun bindDanger(
        views: RemoteViews,
        context: Context,
        status: InundationZoneStatus.InsideRecordedZone,
        summary: EvacuationSummary?,
    ) {
        showTheme(views, WidgetTheme.DANGER)
        views.setTextViewText(
            R.id.widget_zone_status,
            context.getString(R.string.widget_danger_title),
        )
        views.setTextViewText(
            R.id.widget_zone_detail,
            status.zoneName.ifBlank { context.getString(R.string.widget_recorded_danger_zone) },
        )
        views.setTextViewText(
            R.id.widget_destination_name,
            summary?.destinationName ?: context.getString(R.string.widget_route_unavailable_short),
        )

        if (summary == null) {
            views.setViewVisibility(R.id.widget_distance, View.GONE)
            views.setViewVisibility(R.id.widget_eta, View.GONE)
        } else {
            val minutes = max(1, (summary.estimatedSeconds / 60.0).roundToInt())
            views.setViewVisibility(R.id.widget_distance, View.VISIBLE)
            views.setViewVisibility(R.id.widget_eta, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_distance,
                context.getString(R.string.widget_distance_meters, summary.estimatedDistanceMeters),
            )
            views.setTextViewText(
                R.id.widget_eta,
                context.resources.getQuantityString(
                    R.plurals.widget_eta_minutes,
                    minutes,
                    minutes,
                ),
            )
        }
        views.setTextViewText(
            R.id.widget_action_right_text,
            context.getString(R.string.widget_evacuation_guide),
        )
    }

    private fun bindUnknown(
        views: RemoteViews,
        context: Context,
        title: String,
        detail: String,
    ) {
        showTheme(views, WidgetTheme.UNKNOWN)
        views.setTextViewText(R.id.widget_zone_status, title)
        views.setTextViewText(R.id.widget_zone_detail, detail)
        views.setTextViewText(
            R.id.widget_action_left_text,
            context.getString(R.string.widget_open_app),
        )
        views.setImageViewResource(
            R.id.widget_action_left_icon,
            R.drawable.figma_widget_unknown_button_icon,
        )
    }

    private fun showTheme(views: RemoteViews, theme: WidgetTheme) {
        val safeVisibility = if (theme == WidgetTheme.SAFE) View.VISIBLE else View.GONE
        val dangerVisibility = if (theme == WidgetTheme.DANGER) View.VISIBLE else View.GONE
        val unknownVisibility = if (theme == WidgetTheme.UNKNOWN) View.VISIBLE else View.GONE

        views.setViewVisibility(R.id.widget_safe_fill, safeVisibility)
        views.setViewVisibility(R.id.widget_danger_fill, dangerVisibility)
        views.setViewVisibility(R.id.widget_unknown_fill, unknownVisibility)
        views.setViewVisibility(R.id.widget_safe_background, safeVisibility)
        views.setViewVisibility(R.id.widget_safe_character, safeVisibility)
        views.setViewVisibility(R.id.widget_danger_background, dangerVisibility)
        views.setViewVisibility(R.id.widget_danger_character, dangerVisibility)
        views.setViewVisibility(R.id.widget_unknown_background, unknownVisibility)
        views.setViewVisibility(R.id.widget_unknown_character, unknownVisibility)
        views.setViewVisibility(R.id.widget_route_icon, dangerVisibility)
        views.setViewVisibility(R.id.widget_destination_name, dangerVisibility)
        views.setViewVisibility(R.id.widget_distance, dangerVisibility)
        views.setViewVisibility(R.id.widget_eta, dangerVisibility)
        views.setViewVisibility(
            R.id.widget_action_left,
            if (theme == WidgetTheme.DANGER) View.GONE else View.VISIBLE,
        )
        views.setViewVisibility(R.id.widget_action_right, dangerVisibility)
    }

    private fun createViews(
        context: Context,
        binder: (RemoteViews) -> Unit,
    ): RemoteViews = baseViews(context, layoutId).apply(binder)

    private fun baseViews(context: Context, layoutId: Int): RemoteViews =
        RemoteViews(context.packageName, layoutId).apply {
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                OPEN_APP_REQUEST_CODE,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            setOnClickPendingIntent(R.id.widget_card, openAppPendingIntent)
            setOnClickPendingIntent(R.id.widget_action_left, openAppPendingIntent)
            setOnClickPendingIntent(R.id.widget_action_right, openAppPendingIntent)
        }

    private fun Intent.coordinateHint(): GeoCoordinate? {
        if (!hasExtra(EXTRA_LATITUDE) || !hasExtra(EXTRA_LONGITUDE)) return null
        return GeoCoordinate(
            latitude = getDoubleExtra(EXTRA_LATITUDE, Double.NaN),
            longitude = getDoubleExtra(EXTRA_LONGITUDE, Double.NaN),
        ).takeIf { it.latitude.isFinite() && it.longitude.isFinite() }
    }

    private fun hasLocationPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun allWidgetIds(context: Context, manager: AppWidgetManager): IntArray =
        manager.getAppWidgetIds(ComponentName(context, javaClass))

    private enum class WidgetTheme {
        SAFE,
        DANGER,
        UNKNOWN,
    }

    private sealed interface WidgetState {
        data object PermissionRequired : WidgetState
        data object LocationUnavailable : WidgetState

        data class Ready(
            val zoneStatus: InundationZoneStatus,
            val summary: EvacuationSummary?,
        ) : WidgetState
    }

    companion object {
        const val ACTION_DATA_CHANGED = "com.akusukaproject.siagapadang.widget.DATA_CHANGED"
        const val EXTRA_LATITUDE = "widget_latitude"
        const val EXTRA_LONGITUDE = "widget_longitude"
        const val OPEN_APP_REQUEST_CODE = 100
    }
}

class EvacuationWidgetProvider : BaseEvacuationWidgetProvider(R.layout.widget_evacuation)

class CompactEvacuationWidgetProvider :
    BaseEvacuationWidgetProvider(R.layout.widget_evacuation_compact)
