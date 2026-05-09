package com.roadmate.core.location

import android.location.Location
import com.roadmate.core.model.DrivingState
import com.roadmate.core.state.DrivingStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsTracker(
    private val locationProvider: LocationProvider,
    private val drivingStateManager: DrivingStateManager,
    private val scope: CoroutineScope,
) {
    @Inject constructor(
        locationProvider: LocationProvider,
        drivingStateManager: DrivingStateManager,
    ) : this(locationProvider, drivingStateManager, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _locations = MutableSharedFlow<LocationUpdate>(replay = 1, extraBufferCapacity = 64)
    val locations: SharedFlow<LocationUpdate> = _locations.asSharedFlow()

    companion object {
        private const val LOW_ACCURACY_THRESHOLD_METERS = 50f
        private const val DRIVING_INTERVAL_MS = 3000L
    }

    init {
        scope.launch {
            drivingStateManager.drivingState
                .collectLatest { state ->
                    when (state) {
                        is DrivingState.Driving,
                        is DrivingState.Stopping,
                        is DrivingState.GapCheck -> {
                            Timber.d("GPS tracking active at ${DRIVING_INTERVAL_MS}ms interval")
                            locationProvider.requestLocationUpdates(DRIVING_INTERVAL_MS)
                            try {
                                locationProvider.locationUpdates.collect { location ->
                                    _locations.emit(location.toLocationUpdate())
                                }
                            } finally {
                                Timber.d("GPS tracking stopped")
                                locationProvider.stopLocationUpdates()
                            }
                        }
                        DrivingState.Idle -> {
                            Timber.d("GPS tracking idle — no active polling")
                        }
                    }
                }
        }
    }

    fun destroy() {
        scope.cancel()
    }

    private fun Location.toLocationUpdate(): LocationUpdate {
        val rawSpeed = this.speed
        val speedKmh = if (rawSpeed.isNaN() || rawSpeed < 0f) 0f else rawSpeed * 3.6f
        val accuracy = this.accuracy
        return LocationUpdate(
            lat = latitude,
            lng = longitude,
            speedKmh = speedKmh,
            altitude = altitude,
            accuracy = accuracy,
            timestamp = time,
            isLowAccuracy = accuracy > LOW_ACCURACY_THRESHOLD_METERS,
        )
    }
}
