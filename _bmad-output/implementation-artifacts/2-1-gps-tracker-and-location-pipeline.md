# Story 2.1: GPS Tracker & Location Pipeline

Status: ready-for-dev

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

- [ ] Task 1: GpsTracker (AC: #1, #2, #3)
  - [ ] Create `core/location/GpsTracker.kt` — collects from LocationProvider, publishes `SharedFlow<LocationUpdate>`
  - [ ] Create `LocationUpdate` data class (lat, lng, speedKmh, altitude, accuracy, timestamp, isLowAccuracy)
  - [ ] Implement dynamic interval switching based on `DrivingStateManager.drivingState`

- [ ] Task 2: Interval strategy (AC: #2, #3)
  - [ ] Idle: cancel location requests or use passive provider
  - [ ] Driving: request 3s interval with `PRIORITY_HIGH_ACCURACY`
  - [ ] Transition latency <100ms via `collectLatest` on DrivingState

- [ ] Task 3: Accuracy tagging (AC: #4)
  - [ ] Tag locations with accuracy >50m as `isLowAccuracy = true`
  - [ ] Pass all updates downstream regardless of accuracy

- [ ] Task 4: Lifecycle management (AC: #6)
  - [ ] Properly remove location callbacks on service stop
  - [ ] Use `callbackFlow` with `awaitClose { removeUpdates() }` pattern

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
### Debug Log References
### Completion Notes List
### File List
