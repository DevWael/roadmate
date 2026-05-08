# Story 2.3: Trip Recording & TripPoint Persistence

Status: ready-for-dev

## Story

As a driver,
I want each trip to record my route, distance, speed, and duration automatically,
so that I have an accurate log of every journey.

## Acceptance Criteria

1. **TripPoint creation** — During active trip, each location update creates TripPoint with tripId, lat, lng, speed, altitude, accuracy, timestamp.

2. **10-second buffer flush** — TripPoints buffered in memory, flushed to Room every 10 seconds in a single transaction.

3. **Distance calculation** — Haversine formula between consecutive valid points. Low accuracy points (>50m) excluded from distance but persisted.

4. **Trip summary updates** — `distanceKm`, `durationMs`, `maxSpeedKmh`, `avgSpeedKmh` (distance/time), `estimatedFuelL` (distance × cityConsumption/100) updated on each flush.

5. **Flush atomicity** — TripPoints + Trip summary updated in same Room transaction. No UI blocking.

6. **Trip finalization** — On Driving→Idle: `endTime` set, `status=COMPLETED`, `endOdometerKm=startOdometerKm+distanceKm`, remaining buffer flushed immediately.

## Tasks / Subtasks

- [ ] Task 1: TripRecorder (AC: #1, #2)
  - [ ] Create `core/location/TripRecorder.kt`
  - [ ] Buffer TripPoints in memory (ConcurrentLinkedQueue or similar)
  - [ ] 10-second flush timer using coroutine `delay` loop

- [ ] Task 2: Distance calculator (AC: #3)
  - [ ] Create `core/util/HaversineCalculator.kt`
  - [ ] Skip points with accuracy >50m for distance calc
  - [ ] Still persist all points for route completeness

- [ ] Task 3: Trip summary updater (AC: #4)
  - [ ] Update Trip fields on each flush: distance, duration, max/avg speed, fuel estimate
  - [ ] `avgSpeedKmh = distanceKm / (durationMs / 3_600_000.0)`
  - [ ] `estimatedFuelL = distanceKm * (cityConsumption / 100.0)`

- [ ] Task 4: Transactional flush (AC: #5)
  - [ ] Use `@Transaction` for batch TripPoint insert + Trip update
  - [ ] Execute on `Dispatchers.IO`

- [ ] Task 5: Trip finalization (AC: #6)
  - [ ] Flush remaining buffer immediately on trip end
  - [ ] Set `endTime`, `status`, `endOdometerKm`

## Dev Notes

### Architecture Compliance

**Haversine formula:**
```kotlin
fun haversineDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371.0 // Earth radius km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng/2).pow(2)
    return R * 2 * asin(sqrt(a))
}
```

**10s flush = max data loss on power cut.** This is the acceptable tradeoff per NFR (Story 2.4 handles recovery).

### References

- [Source: architecture.md#Trip Recording Pipeline] — Buffer, flush, Haversine
- [Source: architecture.md#Data Integrity] — WAL, transaction pattern
- [Source: epics.md#Story 2.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
