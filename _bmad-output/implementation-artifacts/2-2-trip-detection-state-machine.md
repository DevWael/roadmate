# Story 2.2: Trip Detection State Machine

Status: ready-for-dev

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

- [ ] Task 1: State machine core (AC: #1, #2, #3, #4)
  - [ ] Create `core/state/TripDetector.kt`
  - [ ] Implement state transitions: Idle→Driving, Driving→Stopping, Stopping→Idle, Stopping→Driving
  - [ ] Track consecutive high-speed readings (3 required)
  - [ ] Configurable stop timeout (default 120s)
  - [ ] Set `endTime` to first-stop timestamp, not timeout-elapsed timestamp

- [ ] Task 2: Drift filtering (AC: #5)
  - [ ] Ignore speed readings where accuracy >30m AND speed <5 km/h
  - [ ] Require speed threshold AND accuracy threshold for trip start

- [ ] Task 3: Trip entity creation (AC: #1, #3)
  - [ ] On Idle→Driving: create Trip via `TripRepository` with `ACTIVE` status
  - [ ] On Stopping→Idle: finalize Trip with `COMPLETED` status
  - [ ] Update `DrivingStateManager.drivingState`

- [ ] Task 4: Performance verification (AC: #6)
  - [ ] Ensure no blocking I/O in state evaluation path
  - [ ] Trip creation on background dispatcher

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
### Debug Log References
### Completion Notes List
### File List
