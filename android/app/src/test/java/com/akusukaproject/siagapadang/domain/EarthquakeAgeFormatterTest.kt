package com.akusukaproject.siagapadang.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EarthquakeAgeFormatterTest {
    private fun utcMillis(value: String): Long =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)!!.time

    @Test
    fun `umur gempa memakai menit untuk kejadian kurang dari satu jam`() {
        assertEquals(
            "17 menit lalu",
            EarthquakeAgeFormatter.relativeAge(
                isoDateTime = "2026-09-21T08:00:00+00:00",
                eventDate = "",
                eventTime = "",
                nowMillis = utcMillis("2026-09-21T08:17:45+00:00"),
            ),
        )
    }

    @Test
    fun `umur gempa memakai jam untuk kejadian pada hari yang sama`() {
        assertEquals(
            "3 jam lalu",
            EarthquakeAgeFormatter.relativeAge(
                isoDateTime = "2026-09-21T04:00:00Z",
                eventDate = "",
                eventTime = "",
                nowMillis = utcMillis("2026-09-21T07:59:59+00:00"),
            ),
        )
    }

    @Test
    fun `tanggal dan waktu tampilan menjadi cadangan bila ISO kosong`() {
        assertEquals(
            "2 jam lalu",
            EarthquakeAgeFormatter.relativeAge(
                isoDateTime = null,
                eventDate = "21 Sep 2026",
                eventTime = "14:00:00 WIB",
                nowMillis = utcMillis("2026-09-21T09:00:00+00:00"),
            ),
        )
    }

    @Test
    fun `waktu yang tidak dapat dibaca tidak menghasilkan umur palsu`() {
        assertNull(EarthquakeAgeFormatter.relativeAge(null, "", "", nowMillis = 0L))
    }
}
