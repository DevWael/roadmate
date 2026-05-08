# Story 7.1: Driving Summaries & Statistics

Status: ready-for-dev

## Story

As a driver,
I want to see summaries of my driving, fuel, and maintenance costs over time,
so that I can understand my vehicle usage patterns and total cost of ownership.

## Acceptance Criteria

1. **Period selector** — Day, Week, Month (default), Year.

2. **Statistics** — Total distance, total trips, avg trip distance, total driving time, total fuel cost, total maintenance cost, cost/km.

3. **Month view** — Values from trips, fuel, maintenance within selected calendar month. Query <200ms.

4. **Year view** — Month-by-month breakdown list (distance, fuel cost, maintenance cost) + running total.

5. **Empty period** — All values 0/"—" + "No data for this period".

6. **Week comparison** — Current vs previous week: distance change (↑12%/↓5%), fuel cost change.

## Tasks / Subtasks

- [ ] Task 1: Statistics screen (AC: #1, #5)
  - [ ] Create `app-phone/ui/hub/StatisticsScreen.kt`
  - [ ] Period selector tabs (Day/Week/Month/Year)
  - [ ] Empty state for no-data periods

- [ ] Task 2: Statistics calculator (AC: #2, #3)
  - [ ] Create `core/util/StatisticsCalculator.kt`
  - [ ] Aggregate trips by date range
  - [ ] Sum fuel costs, maintenance costs
  - [ ] Calculate cost/km

- [ ] Task 3: Year breakdown (AC: #4)
  - [ ] Month-by-month list with totals
  - [ ] Running total at top

- [ ] Task 4: Week comparison (AC: #6)
  - [ ] Compare current week aggregates vs previous
  - [ ] Calculate percentage change

## Dev Notes

### Cost per km calculation:
```kotlin
val costPerKm = if (totalDistanceKm > 0) (totalFuelCost + totalMaintenanceCost) / totalDistanceKm else 0.0
```

### References

- [Source: epics.md#Story 7.1] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
