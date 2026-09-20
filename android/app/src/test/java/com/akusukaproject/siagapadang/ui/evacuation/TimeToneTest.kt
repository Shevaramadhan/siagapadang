package com.akusukaproject.siagapadang.ui.evacuation

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeToneTest {
    @Test
    fun `zero seconds uses expired state`() {
        assertEquals(TimeTone.EXPIRED, timeTone(remainingSeconds = 0, walkingSeconds = 600))
    }

    @Test
    fun `enough time stays neutral`() {
        assertEquals(TimeTone.ENOUGH, timeTone(remainingSeconds = 1_200, walkingSeconds = 600))
    }

    @Test
    fun `less than a quarter margin is tight`() {
        assertEquals(TimeTone.TIGHT, timeTone(remainingSeconds = 700, walkingSeconds = 600))
    }

    @Test
    fun `walking longer than remaining time is not enough`() {
        assertEquals(TimeTone.NOT_ENOUGH, timeTone(remainingSeconds = 500, walkingSeconds = 600))
    }
}
