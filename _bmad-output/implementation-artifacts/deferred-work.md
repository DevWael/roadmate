# Deferred Work

## Deferred from: code review of story-1-1 (2026-05-08)

- **No `enableEdgeToEdge()` in MainActivities** — With `targetSdk = 36`, Android enforces edge-to-edge. Placeholder activities don't call `enableEdgeToEdge()`. Address in Story 1-4 (Design System & Theme).
- **`configChanges` override on head unit MainActivity** — `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"` prevents activity recreation. Unnecessary for landscape-locked device and can mask lifecycle bugs. Revisit during UI implementation.
- **Inconsistent plugin application pattern for serialization** — `kotlin.serialization` applied via `alias()` while all other plugins use convention `id()`. Minor inconsistency; a convention plugin could be created later but isn't warranted for a single-use plugin.
- **`play-services-location` in `:core` leaks to head unit** — Dependency is unused scaffold placeholder. When Story 2-1 implements location, split into: interfaces in `:core`, Play Services impl in `:app-phone`, `LocationManager` fallback in `:app-headunit`.

## Deferred from: code review of story 1-2 (2026-05-09)

- **TypeConverter crash on removed enum values** — `Converters.kt:20,26,32` — `valueOf()` throws on unknown strings. Needs graceful fallback (e.g., `OTHER`) or migration strategy when enum values are removed in future versions.
- **No automatic `lastModified` enforcement on writes** — Repositories pass through entities without updating timestamps. Service/ViewModel layer (Story 1-3+) must ensure `lastModified` is set before calling repository save methods.
- **No cascade delete integration test** — FK cascade on `MaintenanceSchedule → MaintenanceRecord` is configured but untested. Requires Robolectric or instrumented test with real Room instance.
- **Both schedule intervals can be null simultaneously** — `MaintenanceSchedule.intervalKm` and `intervalMonths` can both be null, creating a schedule with no trigger interval. Domain validation needed at creation time.
- **No domain validation on numeric fields** — `Vehicle.year`, `engineSize`, `odometerKm` accept any value including negatives and zero. Input validation needed in UI/service layer.

## Deferred from: code review of story 1-3 (2026-05-09)

- **No UNIQUE constraint for multiple ACTIVE trips per vehicle** — `getActiveTrip()` returns LIMIT 1 but nothing prevents inserting multiple ACTIVE trips. Domain invariant to be enforced at the service layer (Story 2-2: Trip Detection State Machine).
- **OBDProvider uses blocking `fun` not `suspend fun`** — Real ELM327 calls will need async I/O. Interface needs a breaking change in V2. V1 is GPS-only with mock returning null.
- **Repositories have no error logging** — Write failures caught by `runCatching` but not logged. Pre-existing pattern established in Story 1-2. Should be addressed when logging infrastructure is set up.
- **`SyncPush.payload` is an untyped `String`** — No schema validation or entity-type envelope. Will be addressed when the sync service is implemented (Epic 5).

## Deferred from: code review of story 1-4 (2026-05-09)

- **`DrivingState.Driving.tripId` is untyped `String`** — Consider a value class wrapper (`TripId`) to prevent mixing with other string fields. Deferred to trip tracking epic (Epic 2) where the ID type will be formally established.

## Deferred from: code review of story 1-5 (2026-05-09)

- **stop/start race on isRequesting AtomicBoolean** — `stopLocationUpdates()` resets `isRequesting` unconditionally without awaiting removal confirmation; concurrent start/stop can leave stale state. Acceptable for V1 single-caller pattern.
- **State managers expose unrestricted public updateState()** — Any class can mutate driving/BT/location state without transition validation. Acceptable for V1 where only the service writes state; should add visibility constraints when multi-writer scenarios arise.
- **Notification.Builder instead of NotificationCompat.Builder** — `Notification.Builder` works on API 28+ but `NotificationCompat` is the Android recommendation for forward compatibility. Low risk for head unit's fixed OS.

## Deferred from: code review of story 1-6 (2026-05-09)

- **VehicleSwitcherDialog is not modal (no Dialog wrapper)** — Uses a `Card` composable instead of `AlertDialog`/`Dialog`. No scrim, no modal behavior, no accessibility semantics. Address when the full navigation graph is implemented (Story 6-3).
- **No @Preview composables in any UI file** — Zero preview functions across 812 lines of Compose UI. Impossible to visually verify in Android Studio without running the app. General practice gap to address when UI polish is prioritized.

## Deferred from: code review of story 1-7 (2026-05-09)

- **GPS acquisition not triggered by service start** — `RoadMateService` creates a foreground notification but does not invoke `LocationStateManager` to begin GPS tracking. This is by design for story 1-7 scope; GPS pipeline will be implemented in Story 2-1 (GPS Tracker and Location Pipeline).

## Deferred from: code review of story 2-1 (2026-05-09)

- **AC #3 transition latency <100ms not testable** — `collectLatest` on `StateFlow` is architecturally correct but the <100ms constraint is not verifiable in a JVM unit test. Requires instrumented test with real scheduling.
- **AC #6 24-hour leak-free not provable** — `callbackFlow`+`awaitClose` pattern is idiomatic but no stress/long-running test exists. Requires instrumented endurance test.
- **`FusedLocationProvider.HandlerThread` never quit** — `HandlerThread("FusedLocationThread")` is started in field initializer with no corresponding `quitSafely()`. Singleton lifetime makes this acceptable for foreground service but technically a resource leak.
- **`DrivingStateManager.updateState` is unrestricted public API** — Any component can mutate driving state without transition validation. Pre-existing pattern from Story 1-5; should constrain when multi-writer scenarios arise.

## Deferred from: code review of story 2-2 (2026-05-09)

- **`resumeDriving` resets distance/duration to zero** — `DrivingState.Driving(tripId, 0.0, 0L)` on resume resets accumulated metrics. Not tracked by TripDetector yet; will be addressed when trip recording writes distance/duration (Story 2-3).
- **`GapCheck` state silently ignored** — `process()` has an empty handler for `DrivingState.GapCheck` with no logging. GapCheck handling is defined in Story 2-7 (GPS Gap Handling).
- **3-consecutive-readings doesn't enforce 9s timing** — AC says "9s" assuming 3s GPS interval, but code counts readings regardless of time gap. Timing enforcement is the responsibility of GPS interval configuration, not the state machine.

## Deferred from: code review of story 2-3 (2026-05-11)

- **No test for 10-second periodic flush timer (AC #2)** — All tests use immediate finalization via TripEndEvent. The periodic `delay(FLUSH_INTERVAL_MS)` path in `startFlushTimer` is untested. Requires `advanceTimeBy` test infrastructure with `StandardTestDispatcher`.
- **`cityConsumption` defaults to 0.0 when vehicle not found** — `vehicleRepository.getVehicle().firstOrNull()` returns null → `cityConsumption = 0.0` → `estimatedFuelL = 0.0`. Silently wrong data rather than indicating "unknown". Pre-existing vehicle lookup pattern.
- **`startOdometerKm` defaults to 0.0 when trip not loaded from DB** — If `ensureTripLoaded()` can't find the trip, `startOdometerKm = 0.0`. End odometer becomes `0.0 + distanceKm`. Linked to the async race between TripDetector.saveTrip and TripRecorder's first flush.

## Deferred from: code review of story 2-4 (2026-05-11)

- **Flush failure extends data loss beyond 10s** — `flushBuffer()` `onFailure` silently logs; consecutive failures violate AC #5 max GPS loss bound. Needs architectural retry or at-least-once delivery guarantee.
- **AC #4 unverified (5-second recovery)** — No performance test or timing assertion for the 5-second recovery bound. Needs instrumented test with real Room and DataStore.
- **AC #1 file size unenforced** — No runtime guard that journal DataStore stays under 1KB. Low risk with current 7-key schema but could drift.
- **PowerReceiver shutdownScope per-instance** — Android creates new BroadcastReceiver instance per broadcast; scope created per-instance but never reused. GC risk on long-running coroutines. Pre-existing BroadcastReceiver pattern.

## Deferred from: code review of story 2-5 (2026-05-11)

- **Hardcoded UI strings not in string resources** — TripListSection.kt and DashboardShell.kt use inline strings ("Distance", "Duration", "Avg Speed", "No trips recorded yet.", etc.) instead of `stringResource()`. i18n cross-cutting concern to address in a dedicated localization pass.
- **No contentDescription on emoji car icon in empty state** — TripListSection.kt empty state uses `🚗` emoji Text with no accessibility semantics. a11y cross-cutting concern.
- **VehicleRepository.addToOdometer uses System.currentTimeMillis()** — Not using injected `Clock` abstraction. Pre-existing pattern in VehicleRepository; all repo writes use system clock directly.

## Deferred from: code review of story 2-6 (2026-05-11)

- **TripLiveIndicatorTest / AlertStripTest test extracted logic not Compose behavior** — Unit tests verify pure logic (scale branching, message formatting) but never render the composable. Acceptable for unit scope, but Compose UI integration tests should be added when test infrastructure supports them.

## Deferred from: code review of story 2-7 (2026-05-11)

- **`recoverGapToDriving` resets Driving state with zeroed distanceKm/durationMs** — `DrivingState.Driving(tripId, 0.0, 0L)` causes HUD consumers to see distance drop to 0 after gap recovery. Pre-existing pattern from Story 2-2 where `resumeDriving` also zeroes these fields.
- **No test for coroutine-based gap timeout path** — The `delay`-driven timeout in `enterGapCheck` (where no location update ever arrives) is untested. All existing gap timeout tests trigger via a late location update in `handleGapCheck`. Requires `advanceTimeBy` + `StandardTestDispatcher` test infrastructure.

