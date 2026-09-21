package com.akusukaproject.siagapadang.ui.evacuation

import com.akusukaproject.siagapadang.data.model.InundationZoneStatus

internal enum class InitialZoneDecision {
    OUTSIDE_RECORDED_ZONE,
    INSIDE_RECORDED_ZONE,
    UNCONFIRMED,
}

internal fun decideInitialZone(
    status: InundationZoneStatus,
    accuracyMeters: Float?,
    maximumAccuracyMeters: Float,
): InitialZoneDecision {
    if (accuracyMeters == null || accuracyMeters > maximumAccuracyMeters) {
        return InitialZoneDecision.UNCONFIRMED
    }
    return when (status) {
        InundationZoneStatus.DataUnavailable -> InitialZoneDecision.UNCONFIRMED
        InundationZoneStatus.OutsideRecordedZone -> InitialZoneDecision.OUTSIDE_RECORDED_ZONE
        is InundationZoneStatus.InsideRecordedZone -> InitialZoneDecision.INSIDE_RECORDED_ZONE
    }
}

internal fun shouldAwaitAccurateOutsideZone(
    status: InundationZoneStatus,
    accuracyMeters: Float?,
    maximumAccuracyMeters: Float,
): Boolean = status == InundationZoneStatus.OutsideRecordedZone &&
    decideInitialZone(status, accuracyMeters, maximumAccuracyMeters) == InitialZoneDecision.UNCONFIRMED
