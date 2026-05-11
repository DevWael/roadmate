# Story 3.6: Notification System (WorkManager)

Status: done

## Story

As a driver,
I want to receive phone notifications when maintenance is due or documents are expiring,
so that I'm alerted even when I'm not actively using the app.

## Acceptance Criteria

1. **Notification channels** — "Maintenance Alerts" (high importance), "Document Reminders" (default importance).

2. **PeriodicWorkRequest** — Every 12 hours, flex 6 hours. `NotificationCheckWorker` queries all schedules and documents for active vehicle.

3. **Maintenance notification** — Title: "[Vehicle] - Maintenance Due", body: "[Item] is due in [X km]". Deep-links to maintenance detail.

4. **Document notification** — Title: "[Vehicle] - Document Expiring", body: "[Document] expires in [X days]". Deep-links to document detail.

5. **No duplicates** — Consistent notification IDs per schedule/document.

6. **Permission handling** — `POST_NOTIFICATIONS` denied = no notifications, no crash.

7. **No-op when nothing due** — Worker completes successfully with no notifications.

## Tasks / Subtasks

- [x] Task 1: Notification channels (AC: #1)
  - [x] Create channels in `RoadMatePhoneApplication.onCreate()`
  - [x] "Maintenance Alerts" high importance, "Document Reminders" default

- [x] Task 2: NotificationCheckWorker (AC: #2, #3, #4)
  - [x] Create `app-phone/worker/NotificationCheckWorker.kt` extending `CoroutineWorker`
  - [x] Query MaintenanceSchedules within alert threshold
  - [x] Query Documents within reminder window
  - [x] Build notifications with PendingIntent deep-links

- [x] Task 3: WorkManager setup (AC: #2)
  - [x] Enqueue `PeriodicWorkRequest` in app init
  - [x] 12-hour repeat, 6-hour flex
  - [x] `ExistingPeriodicWorkPolicy.KEEP`

- [x] Task 4: Deduplication (AC: #5)
  - [x] Notification ID = `scheduleId.hashCode()` or `documentId.hashCode()`
  - [x] Same ID replaces previous notification

- [x] Task 5: Permission handling (AC: #6)
  - [x] Check `POST_NOTIFICATIONS` before posting
  - [x] Gracefully skip if denied

## Dev Notes

### Architecture Compliance

**WorkManager is phone-only.** Head unit doesn't need notifications — it has AttentionBand.

**Deep-link pattern:**
```kotlin
val intent = Intent(context, MainActivity::class.java).apply {
    data = Uri.parse("roadmate://maintenance/$scheduleId")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
}
```

**Notification ID strategy:** Use `scheduleId.hashCode()` for stable IDs. Same item = same ID = no duplicates.

### References

- [Source: architecture.md#Notification Architecture] — WorkManager, channels
- [Source: epics.md#Story 3.6] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
GLM-5.1

### Debug Log References
None

### Completion Notes List
- Created `NotificationChannels` helper object with two channels: "Maintenance Alerts" (IMPORTANCE_HIGH) and "Document Reminders" (IMPORTANCE_DEFAULT)
- Created `NotificationCheckWorker` extending `CoroutineWorker` using `EntryPointAccessors` to inject repositories without hilt-work dependency
- Worker queries active vehicle, checks all maintenance schedules against `MaintenancePredictionEngine.classifyBand()` for WARNING/CRITICAL/OVERDUE states
- Worker checks documents using per-document `reminderDaysBefore` threshold
- Deduplication via `scheduleId.hashCode()` and `documentId.hashCode()` for stable notification IDs
- Permission handling checks `POST_NOTIFICATIONS` on API 33+ before posting
- WorkManager `PeriodicWorkRequest` enqueued in `RoadMatePhoneApplication.onCreate()` with 12h repeat, 6h flex, KEEP policy
- Added `POST_NOTIFICATIONS` permission to AndroidManifest
- Added WorkManager and material-icons-extended dependencies to app-phone

### File List
- `app-phone/build.gradle.kts` (modified — added WorkManager, icons-extended, junit-engine deps)
- `app-phone/src/main/AndroidManifest.xml` (modified — added POST_NOTIFICATIONS permission)
- `app-phone/src/main/kotlin/com/roadmate/phone/RoadMatePhoneApplication.kt` (modified — channel creation, WorkManager enqueue)
- `app-phone/src/main/kotlin/com/roadmate/phone/worker/NotificationChannels.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/worker/NotificationCheckWorker.kt` (new)
- `app-phone/src/main/res/drawable/ic_notification.xml` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/worker/NotificationChannelsTest.kt` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/worker/NotificationIdStrategyTest.kt` (new)

### Review Findings
- [x] [Review][Patch] Dead TripDao injection + dailyAvg computation [NotificationCheckWorker.kt:57,72-73]
- [x] [Review][Patch] Expired document shows "expires in -N days" at boundary [NotificationCheckWorker.kt:149-154]
- [x] [Review][Patch] Notification ID collision between schedule and document [NotificationCheckWorker.kt:143,178]
- [x] [Review][Patch] Worker doWork() lacks top-level try-catch [NotificationCheckWorker.kt:48-104]
- [x] [Review][Patch] Tautological / weak notification ID tests [NotificationIdStrategyTest.kt:13,22]
- [x] [Review][Defer] "Overdue" label for time-only items — deferred, pre-existing engine semantics

## Change Log
- 2026-05-12: Implemented notification system with WorkManager periodic checks, notification channels, deduplication, and permission handling
