# Story 4.2: Delta Sync Protocol

Status: ready-for-dev

## Story

As a developer,
I want an efficient delta sync protocol that transfers only modified records,
so that sync completes quickly and minimizes data transfer over the Bluetooth link.

## Acceptance Criteria

1. **SYNC_STATUS message** — Initiator sends `lastSyncTimestamp` per entity type.

2. **Delta query** — Receiver queries Room for entities with `lastModified > receivedTimestamp`.

3. **PUSH message format** — Length-prefixed JSON: 4-byte big-endian header + UTF-8 JSON. Contains `entityType`, `data` (DTO array), `messageId` (UUID).

4. **TripPoint batching** — >100 TripPoints → batched into 100-per-message. Each batch has own `messageId`.

5. **ACK handling** — Receiver sends ACK with `messageId` after successful upsert.

6. **Sync completion** — All ACKs received → update `lastSyncTimestamp` on both. State → `Connected`.

7. **Performance** — Typical day (50 TripPoints, 1 Trip) syncs within 5 seconds.

## Tasks / Subtasks

- [ ] Task 1: Message protocol (AC: #1, #3, #5)
  - [ ] Create `core/sync/protocol/SyncMessage.kt` sealed hierarchy: SyncStatus, Push, Ack
  - [ ] Create `core/sync/protocol/MessageSerializer.kt` — length-prefixed JSON encoding/decoding
  - [ ] 4-byte big-endian length + UTF-8 JSON payload

- [ ] Task 2: Delta query engine (AC: #2)
  - [ ] Create `core/sync/DeltaSyncEngine.kt`
  - [ ] Query each entity type for `lastModified > threshold`
  - [ ] Convert to sync DTOs

- [ ] Task 3: TripPoint batching (AC: #4)
  - [ ] Chunk TripPoint lists into batches of 100
  - [ ] Each batch = separate PUSH with unique messageId

- [ ] Task 4: ACK tracking & completion (AC: #5, #6)
  - [ ] Track sent messageIds, mark as acked on ACK receipt
  - [ ] When all acked → update lastSyncTimestamp
  - [ ] Update BluetoothStateManager → Connected

- [ ] Task 5: Sync session orchestrator (AC: #7)
  - [ ] Create `core/sync/SyncSession.kt` — orchestrates full sync flow
  - [ ] Bidirectional: both sides send deltas

## Dev Notes

### Architecture Compliance

**Length-prefixed JSON wire format:**
```kotlin
fun writeMessage(output: OutputStream, json: String) {
    val bytes = json.toByteArray(Charsets.UTF_8)
    output.write(ByteBuffer.allocate(4).putInt(bytes.size).array())
    output.write(bytes)
    output.flush()
}
fun readMessage(input: InputStream): String {
    val lengthBytes = ByteArray(4)
    input.readFully(lengthBytes)
    val length = ByteBuffer.wrap(lengthBytes).int
    val payload = ByteArray(length)
    input.readFully(payload)
    return String(payload, Charsets.UTF_8)
}
```

**Sync DTOs from Story 1.3** are the wire format. `@Serializable` with `kotlinx.serialization.json`.

### References

- [Source: architecture.md#Sync Protocol] — Delta sync, message format, batching
- [Source: architecture.md#Serialization] — Length-prefixed JSON
- [Source: epics.md#Story 4.2] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
