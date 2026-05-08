# Story 3.4: Maintenance Prediction & Alerting Logic

Status: ready-for-dev

## Story

As a driver,
I want the system to predict when maintenance will be due and alert me proactively,
so that I can plan service visits before items become overdue.

## Acceptance Criteria

1. **Daily average** — `totalDistanceLast30Days / 30`. Fallback: 50 km/day if <7 days data.

2. **Predicted date** — `today + (remainingKm / dailyAverage)` days. For dual intervals, earlier date wins.

3. **AttentionBand** — Configurable km threshold (default 500km). Amber warning, red critical on head unit parked dashboard.

4. **Band text** — "[Item name] due in [X km]". Tapping navigates to maintenance detail.

5. **Stacking** — Max 2 visible bands + "+N more". Most critical first.

6. **Dismiss** — Swipe to defer (hidden from view). Reappears on next launch/sync.

7. **Overdue** — 100% items stay at 100% red with pulse. Text: "[Item name] overdue by [X km]".

## Tasks / Subtasks

- [ ] Task 1: Prediction engine (AC: #1, #2)
  - [ ] Create `core/util/MaintenancePredictionEngine.kt`
  - [ ] Calculate daily average from Trip data (last 30 days)
  - [ ] Predict next service date for each schedule item
  - [ ] Handle dual intervals: earlier predicted date wins

- [ ] Task 2: AttentionBand component (AC: #3, #4, #5)
  - [ ] Create `core/ui/components/AttentionBand.kt`
  - [ ] Amber (tertiary) for warning, red (error) for critical
  - [ ] Max 2 visible + "+N more" label
  - [ ] Navigate to maintenance detail on tap

- [ ] Task 3: Dismiss & defer (AC: #6)
  - [ ] Swipe gesture to dismiss band
  - [ ] Store deferred item IDs in ViewModel state (not persisted)
  - [ ] Reset on app launch/sync

- [ ] Task 4: Overdue state (AC: #7)
  - [ ] Calculate overdue km: `currentOdo - lastServiceKm - intervalKm`
  - [ ] Display "overdue by [X km]" text
  - [ ] Keep red color + pulse animation

## Dev Notes

### Architecture Compliance

**Prediction algorithm:**
```kotlin
fun predictNextServiceDate(schedule: MaintenanceSchedule, vehicle: Vehicle, dailyAvgKm: Double): LocalDate {
    val remainingKm = schedule.intervalKm?.let { it - (vehicle.odometerKm - schedule.lastServiceKm) }
    val daysUntilKmDue = remainingKm?.let { (it / dailyAvgKm).toLong() }
    val daysUntilMonthDue = schedule.intervalMonths?.let { ... }
    return LocalDate.now().plusDays(minOf(daysUntilKmDue ?: Long.MAX_VALUE, daysUntilMonthDue ?: Long.MAX_VALUE))
}
```

**AttentionBand is shared** between head unit and phone. Lives in `core/ui/components/`.

### References

- [Source: architecture.md#Maintenance Prediction] — Prediction algorithm
- [Source: ux-design-specification.md#AttentionBand] — Component specs
- [Source: epics.md#Story 3.4] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
