# Story 4.3: Conflict Resolution & Idempotency

Status: ready-for-dev

## Story

As a developer,
I want sync to handle conflicts and retries without corrupting or duplicating data,
so that both devices maintain consistent and accurate records.

## Acceptance Criteria

1. **Last-write-wins** — Same UUID modified on both sides → higher `lastModified` wins. Loser silently overwritten.

2. **Local wins** — If local `lastModified` > incoming → discard incoming.

3. **Remote wins** — If local `lastModified` < incoming → overwrite local via `@Upsert`.

4. **Interrupted sync** — Unacked PUSH retransmitted on next session.

5. **Idempotent upsert** — Duplicate PUSH (retry) produces identical result. No duplicates.

6. **Incomplete sync** — `lastSyncTimestamp` NOT updated if sync incomplete. Next session re-evaluates all unsynced.

## Tasks / Subtasks

- [ ] Task 1: Conflict resolver (AC: #1, #2, #3)
  - [ ] Create `core/sync/ConflictResolver.kt`
  - [ ] Compare `lastModified` timestamps before upsert
  - [ ] Skip incoming if local is newer

- [ ] Task 2: Retry handling (AC: #4, #5)
  - [ ] Track unacked messages in SyncSession
  - [ ] Retransmit on next session
  - [ ] UUID-based upsert = natural idempotency

- [ ] Task 3: Timestamp management (AC: #6)
  - [ ] Only update `lastSyncTimestamp` when ALL messages acked
  - [ ] Store per-entity-type timestamps in DataStore

## Dev Notes

### Architecture Compliance

**Conflict resolution is IMPLICIT via @Upsert + timestamp check:**
```kotlin
suspend fun processIncoming(dto: VehicleSyncDto) {
    val local = vehicleDao.getVehicleSync(dto.id) // non-Flow sync query
    if (local == null || dto.lastModified > local.lastModified) {
        vehicleDao.upsert(dto.toEntity())
    }
    // else: discard incoming, local is newer
}
```

**UUID PKs guarantee idempotency.** Re-processing the same PUSH = same UUID = same upsert result.

### References

- [Source: architecture.md#Conflict Resolution] — Last-write-wins, idempotency
- [Source: epics.md#Story 4.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
