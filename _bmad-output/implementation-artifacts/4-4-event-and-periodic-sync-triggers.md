# Story 4.4: Event & Periodic Sync Triggers

Status: ready-for-dev

## Story

As a driver,
I want data to sync automatically when something happens or on a regular schedule,
so that my phone always has the latest vehicle data without me thinking about it.

## Acceptance Criteria

1. **Trip end trigger** — Sync within 5s of trip completion. Trip + TripPoints included.

2. **Maintenance done trigger** — Sync within 5s. New record + updated schedule included.

3. **Fuel entry trigger** — Sync within 5s (phone → head unit). New FuelLog pushed.

4. **Periodic fallback** — Every 15 minutes if connected and no event sync.

5. **Queue serialization** — New trigger during active sync → queued, processed after. No concurrent syncs.

6. **Pull-to-refresh** — Phone VehicleHub pull gesture → immediate manual sync.

7. **Disconnected tolerance** — Sync triggers silently ignored when BT disconnected. No error/retry.

## Tasks / Subtasks

- [ ] Task 1: Sync trigger manager (AC: #1, #2, #3, #7)
  - [ ] Create `core/sync/SyncTriggerManager.kt`
  - [ ] Event triggers: observe trip completion, maintenance save, fuel entry
  - [ ] Check BT connection state before triggering

- [ ] Task 2: Periodic timer (AC: #4)
  - [ ] 15-minute coroutine timer when connected
  - [ ] Cancel when disconnected, restart on reconnect

- [ ] Task 3: Queue serialization (AC: #5)
  - [ ] `Channel<SyncRequest>` or `Mutex` to serialize sync operations
  - [ ] Process one at a time, queue others

- [ ] Task 4: Manual sync (AC: #6)
  - [ ] Expose `triggerManualSync()` for pull-to-refresh
  - [ ] Returns `Flow<SyncResult>` for UI progress

## Dev Notes

### Architecture Compliance

**Sync trigger serialization:**
```kotlin
private val syncChannel = Channel<SyncRequest>(Channel.CONFLATED)
init {
    scope.launch { for (request in syncChannel) { executeSyncSession() } }
}
fun requestSync(reason: SyncReason) { syncChannel.trySend(SyncRequest(reason)) }
```

**Event observation:** Use `Flow.onEach` to watch for write events from repositories. Or use Room's invalidation tracker.

### References

- [Source: architecture.md#Sync Triggers] — Event, periodic, manual triggers
- [Source: epics.md#Story 4.4] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
