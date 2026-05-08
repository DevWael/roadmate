# Story 3.6: Notification System (WorkManager)

Status: ready-for-dev

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

- [ ] Task 1: Notification channels (AC: #1)
  - [ ] Create channels in `RoadMatePhoneApplication.onCreate()`
  - [ ] "Maintenance Alerts" high importance, "Document Reminders" default

- [ ] Task 2: NotificationCheckWorker (AC: #2, #3, #4)
  - [ ] Create `app-phone/worker/NotificationCheckWorker.kt` extending `CoroutineWorker`
  - [ ] Query MaintenanceSchedules within alert threshold
  - [ ] Query Documents within reminder window
  - [ ] Build notifications with PendingIntent deep-links

- [ ] Task 3: WorkManager setup (AC: #2)
  - [ ] Enqueue `PeriodicWorkRequest` in app init
  - [ ] 12-hour repeat, 6-hour flex
  - [ ] `ExistingPeriodicWorkPolicy.KEEP`

- [ ] Task 4: Deduplication (AC: #5)
  - [ ] Notification ID = `scheduleId.hashCode()` or `documentId.hashCode()`
  - [ ] Same ID replaces previous notification

- [ ] Task 5: Permission handling (AC: #6)
  - [ ] Check `POST_NOTIFICATIONS` before posting
  - [ ] Gracefully skip if denied

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
### Debug Log References
### Completion Notes List
### File List
