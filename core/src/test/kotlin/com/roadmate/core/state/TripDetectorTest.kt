package com.roadmate.core.state

import app.cash.turbine.test
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.location.LocationUpdate
import com.roadmate.core.model.DrivingState
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.util.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TripDetector")
class TripDetectorTest {

    private lateinit var drivingStateManager: DrivingStateManager
    private lateinit var fakeTripDao: FakeTripDao
    private lateinit var tripRepository: TripRepository
    private lateinit var fakeActiveVehicleId: MutableStateFlow<String?>
    private var detectorJob: Job? = null
    private val fakeClock = Clock { 999L }

    @BeforeEach
    fun setUp() {
        drivingStateManager = DrivingStateManager()
        fakeTripDao = FakeTripDao()
        tripRepository = TripRepository(fakeTripDao)
        fakeActiveVehicleId = MutableStateFlow("vehicle-1")
    }

    @AfterEach
    fun tearDown() {
        detectorJob?.cancel()
    }

    private fun createDetector(
        config: TripDetectionConfig = TripDetectionConfig(
            startSpeedKmh = 8.0f,
            stopSpeedKmh = 3.0f,
            consecutiveReadingsForStart = 3,
            stopTimeoutMs = 5_000,
            driftAccuracyThreshold = 30f,
            driftSpeedThreshold = 5.0f,
            gapThresholdMs = 30_000,
            gapTimeoutMs = 300_000,
            teleportSpeedKmh = 200.0,
        ),
        testScope: TestScope,
    ): TripDetector {
        val job = SupervisorJob()
        detectorJob = job
        return TripDetector(
            config = config,
            drivingStateManager = drivingStateManager,
            tripRepository = tripRepository,
            activeVehicleId = fakeActiveVehicleId,
            scope = CoroutineScope(job + UnconfinedTestDispatcher(testScope.testScheduler)),
            ioDispatcher = UnconfinedTestDispatcher(testScope.testScheduler),
            clock = fakeClock,
        )
    }

    private fun locationUpdate(
        speedKmh: Float = 0f,
        accuracy: Float = 10f,
        timestamp: Long = System.currentTimeMillis(),
        lat: Double = 37.7749,
        lng: Double = -122.4194,
    ) = LocationUpdate(
        lat = lat,
        lng = lng,
        speedKmh = speedKmh,
        altitude = 50.0,
        accuracy = accuracy,
        timestamp = timestamp,
        isLowAccuracy = accuracy > 50f,
    )

    @Nested
    @DisplayName("initial state")
    inner class InitialState {

        @Test
        @DisplayName("starts in Idle state")
        fun startsIdle() = runTest {
            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())
            }
        }
    }

    @Nested
    @DisplayName("Idle -> Driving transition (AC #1)")
    inner class IdleToDriving {

        @Test
        @DisplayName("transitions to Driving after 3 consecutive high-speed readings")
        fun transitionsAfter3ConsecutiveHighSpeed() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 12f))
                detector.process(locationUpdate(speedKmh = 9f))

                val driving = awaitItem()
                assertTrue(driving is DrivingState.Driving)
                assertNotNull((driving as DrivingState.Driving).tripId)
            }
        }

        @Test
        @DisplayName("resets count when a reading is below threshold")
        fun resetsOnLowSpeed() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 12f))
                detector.process(locationUpdate(speedKmh = 2f))
                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 11f))
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("speed exactly at threshold does not trigger")
        fun exactThresholdDoesNotTrigger() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 8f))
                detector.process(locationUpdate(speedKmh = 8f))
                detector.process(locationUpdate(speedKmh = 8f))
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("speed just above threshold triggers after 3 readings")
        fun justAboveThresholdTriggers() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 8.01f))
                detector.process(locationUpdate(speedKmh = 8.01f))
                detector.process(locationUpdate(speedKmh = 8.01f))

                assertTrue(awaitItem() is DrivingState.Driving)
            }
        }
    }

    @Nested
    @DisplayName("Driving -> Stopping transition (AC #2)")
    inner class DrivingToStopping {

        @Test
        @DisplayName("transitions to Stopping when speed drops below threshold")
        fun transitionsToStopping() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 1000L))
                val state = awaitItem()
                assertTrue(state is DrivingState.Stopping)
                assertEquals(0L, (state as DrivingState.Stopping).timeSinceStopMs)
            }
        }

        @Test
        @DisplayName("speed exactly at threshold does not trigger stop")
        fun exactThresholdDoesNotTriggerStop() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 3f))
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("updates timeSinceStopMs as time progresses")
        fun updatesTimeSinceStop() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 1000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                val state = awaitItem()
                assertTrue(state is DrivingState.Stopping)
                assertEquals(2000L, (state as DrivingState.Stopping).timeSinceStopMs)
            }
        }
    }

    @Nested
    @DisplayName("Stopping -> Idle transition (AC #3)")
    inner class StoppingToIdle {

        @Test
        @DisplayName("transitions to Idle after full timeout elapses")
        fun transitionsToIdleAfterTimeout() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 8000L))
                assertEquals(DrivingState.Idle, awaitItem())
            }
        }

        @Test
        @DisplayName("stays in Stopping before timeout")
        fun staysStoppingBeforeTimeout() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 7000L))
                val state = awaitItem()
                assertTrue(state is DrivingState.Stopping)
            }
        }

        @Test
        @DisplayName("sets endTime to first-stop timestamp, not timeout timestamp")
        fun setsEndTimeToFirstStop() = runTest {
            val detector = createDetector(testScope = this)
            var endEvent: TripEndEvent? = null
            val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.tripEndEvents.collect { endEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 8000L))
                assertEquals(DrivingState.Idle, awaitItem())
            }

            assertNotNull(endEvent)
            assertEquals(3000L, endEvent!!.endTime)
            assertEquals(TripStatus.COMPLETED, endEvent!!.status)
            eventJob.cancel()
        }
    }

    @Nested
    @DisplayName("Stopping -> Driving resume (AC #4)")
    inner class StoppingToDriving {

        @Test
        @DisplayName("resumes Driving when speed goes above threshold before timeout")
        fun resumesDriving() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                val drivingState = awaitItem()
                assertTrue(drivingState is DrivingState.Driving)
                val originalTripId = (drivingState as DrivingState.Driving).tripId

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 5000L))
                val resumed = awaitItem()
                assertTrue(resumed is DrivingState.Driving)
                assertEquals(originalTripId, (resumed as DrivingState.Driving).tripId)
            }
        }

        @Test
        @DisplayName("same trip continues after resume")
        fun sameTripContinues() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                val original = awaitItem() as DrivingState.Driving

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 5000L))
                val resumed = awaitItem() as DrivingState.Driving
                assertEquals(original.tripId, resumed.tripId)
            }
        }
    }

    @Nested
    @DisplayName("garage drift immunity (AC #5)")
    inner class DriftImmunity {

        @Test
        @DisplayName("ignores readings with low speed and high accuracy")
        fun ignoresDriftReadings() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                val drift = locationUpdate(speedKmh = 3f, accuracy = 35f)
                detector.process(drift)
                detector.process(drift)
                detector.process(drift)
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("does not trigger trip from sustained drift")
        fun noFalseTripsFromSustainedDrift() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                repeat(1000) {
                    detector.process(locationUpdate(
                        speedKmh = 4f,
                        accuracy = 40f,
                        timestamp = it * 3000L,
                    ))
                }
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("allows readings with good accuracy even at low speed")
        fun allowsGoodAccuracyLowSpeed() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, accuracy = 10f))
                detector.process(locationUpdate(speedKmh = 10f, accuracy = 10f))
                detector.process(locationUpdate(speedKmh = 10f, accuracy = 10f))
                assertTrue(awaitItem() is DrivingState.Driving)
            }
        }

        @Test
        @DisplayName("allows readings above drift speed even with bad accuracy")
        fun allowsHighSpeedBadAccuracy() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, accuracy = 40f))
                detector.process(locationUpdate(speedKmh = 10f, accuracy = 40f))
                detector.process(locationUpdate(speedKmh = 10f, accuracy = 40f))
                assertTrue(awaitItem() is DrivingState.Driving)
            }
        }

        @Test
        @DisplayName("drift at exact threshold boundary is filtered")
        fun driftAtExactBoundary() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 4.99f, accuracy = 31f))
                detector.process(locationUpdate(speedKmh = 4.99f, accuracy = 31f))
                detector.process(locationUpdate(speedKmh = 4.99f, accuracy = 31f))
                expectNoEvents()
            }
        }
    }

    @Nested
    @DisplayName("Trip entity creation (AC #1, #3)")
    inner class TripEntityCreation {

        @Test
        @DisplayName("creates Trip with ACTIVE status on Idle to Driving")
        fun createsActiveTrip() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 5000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 6000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 7000L))
                assertTrue(awaitItem() is DrivingState.Driving)
            }

            val trip = fakeTripDao.trips.values.firstOrNull()
            assertNotNull(trip)
            assertEquals(TripStatus.ACTIVE, trip!!.status)
            assertEquals("vehicle-1", trip.vehicleId)
            assertEquals(5000L, trip.startTime)
        }

        @Test
        @DisplayName("finalizes Trip with COMPLETED status on Stopping to Idle")
        fun finalizesTripCompleted() = runTest {
            val detector = createDetector(testScope = this)
            var endEvent: TripEndEvent? = null
            val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.tripEndEvents.collect { endEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                val driving = awaitItem() as DrivingState.Driving

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 8000L))
                assertEquals(DrivingState.Idle, awaitItem())
            }

            assertNotNull(endEvent)
            assertEquals(3000L, endEvent!!.endTime)
            assertEquals(TripStatus.COMPLETED, endEvent!!.status)
            eventJob.cancel()
        }

        @Test
        @DisplayName("does not create trip when no active vehicle")
        fun noTripWithoutVehicle() = runTest {
            fakeActiveVehicleId.value = null
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                detector.process(locationUpdate(speedKmh = 10f))
                expectNoEvents()
            }

            assertTrue(fakeTripDao.trips.isEmpty())
        }
    }

    @Nested
    @DisplayName("full trip lifecycle")
    inner class FullLifecycle {

        @Test
        @DisplayName("complete trip: Idle -> Driving -> Stopping -> Idle")
        fun completeTrip() = runTest {
            val detector = createDetector(testScope = this)
            var endEvent: TripEndEvent? = null
            val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.tripEndEvents.collect { endEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 20f, timestamp = 3000L))
                detector.process(locationUpdate(speedKmh = 15f, timestamp = 4000L))

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 5000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 1f, timestamp = 10000L))
                assertEquals(DrivingState.Idle, awaitItem())
            }

            assertEquals(1, fakeTripDao.trips.size)
            assertNotNull(endEvent)
            assertEquals(5000L, endEvent!!.endTime)
            eventJob.cancel()
        }

        @Test
        @DisplayName("trip with resume: Idle -> Driving -> Stopping -> Driving -> Stopping -> Idle")
        fun tripWithResume() = runTest {
            val detector = createDetector(testScope = this)
            var endEvent: TripEndEvent? = null
            val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.tripEndEvents.collect { endEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                val firstDriving = awaitItem()
                assertTrue(firstDriving is DrivingState.Driving)
                val tripId = (firstDriving as DrivingState.Driving).tripId

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 4000L))
                val resumed = awaitItem()
                assertTrue(resumed is DrivingState.Driving)
                assertEquals(tripId, (resumed as DrivingState.Driving).tripId)

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 5000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 1f, timestamp = 10000L))
                assertEquals(DrivingState.Idle, awaitItem())
            }

            assertEquals(1, fakeTripDao.trips.size)
            assertNotNull(endEvent)
            eventJob.cancel()
        }

        @Test
        @DisplayName("two consecutive trips use different IDs")
        fun twoConsecutiveTrips() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                val first = awaitItem() as DrivingState.Driving

                detector.process(locationUpdate(speedKmh = 2f, timestamp = 3000L))
                assertTrue(awaitItem() is DrivingState.Stopping)

                detector.process(locationUpdate(speedKmh = 1f, timestamp = 8000L))
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 10000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 11000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 12000L))
                val second = awaitItem() as DrivingState.Driving

                assertTrue(first.tripId != second.tripId)
            }
        }
    }

    @Nested
    @DisplayName("configurable thresholds")
    inner class ConfigurableThresholds {

        @Test
        @DisplayName("respects custom start speed threshold")
        fun customStartSpeed() = runTest {
            val detector = createDetector(
                config = TripDetectionConfig(startSpeedKmh = 20.0f, consecutiveReadingsForStart = 2),
                testScope = this,
            )

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 15f))
                detector.process(locationUpdate(speedKmh = 15f))
                expectNoEvents()

                detector.process(locationUpdate(speedKmh = 21f))
                detector.process(locationUpdate(speedKmh = 21f))
                assertTrue(awaitItem() is DrivingState.Driving)
            }
        }

        @Test
        @DisplayName("respects custom consecutive readings count")
        fun customConsecutiveCount() = runTest {
            val detector = createDetector(
                config = TripDetectionConfig(consecutiveReadingsForStart = 1),
                testScope = this,
            )

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f))
                assertTrue(awaitItem() is DrivingState.Driving)
            }
        }
    }

    @Nested
    @DisplayName("Gap detection (AC #1)")
    inner class GapDetection {

        @Test
        @DisplayName("transitions to GapCheck when GPS gap exceeds threshold while Driving")
        fun transitionsToGapCheckOnGap() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L))
                val gapState = awaitItem()
                assertTrue(gapState is DrivingState.GapCheck)
                assertTrue((gapState as DrivingState.GapCheck).gapDurationMs >= 30_000L)
            }
        }

        @Test
        @DisplayName("does not transition to GapCheck for short gaps")
        fun noGapCheckForShortGaps() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 25000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 28000L))
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("trip continues during GapCheck — same tripId preserved")
        fun tripContinuesDuringGapCheck() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                val driving = awaitItem() as DrivingState.Driving
                val tripId = driving.tripId

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L))
                assertTrue(awaitItem() is DrivingState.GapCheck)

                val trip = fakeTripDao.trips[tripId]
                assertNotNull(trip)
                assertEquals(TripStatus.ACTIVE, trip!!.status)
            }
        }
    }

    @Nested
    @DisplayName("Gap recovery moving (AC #2)")
    inner class GapRecoveryMoving {

        @Test
        @DisplayName("recovers to Driving when GPS returns with speed >8 within 5min")
        fun recoversToDrivingWithHighSpeed() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                val original = awaitItem() as DrivingState.Driving

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L))
                assertTrue(awaitItem() is DrivingState.GapCheck)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 60000L))
                val recovered = awaitItem()
                assertTrue(recovered is DrivingState.Driving)
                assertEquals(original.tripId, (recovered as DrivingState.Driving).tripId)
            }
        }

        @Test
        @DisplayName("emits GapRecoveredEvent with plausible distance")
        fun emitsGapRecoveredEvent() = runTest {
            val detector = createDetector(testScope = this)
            var gapEvent: GapRecoveredEvent? = null
            val gapJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.gapRecoveredEvents.collect { gapEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L, lat = 37.7749, lng = -122.4194))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L, lat = 37.7749, lng = -122.4194))
                assertTrue(awaitItem() is DrivingState.GapCheck)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 60000L, lat = 37.7849, lng = -122.4194))
                assertTrue(awaitItem() is DrivingState.Driving)
            }

            assertNotNull(gapEvent)
            assertTrue(gapEvent!!.gapDistanceKm > 0)
            assertTrue(gapEvent!!.isPlausible)
            gapJob.cancel()
        }
    }

    @Nested
    @DisplayName("Gap recovery stopped (AC #3)")
    inner class GapRecoveryStopped {

        @Test
        @DisplayName("recovers to Stopping when GPS returns with speed <3 within 5min")
        fun recoversToStoppingWithLowSpeed() = runTest {
            val detector = createDetector(testScope = this)

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L))
                assertTrue(awaitItem() is DrivingState.GapCheck)

                detector.process(locationUpdate(speedKmh = 1f, timestamp = 60000L))
                assertTrue(awaitItem() is DrivingState.Stopping)
            }
        }
    }

    @Nested
    @DisplayName("Gap timeout (AC #4)")
    inner class GapTimeout {

        @Test
        @DisplayName("ends trip as INTERRUPTED when gap exceeds 5min via location update")
        fun endsTripInterruptedOnGapTimeout() = runTest {
            val detector = createDetector(testScope = this)
            var endEvent: TripEndEvent? = null
            val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.tripEndEvents.collect { endEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L))
                assertTrue(awaitItem() is DrivingState.GapCheck)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 340000L))
                assertEquals(DrivingState.Idle, awaitItem())
            }

            assertNotNull(endEvent)
            assertEquals(TripStatus.INTERRUPTED, endEvent!!.status)
            eventJob.cancel()
        }
    }

    @Nested
    @DisplayName("Teleport detection (AC #5)")
    inner class TeleportDetection {

        @Test
        @DisplayName("marks gap as implausible when implied speed >200 km/h")
        fun marksImplausibleOnTeleport() = runTest {
            val detector = createDetector(testScope = this)
            var gapEvent: GapRecoveredEvent? = null
            val gapJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.gapRecoveredEvents.collect { gapEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L, lat = 37.7749, lng = -122.4194))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L, lat = 37.7749, lng = -122.4194))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L, lat = 37.7749, lng = -122.4194))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L, lat = 37.7749, lng = -122.4194))
                assertTrue(awaitItem() is DrivingState.GapCheck)

                detector.process(locationUpdate(
                    speedKmh = 10f,
                    timestamp = 60000L,
                    lat = 40.7128,
                    lng = -74.0060,
                ))
                assertTrue(awaitItem() is DrivingState.Driving)
            }

            assertNotNull(gapEvent)
            assertTrue(gapEvent!!.gapDistanceKm > 0)
            assertEquals(false, gapEvent!!.isPlausible)
            gapJob.cancel()
        }

        @Test
        @DisplayName("plausible distance within 200 km/h is accepted")
        fun plausibleDistanceAccepted() = runTest {
            val detector = createDetector(testScope = this)
            var gapEvent: GapRecoveredEvent? = null
            val gapJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                detector.gapRecoveredEvents.collect { gapEvent = it }
            }

            drivingStateManager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 0L, lat = 37.7749, lng = -122.4194))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 1000L))
                detector.process(locationUpdate(speedKmh = 10f, timestamp = 2000L))
                assertTrue(awaitItem() is DrivingState.Driving)

                detector.process(locationUpdate(speedKmh = 10f, timestamp = 35000L))
                assertTrue(awaitItem() is DrivingState.GapCheck)

                detector.process(locationUpdate(
                    speedKmh = 10f,
                    timestamp = 40000L,
                    lat = 37.7849,
                    lng = -122.4194,
                ))
                assertTrue(awaitItem() is DrivingState.Driving)
            }

            assertNotNull(gapEvent)
            assertTrue(gapEvent!!.isPlausible)
            gapJob.cancel()
        }
    }
}

private class FakeTripDao : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    val tripPoints = mutableMapOf<String, TripPoint>()
    var shouldThrow = false

    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    private val tripPointFlow = MutableStateFlow<List<TripPoint>>(emptyList())

    fun updateFlow() {
        tripFlow.value = trips.values.toList()
        tripPointFlow.value = tripPoints.values.toList()
    }

    override fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripFlow.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.startTime } }

    override fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> =
        tripPointFlow.map { list -> list.filter { it.tripId == tripId }.sortedBy { it.timestamp } }

    override fun getActiveTrip(vehicleId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.vehicleId == vehicleId && it.status == TripStatus.ACTIVE } }

    override fun getTrip(tripId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.id == tripId } }

    override suspend fun upsertTrip(trip: Trip) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips[trip.id] = trip
        updateFlow()
    }

    override suspend fun upsertTrips(trips: List<Trip>) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.forEach { this.trips[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteTrip(trip: Trip) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.remove(trip.id)
        updateFlow()
    }

    override suspend fun deleteTripById(tripId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.remove(tripId)
        updateFlow()
    }

    override suspend fun upsertTripPoint(tripPoint: TripPoint) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints[tripPoint.id] = tripPoint
        updateFlow()
    }

    override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints.forEach { this.tripPoints[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteTripPoint(tripPoint: TripPoint) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints.remove(tripPoint.id)
        updateFlow()
    }

    override suspend fun getTripsModifiedSince(since: Long): List<Trip> =
        trips.values.filter { it.lastModified > since }

    override suspend fun getTripPointsModifiedSince(since: Long): List<TripPoint> =
        tripPoints.values.filter { it.lastModified > since }

    override suspend fun getTripById(id: String): Trip? = null

    override suspend fun getTripPointById(id: String): TripPoint? = null
}
