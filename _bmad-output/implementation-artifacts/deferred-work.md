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
