# Story 2.4: Crash Recovery Journal & Power Loss Protection

Status: ready-for-dev

## Story

As a driver,
I want my trip data to survive sudden power cuts (car ignition off),
so that I never lose more than a few seconds of driving data.

## Acceptance Criteria

1. **DataStore journal** — Every 30s during active trip: `tripId`, `vehicleId`, `currentDistanceKm`, `currentDurationMs`, `lastKnownOdometerKm`, `lastFlushTimestamp`, `status=ACTIVE`. File <1KB.

2. **Boot recovery** — On reboot, `CrashRecoveryManager` reads journal. If active trip found, checks Room.

3. **Trip finalization** — Active trip in journal + Room → finalize: `endTime=lastFlushTimestamp`, `status=INTERRUPTED`, update vehicle odometer. Clear journal after.

4. **Recovery speed** — Full state restoration within 5 seconds of reboot.

5. **Data loss bounds** — Max GPS loss: 10s (one flush). Max summary loss: 30s (one journal write).

6. **Graceful shutdown** — `PowerReceiver` on `ACTION_SHUTDOWN`: flush TripPoints, update journal, finalize Trip as `COMPLETED`.

7. **WAL resilience** — Room WAL mode recovers to last consistent state after interrupted write.

## Tasks / Subtasks

- [ ] Task 1: Crash recovery journal (AC: #1)
  - [ ] Create `core/util/CrashRecoveryJournal.kt` using DataStore Preferences
  - [ ] Define journal keys: tripId, vehicleId, distanceKm, durationMs, odometerKm, lastFlush, status
  - [ ] Write every 30 seconds during active trip via coroutine timer

- [ ] Task 2: CrashRecoveryManager (AC: #2, #3, #4)
  - [ ] Create `core/util/CrashRecoveryManager.kt`
  - [ ] On boot: read journal → check Room for active trip → finalize with INTERRUPTED
  - [ ] Update vehicle odometer from journal's lastKnownOdometerKm
  - [ ] Clear journal after successful recovery
  - [ ] Complete within 5 seconds

- [ ] Task 3: PowerReceiver integration (AC: #6)
  - [ ] Wire `PowerReceiver.onReceive()` to flush buffer + update journal + finalize trip
  - [ ] Use `goAsync()` for extended BroadcastReceiver time

- [ ] Task 4: Integration with TripRecorder (AC: #1, #5)
  - [ ] TripRecorder writes to journal every 30s alongside its 10s Room flush
  - [ ] Journal values sourced from current in-memory trip state

## Dev Notes

### Architecture Compliance

**DataStore journal keys:**
```kotlin
object JournalKeys {
    val TRIP_ID = stringPreferencesKey("journal_trip_id")
    val VEHICLE_ID = stringPreferencesKey("journal_vehicle_id")
    val DISTANCE_KM = doublePreferencesKey("journal_distance_km")
    val DURATION_MS = longPreferencesKey("journal_duration_ms")
    val ODOMETER_KM = doublePreferencesKey("journal_odometer_km")
    val LAST_FLUSH = longPreferencesKey("journal_last_flush")
    val STATUS = stringPreferencesKey("journal_status")
}
```

**Recovery runs BEFORE service starts tracking.** Sequence: Boot → CrashRecoveryManager.recover() → Start GpsTracker/TripDetector.

**goAsync() in PowerReceiver:** BroadcastReceiver has 10s limit. Use `goAsync()` + coroutine scope for flush operations.

### References

- [Source: architecture.md#Data Integrity — Crash Recovery] — Journal pattern, recovery flow
- [Source: architecture.md#Power Loss Handling] — Flush intervals, WAL
- [Source: epics.md#Story 2.4] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
