# Story 6.1: ParkedDashboard — Three-Panel Layout

Status: ready-for-dev

## Story

As a driver,
I want a rich information dashboard when my car is parked,
so that I can review trips, check maintenance status, and see vehicle info at a glance on the head unit.

## Acceptance Criteria

1. **Three-panel landscape** — 12-column grid, 24dp margins, 16dp gutters.

2. **Left panel (4 cols)** — Vehicle ODO (displayLarge), StatusChip tracking, StatusChip sync, vehicle name (headlineMedium).

3. **Center panel (4 cols)** — "Recent Trips" header (titleLarge), scrollable trip cards (max 5), 60fps.

4. **Right panel (4 cols)** — "Maintenance" header (titleLarge), 3 GaugeArc Compact (most urgent). Item name below each in labelLarge.

5. **Empty states** — No trips: "No trips yet". No maintenance: "No maintenance items configured".

6. **Performance** — All panels render within 2s of state transition. Room queries <200ms.

## Tasks / Subtasks

- [ ] Task 1: Grid layout (AC: #1)
  - [ ] Create `app-headunit/ui/parked/ParkedDashboard.kt`
  - [ ] Row with 3 equal-weight columns (or weighted 4:4:4)
  - [ ] 24dp outer margins, 16dp gaps

- [ ] Task 2: Left panel (AC: #2)
  - [ ] ODO displayLarge, StatusChips, vehicle name
  - [ ] Use existing StatusChip from Story 4.5

- [ ] Task 3: Center panel (AC: #3, #5)
  - [ ] Recent trips LazyColumn (max 5 items)
  - [ ] Trip cards: date, distance, duration
  - [ ] Empty state

- [ ] Task 4: Right panel (AC: #4, #5)
  - [ ] Top 3 maintenance GaugeArc Compact
  - [ ] Sort by urgency (highest percentage first)
  - [ ] Empty state

## Dev Notes

### Architecture Compliance

**This replaces DashboardShell from Story 1.6.** ParkedDashboard is the full implementation. DashboardShell was a placeholder.

**Touch targets still 76dp** even for info-display panels — any future interactive elements must comply.

### References

- [Source: ux-design-specification.md#Parked Dashboard] — Three-panel layout
- [Source: architecture.md#Dashboard Architecture] — Panel content
- [Source: epics.md#Story 6.1] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
