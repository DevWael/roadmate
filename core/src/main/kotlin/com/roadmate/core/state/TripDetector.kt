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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class TripEndEvent(val tripId: String, val endTime: Long, val status: TripStatus = TripStatus.COMPLETED)

data class GapRecoveredEvent(
    val tripId: String,
    val gapDurationMs: Long,
    val gapDistanceKm: Double,
    val isPlausible: Boolean,
)

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

    private var lastLocationTimestamp: Long? = null
    private var lastProcessedLocation: LocationUpdate? = null
    private var gapStartTimestamp: Long? = null
    private var preGapLocation: LocationUpdate? = null
    private var gapTimeoutJob: Job? = null

    private val _tripEndEvents = MutableSharedFlow<TripEndEvent>(extraBufferCapacity = 1)
    val tripEndEvents: SharedFlow<TripEndEvent> = _tripEndEvents.asSharedFlow()

    private val _gapRecoveredEvents = MutableSharedFlow<GapRecoveredEvent>(extraBufferCapacity = 1)
    val gapRecoveredEvents: SharedFlow<GapRecoveredEvent> = _gapRecoveredEvents.asSharedFlow()

    @Volatile
    private var cachedVehicleId: String? = null

    init {
        scope.launch {
            activeVehicleId.collect { cachedVehicleId = it }
        }
    }

    @MainThread
    fun process(update: LocationUpdate) {
        if (isDrift(update)) return

        when (drivingStateManager.drivingState.value) {
            is DrivingState.Idle -> handleIdle(update)
            is DrivingState.Driving -> handleDriving(update)
            is DrivingState.Stopping -> handleStopping(update)
            is DrivingState.GapCheck -> handleGapCheck(update)
        }

        lastProcessedLocation = update
        lastLocationTimestamp = update.timestamp
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
        val lastTs = lastLocationTimestamp
        if (lastTs != null && (update.timestamp - lastTs) >= config.gapThresholdMs) {
            enterGapCheck(update, update.timestamp - lastTs)
            return
        }

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

    private fun handleGapCheck(update: LocationUpdate) {
        val gapStart = gapStartTimestamp ?: run {
            drivingStateManager.updateState(DrivingState.Idle)
            return
        }
        val gapDurationMs = update.timestamp - gapStart

        if (gapDurationMs >= config.gapTimeoutMs) {
            gapTimeoutJob?.cancel()
            gapTimeoutJob = null
            endTripInterrupted(gapStart)
            return
        }

        if (update.speedKmh > config.startSpeedKmh) {
            gapTimeoutJob?.cancel()
            gapTimeoutJob = null
            recoverGapToDriving(update, gapDurationMs)
        } else if (update.speedKmh < config.stopSpeedKmh) {
            gapTimeoutJob?.cancel()
            gapTimeoutJob = null
            recoverGapToStopping(update)
        } else {
            drivingStateManager.updateState(DrivingState.GapCheck(gapDurationMs = gapDurationMs))
        }
    }

    private fun enterGapCheck(update: LocationUpdate, gapDurationMs: Long) {
        preGapLocation = lastProcessedLocation
        gapStartTimestamp = update.timestamp - gapDurationMs
        drivingStateManager.updateState(DrivingState.GapCheck(gapDurationMs = gapDurationMs))

        gapTimeoutJob?.cancel()
        gapTimeoutJob = scope.launch(ioDispatcher) {
            delay(config.gapTimeoutMs - gapDurationMs)
            val tripId = currentTripId ?: return@launch
            val endTs = gapStartTimestamp ?: clock.now()
            drivingStateManager.updateState(DrivingState.Idle)
            _tripEndEvents.emit(TripEndEvent(tripId, endTs, TripStatus.INTERRUPTED))
            resetState()
            Timber.w("Gap timeout: trip $tripId ended as INTERRUPTED (GPS signal lost)")
        }
    }

    private fun recoverGapToDriving(update: LocationUpdate, gapDurationMs: Long) {
        val tripId = currentTripId ?: return
        val preGap = preGapLocation

        val gapDistanceKm = if (preGap != null) {
            com.roadmate.core.util.HaversineCalculator.haversineDistanceKm(
                preGap.lat, preGap.lng, update.lat, update.lng,
            )
        } else 0.0

        val impliedSpeedKmh = if (gapDurationMs > 0) gapDistanceKm / (gapDurationMs / 3_600_000.0) else 0.0
        val isPlausible = impliedSpeedKmh <= config.teleportSpeedKmh

        scope.launch { _gapRecoveredEvents.emit(GapRecoveredEvent(tripId, gapDurationMs, gapDistanceKm, isPlausible)) }

        preGapLocation = null
        gapStartTimestamp = null
        firstStopTimestamp = null
        drivingStateManager.updateState(DrivingState.Driving(tripId, 0.0, 0L))

        Timber.d("Gap recovery → Driving: gapDuration=${gapDurationMs}ms, gapDist=${gapDistanceKm}km, plausible=$isPlausible")
    }

    private fun recoverGapToStopping(update: LocationUpdate) {
        val tripId = currentTripId ?: return

        val preGap = preGapLocation
        val gapDistanceKm = if (preGap != null) {
            com.roadmate.core.util.HaversineCalculator.haversineDistanceKm(
                preGap.lat, preGap.lng, update.lat, update.lng,
            )
        } else 0.0

        val gapStart = gapStartTimestamp ?: update.timestamp
        val gapDurationMs = update.timestamp - gapStart

        val impliedSpeedKmh = if (gapDurationMs > 0) gapDistanceKm / (gapDurationMs / 3_600_000.0) else 0.0
        val isPlausible = impliedSpeedKmh <= config.teleportSpeedKmh

        scope.launch { _gapRecoveredEvents.emit(GapRecoveredEvent(tripId, gapDurationMs, gapDistanceKm, isPlausible)) }

        preGapLocation = null
        gapStartTimestamp = null
        firstStopTimestamp = update.timestamp
        drivingStateManager.updateState(DrivingState.Stopping(timeSinceStopMs = 0L))

        Timber.d("Gap recovery → Stopping: gapDuration=${gapDurationMs}ms")
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
        resetState()
        drivingStateManager.updateState(DrivingState.Idle)

        if (tripId != null) {
            scope.launch {
                _tripEndEvents.emit(TripEndEvent(tripId, endTime))
            }
        }
    }

    private fun endTripInterrupted(endTime: Long) {
        val tripId = currentTripId
        resetState()
        drivingStateManager.updateState(DrivingState.Idle)

        if (tripId != null) {
            scope.launch {
                _tripEndEvents.emit(TripEndEvent(tripId, endTime, TripStatus.INTERRUPTED))
            }
        }
    }

    private fun resetState() {
        currentTripId = null
        firstStopTimestamp = null
        firstHighSpeedTimestamp = null
        consecutiveHighSpeedCount = 0
        lastLocationTimestamp = null
        lastProcessedLocation = null
        gapStartTimestamp = null
        preGapLocation = null
        gapTimeoutJob?.cancel()
        gapTimeoutJob = null
    }
}
