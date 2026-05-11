package com.roadmate.core.state

import androidx.annotation.MainThread
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.location.LocationUpdate
import com.roadmate.core.model.DrivingState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.util.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class TripEndEvent(val tripId: String, val endTime: Long)

/**
 * Detects trip start/stop based on GPS speed readings.
 *
 * This class is NOT thread-safe. [process] must be called from a single
 * thread (recommended: main thread via the service's location callback).
 *
 * The internal [scope] is process-scoped by design — as a [Singleton], it
 * lives for the entire process lifetime and does not require cancellation.
 */
@Singleton
class TripDetector(
    private val config: TripDetectionConfig,
    private val drivingStateManager: DrivingStateManager,
    private val tripRepository: TripRepository,
    private val activeVehicleId: Flow<String?>,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
) {
    @Inject constructor(
        drivingStateManager: DrivingStateManager,
        tripRepository: TripRepository,
        activeVehicleRepository: ActiveVehicleRepository,
    ) : this(
        config = TripDetectionConfig(),
        drivingStateManager = drivingStateManager,
        tripRepository = tripRepository,
        activeVehicleId = activeVehicleRepository.activeVehicleId,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        ioDispatcher = Dispatchers.IO,
        clock = Clock.SYSTEM,
    )

    private var consecutiveHighSpeedCount = 0
    private var firstHighSpeedTimestamp: Long? = null
    private var firstStopTimestamp: Long? = null
    private var currentTripId: String? = null

    private val _tripEndEvents = MutableSharedFlow<TripEndEvent>(extraBufferCapacity = 1)
    val tripEndEvents: SharedFlow<TripEndEvent> = _tripEndEvents.asSharedFlow()

    /** Cached vehicle ID — collected from [activeVehicleId] flow to allow synchronous checks. */
    @Volatile
    private var cachedVehicleId: String? = null

    init {
        scope.launch {
            activeVehicleId.collect { cachedVehicleId = it }
        }
    }

    /**
     * Process a single location update through the state machine.
     *
     * Must be called from a single thread — this method is not thread-safe.
     */
    @MainThread
    fun process(update: LocationUpdate) {
        if (isDrift(update)) return

        when (drivingStateManager.drivingState.value) {
            is DrivingState.Idle -> handleIdle(update)
            is DrivingState.Driving -> handleDriving(update)
            is DrivingState.Stopping -> handleStopping(update)
            is DrivingState.GapCheck -> { }
        }
    }

    private fun isDrift(update: LocationUpdate): Boolean {
        return update.speedKmh < config.driftSpeedThreshold &&
                update.accuracy > config.driftAccuracyThreshold
    }

    private fun handleIdle(update: LocationUpdate) {
        if (update.speedKmh > config.startSpeedKmh) {
            if (consecutiveHighSpeedCount == 0) {
                firstHighSpeedTimestamp = update.timestamp
            }
            consecutiveHighSpeedCount++
            if (consecutiveHighSpeedCount >= config.consecutiveReadingsForStart) {
                startTrip(firstHighSpeedTimestamp ?: update.timestamp)
                consecutiveHighSpeedCount = 0
                firstHighSpeedTimestamp = null
            }
        } else {
            consecutiveHighSpeedCount = 0
            firstHighSpeedTimestamp = null
        }
    }

    private fun handleDriving(update: LocationUpdate) {
        if (update.speedKmh < config.stopSpeedKmh) {
            firstStopTimestamp = update.timestamp
            drivingStateManager.updateState(DrivingState.Stopping(timeSinceStopMs = 0L))
        }
    }

    private fun handleStopping(update: LocationUpdate) {
        if (update.speedKmh > config.startSpeedKmh) {
            resumeDriving()
            return
        }

        val stopTime = firstStopTimestamp ?: update.timestamp
        val elapsed = update.timestamp - stopTime
        if (elapsed >= config.stopTimeoutMs) {
            endTrip(stopTime)
        } else {
            drivingStateManager.updateState(DrivingState.Stopping(timeSinceStopMs = elapsed))
        }
    }

    private fun resumeDriving() {
        val tripId = currentTripId ?: return
        firstStopTimestamp = null
        drivingStateManager.updateState(DrivingState.Driving(tripId, 0.0, 0L))
    }

    private fun startTrip(startTime: Long) {
        val vehicleId = cachedVehicleId
        if (vehicleId == null) {
            Timber.w("No active vehicle — cannot create trip")
            return
        }

        val tripId = UUID.randomUUID().toString()
        currentTripId = tripId

        val trip = Trip(
            id = tripId,
            vehicleId = vehicleId,
            startTime = startTime,
            distanceKm = 0.0,
            durationMs = 0L,
            maxSpeedKmh = 0.0,
            avgSpeedKmh = 0.0,
            estimatedFuelL = 0.0,
            startOdometerKm = 0.0,
            endOdometerKm = 0.0,
            status = TripStatus.ACTIVE,
        )
        drivingStateManager.updateState(DrivingState.Driving(tripId, 0.0, 0L))

        scope.launch(ioDispatcher) {
            tripRepository.saveTrip(trip)
                .onSuccess { Timber.d("Trip created: $tripId") }
                .onFailure { Timber.e(it, "Failed to create trip: $tripId") }
        }
    }

    private fun endTrip(endTime: Long) {
        val tripId = currentTripId
        currentTripId = null
        firstStopTimestamp = null
        consecutiveHighSpeedCount = 0
        drivingStateManager.updateState(DrivingState.Idle)

        if (tripId != null) {
            scope.launch {
                _tripEndEvents.emit(TripEndEvent(tripId, endTime))
            }
        }
    }
}
