# Story 5.1: Phone App Shell & Navigation Graph

Status: ready-for-dev

## Story

As a developer,
I want the phone app's navigation structure and screen shell,
so that all phone screens have consistent navigation, top bar, and deep-link support.

## Acceptance Criteria

1. **Theme & NavHost** — `RoadMateTheme` phone typography. `NavHost` with Jetpack Navigation Compose.

2. **Routes** — `@Serializable` classes: VehicleHub, TripList, TripDetail(tripId), MaintenanceList, MaintenanceDetail(scheduleId), FuelLog, DocumentList, DocumentDetail(documentId), VehicleManagement.

3. **Detail scaffolding** — `Scaffold` + `TopAppBar` + back arrow on all detail screens.

4. **Deep-link support** — Notification PendingIntent navigates to target screen. Back from deep-link → VehicleHub.

5. **Empty state** — No vehicles → BT icon + "Connect to your head unit to sync vehicle data."

6. **Max 3 levels** — VehicleHub → Detail → ModalBottomSheet. No deeper nesting.

## Tasks / Subtasks

- [ ] Task 1: Navigation graph (AC: #1, #2)
  - [ ] Define `@Serializable` route classes in `app-phone/navigation/Routes.kt`
  - [ ] Create `NavHost` in `MainActivity` with all routes

- [ ] Task 2: Screen scaffolding (AC: #3)
  - [ ] Create shared `RoadMateScaffold` with TopAppBar + back navigation
  - [ ] Apply to all detail screens

- [ ] Task 3: Deep-link handling (AC: #4)
  - [ ] Configure `deepLinks` in NavHost for `roadmate://` scheme
  - [ ] Handle PendingIntent in MainActivity

- [ ] Task 4: Empty state (AC: #5)
  - [ ] Check vehicle count on launch
  - [ ] Show empty state if zero vehicles

## Dev Notes

### Architecture Compliance

**Type-safe navigation (Compose Navigation 2.8+):**
```kotlin
@Serializable object VehicleHub
@Serializable data class TripDetail(val tripId: String)
@Serializable data class MaintenanceDetail(val scheduleId: String)
```

**Deep-link backstack:** Use `navDeepLink { uriPattern = "roadmate://maintenance/{scheduleId}" }`. NavHost builds synthetic backstack with VehicleHub as root.

### References

- [Source: architecture.md#Navigation Architecture] — Route pattern, deep-links
- [Source: epics.md#Story 5.1] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
