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

## Deferred from: code review of story 3-1 (2026-05-11)

- **No `@Stable` annotation on `GaugeArcVariant` enum** — Pre-existing pattern across project enums. May cause unnecessary recompositions in Compose. Consider a project-wide pass to annotate all enums used in composable parameters.

## Deferred from: code review of story 3-2 (2026-05-12)

- **Three duplicate FakeMaintenanceDao implementations** — `MaintenanceRepositoryTest`, `VehicleSetupViewModelTest`, and `MaintenanceCompletionViewModelTest` each copy the full abstract DAO fake. Any new abstract method requires updating all three. Extract a shared `TestMaintenanceDao` into a `:core-test` or `testFixtures` module.
- **Undo may silently fail after process death** — `cachedSchedule` and `_previousScheduleValues` are ViewModel-memory-only. If process death occurs within the 4-second Snackbar window, the Undo button callback fires on a fresh ViewModel with null fields and silently no-ops. Practically unlikely but architecturally impure.

## Deferred from: code review of story 3-4 (2026-05-12)

- **remainingKm/overdueKm return 0.0 for null intervalKm** — Conflates "no interval" with "exhausted." Callers can't distinguish. Pre-existing entity design; not introduced by this story.
- **lastServiceDate epoch-to-LocalDate uses UTC** — Head unit runs in driver's local timezone. Late-night service records as next-day UTC, shifting predictions by a day. Pre-existing timezone design gap.
- **AC #4: onTap navigation not wired** — Spec says "Tapping navigates to maintenance detail." Callback exists but requires navigation graph from consuming screen. Integration deferred to dashboard polish story.
- **dailyAverage doesn't filter trips to last 30 days** — Function accepts raw list with no enforcement of the 30-day window. Caller must pre-filter. Pre-existing contract gap.

## Deferred from: code review of story 3-5 (2026-05-12)

- **`expiryDate` defaults to `System.currentTimeMillis()`** — New document form initializes expiry to current timestamp, causing immediate "expiring today" if saved without touching date picker. Pre-existing pattern from AddMaintenanceFormState.
- **No input length limits on name/notes fields** — AddDocumentSheet text fields accept unbounded input. Project-wide concern not specific to this story.
- **`daysUntilExpiry` timezone sensitivity** — Uses `ZoneId.systemDefault()` which can produce ±1 day variance across timezone changes. Systemic issue affecting all date calculations in the app.

## Deferred from: code review of story 4-1 (2026-05-12)

- **Race condition on `isRunning` volatile flag** — `BluetoothConnectionManager.isRunning` is `@Volatile` but checked and set without synchronization. `Job.cancel()` is the real shutdown mechanism; the volatile flag is belt-and-suspenders. Low risk, no crash/ANR.
- **`BluetoothPermissionCheckerTest` has no-op test** — `shouldShowRationale does not crash` asserts `assertTrue(true)` with zero coverage. `REQUIRED_PERMISSIONS` test uses OR condition that passes on both API levels. Pre-existing test pattern issue.

## Deferred from: code review of story 4-2 (2026-05-12)

- **No bidirectional sync — only outgoing push, no incoming receive/upsert logic** — `awaitProtocolCompletion()` only sends outgoing PUSH messages and reads ACKs. There is no logic to receive incoming PUSH messages from the remote side, upsert them locally, or send ACKs back. Scope for Story 4-3 (Conflict Resolution) and Story 4-4 (Sync Triggers).
- **`lastSyncTimestamp` never persisted after sync completion (AC #6)** — `syncComplete()` transitions state to `Connected` but never writes the new sync timestamp to DataStore or any persistent storage. Next sync will use `0L` again. Requires a dedicated persistence mechanism.

## Deferred from: code review of story 4-3 (2026-05-12)

- **ConflictResolver is a copy-paste God Object** — Seven `when` branches contain near-identical `getById → compare timestamp → upsert-or-skip` logic. Extract into a generic `resolveEntity<T>()` function parameterized by DAO lambdas. Architectural improvement, no correctness issue.
- **Per-entity timestamps implemented but unused** — `SyncTimestampStore.setEntityTimestamp/getEntityTimestamp` are implemented and tested but `DeltaSyncEngine.queryDeltas()` uses only the global `lastSyncTimestamp`. Dead code today, presumably intended for future per-entity-type delta queries.
- **ConflictResolver not integrated into SyncSession** — ConflictResolver exists as standalone component with tests, but SyncSession has no incoming PUSH handler wiring. Likely Story 4-4 scope for bidirectional sync.
- **UnackedMessageTracker.drainUnacked() non-atomic drain** — `values.toList()` followed by `clear()` on ConcurrentHashMap is technically non-atomic. Low risk since all callers are on single coroutine context.

## Deferred from: code review of story 4-4 (2026-05-13)

- **eventObservationJobs list not thread-safe** — `SyncTriggerManager.eventObservationJobs` uses `mutableListOf<Job>()` with no synchronization. Pre-existing pattern; lifecycle methods are called sequentially in practice. Address if multi-threaded access becomes a concern.

## Deferred from: code review of story 4-5 (2026-05-13)

- **No `@Preview` composables in StatusChip.kt or ShimmerSkeleton.kt** — Zero preview functions in either new UI file. Pre-existing codebase-wide gap first noted in Story 1-6 review.

## Deferred from: code review of story 5-1 (2026-05-13)

- **NavController state lost when vehicleCount toggles >0 → 0 → >0** — `rememberNavController()` is conditionally scoped inside the `vehicleCount > 0` branch. When count drops to 0 and returns to >0, a fresh NavController is created, losing all backstack state. Pre-existing architecture gap; requires lifting NavController above the conditional.
- **Deep-link not handled on `onNewIntent()` warm re-launch** — `MainActivity` only processes the initial intent in `onCreate()`. If the app is already running and a new deep-link intent arrives via `onNewIntent()`, it is never forwarded to `NavController.handleDeepLink()`. Requires lifecycle-aware refactor.
- **EmptyVehicleState text alignment/padding on narrow screens** — The body text "Connect to your head unit to sync vehicle data." has no horizontal padding or center alignment. On narrow devices, the text may wrap poorly and clip against screen edges. Cosmetic.

## Deferred from: code review of story 5-2 (2026-05-13)

- **\"See all\" clickable Row lacks accessibility contentDescription** — SectionCard "See all →" link is a clickable Row without semantic `contentDescription`. Pre-existing pattern across all section cards. Address in a11y pass.

## Deferred from: code review of story 5-3 (2026-05-13)

- **Duplicate FakeTripDao implementations across test files** — `ListFakeTripDao` and `DetailFakeTripDao` are near-identical copies in separate test files. Pre-existing test pattern; extract to shared test fixture when `:core-test` module is established.
- **No testTag/contentDescription on TripCard for accessibility** — TripCard Row is clickable but lacks semantic `contentDescription` for screen readers. Pre-existing a11y gap across all card components; address in dedicated accessibility pass.

## Deferred from: code review of story 5-5 (2026-05-14)

- **GaugeArc Compact is 48dp not 36dp per spec** — Story spec calls for 36dp progress rings in MaintenanceListItem. The `GaugeArcVariant.Compact` enum value is hardcoded at 48dp in the core component. Requires adding a `Mini(36)` variant or a size-override parameter to `GaugeArc`. Core component change, out of scope for this story.

## Deferred from: code review of story 5-6 (2026-05-14)

- **`onVehicleManagementClick` / `onDocumentListClick` params declared but never used in VehicleHubScreen** — AC#4 says "Can navigate to vehicle management" for single-vehicle scenario, but neither callback is wired to any UI element. Pre-existing params from Story 5-2; navigation targets not yet implemented.
