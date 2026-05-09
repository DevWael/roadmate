# Story 2.1: GPS Tracker & Location Pipeline

Status: done

## Story

As a developer,
I want a GPS location pipeline that captures position data at configurable intervals with speed and accuracy metadata,
so that the trip detection system and trip recorder have reliable location data to work with.

## Acceptance Criteria

1. **Active tracking interval** — 3-second interval during `Driving` state. Each update: lat, lng, speed (m/s→km/h), altitude, accuracy, timestamp.

2. **Idle conservation** — 0-second interval (no active polling) during `Idle`. Relies on passive/significant motion detection.

3. **Dynamic switching** — Switches to 3s active polling within 100ms of state change to `Driving`.

4. **Low accuracy handling** — Updates with accuracy >50m tagged `isLowAccuracy = true`, still passed to consumers.

5. **Fallback transparency** — `PlatformLocationProvider` delivers same-interval updates if Play Services unavailable.

6. **No memory leaks** — No location callback leaks after 24+ hours of continuous operation.

## Tasks / Subtasks

- [x] Task 1: GpsTracker (AC: #1, #2, #3)
  - [x] Create `core/location/GpsTracker.kt` — collects from LocationProvider, publishes `SharedFlow<LocationUpdate>`
  - [x] Create `LocationUpdate` data class (lat, lng, speedKmh, altitude, accuracy, timestamp, isLowAccuracy)
  - [x] Implement dynamic interval switching based on `DrivingStateManager.drivingState`

- [x] Task 2: Interval strategy (AC: #2, #3)
  - [x] Idle: cancel location requests or use passive provider
  - [x] Driving: request 3s interval with `PRIORITY_HIGH_ACCURACY`
  - [x] Transition latency <100ms via `collectLatest` on DrivingState

- [x] Task 3: Accuracy tagging (AC: #4)
  - [x] Tag locations with accuracy >50m as `isLowAccuracy = true`
  - [x] Pass all updates downstream regardless of accuracy

- [x] Task 4: Lifecycle management (AC: #6)
  - [x] Properly remove location callbacks on service stop
  - [x] Use `callbackFlow` with `awaitClose { removeUpdates() }` pattern

## Dev Notes

### Architecture Compliance

**GpsTracker emits to SharedFlow:**
```kotlin
class GpsTracker @Inject constructor(
    private val locationProvider: LocationProvider,
    private val drivingStateManager: DrivingStateManager
) {
    private val _locations = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 64)
    val locations: SharedFlow<LocationUpdate> = _locations.asSharedFlow()
}
```

**Speed conversion:** Android provides m/s. Convert: `location.speed * 3.6f` = km/h.

**Power budget:** Idle mode = no active GPS = minimal battery drain. Critical for head unit standby.

### References

- [Source: architecture.md#Location Pipeline] — GpsTracker, LocationProvider, interval strategy
- [Source: architecture.md#Performance Requirements] — 100ms state evaluation
- [Source: epics.md#Story 2.1] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
zai/glm-5.1

### Debug Log References
- Fixed `LocationProvider` interface to accept configurable `intervalMs` parameter (default 3000L)
- Fixed inner class naming conflict (`DrivingState` inner test class shadowing the import)
- Fixed `android.location.Location` not mocked in unit tests — enabled `returnDefaultValues` and used anonymous subclass with overridden getters
- Fixed `UncompletedCoroutinesError` in tests — GpsTracker uses separate `CoroutineScope` (injectable via secondary constructor) instead of TestScope
- Fixed `TurbineTimeoutCancellationException` — SharedFlow has replay=0 so collector must be started before emissions

### Completion Notes List
- Created `LocationUpdate` data class with lat, lng, speedKmh, altitude, accuracy, timestamp, isLowAccuracy fields
- Created `GpsTracker` singleton that observes `DrivingStateManager.drivingState` via `collectLatest` for dynamic interval switching
- Driving state → requests 3s interval location updates from provider, converts `Location` to `LocationUpdate` (including speed m/s→km/h conversion), emits to `SharedFlow<LocationUpdate>`
- Idle state → cancels tracking flow which triggers `callbackFlow.awaitClose` to stop provider updates
- Accuracy tagging: `isLowAccuracy = accuracy > 50f`, all updates passed downstream regardless
- Lifecycle: `callbackFlow` with `awaitClose { stopUpdates() }` pattern ensures cleanup; `destroy()` cancels scope + stops provider
- Updated `LocationProvider` interface with `intervalMs` parameter; both `FusedLocationProvider` and `PlatformLocationProvider` now accept configurable intervals (AC #5 fallback transparency)
- Added `returnDefaultValues = true` to core module test options for `android.location.Location` test compatibility
- All 17 new tests pass; full regression suite passes (0 failures)
- GpsTracker uses dual constructor pattern: `@Inject` constructor for Hilt (creates `Dispatchers.Default` scope), primary constructor for testing (injectable scope)

### File List
- `core/src/main/kotlin/com/roadmate/core/location/LocationUpdate.kt` (new)
- `core/src/main/kotlin/com/roadmate/core/location/GpsTracker.kt` (new)
- `core/src/main/kotlin/com/roadmate/core/location/LocationProvider.kt` (modified — added `intervalMs` parameter)
- `core/src/main/kotlin/com/roadmate/core/location/FusedLocationProvider.kt` (modified — uses configurable `intervalMs`)
- `core/src/main/kotlin/com/roadmate/core/location/PlatformLocationProvider.kt` (modified — uses configurable `intervalMs`)
- `core/src/test/kotlin/com/roadmate/core/location/GpsTrackerTest.kt` (new)
- `core/src/test/kotlin/com/roadmate/core/location/LocationUpdateTest.kt` (new)
- `core/src/test/kotlin/com/roadmate/core/location/LocationProviderTest.kt` (modified — updated for new interface)
- `core/build.gradle.kts` (modified — added `testOptions.unitTests.isReturnDefaultValues = true`)

### Review Findings

- [x] [Review][Decision] `Stopping`/`GapCheck` states now keep GPS active — only `Idle` stops tracking [`GpsTracker.kt:44-47`] ✅ fixed
- [x] [Review][Patch] `collectLatest` race eliminated — replaced `callbackFlow` with `try/finally` direct collect; `collectLatest` guarantees sequential cancellation [`GpsTracker.kt:48-55`] ✅ fixed
- [x] [Review][Patch] `destroy()` double-stop eliminated — now only cancels scope; `finally` block handles cleanup [`GpsTracker.kt:64-66`] ✅ fixed
- [x] [Review][Patch] Speed validated — NaN and negative values clamped to 0 before `* 3.6f` conversion [`GpsTracker.kt:70`] ✅ fixed
- [x] [Review][Patch] `SharedFlow` replay aligned — tracker now uses `replay=1` matching provider pattern [`GpsTracker.kt:31`] ✅ fixed
- [x] [Review][Patch] Redundant `callbackFlow`+`launch` removed — direct `collect` from provider flow [`GpsTracker.kt:50-53`] ✅ fixed
- [x] [Review][Defer] AC #3 transition latency <100ms — `collectLatest` is architecturally correct but no test verifies the <100ms constraint — deferred, not testable in unit suite
- [x] [Review][Defer] AC #6 24-hour leak-free — `callbackFlow`+`awaitClose` pattern is correct idiom but no stress/long-running tests — deferred, requires instrumented test
- [x] [Review][Defer] `FusedLocationProvider.HandlerThread` never quit — singleton lifetime, acceptable for foreground service process — deferred, pre-existing
- [x] [Review][Defer] `DrivingStateManager.updateState` is unrestricted public API — deferred, pre-existing
