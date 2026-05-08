package com.roadmate.core.location

import android.location.Location
import android.os.HandlerThread
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FusedLocationProvider @Inject constructor(
    private val fusedClient: FusedLocationProviderClient,
) : LocationProvider {

    private val _locationUpdates = MutableSharedFlow<Location>(replay = 1)
    override val locationUpdates: Flow<Location> = _locationUpdates.asSharedFlow()

    private val isRequesting = AtomicBoolean(false)

    private val handlerThread = HandlerThread("FusedLocationThread").apply { start() }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _locationUpdates.tryEmit(location)
            }
        }
    }

    override fun requestLocationUpdates() {
        if (!isRequesting.compareAndSet(false, true)) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, callback, handlerThread.looper)
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to request fused location updates")
                    isRequesting.set(false)
                }
        } catch (e: SecurityException) {
            Timber.e(e, "Location permission not granted")
            isRequesting.set(false)
        }
    }

    override fun stopLocationUpdates() {
        try {
            fusedClient.removeLocationUpdates(callback)
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException while stopping location updates")
        }
        isRequesting.set(false)
    }
}
