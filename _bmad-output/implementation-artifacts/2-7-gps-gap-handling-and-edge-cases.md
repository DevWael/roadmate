# Story 2.7: GPS Gap Handling & Edge Cases

Status: done

## Story

As a driver,
I want the system to handle tunnels, garages, and GPS signal gaps intelligently,
so that my trip isn't incorrectly split or ended when I temporarily lose signal.

## Acceptance Criteria

1. **Gap detection** — Driving + no GPS >30s → `GapCheck(gapDuration)`. Trip continues, distance pauses, duration accumulates.

2. **Gap recovery (moving)** — GPS returns <5min with speed >8 km/h → back to `Driving`. Add plausible gap distance.

3. **Gap recovery (stopped)** — GPS returns <5min with speed <3 km/h → `Stopping`.

4. **Gap timeout** — >5min no GPS → end trip with last valid point, status `INTERRUPTED` reason "GPS signal lost".

5. **Teleport detection** — Straight-line distance implies >200 km/h → gap distance discarded. Points persisted but flagged.

6. **Garage exit** — Accuracy improving from >50m to <10m: only ≤50m points used for distance. No false distance spike.

## Tasks / Subtasks

- [x] Task 1: GapCheck state (AC: #1)
  - [x] Add GapCheck handling to TripDetector
  - [x] Track gap start time, continue duration timer, pause distance accumulation

- [x] Task 2: Gap recovery (AC: #2, #3)
  - [x] GPS returns with speed >8 → resume Driving, calculate plausible gap distance
  - [x] GPS returns with speed <3 → transition to Stopping

- [x] Task 3: Gap timeout (AC: #4)
  - [x] 5-minute timer on GapCheck state
  - [x] Finalize trip as INTERRUPTED with reason

- [x] Task 4: Teleport detection (AC: #5)
  - [x] Calculate implied speed across gap: distance / elapsed time
  - [x] If >200 km/h: discard gap distance, flag boundary points

- [x] Task 5: Accuracy ramping (AC: #6)
  - [x] After gap recovery, filter initial low-accuracy readings from distance calculation
  - [x] Only count points with accuracy ≤50m

## Dev Notes

### Architecture Compliance

**GapCheck is part of DrivingState:** Already defined in Story 1.4 as `GapCheck(gapDurationMs: Long)`.

**Plausible gap distance check:**
```kotlin
val impliedSpeedKmh = gapDistanceKm / (gapDurationMs / 3_600_000.0)
val isPlausible = impliedSpeedKmh <= 200.0
if (isPlausible) trip.distanceKm += gapDistanceKm
```

**TripPoint gap flags:** Add `isGapBoundary: Boolean` field to TripPoint (consider if this needs schema migration or was planned in 1.3).

### References

- [Source: architecture.md#GPS Gap Handling] — Gap state machine, teleport detection
- [Source: architecture.md#Accuracy Handling] — Accuracy threshold rules
- [Source: epics.md#Story 2.7] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
GLM-5.1

### Debug Log References
No issues encountered during implementation.

### Completion Notes List
- Implemented GapCheck state machine in TripDetector with gap detection (>30s), recovery (moving/stopped), and timeout (>5min)
- Added `TripEndEvent.status` field to support INTERRUPTED trips alongside COMPLETED
- Added `GapRecoveredEvent` to communicate gap distance/plausibility from TripDetector to TripRecorder
- Added `isGapBoundary` field to TripPoint entity with Room DB migration (v1→v2)
- TripRecorder pauses distance during GapCheck, adds plausible gap distance on recovery, discards implausible (teleport) distance
- Post-gap accuracy ramping filters points with accuracy >50m from distance calculation until accuracy improves
- All existing tests continue to pass; added new test coverage for all 6 ACs

### File List
- `core/src/main/kotlin/com/roadmate/core/state/TripDetector.kt` — Added GapCheck handling, gap detection, recovery, timeout, teleport detection
- `core/src/main/kotlin/com/roadmate/core/state/TripDetectionConfig.kt` — Added gap config fields (gapThresholdMs, gapTimeoutMs, teleportSpeedKmh, accuracyRampMaxMeters)
- `core/src/main/kotlin/com/roadmate/core/location/TripRecorder.kt` — Added gap mode handling, plausible distance, accuracy ramping, INTERRUPTED finalization
- `core/src/main/kotlin/com/roadmate/core/database/entity/TripPoint.kt` — Added isGapBoundary field
- `core/src/main/kotlin/com/roadmate/core/database/RoadMateDatabase.kt` — Bumped version to 2, added MIGRATION_1_2
- `core/src/main/kotlin/com/roadmate/core/di/DatabaseModule.kt` — Added migration to Room builder
- `core/src/main/kotlin/com/roadmate/core/model/sync/TripPointSyncDto.kt` — Added isGapBoundary field
- `core/src/test/kotlin/com/roadmate/core/state/TripDetectorTest.kt` — Added gap detection, recovery, timeout, teleport tests
- `core/src/test/kotlin/com/roadmate/core/location/TripRecorderTest.kt` — Added gap mode, plausible distance, accuracy ramping, INTERRUPTED tests
- `core/src/test/kotlin/com/roadmate/core/database/entity/TripPointTest.kt` — Added isGapBoundary tests

### Review Findings

- [x] [Review][Patch] `resetState()` does not cancel `gapTimeoutJob` before nulling — orphaned coroutine can emit duplicate TripEndEvent [TripDetector.kt:303-310]
- [x] [Review][Patch] Race between `GapRecoveredEvent` and `DrivingState.Driving` flow collection — pending gap data may be zero when `onGapRecovered()` fires [TripRecorder.kt:100-123]
- [x] [Review][Patch] `enterGapCheck` saves post-gap location as `preGapLocation` instead of last-known pre-gap location [TripDetector.kt:175-176]
- [x] [Review][Patch] `firstHighSpeedTimestamp` not cleared in `resetState()` — stale timestamp leaks to next trip [TripDetector.kt:303-310]
- [x] [Review][Patch] `gracefulShutdown()` does not reset gap state (`isGapMode`, `postGapRecovery`, pending gap fields) [TripRecorder.kt:353-391]
- [x] [Review][Patch] Accuracy ramping uses `>` instead of `>=` — off-by-one vs spec "≤50m points used for distance" [TripRecorder.kt:185]
- [x] [Review][Patch] Mid-speed updates in GapCheck (between stop/start thresholds) cancel timeout job but never reschedule it — gap stalls indefinitely [TripDetector.kt:163-172]
- [x] [Review][Defer] `recoverGapToDriving` resets Driving state with zeroed distanceKm/durationMs — HUD sees distance drop to 0 [TripDetector.kt:210] — deferred, pre-existing pattern from Story 2-2
- [x] [Review][Defer] No test for coroutine-based gap timeout path (delay-driven, no location arrives) — deferred, requires advanceTimeBy infrastructure

## Change Log
- 2026-05-11: Story 2-7 implemented — GPS gap handling with GapCheck state machine, recovery, timeout, teleport detection, accuracy ramping (GLM-5.1)
- 2026-05-11: Code review — 7 patches applied (gap timeout Job leak, race condition fix, preGapLocation correction, resetState cleanup, graceful shutdown gap reset, accuracy ramp off-by-one, mid-speed timeout stall)
