package com.akusukaproject.siagapadang.ui.evacuation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvacuationUiStateTest {
    @Test
    fun `classifies gps accuracy for emergency feedback`() {
        assertEquals(LocationQuality.SEARCHING, EvacuationUiState().locationQuality)
        assertEquals(
            LocationQuality.GOOD,
            EvacuationUiState(locationAccuracyMeters = 8f).locationQuality,
        )
        assertEquals(
            LocationQuality.FAIR,
            EvacuationUiState(locationAccuracyMeters = 24f).locationQuality,
        )
        assertEquals(
            LocationQuality.WEAK,
            EvacuationUiState(locationAccuracyMeters = 60f).locationQuality,
        )
    }

    @Test
    fun `evacuation window expires at zero while user has not arrived`() {
        assertFalse(EvacuationUiState(remainingEvacuationSeconds = 1).hasEvacuationWindowExpired)
        assertTrue(EvacuationUiState(remainingEvacuationSeconds = 0).hasEvacuationWindowExpired)
        assertFalse(
            EvacuationUiState(
                remainingEvacuationSeconds = 0,
                hasArrived = true,
            ).hasEvacuationWindowExpired,
        )
        assertFalse(
            EvacuationUiState(
                remainingEvacuationSeconds = 0,
                isOutsideInundationZoneAtStart = true,
            ).hasEvacuationWindowExpired,
        )
    }
}
