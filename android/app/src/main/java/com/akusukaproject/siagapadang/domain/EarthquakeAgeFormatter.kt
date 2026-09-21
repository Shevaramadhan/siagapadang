package com.akusukaproject.siagapadang.domain

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/** Mengubah waktu kejadian BMKG menjadi keterangan ringkas yang mudah dipahami. */
object EarthquakeAgeFormatter {
    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    fun relativeAge(
        isoDateTime: String?,
        eventDate: String,
        eventTime: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): String? {
        val eventMillis = parseIso(isoDateTime)
            ?: parseDisplayTime(eventDate, eventTime)
            ?: return null
        val elapsed = max(0L, nowMillis - eventMillis)
        return when {
            elapsed < MINUTE_MILLIS -> "baru saja"
            elapsed < HOUR_MILLIS -> "${elapsed / MINUTE_MILLIS} menit lalu"
            elapsed < DAY_MILLIS -> "${elapsed / HOUR_MILLIS} jam lalu"
            else -> "${elapsed / DAY_MILLIS} hari lalu"
        }
    }

    private fun parseIso(raw: String?): Long? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    if (pattern.endsWith("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()
        }
    }

    private fun parseDisplayTime(eventDate: String, eventTime: String): Long? {
        val value = "$eventDate $eventTime".trim()
        if (eventDate.isBlank() || eventTime.isBlank()) return null
        val patterns = listOf(
            "dd MMM yyyy HH:mm:ss z",
            "dd MMMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm z",
            "dd MMMM yyyy HH:mm z",
        )
        val locale = Locale.forLanguageTag("id-ID")
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, locale).apply { isLenient = false }.parse(value)?.time
            }.getOrNull()
        }
    }
}
