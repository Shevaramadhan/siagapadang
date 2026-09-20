package com.akusukaproject.siagapadang.ui.evacuation

import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialZoneDecisionTest {
    @Test
    fun `accurate location outside recorded zone stops initial navigation`() {
        assertEquals(
            InitialZoneDecision.OUTSIDE_RECORDED_ZONE,
            decideInitialZone(InundationZoneStatus.OutsideRecordedZone, 12f, 35f),
        )
    }

    @Test
    fun `accurate location inside recorded zone starts navigation`() {
        assertEquals(
            InitialZoneDecision.INSIDE_RECORDED_ZONE,
            decideInitialZone(
                InundationZoneStatus.InsideRecordedZone("Zona A", "tinggi"),
                10f,
                35f,
            ),
        )
    }

    @Test
    fun `weak location does not claim user is outside recorded zone`() {
        assertEquals(
            InitialZoneDecision.UNCONFIRMED,
            decideInitialZone(InundationZoneStatus.OutsideRecordedZone, 60f, 35f),
        )
    }

    @Test
    fun `unavailable zone data leaves initial position unconfirmed`() {
        assertEquals(
            InitialZoneDecision.UNCONFIRMED,
            decideInitialZone(InundationZoneStatus.DataUnavailable, 8f, 35f),
        )
    }
}
