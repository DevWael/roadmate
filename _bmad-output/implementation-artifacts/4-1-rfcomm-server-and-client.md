# Story 4.1: RFCOMM Server & Client Connection

Status: ready-for-dev

## Story

As a developer,
I want a reliable Bluetooth RFCOMM connection between the head unit and phone,
so that a communication channel exists for syncing data between both devices.

## Acceptance Criteria

1. **RFCOMM server** — Head unit opens SPP server socket with app-specific UUID. Listens on background thread. Started as part of `RoadMateService`.

2. **Client connection** — Phone identifies head unit from bonded devices by UUID. Connects to RFCOMM socket.

3. **State management** — Connected: `BtConnectionState.Connected`, both read/write to streams.

4. **Reconnection** — Disconnection → `Disconnected` + exponential backoff (2s, 4s, 8s, max 30s). No crash/ANR.

5. **Audio coexistence** — RFCOMM SPP alongside A2DP/HFP without degradation.

6. **Permissions** — `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`. Rationale shown. Denied = sync disabled, other features work.

7. **Device identification** — Filter bonded devices by UUID service record. No match = stay Disconnected, no error toast.

## Tasks / Subtasks

- [ ] Task 1: RFCOMM server (AC: #1)
  - [ ] Create `core/sync/BluetoothSyncServer.kt`
  - [ ] `listenUsingRfcommWithServiceRecord(name, UUID)` on IO dispatcher
  - [ ] Accept connections in loop, delegate to connection handler

- [ ] Task 2: RFCOMM client (AC: #2, #7)
  - [ ] Create `core/sync/BluetoothSyncClient.kt`
  - [ ] Scan bonded devices for matching UUID
  - [ ] `createRfcommSocketToServiceRecord(UUID)` + connect

- [ ] Task 3: Connection manager (AC: #3, #4)
  - [ ] Create `core/sync/BluetoothConnectionManager.kt`
  - [ ] Update `BluetoothStateManager` on connect/disconnect
  - [ ] Exponential backoff reconnection
  - [ ] Handle IOException gracefully

- [ ] Task 4: Permissions (AC: #6)
  - [ ] Permission request flow in phone app
  - [ ] Rationale dialog
  - [ ] Graceful degradation if denied

## Dev Notes

### Architecture Compliance

**App-specific UUID (MUST be consistent):**
```kotlin
val ROADMATE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
// Or use a custom UUID for RoadMate-specific identification
```

**RFCOMM operates on its own SPP channel** — independent of A2DP (audio) and HFP (calls). No interference expected.

**Reconnection runs on client side only.** Server always listens. Client reconnects with backoff.

### Anti-Patterns to Avoid

| ❌ Do NOT | ✅ Do THIS |
|---|---|
| Block main thread with BT operations | All BT I/O on `Dispatchers.IO` |
| Use `startDiscovery()` for known devices | Use `getBondedDevices()` for paired devices |
| Crash on BT permission denied | Disable sync, keep other features |

### References

- [Source: architecture.md#Bluetooth Sync Architecture] — RFCOMM, UUID, connection model
- [Source: architecture.md#Connection Management] — Reconnection strategy
- [Source: epics.md#Story 4.1] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
