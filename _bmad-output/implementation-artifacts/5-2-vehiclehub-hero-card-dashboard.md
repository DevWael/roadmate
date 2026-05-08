# Story 5.2: VehicleHub — Hero Card & Dashboard Layout

Status: ready-for-dev

## Story

As a driver,
I want a single-scroll dashboard on my phone showing my vehicle's key information,
so that I can see everything important about my car in one glance.

## Acceptance Criteria

1. **LazyColumn layout** — VehicleHeroCard → AttentionBand (if alerts) → Maintenance summary → Recent trips → Fuel log summary.

2. **VehicleHeroCard** — Vehicle silhouette icon (64dp, onSurfaceVariant), vehicle name (headlineMedium), ODO (displayLarge 36sp Bold), sync status (labelSmall). Background surfaceVariant (#1A1A1A), 4dp corners.

3. **Sync status display** — <5min: "Last synced: just now" default color. >1h: "[X] hours ago" default. Never: "Not yet synced" tertiary (#FFB74D).

4. **AttentionBand** — Up to 2 strips + "+N more". Tap navigates. Swipe defers.

5. **Pull-to-refresh** — Triggers manual BT sync (Story 4.4). Shows indicator until complete/fail.

6. **Query performance** — All dashboard data within 200ms.

## Tasks / Subtasks

- [ ] Task 1: VehicleHeroCard (AC: #2, #3)
  - [ ] Create `app-phone/ui/hub/VehicleHeroCard.kt`
  - [ ] Vehicle icon, name, ODO, sync status
  - [ ] Sync time ago logic

- [ ] Task 2: VehicleHub screen (AC: #1)
  - [ ] Create `app-phone/ui/hub/VehicleHubScreen.kt` with LazyColumn
  - [ ] Compose sections: hero, attention, maintenance, trips, fuel

- [ ] Task 3: Pull-to-refresh (AC: #5)
  - [ ] Use M3 `pullToRefreshState`
  - [ ] Wire to SyncTriggerManager.triggerManualSync()

- [ ] Task 4: Section cards (AC: #1)
  - [ ] Maintenance summary card with top 3 GaugeArc Compact
  - [ ] Recent trips card with last 3 trips
  - [ ] Fuel log summary with this month's stats

## Dev Notes

### Architecture Compliance

**Screen/Content split** applies to VehicleHubScreen. ViewModel collects all data Flows.

**Section cards are tap-navigable.** Each section card has "See all →" that navigates to the full list screen.

### References

- [Source: ux-design-specification.md#VehicleHub] — Tesla-style layout
- [Source: ux-design-specification.md#VehicleHeroCard] — Card specs
- [Source: epics.md#Story 5.2] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
