package com.roadmate.headunit.ui.adaptive

enum class DashboardBreakpoint(
    val showTrips: Boolean,
    val showFullAlert: Boolean,
    val showTimeInDriving: Boolean,
    val showTripDistance: Boolean,
    val showMaintenance: Boolean,
) {
    Full(
        showTrips = true,
        showFullAlert = true,
        showTimeInDriving = true,
        showTripDistance = true,
        showMaintenance = true,
    ),
    Compact(
        showTrips = false,
        showFullAlert = false,
        showTimeInDriving = false,
        showTripDistance = true,
        showMaintenance = true,
    ),
    Narrow(
        showTrips = false,
        showFullAlert = false,
        showTimeInDriving = false,
        showTripDistance = false,
        showMaintenance = true,
    ),
    ;

    companion object {
        fun fromWidthDp(widthDp: Float): DashboardBreakpoint = when {
            widthDp >= 960f -> Full
            widthDp >= 480f -> Compact
            else -> Narrow
        }
    }
}
