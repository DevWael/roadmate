package com.roadmate.core.location

import kotlinx.coroutines.flow.Flow
import android.location.Location

interface LocationProvider {
    fun requestLocationUpdates(intervalMs: Long = 3000L)
    fun stopLocationUpdates()
    val locationUpdates: Flow<Location>
}
