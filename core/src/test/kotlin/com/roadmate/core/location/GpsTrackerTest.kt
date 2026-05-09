package com.roadmate.core.location

import android.location.Location
import app.cash.turbine.test
import com.roadmate.core.model.DrivingState
import com.roadmate.core.state.DrivingStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("GpsTracker")
class GpsTrackerTest {

    private lateinit var fakeLocationProvider: FakeLocationProvider
    private lateinit var drivingStateManager: DrivingStateManager

    @BeforeEach
    fun setUp() {
        fakeLocationProvider = FakeLocationProvider()
        drivingStateManager = DrivingStateManager()
    }

    private fun createLocation(
        lat: Double = 37.7749,
        lng: Double = -122.4194,
        speed: Float = 10f,
        altitude: Double = 50.0,
        accuracy: Float = 10f,
        time: Long = System.currentTimeMillis(),
    ): Location = object : Location("test") {
        override fun getLatitude() = lat
        override fun getLongitude() = lng
        override fun getSpeed() = speed
        override fun getAltitude() = altitude
        override fun getAccuracy() = accuracy
        override fun getTime() = time
    }

    private fun createTracker(): Pair<GpsTracker, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        return GpsTracker(fakeLocationProvider, drivingStateManager, scope) to scope
    }

    @Nested
    @DisplayName("when Idle")
    inner class IdleState {

        @Test
        @DisplayName("does not emit location updates")
        fun doesNotEmitWhenIdle() = runTest {
            val (tracker, scope) = createTracker()
            tracker.locations.test {
                expectNoEvents()
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("does not request location updates from provider")
        fun doesNotRequestUpdatesWhenIdle() = runTest {
            val (tracker, scope) = createTracker()
            assertFalse(fakeLocationProvider.isRequestingUpdates)
            tracker.destroy()
        }
    }

    @Nested
    @DisplayName("when Driving")
    inner class WhenDriving {

        @Test
        @DisplayName("emits location updates converted to LocationUpdate")
        fun emitsLocationUpdatesWhenDriving() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation())
                val update = awaitItem()
                assertEquals(37.7749, update.lat)
                assertEquals(-122.4194, update.lng)
                assertEquals(10f * 3.6f, update.speedKmh)
                assertEquals(50.0, update.altitude)
                assertEquals(10f, update.accuracy)
                assertFalse(update.isLowAccuracy)
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("requests updates with 3-second interval")
        fun requestsUpdatesWith3sInterval() = runTest {
            val (tracker, scope) = createTracker()

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            assertEquals(3000L, fakeLocationProvider.lastRequestedIntervalMs)
            tracker.destroy()
        }

        @Test
        @DisplayName("emits multiple location updates")
        fun emitsMultipleUpdates() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(lat = 1.0))
                assertEquals(1.0, awaitItem().lat)

                fakeLocationProvider.emitLocation(createLocation(lat = 2.0))
                assertEquals(2.0, awaitItem().lat)
            }
            tracker.destroy()
        }
    }

    @Nested
    @DisplayName("dynamic interval switching")
    inner class DynamicSwitching {

        @Test
        @DisplayName("starts tracking when transitioning from Idle to Driving")
        fun startsTrackingOnDrivingTransition() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            assertTrue(fakeLocationProvider.isRequestingUpdates)

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation())
                assertEquals(37.7749, awaitItem().lat)
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("stops tracking when transitioning from Driving to Idle")
        fun stopsTrackingOnIdleTransition() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            assertTrue(fakeLocationProvider.isRequestingUpdates)

            drivingStateManager.updateState(DrivingState.Idle)

            val stopCount = fakeLocationProvider.stopCallCount
            assertTrue(stopCount >= 1, "Expected at least 1 stop call, got $stopCount")
            tracker.destroy()
        }

        @Test
        @DisplayName("restarts tracking when transitioning back to Driving")
        fun restartsTrackingOnSecondDrivingTransition() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            drivingStateManager.updateState(DrivingState.Idle)
            drivingStateManager.updateState(DrivingState.Driving("trip-2", 5.0, 1000L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(lat = 3.0))
                assertEquals(3.0, awaitItem().lat)
            }
            tracker.destroy()
        }
    }

    @Nested
    @DisplayName("accuracy tagging")
    inner class AccuracyTagging {

        @Test
        @DisplayName("tags update as low accuracy when accuracy > 50m")
        fun tagsLowAccuracyWhenAboveThreshold() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(accuracy = 55f))
                val update = awaitItem()
                assertTrue(update.isLowAccuracy)
                assertEquals(55f, update.accuracy)
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("does not tag update as low accuracy when accuracy <= 50m")
        fun doesNotTagLowAccuracyWhenAtOrBelowThreshold() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(accuracy = 50f))
                assertFalse(awaitItem().isLowAccuracy)
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("passes low accuracy updates downstream")
        fun passesLowAccuracyUpdatesDownstream() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(accuracy = 100f))
                val update = awaitItem()
                assertTrue(update.isLowAccuracy)
                assertEquals(100f, update.accuracy)
            }
            tracker.destroy()
        }
    }

    @Nested
    @DisplayName("when Stopping or GapCheck")
    inner class StoppingAndGapCheck {

        @Test
        @DisplayName("keeps GPS active during Stopping state")
        fun keepsGpsActiveDuringStopping() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Stopping(timeSinceStopMs = 5000L))
            assertTrue(fakeLocationProvider.isRequestingUpdates)

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation())
                assertEquals(37.7749, awaitItem().lat)
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("keeps GPS active during GapCheck state")
        fun keepsGpsActiveDuringGapCheck() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.GapCheck(gapDurationMs = 3000L))
            assertTrue(fakeLocationProvider.isRequestingUpdates)

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation())
                assertEquals(37.7749, awaitItem().lat)
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("transitions from Stopping to Idle stops GPS")
        fun stoppingToIdleStopsGps() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Stopping(timeSinceStopMs = 5000L))
            assertTrue(fakeLocationProvider.isRequestingUpdates)

            drivingStateManager.updateState(DrivingState.Idle)
            val stopCount = fakeLocationProvider.stopCallCount
            assertTrue(stopCount >= 1, "Expected at least 1 stop call, got $stopCount")
            tracker.destroy()
        }
    }

    @Nested
    @DisplayName("speed validation")
    inner class SpeedValidation {

        @Test
        @DisplayName("clamps negative speed to zero")
        fun clampsNegativeSpeed() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(speed = -5f))
                assertEquals(0f, awaitItem().speedKmh)
            }
            tracker.destroy()
        }

        @Test
        @DisplayName("clamps NaN speed to zero")
        fun clampsNanSpeed() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(speed = Float.NaN))
                assertEquals(0f, awaitItem().speedKmh)
            }
            tracker.destroy()
        }
    }

    @Nested
    @DisplayName("lifecycle management")
    inner class LifecycleManagement {

        @Test
        @DisplayName("stops location updates on destroy")
        fun stopsLocationUpdatesOnDestroy() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            tracker.destroy()

            val stopCount = fakeLocationProvider.stopCallCount
            assertTrue(stopCount >= 1, "Expected stop to be called on destroy, count=$stopCount")
        }

        @Test
        @DisplayName("speed conversion from m/s to km/h")
        fun speedConversion() = runTest {
            val (tracker, scope) = createTracker()
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            tracker.locations.test {
                fakeLocationProvider.emitLocation(createLocation(speed = 27.78f))
                val update = awaitItem()
                assertEquals(27.78f * 3.6f, update.speedKmh, 0.1f)
            }
            tracker.destroy()
        }
    }

    class FakeLocationProvider : LocationProvider {
        private val _locationUpdates = MutableSharedFlow<Location>(extraBufferCapacity = 64)
        override val locationUpdates: Flow<Location> = _locationUpdates.asSharedFlow()

        var isRequestingUpdates = false
            private set
        var lastRequestedIntervalMs: Long = -1L
            private set
        var stopCallCount = 0
            private set

        override fun requestLocationUpdates(intervalMs: Long) {
            isRequestingUpdates = true
            lastRequestedIntervalMs = intervalMs
        }

        override fun stopLocationUpdates() {
            isRequestingUpdates = false
            stopCallCount++
        }

        suspend fun emitLocation(location: Location) {
            _locationUpdates.emit(location)
        }
    }
}
