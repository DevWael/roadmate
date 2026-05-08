package com.roadmate.core.location

import kotlinx.coroutines.flow.Flow
import android.location.Location

interface LocationProvider {
    fun requestLocationUpdates()
    fun stopLocationUpdates()
    val locationUpdates: Flow<Location>
}
