package com.akusukaproject.siagapadang.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.akusukaproject.siagapadang.data.model.GeoCoordinate

/**
 * Mengirim perubahan lokasi aktif ke kedua ukuran widget tanpa menunggu pembaruan berkala launcher.
 * Pembaruan dibatasi agar aliran GPS satu detik tidak terus membuka database untuk widget.
 */
object EvacuationWidgetUpdater {
    private var lastLocationUpdateElapsedMillis: Long? = null

    @Synchronized
    fun notifyLocationChanged(context: Context, coordinate: GeoCoordinate) {
        val now = SystemClock.elapsedRealtime()
        lastLocationUpdateElapsedMillis?.let { lastUpdate ->
            if (now - lastUpdate < LOCATION_UPDATE_THROTTLE_MILLIS) return
        }
        lastLocationUpdateElapsedMillis = now

        sendUpdate(context, EvacuationWidgetProvider::class.java, coordinate)
        sendUpdate(context, CompactEvacuationWidgetProvider::class.java, coordinate)
    }

    fun requestUpdate(context: Context) {
        sendUpdate(context, EvacuationWidgetProvider::class.java, coordinate = null)
        sendUpdate(context, CompactEvacuationWidgetProvider::class.java, coordinate = null)
    }

    private fun sendUpdate(
        context: Context,
        providerClass: Class<out BaseEvacuationWidgetProvider>,
        coordinate: GeoCoordinate?,
    ) {
        context.sendBroadcast(
            Intent(context, providerClass).apply {
                action = BaseEvacuationWidgetProvider.ACTION_DATA_CHANGED
                coordinate?.let {
                    putExtra(BaseEvacuationWidgetProvider.EXTRA_LATITUDE, it.latitude)
                    putExtra(BaseEvacuationWidgetProvider.EXTRA_LONGITUDE, it.longitude)
                }
            },
        )
    }

    private const val LOCATION_UPDATE_THROTTLE_MILLIS = 15_000L
}
