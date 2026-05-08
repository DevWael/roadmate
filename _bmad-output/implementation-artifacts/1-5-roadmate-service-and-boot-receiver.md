# Story 1.5: Head Unit Foreground Service & Boot Receiver

Status: ready-for-dev

## Story

As a driver,
I want the head unit to automatically start tracking when my car boots,
so that I never have to manually launch the app or worry about it running.

## Acceptance Criteria

1. **Boot auto-start** — `BootReceiver` starts `RoadMateService` as foreground service within 5 seconds of `ACTION_BOOT_COMPLETED`.

2. **Foreground notification** — Persistent notification: channel "RoadMate Tracking", content "RoadMate tracking active", not dismissible, `FOREGROUND_SERVICE_LOCATION` type.

3. **State managers** — `DrivingStateManager`, `BluetoothStateManager`, `LocationStateManager` — Hilt `@Singleton`, public `StateFlow` (read-only) + private `MutableStateFlow`. Initials: `Idle`, `Disconnected`, `Acquiring`.

4. **Memory budget** — Service under 50MB during idle.

5. **Zero network permissions** — Verified via `adb shell dumpsys package`.

6. **Graceful shutdown** — `PowerReceiver` handles `ACTION_SHUTDOWN`, triggers pending data flush.

7. **LocationProvider abstraction** — `FusedLocationProvider` when Play Services available, `PlatformLocationProvider` (LocationManager) fallback.

## Tasks / Subtasks

- [ ] Task 1: State managers (AC: #3)
  - [ ] Create `core/state/DrivingStateManager.kt` — singleton, `StateFlow<DrivingState>`
  - [ ] Create `core/state/BluetoothStateManager.kt` — singleton, `StateFlow<BtConnectionState>`
  - [ ] Create `core/state/LocationStateManager.kt` — singleton, `StateFlow<GpsState>`

- [ ] Task 2: LocationProvider interface (AC: #7)
  - [ ] Create `core/location/LocationProvider.kt` interface
  - [ ] Create `core/location/FusedLocationProvider.kt` — uses FusedLocationProviderClient
  - [ ] Create `core/location/PlatformLocationProvider.kt` — fallback using LocationManager.GPS_PROVIDER
  - [ ] Create Hilt module providing LocationProvider with Play Services check

- [ ] Task 3: RoadMateService (AC: #2)
  - [ ] Create `app-headunit/service/RoadMateService.kt` — foreground service
  - [ ] Create notification channel "RoadMate Tracking"
  - [ ] Start with `startForeground()` using `FOREGROUND_SERVICE_LOCATION`
  - [ ] Inject state managers via Hilt `@AndroidEntryPoint`

- [ ] Task 4: Boot receiver (AC: #1)
  - [ ] Create `app-headunit/receiver/BootReceiver.kt`
  - [ ] Register in `AndroidManifest.xml` with `RECEIVE_BOOT_COMPLETED` permission
  - [ ] Start `RoadMateService` on `ACTION_BOOT_COMPLETED`

- [ ] Task 5: Power receiver (AC: #6)
  - [ ] Create `app-headunit/receiver/PowerReceiver.kt`
  - [ ] Register for `ACTION_SHUTDOWN`
  - [ ] Trigger data flush on shutdown intent

- [ ] Task 6: Manifest permissions (AC: #5)
  - [ ] Add `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
  - [ ] Add `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `RECEIVE_BOOT_COMPLETED`
  - [ ] Verify ZERO `INTERNET`/`ACCESS_NETWORK_STATE` permissions

## Dev Notes

### Architecture Compliance

**State manager pattern (MUST follow):**
```kotlin
@Singleton
class DrivingStateManager @Inject constructor() {
    private val _drivingState = MutableStateFlow<DrivingState>(DrivingState.Idle)
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()
    fun updateState(state: DrivingState) { _drivingState.value = state }
}
```

**Foreground service pattern:**
```kotlin
@AndroidEntryPoint
class RoadMateService : Service() {
    @Inject lateinit var drivingStateManager: DrivingStateManager
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
}
```

**Critical: START_STICKY** — Service must restart if killed by system. Car head units may have aggressive battery management.

**LocationProvider fallback:** Check `GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)`. If not `SUCCESS`, use `PlatformLocationProvider`.

### Anti-Patterns to Avoid

| ❌ Do NOT | ✅ Do THIS |
|---|---|
| Use `startService()` without foreground | Always `startForeground()` |
| Put business logic in Service | Service coordinates; logic in managers |
| Use `LiveData` for state | `StateFlow` only |
| Add INTERNET permission | Zero network — hard constraint |

### References

- [Source: architecture.md#State Management] — State manager pattern
- [Source: architecture.md#Service Architecture] — Foreground service, boot receiver
- [Source: architecture.md#Location Pipeline] — LocationProvider abstraction
- [Source: epics.md#Story 1.5] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
