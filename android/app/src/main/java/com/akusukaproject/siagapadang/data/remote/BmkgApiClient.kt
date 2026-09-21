package com.akusukaproject.siagapadang.data.remote

import com.akusukaproject.siagapadang.data.model.BmkgStatus
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.RegionalEarthquake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BmkgApiClient(
    private val baseUrl: String,
) {
    suspend fun getLatestStatus(): BmkgStatus = withContext(Dispatchers.IO) {
        check(baseUrl.isNotBlank()) { "Alamat backend belum dikonfigurasi." }
        val endpoint = "${baseUrl.trimEnd('/')}/api/v1/status/bmkg"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS

            val responseCode = connection.responseCode
            val responseBody = (
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
                )?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                val detail = runCatching { JSONObject(responseBody).optString("detail") }.getOrNull()
                error(detail?.takeIf { it.isNotBlank() } ?: "Backend tidak tersedia ($responseCode).")
            }
            parseStatus(JSONObject(responseBody))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseStatus(json: JSONObject): BmkgStatus = BmkgStatus(
        eventDate = json.optString("tanggal"),
        eventTime = json.optString("jam"),
        magnitude = json.optString("magnitude"),
        depth = json.optString("kedalaman"),
        region = json.optString("wilayah"),
        potential = json.optString("potensi"),
        felt = json.optString("dirasakan"),
        hasTsunamiPotential = json.optBoolean("is_tsunami_potential", false),
        isStale = json.optString("data_status") == "stale",
        fetchedAt = json.optString("fetched_at"),
        source = json.optString("source", "BMKG"),
        epicenter = parseEpicenter(json.optString("coordinates")),
        regionalEvent = parseRegionalEvent(json.optJSONObject("regional_event")),
        regionalDataStatus = json.optString("regional_data_status").takeIf { it.isNotBlank() },
        isoDateTime = json.optString("datetime").takeIf { it.isNotBlank() },
    )

    private fun parseRegionalEvent(json: JSONObject?): RegionalEarthquake? {
        json ?: return null
        return RegionalEarthquake(
            eventDate = json.optString("tanggal"),
            eventTime = json.optString("jam"),
            magnitude = json.optString("magnitude"),
            depth = json.optString("kedalaman"),
            region = json.optString("wilayah"),
            potential = json.optString("potensi"),
            isoDateTime = json.optString("datetime").takeIf { it.isNotBlank() },
            distanceKmFromPadang = json.optDouble("distance_km_from_padang", 0.0),
            epicenter = parseEpicenter(json.optString("coordinates")),
        )
    }

    /** BMKG mengirim "coordinates" sebagai "lintang,bujur", misalnya "-8.05,120.86". */
    private fun parseEpicenter(raw: String?): GeoCoordinate? {
        val parts = raw?.split(",") ?: return null
        if (parts.size != 2) return null
        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return GeoCoordinate(latitude, longitude)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 1_000
        const val READ_TIMEOUT_MILLIS = 2_000
    }
}
