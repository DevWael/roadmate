package com.roadmate.core.location

import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.model.DrivingState
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.state.DrivingStateManager
import com.roadmate.core.state.GapRecoveredEvent
import com.roadmate.core.state.TripDetector
import com.roadmate.core.state.TripEndEvent
import com.roadmate.core.util.Clock
import com.roadmate.core.util.CrashRecoveryJournal
import com.roadmate.core.util.HaversineCalculator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRecorder(
    private val locationUpdates: SharedFlow<LocationUpdate>,
    private val drivingStateFlow: StateFlow<DrivingState>,
    private val tripEndEventFlow: SharedFlow<TripEndEvent>,
    private val gapRecoveredEventFlow: SharedFlow<GapRecoveredEvent>,
    private val tripRepository: TripRepository,
    private val vehicleRepository: VehicleRepository,
    private val journal: CrashRecoveryJournal,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val accuracyRampMaxMeters: Float = 50f,
) {
    @Inject constructor(
        gpsTracker: GpsTracker,
        drivingStateManager: DrivingStateManager,
        tripDetector: TripDetector,
        tripRepository: TripRepository,
        vehicleRepository: VehicleRepository,
        journal: CrashRecoveryJournal,
    ) : this(
        locationUpdates = gpsTracker.locations,
        drivingStateFlow = drivingStateManager.drivingState,
        tripEndEventFlow = tripDetector.tripEndEvents,
        gapRecoveredEventFlow = tripDetector.gapRecoveredEvents,
        tripRepository = tripRepository,
        vehicleRepository = vehicleRepository,
        journal = journal,
        clock = Clock.SYSTEM,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        ioDispatcher = Dispatchers.IO,
    )

    private val buffer = ConcurrentLinkedQueue<TripPoint>()
    private val mutex = Mutex()

    @Volatile private var currentTripId: String? = null
    @Volatile private var currentVehicleId: String? = null
    @Volatile private var cityConsumption: Double = 0.0
    @Volatile private var startOdometerKm: Double = 0.0
    @Volatile private var tripStartTime: Long = 0L
    @Volatile private var accumulatedDistanceKm: Double = 0.0
    @Volatile private var maxSpeedKmh: Double = 0.0
    @Volatile private var lastValidLocation: LocationUpdate? = null
    @Volatile private var tripLoaded = false

    @Volatile private var isGapMode = false
    @Volatile private var postGapRecovery = false

    private var flushJob: Job? = null
    private var journalJob: Job? = null

    companion object {
        private const val FLUSH_INTERVAL_MS = 10_000L
        private const val JOURNAL_INTERVAL_MS = 30_000L
    }

    init {
        scope.launch {
            locationUpdates.collect { update -> onLocationUpdate(update) }
        }

        scope.launch {
            drivingStateFlow.collect { state ->
                when (state) {
                    is DrivingState.Driving -> {
                        if (currentTripId == null) {
                            onTripActive(state)
                        } else if (isGapMode) {
                            onGapRecovered()
                        }
                    }
                    is DrivingState.Idle -> onTripIdle()
                    is DrivingState.GapCheck -> onGapEntered()
                    is DrivingState.Stopping -> { }
                }
            }
        }

        scope.launch {
            tripEndEventFlow.collect { event -> finalizeTrip(event) }
        }

        scope.launch {
            gapRecoveredEventFlow.collect { event -> onGapRecoveredEvent(event) }
        }
    }

    private fun onTripActive(state: DrivingState.Driving) {
        if (currentTripId != null) return
        currentTripId = state.tripId
        accumulatedDistanceKm = 0.0
        maxSpeedKmh = 0.0
        lastValidLocation = null
        tripLoaded = false
        isGapMode = false
        postGapRecovery = false
        startFlushTimer()
        Timber.d("TripRecorder: started recording trip ${state.tripId}")
    }

    private fun onTripIdle() {
        flushJob?.cancel()
        flushJob = null
        journalJob?.cancel()
        journalJob = null
    }

    private fun onGapEntered() {
        isGapMode = true
        postGapRecovery = false
        Timber.d("TripRecorder: entered gap mode — distance accumulation paused")
    }

    private fun onGapRecovered() {
        isGapMode = false
        postGapRecovery = true
        Timber.d("TripRecorder: gap recovered — distance will be applied when event arrives")
    }

    private fun onGapRecoveredEvent(event: GapRecoveredEvent) {
        if (event.tripId != currentTripId) return
        if (event.isPlausible && event.gapDistanceKm > 0.0) {
            accumulatedDistanceKm += event.gapDistanceKm
            Timber.d("TripRecorder: added plausible gap distance ${event.gapDistanceKm}km")
        } else if (!event.isPlausible && event.gapDistanceKm > 0.0) {
            lastValidLocation = null
            Timber.w("TripRecorder: discarded implausible gap distance ${event.gapDistanceKm}km (teleport)")
        }
    }

    private suspend fun onLocationUpdate(update: LocationUpdate) {
        mutex.withLock {
            val tripId = currentTripId ?: return

            val speed = update.speedKmh.toDouble()
            if (speed > maxSpeedKmh) maxSpeedKmh = speed

            if (isGapMode) {
                buffer.add(update.toTripPoint(tripId, isGapBoundary = true))
                return
            }

            if (!update.isLowAccuracy && lastValidLocation != null) {
                if (postGapRecovery && update.accuracy >= accuracyRampMaxMeters) {
                    buffer.add(update.toTripPoint(tripId))
                    return
                }
                accumulatedDistanceKm += HaversineCalculator.haversineDistanceKm(
                    lastValidLocation!!.lat, lastValidLocation!!.lng,
                    update.lat, update.lng,
                )
            }

            if (!update.isLowAccuracy) {
                if (postGapRecovery && update.accuracy <= accuracyRampMaxMeters) {
                    postGapRecovery = false
                }
                lastValidLocation = update
            }

            buffer.add(update.toTripPoint(tripId))
        }
    }

    private fun startFlushTimer() {
        flushJob?.cancel()
        journalJob?.cancel()
        flushJob = scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flushBuffer()
            }
        }
        journalJob = scope.launch {
            while (isActive) {
                delay(JOURNAL_INTERVAL_MS)
                writeJournal()
            }
        }
    }

    private suspend fun flushBuffer() {
        mutex.withLock {
            if (currentTripId == null) return
            ensureTripLoaded()
            val points = drainBuffer()
            if (points.isEmpty()) return

            val trip = buildActiveTrip()
            withContext(ioDispatcher) {
                tripRepository.flushTripPointsAndTrip(points, trip)
                    .onFailure { Timber.e(it, "TripRecorder: flush failed") }
            }
        }
    }

    private suspend fun ensureTripLoaded() {
        if (tripLoaded) return
        val tripId = currentTripId ?: return
        val trip = tripRepository.getTrip(tripId).firstOrNull()
        if (trip != null) {
            tripStartTime = trip.startTime
            startOdometerKm = trip.startOdometerKm
            currentVehicleId = trip.vehicleId
            val vehicle = vehicleRepository.getVehicle(trip.vehicleId).firstOrNull()
            cityConsumption = vehicle?.cityConsumption ?: 0.0
            tripLoaded = true
        } else {
            Timber.w("Trip $tripId not yet in DB — will retry on next flush")
            tripStartTime = clock.now()
            startOdometerKm = 0.0
        }
    }

    private fun drainBuffer(): List<TripPoint> {
        val points = mutableListOf<TripPoint>()
        while (buffer.isNotEmpty()) {
            buffer.poll()?.let { points.add(it) }
        }
        return points
    }

    private fun buildActiveTrip(): Trip {
        val now = clock.now()
        val durationMs = maxOf(0L, now - tripStartTime)
        val avgSpeedKmh = if (durationMs > 0) accumulatedDistanceKm / (durationMs / 3_600_000.0) else 0.0
        val estimatedFuelL = accumulatedDistanceKm * (cityConsumption / 100.0)

        return Trip(
            id = currentTripId!!,
            vehicleId = currentVehicleId ?: "",
            startTime = tripStartTime,
            endTime = null,
            distanceKm = accumulatedDistanceKm,
            durationMs = durationMs,
            maxSpeedKmh = maxSpeedKmh,
            avgSpeedKmh = avgSpeedKmh,
            estimatedFuelL = estimatedFuelL,
            startOdometerKm = startOdometerKm,
            endOdometerKm = startOdometerKm,
            status = TripStatus.ACTIVE,
            lastModified = now,
        )
    }

    private fun buildFinalizedTrip(endTime: Long, status: TripStatus): Trip {
        val durationMs = maxOf(0L, endTime - tripStartTime)
        val avgSpeedKmh = if (durationMs > 0) accumulatedDistanceKm / (durationMs / 3_600_000.0) else 0.0
        val estimatedFuelL = accumulatedDistanceKm * (cityConsumption / 100.0)
        val endOdometerKm = startOdometerKm + accumulatedDistanceKm

        return Trip(
            id = currentTripId!!,
            vehicleId = currentVehicleId ?: "",
            startTime = tripStartTime,
            endTime = endTime,
            distanceKm = accumulatedDistanceKm,
            durationMs = durationMs,
            maxSpeedKmh = maxSpeedKmh,
            avgSpeedKmh = avgSpeedKmh,
            estimatedFuelL = estimatedFuelL,
            startOdometerKm = startOdometerKm,
            endOdometerKm = endOdometerKm,
            status = status,
            lastModified = clock.now(),
        )
    }

    private suspend fun finalizeTrip(event: TripEndEvent) {
        mutex.withLock {
            if (currentTripId != event.tripId) return
            ensureTripLoaded()
            val points = drainBuffer()

            val trip = buildFinalizedTrip(event.endTime, event.status)
            withContext(ioDispatcher) {
                val result = if (points.isNotEmpty()) {
                    tripRepository.flushTripPointsAndTrip(points, trip)
                } else {
                    tripRepository.saveTrip(trip)
                }
                result.onFailure { Timber.e(it, "TripRecorder: finalization save failed for trip ${event.tripId}") }

                if (event.status == TripStatus.COMPLETED) {
                    updateVehicleOdometer(trip)
                }
            }

            Timber.d("TripRecorder: finalized trip ${event.tripId}, distance=${accumulatedDistanceKm}km, status=${event.status}")
            currentTripId = null
            lastValidLocation = null
            isGapMode = false
            postGapRecovery = false
        }
    }

    private suspend fun writeJournal() {
        mutex.withLock {
            val tripId = currentTripId ?: return
            val now = clock.now()
            journal.write(
                tripId = tripId,
                vehicleId = currentVehicleId ?: return,
                distanceKm = accumulatedDistanceKm,
                durationMs = maxOf(0L, now - tripStartTime),
                odometerKm = startOdometerKm + accumulatedDistanceKm,
                lastFlushTimestamp = now,
            )
        }
    }

    suspend fun gracefulShutdown() {
        flushJob?.cancel()
        flushJob = null
        journalJob?.cancel()
        journalJob = null
        mutex.withLock {
            val tripId = currentTripId ?: return
            ensureTripLoaded()
            if (!tripLoaded) {
                Timber.w("TripRecorder: trip $tripId not loaded, cannot finalize gracefully")
                return
            }
            val points = drainBuffer()
            val now = clock.now()
            journal.write(
                tripId = tripId,
                vehicleId = currentVehicleId ?: "",
                distanceKm = accumulatedDistanceKm,
                durationMs = maxOf(0L, now - tripStartTime),
                odometerKm = startOdometerKm + accumulatedDistanceKm,
                lastFlushTimestamp = now,
            )
            val trip = buildFinalizedTrip(now, TripStatus.COMPLETED)
            withContext(ioDispatcher) {
                if (points.isNotEmpty()) {
                    tripRepository.flushTripPointsAndTrip(points, trip)
                        .onFailure { Timber.e(it, "TripRecorder: graceful shutdown flush failed") }
                } else {
                    tripRepository.saveTrip(trip)
                        .onFailure { Timber.e(it, "TripRecorder: graceful shutdown save failed") }
                }
                updateVehicleOdometer(trip)
            }
            journal.clear()
            Timber.i("TripRecorder: graceful shutdown, finalized trip $tripId as COMPLETED")
            currentTripId = null
            lastValidLocation = null
            isGapMode = false
            postGapRecovery = false
        }
    }

    private suspend fun updateVehicleOdometer(trip: Trip) {
        val vehicleId = trip.vehicleId
        if (vehicleId.isBlank() || trip.distanceKm <= 0.0 || trip.distanceKm.isNaN() || trip.distanceKm.isInfinite()) return
        vehicleRepository.addToOdometer(vehicleId, trip.distanceKm)
            .onFailure { Timber.e(it, "TripRecorder: failed to update odometer for vehicle $vehicleId") }
    }

    private fun LocationUpdate.toTripPoint(tripId: String, isGapBoundary: Boolean = false) = TripPoint(
        tripId = tripId,
        latitude = lat,
        longitude = lng,
        speedKmh = speedKmh.toDouble(),
        altitude = altitude,
        accuracy = accuracy,
        timestamp = timestamp,
        isGapBoundary = isGapBoundary,
    )
}
