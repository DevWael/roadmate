# Story 2.7: GPS Gap Handling & Edge Cases

Status: ready-for-dev

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

- [ ] Task 1: GapCheck state (AC: #1)
  - [ ] Add GapCheck handling to TripDetector
  - [ ] Track gap start time, continue duration timer, pause distance accumulation

- [ ] Task 2: Gap recovery (AC: #2, #3)
  - [ ] GPS returns with speed >8 → resume Driving, calculate plausible gap distance
  - [ ] GPS returns with speed <3 → transition to Stopping

- [ ] Task 3: Gap timeout (AC: #4)
  - [ ] 5-minute timer on GapCheck state
  - [ ] Finalize trip as INTERRUPTED with reason

- [ ] Task 4: Teleport detection (AC: #5)
  - [ ] Calculate implied speed across gap: distance / elapsed time
  - [ ] If >200 km/h: discard gap distance, flag boundary points

- [ ] Task 5: Accuracy ramping (AC: #6)
  - [ ] After gap recovery, filter initial low-accuracy readings from distance calculation
  - [ ] Only count points with accuracy ≤50m

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
### Debug Log References
### Completion Notes List
### File List
