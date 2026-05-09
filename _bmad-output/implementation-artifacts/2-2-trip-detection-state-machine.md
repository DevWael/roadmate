# Story 2.2: Trip Detection State Machine

Status: done

## Story

As a driver,
I want the system to automatically detect when I start and stop driving,
so that trips are recorded without me pressing any buttons.

## Acceptance Criteria

1. **Trip start** — Idle→Driving when GPS speed >8 km/h for 3 consecutive readings (9s). New Trip entity created with status `ACTIVE`.

2. **Stop detection** — Driving→Stopping when speed <3 km/h for configurable timeout (default 120s).

3. **Trip end** — Stopping→Idle when full timeout elapses without speed >8 km/h. Trip finalized `COMPLETED`. `endTime` = when vehicle first stopped.

4. **Resume driving** — Stopping→Driving when speed >8 km/h before timeout. Existing trip continues.

5. **Garage drift immunity** — GPS drift with speed <5 km/h and accuracy >30m does NOT create false trips. Zero false trips across 30 days.

6. **Performance** — State transition decision within 100ms per location update.

7. **StateFlow propagation** — All collectors receive new state within 16ms (one frame).

## Tasks / Subtasks

- [x] Task 1: State machine core (AC: #1, #2, #3, #4)
  - [x] Create `core/state/TripDetector.kt`
  - [x] Implement state transitions: Idle→Driving, Driving→Stopping, Stopping→Idle, Stopping→Driving
  - [x] Track consecutive high-speed readings (3 required)
  - [x] Configurable stop timeout (default 120s)
  - [x] Set `endTime` to first-stop timestamp, not timeout-elapsed timestamp

- [x] Task 2: Drift filtering (AC: #5)
  - [x] Ignore speed readings where accuracy >30m AND speed <5 km/h
  - [x] Require speed threshold AND accuracy threshold for trip start

- [x] Task 3: Trip entity creation (AC: #1, #3)
  - [x] On Idle→Driving: create Trip via `TripRepository` with `ACTIVE` status
  - [x] On Stopping→Idle: finalize Trip with `COMPLETED` status
  - [x] Update `DrivingStateManager.drivingState`

- [x] Task 4: Performance verification (AC: #6)
  - [x] Ensure no blocking I/O in state evaluation path
  - [x] Trip creation on background dispatcher

## Dev Notes

### Architecture Compliance

**State machine must be pure logic** — TripDetector evaluates LocationUpdates and produces state transitions. It does NOT directly write to Room — it calls TripRepository.

**Configurable thresholds:**
```kotlin
data class TripDetectionConfig(
    val startSpeedKmh: Double = 8.0,
    val stopSpeedKmh: Double = 3.0,
    val consecutiveReadingsForStart: Int = 3,
    val stopTimeoutMs: Long = 120_000,
    val driftAccuracyThreshold: Float = 30f,
    val driftSpeedThreshold: Double = 5.0
)
```

**endTime precision:** When trip ends, `endTime` = timestamp of the first location reading below `stopSpeedKmh`, NOT the timestamp when the 120s timeout elapsed.

### References

- [Source: architecture.md#Trip Detection State Machine] — State transitions, thresholds
- [Source: architecture.md#GPS Drift Immunity] — Garage drift handling
- [Source: epics.md#Story 2.2] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
glm-5.1 (zai/glm-5.1)

### Debug Log References
- Initial test run: 6 failures due to separate TestDispatcher scheduler and startTime assertion mismatch
- Fix: Used `TestScope.testScheduler` for shared scheduler, `UnconfinedTestDispatcher` for ioDispatcher, tracked first high-speed timestamp for accurate trip startTime

### Completion Notes List
- ✅ Implemented TripDetector as pure state machine with `process(LocationUpdate)` API
- ✅ TripDetectionConfig data class with all configurable thresholds matching spec
- ✅ State transitions: Idle→Driving (3 consecutive >8km/h), Driving→Stopping (<3km/h), Stopping→Idle (timeout), Stopping→Driving (resume)
- ✅ Garage drift immunity: filters readings with speed <5 km/h AND accuracy >30m
- ✅ Trip creation on IO dispatcher (non-blocking), state transitions synchronous (<100ms decision)
- ✅ endTime set to first-stop timestamp (not timeout-elapsed timestamp)
- ✅ startTime set to first high-speed reading timestamp (not confirmation reading)
- ✅ Handles no-active-vehicle edge case: reverts to Idle if no vehicle configured
- ✅ 26 unit tests covering all ACs, edge cases, configurable thresholds, and full lifecycle
- ✅ All existing tests pass (no regressions)

### File List
- `core/src/main/kotlin/com/roadmate/core/state/TripDetectionConfig.kt` (new)
- `core/src/main/kotlin/com/roadmate/core/state/TripDetector.kt` (new)
- `core/src/test/kotlin/com/roadmate/core/state/TripDetectorTest.kt` (new)

### Review Findings

- [x] [Review][Patch] Race between async trip creation and `endTrip` query — fixed: `pendingTrip` local ref eliminates DB race [TripDetector.kt:114-160]
- [x] [Review][Patch] No thread-safety on mutable state fields — fixed: `@MainThread` annotation + KDoc contract [TripDetector.kt:43-46]
- [x] [Review][Patch] Unmanaged CoroutineScope — fixed: documented as process-scoped by Singleton design [TripDetector.kt:39]
- [x] [Review][Patch] `durationMs` never computed on trip finalization — fixed: `endTime - startTime` computed in `endTrip` [TripDetector.kt:141-161]
- [x] [Review][Patch] Optimistic Driving state before vehicle check — fixed: `cachedVehicleId` checked synchronously, no flicker [TripDetector.kt:109-121]
- [x] [Review][Patch] `System.currentTimeMillis()` in endTrip mixes time sources — fixed: injectable `Clock` interface [TripDetector.kt:154]
- [x] [Review][Patch] Float vs Double precision in threshold comparisons — fixed: config thresholds changed to Float [TripDetectionConfig.kt]
- [x] [Review][Patch] Test CoroutineScope never cancelled — fixed: `@AfterEach` cancels `detectorJob` [TripDetectorTest.kt:60]
- [x] [Review][Defer] `resumeDriving` resets distance/duration to zero [TripDetector.kt:106] — deferred, metrics not tracked by TripDetector yet (future story)
- [x] [Review][Defer] `GapCheck` state silently ignored [TripDetector.kt:55] — deferred, GapCheck handling is Story 2-7 scope
- [x] [Review][Defer] 3-consecutive-readings doesn't enforce 9s timing — deferred, timing controlled by GPS interval config not state machine

## Change Log
- 2026-05-09: Implemented Trip Detection State Machine — TripDetector, TripDetectionConfig, and comprehensive tests (26 tests, all ACs covered)
- 2026-05-09: Applied 8 review patches — race fix (pendingTrip), @MainThread, Clock injection, durationMs calc, cached vehicleId, Float config types, test scope cleanup
