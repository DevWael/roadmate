package com.roadmate.core.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlatformLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val _locationUpdates = MutableSharedFlow<Location>(replay = 1)
    override val locationUpdates: Flow<Location> = _locationUpdates.asSharedFlow()

    private val isRequesting = AtomicBoolean(false)

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _locationUpdates.tryEmit(location)
        }

        override fun onProviderDisabled(provider: String) {
            Timber.w("GPS provider disabled")
        }

        @Deprecated("Deprecated in API 29+")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun requestLocationUpdates() {
        if (!isRequesting.compareAndSet(false, true)) return
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Timber.e("GPS provider not available or disabled")
            isRequesting.set(false)
            return
        }
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener,
            )
        } catch (e: SecurityException) {
            Timber.e(e, "Location permission not granted")
            isRequesting.set(false)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "GPS provider became unavailable between check and request")
            isRequesting.set(false)
        }
    }

    override fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(listener)
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException while stopping location updates")
        }
        isRequesting.set(false)
    }
}
